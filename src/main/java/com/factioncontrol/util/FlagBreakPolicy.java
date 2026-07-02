package com.factioncontrol.util;

import com.factioncontrol.faction.FactionObject;
import com.factioncontrol.faction.FactionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public final class FlagBreakPolicy {
    private FlagBreakPolicy() {
    }

    /**
     * Only the faction official (leader) or server operators may break the flag block.
     */
    public static boolean canBreakFlag(ServerLevel level, BlockPos flagPos, @Nullable ServerPlayer player) {
        if (player == null) {
            return false;
        }

        if (player.hasPermissions(2) && !PlayerPlayModeHelper.isSubjectToTerritoryRules(player)) {
            return true;
        }

        FactionObject ownerFaction = FlagHelper.resolveFactionAtFlag(level, flagPos);
        if (ownerFaction == null) {
            return false;
        }

        return ownerFaction.isLeader(player.getUUID());
    }
}
