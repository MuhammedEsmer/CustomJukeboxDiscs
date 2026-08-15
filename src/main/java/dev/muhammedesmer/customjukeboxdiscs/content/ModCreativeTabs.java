package dev.muhammedesmer.customjukeboxdiscs.content;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTRAR =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CustomJukeboxDiscs.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTRAR.register(
            "main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.customjukeboxdiscs"))
                    .icon(() -> new ItemStack(ModItems.BLANK_DISC.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.DISC_WRITER.get());
                        output.accept(ModItems.DISC_RACK.get());
                        output.accept(ModItems.BLANK_DISC.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
