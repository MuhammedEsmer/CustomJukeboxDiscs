package dev.muhammedesmer.customjukeboxdiscs.content;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.rack.DiscRackBlock;
import dev.muhammedesmer.customjukeboxdiscs.content.writer.DiscWriterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks REGISTRAR = DeferredRegister.createBlocks(CustomJukeboxDiscs.MOD_ID);
    public static final DeferredBlock<Block> DISC_WRITER = REGISTRAR.registerBlock(
            "disc_writer", DiscWriterBlock::new, BlockBehaviour.Properties.of().strength(2.5F));
    public static final DeferredBlock<Block> DISC_RACK = REGISTRAR.registerBlock(
            "disc_rack", DiscRackBlock::new, BlockBehaviour.Properties.of().strength(2.0F).noOcclusion());

    private ModBlocks() {
    }
}
