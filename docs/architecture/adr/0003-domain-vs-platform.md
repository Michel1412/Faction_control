# ADR-0003: Domain vs platform split

- **Status:** Accepted
- **Date:** 2026-08-21
- **Applies to:** what is allowed to differ between 1.20.1 and 1.21.1

## Context

Most of the mod is **domain**: faction records, chunk maps, invite TTL, raid session state, GameMode policy. A smaller shell is **platform**: Forge events, `SimpleChannel`, item NBT, capabilities, `mods.toml`.

If we mix those in one doc (or one class with no boundary), every 1.21.1 API rename looks like a gameplay change.

## Decision

Classify every package:

| Layer | Packages | May differ by MC version? |
|-------|----------|---------------------------|
| Domain | `faction`, `config` (JSON maps), `raid` (session rules), `util.TerritoryProtectionHelper`, `util.FlagBreakPolicy` | **No** — same rules |
| Platform | `FactionControlMod`, `registry`, `network`, `event` (bus types), `item` (stack data), `util.BlockInventoryHelper`, `util.FactionPlayerData` (clone/caps) | **Yes** |
| Compat | `compat.create`, `compat.tacz` | **Yes** (mod APIs) |
| Client | `client` | **Yes** (render / color events) |

Platform code must not reimplement territory rules. It only translates events into `TerritoryProtectionHelper` / `FactionManager` calls — same as 1.4.x.

## Consequences

- Conversion work is “rebind the platform shell”, not “redesign factions”.
- Implementation notes live under `docs/implementations/<version>/`, not in `ARCHITECTURE.md`.
- If a 1.21.1 vanilla change *forces* a rule change, that is a new ADR, not a silent port tweak.
