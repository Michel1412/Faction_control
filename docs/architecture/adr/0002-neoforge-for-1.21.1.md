# ADR-0002: Minecraft 1.21.1 uses NeoForge, not Forge

- **Status:** Accepted
- **Date:** 2026-08-21
- **Applies to:** 1.21.1 loader choice

## Context

1.20.1 production is **Forge 47.4.10** (`net.minecraftforge.*`). For 1.21.1 the ecosystem split:

- **NeoForge** is the maintained 1.21.x loader used by Create, LuckPerms-adjacent mods, and most 1.21.1 packs.
- LexForge/Forge 1.21 exists but has a thinner mod catalog and different APIs from both 1.20.1 Forge and NeoForge.

The port is not “bump `minecraft_version` in `gradle.properties`”. Packages, events, networking, item data, and the Gradle plugin all change.

## Decision

Target **NeoForge 21.1.x** (MDK baseline `21.1.248` or newer patch) with **ModDevGradle** (`net.neoforged.moddev`), Java 21.

Metadata file: `META-INF/neoforge.mods.toml` (not `mods.toml`).

We will not ship a Forge 1.21.1 artifact in this first dual-support effort.

## Consequences

- Imports move `net.minecraftforge` → `net.neoforged.neoforge` (plus vanilla Mojang changes).
- Create / TaCZ 1.21.1 builds must be the **NeoForge** artifacts; reflection class names must be re-verified (ADR-0007).
- LuckPerms still maps vanilla permission levels; NeoForge `PermissionAPI` package changes but the node id `faction_control.create_faction` stays.
