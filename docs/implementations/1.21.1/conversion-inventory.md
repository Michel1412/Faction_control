# 1.21.1 — Conversion inventory

This is the checklist to keep **two supported artifacts** (Forge 1.20.1 + NeoForge 1.21.1) with the same domain behavior.

Policy (ADR-0003): **port 1:1**. Do not change claim rules, raid duration, or JSON schema while converting.

Suggested Gradle layout (ADR-0001) — **applied**:

```
src/main/                  ← Forge 1.20.1 production (root)
neoforge-1.21.1/           ← NeoForge 1.21.1 (own Gradle wrapper)
docs/                      ← shared
```

Each row is one current production file.

## Legend

| Tag | Meaning |
|-----|---------|
| **COPY** | Domain; copy and only fix imports if a vanilla class moved |
| **ADAPT** | Same class, Forge → NeoForge / 1.21 method signatures |
| **REWRITE** | Different API (network payloads, Data Components, metadata) |
| **DROP** | Do not port (debug-only) |
| **VERIFY** | Behavior depends on optional mods — confirm FQCN on 1.21.1 NeoForge builds |

---

## Platform bootstrap

| Current file | Tag | 1.21.1 work |
|--------------|-----|-------------|
| `FactionControlMod.java` | **REWRITE** | `@Mod` + inject `IEventBus`; `NeoForge.EVENT_BUS`; no `FMLJavaModLoadingContext` Forge type; `neoforge.mods.toml` |
| `build.gradle` / `gradle.properties` / `settings.gradle` | **REWRITE** | New module: ModDevGradle, Java 21, `neo_version`, no `reobfJar` |
| `META-INF/mods.toml` | **REWRITE** | `neoforge.mods.toml`; dep `neoforge` not `forge`; MC range `[1.21.1]` |
| `pack.mcmeta` | **ADAPT** | `pack_format` 15 → **34** |

## Domain (keep rules identical)

| Current file | Tag | 1.21.1 work |
|--------------|-----|-------------|
| `faction/FactionObject.java` | **COPY** | Vanilla `BlockPos` / `ChunkPos` only |
| `faction/FlagState.java` | **COPY** | |
| `faction/FactionInviteManager.java` | **COPY** | |
| `faction/FactionManager.java` | **ADAPT** | `MinecraftServer` APIs stable; watch any `ServerLevel` imports |
| `config/FactionConfigManager.java` | **ADAPT** | `FMLPaths` → `net.neoforged.fml.loading.FMLPaths` (same `CONFIGDIR`) |
| `raid/WirelessRaidHackManager.java` | **ADAPT** | `ItemStack` / `ServerPlayer` stable; session map unchanged |
| `util/TerritoryProtectionHelper.java` | **COPY** | `GameType` still vanilla |
| `util/SafezoneHelper.java` | **COPY** | |
| `util/FlagHelper.java` | **ADAPT** | block place APIs mostly stable |
| `util/FlagBreakPolicy.java` | **COPY** | |
| `util/FactionChat.java` | **ADAPT** | `sendSystemMessage` / action bar — verify 1.21 overlay packet if used |
| `util/FactionDebugSettings.java` | **COPY** | |
| `util/PlayerPlayModeHelper.java` | **COPY** | delegates to `FactionPlayerData` |
| `util/FactionSync.java` | **ADAPT** | if it calls `ModNetwork` |

## Commands

| Current file | Tag | 1.21.1 work |
|--------------|-----|-------------|
| `command/FactionCommands.java` | **ADAPT** | Brigadier is vanilla; `Commands.literal` package stable; permission predicates unchanged |
| `command/FactionCommandRegistry.java` | **ADAPT** | `net.neoforged.neoforge.event.RegisterCommandsEvent`; bus `Bus.GAME` |
| `command/FactionPermissions.java` | **ADAPT** | `net.neoforged.neoforge.server.permission.*`; same node id `create_faction` |

## Events (every `@SubscribeEvent` class)

| Current file | Tag | 1.21.1 work |
|--------------|-----|-------------|
| `event/GameModeSyncHandler.java` | **ADAPT** | `TickEvent.PlayerTickEvent` → `PlayerTickEvent.Post`; `event.player` → `event.getEntity()`; see [events.md](events.md) |
| `event/TerritoryProtectionHandler.java` | **ADAPT** | `BlockEvent.BreakEvent` / `EntityPlaceEvent` packages; `PlayerInteractEvent` still exists |
| `event/FlagEventHandler.java` | **ADAPT** | same break event |
| `event/AdminSafezoneHandler.java` | **ADAPT** | `MobSpawnEvent.FinalizeSpawn`, `LivingHurtEvent`, `LivingChangeTargetEvent` — confirm `setSpawnCancelled` |
| `event/WirelessRaidHackHandler.java` | **ADAPT** | player tick split + logout/death |
| `event/FactionServerLifecycleHandler.java` | **ADAPT** | `ServerTickEvent.Post`; `ServerStartingEvent` package |
| `event/TerritoryInteractionLogHandler.java` | **ADAPT** | same as protection handler |

