package dev.hoodoo.customjukeboxdiscs.content.writer;

import dev.hoodoo.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.hoodoo.customjukeboxdiscs.client.GuiHandler;
import dev.hoodoo.customjukeboxdiscs.content.ModCreativeTabs;
import dev.hoodoo.customjukeboxdiscs.content.ModItems;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockDiscWriter extends Block {
    public BlockDiscWriter() {
        super(Material.IRON);
        setHardness(2.5F);
        setResistance(10.0F);
        setRegistryName(ModItems.MOD_ID, "disc_writer");
        setTranslationKey(ModItems.MOD_ID + ".disc_writer");
        setCreativeTab(ModCreativeTabs.TAB);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityDiscWriter();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            playerIn.openGui(CustomJukeboxDiscs.instance, GuiHandler.GUI_DISC_WRITER, worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityDiscWriter) {
            InventoryHelper.dropInventoryItems(worldIn, pos, (TileEntityDiscWriter) te);
        }
        super.breakBlock(worldIn, pos, state);
    }
}
