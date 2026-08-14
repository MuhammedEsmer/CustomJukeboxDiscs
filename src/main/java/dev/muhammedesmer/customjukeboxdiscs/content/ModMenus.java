package dev.muhammedesmer.customjukeboxdiscs.content;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.writer.DiscWriterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRAR =
            DeferredRegister.create(Registries.MENU, CustomJukeboxDiscs.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<DiscWriterMenu>> DISC_WRITER =
            REGISTRAR.register("disc_writer", () -> IMenuTypeExtension.create(DiscWriterMenu::new));

    private ModMenus() {
    }
}
