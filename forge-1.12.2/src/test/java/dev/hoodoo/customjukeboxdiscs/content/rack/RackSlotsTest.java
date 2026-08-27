package dev.hoodoo.customjukeboxdiscs.content.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class RackSlotsTest {
    @Test
    void maps3x3Indices() {
        assertEquals(0, RackSlots.row(0));
        assertEquals(0, RackSlots.column(0));
        assertEquals(0, RackSlots.slot(0, 0));

        assertEquals(2, RackSlots.row(8));
        assertEquals(2, RackSlots.column(8));
        assertEquals(8, RackSlots.slot(2, 2));

        assertEquals(1, RackSlots.row(4));
        assertEquals(1, RackSlots.column(4));
        assertEquals(4, RackSlots.slot(1, 1));
    }
}
