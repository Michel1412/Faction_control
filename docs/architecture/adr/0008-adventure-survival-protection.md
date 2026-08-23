# ADR-0008: Adventure/Survival remains the protection primitive

- **Status:** Accepted
- **Date:** 2026-08-21
- **Applies to:** `GameModeSyncHandler` + `TerritoryProtectionHelper`

## Context

The mod was designed for a server where **Adventure is the default**. Faction Control elevates players to Survival inside allowed claims and forces Adventure in free world / admin Safezones. Event handlers add rules Adventure cannot express (indestructible containers, interact deny, PvP off).

1.21.1 does not remove Adventure or `ServerPlayer.setGameMode`. The policy is product, not Forge-specific.

## Decision

Port the same table:

| Zone | GameMode | Extra events |
|------|----------|----------------|
| Own / claimed faction chunks | Survival | Break/interact per helper |
| Unclaimed Overworld | Adventure | Vanilla Adventure |
| Admin Safezone | Adventure | Spawn/PvP/TaCZ cancel |
| Non-Overworld | Survival | No territory sync |

OP Admin Mode still bypasses sync. Creative/Spectator still ignored.

Tick still only syncs when chunk / dimension / mode **signature** changes.

## Consequences

- 1.21.1 work is event-class rebinding (`PlayerTickEvent.Post`, etc.), not a new permission engine.
- Any future “don’t use GameMode” redesign would be a separate ADR and a breaking server-policy change.
