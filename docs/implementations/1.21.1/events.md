# 1.21.1 — Events

Forge 47 flattened tick events. NeoForge 21.1 **splits** them. Handlers stay the same classes; signatures change.

## Bus

| 1.20.1 | 1.21.1 |
|--------|--------|
| `Mod.EventBusSubscriber.Bus.FORGE` | `Mod.EventBusSubscriber.Bus.GAME` |
| `MinecraftForge.EVENT_BUS` | `NeoForge.EVENT_BUS` |
| `Bus.MOD` | `Bus.MOD` (unchanged idea) |

## Tick

| 1.20.1 | 1.21.1 |
|--------|--------|
| `TickEvent.PlayerTickEvent` + `phase == END` | `PlayerTickEvent.Post` (no phase check) |
| `event.player` | `event.getEntity()` |
| `TickEvent.ServerTickEvent` + `phase == END` | `ServerTickEvent.Post` |
| `event.getServer()` | `event.getServer()` |

`GameModeSyncHandler` and `WirelessRaidHackHandler` must not subscribe to `PlayerTickEvent.Pre`.

## Interact / block

Still exist under `net.neoforged.neoforge.event.*`:

- `PlayerInteractEvent.LeftClickBlock` / `RightClickBlock`
- `BlockEvent.BreakEvent`
- `BlockEvent.EntityPlaceEvent`

Confirm getter names (`getEntity()` vs `getPlayer()`) at compile time — NeoForge aligned several events to `getEntity()`.

## Lifecycle / player

| 1.20.1 | 1.21.1 |
|--------|--------|
| `ServerStartingEvent` / `Started` / `Stopping` | same names, `neoforge.event.server` |
| `PlayerEvent.PlayerLoggedInEvent` | `PlayerEvent.PlayerLoggedInEvent` (package neoforge) |
| `PlayerEvent.Clone` | `PlayerEvent.Clone` |
| `LivingDeathEvent` | `LivingDeathEvent` |
| `LivingHurtEvent` | `LivingHurtEvent` |
| `LivingChangeTargetEvent` | `LivingChangeTargetEvent` — `getNewTarget()` may be `getOriginalTarget`/`getNewAboutToBeSetTarget` on some versions; **compile-check** |
| `MobSpawnEvent.FinalizeSpawn` (`setSpawnCancelled`) | `FinalizeSpawnEvent` (same cancel method) |

## Priority

`EventPriority.HIGHEST` / `HIGH` still valid. Keep the 1.20.1 priorities so GameMode sync still runs before protection cancel.
