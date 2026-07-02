package com.factioncontrol.util;

import com.factioncontrol.config.FactionConfigManager;
import com.factioncontrol.faction.FactionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;

public final class SafezoneHelper {
    private SafezoneHelper() {
    }

    public static boolean isAdminChunk(FactionManager manager, ChunkPos chunkPos) {
        return FactionConfigManager.adminChunksSet.contains(chunkPos);
    }

    public static boolean isAdminChunk(ServerLevel level, ChunkPos chunkPos) {
        return isAdminChunk(FactionManager.get(level), chunkPos);
    }

    public static boolean isAdminChunk(ServerLevel level, BlockPos pos) {
        return isAdminChunk(level, new ChunkPos(pos));
    }

    public static boolean isStaffProtectedZone(FactionManager manager, ChunkPos chunkPos) {
        return FactionConfigManager.adminChunksSet.contains(chunkPos);
    }

    public static boolean isStaffProtectedZone(ServerLevel level, ChunkPos chunkPos) {
        return isStaffProtectedZone(FactionManager.get(level), chunkPos);
    }

    @Nullable
    public static ServerLevel asServerLevel(net.minecraft.world.level.Level level) {
        return level instanceof ServerLevel serverLevel ? serverLevel : null;
    }
}
