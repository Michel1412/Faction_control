# Faction Control — Command Reference

All commands are registered under the root literal `/faction`. Unless noted, commands require the executor to be a **player** (not the console).

Persistent data is written to `config/faction_control.json` after most mutating operations.

---

## Permission Model

| Role | Requirement | Territory rules |
|------|-------------|-----------------|
| **Player** | No OP | Full faction gameplay rules apply |
| **Faction Official (Leader)** | Member + leader UUID | Can set flag, invite, apply chunk upgrades |
| **OP (Admin Mode)** | OP level **2+**, default play mode | Bypasses territory GameMode sync and event denial |
| **OP (Player Mode)** | OP level **2+**, `/faction playmode true` | Same restrictions as a normal player |

OP play mode is stored in the player's persistent data under `faction_control.play_as_player` ([1.20.1](implementations/1.20.1/player-data.md), [1.21.1](implementations/1.21.1/player-data.md)).

---

## Player Commands

### `/faction set flag`

| | |
|---|---|
| **Permission** | Faction Official |
| **Dimension** | Overworld only |

**Backend behavior:**
- Validates the player belongs to a faction and is its Official.
- Ensures the faction has no active flag already registered in the world.
- Requires two blocks of vertical clearance at the player's feet.
- Spawns a `flag_block` at the player's position via `FlagHelper.spawnFactionFlag`.
- Claims the current chunk (`player.chunkPosition()`) for the faction in `chunkToFactionMap`.
- Persists `flag_position`, `flag_state` (`ACTIVE`), and `claimed_chunks` to JSON.
- Sends an S2C faction sync packet to the client.

---

### `/faction invite <player>`

| | |
|---|---|
| **Permission** | Faction Official |

**Backend behavior:**
- Creates an invite via `FactionInviteManager.createInvite` and writes it to `pending_invites` in JSON (TTL 5 minutes, survives restart).
- Sends a clickable `[ACCEPT]` chat message to the target (`/faction accept`).

---

### `/faction accept`

| | |
|---|---|
| **Permission** | Any player without a faction |

**Backend behavior:**
- Consumes a pending invite from `FactionInviteManager`.
- Adds the player's UUID to the faction member list and `playerToFactionMap`.
- Saves JSON and syncs faction data to the client.

---

## Administrative Commands (OP Level 2+)

All commands below use `.requires(FactionPermissions::isAdmin)` (vanilla level 2) unless noted.

### `/faction create <nome> <cor>`

| | |
|---|---|
| **Visibility** | Vanilla level **1** (LuckPerms) **or** player already unlocked |
| **Runtime gate** | NBT `faction_control.can_create_faction` **or** Forge node `faction_control.create_faction` **or** OP 2+ |

**Backend behavior:**
- Rejects players who already belong to a faction.
- Validates unique faction name and `#RRGGBB` color hex.
- Creates a `FactionObject`, sets executor as Official/member, persists JSON.

Staff with level 1 still cannot create unless the NBT flag or LuckPerms node is set. OP 2+ always can.

### `/faction cancreate <player>`

| | |
|---|---|
| **Permission** | Vanilla level **1** (LuckPerms, quest reward, console) |

Shows NBT flag and effective unlock (flag / LuckPerms node / OP).

#### `/faction cancreate <player> toggle`
#### `/faction cancreate <player> <true|false>`

Sets or inverts persistent NBT `can_create_faction`. Quest example:

```
/faction cancreate Steve true
```

LuckPerms node (no command needed):

```
lp user Steve permission set faction_control.create_faction true
```

---

### `/faction delete_force <nome>`

**Backend behavior:**
- Resolves faction by name, strips world flag blocks, removes all claims and members.
- Deletes faction entry from `factionsMap` and rebuilds derived maps.

---

### `/faction join_forced <nome>`

**Backend behavior:**
- Force-adds the executing player to the target faction (bypasses invite flow).
- Updates `playerToFactionMap` and saves JSON.

---

