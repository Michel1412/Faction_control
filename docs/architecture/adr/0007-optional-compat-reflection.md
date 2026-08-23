# ADR-0007: Optional Create/TaCZ via reflection

- **Status:** Accepted
- **Date:** 2026-08-21
- **Applies to:** `compat` package

## Context

Create and TaCZ are optional. 1.20.1 uses:

- `ModList.isLoaded("create" | "tacz")`
- Reflection for TaCZ `GunFireEvent` (`com.tacz.guns.api.event.common.GunFireEvent`)
- Reflection for Create `AbstractContraptionEntity`
- Forge event bus + `ForgeRegistries` for Create block namespace checks

Hard compile dependencies would force every builder to install those mods and would pin incompatible versions per MC line.

## Decision

Keep **no compile-time dependency** on either mod in both artifacts.

Each version’s `compat` folder re-verifies:

1. Mod id still `create` / `tacz` (or the 1.21.1 NeoForge fork id).
2. Fully qualified event/entity class names.
3. Event bus type (`MinecraftForge.EVENT_BUS` vs `NeoForge.EVENT_BUS`).
4. Whether `GunFireEvent` is still a Forge/NeoForge `Event` with `setCanceled`.

If a 1.21.1 port of TaCZ is missing or the event moved to a custom bus, Safezone gun-block is **degraded** (log + skip), not a compile failure.

## Consequences

- Compat is always in the “rewrite/verify” conversion bucket.
- Domain rule does not change: guns allowed in enemy territory; blocked when shooter or target is in an admin chunk.
