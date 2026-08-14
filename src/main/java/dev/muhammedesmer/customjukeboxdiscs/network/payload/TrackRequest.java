package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TrackRequest(String sha256) implements CustomPacketPayload {
    public static final Type<TrackRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "track_request"));
    public static final StreamCodec<FriendlyByteBuf, TrackRequest> STREAM_CODEC = StreamCodec.of((buf, value) -> buf.writeUtf(value.sha256, 64), buf -> new TrackRequest(buf.readUtf(64)));
    public TrackRequest { PayloadValidation.requireHash(sha256); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
