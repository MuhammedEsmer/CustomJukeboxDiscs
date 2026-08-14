package dev.muhammedesmer.customjukeboxdiscs.content.writer;

import dev.muhammedesmer.customjukeboxdiscs.content.ModBlockEntities;
import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import dev.muhammedesmer.customjukeboxdiscs.content.ModItems;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DiscWriterBlockEntity extends BlockEntity implements Container, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public DiscWriterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISC_WRITER.get(), pos, state);
    }

    public long inputFingerprint() {
        ItemStack stack = items.getFirst();
        return ItemStack.hashItemAndComponents(stack) * 31L + stack.getCount();
    }

    public boolean writeDisc(long expectedFingerprint, TrackReference track) {
        ItemStack input = items.getFirst();
        DiscWriterTransaction transaction = new DiscWriterTransaction(expectedFingerprint);
        if (!transaction.canComplete(inputFingerprint(), input.is(ModItems.BLANK_DISC) && input.getCount() == 1, track)) {
            return false;
        }
        ItemStack output = new ItemStack(ModItems.PROGRAMMED_DISC.get());
        output.set(ModDataComponents.TRACK_REFERENCE, track);
        items.set(0, output);
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.customjukeboxdiscs.disc_writer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DiscWriterMenu(containerId, inventory, this);
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return items.getFirst().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(items, slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); setChanged(); }
}
