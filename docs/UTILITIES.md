# Faction Control — Internal Mechanics

This document describes **domain** mechanics: JSON schema, raid flow, protection table, upgrade adjacency.

Loader-specific APIs (Forge events, item NBT, packets) live next to the Minecraft version:

- [1.20.1 implementation](implementations/1.20.1/) — current production
- [1.21.1 conversion](implementations/1.21.1/conversion-inventory.md) — planned NeoForge port

---

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
  ],
  "pending_invites": [
    {
      "target_uuid": "player-uuid",
      "faction_uuid": "faction-uuid",
      "inviter_uuid": "official-uuid",
      "expires_at_ms": 0
    }
  ]
}
```

### In-memory maps (`FactionConfigManager`)

| Map | Key | Value | Purpose |
|-----|-----|-------|---------|
| `factionsMap` | Faction UUID | `FactionObject` | Full faction records |
| `chunkToFactionMap` | `ChunkPos` | Faction UUID | O(1) chunk ownership lookup |
| `playerToFactionMap` | Player UUID | Faction UUID | O(1) membership lookup |
| `nameToFactionMap` | Lowercased name | Faction UUID | O(1) `/faction` name resolve |
| `flagChunkToFactionMap` | Chunk long key | Faction UUID | O(1) flag-chunk owner |
| `adminChunksSet` | `ChunkPos` | (set membership) | Admin Safezone registry |

`rebuildDerivedMaps()` repopulates derived structures from `factionsMap` after every load or write. Multi-step mutations use `withSinglePersist` so the JSON is written once.

---

## Create-faction gate

Default: **blocked**. Stored on the player under persistent compound `faction_control.can_create_faction` (boolean, default false). Copied on death (clone hook is version-specific: [1.20.1](implementations/1.20.1/player-data.md)).

Unlock (OR):

1. `/faction cancreate <player> true` (or `toggle`) — vanilla permission **level 1**
2. LuckPerms / Forge PermissionAPI node `faction_control.create_faction`
3. OP level **2+** (bypass)

See [ARCHITECTURE.md](ARCHITECTURE.md) and [COMANDOS.md](COMANDOS.md).

### Flag states

| State | Meaning |
|-------|---------|
| `ACTIVE` | Territory protection enforced for outsiders (interaction blocked; containers indestructible for enemies) |
| `RAIDED` | Wireless hack succeeded; outsiders may interact inside territory but still cannot break inventory blocks |

---

## Faction Upgrade Item (Adjacent Chunk Expansion)

**Item ID:** `faction_control:faction_upgrade_item`

### Step 1 — Bind chunk

The Official **Shift + right-clicks** (or Shift + uses) while holding the upgrade item.

Backend (`FactionUpgradeItem.bindPlayerChunk`):
1. Reads `player.chunkPosition()`.
2. Rejects admin Safezone chunks (`TerritoryProtectionHelper.canRegisterChunkForUpgrade`).
3. Stores the chunk on the **item** until consumed:
   - **1.20.1:** stack NBT `SelectedChunkX` / `SelectedChunkZ` — [items-nbt.md](implementations/1.20.1/items-nbt.md)
   - **1.21.1:** Data Component `faction_control:selected_chunk` — [data-components.md](implementations/1.21.1/data-components.md)

### Step 2 — Apply to flag

The Official **right-clicks** the faction's `flag_block` (without Shift).

Backend validation chain:
1. Resolve faction from flag block position (`FlagHelper.resolveFactionAtFlag`).
2. Verify executor is the Official.
3. Read bound chunk from the item (NBT on 1.20.1, Data Component on 1.21.1).
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

1. Attacker must be the **Official** of their own faction (members and unaffiliated players are blocked).
2. Attacker stands inside **enemy ACTIVE territory** (must be an outsider relative to chunk owner).
3. **Hold right-click** to begin item use (`RaidControllerItem.use` → `player.startUsingItem`).
4. `WirelessRaidHackHandler` calls `WirelessRaidHackManager.tick` every server tick while the item is held.

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

Additionally:
- The **hacker** (player holding the Raid Controller) receives Slowness X while the hack is active — no other players are affected.
- The enemy faction Official receives a raid horn sound and chat/action bar: `SISTEMAS EXPOSTOS: Voce esta sendo hackeado via wireless!`

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

`S2CPlayerFactionSyncPacket` pushes faction membership and color data to clients after joins, invites, flag placement, and `/faction reload`.

- **1.20.1:** Forge `SimpleChannel` — [networking.md](implementations/1.20.1/networking.md)
- **1.21.1:** `CustomPacketPayload` — [networking.md](implementations/1.21.1/networking.md)
