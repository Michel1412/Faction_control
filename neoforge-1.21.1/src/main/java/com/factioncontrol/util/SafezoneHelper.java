package com.factioncontrol.util;

import com.factioncontrol.config.FactionConfigManager;
import com.factioncontrol.faction.FactionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import org.jetbrains.annotations.Nullable;

public final class SafezoneHelper {
    private SafezoneHelper() {
    }

    public static boolean isAdminChunk(FactionManager manager, ChunkPos chunkPos) {
        return FactionConfigManager.isAdminChunk(chunkPos);
    }

    public static boolean isAdminChunk(ServerLevel level, ChunkPos chunkPos) {
        return FactionConfigManager.isAdminChunk(chunkPos);
    }

    public static boolean isAdminChunk(ServerLevel level, BlockPos pos) {
        return isAdminChunk(level, new ChunkPos(pos));
    }

    public static boolean isStaffProtectedZone(FactionManager manager, ChunkPos chunkPos) {
        return FactionConfigManager.isAdminChunk(chunkPos);
    }

    public static boolean isStaffProtectedZone(ServerLevel level, ChunkPos chunkPos) {
        return FactionConfigManager.isAdminChunk(chunkPos);
    }

    @Nullable
    public static ServerLevel asServerLevel(net.minecraft.world.level.Level level) {
        return level instanceof ServerLevel serverLevel ? serverLevel : null;
    }
}
