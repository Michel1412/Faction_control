package com.factioncontrol.registry;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.block.FlagBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(FactionControlMod.MODID);

    public static final DeferredBlock<FlagBlock> FLAG_BLOCK = BLOCKS.register(
            "flag_block",
            () -> new FlagBlock(FlagBlock.createProperties())
    );

    private ModBlocks() {
    }
}
