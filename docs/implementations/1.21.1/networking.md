# 1.21.1 — Networking (CustomPacketPayload)

Replace `SimpleChannel` entirely.

## Shape

1. `S2CPlayerFactionSyncPacket` implements `CustomPacketPayload`.
2. Static `Type<S2CPlayerFactionSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "player_faction_sync"))`.
3. Static `StreamCodec<RegistryFriendlyByteBuf, S2CPlayerFactionSyncPacket> STREAM_CODEC` (or `FriendlyByteBuf` if no registry values — this packet is UUID/string/int/bool only).
4. Register on play-to-client in `RegisterPayloadHandlersEvent` via `PayloadRegistrar.playToClient(...)`.
5. Client handler: `context.enqueueWork(() -> ClientFactionData.apply(packet))`.
6. Server send: `PacketDistributor.sendToPlayer(player, packet)` (NeoForge 21.1 helper; confirm exact method on the MDK version).

## ResourceLocation

`new ResourceLocation(ns, path)` is removed/restricted. Use:

```java
ResourceLocation.fromNamespaceAndPath(FactionControlMod.MODID, "player_faction_sync")
```

## Protocol string

`SimpleChannel` protocol `"1"` goes away. Payload id is the `ResourceLocation`. Bump the path suffix if the record fields change.

Fields stay: `hasFaction`, `factionId`, `factionName`, `factionColor`, `official`.
