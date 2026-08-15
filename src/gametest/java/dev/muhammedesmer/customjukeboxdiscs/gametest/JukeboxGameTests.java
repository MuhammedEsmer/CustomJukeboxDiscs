package dev.muhammedesmer.customjukeboxdiscs.gametest;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import dev.muhammedesmer.customjukeboxdiscs.content.ModItems;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CustomJukeboxDiscs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JukeboxGameTests {
    private static final String EMPTY = "empty";
    private static final BlockPos JUKEBOX = new BlockPos(1, 1, 1);

    private JukeboxGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void vanillaJukeboxAcceptsAProgrammedDisc(GameTestHelper helper) {
        JukeboxBlockEntity jukebox = placeJukebox(helper);

        jukebox.setTheItem(programmedDisc("Test track"));

        helper.assertBlockProperty(JUKEBOX, JukeboxBlock.HAS_RECORD, true);
        helper.assertTrue(jukebox.getTheItem().is(ModItems.PROGRAMMED_DISC.get()), "jukebox must hold the disc");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void programmedDiscDrivesComparatorOutputFifteen(GameTestHelper helper) {
        JukeboxBlockEntity jukebox = placeJukebox(helper);

        jukebox.setTheItem(programmedDisc("Test track"));

        helper.assertValueEqual(jukebox.getComparatorOutput(), 15, "comparator output");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void ejectingTheDiscEmptiesTheJukebox(GameTestHelper helper) {
        JukeboxBlockEntity jukebox = placeJukebox(helper);
        jukebox.setTheItem(programmedDisc("Test track"));

        jukebox.popOutTheItem();

        helper.assertTrue(jukebox.getTheItem().isEmpty(), "jukebox must be empty after ejecting");
        helper.assertBlockProperty(JUKEBOX, JukeboxBlock.HAS_RECORD, false);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void jukeboxStillAcceptsVanillaDiscs(GameTestHelper helper) {
        JukeboxBlockEntity jukebox = placeJukebox(helper);

        jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_13));

        helper.assertBlockProperty(JUKEBOX, JukeboxBlock.HAS_RECORD, true);
        helper.assertValueEqual(jukebox.getComparatorOutput(), 1, "vanilla comparator output");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void blankDiscIsNotPlayableInAJukebox(GameTestHelper helper) {
        ItemStack blank = new ItemStack(ModItems.BLANK_DISC.get());
        JukeboxBlockEntity jukebox = placeJukebox(helper);

        helper.assertFalse(jukebox.canPlaceItem(0, blank), "a blank disc must not be insertable");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void programmedDiscKeepsItsTrackAcrossSaveAndLoad(GameTestHelper helper) {
        JukeboxBlockEntity jukebox = placeJukebox(helper);
        TrackReference track = track("Persisted track");
        jukebox.setTheItem(programmedDisc(track));

        var saved = jukebox.saveWithFullMetadata(helper.getLevel().registryAccess());
        JukeboxBlockEntity reloaded = new JukeboxBlockEntity(
                helper.absolutePos(JUKEBOX), Blocks.JUKEBOX.defaultBlockState());
        reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());

        helper.assertValueEqual(reloaded.getTheItem().get(ModDataComponents.TRACK_REFERENCE), track, "track reference");
        helper.succeed();
    }

    private static JukeboxBlockEntity placeJukebox(GameTestHelper helper) {
        helper.setBlock(JUKEBOX, Blocks.JUKEBOX);
        return helper.getBlockEntity(JUKEBOX);
    }

    private static ItemStack programmedDisc(String title) {
        return programmedDisc(track(title));
    }

    private static ItemStack programmedDisc(TrackReference track) {
        ItemStack stack = new ItemStack(ModItems.PROGRAMMED_DISC.get());
        stack.set(ModDataComponents.TRACK_REFERENCE, track);
        return stack;
    }

    private static TrackReference track(String title) {
        return new TrackReference(
                "a".repeat(64),
                title,
                UUID.fromString("12345678-1234-5678-9234-567812345678"),
                "Player",
                5_000,
                AudioFormat.OGG);
    }
}
