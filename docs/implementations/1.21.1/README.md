# Implementation notes — Minecraft 1.21.1 (NeoForge)

**Status:** implemented module at `neoforge-1.21.1/` (ADR-0001, ADR-0002). Build with that folder's Gradle wrapper — do not use the root ForgeGradle wrapper.

Baseline from NeoForge MDK (verify patch before first build):

| Property | Target |
|----------|--------|
| Minecraft | 1.21.1 |
| Loader | NeoForge **21.1.248+** |
| Java | 21 |
| Gradle plugin | `net.neoforged.moddev` (ModDevGradle 2.x) |
| Metadata | `META-INF/neoforge.mods.toml` |
| Pack format | **34** |
| Mappings | official or Parchment `2024.11.17-1.21.1` |

Start with [conversion-inventory.md](conversion-inventory.md) — every current Java/resource file classified as copy / adapt / rewrite.

Side-by-side API notes (open next to the 1.20.1 file of the same name):

| Topic | This version | 1.20.1 twin |
|-------|--------------|-------------|
| Gradle / toml | [loader-gradle.md](loader-gradle.md) | [../1.20.1/loader-gradle.md](../1.20.1/loader-gradle.md) |
| Events | [events.md](events.md) | [../1.20.1/events.md](../1.20.1/events.md) |
| Networking | [networking.md](networking.md) | [../1.20.1/networking.md](../1.20.1/networking.md) |
| Item data | [data-components.md](data-components.md) | [../1.20.1/items-nbt.md](../1.20.1/items-nbt.md) |
| Player NBT | [player-data.md](player-data.md) | [../1.20.1/player-data.md](../1.20.1/player-data.md) |
| Capabilities | [capabilities.md](capabilities.md) | [../1.20.1/capabilities.md](../1.20.1/capabilities.md) |
| Resources | [resources.md](resources.md) | [../1.20.1/resources.md](../1.20.1/resources.md) |
| Permissions | [permissions.md](permissions.md) | [../1.20.1/permissions.md](../1.20.1/permissions.md) |
| Compat | [compat.md](compat.md) | [../1.20.1/compat.md](../1.20.1/compat.md) |
| Client | [client.md](client.md) | [../1.20.1/client.md](../1.20.1/client.md) |
