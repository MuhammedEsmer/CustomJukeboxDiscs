package dev.muhammedesmer.customjukeboxdiscs.content;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.writer.DiscWriterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTRAR =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CustomJukeboxDiscs.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiscWriterBlockEntity>> DISC_WRITER =
            REGISTRAR.register("disc_writer", () -> BlockEntityType.Builder.of(
                    DiscWriterBlockEntity::new, ModBlocks.DISC_WRITER.get()).build(null));

    private ModBlockEntities() {
    }
}
