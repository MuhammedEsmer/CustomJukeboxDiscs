package dev.hoodoo.customjukeboxdiscs.content.disc;

import dev.hoodoo.customjukeboxdiscs.content.ModCreativeTabs;
import dev.hoodoo.customjukeboxdiscs.content.ModItems;
import dev.hoodoo.customjukeboxdiscs.content.ModSounds;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ProgrammedDiscItem extends ItemRecord {
    public static final String NBT_KEY = "customjukeboxdiscs";
    public static final String NBT_VARIANT_KEY = "disc_variant";

    public ProgrammedDiscItem() {
        super("custom_disc", ModSounds.DYNAMIC_TRACK);
        setRegistryName(ModItems.MOD_ID, "programmed_disc");
        setTranslationKey(ModItems.MOD_ID + ".programmed_disc");
        setMaxStackSize(1);
        setCreativeTab(ModCreativeTabs.TAB);
    }

    @Nullable
    public static TrackReference getTrackReference(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(NBT_KEY)) {
            return TrackReference.fromNBT(tag.getCompoundTag(NBT_KEY));
        }
        return null;
    }

    public static void setTrackReference(ItemStack stack, TrackReference track) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setTag(NBT_KEY, track.toNBT());
    }

    public static int getVariant(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return 0;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? DiscVariant.clamp(tag.getInteger(NBT_VARIANT_KEY)) : 0;
    }

    public static void setVariant(ItemStack stack, int variant) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(NBT_VARIANT_KEY, DiscVariant.clamp(variant));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        TrackReference track = getTrackReference(stack);
        if (track != null) {
            tooltip.add(TextFormatting.GRAY + track.getTitle());
            tooltip.add(TextFormatting.DARK_GRAY + I18n.format("item.customjukeboxdiscs.programmed_disc.uploader", track.getUploaderName()));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getRecordNameLocal() {
        return I18n.format("item.customjukeboxdiscs.programmed_disc.name");
    }
}
