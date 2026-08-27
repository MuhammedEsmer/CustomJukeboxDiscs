package dev.hoodoo.customjukeboxdiscs.storage;

import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import java.time.Instant;
import java.util.Objects;
import net.minecraft.nbt.NBTTagCompound;

public final class TrackMetadata {
    private final TrackReference reference;
    private final long byteCount;
    private final Instant createdAt;

    public TrackMetadata(TrackReference reference, long byteCount, Instant createdAt) {
        this.reference = Objects.requireNonNull(reference, "reference");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (byteCount <= 0) {
            throw new IllegalArgumentException("byteCount must be positive");
        }
        this.byteCount = byteCount;
    }

    public TrackReference reference() { return reference; }
    public TrackReference getReference() { return reference; }
    public long byteCount() { return byteCount; }
    public long getByteCount() { return byteCount; }
    public Instant createdAt() { return createdAt; }
    public Instant getCreatedAt() { return createdAt; }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("reference", reference.toNBT());
        tag.setLong("byte_count", byteCount);
        tag.setLong("created_at_epoch_millis", createdAt.toEpochMilli());
        return tag;
    }

    public static TrackMetadata fromNBT(NBTTagCompound tag) {
        TrackReference ref = TrackReference.fromNBT(tag.getCompoundTag("reference"));
        long byteCount = tag.getLong("byte_count");
        long createdAtEpoch = tag.getLong("created_at_epoch_millis");
        return new TrackMetadata(ref, byteCount, Instant.ofEpochMilli(createdAtEpoch));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrackMetadata)) return false;
        TrackMetadata that = (TrackMetadata) o;
        return byteCount == that.byteCount &&
                Objects.equals(reference, that.reference) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, byteCount, createdAt);
    }
}
