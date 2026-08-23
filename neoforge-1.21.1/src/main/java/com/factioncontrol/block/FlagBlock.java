package com.factioncontrol.block;

import com.factioncontrol.util.FlagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlagBlock extends Block {
    private static final float BEDROCK_EXPLOSION_RESISTANCE = 3600000.0F;

    /** Model spans x/z ~4-18 and y 0-32 (two blocks tall). */
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 18, 32, 12);

    public FlagBlock(Properties properties) {
        super(properties);
    }

    public static Properties createProperties() {
        return BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(-1.0F, BEDROCK_EXPLOSION_RESISTANCE)
                .lightLevel(state -> 15)
                .noOcclusion();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return BEDROCK_EXPLOSION_RESISTANCE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel && !state.is(newState.getBlock())) {
            FlagHelper.onFlagBlockRemoved(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public static boolean isFlagBlock(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof FlagBlock;
    }
}
