package dev.muhammedesmer.customjukeboxdiscs.client.screen;

import dev.muhammedesmer.customjukeboxdiscs.client.transfer.UploadFileScanner;
import dev.muhammedesmer.customjukeboxdiscs.client.transfer.ClientUploadManager;
import dev.muhammedesmer.customjukeboxdiscs.config.ClientConfig;
import dev.muhammedesmer.customjukeboxdiscs.content.writer.DiscWriterMenu;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class DiscWriterScreen extends AbstractContainerScreen<DiscWriterMenu> {
    private final Path uploadDirectory = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("customjukeboxdiscs/uploads");
    private List<Path> files = List.of();
    private int selected;
    private EditBox titleBox;
    private Button fileButton;
    private Component status = Component.empty();

    public DiscWriterScreen(DiscWriterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        refreshFiles();
        fileButton = addRenderableWidget(Button.builder(selectedFileName(), button -> selectNext())
                .bounds(leftPos + 54, topPos + 22, 112, 20).build());
        titleBox = new EditBox(font, leftPos + 54, topPos + 47, 112, 18,
                Component.translatable("screen.customjukeboxdiscs.disc_writer.title"));
        titleBox.setMaxLength(64);
        updateSuggestedTitle();
        addRenderableWidget(titleBox);
        addRenderableWidget(Button.builder(Component.translatable("screen.customjukeboxdiscs.disc_writer.folder"),
                button -> Util.getPlatform().openPath(uploadDirectory)).bounds(leftPos + 54, topPos + 68, 54, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.customjukeboxdiscs.disc_writer.upload"),
                button -> upload())
                .bounds(leftPos + 112, topPos + 68, 54, 20).build());
    }

    private void upload() {
        if (files.isEmpty()) return;
        String sanitized = UploadFileScanner.sanitizeTitle(titleBox.getValue());
        if (sanitized.isEmpty()) {
            status = Component.translatable("screen.customjukeboxdiscs.disc_writer.empty_title");
            return;
        }
        ClientUploadManager.INSTANCE.begin(files.get(selected), sanitized, menu.inputFingerprint(), value -> status = value);
    }

    private void refreshFiles() {
        try {
            files = UploadFileScanner.scan(uploadDirectory, ClientConfig.INSTANCE.snapshot().maxUploadScanFiles());
        } catch (IOException exception) {
            files = List.of();
            status = Component.translatable("screen.customjukeboxdiscs.disc_writer.scan_failed");
        }
        selected = 0;
    }

    private void selectNext() {
        if (!files.isEmpty()) selected = (selected + 1) % files.size();
        fileButton.setMessage(selectedFileName());
        updateSuggestedTitle();
    }

    private Component selectedFileName() {
        return files.isEmpty() ? Component.translatable("screen.customjukeboxdiscs.disc_writer.no_files")
                : Component.literal(files.get(selected).getFileName().toString());
    }

    private void updateSuggestedTitle() {
        if (titleBox != null && !files.isEmpty()) {
            titleBox.setValue(UploadFileScanner.titleFromFile(files.get(selected).getFileName().toString()));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF252525);
        graphics.fill(leftPos + 7, topPos + 17, leftPos + 45, topPos + 61, 0xFF151515);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, status, leftPos + 8, topPos + 72, 0xB0B0B0, false);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
