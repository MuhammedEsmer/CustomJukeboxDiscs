package dev.muhammedesmer.customjukeboxdiscs.content.rack;

import com.mojang.serialization.MapCodec;
import dev.muhammedesmer.customjukeboxdiscs.content.ModItems;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class DiscRackBlock extends BaseEntityBlock {
    public static final MapCodec<DiscRackBlock> CODEC = simpleCodec(DiscRackBlock::new);

    public DiscRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    public static boolean isDisc(ItemStack stack) {
        return stack.is(ModItems.PROGRAMMED_DISC.get()) || stack.is(ModItems.BLANK_DISC.get());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DiscRackBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Placing a disc into the slot the player pointed at. */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (player.isSecondaryUseActive() || !isDisc(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof DiscRackBlockEntity rack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        OptionalInt slot = hitSlot(state, pos, hit);
        if (slot.isEmpty() || !rack.getItem(slot.getAsInt()).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            rack.setItem(slot.getAsInt(), stack.split(1));
            level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** An empty hand takes the disc that was pointed at; anything else opens the rack. */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DiscRackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        OptionalInt slot = player.isSecondaryUseActive() ? OptionalInt.empty() : hitSlot(state, pos, hit);
        if (slot.isPresent() && !rack.getItem(slot.getAsInt()).isEmpty()) {
            if (!level.isClientSide) {
                ItemStack taken = rack.removeItem(slot.getAsInt(), 1);
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = state.getMenuProvider(level, pos);
            if (provider != null) {
                serverPlayer.openMenu(provider, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static OptionalInt hitSlot(BlockState state, BlockPos pos, BlockHitResult hit) {
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        return RackSlots.slotAt(state.getValue(HorizontalDirectionalBlock.FACING), hit.getDirection(), local);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DiscRackBlockEntity rack) {
            for (int slot = 0; slot < rack.getContainerSize(); slot++) {
                Block.popResource(level, pos, rack.getItem(slot));
            }
            rack.clearContent();
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof DiscRackBlockEntity rack)) {
            return 0;
        }
        int filled = 0;
        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            if (!rack.getItem(slot).isEmpty()) {
                filled++;
            }
        }
        return filled == 0 ? 0 : Math.max(1, filled * 15 / rack.getContainerSize());
    }
}
