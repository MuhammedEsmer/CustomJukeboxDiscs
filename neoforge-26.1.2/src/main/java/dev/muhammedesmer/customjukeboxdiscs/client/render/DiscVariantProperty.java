package dev.muhammedesmer.customjukeboxdiscs.client.render;

import com.mojang.serialization.MapCodec;
import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class DiscVariantProperty implements RangeSelectItemModelProperty {
    public static final DiscVariantProperty INSTANCE = new DiscVariantProperty();
    public static final MapCodec<DiscVariantProperty> MAP_CODEC = MapCodec.unit(INSTANCE);

    private DiscVariantProperty() {
    }

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return stack.getOrDefault(ModDataComponents.DISC_VARIANT, 0);
    }

    @Override
    public MapCodec<DiscVariantProperty> type() {
        return MAP_CODEC;
    }
}
