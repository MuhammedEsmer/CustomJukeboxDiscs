package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import dev.muhammedesmer.customjukeboxdiscs.transfer.UploadError;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UploadBeginResponse(
        Status status,
        UploadError error,
        UUID sessionId,
        int chunkBytes,
        TrackReference existingTrack) implements CustomPacketPayload {
    public static final Type<UploadBeginResponse> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "upload_begin_response"));
    public static final StreamCodec<FriendlyByteBuf, UploadBeginResponse> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.status.ordinal());
                buffer.writeVarInt(value.error.ordinal());
                buffer.writeBoolean(value.sessionId != null);
                if (value.sessionId != null) {
                    buffer.writeUUID(value.sessionId);
                }
                buffer.writeVarInt(value.chunkBytes);
                buffer.writeBoolean(value.existingTrack != null);
                if (value.existingTrack != null) {
                    TrackReference.STREAM_CODEC.encode(buffer, value.existingTrack);
                }
            },
            buffer -> new UploadBeginResponse(
                    enumValue(Status.values(), buffer.readVarInt(), "status"),
                    enumValue(UploadError.values(), buffer.readVarInt(), "error"),
                    buffer.readBoolean() ? buffer.readUUID() : null,
                    buffer.readVarInt(),
                    buffer.readBoolean() ? TrackReference.STREAM_CODEC.decode(buffer) : null));

    public UploadBeginResponse {
        switch (status) {
            case ACCEPTED -> {
                if (error != UploadError.NONE || sessionId == null || chunkBytes < 1 || existingTrack != null) {
                    throw new IllegalArgumentException("invalid accepted response");
                }
            }
            case ALREADY_PRESENT -> {
                if (error != UploadError.NONE || sessionId != null || chunkBytes != 0 || existingTrack == null) {
                    throw new IllegalArgumentException("invalid existing-track response");
                }
            }
            case REJECTED -> {
                if (error == UploadError.NONE || sessionId != null || chunkBytes != 0 || existingTrack != null) {
                    throw new IllegalArgumentException("invalid rejected response");
                }
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static <T> T enumValue(T[] values, int ordinal, String name) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("invalid " + name + " ordinal");
        }
        return values[ordinal];
    }

    public enum Status {
        ACCEPTED,
        ALREADY_PRESENT,
        REJECTED
    }
}
