package dev.hoodoo.customjukeboxdiscs.content.disc;

import dev.hoodoo.customjukeboxdiscs.content.ModCreativeTabs;
import dev.hoodoo.customjukeboxdiscs.content.ModItems;
import net.minecraft.item.Item;

public class BlankDiscItem extends Item {
    public BlankDiscItem() {
        setRegistryName(ModItems.MOD_ID, "blank_disc");
        setTranslationKey(ModItems.MOD_ID + ".blank_disc");
        setMaxStackSize(16);
        setCreativeTab(ModCreativeTabs.TAB);
    }
}
