package dev.muhammedesmer.customjukeboxdiscs.content.disc;

import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ProgrammedDiscItem extends Item {
    public ProgrammedDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TrackReference track = stack.get(ModDataComponents.TRACK_REFERENCE);
        if (track != null) {
            tooltip.add(Component.literal(track.title()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.customjukeboxdiscs.programmed_disc.uploader", track.uploaderName())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
