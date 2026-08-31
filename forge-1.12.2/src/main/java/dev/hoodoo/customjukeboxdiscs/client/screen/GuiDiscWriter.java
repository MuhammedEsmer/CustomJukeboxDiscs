package dev.hoodoo.customjukeboxdiscs.client.screen;

import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientUploadManager;
import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientLibraryManager;
import dev.hoodoo.customjukeboxdiscs.client.transfer.UploadFileScanner;
import dev.hoodoo.customjukeboxdiscs.config.ForgeClientConfig;
import dev.hoodoo.customjukeboxdiscs.content.ModItems;
import dev.hoodoo.customjukeboxdiscs.content.writer.ContainerDiscWriter;
import dev.hoodoo.customjukeboxdiscs.content.writer.TileEntityDiscWriter;
import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryPageResponse;
import dev.hoodoo.customjukeboxdiscs.network.packet.PacketLibraryWriteResponse;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;

public class GuiDiscWriter extends GuiContainer implements ClientLibraryManager.Listener {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(ModItems.MOD_ID, "textures/gui/disc_writer.png");
    private static final int LIST_X = 8;
    private static final int LIST_Y = 18;
    private static final int LIST_WIDTH = 144;
    private static final int ROW_HEIGHT = 13;
    private static final int VISIBLE_ROWS = 4;
    private static final int LIBRARY_ROW_HEIGHT = 22;
    private static final int PROGRESS_X = 8;
    private static final int PROGRESS_Y = 129;
    private static final int PROGRESS_WIDTH = 176;
    private static final int PROGRESS_HEIGHT = 5;

    private final Path uploadDirectory = Minecraft.getMinecraft().gameDir.toPath().resolve("customjukeboxdiscs/uploads");
    private final ContainerDiscWriter containerWriter;

    private List<Path> files = Collections.emptyList();
    private int selected;
    private int scroll;
    private GuiTextField titleField;
    private GuiTextField urlField;
    private ITextComponent status = new TextComponentString("");
    private float progress;
    private List<TrackReference> libraryTracks = Collections.emptyList();
    private boolean serverLibrary;
    private int libraryPage = 1;
    private int libraryPageCount = 1;

    public GuiDiscWriter(InventoryPlayer playerInventory, TileEntityDiscWriter writer) {
        super(new ContainerDiscWriter(playerInventory, writer));
        this.containerWriter = (ContainerDiscWriter) this.inventorySlots;
        this.xSize = 192;
        this.ySize = 254;
    }

