package dev.hoodoo.customjukeboxdiscs.content.rack;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RackSlots {
    public static final int COLUMNS = 3;
    public static final int ROWS = 3;
    public static final int SIZE = COLUMNS * ROWS;

    private RackSlots() {
    }

    public static int slotAt(EnumFacing facing, EnumFacing hitFace, Vec3d localHit) {
        if (hitFace != facing) {
            return -1;
        }
        double across;
        switch (facing) {
            case NORTH:
                across = 1.0D - localHit.x;
                break;
            case SOUTH:
                across = localHit.x;
                break;
            case WEST:
                across = localHit.z;
                break;
            case EAST:
                across = 1.0D - localHit.z;
                break;
            default:
                across = -1.0D;
                break;
        }
        if (across < 0.0D) {
            return -1;
        }
        int column = MathHelper.clamp((int) (across * COLUMNS), 0, COLUMNS - 1);
        int row = MathHelper.clamp((int) ((1.0D - localHit.y) * ROWS), 0, ROWS - 1);
        return row * COLUMNS + column;
    }

    public static int column(int slot) {
        return slot % COLUMNS;
    }

    public static int row(int slot) {
        return slot / COLUMNS;
    }

    public static int slot(int row, int column) {
        return row * COLUMNS + column;
    }
}
