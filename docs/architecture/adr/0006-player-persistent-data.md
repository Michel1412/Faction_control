# ADR-0006: Player flags stay on persistent NBT

- **Status:** Accepted
- **Date:** 2026-08-21
- **Applies to:** `play_as_player`, `can_create_faction`

## Context

`FactionPlayerData` stores a compound `faction_control` on `player.getPersistentData()`:

- `play_as_player` — OP “play as normal player”
- `can_create_faction` — create-faction gate

On death, Forge recreates the player. 1.20.1 copies the compound in `PlayerEvent.Clone` and uses `reviveCaps()` / `invalidateCaps()` so the original entity’s data is readable.

NeoForge 1.21.1 has **Attachments** as the preferred entity extra-data API. Persistent NBT (`getPersistentData()`) still exists and is enough for two booleans.

## Decision

Keep **the same NBT keys** on both versions for the first 1.21.1 port:

- Root: `faction_control`
- Booleans: `play_as_player`, `can_create_faction`

Adapt only the clone/copy hook (event package + whether `reviveCaps` still exists). Do not migrate to Attachments until both versions need richer player data.

## Consequences

- LuckPerms/quest flow (`/faction cancreate`) stays identical.
- Player data survives a world copy between loaders as long as the player DAT still has that compound.
- Clone-handler details are version-specific (`docs/implementations/*/player-data.md`).
