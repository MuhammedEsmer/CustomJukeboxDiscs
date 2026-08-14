package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UploadBeginRequest(String clientHash, long declaredBytes, AudioFormat formatHint, String title, long inputFingerprint)
        implements CustomPacketPayload {
    public static final Type<UploadBeginRequest> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "upload_begin"));

    public static final StreamCodec<FriendlyByteBuf, UploadBeginRequest> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeUtf(value.clientHash, 64);
                buffer.writeVarLong(value.declaredBytes);
                buffer.writeUtf(value.formatHint.serializedName(), 3);
                buffer.writeUtf(value.title, 256);
                buffer.writeLong(value.inputFingerprint);
            },
            buffer -> new UploadBeginRequest(
                    buffer.readUtf(64),
                    buffer.readVarLong(),
                    AudioFormat.fromSerializedName(buffer.readUtf(3)),
                    buffer.readUtf(256),
                    buffer.readLong()));

    public UploadBeginRequest {
        PayloadValidation.requireHash(clientHash);
        if (declaredBytes <= 0) {
            throw new IllegalArgumentException("declaredBytes must be positive");
        }
        PayloadValidation.requireCodePoints(title, 64, "title");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
