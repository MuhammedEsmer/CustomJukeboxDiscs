package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Tells the server whether this player wants to hear custom discs at all. A player who says no is
 * never sent a play message, so their client never downloads a track.
 */
public record PlaybackPreference(boolean enabled) implements CustomPacketPayload {
    public static final Type<PlaybackPreference> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "playback_preference"));
    public static final StreamCodec<FriendlyByteBuf, PlaybackPreference> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeBoolean(value.enabled),
            buffer -> new PlaybackPreference(buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
