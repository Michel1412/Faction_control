package com.factioncontrol.faction;

import com.factioncontrol.config.FactionConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Thin facade over {@link FactionConfigManager} map lookups for server lifecycle and legacy call sites.
 */
public final class FactionManager {
    private static final FactionManager INSTANCE = new FactionManager();
    private static boolean serverDataReady;

    private FactionManager() {
    }

    public static FactionManager get(ServerLevel level) {
        return get(level.getServer());
    }

    public static FactionManager get(MinecraftServer server) {
        FactionConfigManager.bindServer(server);
        return INSTANCE;
    }

    public static boolean isServerDataReady() {
        return serverDataReady;
    }

    public static void reload(MinecraftServer server) {
        FactionConfigManager.load(server);
        markServerDataReady();
    }

    public static void shutdown(MinecraftServer server) {
        FactionConfigManager.forceSave();
        serverDataReady = false;
    }

    public static void markServerDataReady() {
        serverDataReady = true;
    }

    public void forceSave() {
        FactionConfigManager.forceSave();
    }

    public FactionObject createFaction(String name, int color, UUID founderId) {
        return FactionConfigManager.createFaction(name, color, founderId);
    }

    @Nullable
    public FactionObject getFaction(UUID factionId) {
        return FactionConfigManager.getFaction(factionId);
    }

    @Nullable
    public FactionObject getFactionByName(String name) {
        return FactionConfigManager.getFactionByName(name);
    }

    public boolean isNameTaken(String name) {
        return FactionConfigManager.isNameTaken(name);
    }

    public Collection<FactionObject> getAllFactions() {
        return FactionConfigManager.getAllFactions();
    }

    public boolean addMember(UUID factionId, UUID playerId) {
        return FactionConfigManager.addMember(factionId, playerId);
    }

    public boolean kickMember(UUID factionId, UUID targetPlayerId) {
        return FactionConfigManager.removeMember(factionId, targetPlayerId);
    }

    public boolean forceRemoveMember(UUID playerId) {
        return FactionConfigManager.forceRemoveMember(playerId);
    }

    public void forceJoinFaction(UUID playerId, UUID factionId) {
        FactionConfigManager.forceJoinFaction(playerId, factionId);
    }

    public boolean setFactionLeader(UUID factionId, UUID newLeaderId) {
        return FactionConfigManager.setFactionLeader(factionId, newLeaderId);
    }

    public void setFactionFlagBlockPos(UUID factionId, @Nullable BlockPos flagBlockPos) {
        setFactionFlagBlockPos(factionId, flagBlockPos, null);
    }

    public void setFactionFlagBlockPos(UUID factionId, @Nullable BlockPos flagBlockPos, @Nullable String dimension) {
        FactionConfigManager.setFlagBlockPos(factionId, flagBlockPos, dimension);
    }

    @Nullable
    public FactionObject getFactionByFlag(ChunkPos flagChunk) {
        return FactionConfigManager.getFactionByFlagChunk(flagChunk);
    }

    @Nullable
    public FactionObject getFactionOfMember(UUID playerId) {
        return FactionConfigManager.getFactionOfPlayer(playerId);
    }

    @Nullable
    public UUID getChunkOwner(ChunkPos chunkPos) {
        return FactionConfigManager.getChunkOwner(chunkPos);
    }

    public boolean isChunkClaimed(ChunkPos chunkPos) {
        return FactionConfigManager.isChunkClaimed(chunkPos);
    }

    public Set<ChunkPos> getFactionClaims(UUID factionId) {
        FactionObject faction = FactionConfigManager.getFaction(factionId);
        return faction != null ? new HashSet<>(faction.getClaimedChunks()) : Set.of();
    }

    public void releaseFactionClaims(UUID factionId) {
        FactionConfigManager.releaseFactionClaims(factionId);
    }

    public void forceClaimChunk(UUID factionId, ChunkPos chunkPos) {
        FactionConfigManager.forceClaimChunk(factionId, chunkPos);
    }

    public boolean claimSingleChunk(UUID factionId, ChunkPos chunkPos) {
        return FactionConfigManager.claimChunk(factionId, chunkPos);
    }

    public boolean removeSingleChunk(UUID factionId, ChunkPos chunkPos) {
        return FactionConfigManager.removeChunk(factionId, chunkPos);
    }

    public boolean isChunkAdjacentToFactionTerritory(UUID factionId, ChunkPos candidate) {
        return FactionConfigManager.isChunkAdjacentToFactionTerritory(factionId, candidate);
    }

    public boolean isChunkOwnedByFaction(UUID factionId, ChunkPos chunkPos) {
        return FactionConfigManager.isChunkOwnedByFaction(factionId, chunkPos);
    }

    public void setFactionFlagState(UUID factionId, FlagState flagState) {
        FactionConfigManager.setFlagState(factionId, flagState);
    }

    public boolean isAdminChunk(ChunkPos chunkPos) {
        return FactionConfigManager.isAdminChunk(chunkPos);
    }

    public boolean claimAdminChunk(ChunkPos chunkPos) {
        return FactionConfigManager.claimAdminChunk(chunkPos);
    }

    public boolean unclaimAdminChunk(ChunkPos chunkPos) {
        return FactionConfigManager.unclaimAdminChunk(chunkPos);
    }

    public Set<ChunkPos> getAdminChunks() {
        return FactionConfigManager.copyAdminChunks();
    }

    @Nullable
    public FactionObject deleteFaction(UUID factionId) {
        return FactionConfigManager.deleteFaction(factionId);
    }
}
