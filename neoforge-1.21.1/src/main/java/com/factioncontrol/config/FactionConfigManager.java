package com.factioncontrol.config;

import com.factioncontrol.faction.FactionInviteManager;
import com.factioncontrol.faction.FactionObject;
import com.factioncontrol.faction.FlagState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Single source of truth: JSON is mirrored into in-memory maps for O(1) lookups.
 */
public final class FactionConfigManager {
    public static final String CONFIG_FILE_NAME = "faction_control.json";

    private static final Map<UUID, FactionObject> factionsMap = new HashMap<>();
    private static final Map<ChunkPos, UUID> chunkToFactionMap = new HashMap<>();
    private static final Map<UUID, UUID> playerToFactionMap = new HashMap<>();
    private static final Map<String, UUID> nameToFactionMap = new HashMap<>();
    private static final Map<Long, UUID> flagChunkToFactionMap = new HashMap<>();
    private static final Set<ChunkPos> adminChunksSet = new HashSet<>();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static long lastKnownModified = -1L;

    @Nullable
    private static MinecraftServer boundServer;
    private static int persistBatchDepth;

    private FactionConfigManager() {
    }

    public static void bindServer(MinecraftServer server) {
        boundServer = server;
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME);
    }

    public static void load(MinecraftServer server) {
        bindServer(server);
        clearAllMaps();
        FactionInviteManager.clear();
        Path path = getConfigPath();

        if (!Files.exists(path)) {
            LOGGER.info("Faction Control config not found at {}, creating empty template", path);
            persistToDisk();
            updateLastModified(path);
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            FactionConfigRoot root = GSON.fromJson(reader, FactionConfigRoot.class);
            if (root == null) {
                LOGGER.warn("Faction Control config at {} is empty, using defaults", path);
                rebuildDerivedMaps();
                updateLastModified(path);
                return;
            }
            applyRoot(root);
            rebuildDerivedMaps();
            LOGGER.info("Faction Control loaded {} faction(s) from {}", factionCount(), path);
            updateLastModified(path);
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load Faction Control config from {}", path, exception);
            rebuildDerivedMaps();
        }
    }

    public static void clearAllMaps() {
        factionsMap.clear();
        chunkToFactionMap.clear();
        playerToFactionMap.clear();
        nameToFactionMap.clear();
        flagChunkToFactionMap.clear();
        adminChunksSet.clear();
    }

    public static void rebuildDerivedMaps() {
        chunkToFactionMap.clear();
        playerToFactionMap.clear();
        nameToFactionMap.clear();
        flagChunkToFactionMap.clear();

        for (FactionObject faction : factionsMap.values()) {
            UUID factionId = faction.getFactionId();
            nameToFactionMap.put(normalizeName(faction.getName()), factionId);
            ChunkPos flagChunk = faction.getFlagChunk();
            if (flagChunk != null) {
                flagChunkToFactionMap.put(flagChunk.toLong(), factionId);
            }
            for (UUID memberId : faction.getMembers()) {
                playerToFactionMap.put(memberId, factionId);
            }
            if (faction.getOfficialUuid() != null) {
                playerToFactionMap.put(faction.getOfficialUuid(), factionId);
            }
            for (ChunkPos chunkPos : faction.getClaimedChunks()) {
                chunkToFactionMap.put(chunkPos, factionId);
            }
        }
    }

    /**
     * Runs mutations with a single JSON write. Nested calls still write once at the outermost exit.
     */
    public static void withSinglePersist(Runnable action) {
        persistBatchDepth++;
        try {
            action.run();
        } finally {
            persistBatchDepth--;
            if (persistBatchDepth == 0) {
                persistToDisk();
            }
        }
    }

    private static void persist() {
        rebuildDerivedMaps();
        if (persistBatchDepth == 0) {
            persistToDisk();
        }
    }

    private static void persistToDisk() {
        if (boundServer == null) {
            return;
        }
        Path path = getConfigPath();
        FactionConfigRoot root = buildRoot();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            updateLastModified(path);
        } catch (IOException exception) {
            LOGGER.error("Failed to save Faction Control config to {}", path, exception);
        }
    }

    public static void forceSave() {
        persist();
    }

    public static void checkAndReloadIfModified(MinecraftServer server) {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (lastKnownModified >= 0 && modified > lastKnownModified) {
                LOGGER.info("External change detected in {}, reloading factions", path);
                load(server);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not check modification time for {}", path, exception);
        }
    }

    public static int factionCount() {
        return factionsMap.size();
    }

    public static boolean isAdminChunk(ChunkPos chunkPos) {
        return adminChunksSet.contains(chunkPos);
    }

    public static Set<ChunkPos> copyAdminChunks() {
        return new HashSet<>(adminChunksSet);
    }

    @Nullable
    public static UUID getPlayerFactionId(UUID playerId) {
        return playerToFactionMap.get(playerId);
    }

    @Nullable
    public static FactionObject getFaction(UUID factionId) {
        return factionsMap.get(factionId);
    }

    @Nullable
    public static FactionObject getFactionByName(String name) {
        UUID factionId = nameToFactionMap.get(normalizeName(name));
        return factionId != null ? factionsMap.get(factionId) : null;
    }

    @Nullable
    public static FactionObject getFactionOfPlayer(UUID playerId) {
        UUID factionId = playerToFactionMap.get(playerId);
        return factionId != null ? factionsMap.get(factionId) : null;
    }

    @Nullable
    public static UUID getChunkOwner(ChunkPos chunkPos) {
        return chunkToFactionMap.get(chunkPos);
    }

    public static boolean isChunkClaimed(ChunkPos chunkPos) {
        return chunkToFactionMap.containsKey(chunkPos);
    }

    public static boolean isFactionRaided(@Nullable UUID factionId) {
        if (factionId == null) {
            return false;
        }
        FactionObject faction = factionsMap.get(factionId);
        return faction != null && faction.isRaided();
    }

    public static Collection<FactionObject> getAllFactions() {
        return factionsMap.values();
    }

    public static boolean isNameTaken(String name) {
        return getFactionByName(name) != null;
    }

    public static FactionObject createFaction(String name, int color, UUID founderId) {
        UUID factionId = UUID.randomUUID();
        FactionObject faction = new FactionObject(factionId, name, color);
        faction.addMember(founderId);
        faction.setOfficialUuid(founderId);
        factionsMap.put(factionId, faction);
        persist();
        return faction;
    }

    @Nullable
    public static FactionObject deleteFaction(UUID factionId) {
        FactionObject removed = factionsMap.remove(factionId);
        if (removed != null) {
            persist();
        }
        return removed;
    }

    public static boolean addMember(UUID factionId, UUID playerId) {
        FactionObject faction = factionsMap.get(factionId);
        if (faction == null || faction.isMember(playerId)) {
            return false;
        }
        removePlayerFromAllFactions(playerId);
        faction.addMember(playerId);
        persist();
        return true;
    }

    public static boolean removeMember(UUID factionId, UUID playerId) {
        FactionObject faction = factionsMap.get(factionId);
        if (faction == null || !faction.isMember(playerId)) {
            return false;
        }
        faction.removeMember(playerId);
        persist();
        return true;
    }

    public static boolean forceRemoveMember(UUID playerId) {
        UUID factionId = playerToFactionMap.get(playerId);
        if (factionId == null) {
            return false;
        }
        return removeMember(factionId, playerId);
    }

    public static void forceJoinFaction(UUID playerId, UUID factionId) {
        if (!factionsMap.containsKey(factionId)) {
            return;
        }
        forceRemoveMember(playerId);
        addMember(factionId, playerId);
    }

    public static boolean setFactionLeader(UUID factionId, UUID newLeaderId) {
        FactionObject faction = factionsMap.get(factionId);
        if (faction == null || !faction.isMember(newLeaderId)) {
            return false;
        }
        faction.setOfficialUuid(newLeaderId);
        persist();
        return true;
    }

    public static void setFlagBlockPos(UUID factionId, @Nullable BlockPos pos, @Nullable String dimension) {
        FactionObject faction = factionsMap.get(factionId);
        if (faction == null) {
            return;
        }
        faction.setFlagBlockPos(pos);
        if (dimension != null) {
            faction.setFlagDimension(dimension);
        }
        persist();
    }

    public static void setFlagState(UUID factionId, FlagState state) {
        FactionObject faction = factionsMap.get(factionId);
        if (faction == null) {
            return;
        }
        faction.setFlagState(state);
        persist();
    }

    public static boolean claimChunk(UUID factionId, ChunkPos chunkPos) {
        if (chunkToFactionMap.containsKey(chunkPos)) {
            return false;
        }
        FactionObject faction = factionsMap.get(factionId);
        if (faction == null) {
            return false;
        }
        faction.claimChunk(chunkPos);
        persist();
        return true;
    }

    public static boolean forceClaimChunk(UUID factionId, ChunkPos chunkPos) {
        FactionObject faction = factionsMap.get(factionId);
        if (faction == null) {
            return false;
        }
        UUID previousOwner = chunkToFactionMap.get(chunkPos);
        if (previousOwner != null && !previousOwner.equals(factionId)) {
            FactionObject previous = factionsMap.get(previousOwner);
            if (previous != null) {
                previous.unclaimChunk(chunkPos);
            }
        }
        faction.claimChunk(chunkPos);
        persist();
        return true;
    }

    public static boolean removeChunk(UUID factionId, ChunkPos chunkPos) {
        FactionObject faction = factionsMap.get(factionId);
        if (faction == null || !faction.ownsChunk(chunkPos)) {
            return false;
        }
        faction.unclaimChunk(chunkPos);
        persist();
        return true;
    }

    public static void releaseFactionClaims(UUID factionId) {
        FactionObject faction = factionsMap.get(factionId);
        if (faction != null) {
            faction.clearClaims();
            persist();
        }
    }

    public static boolean isChunkOwnedByFaction(UUID factionId, ChunkPos chunkPos) {
        return factionId.equals(chunkToFactionMap.get(chunkPos));
    }

    public static boolean isChunkAdjacentToFactionTerritory(UUID factionId, ChunkPos candidate) {
        FactionObject faction = factionsMap.get(factionId);
        if (faction == null) {
            return false;
        }
        Set<ChunkPos> territory = new HashSet<>(faction.getClaimedChunks());
        ChunkPos flagChunk = faction.getFlagChunk();
        if (flagChunk != null) {
            territory.add(flagChunk);
        }
        for (ChunkPos owned : territory) {
            if (areChunksAdjacent(owned, candidate)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static FactionObject getFactionByFlagChunk(ChunkPos flagChunk) {
        UUID factionId = flagChunkToFactionMap.get(flagChunk.toLong());
        return factionId != null ? factionsMap.get(factionId) : null;
    }

    public static boolean claimAdminChunk(ChunkPos chunkPos) {
        if (!adminChunksSet.add(chunkPos)) {
            return false;
        }
        persist();
        return true;
    }

    public static boolean unclaimAdminChunk(ChunkPos chunkPos) {
        if (!adminChunksSet.remove(chunkPos)) {
            return false;
        }
        persist();
        return true;
    }

    private static void removePlayerFromAllFactions(UUID playerId) {
        UUID currentFactionId = playerToFactionMap.get(playerId);
        if (currentFactionId != null) {
            FactionObject current = factionsMap.get(currentFactionId);
            if (current != null) {
                current.removeMember(playerId);
            }
        }
    }

    private static boolean areChunksAdjacent(ChunkPos first, ChunkPos second) {
        int deltaX = Math.abs(first.x - second.x);
        int deltaZ = Math.abs(first.z - second.z);
        return (deltaX == 1 && deltaZ == 0) || (deltaX == 0 && deltaZ == 1);
    }

    private static void applyRoot(FactionConfigRoot root) {
        if (root.factions != null) {
            for (FactionConfigEntry entry : root.factions) {
                FactionObject faction = toFactionObject(entry);
                if (faction != null) {
                    factionsMap.put(faction.getFactionId(), faction);
                }
            }
        }
        if (root.admin_chunks != null) {
            for (ChunkConfig chunk : root.admin_chunks) {
                adminChunksSet.add(new ChunkPos(chunk.x, chunk.z));
            }
        }
        if (root.pending_invites != null) {
            Map<UUID, FactionInviteManager.Invite> loaded = new HashMap<>();
            for (PendingInviteConfig entry : root.pending_invites) {
                UUID targetId = parseUuid(entry.target_uuid, "invite target");
                UUID factionId = parseUuid(entry.faction_uuid, "invite faction");
                UUID inviterId = parseUuid(entry.inviter_uuid, "invite inviter");
                if (targetId == null || factionId == null || inviterId == null) {
                    continue;
                }
                if (!factionsMap.containsKey(factionId)) {
                    continue;
                }
                loaded.put(targetId, new FactionInviteManager.Invite(factionId, inviterId, entry.expires_at_ms));
            }
            FactionInviteManager.replaceAll(loaded);
        }
    }

    private static FactionConfigRoot buildRoot() {
        FactionConfigRoot root = new FactionConfigRoot();
        root.factions = new ArrayList<>();
        for (FactionObject faction : factionsMap.values()) {
            FactionConfigEntry entry = new FactionConfigEntry();
            entry.name = faction.getName();
            entry.color = formatColorHex(faction.getColor());
            entry.official_uuid = faction.getOfficialUuid() != null
                    ? faction.getOfficialUuid().toString()
                    : null;
            entry.members = new ArrayList<>();
            for (UUID memberId : faction.getMembers()) {
                entry.members.add(memberId.toString());
            }
            entry.flag_state = faction.getFlagState().name();
            BlockPos flagPos = faction.getFlagBlockPos();
            if (flagPos != null) {
                FlagPositionConfig flagPosition = new FlagPositionConfig();
                flagPosition.x = flagPos.getX();
                flagPosition.y = flagPos.getY();
                flagPosition.z = flagPos.getZ();
                flagPosition.dimension = faction.getFlagDimension();
                entry.flag_position = flagPosition;
            }
            entry.claimed_chunks = new ArrayList<>();
            for (ChunkPos chunkPos : faction.getClaimedChunks()) {
                ChunkConfig chunk = new ChunkConfig();
                chunk.x = chunkPos.x;
                chunk.z = chunkPos.z;
                entry.claimed_chunks.add(chunk);
            }
            root.factions.add(entry);
        }
        root.admin_chunks = new ArrayList<>();
        for (ChunkPos chunkPos : adminChunksSet) {
            ChunkConfig chunk = new ChunkConfig();
            chunk.x = chunkPos.x;
            chunk.z = chunkPos.z;
            root.admin_chunks.add(chunk);
        }
        root.pending_invites = new ArrayList<>();
        for (Map.Entry<UUID, FactionInviteManager.Invite> inviteEntry
                : FactionInviteManager.snapshotNonExpired().entrySet()) {
            PendingInviteConfig invite = new PendingInviteConfig();
            invite.target_uuid = inviteEntry.getKey().toString();
            invite.faction_uuid = inviteEntry.getValue().factionId().toString();
            invite.inviter_uuid = inviteEntry.getValue().inviterId().toString();
            invite.expires_at_ms = inviteEntry.getValue().expiresAtMs();
            root.pending_invites.add(invite);
        }
        return root;
    }

    @Nullable
    private static FactionObject toFactionObject(FactionConfigEntry entry) {
        if (entry.name == null || entry.name.isBlank()) {
            LOGGER.warn("Skipping invalid faction entry: missing name");
            return null;
        }
        UUID factionId = parseUuid(entry.uuid, "faction uuid");
        if (factionId == null) {
            factionId = UUID.nameUUIDFromBytes(("faction:" + entry.name).getBytes(StandardCharsets.UTF_8));
        }
        FactionObject faction = new FactionObject(factionId, entry.name, parseColorHex(entry.color));
        UUID officialUuid = parseUuid(entry.official_uuid, "official uuid");
        if (officialUuid != null) {
            faction.setOfficialUuid(officialUuid);
            faction.addMember(officialUuid);
        }
        if (entry.members != null) {
            for (String memberUuid : entry.members) {
                UUID memberId = parseUuid(memberUuid, "member uuid");
                if (memberId != null) {
                    faction.addMember(memberId);
                }
            }
        }
        String stateName = entry.flag_state != null ? entry.flag_state : entry.anchor_state;
        if (stateName != null) {
            faction.setFlagState(FlagState.fromName(stateName));
        }
        FlagPositionConfig flagPosition = entry.flag_position != null ? entry.flag_position : entry.anchor_position;
        if (flagPosition != null) {
            faction.setFlagBlockPos(new BlockPos(flagPosition.x, flagPosition.y, flagPosition.z));
            faction.setFlagDimension(flagPosition.dimension != null ? flagPosition.dimension : "minecraft:overworld");
        }
        if (entry.claimed_chunks != null) {
            for (ChunkConfig chunk : entry.claimed_chunks) {
                faction.claimChunk(new ChunkPos(chunk.x, chunk.z));
            }
        }
        return faction;
    }

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static UUID parseUuid(@Nullable String raw, String context) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Invalid UUID for {}: {}", context, raw);
            return null;
        }
    }

    public static int parseColorHex(@Nullable String hex) {
        if (hex == null || hex.isBlank()) {
            return 0xFFFFFF;
        }
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return Integer.parseInt(cleaned, 16) & 0xFFFFFF;
        } catch (NumberFormatException exception) {
            LOGGER.warn("Invalid color hex '{}', using white", hex);
            return 0xFFFFFF;
        }
    }

    public static boolean isValidColorHex(@Nullable String hex) {
        if (hex == null || hex.isBlank()) {
            return false;
        }
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        if (cleaned.length() != 6) {
            return false;
        }
        for (int i = 0; i < cleaned.length(); i++) {
            char character = cleaned.charAt(i);
            boolean hexDigit = (character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f')
                    || (character >= 'A' && character <= 'F');
            if (!hexDigit) {
                return false;
            }
        }
        return true;
    }

    public static String formatColorHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static void updateLastModified(Path path) {
        try {
            if (Files.exists(path)) {
                lastKnownModified = Files.getLastModifiedTime(path).toMillis();
            }
        } catch (IOException exception) {
            lastKnownModified = -1L;
        }
    }

    public static final class FactionConfigRoot {
        public List<FactionConfigEntry> factions;
        public List<ChunkConfig> admin_chunks;
        public List<PendingInviteConfig> pending_invites;
    }

    public static final class FactionConfigEntry {
        public String name;
        @Nullable
        public String uuid;
        public String color;
        public String official_uuid;
        public List<String> members;
        public String flag_state;
        @Nullable
        public String anchor_state;
        public FlagPositionConfig flag_position;
        @Nullable
        public FlagPositionConfig anchor_position;
        public List<ChunkConfig> claimed_chunks;
    }

    public static final class FlagPositionConfig {
        public int x;
        public int y;
        public int z;
        public String dimension;
    }

    public static final class ChunkConfig {
        public int x;
        public int z;
    }

    public static final class PendingInviteConfig {
        public String target_uuid;
        public String faction_uuid;
        public String inviter_uuid;
        public long expires_at_ms;
    }
}
