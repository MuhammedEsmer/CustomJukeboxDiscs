package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LibraryPageRequest(int page) implements CustomPacketPayload {
    public static final Type<LibraryPageRequest> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "library_page_request"));
    public static final StreamCodec<FriendlyByteBuf, LibraryPageRequest> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeVarInt(value.page),
            buffer -> new LibraryPageRequest(buffer.readVarInt()));

    public LibraryPageRequest {
        if (page < 1) throw new IllegalArgumentException("page must be positive");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