## Items / blocks

| Current file | Tag | 1.21.1 work |
|--------------|-----|-------------|
| `item/FactionUpgradeItem.java` | **REWRITE** | Data Components instead of `getOrCreateTag`; new tooltip + `use` signatures — [data-components.md](data-components.md) |
| `item/RaidControllerItem.java` | **ADAPT** | `appendHoverText(Item.TooltipContext, …)`; `getUseDuration(ItemStack, LivingEntity)` |
| `item/FlagBlockItem.java` | **ADAPT** | tooltip signature only |
| `block/FlagBlock.java` | **ADAPT** | `onRemove` still present on 1.21.1; explosion method may take `Explosion` record — compile and fix |
| `registry/ModBlocks.java` | **ADAPT** | `DeferredRegister` + `DeferredHolder` from NeoForge; `BuiltInRegistries` / `Registries` |
| `registry/ModItems.java` | **ADAPT** | same + register component type (new class) |
| `registry/ModCreativeTabs.java` | **ADAPT** | `DeferredHolder<CreativeModeTab, …>` |

## Player / inventory helpers

| Current file | Tag | 1.21.1 work |
|--------------|-----|-------------|
| `util/FactionPlayerData.java` | **ADAPT** | keep NBT keys; Clone event + drop/replace `reviveCaps` — [player-data.md](player-data.md) |
| `util/BlockInventoryHelper.java` | **REWRITE** | `Capabilities.ItemHandler.BLOCK` — [capabilities.md](capabilities.md) |
| `util/ModItemHelper.java` | **ADAPT** | `BuiltInRegistries.ITEM.getKey` or NeoForge registry |

## Network

| Current file | Tag | 1.21.1 work |
|--------------|-----|-------------|
| `network/ModNetwork.java` | **REWRITE** | `PayloadRegistrar` / `CustomPacketPayload` |
| `network/packet/S2CPlayerFactionSyncPacket.java` | **REWRITE** | implement `CustomPacketPayload`; `StreamCodec`; client handler via payload context |

## Client

| Current file | Tag | 1.21.1 work |
|--------------|-----|-------------|
| `client/ClientFactionData.java` | **COPY** | |
| `client/ItemTextureColors.java` | **COPY** | |
| `client/ClientModEvents.java` | **ADAPT** + **DROP** | colors event package; prefer `render_type` in block model instead of `ItemBlockRenderTypes`; **drop** `DEBUG_LOG_PATH` probe |

## Compat (verify against 1.21.1 NeoForge jars)

| Current file | Tag | 1.21.1 work |
|--------------|-----|-------------|
| `compat/ModCompatibility.java` | **ADAPT** | `net.neoforged.fml.ModList` |
| `compat/tacz/TaczIntegration.java` | **VERIFY** | event bus `NeoForge.EVENT_BUS`; confirm `GunFireEvent` FQCN on the 1.21.1 TaCZ port |
| `compat/create/CreateIntegration.java` | **VERIFY** | Create 6.x NeoForge packages; `BuiltInRegistries.BLOCK` |
| `compat/create/CreateReflection.java` | **VERIFY** | `AbstractContraptionEntity` FQCN |
| `compat/create/ContraptionOwnerTracker.java` | **COPY** | pure UUID/chunk map |

## Resources

| Current file | Tag | 1.21.1 work |
|--------------|-----|-------------|
| `models/item/flag_block.json` | **ADAPT** | `forge:separate_transforms` → `neoforge:separate_transforms` |
| other models / textures / lang | **COPY** | 1.21.1 still uses `models/item` |
| `loot_tables/blocks/flag_block.json` | **COPY** | folder still `loot_tables` on 1.21.1 |
| `blockstates/flag_block.json` | **COPY** | add `render_type: cutout` on the **block model** |

## Tools (not in the mod JAR)

| Current file | Tag | 1.21.1 work |
|--------------|-----|-------------|
| `tools/TexturePlaceholderGenerator.java` | **COPY** | optional; not versioned |
| `tools/flag_block_source.json` | **COPY** | Blockbench source |

---

## Port order (recommended)

1. Gradle module + `neoforge.mods.toml` + empty `@Mod` that loads.
2. Registries (block, items, tab) + resources (pack 34, separate_transforms).
3. `FactionConfigManager` + `FactionManager` + lifecycle events (JSON load).
4. Commands + permissions.
5. Territory helpers + protection / GameMode / flag / safezone events.
6. Data component on upgrade item + raid controller use-duration.
7. `CustomPacketPayload` sync.
8. Compat verify (Create, TaCZ) last.

After each step: `runServer` smoke — load JSON, `/faction create` as OP, place flag, GameMode swap, upgrade bind.

## Dual-support rules after the port

- Same `mod_version` line (e.g. 1.4.1) on both artifacts unless a version-only hotfix is required.
- JSON schema changes land in **both** modules in the same release.
- Domain bugs: patch `TerritoryProtectionHelper` / `FactionObject` in both trees (or extract `common` later).
- Do not edit 1.20.1 Forge APIs to “look like” NeoForge.
