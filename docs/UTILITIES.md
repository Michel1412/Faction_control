# Faction Control — Internal Mechanics

This document describes how core systems are structured under the hood.

---

## Configuration & O(1) Memory Maps

All runtime territory decisions read from in-memory structures rebuilt whenever `config/faction_control.json` is loaded or mutated. The JSON file is the **single source of truth** on disk; maps exist for constant-time lookups.

### File location

```
config/faction_control.json
```

### JSON schema (simplified)

```json
{
  "factions": [
    {
      "name": "ExampleFaction",
      "uuid": "optional-stable-uuid",
      "color": "#FF0000",
      "official_uuid": "leader-player-uuid",
      "members": ["uuid-1", "uuid-2"],
      "flag_state": "ACTIVE",
      "flag_position": {
        "x": 0, "y": 64, "z": 0,
        "dimension": "minecraft:overworld"
      },
      "claimed_chunks": [
        { "x": 0, "z": 0 },
        { "x": 1, "z": 0 }
      ]
    }
  ],
  "admin_chunks": [
    { "x": -7, "z": -8 }
  ]
}
```

### In-memory maps (`FactionConfigManager`)

| Map | Key | Value | Purpose |
|-----|-----|-------|---------|
| `factionsMap` | Faction UUID | `FactionObject` | Full faction records |
| `chunkToFactionMap` | `ChunkPos` | Faction UUID | O(1) chunk ownership lookup |
| `playerToFactionMap` | Player UUID | Faction UUID | O(1) membership lookup |
| `adminChunksSet` | `ChunkPos` | (set membership) | Admin Safezone registry |

`rebuildDerivedMaps()` repopulates the three derived structures from `factionsMap` after every load or write.

### Flag states

| State | Meaning |
|-------|---------|
| `ACTIVE` | Territory protection enforced for outsiders (interaction blocked; containers indestructible for enemies) |
| `RAIDED` | Wireless hack succeeded; outsiders may interact inside territory but still cannot break inventory blocks |

---

## Faction Upgrade Item (Adjacent Chunk Expansion)

**Item ID:** `faction_control:faction_upgrade_item`

### Step 1 — Bind chunk (NBT)

The Official **Shift + right-clicks** (or Shift + uses) while holding the upgrade item.

Backend (`FactionUpgradeItem.bindPlayerChunk`):
1. Reads `player.chunkPosition()`.
2. Rejects admin Safezone chunks (`TerritoryProtectionHelper.canRegisterChunkForUpgrade`).
3. Writes NBT to the item stack:
   - `SelectedChunkX` (int)
   - `SelectedChunkZ` (int)

The selected chunk coordinates travel with the item until consumed.

### Step 2 — Apply to flag

The Official **right-clicks** the faction's `flag_block` (without Shift).

Backend validation chain:
1. Resolve faction from flag block position (`FlagHelper.resolveFactionAtFlag`).
2. Verify executor is the Official.
3. Read bound chunk from item NBT.
4. Reject admin chunks, already-owned chunks, and chunks owned by other factions.
5. **Adjacency check:** `FactionConfigManager.isChunkAdjacentToFactionTerritory` — the candidate chunk must share an edge (not corner) with any chunk already owned by the faction, including the flag chunk.
6. `FactionManager.claimSingleChunk` adds the chunk to the faction's `claimed_chunks` set and updates `chunkToFactionMap`.
7. Item stack shrinks by 1; JSON persisted; success particles spawned at flag.

### Adjacency rule

Two chunks are adjacent when:

```
|deltaX| == 1 && deltaZ == 0   OR   deltaX == 0 && |deltaZ| == 1
```

Diagonal chunks are **not** valid upgrade targets.

---

## Flag Block & Initial Claim

**Command:** `/faction set flag`  
**Block:** `faction_control:flag_block`

When placed:
- Records `flag_position` in JSON.
- Claims the chunk under the flag automatically.
- Flag break policy: only the faction Official (or OP in Admin Mode) may destroy the flag block.

