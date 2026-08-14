package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import dev.muhammedesmer.customjukeboxdiscs.transfer.UploadError;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UploadResult(UploadError error, TrackReference track) implements CustomPacketPayload {
    public static final Type<UploadResult> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "upload_result"));
    public static final StreamCodec<FriendlyByteBuf, UploadResult> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.error.ordinal());
                buffer.writeBoolean(value.track != null);
                if (value.track != null) {
                    TrackReference.STREAM_CODEC.encode(buffer, value.track);
                }
            },
            buffer -> {
                int ordinal = buffer.readVarInt();
                if (ordinal < 0 || ordinal >= UploadError.values().length) {
                    throw new IllegalArgumentException("invalid upload error ordinal");
                }
                return new UploadResult(
                        UploadError.values()[ordinal],
                        buffer.readBoolean() ? TrackReference.STREAM_CODEC.decode(buffer) : null);
            });

    public UploadResult {
        if ((error == UploadError.NONE) != (track != null)) {
            throw new IllegalArgumentException("successful results require a track and failures forbid one");
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
