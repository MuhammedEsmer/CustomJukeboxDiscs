package dev.muhammedesmer.customjukeboxdiscs.content.rack;

import java.util.OptionalInt;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The three by three grid of disc slots on the front face of a disc rack.
 *
 * <p>Slot 0 is the top left cell as the player sees it, so the horizontal axis is mirrored for the
 * facings whose front face is looked at from the opposite direction.
 */
public final class RackSlots {
    public static final int COLUMNS = 3;
    public static final int ROWS = 3;
    public static final int SIZE = COLUMNS * ROWS;

    private RackSlots() {
    }

    /** {@return the slot the player pointed at, or empty when they did not hit the front face} */
    public static OptionalInt slotAt(Direction facing, Direction hitFace, Vec3 localHit) {
        if (hitFace != facing) {
            return OptionalInt.empty();
        }
        double across = switch (facing) {
            case NORTH -> 1.0 - localHit.x();
            case SOUTH -> localHit.x();
            case WEST -> localHit.z();
            case EAST -> 1.0 - localHit.z();
            default -> -1.0;
        };
        if (across < 0.0) {
            return OptionalInt.empty();
        }
        int column = Mth.clamp((int) (across * COLUMNS), 0, COLUMNS - 1);
        int row = Mth.clamp((int) ((1.0 - localHit.y()) * ROWS), 0, ROWS - 1);
        return OptionalInt.of(row * COLUMNS + column);
    }

    public static int column(int slot) {
        return slot % COLUMNS;
    }

    public static int row(int slot) {
        return slot / COLUMNS;
    }
}
