package com.factioncontrol.util;

import com.factioncontrol.config.FactionConfigManager;
import com.factioncontrol.faction.FactionObject;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.block.FlagBlock;
import com.factioncontrol.registry.ModBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.slf4j.Logger;

import org.jetbrains.annotations.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FlagHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Long> TERRITORY_CLEANUP_IN_PROGRESS = ConcurrentHashMap.newKeySet();

    private FlagHelper() {
    }

    public static boolean spawnFactionFlag(ServerPlayer player, FactionObject faction) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        BlockPos above = pos.above();
        ChunkPos chunkPos = new ChunkPos(pos);
        FactionManager manager = FactionManager.get(level);

        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        if (!level.getBlockState(above).canBeReplaced() && !level.getBlockState(above).isAir()) {
            return false;
        }

        BlockState lower = ModBlocks.FLAG_BLOCK.get().defaultBlockState().setValue(FlagBlock.HALF, DoubleBlockHalf.LOWER);
        if (!level.setBlockAndUpdate(pos, lower)) {
            return false;
        }
        if (!level.setBlockAndUpdate(above, lower.setValue(FlagBlock.HALF, DoubleBlockHalf.UPPER))) {
            level.removeBlock(pos, false);
            return false;
        }

        FactionConfigManager.withSinglePersist(() -> {
            manager.setFactionFlagBlockPos(
                    faction.getFactionId(),
                    pos,
                    level.dimension().location().toString()
            );
            manager.claimSingleChunk(faction.getFactionId(), chunkPos);
        });
        return true;
    }

    /**
     * Flags saved before the upper half existed only occupy the anchor block.
     * Fill the block above when it is still replaceable.
     */
    public static void ensureUpperHalves(MinecraftServer server) {
        for (FactionObject faction : FactionConfigManager.getAllFactions()) {
            BlockPos flagPos = faction.getFlagBlockPos();
            if (flagPos == null) {
                continue;
            }
            ServerLevel level = resolveFlagLevel(server, faction.getFlagDimension());
            if (level == null) {
                continue;
            }
            BlockState state = level.getBlockState(flagPos);
            if (!state.is(ModBlocks.FLAG_BLOCK.get()) || state.getValue(FlagBlock.HALF) == DoubleBlockHalf.UPPER) {
                continue;
            }
            BlockPos above = flagPos.above();
            BlockState aboveState = level.getBlockState(above);
            if (aboveState.is(ModBlocks.FLAG_BLOCK.get()) && aboveState.getValue(FlagBlock.HALF) == DoubleBlockHalf.UPPER) {
                continue;
            }
            if (!aboveState.canBeReplaced()) {
                LOGGER.warn(
                        "Faction {} flag at {} is missing its upper half because {} is occupied",
                        faction.getName(),
                        flagPos,
                        above
                );
                continue;
            }
            level.setBlock(above, state.setValue(FlagBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    public static boolean hasActiveFlagInWorld(MinecraftServer server, FactionObject faction) {
        BlockPos flagPos = faction.getFlagBlockPos();
        if (flagPos == null) {
            return false;
        }

        ServerLevel level = resolveFlagLevel(server, faction.getFlagDimension());
        if (level == null) {
            return false;
        }

        return level.getBlockState(flagPos).is(ModBlocks.FLAG_BLOCK.get());
    }

    public static void removeFlagsInChunk(ServerLevel level, ChunkPos chunkPos) {
        BlockPos savedPos = findFlagBlockPos(level, chunkPos);
        if (savedPos != null && level.getBlockState(savedPos).is(ModBlocks.FLAG_BLOCK.get())) {
            level.destroyBlock(savedPos, false);
        }
    }

    @Nullable
    public static FactionObject resolveFactionAtFlag(ServerLevel level, BlockPos pos) {
        FactionManager manager = FactionManager.get(level);
        FactionObject byFlagChunk = manager.getFactionByFlag(new ChunkPos(pos));
        if (byFlagChunk != null) {
            return byFlagChunk;
        }
        for (FactionObject faction : manager.getAllFactions()) {
            if (faction.matchesRegisteredFlag(level, pos)) {
                return faction;
            }
        }
        return null;
    }

    public static void clearFlagTerritory(ServerLevel level, BlockPos flagPos) {
        FactionObject faction = resolveFactionAtFlag(level, flagPos);
        if (faction == null) {
            UUID chunkOwner = FactionConfigManager.getChunkOwner(new ChunkPos(flagPos));
            if (chunkOwner != null) {
                FactionManager.get(level).releaseFactionClaims(chunkOwner);
            }
            return;
        }

        ChunkPos flagChunk = faction.getFlagChunk();
        if (flagChunk != null && flagChunk.equals(new ChunkPos(flagPos))) {
            faction.clearFlag();
            FactionConfigManager.setFlagBlockPos(faction.getFactionId(), null, null);
        } else {
            FactionConfigManager.withSinglePersist(() -> {
                FactionConfigManager.releaseFactionClaims(faction.getFactionId());
                faction.clearFlag();
                FactionConfigManager.setFlagBlockPos(faction.getFactionId(), null, null);
            });
        }
    }

    public static void onFlagBlockRemoved(ServerLevel level, BlockPos pos) {
        long key = pos.asLong();
        if (!TERRITORY_CLEANUP_IN_PROGRESS.add(key)) {
            return;
        }
        try {
            clearFlagTerritory(level, pos);
        } finally {
            TERRITORY_CLEANUP_IN_PROGRESS.remove(key);
        }
    }

    public static void stripFactionFlag(MinecraftServer server, FactionObject faction) {
        ServerLevel level = resolveFlagLevel(server, faction.getFlagDimension());
        if (level == null) {
            return;
        }

        BlockPos flagPos = faction.getFlagBlockPos();
        if (flagPos == null) {
            ChunkPos flagChunk = faction.getFlagChunk();
            if (flagChunk != null) {
                flagPos = findFlagBlockPos(level, flagChunk);
            }
        }

        if (flagPos != null && level.getBlockState(flagPos).is(ModBlocks.FLAG_BLOCK.get())) {
            level.destroyBlock(flagPos, false);
        }
    }

    /**
     * Resolves a flag from saved {@code flag_position} only — no full-chunk Y scan.
     */
    @Nullable
    public static BlockPos findFlagBlockPos(ServerLevel level, ChunkPos chunkPos) {
        FactionObject faction = FactionConfigManager.getFactionByFlagChunk(chunkPos);
        if (faction == null) {
            return null;
        }
        BlockPos savedPos = faction.getFlagBlockPos();
        if (savedPos == null || !new ChunkPos(savedPos).equals(chunkPos)) {
            return null;
        }
        if (level.getBlockState(savedPos).is(ModBlocks.FLAG_BLOCK.get())) {
            return savedPos;
        }
        return null;
    }

    @Nullable
    private static ServerLevel resolveFlagLevel(MinecraftServer server, String dimensionId) {
        ResourceLocation location = ResourceLocation.tryParse(dimensionId);
        if (location == null) {
            return null;
        }
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, location);
        return server.getLevel(dimensionKey);
    }
}
