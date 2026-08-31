package dev.muhammedesmer.customjukeboxdiscs.client.screen;

import dev.muhammedesmer.customjukeboxdiscs.content.rack.DiscRackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class DiscRackScreen extends AbstractContainerScreen<DiscRackMenu> {
    // The dispenser window is already a three by three grid with a player inventory below it.
    private static final Identifier BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");

    public DiscRackScreen(DiscRackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
}
