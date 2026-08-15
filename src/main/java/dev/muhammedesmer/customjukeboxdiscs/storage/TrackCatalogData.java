package dev.muhammedesmer.customjukeboxdiscs.storage;

import com.mojang.serialization.Codec;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.saveddata.SavedData;

public final class TrackCatalogData extends SavedData {
    public static final Codec<TrackCatalogData> CODEC = Codec.unboundedMap(Codec.STRING, TrackMetadata.CODEC)
            .xmap(TrackCatalogData::new, TrackCatalogData::tracks);

    private final Map<String, TrackMetadata> tracks;

    public TrackCatalogData() {
        this(Map.of());
    }

    private TrackCatalogData(Map<String, TrackMetadata> tracks) {
        this.tracks = new HashMap<>();
        tracks.forEach((hash, metadata) -> {
            if (!hash.equals(metadata.reference().sha256())) {
                throw new IllegalArgumentException("catalog key does not match track hash");
            }
            this.tracks.put(hash, metadata);
        });
    }

    public boolean add(TrackMetadata metadata) {
        String hash = metadata.reference().sha256();
        if (tracks.containsKey(hash)) {
            return false;
        }
        tracks.put(hash, metadata);
        setDirty();
        return true;
    }

    public Optional<TrackMetadata> find(String sha256) {
        return Optional.ofNullable(tracks.get(sha256));
    }

    public Optional<TrackMetadata> remove(String sha256) {
        TrackMetadata removed = tracks.remove(sha256);
        if (removed != null) {
            setDirty();
        }
        return Optional.ofNullable(removed);
    }

    public Map<String, TrackMetadata> tracks() {
        return Map.copyOf(tracks);
    }

    public CatalogPage page(int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("page and pageSize must be positive");
        }
        List<TrackMetadata> ordered = tracks.values().stream()
                .sorted(Comparator.comparing(TrackMetadata::createdAt)
                        .thenComparing(metadata -> metadata.reference().sha256()))
                .toList();
        int pageCount = Math.max(1, (ordered.size() + pageSize - 1) / pageSize);
        int from = Math.min((page - 1) * pageSize, ordered.size());
        int to = Math.min(from + pageSize, ordered.size());
        return new CatalogPage(page, pageCount, ordered.size(), ordered.subList(from, to));
    }

    public record CatalogPage(int page, int pageCount, int totalTracks, List<TrackMetadata> entries) {
    }

    public long trackCount(UUID owner) {
        return tracks.values().stream()
                .filter(metadata -> metadata.reference().uploaderUuid().equals(owner))
                .count();
    }

    public long byteCount(UUID owner) {
        return tracks.values().stream()
                .filter(metadata -> metadata.reference().uploaderUuid().equals(owner))
                .mapToLong(TrackMetadata::byteCount)
                .sum();
    }

    public long totalByteCount() {
        return tracks.values().stream().mapToLong(TrackMetadata::byteCount).sum();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
    }

    public static TrackCatalogData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }
}
