package dev.hoodoo.customjukeboxdiscs.permission;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

public final class AccessPolicySavedData extends WorldSavedData {
    public static final String DATA_NAME = "customjukeboxdiscs_access";

    private AccessMode mode = AccessMode.OPS;
    private final Set<UUID> allowedPlayers = new HashSet<>();
    private final Set<UUID> deniedPlayers = new HashSet<>();

    public AccessPolicySavedData() {
        super(DATA_NAME);
    }

    public AccessPolicySavedData(String name) {
        super(name);
    }

    public AccessMode mode() {
        return mode;
    }

    public Set<UUID> allowedPlayers() {
        return Collections.unmodifiableSet(allowedPlayers);
    }

    public Set<UUID> deniedPlayers() {
        return Collections.unmodifiableSet(deniedPlayers);
    }

    public void setMode(AccessMode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            markDirty();
        }
    }

    public void allow(UUID playerId) {
        boolean changed = deniedPlayers.remove(playerId);
        changed |= allowedPlayers.add(playerId);
        if (changed) {
            markDirty();
        }
    }

    public void deny(UUID playerId) {
        boolean changed = allowedPlayers.remove(playerId);
        changed |= deniedPlayers.add(playerId);
        if (changed) {
            markDirty();
        }
    }

    public void remove(UUID playerId) {
        boolean changed = allowedPlayers.remove(playerId);
        changed |= deniedPlayers.remove(playerId);
        if (changed) {
            markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        allowedPlayers.clear();
        deniedPlayers.clear();
        if (nbt.hasKey("mode")) {
            mode = AccessMode.fromSerializedName(nbt.getString("mode"));
        } else {
            mode = AccessMode.OPS;
        }

        if (nbt.hasKey("allowed_players", Constants.NBT.TAG_LIST)) {
            NBTTagList list = nbt.getTagList("allowed_players", Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) {
                try {
                    allowedPlayers.add(UUID.fromString(list.getStringTagAt(i)));
                } catch (Exception ignored) {
                }
            }
        }

        if (nbt.hasKey("denied_players", Constants.NBT.TAG_LIST)) {
            NBTTagList list = nbt.getTagList("denied_players", Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) {
                try {
                    deniedPlayers.add(UUID.fromString(list.getStringTagAt(i)));
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setString("mode", mode.serializedName());

        NBTTagList allowedList = new NBTTagList();
        for (UUID uuid : allowedPlayers) {
            allowedList.appendTag(new NBTTagString(uuid.toString()));
        }
        compound.setTag("allowed_players", allowedList);

        NBTTagList deniedList = new NBTTagList();
        for (UUID uuid : deniedPlayers) {
            deniedList.appendTag(new NBTTagString(uuid.toString()));
        }
        compound.setTag("denied_players", deniedList);

        return compound;
    }
}
