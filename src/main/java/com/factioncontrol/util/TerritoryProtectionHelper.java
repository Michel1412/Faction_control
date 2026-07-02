package com.factioncontrol.util;

import com.factioncontrol.config.FactionConfigManager;
import com.factioncontrol.faction.FactionObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * FTB-style territory permission checks against {@link FactionConfigManager} map caches.
 */
public final class TerritoryProtectionHelper {
    public static final String ADMIN_OWNER_NAME = "Administradores";

    public enum FactionRole {
        OFFICIAL,
        MEMBER,
        OUTSIDER
    }

    private TerritoryProtectionHelper() {
    }

    public record ProtectionDenial(String ownerDisplayName) {
    }

    /**
     * Resolves the player's role within a specific faction.
     */
    public static FactionRole getRole(UUID playerId, @Nullable UUID factionId) {
        if (factionId == null) {
            return FactionRole.OUTSIDER;
        }

        FactionObject faction = FactionConfigManager.getFaction(factionId);
        if (faction == null) {
            return FactionRole.OUTSIDER;
        }

        if (faction.isLeader(playerId)) {
            return FactionRole.OFFICIAL;
        }

        return faction.isMember(playerId) ? FactionRole.MEMBER : FactionRole.OUTSIDER;
    }

    /**
     * FTB-style: {@code true} when the player may edit or interact in the chunk (event cancellation).
     */
    public static boolean hasPermission(ServerPlayer player, ChunkPos chunkPos) {
        return evaluate(player, chunkPos) == null;
    }

    /**
     * Expected game mode for the chunk context.
     * SURVIVAL in faction-claimed chunks (including enemy territory); ADVENTURE in free zones and admin safezones.
     */
    public static GameType resolveExpectedGameMode(ServerPlayer player, ChunkPos chunkPos) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) {
            return GameType.SURVIVAL;
        }

        if (!PlayerPlayModeHelper.isSubjectToTerritoryRules(player)) {
            return GameType.SURVIVAL;
        }

        // Admin safezones: Adventure (baús liberados, quebra bloqueada pelo vanilla); PvP em AdminSafezoneHandler.
        if (FactionConfigManager.adminChunksSet.contains(chunkPos)) {
            return GameType.ADVENTURE;
        }

        UUID ownerId = FactionConfigManager.chunkToFactionMap.get(chunkPos);
        if (ownerId == null) {
            return GameType.ADVENTURE;
        }

        return GameType.SURVIVAL;
    }

    /**
     * {@code true} when the chunk is claimed by a player faction (not admin safezone).
     */
    public static boolean isFactionClaimedChunk(ChunkPos chunkPos) {
        if (isAdminChunk(chunkPos)) {
            return false;
        }
        return FactionConfigManager.chunkToFactionMap.containsKey(chunkPos);
    }

    /**
     * Whether the player may break the block at {@code pos}. Interaction/place rules use {@link #hasPermission}.
     */
    public static boolean canBreakBlock(
            ServerPlayer player,
            ChunkPos chunkPos,
            BlockGetter level,
            BlockPos pos,
            BlockState state
    ) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) {
            return true;
        }

        if (!PlayerPlayModeHelper.isSubjectToTerritoryRules(player)) {
            return true;
        }

        if (!isFactionClaimedChunk(chunkPos)) {
            return true;
        }

        UUID ownerId = FactionConfigManager.chunkToFactionMap.get(chunkPos);
        if (ownerId == null) {
            return true;
        }

        FactionRole role = getRole(player.getUUID(), ownerId);
        if (role == FactionRole.OFFICIAL || role == FactionRole.MEMBER) {
            return true;
        }

        return !BlockInventoryHelper.hasProtectableInventory(level, pos, state);
    }

    /**
     * Returns a denial when the player may not modify or interact in the chunk; null if allowed.
     */
    @Nullable
    public static ProtectionDenial evaluate(ServerPlayer player, ChunkPos chunkPos) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) {
            return null;
        }

        if (!PlayerPlayModeHelper.isSubjectToTerritoryRules(player)) {
            return null;
        }

        // Admin chunks: no block break/place/interact restriction (safezone rules handled by AdminSafezoneHandler).
        if (FactionConfigManager.adminChunksSet.contains(chunkPos)) {
            return null;
        }

        UUID ownerId = FactionConfigManager.chunkToFactionMap.get(chunkPos);
        if (ownerId == null) {
            return null;
        }

        if (FactionConfigManager.isFactionRaided(ownerId)) {
            return null;
        }

        FactionRole role = getRole(player.getUUID(), ownerId);
        if (role == FactionRole.OFFICIAL || role == FactionRole.MEMBER) {
            return null;
        }

        FactionObject ownerFaction = FactionConfigManager.getFaction(ownerId);
        if (ownerFaction == null) {
            return null;
        }

        return new ProtectionDenial(ownerFaction.getName());
    }

    public static boolean isProtectedChunk(ServerPlayer player, ChunkPos chunkPos) {
        return !hasPermission(player, chunkPos);
    }

    @Nullable
    public static FactionObject getEnemyFactionAtChunk(ServerPlayer player, ChunkPos chunkPos) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) {
            return null;
        }

        if (FactionConfigManager.adminChunksSet.contains(chunkPos)) {
            return null;
        }

        UUID ownerId = FactionConfigManager.chunkToFactionMap.get(chunkPos);
        if (ownerId == null || FactionConfigManager.isFactionRaided(ownerId)) {
            return null;
        }

        FactionRole role = getRole(player.getUUID(), ownerId);
        if (role == FactionRole.OFFICIAL || role == FactionRole.MEMBER) {
            return null;
        }

        return FactionConfigManager.getFaction(ownerId);
    }

    public static boolean isPlayerInsideFactionTerritory(ServerPlayer player, UUID factionId) {
        UUID chunkOwner = FactionConfigManager.chunkToFactionMap.get(player.chunkPosition());
        return factionId.equals(chunkOwner);
    }

    public static boolean isAdminChunk(ChunkPos chunkPos) {
        return FactionConfigManager.adminChunksSet.contains(chunkPos);
    }

    /**
     * Whether the chunk is claimed by the player's faction (domínio).
     */
    public static boolean isPlayerFactionChunk(UUID playerId, ChunkPos chunkPos) {
        UUID factionId = FactionConfigManager.playerToFactionMap.get(playerId);
        if (factionId == null) {
            return false;
        }
        return factionId.equals(FactionConfigManager.chunkToFactionMap.get(chunkPos));
    }

    /**
     * Whether a chunk can be selected or claimed via faction upgrade item.
     */
    public static boolean canRegisterChunkForUpgrade(ChunkPos chunkPos) {
        return !isAdminChunk(chunkPos);
    }

    @Nullable
    public static String describeChunkOwner(ChunkPos chunkPos) {
        if (FactionConfigManager.adminChunksSet.contains(chunkPos)) {
            return ADMIN_OWNER_NAME;
        }

        UUID ownerId = FactionConfigManager.chunkToFactionMap.get(chunkPos);
        if (ownerId == null) {
            return "Zona Livre";
        }

        FactionObject faction = FactionConfigManager.getFaction(ownerId);
        return faction != null ? faction.getName() : ownerId.toString();
    }
}
