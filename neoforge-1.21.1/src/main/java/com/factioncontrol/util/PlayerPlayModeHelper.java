package com.factioncontrol.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * When an OP toggles {@code play_as_player}, territory game mode and protection rules apply as for a normal player.
 * Without it, OPs bypass territory restrictions and are not forced into Adventure/Survival by the mod.
 */
public final class PlayerPlayModeHelper {
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
        return FactionPlayerData.getFlag(player, FactionPlayerData.NBT_PLAY_AS_PLAYER);
    }

    public static void setPlayAsPlayer(ServerPlayer player, boolean playAsPlayer) {
        FactionPlayerData.setFlag(player, FactionPlayerData.NBT_PLAY_AS_PLAYER, playAsPlayer);
    }

    public static boolean togglePlayAsPlayer(ServerPlayer player) {
        return FactionPlayerData.toggleFlag(player, FactionPlayerData.NBT_PLAY_AS_PLAYER);
    }

    public static Component describeMode(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            return Component.translatable("faction_control.playmode.player_default");
        }
        return Component.translatable(getPlayAsPlayer(player)
                ? "faction_control.playmode.player"
                : "faction_control.playmode.admin");
    }
}
