package com.factioncontrol.network;

import com.factioncontrol.network.packet.S2CPlayerFactionSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                S2CPlayerFactionSyncPacket.TYPE,
                S2CPlayerFactionSyncPacket.STREAM_CODEC,
                S2CPlayerFactionSyncPacket::handle
        );
    }

    public static void sendPlayerFactionSync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, S2CPlayerFactionSyncPacket.fromServer(player));
    }
}
