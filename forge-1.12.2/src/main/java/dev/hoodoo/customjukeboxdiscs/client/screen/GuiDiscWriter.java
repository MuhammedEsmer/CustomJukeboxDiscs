package dev.hoodoo.customjukeboxdiscs.client.screen;

import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientUploadManager;
import dev.hoodoo.customjukeboxdiscs.client.transfer.UploadFileScanner;
import dev.hoodoo.customjukeboxdiscs.config.ForgeClientConfig;
import dev.hoodoo.customjukeboxdiscs.content.ModItems;
import dev.hoodoo.customjukeboxdiscs.content.writer.ContainerDiscWriter;
import dev.hoodoo.customjukeboxdiscs.content.writer.TileEntityDiscWriter;
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

public class GuiDiscWriter extends GuiContainer {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(ModItems.MOD_ID, "textures/gui/disc_writer.png");
    private static final int LIST_X = 8;
    private static final int LIST_Y = 18;
    private static final int LIST_WIDTH = 144;
    private static final int ROW_HEIGHT = 13;
    private static final int VISIBLE_ROWS = 4;
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

    public GuiDiscWriter(InventoryPlayer playerInventory, TileEntityDiscWriter writer) {
        super(new ContainerDiscWriter(playerInventory, writer));
        this.containerWriter = (ContainerDiscWriter) this.inventorySlots;
        this.xSize = 192;
        this.ySize = 254;
    }

    @Override
    public void initGui() {
        super.initGui();
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;

        refreshFiles();

        titleField = new GuiTextField(0, fontRenderer, left + 8, top + 74, 176, 16);
        titleField.setMaxStringLength(64);

        urlField = new GuiTextField(1, fontRenderer, left + 8, top + 94, 176, 16);
        urlField.setMaxStringLength(512);

        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, left + 8, top + 136, 56, 20, I18n.format("screen.customjukeboxdiscs.disc_writer.folder")));
        this.buttonList.add(new GuiButton(1, left + 68, top + 136, 56, 20, I18n.format("screen.customjukeboxdiscs.disc_writer.refresh")));
        this.buttonList.add(new GuiButton(2, left + 128, top + 136, 56, 20, I18n.format("screen.customjukeboxdiscs.disc_writer.write")));

        updateSuggestedTitle();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            openUploadsFolder();
        } else if (button.id == 1) {
            refreshFiles();
        } else if (button.id == 2) {
            write();
        }
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
        if (dWheel != 0 && files.size() > VISIBLE_ROWS) {
            if (dWheel > 0) scroll--;
            if (dWheel < 0) scroll++;
            scroll = MathHelper.clamp(scroll, 0, files.size() - VISIBLE_ROWS);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        titleField.mouseClicked(mouseX, mouseY, mouseButton);
        urlField.mouseClicked(mouseX, mouseY, mouseButton);

        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        int localX = mouseX - left - LIST_X;
        int localY = mouseY - top - LIST_Y;

        if (localX >= 0 && localX < LIST_WIDTH && localY >= 0 && localY < VISIBLE_ROWS * ROW_HEIGHT) {
            int row = localY / ROW_HEIGHT;
            if (row + scroll < files.size()) {
                selected = row + scroll;
                updateSuggestedTitle();
                urlField.setText("");
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (titleField.textboxKeyTyped(typedChar, keyCode) || urlField.textboxKeyTyped(typedChar, keyCode)) {
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
        fontRenderer.drawString(I18n.format("container." + ModItems.MOD_ID + ".disc_writer"), LIST_X, 6, 0x404040);
        fontRenderer.drawString(I18n.format("screen.customjukeboxdiscs.disc_writer.slot"), 160, 15, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"), LIST_X, 161, 0x404040);

        String statusStr = status.getFormattedText();
        if (!statusStr.isEmpty()) {
            fontRenderer.drawString(fontRenderer.trimStringToWidth(statusStr, this.xSize - 16), LIST_X, 114, 0x404040);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);

        // Render file list
        if (files.isEmpty()) {
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
        if (progress > 0.0F) {
            int filled = (int) (PROGRESS_WIDTH * Math.min(1.0F, progress));
            drawRect(left + PROGRESS_X, top + PROGRESS_Y, left + PROGRESS_X + filled, top + PROGRESS_Y + PROGRESS_HEIGHT, 0xFF3FA34D);
        }

        titleField.drawTextBox();
        urlField.drawTextBox();
    }
}
