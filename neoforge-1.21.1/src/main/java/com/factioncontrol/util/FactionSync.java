package com.factioncontrol.util;

import com.factioncontrol.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;

public final class FactionSync {
    private FactionSync() {
    }

    public static void sendTo(ServerPlayer player) {
        ModNetwork.sendPlayerFactionSync(player);
    }
}
