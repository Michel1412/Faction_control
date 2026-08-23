package com.factioncontrol.faction;

import com.factioncontrol.config.FactionConfigManager;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pending faction invites. Kept in memory for O(1) accept; mirrored to {@code pending_invites} in JSON
 * so they survive a restart until TTL (5 minutes).
 */
public final class FactionInviteManager {
    public static final long INVITE_TTL_MS = 5 * 60 * 1000L;
    private static final Map<UUID, Invite> PENDING = new ConcurrentHashMap<>();

    private FactionInviteManager() {
    }

    public record Invite(UUID factionId, UUID inviterId, long expiresAtMs) {
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMs;
        }
    }

    public static void createInvite(UUID targetPlayerId, UUID factionId, UUID inviterId) {
        removeExpired();
        PENDING.put(targetPlayerId, new Invite(factionId, inviterId, System.currentTimeMillis() + INVITE_TTL_MS));
        FactionConfigManager.forceSave();
    }

    public static boolean hasPending(UUID targetPlayerId) {
        removeExpired();
        Invite invite = PENDING.get(targetPlayerId);
        return invite != null && !invite.isExpired();
    }

    @Nullable
    public static Invite accept(UUID targetPlayerId) {
        removeExpired();
        Invite invite = PENDING.remove(targetPlayerId);
        if (invite == null || invite.isExpired()) {
            return null;
        }
        FactionConfigManager.forceSave();
        return invite;
    }

    public static void removeInvite(UUID targetPlayerId) {
        if (PENDING.remove(targetPlayerId) != null) {
            FactionConfigManager.forceSave();
        }
    }

    public static void removeExpired() {
        long now = System.currentTimeMillis();
        PENDING.entrySet().removeIf(entry -> entry.getValue().expiresAtMs() <= now);
    }

    public static void clear() {
        PENDING.clear();
    }

    public static void replaceAll(Map<UUID, Invite> loaded) {
        PENDING.clear();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Invite> entry : loaded.entrySet()) {
            Invite invite = entry.getValue();
            if (invite != null && invite.expiresAtMs() > now) {
                PENDING.put(entry.getKey(), invite);
            }
        }
    }

    public static Map<UUID, Invite> snapshotNonExpired() {
        removeExpired();
        return new HashMap<>(PENDING);
    }
}
