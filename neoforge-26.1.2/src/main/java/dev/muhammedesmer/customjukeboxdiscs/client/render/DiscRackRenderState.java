package dev.muhammedesmer.customjukeboxdiscs.client.render;

import dev.muhammedesmer.customjukeboxdiscs.content.rack.RackSlots;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public final class DiscRackRenderState extends BlockEntityRenderState {
    public final @Nullable ItemStackRenderState[] items = new ItemStackRenderState[RackSlots.SIZE];
    public Direction facing = Direction.NORTH;
}
