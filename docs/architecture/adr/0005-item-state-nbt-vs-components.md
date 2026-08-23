# ADR-0005: Item state — NBT on 1.20.1, Data Components on 1.21.1

- **Status:** Accepted
- **Date:** 2026-08-21
- **Applies to:** `FactionUpgradeItem` bound chunk

## Context

On 1.20.1 the upgrade item stores the selected chunk on the stack:

- `SelectedChunkX` (int)
- `SelectedChunkZ` (int)

via `ItemStack.getOrCreateTag()` / `getTag()`.

Minecraft 1.20.5+ replaced arbitrary item NBT with **Data Components**. 1.21.1 NeoForge registers custom `DataComponentType`s. Writing `CompoundTag` on the stack is no longer the supported path and will not sync/tooltip reliably.

Player-level flags are a different store (ADR-0006).

## Decision

- **1.20.1:** keep custom NBT keys on the item (documented in `docs/implementations/1.20.1/items-nbt.md`).
- **1.21.1:** register a custom component (e.g. `faction_control:selected_chunk` with `x`/`z` ints) and read/write it from `FactionUpgradeItem`.
- Logical meaning stays: Shift+use binds `player.chunkPosition()`; apply on flag consumes the stack.

Do not invent a JSON-side “pending upgrade chunk” to avoid components.

## Consequences

- Upgrade items bound in 1.20.1 worlds will **not** automatically carry the chunk into 1.21.1 if someone copies the player inventory NBT blindly — operators re-bind the chunk (one Shift+click). Document this in the 1.21.1 port notes.
- Tooltip / bind / apply code is platform-specific; validation (`canRegisterChunkForUpgrade`, adjacency) stays domain.
