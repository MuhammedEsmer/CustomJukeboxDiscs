package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record JukeboxPlay(BlockPos pos, TrackReference track, long elapsedMillis) implements CustomPacketPayload {
    public static final Type<JukeboxPlay> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "jukebox_play"));
    public static final StreamCodec<FriendlyByteBuf, JukeboxPlay> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeBlockPos(value.pos); TrackReference.STREAM_CODEC.encode(buf, value.track); buf.writeVarLong(value.elapsedMillis); },
            buf -> new JukeboxPlay(buf.readBlockPos(), TrackReference.STREAM_CODEC.decode(buf), buf.readVarLong()));
    public JukeboxPlay { if (elapsedMillis < 0) throw new IllegalArgumentException("elapsedMillis cannot be negative"); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
