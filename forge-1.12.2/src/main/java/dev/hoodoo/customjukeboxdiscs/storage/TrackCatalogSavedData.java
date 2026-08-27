package dev.hoodoo.customjukeboxdiscs.storage;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

public final class TrackCatalogSavedData extends WorldSavedData {
    public static final String DATA_NAME = "customjukeboxdiscs_tracks";

    private final Map<String, TrackMetadata> tracks = new HashMap<>();

    public TrackCatalogSavedData() {
        super(DATA_NAME);
    }

    public TrackCatalogSavedData(String name) {
        super(name);
    }

    public boolean add(TrackMetadata metadata) {
        String hash = metadata.reference().sha256();
        if (tracks.containsKey(hash)) {
            return false;
        }
        tracks.put(hash, metadata);
        markDirty();
        return true;
    }

    public Optional<TrackMetadata> find(String sha256) {
        return Optional.ofNullable(tracks.get(sha256));
    }

    public Optional<TrackMetadata> remove(String sha256) {
        TrackMetadata removed = tracks.remove(sha256);
        if (removed != null) {
            markDirty();
        }
        return Optional.ofNullable(removed);
    }

    public Map<String, TrackMetadata> tracks() {
        return Collections.unmodifiableMap(tracks);
    }

    public CatalogPage page(int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("page and pageSize must be positive");
        }
        List<TrackMetadata> ordered = tracks.values().stream()
                .sorted(Comparator.comparing(TrackMetadata::createdAt)
                        .thenComparing(metadata -> metadata.reference().sha256()))
                .collect(Collectors.toList());
        int pageCount = Math.max(1, (ordered.size() + pageSize - 1) / pageSize);
        int from = Math.min((page - 1) * pageSize, ordered.size());
        int to = Math.min(from + pageSize, ordered.size());
        return new CatalogPage(page, pageCount, ordered.size(), ordered.subList(from, to));
    }

    public static final class CatalogPage {
        private final int page;
        private final int pageCount;
        private final int totalTracks;
        private final List<TrackMetadata> entries;

        public CatalogPage(int page, int pageCount, int totalTracks, List<TrackMetadata> entries) {
            this.page = page;
            this.pageCount = pageCount;
            this.totalTracks = totalTracks;
            this.entries = entries;
        }

        public int page() { return page; }
        public int getPage() { return page; }
        public int pageCount() { return pageCount; }
        public int getPageCount() { return pageCount; }
        public int totalTracks() { return totalTracks; }
        public int getTotalTracks() { return totalTracks; }
        public List<TrackMetadata> entries() { return entries; }
        public List<TrackMetadata> getEntries() { return entries; }
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
    public void readFromNBT(NBTTagCompound nbt) {
        tracks.clear();
        if (nbt.hasKey("tracks", Constants.NBT.TAG_LIST)) {
            NBTTagList list = nbt.getTagList("tracks", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                TrackMetadata metadata = TrackMetadata.fromNBT(tag);
                if (metadata != null && metadata.reference() != null) {
                    tracks.put(metadata.reference().sha256(), metadata);
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (TrackMetadata metadata : tracks.values()) {
            list.appendTag(metadata.toNBT());
        }
        compound.setTag("tracks", list);
        return compound;
    }
}
