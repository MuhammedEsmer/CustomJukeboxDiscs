package dev.muhammedesmer.customjukeboxdiscs.client.audio;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.sounds.FloatSampleSource;

public final class Mp3PcmStream implements FloatSampleSource {
    private final InputStream input;
    private final Bitstream bitstream;
    private final Decoder decoder = new Decoder();
    private SampleBuffer pending;
    private final AudioFormat format;
    private boolean closed;

    public Mp3PcmStream(InputStream input) throws IOException {
        this.input = input;
        bitstream = new Bitstream(input);
        pending = decodeFrame();
        if (pending == null) throw new IOException("MP3 contains no audio frames");
        format = new AudioFormat(pending.getSampleFrequency(), 16, pending.getChannelCount(), true, false);
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public boolean readChunk(FloatConsumer output) throws IOException {
        if (closed) return false;
        SampleBuffer samples = pending != null ? pending : decodeFrame();
        pending = null;
        if (samples == null) return false;
        short[] buffer = samples.getBuffer();
        for (int index = 0; index < samples.getBufferLength(); index++) {
            output.accept(buffer[index] / 32768.0F);
        }
        return true;
    }

    private SampleBuffer decodeFrame() throws IOException {
        try {
            Header header = bitstream.readFrame();
            if (header == null) return null;
            SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            bitstream.closeFrame();
            return samples;
        } catch (BitstreamException | DecoderException exception) {
            throw new IOException("Could not decode MP3 frame", exception);
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        try { bitstream.close(); } catch (BitstreamException exception) { throw new IOException(exception); }
        finally { input.close(); }
    }
}
