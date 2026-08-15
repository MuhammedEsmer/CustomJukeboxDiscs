package dev.muhammedesmer.customjukeboxdiscs.gametest;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.ModBlocks;
import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import dev.muhammedesmer.customjukeboxdiscs.content.ModItems;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import dev.muhammedesmer.customjukeboxdiscs.content.writer.DiscWriterBlockEntity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CustomJukeboxDiscs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DiscWriterGameTests {
    private static final String EMPTY = "empty";
    private static final BlockPos WRITER = new BlockPos(1, 1, 1);

    private DiscWriterGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void writingTurnsOneBlankDiscIntoAProgrammedDisc(GameTestHelper helper) {
        DiscWriterBlockEntity writer = placeWriter(helper);
        writer.setItem(0, new ItemStack(ModItems.BLANK_DISC.get()));
        TrackReference track = track();

        helper.assertTrue(writer.writeDisc(writer.inputFingerprint(), track), "write must succeed");

        ItemStack result = writer.getItem(0);
        helper.assertTrue(result.is(ModItems.PROGRAMMED_DISC.get()), "slot must hold a programmed disc");
        helper.assertValueEqual(result.get(ModDataComponents.TRACK_REFERENCE), track, "track reference");
        helper.assertValueEqual(result.getCount(), 1, "programmed disc count");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void writingIsRejectedWithoutABlankDisc(GameTestHelper helper) {
        DiscWriterBlockEntity writer = placeWriter(helper);

        helper.assertFalse(writer.writeDisc(writer.inputFingerprint(), track()), "empty writer must reject");
        helper.assertTrue(writer.getItem(0).isEmpty(), "slot must stay empty");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void writingIsRejectedWhenTheInputChanged(GameTestHelper helper) {
        DiscWriterBlockEntity writer = placeWriter(helper);
        writer.setItem(0, new ItemStack(ModItems.BLANK_DISC.get()));
        long fingerprint = writer.inputFingerprint();
        writer.setItem(0, new ItemStack(ModItems.BLANK_DISC.get(), 2));

        helper.assertFalse(writer.writeDisc(fingerprint, track()), "changed input must reject");
        helper.assertTrue(writer.getItem(0).is(ModItems.BLANK_DISC.get()), "blank discs must be untouched");
        helper.assertValueEqual(writer.getItem(0).getCount(), 2, "blank disc count");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void writingIsRejectedForAnAlreadyProgrammedDisc(GameTestHelper helper) {
        DiscWriterBlockEntity writer = placeWriter(helper);
        ItemStack programmed = new ItemStack(ModItems.PROGRAMMED_DISC.get());
        programmed.set(ModDataComponents.TRACK_REFERENCE, track());
        writer.setItem(0, programmed);

        helper.assertFalse(writer.writeDisc(writer.inputFingerprint(), track()), "only blank discs may be written");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void writtenDiscSurvivesBlockEntityReload(GameTestHelper helper) {
        DiscWriterBlockEntity writer = placeWriter(helper);
        writer.setItem(0, new ItemStack(ModItems.BLANK_DISC.get()));
        TrackReference track = track();
        writer.writeDisc(writer.inputFingerprint(), track);

        var saved = writer.saveWithFullMetadata(helper.getLevel().registryAccess());
        DiscWriterBlockEntity reloaded = new DiscWriterBlockEntity(
                helper.absolutePos(WRITER), ModBlocks.DISC_WRITER.get().defaultBlockState());
        reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());

        helper.assertValueEqual(
                reloaded.getItem(0).get(ModDataComponents.TRACK_REFERENCE), track, "persisted track reference");
        helper.succeed();
    }

    private static DiscWriterBlockEntity placeWriter(GameTestHelper helper) {
        helper.setBlock(WRITER, ModBlocks.DISC_WRITER.get());
        return helper.getBlockEntity(WRITER);
    }

    private static TrackReference track() {
        return new TrackReference(
                "b".repeat(64),
                "Written track",
                UUID.fromString("12345678-1234-5678-9234-567812345678"),
                "Player",
                5_000,
                AudioFormat.MP3);
    }
}
