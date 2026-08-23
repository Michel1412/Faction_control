# Implementation notes — Minecraft 1.20.1 (Forge)

Production tree today: `src/main` at the repo root. Loader, Java, and Gradle versions are locked in `gradle.properties`.

| Property | Value |
|----------|-------|
| Minecraft | 1.20.1 |
| Loader | Forge **47.4.10** (`forge_version_range=[47,)`) |
| Java | 17 |
| Gradle | 8.8 + ForgeGradle `[6.0,6.2)` |
| Metadata | `src/main/resources/META-INF/mods.toml` |
| Pack format | 15 (`pack.mcmeta`) |

When something breaks only on 1.20.1, start here — not in `docs/ARCHITECTURE.md`.

| Topic | File |
|-------|------|
| Gradle, `mods.toml`, toolchain | [loader-gradle.md](loader-gradle.md) |
| DeferredRegister / creative tab | [registries.md](registries.md) |
| Forge event bus + handlers | [events.md](events.md) |
| `SimpleChannel` sync packet | [networking.md](networking.md) |
| Upgrade item NBT | [items-nbt.md](items-nbt.md) |
| Player `getPersistentData` + Clone | [player-data.md](player-data.md) |
| `ForgeCapabilities.ITEM_HANDLER` | [capabilities.md](capabilities.md) |
| Models, loot, `forge:separate_transforms` | [resources.md](resources.md) |
| PermissionAPI + vanilla levels | [permissions.md](permissions.md) |
| Create / TaCZ reflection | [compat.md](compat.md) |
| Client render layer + item colors | [client.md](client.md) |

Domain rules (maps, raid 60s, Adventure/Survival table) stay in [ARCHITECTURE.md](../../ARCHITECTURE.md) and [UTILITIES.md](../../UTILITIES.md).
