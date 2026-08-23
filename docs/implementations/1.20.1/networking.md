# 1.20.1 — Networking

## Channel

`network.ModNetwork`:

```java
NetworkRegistry.newSimpleChannel(
    new ResourceLocation(MODID, "main"),
    () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
);
```

- Protocol string: `"1"`
- `CHANNEL.registerMessage(id, class, encode, decode, handle)`
- Send: `CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet)`

`ResourceLocation` is constructed with `new ResourceLocation(namespace, path)` (1.21 replaces this with `fromNamespaceAndPath`).

## Packet

`S2CPlayerFactionSyncPacket` (record): `hasFaction`, `factionId`, `factionName`, `factionColor`, `official`.

| Direction | Codec |
|-----------|--------|
| Encode/decode | `FriendlyByteBuf` (`writeBoolean`, `writeUUID`, `writeUtf`, `writeVarInt`) |
| Handle | `Supplier<NetworkEvent.Context>` → `enqueueWork` → `ClientFactionData.apply` → `setPacketHandled(true)` |

Payload is built server-side with `FactionManager.get(player.server)` + `getFactionOfMember`.

Sync is triggered after join, invite accept, flag place, `/faction reload` (callers of `ModNetwork.sendPlayerFactionSync`).
