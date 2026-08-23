# ADR-0009: Docs layout — config + ADRs + per-version folders

- **Status:** Accepted
- **Date:** 2026-08-21
- **Applies to:** `docs/`

## Context

`docs/ARCHITECTURE.md` mixed domain layers with Forge names (`PermissionAPI`, `PlayerEvent.Clone`, tick costs). `docs/UTILITIES.md` mixed JSON schema with `stack.getTag()` and Forge events. That is fine for a single-version mod and painful once 1.21.1 exists.

We need a place that answers “why” (ADRs) and a place that answers “how on this MC version” without duplicating command lists.

## Decision

```
docs/
  ARCHITECTURE.md                 # domain only (layers, maps, gate, failure points)
  architecture/
    architecture.yml              # versions + ADR index
    adr/NNNN-*.md
  implementations/
    1.20.1/                       # Forge APIs, NBT, SimpleChannel, resources
    1.21.1/                       # NeoForge targets + conversion inventory
  COMMANDS.md / COMANDOS.md       # command surface (shared)
  UTILITIES.md                    # JSON + raid/protection *rules* (shared)
```

Rules:

- Forge/NeoForge class names belong under `implementations/<version>/`.
- ADRs are not tutorials; they record a choice and point to the impl folder.
- Adding 1.21.1 (or later) **starts** by adding a folder + a row in `architecture.yml`.

## Consequences

- README links this tree.
- Port engineers open `implementations/1.20.1/<topic>.md` next to `implementations/1.21.1/<topic>.md` for a side-by-side API map.
