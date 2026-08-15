package dev.muhammedesmer.customjukeboxdiscs.network.payload;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Where a track is playing: a fixed block, or an entity the sound travels with.
 */
public record PlaybackAnchor(Kind kind, BlockPos pos, int entityId) {
    public enum Kind { BLOCK, ENTITY }

    public static final StreamCodec<FriendlyByteBuf, PlaybackAnchor> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeByte(value.kind.ordinal());
                if (value.kind == Kind.BLOCK) {
                    buffer.writeBlockPos(value.pos);
                } else {
                    buffer.writeVarInt(value.entityId);
                }
            },
            buffer -> {
                int ordinal = buffer.readByte();
                if (ordinal == Kind.BLOCK.ordinal()) {
                    return atBlock(buffer.readBlockPos());
                }
                if (ordinal == Kind.ENTITY.ordinal()) {
                    return onEntity(buffer.readVarInt());
                }
                throw new IllegalArgumentException("Unsupported playback anchor kind: " + ordinal);
            });

    public PlaybackAnchor {
        Objects.requireNonNull(kind, "kind");
        if (kind == Kind.BLOCK) {
            Objects.requireNonNull(pos, "pos");
        }
    }

    public static PlaybackAnchor atBlock(BlockPos pos) {
        return new PlaybackAnchor(Kind.BLOCK, pos.immutable(), 0);
    }

    public static PlaybackAnchor onEntity(int entityId) {
        return new PlaybackAnchor(Kind.ENTITY, BlockPos.ZERO, entityId);
    }

    public boolean isEntity() {
        return kind == Kind.ENTITY;
    }
}
