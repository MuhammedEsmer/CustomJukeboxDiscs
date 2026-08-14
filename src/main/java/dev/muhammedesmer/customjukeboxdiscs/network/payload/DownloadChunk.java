package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import java.util.Arrays;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DownloadChunk(String sha256, long offset, byte[] bytes, boolean last) implements CustomPacketPayload {
    public static final int MAX_BYTES = 31 * 1024;
    public static final Type<DownloadChunk> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "download_chunk"));
    public static final StreamCodec<FriendlyByteBuf, DownloadChunk> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUtf(value.sha256, 64); buf.writeVarLong(value.offset); buf.writeByteArray(value.bytes); buf.writeBoolean(value.last); },
            buf -> new DownloadChunk(buf.readUtf(64), buf.readVarLong(), buf.readByteArray(MAX_BYTES), buf.readBoolean()));
    public DownloadChunk { PayloadValidation.requireHash(sha256); if (offset < 0 || bytes == null || bytes.length < 1 || bytes.length > MAX_BYTES) throw new IllegalArgumentException("invalid chunk"); bytes = Arrays.copyOf(bytes, bytes.length); }
    @Override public byte[] bytes() { return Arrays.copyOf(bytes, bytes.length); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
