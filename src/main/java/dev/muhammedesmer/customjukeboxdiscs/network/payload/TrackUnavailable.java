package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TrackUnavailable(String sha256) implements CustomPacketPayload {
    public static final Type<TrackUnavailable> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "track_unavailable"));
    public static final StreamCodec<FriendlyByteBuf, TrackUnavailable> STREAM_CODEC = StreamCodec.of((buf, value) -> buf.writeUtf(value.sha256, 64), buf -> new TrackUnavailable(buf.readUtf(64)));
    public TrackUnavailable { PayloadValidation.requireHash(sha256); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
