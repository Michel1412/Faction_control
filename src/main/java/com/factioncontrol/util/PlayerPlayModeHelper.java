package com.factioncontrol.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * When an OP toggles {@code play_as_player}, territory game mode and protection rules apply as for a normal player.
 * Without it, OPs bypass territory restrictions and are not forced into Adventure/Survival by the mod.
 */
public final class PlayerPlayModeHelper {
    private static final String NBT_ROOT = "faction_control";
    private static final String NBT_PLAY_AS_PLAYER = "play_as_player";

    private PlayerPlayModeHelper() {
    }

    /**
     * {@code true} when territory rules (game mode sync, event denial) should apply to this player.
     */
    public static boolean isSubjectToTerritoryRules(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            return true;
        }
        return getPlayAsPlayer(player);
    }

    /**
     * {@code true} when an OP chose to play under normal faction territory rules.
     */
    public static boolean getPlayAsPlayer(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(NBT_ROOT)) {
            return false;
        }
        return root.getCompound(NBT_ROOT).getBoolean(NBT_PLAY_AS_PLAYER);
    }

    public static void setPlayAsPlayer(ServerPlayer player, boolean playAsPlayer) {
        CompoundTag root = player.getPersistentData();
        CompoundTag factionControl = root.contains(NBT_ROOT)
                ? root.getCompound(NBT_ROOT)
                : new CompoundTag();
        factionControl.putBoolean(NBT_PLAY_AS_PLAYER, playAsPlayer);
        root.put(NBT_ROOT, factionControl);
    }

    public static boolean togglePlayAsPlayer(ServerPlayer player) {
        boolean next = !getPlayAsPlayer(player);
        setPlayAsPlayer(player, next);
        return next;
    }

    public static String describeMode(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            return "Jogador (regras de territorio ativas)";
        }
        return getPlayAsPlayer(player)
                ? "Modo Jogador (regras de territorio ativas)"
                : "Modo Admin (bypass de territorio)";
    }
}
