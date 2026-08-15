package dev.muhammedesmer.customjukeboxdiscs.client.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.FloatSampleSource;
import org.junit.jupiter.api.Test;

final class MonoAudioStreamTest {
    @Test
    void reportsASingleChannelSoOpenAlCanPositionTheSound() throws IOException {
        MonoAudioStream stream = new MonoAudioStream(new FakeSource(2, 44_100F, List.of(new float[] {1F, 1F})));

        assertEquals(1, stream.getFormat().getChannels());
        assertEquals(44_100F, stream.getFormat().getSampleRate());
        stream.close();
    }

    @Test
    void averagesTheChannelsOfEveryFrame() throws IOException {
        MonoAudioStream stream = new MonoAudioStream(
                new FakeSource(2, 44_100F, List.of(new float[] {1.0F, 0.0F, 0.5F, -0.5F})));

        assertEquals(List.of(0.5F, 0.0F), collect(stream));
        stream.close();
    }

    @Test
    void carriesAnIncompleteFrameIntoTheNextChunk() throws IOException {
        MonoAudioStream stream = new MonoAudioStream(new FakeSource(
                2, 44_100F, List.of(new float[] {1.0F}, new float[] {0.0F, 0.25F, 0.75F})));

        assertEquals(List.of(), collect(stream), "half a frame cannot produce a sample yet");
        assertEquals(List.of(0.5F, 0.5F), collect(stream), "the carried sample completes the first frame");
        stream.close();
    }

    @Test
    void leavesAMonoSourceUntouched() throws IOException {
        MonoAudioStream stream = new MonoAudioStream(
                new FakeSource(1, 22_050F, List.of(new float[] {0.25F, -0.75F})));

        assertEquals(1, stream.getFormat().getChannels());
        assertEquals(22_050F, stream.getFormat().getSampleRate());
        assertEquals(List.of(0.25F, -0.75F), collect(stream));
        stream.close();
    }

    @Test
    void reportsTheEndOfTheDelegate() throws IOException {
        MonoAudioStream stream = new MonoAudioStream(new FakeSource(2, 44_100F, List.of(new float[] {1F, 1F})));

        assertTrue(stream.readChunk(value -> { }));
        assertFalse(stream.readChunk(value -> { }));
        stream.close();
    }

    @Test
    void closingClosesTheDelegate() throws IOException {
        FakeSource delegate = new FakeSource(2, 44_100F, List.of(new float[] {1F, 1F}));
        MonoAudioStream stream = new MonoAudioStream(delegate);

        stream.close();

        assertTrue(delegate.closed);
    }

    private static List<Float> collect(MonoAudioStream stream) throws IOException {
        FloatArrayList collected = new FloatArrayList();
        stream.readChunk(collected::add);
        return collected.stream().toList();
    }

    private static final class FakeSource implements FloatSampleSource {
        private final AudioFormat format;
        private final Deque<float[]> chunks = new ArrayDeque<>();
        private boolean closed;

        private FakeSource(int channels, float sampleRate, List<float[]> chunks) {
            this.format = new AudioFormat(sampleRate, 16, channels, true, false);
            this.chunks.addAll(chunks);
        }

        @Override
        public AudioFormat getFormat() {
            return format;
        }

        @Override
        public boolean readChunk(FloatConsumer output) {
            float[] chunk = chunks.pollFirst();
            if (chunk == null) {
                return false;
            }
            for (float value : chunk) {
                output.accept(value);
            }
            return true;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
