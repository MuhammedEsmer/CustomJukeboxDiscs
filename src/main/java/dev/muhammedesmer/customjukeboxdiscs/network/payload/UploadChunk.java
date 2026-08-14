package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UploadChunk(UUID sessionId, long offset, byte[] bytes) implements CustomPacketPayload {
    public static final int MAX_BYTES = 31 * 1024;
    public static final Type<UploadChunk> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "upload_chunk"));
    public static final StreamCodec<FriendlyByteBuf, UploadChunk> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeUUID(value.sessionId);
                buffer.writeVarLong(value.offset);
                buffer.writeByteArray(value.bytes);
            },
            buffer -> new UploadChunk(buffer.readUUID(), buffer.readVarLong(), buffer.readByteArray(MAX_BYTES)));

    public UploadChunk {
        Objects.requireNonNull(sessionId, "sessionId");
        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
        }
        if (bytes == null || bytes.length < 1 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("chunk must contain 1-" + MAX_BYTES + " bytes");
        }
        bytes = Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public byte[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
