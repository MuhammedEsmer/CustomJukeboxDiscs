package dev.hoodoo.customjukeboxdiscs.content;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public final class ModCreativeTabs {
    public static final CreativeTabs TAB = new CreativeTabs(ModItems.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModItems.BLANK_DISC);
        }
    };

    private ModCreativeTabs() {
    }
}
