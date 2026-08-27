package dev.hoodoo.customjukeboxdiscs.content.rack;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerDiscRack extends Container {
    private final IInventory rack;

    public ContainerDiscRack(InventoryPlayer playerInventory, IInventory rack) {
        this.rack = rack;
        rack.openInventory(playerInventory.player);

        for (int slot = 0; slot < RackSlots.SIZE; slot++) {
            addSlotToContainer(new Slot(rack, slot, 62 + RackSlots.column(slot) * 18, 17 + RackSlots.row(slot) * 18) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return BlockDiscRack.isDisc(stack);
                }

                @Override
                public int getSlotStackLimit() {
                    return 1;
                }
            });
        }

        // Player Inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public IInventory getRack() {
        return rack;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return rack.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            if (index < RackSlots.SIZE) {
                if (!mergeItemStack(stackInSlot, RackSlots.SIZE, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (BlockDiscRack.isDisc(stackInSlot)) {
                    if (!mergeItemStack(stackInSlot, 0, RackSlots.SIZE, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, stackInSlot);
        }

        return itemstack;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        rack.closeInventory(playerIn);
    }
}
