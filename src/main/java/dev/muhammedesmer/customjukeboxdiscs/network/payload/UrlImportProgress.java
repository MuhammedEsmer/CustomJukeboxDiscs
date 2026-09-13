package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UrlImportProgress(Stage stage, int percent, String suggestedTitle) implements CustomPacketPayload {
    public enum Stage { RESOLVING, DOWNLOADING, WRITING }

    public static final Type<UrlImportProgress> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "url_import_progress"));
    public static final StreamCodec<FriendlyByteBuf, UrlImportProgress> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.stage.ordinal());
                buffer.writeVarInt(value.percent + 1);
                buffer.writeUtf(value.suggestedTitle, TrackReference.MAX_TITLE_CODE_POINTS * 4);
            },
            buffer -> {
                int ordinal = buffer.readVarInt();
                if (ordinal < 0 || ordinal >= Stage.values().length) {
                    throw new IllegalArgumentException("invalid link import stage");
                }
                return new UrlImportProgress(
                        Stage.values()[ordinal], buffer.readVarInt() - 1,
                        buffer.readUtf(TrackReference.MAX_TITLE_CODE_POINTS * 4));
            });

    public UrlImportProgress {
        if (stage == null) throw new NullPointerException("stage");
        suggestedTitle = suggestedTitle == null ? "" : suggestedTitle;
        if (percent < -1 || percent > 100) throw new IllegalArgumentException("percent must be -1-100");
        PayloadValidation.requireCodePoints(suggestedTitle, TrackReference.MAX_TITLE_CODE_POINTS, "suggestedTitle");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
