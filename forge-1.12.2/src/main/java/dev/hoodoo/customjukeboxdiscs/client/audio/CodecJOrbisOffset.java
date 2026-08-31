package dev.hoodoo.customjukeboxdiscs.client.audio;

import java.net.URL;
import java.util.Arrays;
import paulscode.sound.SoundBuffer;
import paulscode.sound.codecs.CodecJOrbis;

public final class CodecJOrbisOffset extends CodecJOrbis {
    private SoundBuffer pending;

    @Override
    public boolean initialize(URL url) {
        pending = null;
        if (!super.initialize(url)) {
            return false;
        }
        skip(PlaybackOffsetRegistry.consume(url));
        return true;
    }

    @Override
    public SoundBuffer read() {
        if (pending != null) {
            SoundBuffer result = pending;
            pending = null;
            return result;
        }
        return super.read();
    }

    private void skip(long elapsedMillis) {
        long bytes = bytesForMillis(getAudioFormat(), elapsedMillis);
        while (bytes > 0L) {
            SoundBuffer buffer = super.read();
            if (buffer == null || buffer.audioData == null) {
                return;
            }
            if (bytes < buffer.audioData.length) {
                pending = new SoundBuffer(
                        Arrays.copyOfRange(buffer.audioData, (int) bytes, buffer.audioData.length),
                        buffer.audioFormat);
                return;
            }
            bytes -= buffer.audioData.length;
        }
    }

    private static long bytesForMillis(javax.sound.sampled.AudioFormat format, long elapsedMillis) {
        return (long) format.getFrameSize() * (long) format.getSampleRate() * elapsedMillis / 1_000L;
    }
}
