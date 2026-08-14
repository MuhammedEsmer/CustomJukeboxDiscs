package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TrackBegin(String sha256, long size, AudioFormat format) implements CustomPacketPayload {
    public static final Type<TrackBegin> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "track_begin"));
    public static final StreamCodec<FriendlyByteBuf, TrackBegin> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUtf(value.sha256, 64); buf.writeVarLong(value.size); buf.writeUtf(value.format.serializedName(), 3); },
            buf -> new TrackBegin(buf.readUtf(64), buf.readVarLong(), AudioFormat.fromSerializedName(buf.readUtf(3))));
    public TrackBegin { PayloadValidation.requireHash(sha256); if (size <= 0) throw new IllegalArgumentException("size must be positive"); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
