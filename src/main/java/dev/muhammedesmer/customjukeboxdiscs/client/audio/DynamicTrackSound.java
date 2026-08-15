package dev.muhammedesmer.customjukeboxdiscs.client.audio;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.ModSounds;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.PlaybackAnchor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

public final class DynamicTrackSound extends AbstractTickableSoundInstance {
    private final Path file;
    private final AudioFormat format;
    private final TrackTimeline timeline;
    private final PlaybackAnchor anchor;
    private final boolean carriedByListener;

    public DynamicTrackSound(PlaybackAnchor anchor, Path file, AudioFormat format, TrackTimeline timeline) {
        super(ModSounds.DYNAMIC_TRACK.get(), SoundSource.RECORDS, RandomSource.create());
        this.file = file;
        this.format = format;
        this.timeline = timeline;
        this.anchor = anchor;
        this.carriedByListener = anchor.isEntity() && isLocalPlayer(anchor.entityId());
        if (carriedByListener) {
            // A source sitting on the listener flips between the ears on the smallest sideways step,
            // so music the player carries themselves is played flat instead of positioned.
            attenuation = Attenuation.NONE;
            relative = true;
            x = 0.0;
            y = 0.0;
            z = 0.0;
            return;
        }
        moveToAnchor();
    }

    private static boolean isLocalPlayer(int entityId) {
        var player = Minecraft.getInstance().player;
        return player != null && player.getId() == entityId;
    }

    /** Keeps the sound on its carrier, so a backpack's music travels with the player. */
    private void moveToAnchor() {
        if (carriedByListener) {
            return;
        }
        if (!anchor.isEntity()) {
            BlockPos pos = anchor.pos();
            x = pos.getX() + 0.5;
            y = pos.getY() + 0.5;
            z = pos.getZ() + 0.5;
            return;
        }
        var level = Minecraft.getInstance().level;
        Entity carrier = level == null ? null : level.getEntity(anchor.entityId());
        if (carrier != null) {
            x = carrier.getX();
            y = carrier.getEyeY();
            z = carrier.getZ();
        }
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        // Same volume and attenuation distance a vanilla record uses, so the falloff curve and the
        // RECORDS slider behave identically: 4 x 16 gives the usual 64 block range.
        sound = new Sound(location, ConstantFloat.of(4.0F), ConstantFloat.of(1.0F), 1,
                Sound.Type.FILE, true, false, 16);
        return SoundManager.INTENTIONALLY_EMPTY_SOUND_EVENT;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AudioStream stream = new MonoAudioStream(format == AudioFormat.OGG
                        ? new JOrbisAudioStream(Files.newInputStream(file))
                        : new Mp3PcmStream(Files.newInputStream(file)));
                discardElapsed(stream, timeline.elapsedMillisAt(System.nanoTime()));
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
            // The buffers come from BufferUtils and are garbage collected; freeing them manually corrupts the heap.
            ByteBuffer skipped = stream.read((int) Math.min(32_768L, bytes));
            int count = skipped.remaining();
            if (count == 0) return;
            bytes -= count;
        }
    }

    @Override
    public void tick() {
        if (timeline.finishedAt(System.nanoTime())) {
            stop();
            return;
        }
        moveToAnchor();
    }
}
