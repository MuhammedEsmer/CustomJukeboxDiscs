package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record JukeboxStop(BlockPos pos) implements CustomPacketPayload {
    public static final Type<JukeboxStop> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "jukebox_stop"));
    public static final StreamCodec<FriendlyByteBuf, JukeboxStop> STREAM_CODEC = StreamCodec.of((buf, value) -> buf.writeBlockPos(value.pos), buf -> new JukeboxStop(buf.readBlockPos()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
