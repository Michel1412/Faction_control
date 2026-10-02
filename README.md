# Faction Control

**Minecraft Forge 1.20.1** mod — a faction and chunk protection system built around anchor flag blocks, JSON-backed territory maps, and Adventure-mode-native access control. A **1.21.1 NeoForge** port lives in `neoforge-1.21.1/`.

| Property | Value |
|----------|-------|
| Mod ID | `faction_control` |
| Current loader | Forge 47+ (Minecraft 1.20.1) |
| 1.21.1 loader | NeoForge 21.1 (see `neoforge-1.21.1/`) |
| Config | `config/faction_control.json` (same schema on both versions) |
| Version | See `gradle.properties` |

---

## Overview

Faction Control provides:

- **Faction management** — create factions, invite members, assign Officials, and persist all data to JSON.
- **Territory claiming** — place a flag block to claim a chunk; expand with the Faction Upgrade item on adjacent chunks.
- **Protection framework** — O(1) HashMap lookups drive block break, interaction, and GameMode rules per chunk.
- **Wireless raids** — the Raid Controller item runs a 60-second hack to set an enemy faction to `RAIDED`.
- **Admin Safezones** — OP-claimable chunks with PvP disabled, hostile spawn blocked, and container access open.

### Items

| Item | Purpose |
|------|---------|
| `flag_block` | Faction anchor; claims chunk on placement |
| `faction_upgrade_item` | Expand territory to an NBT-bound adjacent chunk |
| `raid_controller_item` | 60s wireless hack against enemy ACTIVE territory |

---

## Critical Design Architecture Notice

> **This mod was conceived, engineered, and fine-tuned for a specific server environment where ALL players inherently play in Adventure Mode by default.**

On that target server, Adventure is the global baseline enforced outside this mod. Faction Control's protection framework **leverages native Adventure Mode properties** — block breaking restricted, container interaction governed by vanilla rules — as the default security posture for the open world.

The mod layers **O(1) in-memory HashMaps** (`chunkToFactionMap`, `playerToFactionMap`, `adminChunksSet`) mirrored from `config/faction_control.json` so every permission check is a constant-time lookup with no disk I/O on the hot path.

**GameMode orchestration** works as a privilege elevation system:

1. Players enter the world in **Adventure** (server default).
2. When entering **authorized territory** — their own faction's claims, raided zones where they have access, or other contexts resolved by `TerritoryProtectionHelper` — the mod **elevates them to Survival** via `GameModeSyncHandler`, granting block-breaking capability where rules allow.
3. When entering **restricted claims** — foreign ACTIVE faction territory, admin Safezones, or free wilderness zones depending on context — the mod **forces Adventure back**, re-applying vanilla break restrictions and complementing event-level denial for interactions.

This design avoids reimplementing Minecraft's built-in Adventure semantics from scratch. Instead, the mod toggles Survival as a scoped permission inside allowed zones while HashMap-backed event handlers enforce finer rules (indestructible containers for enemies, interaction locks, PvP Safezones) that Adventure alone cannot express.

**Operators (OP 2+)** default to **Admin Mode** (`/faction playmode false`), which bypasses all territory GameMode sync and event cancellation — intended for moderation and world editing.

---

## Territory Rules (Summary)

| Zone | GameMode | Break (outsider) | Interact (outsider) |
|------|----------|------------------|---------------------|
| Own faction | Survival | Allowed | Allowed |
| Enemy ACTIVE | Survival | Non-inventory blocks only | Blocked |
| RAIDED | Survival | Non-inventory blocks only | Allowed |
| Free zone | Adventure | Vanilla Adventure limits | Allowed |
| Admin Safezone | Adventure | Blocked (Adventure) | Open; PvP/TaCZ off |

See [docs/UTILITIES.md](docs/UTILITIES.md) for full mechanical detail.

---

## Documentation

| Document | Contents |
|----------|----------|
| [docs/architecture/architecture.yml](docs/architecture/architecture.yml) | Supported versions, loaders, ADR index |
| [docs/architecture/adr/](docs/architecture/adr/) | Architecture Decision Records (dual version, NeoForge, JSON, item data) |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Domain layers, tick priority, create-faction gate, failure points |
| [docs/implementations/1.20.1/](docs/implementations/1.20.1/) | Forge 1.20.1 APIs (events, NBT, SimpleChannel, resources) |
| [docs/implementations/1.21.1/](docs/implementations/1.21.1/) | NeoForge 1.21.1 conversion inventory and API maps |
| [docs/COMMANDS.md](docs/COMMANDS.md) | All `/faction` commands, permissions, backend behavior |
| [docs/COMANDOS.md](docs/COMANDOS.md) | Referência de comandos em português |
| [docs/UTILITIES.md](docs/UTILITIES.md) | JSON schema, raid timer, protection pipeline (domain) |

---

## Building

Requirements: **Java 17**, Gradle wrapper included.

```bash
./gradlew build
```

Output JAR: `build/libs/faction_control-1.20.1-<version>.jar`

**1.21.1 (NeoForge)** is a separate project. From `neoforge-1.21.1/` (Java 21):

```bash
./gradlew build
```

Output JAR: `neoforge-1.21.1/build/libs/faction_control-1.21.1-<version>.jar`

Development client (1.20.1):

```bash
./gradlew runClient
```

---

## License

Copyright (c) 2024-2026 Michel1412 / RN Team. All Rights Reserved.

Redistribution and modification without permission are not allowed.

The full notice is in `LICENSE`. Both modules declare `license = All Rights Reserved` through `mod_license` in `gradle.properties` and `neoforge-1.21.1/gradle.properties`, expanded into `mods.toml` and `neoforge.mods.toml`.
