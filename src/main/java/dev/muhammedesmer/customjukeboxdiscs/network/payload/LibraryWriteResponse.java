package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LibraryWriteResponse(Result result) implements CustomPacketPayload {
    public enum Result { WRITTEN, INVALID_WRITER, TRACK_UNAVAILABLE }

    public static final Type<LibraryWriteResponse> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "library_write_response"));
    public static final StreamCodec<FriendlyByteBuf, LibraryWriteResponse> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeVarInt(value.result.ordinal()),
            buffer -> {
                int ordinal = buffer.readVarInt();
                if (ordinal < 0 || ordinal >= Result.values().length) {
                    throw new IllegalArgumentException("invalid library write result");
                }
                return new LibraryWriteResponse(Result.values()[ordinal]);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
