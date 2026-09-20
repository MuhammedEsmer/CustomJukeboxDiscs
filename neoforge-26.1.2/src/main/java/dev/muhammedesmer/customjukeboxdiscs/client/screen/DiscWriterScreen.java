package dev.muhammedesmer.customjukeboxdiscs.client.screen;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.client.transfer.ClientUploadManager;
import dev.muhammedesmer.customjukeboxdiscs.client.transfer.ClientLibraryManager;
import dev.muhammedesmer.customjukeboxdiscs.client.transfer.UploadFileScanner;
import dev.muhammedesmer.customjukeboxdiscs.config.ClientConfig;
import dev.muhammedesmer.customjukeboxdiscs.content.writer.DiscWriterMenu;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.LibraryPageResponse;
import dev.muhammedesmer.customjukeboxdiscs.network.payload.LibraryWriteResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class DiscWriterScreen extends AbstractContainerScreen<DiscWriterMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            CustomJukeboxDiscs.MOD_ID, "textures/gui/disc_writer.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 18;
    private static final int LIST_WIDTH = 144;
    private static final int ROW_HEIGHT = 13;
    private static final int VISIBLE_ROWS = 4;
    private static final int LIBRARY_ROW_HEIGHT = 22;
    private static final int PROGRESS_X = 8;
    private static final int PROGRESS_Y = 134;
    private static final int PROGRESS_WIDTH = 176;
    private static final int PROGRESS_HEIGHT = 3;

    private final Path uploadDirectory = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("customjukeboxdiscs/uploads");

    private List<Path> files = List.of();
    private int selected;
    private int scroll;
    private EditBox titleBox;
    private EditBox urlBox;
    private Component status = Component.empty();
    private float progress;
    private List<TrackReference> libraryTracks = List.of();
    private boolean serverLibrary;
    private int libraryPage = 1;
    private int libraryPageCount = 1;
    private Button folderButton;
    private Button refreshButton;
    private Button writeButton;
    private Button previousButton;
    private Button nextButton;

    public DiscWriterScreen(DiscWriterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 192, 254);
        inventoryLabelY = 161;
    }

    @Override
    protected void init() {
        super.init();
        ClientLibraryManager.INSTANCE.attach(this);
        refreshFiles();

        addRenderableWidget(Button.builder(Component.translatable("screen.customjukeboxdiscs.disc_writer.local_files"),
                        button -> switchTab(false))
                .bounds(leftPos + 8, topPos + 3, 70, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.customjukeboxdiscs.disc_writer.server_library"),
                        button -> switchTab(true))
                .bounds(leftPos + 80, topPos + 3, 76, 14).build());

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

        folderButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.customjukeboxdiscs.disc_writer.folder"),
                        button -> Util.getPlatform().openPath(uploadDirectory))
                .bounds(leftPos + 8, topPos + 138, 56, 16).build());
        refreshButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.customjukeboxdiscs.disc_writer.refresh"),
                        button -> refreshFiles())
                .bounds(leftPos + 68, topPos + 138, 56, 16).build());
        writeButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.customjukeboxdiscs.disc_writer.write"),
                        button -> write())
                .bounds(leftPos + 128, topPos + 138, 56, 16).build());

        previousButton = addRenderableWidget(Button.builder(Component.literal("<"),
                        button -> ClientLibraryManager.INSTANCE.requestPage(libraryPage - 1))
                .bounds(leftPos + 8, topPos + 112, 26, 16).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"),
                        button -> ClientLibraryManager.INSTANCE.requestPage(libraryPage + 1))
                .bounds(leftPos + 36, topPos + 112, 26, 16).build());

        updateSuggestedTitle();
        updateModeWidgets();
    }

    private void switchTab(boolean library) {
        serverLibrary = library;
        selected = 0;
        scroll = 0;
        status = library ? Component.translatable("screen.customjukeboxdiscs.disc_writer.library_loading") : Component.empty();
        updateModeWidgets();
        if (library) ClientLibraryManager.INSTANCE.requestPage(libraryPage);
    }

    private void updateModeWidgets() {
        if (titleBox == null) return;
        titleBox.visible = !serverLibrary;
        urlBox.visible = !serverLibrary;
        folderButton.visible = !serverLibrary;
        previousButton.visible = serverLibrary;
        nextButton.visible = serverLibrary;
        refreshButton.setMessage(Component.translatable("screen.customjukeboxdiscs.disc_writer.refresh"));
        refreshButton.setX(leftPos + 68);
        refreshButton.setY(topPos + (serverLibrary ? 112 : 138));
        writeButton.setX(leftPos + 128);
        writeButton.setY(topPos + (serverLibrary ? 112 : 138));
        previousButton.active = libraryPage > 1;
        nextButton.active = libraryPage < libraryPageCount;
    }

    private void write() {
        if (serverLibrary) {
            if (libraryTracks.isEmpty()) {
                status = Component.translatable("screen.customjukeboxdiscs.disc_writer.library_empty");
                return;
            }
            ClientLibraryManager.INSTANCE.write(libraryTracks.get(selected).sha256());
            return;
        }
        String url = urlBox.getValue().strip();
        String sanitized = UploadFileScanner.sanitizeTitle(titleBox.getValue());
        if (sanitized.isEmpty() && url.isEmpty()) {
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
        if (serverLibrary) {
            status = Component.translatable("screen.customjukeboxdiscs.disc_writer.library_loading");
            ClientLibraryManager.INSTANCE.requestPage(libraryPage);
            return;
        }
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int row = rowAt(event.x(), event.y());
        int size = serverLibrary ? libraryTracks.size() : files.size();
        if (row >= 0 && row + scroll < size) {
            selected = row + scroll;
            if (!serverLibrary) {
                updateSuggestedTitle();
                urlBox.setValue("");
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int size = serverLibrary ? libraryTracks.size() : files.size();
        if (rowAt(mouseX, mouseY) >= 0 && size > VISIBLE_ROWS) {
            scroll = net.minecraft.util.Mth.clamp(scroll - (int) Math.signum(deltaY), 0, size - VISIBLE_ROWS);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private int rowAt(double mouseX, double mouseY) {
        int localX = (int) mouseX - leftPos - LIST_X;
        int localY = (int) mouseY - topPos - LIST_Y;
        int rowHeight = serverLibrary ? LIBRARY_ROW_HEIGHT : ROW_HEIGHT;
        boolean inside = localX >= 0 && localX < LIST_WIDTH && localY >= 0 && localY < VISIBLE_ROWS * rowHeight;
        return inside ? localY / rowHeight : -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        if (serverLibrary) renderLibrary(graphics); else renderFileList(graphics);
        renderProgress(graphics);
    }

    private void renderLibrary(GuiGraphicsExtractor graphics) {
        if (libraryTracks.isEmpty()) {
            graphics.text(font, Component.translatable("screen.customjukeboxdiscs.disc_writer.library_empty"),
                    leftPos + LIST_X + 3, topPos + LIST_Y + 4, 0xFF686868, false);
            return;
        }
        for (int row = 0; row < VISIBLE_ROWS && row + scroll < libraryTracks.size(); row++) {
            int index = row + scroll;
            TrackReference track = libraryTracks.get(index);
            int y = topPos + LIST_Y + row * LIBRARY_ROW_HEIGHT;
            if (index == selected) graphics.fill(leftPos + LIST_X, y, leftPos + LIST_X + LIST_WIDTH, y + 21, 0xFF8B6B34);
            int color = index == selected ? 0xFFFFFFFF : 0xFF3F3528;
            graphics.text(font, font.plainSubstrByWidth(track.title(), LIST_WIDTH - 6),
                    leftPos + LIST_X + 3, y + 2, color, false);
            String details = track.uploaderName() + " · " + duration(track.durationMillis()) + " · "
                    + track.format().serializedName().toUpperCase(java.util.Locale.ROOT);
            graphics.text(font, font.plainSubstrByWidth(details, LIST_WIDTH - 6),
                    leftPos + LIST_X + 3, y + 12, index == selected ? 0xFFF0DFC1 : 0xFF776A58, false);
        }
    }

    private static String duration(long millis) {
        long seconds = millis / 1_000L;
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    public void onLibraryPage(LibraryPageResponse response) {
        libraryTracks = response.tracks();
        libraryPage = response.page();
        libraryPageCount = response.pageCount();
        selected = 0;
        scroll = 0;
        status = libraryTracks.isEmpty()
                ? Component.translatable("screen.customjukeboxdiscs.disc_writer.library_empty") : Component.empty();
        updateModeWidgets();
    }

    public void onLibraryWrite(LibraryWriteResponse response) {
        status = Component.translatable(switch (response.result()) {
            case WRITTEN -> "screen.customjukeboxdiscs.disc_writer.disc_written";
            case INVALID_WRITER -> "screen.customjukeboxdiscs.disc_writer.insert_blank";
            case TRACK_UNAVAILABLE -> "screen.customjukeboxdiscs.disc_writer.track_unavailable";
        });
        if (response.result() == LibraryWriteResponse.Result.TRACK_UNAVAILABLE) {
            ClientLibraryManager.INSTANCE.requestPage(libraryPage);
        }
    }

    private void renderFileList(GuiGraphicsExtractor graphics) {
        if (files.isEmpty()) {
            graphics.text(font, Component.translatable("screen.customjukeboxdiscs.disc_writer.no_files"),
                    leftPos + LIST_X + 3, topPos + LIST_Y + 4, 0xFF808080, false);
            return;
        }
        for (int row = 0; row < VISIBLE_ROWS && row + scroll < files.size(); row++) {
            int index = row + scroll;
            int y = topPos + LIST_Y + row * ROW_HEIGHT;
            if (index == selected) {
                graphics.fill(leftPos + LIST_X, y, leftPos + LIST_X + LIST_WIDTH - 2, y + ROW_HEIGHT - 1, 0xFF4A6E9C);
            }
            String name = files.get(index).getFileName().toString();
            graphics.text(font, font.plainSubstrByWidth(name, LIST_WIDTH - 8),
                    leftPos + LIST_X + 3, y + 3, index == selected ? 0xFFFFFFFF : 0xFF404040, false);
        }
    }

    private void renderProgress(GuiGraphicsExtractor graphics) {
        if (serverLibrary || progress <= 0.0F) {
            return;
        }
        int filled = (int) (PROGRESS_WIDTH * Math.min(1.0F, progress));
        graphics.fill(leftPos + PROGRESS_X, topPos + PROGRESS_Y,
                leftPos + PROGRESS_X + filled, topPos + PROGRESS_Y + PROGRESS_HEIGHT, 0xFF3FA34D);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.translatable("screen.customjukeboxdiscs.disc_writer.slot"),
                160, 15, 0xFF404040, false);
        graphics.text(font, playerInventoryTitle, LIST_X, inventoryLabelY, 0xFF404040, false);
        int statusX = serverLibrary ? 48 : LIST_X;
        int statusY = serverLibrary ? 132 : 114;
        if (serverLibrary) {
            graphics.text(font, libraryPage + "/" + libraryPageCount, LIST_X, statusY, 0xFF5A4935, false);
        }
        if (status.getString().isEmpty()) {
            return;
        }
        var lines = font.split(status, imageWidth - statusX - 8);
        for (int index = 0; index < Math.min(2, lines.size()); index++) {
            graphics.text(font, lines.get(index), statusX, statusY + index * 9, 0xFF404040, false);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((titleBox.isFocused() || urlBox.isFocused()) && event.key() != 256) {
            return titleBox.keyPressed(event)
                    || urlBox.keyPressed(event)
                    || super.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        ClientLibraryManager.INSTANCE.detach(this);
        super.removed();
    }
}
