# 1.20.1 — Player persistent data

Class: `util.FactionPlayerData`.

Store: `player.getPersistentData()` compound `faction_control`.

| Key | Default | Meaning |
|-----|---------|---------|
| `play_as_player` | false | OP subject to territory rules (`PlayerPlayModeHelper`) |
| `can_create_faction` | false | create-faction gate (OR with PermissionAPI / OP 2) |

Forge death clone:

```java
@SubscribeEvent
onPlayerClone(PlayerEvent.Clone event)
  original.reviveCaps();
  try { clone.getPersistentData().put(NBT_ROOT, source.getCompound(NBT_ROOT).copy()); }
  finally { original.invalidateCaps(); }
```

`reviveCaps()` is required on Forge 1.20.1 because the original entity’s capability/NBT view is invalidated before clone listeners run.

Bus: `Mod.EventBusSubscriber.Bus.FORGE`.
