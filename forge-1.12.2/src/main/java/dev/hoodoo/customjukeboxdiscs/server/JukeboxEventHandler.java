package dev.hoodoo.customjukeboxdiscs.server;

import dev.hoodoo.customjukeboxdiscs.client.ClientPlaybackPreference;
import dev.hoodoo.customjukeboxdiscs.client.transfer.ClientPlaybackManager;
import dev.hoodoo.customjukeboxdiscs.content.disc.ProgrammedDiscItem;
import dev.hoodoo.customjukeboxdiscs.content.disc.TrackReference;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class JukeboxEventHandler {

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);

        if (!(state.getBlock() instanceof BlockJukebox)) {
            return;
        }

        if (!world.isRemote) {
            boolean hasRecord = state.getValue(BlockJukebox.HAS_RECORD);
            ItemStack heldItem = event.getItemStack();

            if (hasRecord && event.getHand() == EnumHand.MAIN_HAND) {
                // Forge may fire the off-hand interaction after the main-hand disc was inserted.
                // Treating that second event as an ejection immediately cancels custom playback.
                ServerRuntime.stop(world, pos);
            } else if (heldItem.getItem() instanceof ProgrammedDiscItem) {
                // Inserted programmed disc
                TrackReference track = ProgrammedDiscItem.getTrackReference(heldItem);
                if (track != null) {
                    ServerRuntime.play(world, pos, track, 0);
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getWorld().isRemote && event.getState().getBlock() instanceof BlockJukebox) {
            ServerRuntime.stop(event.getWorld(), event.getPos());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerRuntime.serverTick();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            ServerRuntime.onPlayerLoggedOut((EntityPlayerMP) event.player);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientPlaybackManager.getInstance().getAudioEngine().tick();
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        ClientPlaybackPreference.publish();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientPlaybackManager.getInstance().reset();
    }
}
