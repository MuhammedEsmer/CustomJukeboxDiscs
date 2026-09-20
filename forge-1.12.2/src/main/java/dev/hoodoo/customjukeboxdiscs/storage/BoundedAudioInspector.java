package dev.hoodoo.customjukeboxdiscs.storage;

import dev.hoodoo.customjukeboxdiscs.content.disc.AudioFormat;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;

public final class BoundedAudioInspector implements AudioInspector {
    private static final int COPY_BUFFER_BYTES = 16 * 1024;

    private static final int[][] MP3_BITRATES = {
        {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448}, // V1, L1
        {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384},     // V1, L2
        {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320},     // V1, L3
        {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256},     // V2, L1
        {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160},          // V2, L2/L3
    };

    private static final int[][] MP3_SAMPLERATES = {
        {44100, 48000, 32000}, // MPEG 1
        {22050, 24000, 16000}, // MPEG 2
        {11025, 12000, 8000}   // MPEG 2.5
    };

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
        long durationMillis;
        switch (format) {
            case MP3:
                durationMillis = inspectMp3(path, maxDuration);
                break;
            case OGG:
                durationMillis = inspectOgg(path, maxDuration);
                break;
            default:
                throw new AudioValidationException(
                        AudioValidationException.Reason.UNSUPPORTED_FORMAT,
                        "unsupported format");
        }
        return new InspectionResult(format, sha256(path), byteCount, durationMillis);
    }

    private static AudioFormat detectFormat(Path path) throws IOException {
        byte[] buffer = new byte[8192];
        int read = 0;
        try (InputStream input = Files.newInputStream(path)) {
            read = input.read(buffer);
        }
        if (read <= 0) {
            throw new AudioValidationException(
                    AudioValidationException.Reason.MALFORMED, "audio file is empty");
        }

        // 1. Check Ogg signature at start
        if (read >= 4 && buffer[0] == 'O' && buffer[1] == 'g' && buffer[2] == 'g' && buffer[3] == 'S') {
            return AudioFormat.OGG;
        }

        // 2. Check ID3 tag at start
        if (read >= 3 && buffer[0] == 'I' && buffer[1] == 'D' && buffer[2] == '3') {
            return AudioFormat.MP3;
        }

        // 3. Scan first 8 KB for MPEG frame sync or OggS
        for (int i = 0; i < read - 3; i++) {
            if (buffer[i] == 'O' && buffer[i + 1] == 'g' && buffer[i + 2] == 'g' && buffer[i + 3] == 'S') {
                return AudioFormat.OGG;
            }
            if ((buffer[i] & 0xFF) == 0xFF && (buffer[i + 1] & 0xE0) == 0xE0) {
                int versionIdx = (buffer[i + 1] >> 3) & 0x03;
                int layerIdx = (buffer[i + 1] >> 1) & 0x03;
                int bitrateIdx = (buffer[i + 2] >> 4) & 0x0F;
                int sampleRateIdx = (buffer[i + 2] >> 2) & 0x03;
                if (versionIdx != 1 && layerIdx != 0 && bitrateIdx != 0 && bitrateIdx != 15 && sampleRateIdx != 3) {
                    return AudioFormat.MP3;
                }
            }
        }

        throw new AudioValidationException(
                AudioValidationException.Reason.UNSUPPORTED_FORMAT,
                "audio signature is unsupported");
    }

    private static long inspectMp3(Path path, Duration maxDuration) throws IOException {
        long fileSize = Files.size(path);
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            long id3Size = 0;
            byte[] id3Header = new byte[10];
            if (fileSize >= 10) {
                raf.readFully(id3Header);
                if (id3Header[0] == 'I' && id3Header[1] == 'D' && id3Header[2] == '3') {
                    int tagSize = ((id3Header[6] & 0x7F) << 21) |
                                  ((id3Header[7] & 0x7F) << 14) |
                                  ((id3Header[8] & 0x7F) << 7) |
                                  (id3Header[9] & 0x7F);
                    id3Size = 10L + tagSize;
                }
            }

            long startOffset = Math.min(id3Size, fileSize);
            raf.seek(startOffset);

            int scanLength = (int) Math.min(65536L, fileSize - startOffset);
            if (scanLength <= 4) {
                throw malformed("MP3 file too short", null);
            }

            byte[] scanBuffer = new byte[scanLength];
            raf.readFully(scanBuffer);

            int headerOffset = -1;
            int versionIdx = 0, layerIdx = 0, bitrateKbps = 0, sampleRate = 0, samplesPerFrame = 1152;
            boolean isStereo = true;

            for (int i = 0; i < scanLength - 4; i++) {
                if ((scanBuffer[i] & 0xFF) == 0xFF && (scanBuffer[i + 1] & 0xE0) == 0xE0) {
                    int v = (scanBuffer[i + 1] >> 3) & 0x03; // 0=2.5, 2=2, 3=1
                    int l = (scanBuffer[i + 1] >> 1) & 0x03; // 1=L3, 2=L2, 3=L1
                    if (v == 1 || l == 0) continue;

                    int brIdx = (scanBuffer[i + 2] >> 4) & 0x0F;
                    int srIdx = (scanBuffer[i + 2] >> 2) & 0x03;
                    int channelMode = (scanBuffer[i + 3] >> 6) & 0x03; // 3 = mono

                    if (brIdx == 0 || brIdx == 15 || srIdx == 3) continue;

                    int srTableIdx = v == 3 ? 0 : (v == 2 ? 1 : 2);
                    sampleRate = MP3_SAMPLERATES[srTableIdx][srIdx];

                    int brTableIdx;
                    if (v == 3) {
                        brTableIdx = l == 3 ? 0 : (l == 2 ? 1 : 2);
                    } else {
                        brTableIdx = l == 3 ? 3 : 4;
                    }
                    bitrateKbps = MP3_BITRATES[brTableIdx][brIdx];

                    if (l == 3) samplesPerFrame = 384;
                    else if (l == 2) samplesPerFrame = 1152;
                    else samplesPerFrame = (v == 3) ? 1152 : 576;

                    versionIdx = v;
                    layerIdx = l;
                    isStereo = (channelMode != 3);
                    headerOffset = i;
                    break;
                }
            }

            if (headerOffset == -1 || sampleRate <= 0 || bitrateKbps <= 0) {
                // Fallback: estimate from file size assuming 128 kbps
                long estimatedMs = (fileSize * 8000L) / (128 * 1000L);
                return requireDuration(estimatedMs, maxDuration);
            }

            // Check for VBR Xing / Info header in the first frame
            int xingOffset;
            if (versionIdx == 3) { // MPEG 1
                xingOffset = headerOffset + 4 + (isStereo ? 32 : 17);
            } else { // MPEG 2 / 2.5
                xingOffset = headerOffset + 4 + (isStereo ? 17 : 9);
            }

            long totalDurationMs = -1;
            if (xingOffset + 12 <= scanLength) {
                boolean isXing = scanBuffer[xingOffset] == 'X' && scanBuffer[xingOffset + 1] == 'i'
                        && scanBuffer[xingOffset + 2] == 'n' && scanBuffer[xingOffset + 3] == 'g';
                boolean isInfo = scanBuffer[xingOffset] == 'I' && scanBuffer[xingOffset + 1] == 'n'
                        && scanBuffer[xingOffset + 2] == 'f' && scanBuffer[xingOffset + 3] == 'o';

                if (isXing || isInfo) {
                    int flags = ((scanBuffer[xingOffset + 4] & 0xFF) << 24) |
                                ((scanBuffer[xingOffset + 5] & 0xFF) << 16) |
                                ((scanBuffer[xingOffset + 6] & 0xFF) << 8) |
                                (scanBuffer[xingOffset + 7] & 0xFF);
                    if ((flags & 1) != 0) { // Frames field is present
                        long frames = ((scanBuffer[xingOffset + 8] & 0xFFL) << 24) |
                                      ((scanBuffer[xingOffset + 9] & 0xFFL) << 16) |
                                      ((scanBuffer[xingOffset + 10] & 0xFFL) << 8) |
                                      (scanBuffer[xingOffset + 11] & 0xFFL);
                        if (frames > 0) {
                            totalDurationMs = (frames * samplesPerFrame * 1000L) / sampleRate;
                        }
                    }
                }
            }

            if (totalDurationMs <= 0) {
                // Constant Bitrate (CBR) calculation
                long audioBytes = Math.max(1L, fileSize - id3Size);
                totalDurationMs = (audioBytes * 8000L) / ((long) bitrateKbps * 1000L);
            }

            return requireDuration(totalDurationMs, maxDuration);
        } catch (AudioValidationException e) {
            throw e;
        } catch (Exception e) {
            throw malformed("malformed MP3 stream", e);
        }
    }

    private static long inspectOgg(Path path, Duration maxDuration) throws IOException {
        File file = path.toFile();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            if (length < 28) {
                throw malformed("OGG file too short", null);
            }

            // Read first page to extract sample rate from Vorbis header
            raf.seek(0);
            byte[] pageHeader = new byte[27];
            raf.readFully(pageHeader);
            if (pageHeader[0] != 'O' || pageHeader[1] != 'g' || pageHeader[2] != 'g' || pageHeader[3] != 'S') {
                throw malformed("OGG signature not found", null);
            }

            int pageSegments = pageHeader[26] & 0xFF;
            byte[] segmentTable = new byte[pageSegments];
            raf.readFully(segmentTable);

            // Vorbis Identification Header
            byte[] vorbisHeader = new byte[16];
            raf.readFully(vorbisHeader);
            if (vorbisHeader[0] != 1 || vorbisHeader[1] != 'v' || vorbisHeader[2] != 'o' ||
                vorbisHeader[3] != 'r' || vorbisHeader[4] != 'b' || vorbisHeader[5] != 'i' || vorbisHeader[6] != 's') {
                throw malformed("Vorbis header not found in OGG stream", null);
            }

            int channels = vorbisHeader[11] & 0xFF;
            long sampleRate = (vorbisHeader[12] & 0xFFL) |
                              ((vorbisHeader[13] & 0xFFL) << 8) |
                              ((vorbisHeader[14] & 0xFFL) << 16) |
                              ((vorbisHeader[15] & 0xFFL) << 24);

            if (sampleRate <= 0 || channels <= 0) {
                throw malformed("Invalid Vorbis sample rate or channels", null);
            }

            // Search for last OggS page to get total PCM samples
            long lastGranule = findLastOggGranule(raf, length);
            if (lastGranule <= 0) {
                throw malformed("Could not determine OGG duration", null);
            }

            long durationMillis = (lastGranule * 1000L) / sampleRate;
            return requireDuration(durationMillis, maxDuration);
        } catch (AudioValidationException e) {
            throw e;
        } catch (Exception e) {
            throw malformed("malformed OGG/Vorbis stream", e);
        }
    }

    private static long findLastOggGranule(RandomAccessFile raf, long fileLength) throws IOException {
        long searchBytes = Math.min(fileLength, 65536L);
        long searchStart = fileLength - searchBytes;
        raf.seek(searchStart);
        byte[] buffer = new byte[(int) searchBytes];
        raf.readFully(buffer);

        for (int i = buffer.length - 27; i >= 0; i--) {
            if (buffer[i] == 'O' && buffer[i + 1] == 'g' && buffer[i + 2] == 'g' && buffer[i + 3] == 'S') {
                long granule = (buffer[i + 6] & 0xFFL) |
                               ((buffer[i + 7] & 0xFFL) << 8) |
                               ((buffer[i + 8] & 0xFFL) << 16) |
                               ((buffer[i + 9] & 0xFFL) << 24) |
                               ((buffer[i + 10] & 0xFFL) << 32) |
                               ((buffer[i + 11] & 0xFFL) << 40) |
                               ((buffer[i + 12] & 0xFFL) << 48) |
                               ((buffer[i + 13] & 0xFFL) << 56);
                if (granule > 0) {
                    return granule;
                }
            }
        }
        return -1;
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
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    private static AudioValidationException malformed(String message, Throwable cause) {
        return cause == null
                ? new AudioValidationException(AudioValidationException.Reason.MALFORMED, message)
                : new AudioValidationException(AudioValidationException.Reason.MALFORMED, message, cause);
    }
}
