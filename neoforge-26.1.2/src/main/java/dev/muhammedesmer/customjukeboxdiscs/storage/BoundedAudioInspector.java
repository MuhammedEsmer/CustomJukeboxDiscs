package dev.muhammedesmer.customjukeboxdiscs.storage;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.gagravarr.ogg.OggFile;
import org.gagravarr.vorbis.VorbisAudioData;
import org.gagravarr.vorbis.VorbisFile;

public final class BoundedAudioInspector implements AudioInspector {
    private static final int COPY_BUFFER_BYTES = 16 * 1024;
    private static final int SIGNATURE_SCAN_BYTES = 8 * 1024;

    @Override
    public InspectionResult inspect(Path path, long maxBytes, Duration maxDuration) throws IOException {
        if (maxBytes <= 0 || maxDuration.isZero() || maxDuration.isNegative()) {
            throw new IllegalArgumentException("inspection limits must be positive");
        }

        long byteCount = Files.size(path);
        if (byteCount <= 0) {
            throw malformed("audio file is empty", null);
        }
        if (byteCount > maxBytes) {
            throw new AudioValidationException(
                    AudioValidationException.Reason.SIZE_LIMIT,
                    "audio file exceeds the configured byte limit");
        }

        AudioFormat format = detectFormat(path);
        long durationMillis = switch (format) {
            case MP3 -> inspectMp3(path, maxDuration);
            case OGG -> inspectOgg(path, maxDuration);
        };
        return new InspectionResult(format, sha256(path), byteCount, durationMillis);
    }

    private static AudioFormat detectFormat(Path path) throws IOException {
        byte[] signature = new byte[SIGNATURE_SCAN_BYTES];
        int read;
        try (InputStream input = Files.newInputStream(path)) {
            read = input.read(signature);
        }
        for (int offset = 0; offset < read; offset++) {
            if (offset + 3 < read
                    && signature[offset] == 'O' && signature[offset + 1] == 'g'
                    && signature[offset + 2] == 'g' && signature[offset + 3] == 'S') {
                return AudioFormat.OGG;
            }
            if (offset + 2 < read
                    && signature[offset] == 'I' && signature[offset + 1] == 'D' && signature[offset + 2] == '3') {
                return AudioFormat.MP3;
            }
            if (offset + 2 < read && isMpegFrameHeader(signature, offset)) {
                return AudioFormat.MP3;
            }
        }
        throw new AudioValidationException(
                AudioValidationException.Reason.UNSUPPORTED_FORMAT,
                "audio signature is unsupported");
    }

    private static boolean isMpegFrameHeader(byte[] bytes, int offset) {
        if ((bytes[offset] & 0xFF) != 0xFF || (bytes[offset + 1] & 0xE0) != 0xE0) return false;
        int version = (bytes[offset + 1] >> 3) & 0x03;
        int layer = (bytes[offset + 1] >> 1) & 0x03;
        int bitrate = (bytes[offset + 2] >> 4) & 0x0F;
        int sampleRate = (bytes[offset + 2] >> 2) & 0x03;
        return version != 1 && layer != 0 && bitrate != 0 && bitrate != 15 && sampleRate != 3;
    }

    private static long inspectMp3(Path path, Duration maxDuration) throws IOException {
        try {
            Mp3File mp3 = new Mp3File(path.toString());
            if (mp3.getFrameCount() <= 0 || mp3.getSampleRate() <= 0) {
                throw malformed("MP3 stream contains no valid audio frames", null);
            }
            return requireDuration(mp3.getLengthInMilliseconds(), maxDuration);
        } catch (AudioValidationException exception) {
            throw exception;
        } catch (InvalidDataException | UnsupportedTagException | RuntimeException exception) {
            throw malformed("malformed MP3 stream", exception);
        }
    }

    private static long inspectOgg(Path path, Duration maxDuration) throws IOException {
        try (InputStream input = Files.newInputStream(path);
                OggFile ogg = new OggFile(input);
                VorbisFile vorbis = new VorbisFile(ogg)) {
            long sampleRate = vorbis.getInfo().getRate();
            if (sampleRate <= 0 || vorbis.getInfo().getChannels() <= 0) {
                throw malformed("OGG/Vorbis header is invalid", null);
            }

            long lastGranule = 0;
            VorbisAudioData packet;
            while ((packet = vorbis.getNextAudioPacket()) != null) {
                lastGranule = Math.max(lastGranule, packet.getGranulePosition());
                if (lastGranule * 1000L > maxDuration.toMillis() * sampleRate) {
                    throw new AudioValidationException(
                            AudioValidationException.Reason.DURATION_LIMIT,
                            "audio duration exceeds the configured limit");
                }
            }
            return requireDuration(lastGranule * 1000L / sampleRate, maxDuration);
        } catch (AudioValidationException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw malformed("malformed OGG/Vorbis stream", exception);
        }
    }

    private static long requireDuration(long durationMillis, Duration maximum) throws AudioValidationException {
        if (durationMillis <= 0) {
            throw malformed("audio duration is zero", null);
        }
        if (durationMillis > maximum.toMillis()) {
            throw new AudioValidationException(
                    AudioValidationException.Reason.DURATION_LIMIT,
                    "audio duration exceeds the configured limit");
        }
        return durationMillis;
    }

    private static String sha256(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }

        byte[] buffer = new byte[COPY_BUFFER_BYTES];
        try (InputStream input = Files.newInputStream(path)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static AudioValidationException malformed(String message, Throwable cause) {
        return cause == null
                ? new AudioValidationException(AudioValidationException.Reason.MALFORMED, message)
                : new AudioValidationException(AudioValidationException.Reason.MALFORMED, message, cause);
    }

}
