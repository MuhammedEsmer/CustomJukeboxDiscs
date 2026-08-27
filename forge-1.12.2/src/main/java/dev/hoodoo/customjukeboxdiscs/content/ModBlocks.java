package dev.hoodoo.customjukeboxdiscs.content;

import dev.hoodoo.customjukeboxdiscs.content.rack.BlockDiscRack;
import dev.hoodoo.customjukeboxdiscs.content.rack.TileEntityDiscRack;
import dev.hoodoo.customjukeboxdiscs.content.writer.BlockDiscWriter;
import dev.hoodoo.customjukeboxdiscs.content.writer.TileEntityDiscWriter;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = ModItems.MOD_ID)
public final class ModBlocks {
    public static final BlockDiscWriter DISC_WRITER = new BlockDiscWriter();
    public static final BlockDiscRack DISC_RACK = new BlockDiscRack();

    private ModBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(DISC_WRITER, DISC_RACK);
        GameRegistry.registerTileEntity(TileEntityDiscWriter.class, new ResourceLocation(ModItems.MOD_ID, "disc_writer"));
        GameRegistry.registerTileEntity(TileEntityDiscRack.class, new ResourceLocation(ModItems.MOD_ID, "disc_rack"));
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        ItemBlock writerItem = new ItemBlock(DISC_WRITER);
        writerItem.setRegistryName(DISC_WRITER.getRegistryName());

        ItemBlock rackItem = new ItemBlock(DISC_RACK);
        rackItem.setRegistryName(DISC_RACK.getRegistryName());

        event.getRegistry().registerAll(writerItem, rackItem);
    }
}
