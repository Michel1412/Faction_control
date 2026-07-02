package com.factioncontrol.network.packet;



import com.factioncontrol.client.ClientFactionData;

import com.factioncontrol.faction.FactionObject;

import com.factioncontrol.faction.FactionManager;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.network.NetworkEvent;



import javax.annotation.Nullable;

import java.util.UUID;

import java.util.function.Supplier;



public record S2CPlayerFactionSyncPacket(

        boolean hasFaction,

        @Nullable UUID factionId,

        @Nullable String factionName,

        int factionColor,

        boolean official

) {

    public static S2CPlayerFactionSyncPacket fromServer(net.minecraft.server.level.ServerPlayer player) {

        FactionManager manager = FactionManager.get(player.server);

        FactionObject faction = manager.getFactionOfMember(player.getUUID());

        if (faction == null) {

            return new S2CPlayerFactionSyncPacket(false, null, null, 0, false);

        }

        return new S2CPlayerFactionSyncPacket(

                true,

                faction.getFactionId(),

                faction.getName(),

                faction.getColor(),

                faction.isLeader(player.getUUID())

        );

    }



    public static void encode(S2CPlayerFactionSyncPacket packet, FriendlyByteBuf buffer) {

        buffer.writeBoolean(packet.hasFaction);

        if (packet.hasFaction) {

            buffer.writeUUID(packet.factionId);

            buffer.writeUtf(packet.factionName);

            buffer.writeVarInt(packet.factionColor);

            buffer.writeBoolean(packet.official);

        }

    }



    public static S2CPlayerFactionSyncPacket decode(FriendlyByteBuf buffer) {

        boolean hasFaction = buffer.readBoolean();

        if (!hasFaction) {

            return new S2CPlayerFactionSyncPacket(false, null, null, 0, false);

        }

        return new S2CPlayerFactionSyncPacket(

                true,

                buffer.readUUID(),

                buffer.readUtf(),

                buffer.readVarInt(),

                buffer.readBoolean()

        );

    }



    public static void handle(S2CPlayerFactionSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {

        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> ClientFactionData.apply(packet));

        context.setPacketHandled(true);

    }

}

