package com.factioncontrol.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Detects blocks that store items and should not be broken by outsiders in faction territory.
 */
public final class BlockInventoryHelper {
    private BlockInventoryHelper() {
    }

    public static boolean hasProtectableInventory(BlockGetter level, BlockPos pos, BlockState state) {
        if (!state.hasBlockEntity()) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }

        if (blockEntity instanceof Container) {
            return true;
        }

        if (!(level instanceof Level world)) {
            return false;
        }

        IItemHandler handler = world.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, null);
        return handler != null;
    }
}
