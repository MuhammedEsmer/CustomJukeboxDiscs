package dev.muhammedesmer.customjukeboxdiscs.content;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.ProgrammedDiscItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTRAR = DeferredRegister.createItems(CustomJukeboxDiscs.MOD_ID);

    public static final DeferredItem<Item> BLANK_DISC = REGISTRAR.registerSimpleItem(
            "blank_disc", new Item.Properties().stacksTo(16));
    public static final DeferredItem<ProgrammedDiscItem> PROGRAMMED_DISC = REGISTRAR.registerItem(
            "programmed_disc", ProgrammedDiscItem::new, new Item.Properties().stacksTo(1).component(
                    DataComponents.JUKEBOX_PLAYABLE,
                    new JukeboxPlayable(new EitherHolder<>(ResourceKey.create(
                            Registries.JUKEBOX_SONG,
                            ResourceLocation.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, "dynamic_track"))), false)));
    public static final DeferredItem<BlockItem> DISC_WRITER = REGISTRAR.registerSimpleBlockItem(ModBlocks.DISC_WRITER);
    public static final DeferredItem<BlockItem> DISC_RACK = REGISTRAR.registerSimpleBlockItem(ModBlocks.DISC_RACK);

    private ModItems() {
    }
}
