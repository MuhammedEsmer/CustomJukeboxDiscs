package dev.hoodoo.customjukeboxdiscs.content.writer;

import dev.hoodoo.customjukeboxdiscs.content.ModItems;
import dev.hoodoo.customjukeboxdiscs.content.disc.DiscVariant;
import dev.hoodoo.customjukeboxdiscs.content.disc.ProgrammedDiscItem;
import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import java.util.Objects;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class TileEntityDiscWriter extends TileEntity implements IInventory {
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public long inputFingerprint() {
        ItemStack stack = items.get(0);
        if (stack.isEmpty()) {
            return 0L;
        }
        long hash = Objects.hash(stack.getItem(), stack.getItemDamage(), stack.getTagCompound());
        return hash * 31L + stack.getCount();
    }

    public boolean writeDisc(long expectedFingerprint, TrackReference track) {
        ItemStack input = items.get(0);
        DiscWriterTransaction transaction = new DiscWriterTransaction(expectedFingerprint);
        boolean hasBlank = !input.isEmpty() && input.getItem() == ModItems.BLANK_DISC && input.getCount() == 1;
        if (!transaction.canComplete(inputFingerprint(), hasBlank, track)) {
            return false;
        }
        ItemStack output = new ItemStack(ModItems.PROGRAMMED_DISC);
        ProgrammedDiscItem.setTrackReference(output, track);
        ProgrammedDiscItem.setVariant(output, DiscVariant.random(world != null ? world.rand : new Random()));
        items.set(0, output);
        markDirty();
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
        return true;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.get(0).isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack result = ItemStackHelper.getAndSplit(items, index, count);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack result = ItemStackHelper.getAndRemove(items, index);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        items.set(index, stack);
        if (stack.getCount() > getInventoryStackLimit()) {
            stack.setCount(getInventoryStackLimit());
        }
        markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        if (this.world == null || this.world.getTileEntity(this.pos) != this) {
            return false;
        }
        return player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModItems.BLANK_DISC;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }

    @Override
    public String getName() {
        return "container." + ModItems.MOD_ID + ".disc_writer";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation(getName());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        items.clear();
        ItemStackHelper.loadAllItems(compound, items);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStackHelper.saveAllItems(compound, items);
        return compound;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
