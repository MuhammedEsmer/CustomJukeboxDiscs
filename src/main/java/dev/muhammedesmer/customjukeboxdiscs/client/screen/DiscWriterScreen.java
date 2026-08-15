package dev.muhammedesmer.customjukeboxdiscs.client.screen;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.client.transfer.ClientUploadManager;
import dev.muhammedesmer.customjukeboxdiscs.client.transfer.UploadFileScanner;
import dev.muhammedesmer.customjukeboxdiscs.config.ClientConfig;
import dev.muhammedesmer.customjukeboxdiscs.content.writer.DiscWriterMenu;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class DiscWriterScreen extends AbstractContainerScreen<DiscWriterMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            CustomJukeboxDiscs.MOD_ID, "textures/gui/disc_writer.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 18;
    private static final int LIST_WIDTH = 144;
    private static final int ROW_HEIGHT = 13;
    private static final int VISIBLE_ROWS = 4;
    private static final int PROGRESS_X = 8;
    private static final int PROGRESS_Y = 129;
    private static final int PROGRESS_WIDTH = 176;
    private static final int PROGRESS_HEIGHT = 5;

    private final Path uploadDirectory = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("customjukeboxdiscs/uploads");

    private List<Path> files = List.of();
    private int selected;
    private int scroll;
    private EditBox titleBox;
    private EditBox urlBox;
    private Component status = Component.empty();
    private float progress;

    public DiscWriterScreen(DiscWriterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 192;
        imageHeight = 254;
        inventoryLabelY = 161;
    }

    @Override
    protected void init() {
        super.init();
        refreshFiles();

        titleBox = new EditBox(font, leftPos + 8, topPos + 74, 176, 16,
                Component.translatable("screen.customjukeboxdiscs.disc_writer.title"));
        titleBox.setMaxLength(64);
        titleBox.setHint(Component.translatable("screen.customjukeboxdiscs.disc_writer.title_hint"));
        addRenderableWidget(titleBox);

        urlBox = new EditBox(font, leftPos + 8, topPos + 94, 176, 16,
                Component.translatable("screen.customjukeboxdiscs.disc_writer.url"));
        urlBox.setMaxLength(512);
        urlBox.setHint(Component.translatable("screen.customjukeboxdiscs.disc_writer.url_hint"));
        addRenderableWidget(urlBox);

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.customjukeboxdiscs.disc_writer.folder"),
                        button -> Util.getPlatform().openPath(uploadDirectory))
                .bounds(leftPos + 8, topPos + 138, 56, 16).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.customjukeboxdiscs.disc_writer.refresh"),
                        button -> refreshFiles())
                .bounds(leftPos + 68, topPos + 138, 56, 16).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.customjukeboxdiscs.disc_writer.write"),
                        button -> write())
                .bounds(leftPos + 128, topPos + 138, 56, 16).build());

        updateSuggestedTitle();
    }

    private void write() {
        String url = urlBox.getValue().strip();
        String sanitized = UploadFileScanner.sanitizeTitle(titleBox.getValue());
        if (sanitized.isEmpty()) {
            status = Component.translatable("screen.customjukeboxdiscs.disc_writer.empty_title")
                    .withStyle(ChatFormatting.RED);
            return;
        }
        progress = 0.0F;
        if (!url.isEmpty()) {
            ClientUploadManager.INSTANCE.beginFromUrl(url, sanitized, menu.inputFingerprint(), this::onStatus);
            return;
        }
        if (files.isEmpty()) {
            status = Component.translatable("screen.customjukeboxdiscs.disc_writer.no_files")
                    .withStyle(ChatFormatting.RED);
            return;
        }
        ClientUploadManager.INSTANCE.begin(
                files.get(selected), sanitized, menu.inputFingerprint(), this::onStatus, this::onProgress);
    }

    private void onStatus(Component message) {
        status = message;
    }

    private void onProgress(double value) {
        progress = (float) value;
    }

    private void refreshFiles() {
        try {
            files = UploadFileScanner.scan(uploadDirectory, ClientConfig.INSTANCE.snapshot().maxUploadScanFiles());
            status = files.isEmpty()
                    ? Component.translatable("screen.customjukeboxdiscs.disc_writer.no_files")
                    : Component.empty();
        } catch (IOException exception) {
            files = List.of();
            status = Component.translatable("screen.customjukeboxdiscs.disc_writer.scan_failed")
                    .withStyle(ChatFormatting.RED);
        }
        selected = 0;
        scroll = 0;
        updateSuggestedTitle();
    }

    private void updateSuggestedTitle() {
        if (titleBox != null && !files.isEmpty()) {
            titleBox.setValue(UploadFileScanner.titleFromFile(files.get(selected).getFileName().toString()));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int row = rowAt(mouseX, mouseY);
        if (row >= 0 && row + scroll < files.size()) {
            selected = row + scroll;
            updateSuggestedTitle();
            urlBox.setValue("");
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (rowAt(mouseX, mouseY) >= 0 && files.size() > VISIBLE_ROWS) {
            scroll = net.minecraft.util.Mth.clamp(scroll - (int) Math.signum(deltaY), 0, files.size() - VISIBLE_ROWS);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private int rowAt(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos - LIST_X;
        int localY = (int) mouseY - topPos - LIST_Y;
        boolean inside = localX >= 0 && localX < LIST_WIDTH && localY >= 0 && localY < VISIBLE_ROWS * ROW_HEIGHT;
        return inside ? localY / ROW_HEIGHT : -1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        renderFileList(graphics);
        renderProgress(graphics);
    }

    private void renderFileList(GuiGraphics graphics) {
        if (files.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.customjukeboxdiscs.disc_writer.no_files"),
                    leftPos + LIST_X + 3, topPos + LIST_Y + 4, 0x808080, false);
            return;
        }
        for (int row = 0; row < VISIBLE_ROWS && row + scroll < files.size(); row++) {
            int index = row + scroll;
            int y = topPos + LIST_Y + row * ROW_HEIGHT;
            if (index == selected) {
                graphics.fill(leftPos + LIST_X, y, leftPos + LIST_X + LIST_WIDTH - 2, y + ROW_HEIGHT - 1, 0xFF4A6E9C);
            }
            String name = files.get(index).getFileName().toString();
            graphics.drawString(font, font.plainSubstrByWidth(name, LIST_WIDTH - 8),
                    leftPos + LIST_X + 3, y + 3, index == selected ? 0xFFFFFF : 0x404040, false);
        }
    }

    private void renderProgress(GuiGraphics graphics) {
        if (progress <= 0.0F) {
            return;
        }
        int filled = (int) (PROGRESS_WIDTH * Math.min(1.0F, progress));
        graphics.fill(leftPos + PROGRESS_X, topPos + PROGRESS_Y,
                leftPos + PROGRESS_X + filled, topPos + PROGRESS_Y + PROGRESS_HEIGHT, 0xFF3FA34D);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, LIST_X, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("screen.customjukeboxdiscs.disc_writer.slot"),
                160, 15, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, LIST_X, inventoryLabelY, 0x404040, false);
        if (status.getString().isEmpty()) {
            return;
        }
        graphics.drawString(font, font.plainSubstrByWidth(status.getString(), imageWidth - 16),
                LIST_X, 114, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if ((titleBox.isFocused() || urlBox.isFocused()) && key != 256) {
            return titleBox.keyPressed(key, scanCode, modifiers)
                    || urlBox.keyPressed(key, scanCode, modifiers)
                    || super.keyPressed(key, scanCode, modifiers);
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
