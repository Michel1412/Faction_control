package com.factioncontrol.util;

import com.factioncontrol.config.FactionConfigManager;
import com.factioncontrol.faction.FactionObject;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FlagHelper {
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

        if (!level.setBlockAndUpdate(pos, ModBlocks.FLAG_BLOCK.get().defaultBlockState())) {
            return false;
        }

        manager.setFactionFlagBlockPos(
                faction.getFactionId(),
                pos,
                level.dimension().location().toString()
        );
        manager.claimSingleChunk(faction.getFactionId(), chunkPos);
        manager.forceSave();
        return true;
    }

    public static boolean hasActiveFlagInWorld(MinecraftServer server, FactionObject faction) {
        if (!faction.hasFlag()) {
            return false;
        }

        ServerLevel level = resolveFlagLevel(server, faction.getFlagDimension());
        if (level == null) {
            return false;
        }

        BlockPos flagPos = faction.getFlagBlockPos();
        if (flagPos != null && level.getBlockState(flagPos).is(ModBlocks.FLAG_BLOCK.get())) {
            return true;
        }

        ChunkPos flagChunk = faction.getFlagChunk();
        if (flagChunk != null) {
            return findFlagBlockPos(level, flagChunk) != null;
        }

        return false;
    }

    public static void removeFlagsInChunk(ServerLevel level, ChunkPos chunkPos) {
        forceLoadChunk(level, chunkPos);

        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(ModBlocks.FLAG_BLOCK.get())) {
                        level.destroyBlock(pos, false);
                    }
                }
            }
        }

        releaseChunk(level, chunkPos);
    }

    @Nullable
    public static FactionObject resolveFactionAtFlag(ServerLevel level, BlockPos pos) {
        FactionManager manager = FactionManager.get(level);

        for (FactionObject faction : manager.getAllFactions()) {
            if (faction.matchesRegisteredFlag(level, pos)) {
                return faction;
            }
        }

        return manager.getFactionByFlag(new ChunkPos(pos));
    }

    public static void clearFlagTerritory(ServerLevel level, BlockPos flagPos) {
        FactionObject faction = resolveFactionAtFlag(level, flagPos);
        if (faction == null) {
            UUID chunkOwner = FactionConfigManager.chunkToFactionMap.get(new ChunkPos(flagPos));
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
            FactionConfigManager.releaseFactionClaims(faction.getFactionId());
            faction.clearFlag();
            FactionConfigManager.setFlagBlockPos(faction.getFactionId(), null, null);
        }
        FactionConfigManager.forceSave();
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

    @Nullable
    public static BlockPos findFlagBlockPos(ServerLevel level, ChunkPos chunkPos) {
        FactionManager manager = FactionManager.get(level);
        for (FactionObject faction : manager.getAllFactions()) {
            BlockPos savedPos = faction.getFlagBlockPos();
            if (savedPos != null && new ChunkPos(savedPos).equals(chunkPos)) {
                if (level.getBlockState(savedPos).is(ModBlocks.FLAG_BLOCK.get())) {
                    return savedPos;
                }
            }
        }

        forceLoadChunk(level, chunkPos);

        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(ModBlocks.FLAG_BLOCK.get())) {
                        releaseChunk(level, chunkPos);
                        return pos;
                    }
                }
            }
        }

        releaseChunk(level, chunkPos);
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

    private static void forceLoadChunk(ServerLevel level, ChunkPos chunkPos) {
        level.getChunkSource().addRegionTicket(TicketType.FORCED, chunkPos, 2, chunkPos);
        level.getChunk(chunkPos.x, chunkPos.z);
    }

    private static void releaseChunk(ServerLevel level, ChunkPos chunkPos) {
        level.getChunkSource().removeRegionTicket(TicketType.FORCED, chunkPos, 2, chunkPos);
    }
}