### `/faction leave_force <player>`

**Backend behavior:**
- Removes target player UUID from their faction via `FactionManager.forceRemoveMember`.
- Rebuilds maps and syncs client data.

---

### `/faction set_leader <player> <nome>`

**Backend behavior:**
- Promotes target to Official (`official_uuid`) of the named faction.
- Target must already be a member.

---

### `/faction admin_claim`

| | |
|---|---|
| **Dimension** | Overworld only |

**Backend behavior:**
- Adds the OP's current chunk to `admin_chunks` in JSON and `adminChunksSet`.
- Chunk becomes an admin **Safezone** (Adventure GameMode, PvP off, hostile mob spawn blocked).

---

### `/faction admin_unclaim`

**Backend behavior:**
- Removes current chunk from `admin_chunks` / `adminChunksSet`.

---

### `/faction admin_setchunk <nome>`

**Backend behavior:**
- Force-claims the OP's current chunk for the named faction without adjacency or item checks.
- Updates `claimed_chunks` for that faction in JSON.

---

### `/faction admin_removechunk <nome>`

**Backend behavior:**
- Removes the OP's current chunk from the named faction's claims if owned.

---

### `/faction list`

**Backend behavior:**
- Lists all factions sorted by name with online/total member counts (read-only).

---

### `/faction info <nome>`

**Backend behavior:**
- Displays faction name, Official, flag coordinates, `FlagState`, and total claimed chunk count.

---

### `/faction reload`

**Backend behavior:**
- Reloads `config/faction_control.json` into memory HashMaps via `FactionManager.reload`.
- Re-sends faction sync packets to all online players.

---

### `/faction playmode`

**Backend behavior:**
- Shows whether the OP is in **Admin Mode** (territory bypass) or **Player Mode** (rules apply).

#### `/faction playmode toggle`
#### `/faction playmode <true|false>`

**Backend behavior:**
- Toggles or sets `play_as_player` NBT flag controlling `PlayerPlayModeHelper.isSubjectToTerritoryRules`.

---

### `/faction debug clicks`

**Backend behavior:**
- Shows whether `[FACTION CLICK]` / `[FACTION GAMEMODE]` debug logging is enabled (in-memory only, not persisted).

#### `/faction debug clicks toggle`
#### `/faction debug clicks <true|false>`

**Backend behavior:**
- Enables/disables verbose interaction logging in `TerritoryInteractionLogHandler` and `GameModeSyncHandler`.

---

## Command Permission Matrix

| Command | Player | Member | Official | OP (Admin Mode) | OP (Player Mode) |
|---------|:------:|:------:|:--------:|:---------------:|:----------------:|
| `set flag` | | | ✓ | ✓* | ✓ |
| `invite` / `accept` | | ✓ | ✓ | ✓* | ✓ |
| `create` | ✓† | | | ✓ | ✓ |
| `cancreate` | | | | ✓‡ | ✓‡ |
| `delete_force` | | | | ✓ | ✓ |
| `join_forced` / `leave_force` | | | | ✓ | ✓ |
| `set_leader` | | | | ✓ | ✓ |
| `admin_claim` / `admin_unclaim` | | | | ✓ | ✓ |
| `admin_setchunk` / `admin_removechunk` | | | | ✓ | ✓ |
| `list` / `info` / `reload` | | | | ✓ | ✓ |
| `playmode` / `debug clicks` | | | | ✓ | ✓ |

\*OP must be the faction Official **and** in Player Mode to experience the same territory restrictions as members.  
†Requires NBT flag, LuckPerms node `faction_control.create_faction`, or OP 2. Level 1 only makes the command visible.  
‡Vanilla level **1** (LuckPerms / quest / console).

---

## Related Documentation

- Territory mechanics and JSON schema: [UTILITIES.md](UTILITIES.md)
- Server architecture, hot paths, failure points: [ARCHITECTURE.md](ARCHITECTURE.md)
- Server design philosophy: [README.md](../README.md)
