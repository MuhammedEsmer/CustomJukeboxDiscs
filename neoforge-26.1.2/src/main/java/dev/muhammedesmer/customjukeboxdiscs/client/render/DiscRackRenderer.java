package dev.muhammedesmer.customjukeboxdiscs.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.muhammedesmer.customjukeboxdiscs.content.rack.DiscRackBlockEntity;
import dev.muhammedesmer.customjukeboxdiscs.content.rack.RackSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Draws the discs a rack holds on its front face, so their colours are readable without opening it.
 */
public final class DiscRackRenderer implements BlockEntityRenderer<DiscRackBlockEntity> {
    private static final float CELL = 1.0F / 3.0F;
    private static final float DISC_SCALE = 0.26F;
    private static final float FRONT_DEPTH = 0.001F;

    public DiscRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            DiscRackBlockEntity rack, float partialTick, PoseStack poses,
            MultiBufferSource buffers, int light, int overlay) {
        Direction facing = rack.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        for (int slot = 0; slot < RackSlots.SIZE; slot++) {
            ItemStack stack = rack.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            poses.pushPose();
            poses.translate(0.5F, 0.5F, 0.5F);
            poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facing.toYRot()));
            // After the rotation the local +Z axis points out of the front face, so the disc is moved
            // out along +Z and already faces the player without a further turn.
            float across = (RackSlots.column(slot) + 0.5F) * CELL - 0.5F;
            float up = 0.5F - (RackSlots.row(slot) + 0.5F) * CELL;
            poses.translate(across, up, 0.5F + FRONT_DEPTH);
            poses.scale(DISC_SCALE, DISC_SCALE, DISC_SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    poses, buffers, rack.getLevel(), (int) rack.getBlockPos().asLong() + slot);
            poses.popPose();
        }
    }
}
