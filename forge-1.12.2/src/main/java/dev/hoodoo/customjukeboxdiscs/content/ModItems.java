package dev.hoodoo.customjukeboxdiscs.content;

import dev.hoodoo.customjukeboxdiscs.content.disc.BlankDiscItem;
import dev.hoodoo.customjukeboxdiscs.content.disc.DiscVariant;
import dev.hoodoo.customjukeboxdiscs.content.disc.ProgrammedDiscItem;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = ModItems.MOD_ID)
public final class ModItems {
    public static final String MOD_ID = "customjukeboxdiscs";

    public static final BlankDiscItem BLANK_DISC = new BlankDiscItem();
    public static final ProgrammedDiscItem PROGRAMMED_DISC = new ProgrammedDiscItem();

    private ModItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(BLANK_DISC, PROGRAMMED_DISC);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        registerSimpleItemModel(BLANK_DISC, "blank_disc");
        registerSimpleItemModel(Item.getItemFromBlock(ModBlocks.DISC_WRITER), "disc_writer");
        registerSimpleItemModel(Item.getItemFromBlock(ModBlocks.DISC_RACK), "disc_rack");

        // Register 12 variant models for programmed_disc
        ModelResourceLocation[] variantLocations = new ModelResourceLocation[DiscVariant.COUNT];
        ResourceLocation[] variantNames = new ResourceLocation[DiscVariant.COUNT];
        for (int i = 0; i < DiscVariant.COUNT; i++) {
            variantLocations[i] = new ModelResourceLocation(new ResourceLocation(MOD_ID, "programmed_disc_" + i), "inventory");
            variantNames[i] = new ResourceLocation(MOD_ID, "programmed_disc_" + i);
        }
        ModelBakery.registerItemVariants(PROGRAMMED_DISC, variantNames);
        ModelLoader.setCustomMeshDefinition(PROGRAMMED_DISC, new ItemMeshDefinition() {
            @Override
            public ModelResourceLocation getModelLocation(ItemStack stack) {
                int variant = ProgrammedDiscItem.getVariant(stack);
                return variantLocations[variant];
            }
        });
    }

    @SideOnly(Side.CLIENT)
    private static void registerSimpleItemModel(Item item, String name) {
        ModelLoader.setCustomModelResourceLocation(
                item, 0, new ModelResourceLocation(new ResourceLocation(MOD_ID, name), "inventory"));
    }
}