---

## Raid Controller — 60-Second Wireless Hack

**Item ID:** `faction_control:raid_controller_item`

### Activation

1. Attacker stands inside **enemy ACTIVE territory** (must be an outsider relative to chunk owner).
2. **Hold right-click** to begin item use (`RaidControllerItem.use` → `player.startUsingItem`).
3. `WirelessRaidHackHandler` calls `WirelessRaidHackManager.tick` every server tick while the item is held.

### Session lifecycle

| Constant | Value |
|----------|-------|
| `HACK_DURATION_TICKS` | `20 * 60` (60 seconds) |
| Progress display | Action bar `Hackeando... N%` |

**Start conditions** (`tryStartSession`):
- Player chunk is owned by an enemy faction (`TerritoryProtectionHelper.getEnemyFactionAtChunk`).
- Faction `flag_state` is `ACTIVE` (not already raided).
- Creates a `HackSession` in `ACTIVE_SESSIONS` (ConcurrentHashMap keyed by attacker UUID).

**Tick requirements** (session cancelled if violated):
- Attacker keeps holding the Raid Controller.
- Attacker remains inside the **target faction's** claimed territory.
- Attacker stays alive and in the Overworld.

**On completion:**
- Sets target faction `flag_state` → `RAIDED`.
- Persists JSON.
- Consumes one Raid Controller item.
- Broadcasts success to attacker; alerts all online faction members.

### Hack alert milestones

Broadcast to **all online members** of the attacked faction:

| Progress | Channel | Message |
|----------|---------|---------|
| 30% | Chat | ALERTA: hack at 30% |
| 50% | Chat | ALERTA: hack at 50% |
| 80% | Chat | ALERTA: hack at 80% |
| 90% | Action bar | PERIGO! Hack em 90% |

Additionally, the faction Official receives:
- Raid horn sound
- Slowness X while hack is active
- Immediate chat/action bar: `SISTEMAS EXPOSTOS: Voce esta sendo hackeado via wireless!`

### Cancel reasons

| Reason | Trigger |
|--------|---------|
| `RELEASED` | Player stops holding / logs out |
| `LEFT_TERRITORY` | Attacker leaves enemy chunks |
| `DEATH` | Attacker dies |
| `INVALID` | Wrong dimension |
| `COMPLETED` | Hack finished successfully |

---

## Territory Protection Pipeline

### GameMode sync (`GameModeSyncHandler`)

On player tick and block interaction events, resolves expected GameMode via `TerritoryProtectionHelper.resolveExpectedGameMode`:

| Zone | Expected GameMode |
|------|-------------------|
| Own faction / any faction-claimed chunk | `SURVIVAL` |
| Free zone (unclaimed) | `ADVENTURE` |
| Admin Safezone | `ADVENTURE` |
| Non-Overworld | `SURVIVAL` |

### Event denial (`TerritoryProtectionHandler`)

| Action | Rule |
|--------|------|
| **Break** | Anyone may break non-inventory blocks in claimed chunks; inventory blocks indestructible for outsiders |
| **Place / interact** | Outsiders denied in ACTIVE enemy territory (`evaluate()` → event cancel + action bar) |
| **Left-click mining** | Same break rules as above (must not cancel valid enemy mining) |

### Admin Safezone (`AdminSafezoneHandler`)

- Hostile mob spawn cancelled.
- PvP cancelled if **attacker or victim** is in an admin chunk.
- TaCZ gun fire blocked when shooter or reflected target is in admin chunk.

---

## Optional Mod Integrations

| Mod | Behavior |
|-----|----------|
| **Create** | Contraptions only affect chunks owned by the assembler's faction |
| **TaCZ** | Guns usable in enemy territory; blocked in admin Safezones |

Registered via `ModCompatibility` at server start (reflection-based, no hard compile dependency).

---

## Network Sync

`ModNetwork` / `S2CPlayerFactionSyncPacket` pushes faction membership and color data to clients after joins, invites, flag placement, and `/faction reload`.
