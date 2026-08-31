package dev.muhammedesmer.customjukeboxdiscs.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import java.time.Instant;
import java.util.Objects;

public record TrackMetadata(TrackReference reference, long byteCount, Instant createdAt) {
    public static final Codec<TrackMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TrackReference.CODEC.fieldOf("reference").forGetter(TrackMetadata::reference),
            Codec.LONG.fieldOf("byte_count").forGetter(TrackMetadata::byteCount),
            Codec.LONG.fieldOf("created_at_epoch_millis")
                    .xmap(Instant::ofEpochMilli, Instant::toEpochMilli)
                    .forGetter(TrackMetadata::createdAt)
    ).apply(instance, TrackMetadata::new));

    public TrackMetadata {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(createdAt, "createdAt");
        if (byteCount <= 0) {
            throw new IllegalArgumentException("byteCount must be positive");
        }
    }
}
