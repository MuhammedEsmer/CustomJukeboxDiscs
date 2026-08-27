package dev.hoodoo.customjukeboxdiscs.network;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.minecraft.util.math.BlockPos;

public final class PlaybackAnchor {
    public enum Kind { BLOCK, ENTITY }

    private final Kind kind;
    private final BlockPos pos;
    private final int entityId;

    public PlaybackAnchor(Kind kind, BlockPos pos, int entityId) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.pos = pos != null ? pos : BlockPos.ORIGIN;
        this.entityId = entityId;
    }

    public static PlaybackAnchor atBlock(BlockPos pos) {
        return new PlaybackAnchor(Kind.BLOCK, pos, 0);
    }

    public static PlaybackAnchor onEntity(int entityId) {
        return new PlaybackAnchor(Kind.ENTITY, BlockPos.ORIGIN, entityId);
    }

    public Kind getKind() {
        return kind;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getEntityId() {
        return entityId;
    }

    public boolean isEntity() {
        return kind == Kind.ENTITY;
    }

    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind.ordinal());
        if (kind == Kind.BLOCK) {
            buf.writeLong(pos.toLong());
        } else {
            buf.writeInt(entityId);
        }
    }

    public static PlaybackAnchor fromBytes(ByteBuf buf) {
        int ordinal = buf.readByte();
        if (ordinal == Kind.BLOCK.ordinal()) {
            return atBlock(BlockPos.fromLong(buf.readLong()));
        } else if (ordinal == Kind.ENTITY.ordinal()) {
            return onEntity(buf.readInt());
        }
        throw new IllegalArgumentException("Unsupported playback anchor kind: " + ordinal);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaybackAnchor)) return false;
        PlaybackAnchor that = (PlaybackAnchor) o;
        return entityId == that.entityId && kind == that.kind && Objects.equals(pos, that.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, pos, entityId);
    }

    @Override
    public String toString() {
        return "PlaybackAnchor{" +
                "kind=" + kind +
                ", pos=" + pos +
                ", entityId=" + entityId +
                '}';
    }
}
