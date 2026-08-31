package dev.hoodoo.customjukeboxdiscs.client.audio;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import paulscode.sound.ICodec;
import paulscode.sound.SoundBuffer;

public class CodecMP3 implements ICodec {
    private static final Logger LOGGER = LogManager.getLogger("CustomJukeboxDiscs-CodecMP3");

    private InputStream inputStream;
    private Bitstream bitstream;
    private Decoder decoder;
    private AudioFormat audioFormat;
    private boolean initialized = false;
    private boolean endOfStream = false;
    private byte[] pendingBytes = null;

    @Override
    public void reverseByteOrder(boolean b) {
    }

    @Override
    public boolean initialize(URL url) {
        cleanup();
        if (url == null) return false;
        try {
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            inputStream = new BufferedInputStream(connection.getInputStream(), 65536);
            bitstream = new Bitstream(inputStream);
            decoder = new Decoder();

            Header header = bitstream.readFrame();
            if (header == null) {
                LOGGER.error("MP3 stream contains no frames: {}", url);
                cleanup();
                return false;
            }

            SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            bitstream.closeFrame();

            int sampleRate = samples.getSampleFrequency();
            int channels = samples.getChannelCount();
            audioFormat = new AudioFormat(sampleRate, 16, channels, true, false);

            pendingBytes = sampleBufferToBytes(samples);
            initialized = true;
            endOfStream = false;
            skip(PlaybackOffsetRegistry.consume(url));
            LOGGER.info("Initialized MP3 stream: {} ({}Hz, {} channels)", url, sampleRate, channels);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize MP3 codec for URL: {}", url, e);
            cleanup();
            return false;
        }
    }

    @Override
    public boolean initialized() {
        return initialized;
    }

    @Override
    public SoundBuffer read() {
        if (!initialized || endOfStream) {
            return null;
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(16384);
            if (pendingBytes != null) {
                out.write(pendingBytes);
                pendingBytes = null;
            }

            int framesRead = 0;
            while (framesRead < 4 && out.size() < 16384) {
                Header header = bitstream.readFrame();
                if (header == null) {
                    endOfStream = true;
                    break;
                }
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                bitstream.closeFrame();
                if (samples != null) {
                    byte[] frameBytes = sampleBufferToBytes(samples);
                    out.write(frameBytes);
                    framesRead++;
                }
            }

            byte[] data = out.toByteArray();
            if (data.length == 0) {
                endOfStream = true;
                return null;
            }
            return new SoundBuffer(data, audioFormat);
        } catch (Exception e) {
            LOGGER.warn("Error reading MP3 frame", e);
            endOfStream = true;
            return null;
        }
    }

    @Override
    public SoundBuffer readAll() {
        if (!initialized) return null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (pendingBytes != null) {
                out.write(pendingBytes);
                pendingBytes = null;
            }

            while (!endOfStream) {
                Header header = bitstream.readFrame();
                if (header == null) {
                    endOfStream = true;
                    break;
                }
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                bitstream.closeFrame();
                if (samples != null) {
                    out.write(sampleBufferToBytes(samples));
                }
            }

            byte[] data = out.toByteArray();
            return new SoundBuffer(data, audioFormat);
        } catch (Exception e) {
            LOGGER.error("Error decoding entire MP3", e);
            endOfStream = true;
            return null;
        }
    }

    @Override
    public boolean endOfStream() {
        return endOfStream;
    }

    @Override
    public void cleanup() {
        if (bitstream != null) {
            try { bitstream.close(); } catch (Exception ignored) {}
            bitstream = null;
        }
        if (inputStream != null) {
            try { inputStream.close(); } catch (Exception ignored) {}
            inputStream = null;
        }
        decoder = null;
        pendingBytes = null;
        initialized = false;
        endOfStream = true;
    }

    @Override
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    private void skip(long elapsedMillis) {
        long bytes = (long) audioFormat.getFrameSize()
                * (long) audioFormat.getSampleRate() * elapsedMillis / 1_000L;
        while (bytes > 0L) {
            SoundBuffer buffer = read();
            if (buffer == null || buffer.audioData == null) {
                return;
            }
            if (bytes < buffer.audioData.length) {
                pendingBytes = Arrays.copyOfRange(buffer.audioData, (int) bytes, buffer.audioData.length);
                return;
            }
            bytes -= buffer.audioData.length;
        }
    }

    private static byte[] sampleBufferToBytes(SampleBuffer samples) {
        short[] buffer = samples.getBuffer();
        int length = samples.getBufferLength();
        byte[] bytes = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            short val = buffer[i];
            bytes[i * 2] = (byte) (val & 0xFF);
            bytes[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return bytes;
    }
}
