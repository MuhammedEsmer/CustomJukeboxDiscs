package dev.hoodoo.customjukeboxdiscs.content.writer;

import dev.hoodoo.customjukeboxdiscs.content.ModItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerDiscWriter extends Container {
    private final TileEntityDiscWriter writer;

    public ContainerDiscWriter(InventoryPlayer playerInventory, TileEntityDiscWriter writer) {
        this.writer = writer;
        writer.openInventory(playerInventory.player);

        addSlotToContainer(new Slot(writer, 0, 167, 25) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return !stack.isEmpty() && stack.getItem() == ModItems.BLANK_DISC;
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 172 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 230));
        }
    }

    public TileEntityDiscWriter getWriter() {
        return writer;
    }

    public long inputFingerprint() {
        return writer != null ? writer.inputFingerprint() : 0L;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return writer != null && writer.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            if (index == 0) {
                if (!mergeItemStack(stackInSlot, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stackInSlot.getItem() == ModItems.BLANK_DISC) {
                    if (!mergeItemStack(stackInSlot, 0, 1, false)) {
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
        if (writer != null) {
            writer.closeInventory(playerIn);
        }
    }
}
