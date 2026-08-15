package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record JukeboxStop(PlaybackAnchor anchor) implements CustomPacketPayload {
    public static final Type<JukeboxStop> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "jukebox_stop"));
    public static final StreamCodec<FriendlyByteBuf, JukeboxStop> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> PlaybackAnchor.STREAM_CODEC.encode(buf, value.anchor),
            buf -> new JukeboxStop(PlaybackAnchor.STREAM_CODEC.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
