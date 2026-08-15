package dev.muhammedesmer.customjukeboxdiscs.content.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class RackSlotsTest {
    @Test
    void ignoresEveryFaceExceptTheFront() {
        assertTrue(RackSlots.slotAt(Direction.NORTH, Direction.UP, new Vec3(0.5, 1.0, 0.5)).isEmpty());
        assertTrue(RackSlots.slotAt(Direction.NORTH, Direction.SOUTH, new Vec3(0.5, 0.5, 1.0)).isEmpty());
    }

    @Test
    void readsTheTopLeftCellOfANorthFacingRack() {
        // Seen from the north the block's right hand side runs towards decreasing x.
        assertEquals(OptionalInt.of(0), RackSlots.slotAt(Direction.NORTH, Direction.NORTH, new Vec3(0.9, 0.9, 0.0)));
    }

    @Test
    void readsTheBottomRightCellOfANorthFacingRack() {
        assertEquals(OptionalInt.of(8), RackSlots.slotAt(Direction.NORTH, Direction.NORTH, new Vec3(0.1, 0.1, 0.0)));
    }

    @Test
    void readsTheCentreCell() {
        assertEquals(OptionalInt.of(4), RackSlots.slotAt(Direction.NORTH, Direction.NORTH, new Vec3(0.5, 0.5, 0.0)));
    }

    @Test
    void mirrorsCorrectlyForEveryFacing() {
        assertEquals(OptionalInt.of(0), RackSlots.slotAt(Direction.SOUTH, Direction.SOUTH, new Vec3(0.1, 0.9, 1.0)));
        assertEquals(OptionalInt.of(0), RackSlots.slotAt(Direction.WEST, Direction.WEST, new Vec3(0.0, 0.9, 0.1)));
        assertEquals(OptionalInt.of(0), RackSlots.slotAt(Direction.EAST, Direction.EAST, new Vec3(1.0, 0.9, 0.9)));
    }

    @Test
    void keepsCoordinatesOnTheEdgeInsideTheGrid() {
        assertEquals(OptionalInt.of(2), RackSlots.slotAt(Direction.NORTH, Direction.NORTH, new Vec3(0.0, 1.0, 0.0)));
        assertEquals(OptionalInt.of(6), RackSlots.slotAt(Direction.NORTH, Direction.NORTH, new Vec3(1.0, 0.0, 0.0)));
    }

    @Test
    void everySlotHasAFrontFacePosition() {
        for (int slot = 0; slot < RackSlots.SIZE; slot++) {
            assertTrue(RackSlots.column(slot) >= 0 && RackSlots.column(slot) < 3);
            assertTrue(RackSlots.row(slot) >= 0 && RackSlots.row(slot) < 3);
        }
    }
}
