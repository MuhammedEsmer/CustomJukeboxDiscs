package dev.hoodoo.customjukeboxdiscs.content.rack;

import dev.hoodoo.customjukeboxdiscs.CustomJukeboxDiscs;
import dev.hoodoo.customjukeboxdiscs.client.GuiHandler;
import dev.hoodoo.customjukeboxdiscs.content.ModCreativeTabs;
import dev.hoodoo.customjukeboxdiscs.content.ModItems;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BlockDiscRack extends BlockHorizontal {
    public BlockDiscRack() {
        super(Material.WOOD);
        setHardness(2.0F);
        setResistance(5.0F);
        setRegistryName(ModItems.MOD_ID, "disc_rack");
        setTranslationKey(ModItems.MOD_ID + ".disc_rack");
        setCreativeTab(ModCreativeTabs.TAB);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    public static boolean isDisc(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() == ModItems.PROGRAMMED_DISC || stack.getItem() == ModItems.BLANK_DISC);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityDiscRack();
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byHorizontalIndex(meta);
        return this.getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{FACING});
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityDiscRack)) {
            return false;
        }
        TileEntityDiscRack rack = (TileEntityDiscRack) te;
        ItemStack held = playerIn.getHeldItem(hand);

        if (playerIn.isSneaking()) {
            if (!worldIn.isRemote) {
                playerIn.openGui(CustomJukeboxDiscs.instance, GuiHandler.GUI_DISC_RACK, worldIn, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }

        Vec3d localHit = new Vec3d(hitX, hitY, hitZ);
        EnumFacing blockFacing = state.getValue(FACING);
        int slot = RackSlots.slotAt(blockFacing, facing, localHit);

        if (isDisc(held)) {
            if (slot >= 0 && rack.getStackInSlot(slot).isEmpty()) {
                if (!worldIn.isRemote) {
                    rack.setInventorySlotContents(slot, held.splitStack(1));
                    worldIn.playSound(null, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                return true;
            }
        }

        if (slot >= 0 && !rack.getStackInSlot(slot).isEmpty()) {
            if (!worldIn.isRemote) {
                ItemStack taken = rack.removeStackFromSlot(slot);
                if (!playerIn.inventory.addItemStackToInventory(taken)) {
                    playerIn.dropItem(taken, false);
                }
                worldIn.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }

        if (!worldIn.isRemote) {
            playerIn.openGui(CustomJukeboxDiscs.instance, GuiHandler.GUI_DISC_RACK, worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityDiscRack) {
            InventoryHelper.dropInventoryItems(worldIn, pos, (TileEntityDiscRack) te);
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityDiscRack)) {
            return 0;
        }
        TileEntityDiscRack rack = (TileEntityDiscRack) te;
        int filled = 0;
        for (int slot = 0; slot < rack.getSizeInventory(); slot++) {
            if (!rack.getStackInSlot(slot).isEmpty()) {
                filled++;
            }
        }
        return filled == 0 ? 0 : Math.max(1, filled * 15 / rack.getSizeInventory());
    }
}
