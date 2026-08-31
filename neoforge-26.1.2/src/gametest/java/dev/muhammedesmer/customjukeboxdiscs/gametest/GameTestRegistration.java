package dev.muhammedesmer.customjukeboxdiscs.gametest;

import dev.muhammedesmer.customjukeboxdiscs.CustomJukeboxDiscs;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = CustomJukeboxDiscs.MOD_ID)
public final class GameTestRegistration {
    private static final Identifier EMPTY = id("empty");

    private GameTestRegistration() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("default"), new TestEnvironmentDefinition.AllOf(List.of()));

        register(event, environment, "rack_holds_nine_discs", DiscRackGameTests::rackHoldsNineDiscs);
        register(event, environment, "rack_only_accepts_discs", DiscRackGameTests::rackOnlyAcceptsDiscs);
        register(event, environment, "occupied_slot_refuses_a_second_disc", DiscRackGameTests::occupiedSlotRefusesASecondDisc);
        register(event, environment, "comparator_follows_how_full_the_rack_is", DiscRackGameTests::comparatorFollowsHowFullTheRackIs);
        register(event, environment, "rack_keeps_its_discs_across_reload", DiscRackGameTests::rackKeepsItsDiscsAcrossReload);

        register(event, environment, "writing_turns_one_blank_disc_into_a_programmed_disc", DiscWriterGameTests::writingTurnsOneBlankDiscIntoAProgrammedDisc);
        register(event, environment, "writing_is_rejected_without_a_blank_disc", DiscWriterGameTests::writingIsRejectedWithoutABlankDisc);
        register(event, environment, "writing_is_rejected_when_the_input_changed", DiscWriterGameTests::writingIsRejectedWhenTheInputChanged);
        register(event, environment, "writing_is_rejected_for_an_already_programmed_disc", DiscWriterGameTests::writingIsRejectedForAnAlreadyProgrammedDisc);
        register(event, environment, "written_disc_survives_block_entity_reload", DiscWriterGameTests::writtenDiscSurvivesBlockEntityReload);

        register(event, environment, "vanilla_jukebox_accepts_a_programmed_disc", JukeboxGameTests::vanillaJukeboxAcceptsAProgrammedDisc);
        register(event, environment, "programmed_disc_drives_comparator_output_fifteen", JukeboxGameTests::programmedDiscDrivesComparatorOutputFifteen);
        register(event, environment, "ejecting_the_disc_empties_the_jukebox", JukeboxGameTests::ejectingTheDiscEmptiesTheJukebox);
        register(event, environment, "jukebox_still_accepts_vanilla_discs", JukeboxGameTests::jukeboxStillAcceptsVanillaDiscs);
        register(event, environment, "blank_disc_is_not_playable_in_a_jukebox", JukeboxGameTests::blankDiscIsNotPlayableInAJukebox);
        register(event, environment, "programmed_disc_keeps_its_track_across_save_and_load", JukeboxGameTests::programmedDiscKeepsItsTrackAcrossSaveAndLoad);
    }

    private static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String name,
            Consumer<GameTestHelper> test) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(environment, EMPTY, 100, 0, true, Rotation.NONE);
        event.registerTest(id(name), new DirectGameTestInstance(data, test));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CustomJukeboxDiscs.MOD_ID, path);
    }
}
