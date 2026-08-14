package dev.muhammedesmer.customjukeboxdiscs.content.writer;

import dev.muhammedesmer.customjukeboxdiscs.content.ModBlocks;
import dev.muhammedesmer.customjukeboxdiscs.content.ModItems;
import dev.muhammedesmer.customjukeboxdiscs.content.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DiscWriterMenu extends AbstractContainerMenu {
    private final Container writer;

    public DiscWriterMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, (DiscWriterBlockEntity) inventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public DiscWriterMenu(int containerId, Inventory inventory, Container writer) {
        super(ModMenus.DISC_WRITER.get(), containerId);
        this.writer = writer;
        checkContainerSize(writer, 1);
        writer.startOpen(inventory.player);
        addSlot(new Slot(writer, 0, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.BLANK_DISC);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addPlayerInventory(inventory);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return empty;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index == 0 ? !moveItemStackTo(original, 1, 37, true)
                : !original.is(ModItems.BLANK_DISC) || !moveItemStackTo(original, 0, 1, false)) {
            return empty;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return writer.stillValid(player);
    }

    public long inputFingerprint() {
        return writer instanceof DiscWriterBlockEntity blockEntity ? blockEntity.inputFingerprint() : 0L;
    }

    public Container writer() {
        return writer;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        writer.stopOpen(player);
    }
}
