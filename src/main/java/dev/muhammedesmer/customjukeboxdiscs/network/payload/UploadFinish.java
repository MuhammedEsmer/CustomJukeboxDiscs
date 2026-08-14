package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UploadFinish(UUID sessionId, String clientHash) implements CustomPacketPayload {
    public static final Type<UploadFinish> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "upload_finish"));
    public static final StreamCodec<FriendlyByteBuf, UploadFinish> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeUUID(value.sessionId);
                buffer.writeUtf(value.clientHash, 64);
            },
            buffer -> new UploadFinish(buffer.readUUID(), buffer.readUtf(64)));

    public UploadFinish {
        Objects.requireNonNull(sessionId, "sessionId");
        PayloadValidation.requireHash(clientHash);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
