package dev.muhammedesmer.customjukeboxdiscs.content.disc;

import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class ProgrammedDiscItem extends Item {
    public ProgrammedDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        TrackReference track = stack.get(ModDataComponents.TRACK_REFERENCE);
        if (track != null) {
            tooltip.accept(Component.literal(track.title()).withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("item.customjukeboxdiscs.programmed_disc.uploader", track.uploaderName())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
