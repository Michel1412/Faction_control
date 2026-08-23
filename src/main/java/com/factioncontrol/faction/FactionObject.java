package com.factioncontrol.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory faction record stored by {@link com.factioncontrol.config.FactionConfigManager}.
 */
public class FactionObject {
    private final UUID factionId;
    private String name;
    private int color;
    private final Set<UUID> members = new HashSet<>();
    private final Set<ChunkPos> claimedChunks = new HashSet<>();
    @Nullable
    private UUID officialUuid;
    @Nullable
    private BlockPos flagBlockPos;
    private String flagDimension = "minecraft:overworld";
    private FlagState flagState = FlagState.ACTIVE;

    public FactionObject(UUID factionId, String name, int color) {
        this.factionId = factionId;
        this.name = name;
        this.color = color;
    }

    public UUID getFactionId() {
        return factionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public Set<ChunkPos> getClaimedChunks() {
        return Collections.unmodifiableSet(claimedChunks);
    }

    public Set<ChunkPos> getClaimedChunksMutable() {
        return claimedChunks;
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean addMember(UUID playerId) {
        return members.add(playerId);
    }

    public boolean removeMember(UUID playerId) {
        if (playerId.equals(officialUuid)) {
            officialUuid = null;
        }
        return members.remove(playerId);
    }

    @Nullable
    public UUID getOfficialUuid() {
        return officialUuid;
    }

    /** @deprecated use {@link #getOfficialUuid()} */
    @Nullable
    public UUID getLeaderId() {
        return officialUuid;
    }

    public boolean isLeader(UUID playerId) {
        return officialUuid != null && officialUuid.equals(playerId);
    }

    public void setOfficialUuid(@Nullable UUID officialUuid) {
        this.officialUuid = officialUuid;
    }

    /** @deprecated use {@link #setOfficialUuid(UUID)} */
    public void setLeaderId(@Nullable UUID leaderId) {
        setOfficialUuid(leaderId);
    }

    @Nullable
    public BlockPos getFlagBlockPos() {
        return flagBlockPos;
    }

    public void setFlagBlockPos(@Nullable BlockPos flagBlockPos) {
        this.flagBlockPos = flagBlockPos;
    }

    @Nullable
    public ChunkPos getFlagChunk() {
        return flagBlockPos != null ? new ChunkPos(flagBlockPos) : null;
    }

    public boolean hasFlag() {
        return flagBlockPos != null;
    }

    public void clearFlag() {
        flagBlockPos = null;
        flagState = FlagState.ACTIVE;
    }

    public String getFlagDimension() {
        return flagDimension;
    }

    public void setFlagDimension(String flagDimension) {
        this.flagDimension = flagDimension != null && !flagDimension.isBlank()
                ? flagDimension
                : "minecraft:overworld";
    }

    public boolean matchesRegisteredFlag(ServerLevel level, BlockPos pos) {
        if (flagBlockPos == null || !flagBlockPos.equals(pos)) {
            return false;
        }
        return flagDimension.equals(level.dimension().location().toString());
    }

    public FlagState getFlagState() {
        return flagState;
    }

    public void setFlagState(FlagState flagState) {
        this.flagState = flagState;
    }

    public boolean isRaided() {
        return flagState == FlagState.RAIDED;
    }

    public boolean ownsChunk(ChunkPos chunkPos) {
        return claimedChunks.contains(chunkPos);
    }

    public boolean claimChunk(ChunkPos chunkPos) {
        return claimedChunks.add(chunkPos);
    }

    public boolean unclaimChunk(ChunkPos chunkPos) {
        return claimedChunks.remove(chunkPos);
    }

    public void clearClaims() {
        claimedChunks.clear();
    }
}
