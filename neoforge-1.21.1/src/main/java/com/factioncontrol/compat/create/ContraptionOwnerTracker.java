package com.factioncontrol.compat.create;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which player assembled/activated a Create contraption.
 */
public final class ContraptionOwnerTracker {
    private static final long PENDING_TTL_TICKS = 200L;
    private static final Map<Integer, UUID> CONTRAPTION_OWNERS = new ConcurrentHashMap<>();
    private static final Map<ChunkPos, PendingOwner> PENDING_BY_CHUNK = new ConcurrentHashMap<>();

    private ContraptionOwnerTracker() {
    }

    private record PendingOwner(UUID ownerId, long expiresAtTick) {
        boolean isExpired(long currentTick) {
            return currentTick >= expiresAtTick;
        }
    }

    public static void setPendingOwner(UUID ownerId, ChunkPos chunkPos, long currentTick) {
        PENDING_BY_CHUNK.put(chunkPos, new PendingOwner(ownerId, currentTick + PENDING_TTL_TICKS));
    }

    public static void bindContraption(int entityId, UUID ownerId) {
        CONTRAPTION_OWNERS.put(entityId, ownerId);
    }

    public static void unbindContraption(int entityId) {
        CONTRAPTION_OWNERS.remove(entityId);
    }

    @Nullable
    public static UUID consumePendingOwner(ChunkPos chunkPos, long currentTick) {
        PendingOwner pending = PENDING_BY_CHUNK.get(chunkPos);
        if (pending == null || pending.isExpired(currentTick)) {
            PENDING_BY_CHUNK.remove(chunkPos);
            return null;
        }
        PENDING_BY_CHUNK.remove(chunkPos);
        return pending.ownerId();
    }

    @Nullable
    public static UUID getOwnerForContraption(int entityId) {
        return CONTRAPTION_OWNERS.get(entityId);
    }

    /**
     * Resolves contraption owner affecting a world position (same chunk as any tracked contraption entity).
     */
    @Nullable
    public static UUID resolveOwnerAt(ServerLevel level, ChunkPos chunkPos) {
        Class<?> contraptionClass = CreateReflection.contraptionEntityClass();
        if (contraptionClass == null) {
            return null;
        }

        AABB searchBox = new AABB(
                chunkPos.getMinBlockX(), level.getMinBuildHeight(), chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX() + 1, level.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 1
        );

        for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, searchBox, e -> contraptionClass.isInstance(e))) {
            UUID owner = CONTRAPTION_OWNERS.get(entity.getId());
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }

    public static void pruneExpired(long currentTick) {
        PENDING_BY_CHUNK.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTick));
    }
}
