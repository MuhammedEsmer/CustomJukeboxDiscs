package dev.muhammedesmer.customjukeboxdiscs.permission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.saveddata.SavedData;

public final class AccessPolicyData extends SavedData {
    private static final Codec<Set<UUID>> UUID_SET_CODEC = UUIDUtil.CODEC.listOf()
            .xmap(HashSet::new, ArrayList::new);

    public static final Codec<AccessPolicyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AccessMode.CODEC.optionalFieldOf("mode", AccessMode.OPS).forGetter(AccessPolicyData::mode),
            UUID_SET_CODEC.optionalFieldOf("allowed_players", Set.of()).forGetter(AccessPolicyData::allowedPlayers),
            UUID_SET_CODEC.optionalFieldOf("denied_players", Set.of()).forGetter(AccessPolicyData::deniedPlayers)
    ).apply(instance, AccessPolicyData::new));

    private AccessMode mode;
    private final Set<UUID> allowedPlayers;
    private final Set<UUID> deniedPlayers;

    public AccessPolicyData() {
        this(AccessMode.OPS, Set.of(), Set.of());
    }

    private AccessPolicyData(AccessMode mode, Set<UUID> allowedPlayers, Set<UUID> deniedPlayers) {
        this.mode = mode;
        this.allowedPlayers = new HashSet<>(allowedPlayers);
        this.deniedPlayers = new HashSet<>(deniedPlayers);
    }

    public AccessMode mode() {
        return mode;
    }

    public Set<UUID> allowedPlayers() {
        return Set.copyOf(allowedPlayers);
    }

    public Set<UUID> deniedPlayers() {
        return Set.copyOf(deniedPlayers);
    }

    public void setMode(AccessMode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            setDirty();
        }
    }

    public void allow(UUID playerId) {
        boolean changed = deniedPlayers.remove(playerId);
        changed |= allowedPlayers.add(playerId);
        if (changed) {
            setDirty();
        }
    }

    public void deny(UUID playerId) {
        boolean changed = allowedPlayers.remove(playerId);
        changed |= deniedPlayers.add(playerId);
        if (changed) {
            setDirty();
        }
    }

    public void remove(UUID playerId) {
        boolean changed = allowedPlayers.remove(playerId);
        changed |= deniedPlayers.remove(playerId);
        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
    }

    public static AccessPolicyData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }
}
