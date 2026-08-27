package dev.hoodoo.customjukeboxdiscs.client.render;

import dev.hoodoo.customjukeboxdiscs.content.rack.BlockDiscRack;
import dev.hoodoo.customjukeboxdiscs.content.rack.RackSlots;
import dev.hoodoo.customjukeboxdiscs.content.rack.TileEntityDiscRack;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class TileEntityDiscRackRenderer extends TileEntitySpecialRenderer<TileEntityDiscRack> {
    private static final float CELL = 1.0F / 3.0F;
    private static final float DISC_SCALE = 0.26F;
    private static final float FRONT_DEPTH = 0.005F;

    @Override
    public void render(TileEntityDiscRack te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (!te.hasWorld()) return;
        IBlockState state = te.getWorld().getBlockState(te.getPos());
        if (!(state.getBlock() instanceof BlockDiscRack)) return;

        EnumFacing facing = state.getValue(BlockDiscRack.FACING);
        float rotation = facing.getHorizontalAngle();

        for (int slot = 0; slot < RackSlots.SIZE; slot++) {
            ItemStack stack = te.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.5D);
            GlStateManager.rotate(-rotation, 0.0F, 1.0F, 0.0F);

            float across = (RackSlots.column(slot) + 0.5F) * CELL - 0.5F;
            float up = 0.5F - (RackSlots.row(slot) + 0.5F) * CELL;
            GlStateManager.translate(across, up, 0.5F + FRONT_DEPTH);
            GlStateManager.scale(DISC_SCALE, DISC_SCALE, DISC_SCALE);

            Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
            GlStateManager.popMatrix();
        }
    }
}
