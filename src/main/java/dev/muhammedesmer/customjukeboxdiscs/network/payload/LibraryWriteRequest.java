package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import java.util.regex.Pattern;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LibraryWriteRequest(String sha256) implements CustomPacketPayload {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    public static final Type<LibraryWriteRequest> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "library_write_request"));
    public static final StreamCodec<FriendlyByteBuf, LibraryWriteRequest> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeUtf(value.sha256, 64),
            buffer -> new LibraryWriteRequest(buffer.readUtf(64)));

    public LibraryWriteRequest {
        if (sha256 == null || !SHA_256.matcher(sha256).matches()) {
            throw new IllegalArgumentException("invalid track hash");
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
