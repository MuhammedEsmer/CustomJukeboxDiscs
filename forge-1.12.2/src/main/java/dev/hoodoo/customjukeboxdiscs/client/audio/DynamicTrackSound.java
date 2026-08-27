package dev.hoodoo.customjukeboxdiscs.client.audio;

import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import dev.hoodoo.customjukeboxdiscs.network.PlaybackAnchor;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DynamicTrackSound {
    private static final Logger LOGGER = LogManager.getLogger("CustomJukeboxDiscs-Sound");
    private static Field sndManagerField;
    private static Field sndSystemField;

    private final PlaybackAnchor anchor;
    private final Path file;
    private final TrackReference track;
    private final TrackTimeline timeline;
    private final String sourceName;
    private final boolean carriedByListener;
    private boolean donePlaying;
    private boolean playing;

    public DynamicTrackSound(PlaybackAnchor anchor, Path file, TrackReference track, TrackTimeline timeline) {
        this.anchor = anchor;
        this.file = file;
        this.track = track;
        this.timeline = timeline;
        this.sourceName = "cjd_" + track.getSha256().substring(0, 16) + "_" + Math.abs(anchor.hashCode());
        this.carriedByListener = anchor.isEntity() && isLocalPlayer(anchor.getEntityId());
    }

    public void start() {
        SoundSystem sndSystem = getSoundSystem();
        if (sndSystem == null) {
            LOGGER.warn("SoundSystem is null, cannot play track: {}", track.getTitle());
            donePlaying = true;
            return;
        }

        try {
            SoundSystemConfig.setCodec("mp3", CodecMP3.class);
            File audioFile = file.toFile();
            URL url = audioFile.toURI().toURL();
            float[] pos = getPosition();

            LOGGER.info("Starting audio stream for '{}' (file: {}, pos: {},{},{})",
                    track.getTitle(), audioFile.getName(), pos[0], pos[1], pos[2]);

            if (carriedByListener) {
                sndSystem.newStreamingSource(false, sourceName, url, audioFile.getName(), false, 0.0F, 0.0F, 0.0F, SoundSystemConfig.ATTENUATION_NONE, 0.0F);
            } else {
                sndSystem.newStreamingSource(false, sourceName, url, audioFile.getName(), false, pos[0], pos[1], pos[2], SoundSystemConfig.ATTENUATION_LINEAR, 64.0F);
            }

            float volume = getVolume();
            sndSystem.setVolume(sourceName, volume);
            sndSystem.play(sourceName);
            playing = true;
        } catch (Exception e) {
            LOGGER.error("Failed to start audio playback for '{}'", track.getTitle(), e);
            donePlaying = true;
        }
    }

    public void update() {
        if (donePlaying) return;

        if (timeline.finishedAt(System.nanoTime())) {
            stop();
            return;
        }

        SoundSystem sndSystem = getSoundSystem();
        if (sndSystem == null) {
            donePlaying = true;
            return;
        }

        if (!carriedByListener) {
            float[] pos = getPosition();
            sndSystem.setPosition(sourceName, pos[0], pos[1], pos[2]);
        }
        sndSystem.setVolume(sourceName, getVolume());
    }

    public void stop() {
        if (playing) {
            SoundSystem sndSystem = getSoundSystem();
            if (sndSystem != null) {
                try {
                    sndSystem.stop(sourceName);
                    sndSystem.removeSource(sourceName);
                } catch (Exception ignored) {
                }
            }
            playing = false;
        }
        donePlaying = true;
    }

    public boolean isDonePlaying() {
        return donePlaying;
    }

    private float getVolume() {
        Minecraft mc = Minecraft.getMinecraft();
        float master = mc.gameSettings.getSoundLevel(SoundCategory.MASTER);
        float records = mc.gameSettings.getSoundLevel(SoundCategory.RECORDS);
        return master * records;
    }

    private float[] getPosition() {
        if (carriedByListener) {
            return new float[]{0.0F, 0.0F, 0.0F};
        }
        if (!anchor.isEntity()) {
            BlockPos pos = anchor.getPos();
            return new float[]{(float) pos.getX() + 0.5F, (float) pos.getY() + 0.5F, (float) pos.getZ() + 0.5F};
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null) {
            Entity carrier = mc.world.getEntityByID(anchor.getEntityId());
            if (carrier != null) {
                return new float[]{(float) carrier.posX, (float) carrier.posY + carrier.getEyeHeight(), (float) carrier.posZ};
            }
        }
        return new float[]{0.0F, 0.0F, 0.0F};
    }

    private static boolean isLocalPlayer(int entityId) {
        Entity player = Minecraft.getMinecraft().player;
        return player != null && player.getEntityId() == entityId;
    }

    private static SoundSystem getSoundSystem() {
        try {
            SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
            if (sndManagerField == null) {
                sndManagerField = ReflectionHelper.findField(SoundHandler.class, "sndManager", "field_147694_f");
                sndManagerField.setAccessible(true);
            }
            SoundManager manager = (SoundManager) sndManagerField.get(handler);
            if (sndSystemField == null) {
                sndSystemField = ReflectionHelper.findField(SoundManager.class, "sndSystem", "field_148620_e");
                sndSystemField.setAccessible(true);
            }
            return (SoundSystem) sndSystemField.get(manager);
        } catch (Exception e) {
            return null;
        }
    }
}
