package com.factioncontrol.network.packet;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.client.ClientFactionData;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.faction.FactionObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record S2CPlayerFactionSyncPacket(
        boolean hasFaction,
        @Nullable UUID factionId,
        @Nullable String factionName,
        int factionColor,
        boolean official
) implements CustomPacketPayload {
    public static final Type<S2CPlayerFactionSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FactionControlMod.MODID, "player_faction_sync")
    );

    public static final StreamCodec<FriendlyByteBuf, S2CPlayerFactionSyncPacket> STREAM_CODEC = StreamCodec.of(
            S2CPlayerFactionSyncPacket::encode,
            S2CPlayerFactionSyncPacket::decode
    );

    public static S2CPlayerFactionSyncPacket fromServer(ServerPlayer player) {
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

    private static void encode(FriendlyByteBuf buffer, S2CPlayerFactionSyncPacket packet) {
        buffer.writeBoolean(packet.hasFaction);
        if (packet.hasFaction) {
            buffer.writeUUID(packet.factionId);
            buffer.writeUtf(packet.factionName);
            buffer.writeVarInt(packet.factionColor);
            buffer.writeBoolean(packet.official);
        }
    }

    private static S2CPlayerFactionSyncPacket decode(FriendlyByteBuf buffer) {
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

    public static void handle(S2CPlayerFactionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientFactionData.apply(packet));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
