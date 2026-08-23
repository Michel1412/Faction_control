# ADR-0001: Dual-version Gradle multi-project

- **Status:** Accepted
- **Date:** 2026-08-21
- **Applies to:** repo layout for 1.20.1 + 1.21.1

## Context

The current tree is a single ForgeGradle project targeting Minecraft **1.20.1**. We need a supported **1.21.1** build without freezing or rewriting the 1.20.1 server that already runs this mod.

Options considered:

| Option | Pros | Cons |
|--------|------|------|
| Two git repos | Isolation | Duplicate domain bugs, split PRs |
| Two long-lived branches (`1.20.1` / `1.21.1`) | Simple | Domain fixes must be cherry-picked by hand |
| Architectury | Shared common sources | Extra abstraction; 1.20.1 Forge vs 1.21.1 NeoForge is a poor Architectury fit |
| Gradle multi-project in this repo | One PR can touch domain + both loaders; docs stay together | Root `settings.gradle` must grow |

## Decision

Keep **one repository**. Split Gradle into version modules:

```
faction_control/                 # git root
  docs/                          # shared docs + ADRs
  forge-1.20.1/                  # current sources move here (or stay at root until the split)
  neoforge-1.21.1/               # port
```

Until the split is applied in code, the **current** `src/main` remains the 1.20.1 production tree. The 1.21.1 module is created from the conversion inventory, not by editing 1.20.1 APIs in place.

We do **not** adopt Architectury for this port. Shared logic is copied first, then extracted only if duplication actually hurts (JSON maps, `TerritoryProtectionHelper` rules).

## Consequences

- Two JARs: `faction_control-<ver>-1.20.1.jar` and `faction_control-<ver>-1.21.1.jar`.
- Domain bugfixes may need to land in both modules until a `common` source set exists.
- CurseForge/Modrinth will publish two game-version files, same mod id.