    @Override
    public void initGui() {
        super.initGui();
        ClientLibraryManager.getInstance().attach(this);
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;

        refreshFiles();

        titleField = new GuiTextField(0, fontRenderer, left + 8, top + 74, 176, 16);
        titleField.setMaxStringLength(64);

        urlField = new GuiTextField(1, fontRenderer, left + 8, top + 94, 176, 16);
        urlField.setMaxStringLength(512);

        this.buttonList.clear();
        this.buttonList.add(new GuiButton(10, left + 8, top + 3, 70, 14, I18n.format("screen.customjukeboxdiscs.disc_writer.local_files")));
        this.buttonList.add(new GuiButton(11, left + 80, top + 3, 76, 14, I18n.format("screen.customjukeboxdiscs.disc_writer.server_library")));
        this.buttonList.add(new GuiButton(0, left + 8, top + 136, 56, 20, I18n.format("screen.customjukeboxdiscs.disc_writer.folder")));
        this.buttonList.add(new GuiButton(1, left + 68, top + 136, 56, 20, I18n.format("screen.customjukeboxdiscs.disc_writer.refresh")));
        this.buttonList.add(new GuiButton(2, left + 128, top + 136, 56, 20, I18n.format("screen.customjukeboxdiscs.disc_writer.write")));
        this.buttonList.add(new GuiButton(3, left + 8, top + 110, 26, 20, "<"));
        this.buttonList.add(new GuiButton(4, left + 36, top + 110, 26, 20, ">"));

        updateSuggestedTitle();
        updateModeWidgets();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            openUploadsFolder();
        } else if (button.id == 1) {
            refreshFiles();
        } else if (button.id == 2) {
            write();
        } else if (button.id == 3) {
            ClientLibraryManager.getInstance().requestPage(libraryPage - 1);
        } else if (button.id == 4) {
            ClientLibraryManager.getInstance().requestPage(libraryPage + 1);
        } else if (button.id == 10) {
            switchTab(false);
        } else if (button.id == 11) {
            switchTab(true);
        }
    }

    private void switchTab(boolean library) {
        serverLibrary = library;
        selected = 0;
        scroll = 0;
        status = new TextComponentString(library ? I18n.format("screen.customjukeboxdiscs.disc_writer.library_loading") : "");
        updateModeWidgets();
        if (library) ClientLibraryManager.getInstance().requestPage(libraryPage);
    }

    private void updateModeWidgets() {
        if (titleField == null) return;
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        titleField.setVisible(!serverLibrary);
        urlField.setVisible(!serverLibrary);
        button(0).visible = !serverLibrary;
        button(1).x = left + 68;
        button(1).y = top + (serverLibrary ? 110 : 136);
        button(2).x = left + 128;
        button(2).y = top + (serverLibrary ? 110 : 136);
        button(3).visible = serverLibrary;
        button(4).visible = serverLibrary;
        button(3).enabled = libraryPage > 1;
        button(4).enabled = libraryPage < libraryPageCount;
    }

    private GuiButton button(int id) {
        for (GuiButton button : buttonList) if (button.id == id) return button;
        throw new IllegalStateException("missing button " + id);
    }

    private void openUploadsFolder() {
        try {
            File dir = uploadDirectory.toFile();
            if (!dir.exists()) dir.mkdirs();
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir);
            }
        } catch (Exception ignored) {
        }
    }

    private void write() {
        if (serverLibrary) {
            if (libraryTracks.isEmpty()) {
                status = new TextComponentString(I18n.format("screen.customjukeboxdiscs.disc_writer.library_empty"));
                return;
            }
            ClientLibraryManager.getInstance().write(libraryTracks.get(selected).getSha256(), containerWriter.inputFingerprint());
            return;
        }
        String url = urlField.getText().trim();
        String sanitized = UploadFileScanner.sanitizeTitle(titleField.getText());
        if (sanitized.isEmpty()) {
            status = new TextComponentString(TextFormatting.RED + I18n.format("screen.customjukeboxdiscs.disc_writer.empty_title"));
            return;
        }
        progress = 0.0F;
        if (!url.isEmpty()) {
            ClientUploadManager.getInstance().beginFromUrl(url, sanitized, containerWriter.inputFingerprint(), this::onStatus);
            return;
        }
        if (files.isEmpty()) {
            status = new TextComponentString(TextFormatting.RED + I18n.format("screen.customjukeboxdiscs.disc_writer.no_files"));
            return;
        }
        ClientUploadManager.getInstance().begin(
                files.get(selected), sanitized, containerWriter.inputFingerprint(), this::onStatus, this::onProgress);
    }

    private void onStatus(ITextComponent message) {
        this.status = message;
    }

    private void onProgress(Double value) {
        this.progress = value.floatValue();
    }

    private void refreshFiles() {
        if (serverLibrary) {
            status = new TextComponentString(I18n.format("screen.customjukeboxdiscs.disc_writer.library_loading"));
            ClientLibraryManager.getInstance().requestPage(libraryPage);
            return;
        }
        try {
            files = UploadFileScanner.scan(uploadDirectory, ForgeClientConfig.maxUploadScanFiles);
            status = files.isEmpty()
                    ? new TextComponentString(I18n.format("screen.customjukeboxdiscs.disc_writer.no_files"))
                    : new TextComponentString("");
        } catch (IOException exception) {
            files = Collections.emptyList();
            status = new TextComponentString(TextFormatting.RED + I18n.format("screen.customjukeboxdiscs.disc_writer.scan_failed"));
        }
        selected = 0;
        scroll = 0;
        updateSuggestedTitle();
    }

    private void updateSuggestedTitle() {
        if (titleField != null && !files.isEmpty()) {
            titleField.setText(UploadFileScanner.titleFromFile(files.get(selected).getFileName().toString()));
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        int size = serverLibrary ? libraryTracks.size() : files.size();
        if (dWheel != 0 && size > VISIBLE_ROWS) {
            if (dWheel > 0) scroll--;
            if (dWheel < 0) scroll++;
            scroll = MathHelper.clamp(scroll, 0, size - VISIBLE_ROWS);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!serverLibrary) {
            titleField.mouseClicked(mouseX, mouseY, mouseButton);
            urlField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        int localX = mouseX - left - LIST_X;
        int localY = mouseY - top - LIST_Y;

        int rowHeight = serverLibrary ? LIBRARY_ROW_HEIGHT : ROW_HEIGHT;
        int size = serverLibrary ? libraryTracks.size() : files.size();
        if (localX >= 0 && localX < LIST_WIDTH && localY >= 0 && localY < VISIBLE_ROWS * rowHeight) {
            int row = localY / rowHeight;
            if (row + scroll < size) {
                selected = row + scroll;
                if (!serverLibrary) {
                    updateSuggestedTitle();
                    urlField.setText("");
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!serverLibrary && (titleField.textboxKeyTyped(typedChar, keyCode) || urlField.textboxKeyTyped(typedChar, keyCode))) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        titleField.updateCursorCounter();
        urlField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("screen.customjukeboxdiscs.disc_writer.slot"), 160, 15, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"), LIST_X, 161, 0x404040);

        String statusStr = status.getFormattedText();
        int statusX = serverLibrary ? 48 : LIST_X;
        int statusY = serverLibrary ? 132 : 114;
        if (serverLibrary) fontRenderer.drawString(libraryPage + "/" + libraryPageCount, LIST_X, statusY, 0x5A4935);
        if (!statusStr.isEmpty()) {
            fontRenderer.drawString(fontRenderer.trimStringToWidth(statusStr, this.xSize - statusX - 8), statusX, statusY, 0x404040);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);

        if (serverLibrary) {
            renderLibrary(left, top);
        } else if (files.isEmpty()) {
            fontRenderer.drawString(I18n.format("screen.customjukeboxdiscs.disc_writer.no_files"),
                    left + LIST_X + 3, top + LIST_Y + 4, 0x808080);
        } else {
            for (int row = 0; row < VISIBLE_ROWS && row + scroll < files.size(); row++) {
                int index = row + scroll;
                int y = top + LIST_Y + row * ROW_HEIGHT;
                if (index == selected) {
                    drawRect(left + LIST_X, y, left + LIST_X + LIST_WIDTH - 2, y + ROW_HEIGHT - 1, 0xFF4A6E9C);
                }
                String name = files.get(index).getFileName().toString();
                fontRenderer.drawString(fontRenderer.trimStringToWidth(name, LIST_WIDTH - 8),
                        left + LIST_X + 3, y + 3, index == selected ? 0xFFFFFF : 0x404040);
            }
        }

        // Render progress bar
        if (!serverLibrary && progress > 0.0F) {
            int filled = (int) (PROGRESS_WIDTH * Math.min(1.0F, progress));
            drawRect(left + PROGRESS_X, top + PROGRESS_Y, left + PROGRESS_X + filled, top + PROGRESS_Y + PROGRESS_HEIGHT, 0xFF3FA34D);
        }

        if (!serverLibrary) {
            titleField.drawTextBox();
            urlField.drawTextBox();
        }
    }

    private void renderLibrary(int left, int top) {
        if (libraryTracks.isEmpty()) {
            fontRenderer.drawString(I18n.format("screen.customjukeboxdiscs.disc_writer.library_empty"),
                    left + LIST_X + 3, top + LIST_Y + 4, 0x686868);
            return;
        }
        for (int row = 0; row < VISIBLE_ROWS && row + scroll < libraryTracks.size(); row++) {
            int index = row + scroll;
            TrackReference track = libraryTracks.get(index);
            int y = top + LIST_Y + row * LIBRARY_ROW_HEIGHT;
            if (index == selected) drawRect(left + LIST_X, y, left + LIST_X + LIST_WIDTH, y + 21, 0xFF8B6B34);
            int color = index == selected ? 0xFFFFFF : 0x3F3528;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(track.getTitle(), LIST_WIDTH - 6), left + LIST_X + 3, y + 2, color);
            String details = track.getUploaderName() + " · " + duration(track.getDurationMillis()) + " · "
                    + track.getFormat().serializedName().toUpperCase(java.util.Locale.ROOT);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(details, LIST_WIDTH - 6), left + LIST_X + 3, y + 12,
                    index == selected ? 0xF0DFC1 : 0x776A58);
        }
    }

    private static String duration(long millis) {
        long seconds = millis / 1000L;
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    @Override
    public void onLibraryPage(PacketLibraryPageResponse response) {
        if (Minecraft.getMinecraft().currentScreen != this) return;
        libraryTracks = response.getTracks();
        libraryPage = response.getPage();
        libraryPageCount = response.getPageCount();
        selected = 0;
        scroll = 0;
        status = new TextComponentString(libraryTracks.isEmpty()
                ? I18n.format("screen.customjukeboxdiscs.disc_writer.library_empty") : "");
        updateModeWidgets();
    }

    @Override
    public void onLibraryWrite(PacketLibraryWriteResponse response) {
        if (Minecraft.getMinecraft().currentScreen != this) return;
        String key;
        switch (response.getResult()) {
            case WRITTEN: key = "screen.customjukeboxdiscs.disc_writer.disc_written"; break;
            case INVALID_WRITER: key = "screen.customjukeboxdiscs.disc_writer.insert_blank"; break;
            default: key = "screen.customjukeboxdiscs.disc_writer.track_unavailable"; break;
        }
        status = new TextComponentString(I18n.format(key));
        if (response.getResult() == PacketLibraryWriteResponse.Result.TRACK_UNAVAILABLE) {
            ClientLibraryManager.getInstance().requestPage(libraryPage);
        }
    }

    @Override
    public void onGuiClosed() {
        ClientLibraryManager.getInstance().detach(this);
        super.onGuiClosed();
    }
}
