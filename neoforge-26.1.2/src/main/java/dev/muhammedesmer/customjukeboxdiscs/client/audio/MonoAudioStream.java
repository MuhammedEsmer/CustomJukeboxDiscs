package dev.muhammedesmer.customjukeboxdiscs.client.audio;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.util.Objects;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.FloatSampleSource;

/**
 * Downmixes a decoded stream to one channel.
 *
 * <p>OpenAL only positions mono sources: a stereo buffer is played at a constant gain no matter where
 * the listener stands, which would leave a programmed disc audible everywhere at full volume.
 */
public final class MonoAudioStream implements FloatSampleSource {
    private final FloatSampleSource delegate;
    private final AudioFormat format;
    private final int sourceChannels;
    private float frameSum;
    private int frameSamples;

    public MonoAudioStream(FloatSampleSource delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        AudioFormat source = delegate.getFormat();
        this.sourceChannels = Math.max(1, source.getChannels());
        this.format = sourceChannels == 1
                ? source
                : new AudioFormat(source.getSampleRate(), 16, 1, true, false);
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public boolean readChunk(FloatConsumer output) throws IOException {
        if (sourceChannels == 1) {
            return delegate.readChunk(output);
        }
        return delegate.readChunk(sample -> {
            frameSum += sample;
            if (++frameSamples == sourceChannels) {
                output.accept(frameSum / sourceChannels);
                frameSum = 0F;
                frameSamples = 0;
            }
        });
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
