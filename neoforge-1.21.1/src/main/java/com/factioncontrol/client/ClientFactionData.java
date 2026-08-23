package com.factioncontrol.client;



import com.factioncontrol.network.packet.S2CPlayerFactionSyncPacket;



import org.jetbrains.annotations.Nullable;

import java.util.UUID;



public final class ClientFactionData {

    @Nullable

    private static PlayerFactionView currentFaction;



    private ClientFactionData() {

    }



    public static void apply(S2CPlayerFactionSyncPacket packet) {

        if (!packet.hasFaction()) {

            currentFaction = null;

            return;

        }

        currentFaction = new PlayerFactionView(

                packet.factionId(),

                packet.factionName(),

                packet.factionColor(),

                packet.official()

        );

    }



    @Nullable

    public static PlayerFactionView getCurrentFaction() {

        return currentFaction;

    }



    public record PlayerFactionView(UUID factionId, String name, int color, boolean official) {

    }

}

