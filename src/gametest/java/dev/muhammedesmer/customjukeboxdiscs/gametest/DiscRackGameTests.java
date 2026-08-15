package dev.muhammedesmer.customjukeboxdiscs.gametest;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.muhammedesmer.customjukeboxdiscs.content.ModBlocks;
import dev.muhammedesmer.customjukeboxdiscs.content.ModDataComponents;
import dev.muhammedesmer.customjukeboxdiscs.content.ModItems;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.AudioFormat;
import dev.muhammedesmer.customjukeboxdiscs.content.disc.TrackReference;
import dev.muhammedesmer.customjukeboxdiscs.content.rack.DiscRackBlockEntity;
import dev.muhammedesmer.customjukeboxdiscs.content.rack.RackSlots;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CustomJukeboxDiscs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DiscRackGameTests {
    private static final String EMPTY = "empty";
    private static final BlockPos RACK = new BlockPos(1, 1, 1);

    private DiscRackGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void rackHoldsNineDiscs(GameTestHelper helper) {
        DiscRackBlockEntity rack = placeRack(helper);

        helper.assertValueEqual(rack.getContainerSize(), RackSlots.SIZE, "rack size");
        for (int slot = 0; slot < RackSlots.SIZE; slot++) {
            rack.setItem(slot, programmedDisc());
        }
        helper.assertFalse(rack.isEmpty(), "a filled rack is not empty");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void rackOnlyAcceptsDiscs(GameTestHelper helper) {
        DiscRackBlockEntity rack = placeRack(helper);

        helper.assertTrue(rack.canPlaceItem(0, new ItemStack(ModItems.BLANK_DISC.get())), "blank disc fits");
        helper.assertTrue(rack.canPlaceItem(0, programmedDisc()), "programmed disc fits");
        helper.assertFalse(rack.canPlaceItem(0, new ItemStack(Items.DIRT)), "dirt does not belong on a disc rack");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void occupiedSlotRefusesASecondDisc(GameTestHelper helper) {
        DiscRackBlockEntity rack = placeRack(helper);
        rack.setItem(0, programmedDisc());

        helper.assertFalse(rack.canPlaceItem(0, programmedDisc()), "an occupied slot is full");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void comparatorFollowsHowFullTheRackIs(GameTestHelper helper) {
        DiscRackBlockEntity rack = placeRack(helper);
        var level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(RACK);

        helper.assertValueEqual(level.getBlockState(absolute)
                .getAnalogOutputSignal(level, absolute), 0, "empty rack signal");
        for (int slot = 0; slot < RackSlots.SIZE; slot++) {
            rack.setItem(slot, programmedDisc());
        }
        helper.assertValueEqual(level.getBlockState(absolute)
                .getAnalogOutputSignal(level, absolute), 15, "full rack signal");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void rackKeepsItsDiscsAcrossReload(GameTestHelper helper) {
        DiscRackBlockEntity rack = placeRack(helper);
        TrackReference track = track();
        ItemStack disc = new ItemStack(ModItems.PROGRAMMED_DISC.get());
        disc.set(ModDataComponents.TRACK_REFERENCE, track);
        rack.setItem(4, disc);

        var saved = rack.saveWithFullMetadata(helper.getLevel().registryAccess());
        DiscRackBlockEntity reloaded = new DiscRackBlockEntity(
                helper.absolutePos(RACK), ModBlocks.DISC_RACK.get().defaultBlockState());
        reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());

        helper.assertValueEqual(
                reloaded.getItem(4).get(ModDataComponents.TRACK_REFERENCE), track, "persisted track");
        helper.succeed();
    }

    private static DiscRackBlockEntity placeRack(GameTestHelper helper) {
        helper.setBlock(RACK, ModBlocks.DISC_RACK.get());
        return helper.getBlockEntity(RACK);
    }

    private static ItemStack programmedDisc() {
        ItemStack stack = new ItemStack(ModItems.PROGRAMMED_DISC.get());
        stack.set(ModDataComponents.TRACK_REFERENCE, track());
        return stack;
    }

    private static TrackReference track() {
        return new TrackReference(
                "e".repeat(64),
                "Racked track",
                UUID.fromString("12345678-1234-5678-9234-567812345678"),
                "Player",
                5_000,
                AudioFormat.OGG);
    }
}
