package com.factioncontrol.registry;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.block.FlagBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FactionControlMod.MODID);

    public static final RegistryObject<FlagBlock> FLAG_BLOCK = BLOCKS.register(
            "flag_block",
            () -> new FlagBlock(FlagBlock.properties())
    );

    private ModBlocks() {
    }
}
