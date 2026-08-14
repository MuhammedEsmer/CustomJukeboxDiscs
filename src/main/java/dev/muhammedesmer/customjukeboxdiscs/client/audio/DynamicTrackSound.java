package dev.muhammedesmer.customjukeboxdiscs.client.audio;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.ModSounds;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

public final class DynamicTrackSound extends AbstractTickableSoundInstance {
    private final Path file;
    private final AudioFormat format;
    private final long stopAtNanos;
    private final long elapsedMillis;

    public DynamicTrackSound(BlockPos pos, Path file, AudioFormat format, long elapsedMillis, long remainingMillis) {
        super(ModSounds.DYNAMIC_TRACK.get(), SoundSource.RECORDS, RandomSource.create());
        this.file = file;
        this.format = format;
        this.elapsedMillis = elapsedMillis;
        x = pos.getX() + 0.5;
        y = pos.getY() + 0.5;
        z = pos.getZ() + 0.5;
        stopAtNanos = System.nanoTime() + Math.max(1, remainingMillis) * 1_000_000L;
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        sound = new Sound(location, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1,
                Sound.Type.FILE, true, false, 64);
        return SoundManager.INTENTIONALLY_EMPTY_SOUND_EVENT;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AudioStream stream = format == AudioFormat.OGG
                        ? new JOrbisAudioStream(Files.newInputStream(file))
                        : new Mp3PcmStream(Files.newInputStream(file));
                discardElapsed(stream, elapsedMillis);
                return stream;
            } catch (IOException exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        });
    }

    private static void discardElapsed(AudioStream stream, long elapsedMillis) throws IOException {
        long bytes = (long) stream.getFormat().getFrameSize()
                * (long) stream.getFormat().getSampleRate() * elapsedMillis / 1_000L;
        while (bytes > 0) {
            ByteBuffer skipped = stream.read((int) Math.min(32_768L, bytes));
            int count = skipped.remaining();
            org.lwjgl.system.MemoryUtil.memFree(skipped);
            if (count == 0) return;
            bytes -= count;
        }
    }

    @Override
    public void tick() {
        if (System.nanoTime() >= stopAtNanos) stop();
    }
}
