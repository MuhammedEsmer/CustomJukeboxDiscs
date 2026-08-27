package dev.hoodoo.customjukeboxdiscs.client;

import dev.hoodoo.customjukeboxdiscs.client.screen.GuiDiscRack;
import dev.hoodoo.customjukeboxdiscs.client.screen.GuiDiscWriter;
import dev.hoodoo.customjukeboxdiscs.content.rack.ContainerDiscRack;
import dev.hoodoo.customjukeboxdiscs.content.rack.TileEntityDiscRack;
import dev.hoodoo.customjukeboxdiscs.content.writer.ContainerDiscWriter;
import dev.hoodoo.customjukeboxdiscs.content.writer.TileEntityDiscWriter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
    public static final int GUI_DISC_WRITER = 0;
    public static final int GUI_DISC_RACK = 1;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        if (ID == GUI_DISC_WRITER && te instanceof TileEntityDiscWriter) {
            return new ContainerDiscWriter(player.inventory, (TileEntityDiscWriter) te);
        }
        if (ID == GUI_DISC_RACK && te instanceof TileEntityDiscRack) {
            return new ContainerDiscRack(player.inventory, (TileEntityDiscRack) te);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        if (ID == GUI_DISC_WRITER && te instanceof TileEntityDiscWriter) {
            return new GuiDiscWriter(player.inventory, (TileEntityDiscWriter) te);
        }
        if (ID == GUI_DISC_RACK && te instanceof TileEntityDiscRack) {
            return new GuiDiscRack(player.inventory, (TileEntityDiscRack) te);
        }
        return null;
    }
}
