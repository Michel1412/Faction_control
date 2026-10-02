package com.factioncontrol.block;

import com.factioncontrol.util.FlagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public class FlagBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final float BEDROCK_EXPLOSION_RESISTANCE = 3600000.0F;
    /** Positive so an authorized player can mine it. Explosions stay at bedrock resistance. */
    private static final float FLAG_DESTROY_TIME = 5.0F;

    /**
     * The Blockbench model is two blocks tall and sticks 2 pixels past +X.
     * Collision stays inside this block; the upper half occupies the block above.
     */
    private static final VoxelShape LOWER_SHAPE = Block.box(4, 0, 4, 16, 16, 12);
    private static final VoxelShape UPPER_SHAPE = Block.box(4, 0, 4, 16, 16, 12);

    public FlagBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    public static Properties createProperties() {
        return BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(FLAG_DESTROY_TIME, BEDROCK_EXPLOSION_RESISTANCE)
                .lightLevel(state -> 15)
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockPos above = pos.above();
        Level level = context.getLevel();
        if (above.getY() >= level.getMaxBuildHeight()) {
            return null;
        }
        if (!level.getBlockState(above).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return shapeFor(state);
    }

    private static VoxelShape shapeFor(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? UPPER_SHAPE : LOWER_SHAPE;
    }

    public static BlockPos anchorPos(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof FlagBlock && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return BEDROCK_EXPLOSION_RESISTANCE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockPos above = pos.above();
                BlockState aboveState = level.getBlockState(above);
                if (aboveState.is(this) && aboveState.getValue(HALF) == DoubleBlockHalf.UPPER) {
                    level.removeBlock(above, false);
                }
                FlagHelper.onFlagBlockRemoved(serverLevel, pos);
            } else {
                BlockPos below = pos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                    level.destroyBlock(below, true);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public static boolean isFlagBlock(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof FlagBlock;
    }
}
