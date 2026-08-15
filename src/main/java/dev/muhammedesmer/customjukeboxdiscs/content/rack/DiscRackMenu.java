package dev.muhammedesmer.customjukeboxdiscs.content.rack;

import dev.muhammedesmer.customjukeboxdiscs.content.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DiscRackMenu extends AbstractContainerMenu {
    private final Container rack;

    public DiscRackMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, rackAt(inventory, data));
    }

    public DiscRackMenu(int containerId, Inventory inventory, Container rack) {
        super(ModMenus.DISC_RACK.get(), containerId);
        this.rack = rack;
        checkContainerSize(rack, RackSlots.SIZE);
        rack.startOpen(inventory.player);
        for (int slot = 0; slot < RackSlots.SIZE; slot++) {
            addSlot(new Slot(rack, slot, 62 + RackSlots.column(slot) * 18, 17 + RackSlots.row(slot) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return DiscRackBlock.isDisc(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    private static Container rackAt(Inventory inventory, FriendlyByteBuf data) {
        return inventory.player.level().getBlockEntity(data.readBlockPos()) instanceof DiscRackBlockEntity rack
                ? rack
                : new SimpleContainer(RackSlots.SIZE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < RackSlots.SIZE
                ? !moveItemStackTo(original, RackSlots.SIZE, slots.size(), true)
                : !moveItemStackTo(original, 0, RackSlots.SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return rack.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        rack.stopOpen(player);
    }
}
