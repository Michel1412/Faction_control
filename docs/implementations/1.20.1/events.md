# 1.20.1 — Forge events

Subscriber pattern: `@Mod.EventBusSubscriber(modid, bus = Mod.EventBusSubscriber.Bus.FORGE)` on the **game** bus (`MinecraftForge.EVENT_BUS`). Client setup uses `Bus.MOD`.

## Handler map

| Class | Events | Notes |
|-------|--------|-------|
| `FactionCommandRegistry` | `RegisterCommandsEvent` | `/faction` tree |
| `FactionPermissions` | `PermissionGatherEvent.Nodes` | register `CREATE_FACTION` node |
| `FactionPlayerData` | `PlayerEvent.Clone` | copy NBT; `reviveCaps` / `invalidateCaps` |
| `FactionServerLifecycleHandler` | `ServerStartingEvent`, `ServerStartedEvent`, `ServerStoppingEvent`, `TickEvent.ServerTickEvent` (END, every 100 ticks poll JSON `lastModified`), `PlayerEvent.PlayerLoggedInEvent` | load/ready/shutdown + S2C sync |
| `GameModeSyncHandler` | `TickEvent.PlayerTickEvent` (END), `PlayerInteractEvent.LeftClickBlock/RightClickBlock`, `BlockEvent.BreakEvent`, `BlockEvent.EntityPlaceEvent`, `PlayerEvent.PlayerLoggedInEvent` / logout | skip tick if chunk+dim+mode signature unchanged |
| `TerritoryProtectionHandler` | `BlockEvent.BreakEvent`, `EntityPlaceEvent`, `RightClickBlock`, `LeftClickBlock` | cancel → helper; flag break deferred to `FlagEventHandler` |
| `FlagEventHandler` | `BlockEvent.BreakEvent` (HIGH) | Official/admin-only flag destroy |
| `AdminSafezoneHandler` | `MobSpawnEvent.FinalizeSpawn` (`setSpawnCancelled`), `LivingChangeTargetEvent`, `LivingHurtEvent` | Overworld admin chunks |
| `WirelessRaidHackHandler` | `TickEvent.PlayerTickEvent` (END), `LivingDeathEvent`, `PlayerEvent.PlayerLoggedOutEvent` | tick only if using item or session exists |
| `TerritoryInteractionLogHandler` | same interact/break as protection | debug logging |
| `CreateIntegration` | `RightClickBlock`, `EntityJoinLevelEvent`, `EntityLeaveLevelEvent`, `BreakEvent`, `EntityPlaceEvent` | gated by `CreateIntegration.register()` |
| `TaczIntegration` | dynamic `MinecraftForge.EVENT_BUS.addListener` | see [compat.md](compat.md) |

## TickEvent shape (Forge 47)

```java
TickEvent.PlayerTickEvent event
event.phase != TickEvent.Phase.END
event.player
```

```java
TickEvent.ServerTickEvent event
event.phase != TickEvent.Phase.END
event.getServer()
```

There is **no** `PlayerTickEvent.Post` nested type on this loader — that is a NeoForge 1.21 split.

## Cancel pattern

`event.setCanceled(true)` on `ICancellableEvent` types. Early-return if `event.isCanceled()`, client side, or `!FactionManager.isServerDataReady()`.
