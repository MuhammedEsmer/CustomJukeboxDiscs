package dev.muhammedesmer.customjukeboxdiscs.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.muhammedesmer.customjukeboxdiscs.content.rack.DiscRackBlockEntity;
import dev.muhammedesmer.customjukeboxdiscs.content.rack.RackSlots;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the discs a rack holds on its front face, so their colours are readable without opening it.
 */
public final class DiscRackRenderer implements BlockEntityRenderer<DiscRackBlockEntity, DiscRackRenderState> {
    private static final float CELL = 1.0F / 3.0F;
    private static final float DISC_SCALE = 0.26F;
    private static final float FRONT_DEPTH = 0.001F;
    private final ItemModelResolver itemModelResolver;

    public DiscRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public DiscRackRenderState createRenderState() {
        return new DiscRackRenderState();
    }

    @Override
    public void extractRenderState(
            DiscRackBlockEntity rack, DiscRackRenderState state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(rack, state, partialTick, cameraPosition, breakProgress);
        state.facing = rack.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        int seed = (int) rack.getBlockPos().asLong();
        for (int slot = 0; slot < RackSlots.SIZE; slot++) {
            ItemStack stack = rack.getItem(slot);
            if (stack.isEmpty()) {
                state.items[slot] = null;
                continue;
            }
            ItemStackRenderState itemState = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(
                    itemState, stack, ItemDisplayContext.FIXED, rack.getLevel(), null, seed + slot);
            state.items[slot] = itemState;
        }
    }

    @Override
    public void submit(
            DiscRackRenderState state, PoseStack poses,
            SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        Direction facing = state.facing;
        for (int slot = 0; slot < RackSlots.SIZE; slot++) {
            ItemStackRenderState itemState = state.items[slot];
            if (itemState == null || itemState.isEmpty()) {
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
            // FIXED item models face local -Z in 26.1; turn the item so its lit front faces out of the rack.
            poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            poses.scale(DISC_SCALE, DISC_SCALE, DISC_SCALE);
            // The final argument is an outline color, not a per-slot render seed.
            itemState.submit(poses, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poses.popPose();
        }
    }
}
