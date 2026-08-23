# ADR-0004: One JSON schema across versions

- **Status:** Accepted
- **Date:** 2026-08-21
- **Applies to:** `config/faction_control.json`

## Context

The JSON file is the on-disk source of truth. Servers that upgrade from 1.20.1 to 1.21.1 (or run both in a network) must keep faction UUIDs, claims, flag positions, admin chunks, and pending invites.

The schema is already documented in `docs/UTILITIES.md`.

## Decision

**Do not version-fork the JSON.** Both builds read/write the same `faction_control.json` shape:

- `factions[]` with `uuid`, `name`, `color`, `official_uuid`, `members`, `flag_state`, `flag_position`, `claimed_chunks`
- `admin_chunks[]`
- `pending_invites[]`

`FactionConfigManager` logic (maps, `rebuildDerivedMaps`, `withSinglePersist`) stays semantically identical. Only the path helper (`FMLPaths.CONFIGDIR`) changes package.

If a future field is needed, add it in a backward-compatible way on **both** versions in the same release line.

## Consequences

- World/config copies between 1.20.1 and 1.21.1 servers are valid as long as chunk coords and dimension ids match.
- Item NBT vs Data Components (ADR-0005) do **not** leak into this file — chunk bind lives on the upgrade item, not in JSON.
