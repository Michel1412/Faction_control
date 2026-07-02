package com.factioncontrol.faction;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory pending faction invites (not persisted across restarts).
 */
public final class FactionInviteManager {
    private static final long INVITE_TTL_MS = 5 * 60 * 1000L;
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
        return invite;
    }

    public static void removeInvite(UUID targetPlayerId) {
        PENDING.remove(targetPlayerId);
    }

    public static void removeExpired() {
        long now = System.currentTimeMillis();
        PENDING.entrySet().removeIf(entry -> entry.getValue().expiresAtMs() <= now);
    }
}
