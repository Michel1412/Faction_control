# 1.20.1 — Items and stack NBT

## `FactionUpgradeItem`

Bound chunk lives on the **ItemStack compound tag** (not JSON, not player NBT):

| Key | Type | Writer |
|-----|------|--------|
| `SelectedChunkX` | int | `stack.getOrCreateTag().putInt` |
| `SelectedChunkZ` | int | same |

Readers: `stack.getTag()` + `tag.contains(NBT_CHUNK_X)` / `getInt`.

Gameplay (domain): Shift+use or Shift+`useOn` → `bindPlayerChunk`; non-shift `useOn` a `FlagBlock` → `applyUpgradeToFlag`. Validation is `TerritoryProtectionHelper` + `FactionManager.claimSingleChunk`. Consume with `stack.shrink(1)`.

Tooltip: `appendHoverText(ItemStack, @Nullable Level, List<Component>, TooltipFlag)` — the 1.20.1 signature with `Level`.

## `RaidControllerItem`

No custom NBT. `use` → Official check → `startUsingItem`. 

- `getUseDuration(ItemStack)` → `72000`
- `getUseAnimation(ItemStack)` → `UseAnim.SPYGLASS`

Hack progress is **not** on the item; it is `WirelessRaidHackManager` session state.

## `FlagBlockItem`

`BlockItem` + tooltip only. Same `appendHoverText(..., Level, ...)` signature.

## `FlagBlock`

`onRemove(BlockState, Level, BlockPos, BlockState, boolean)` → `FlagHelper.onFlagBlockRemoved`.  
`getExplosionResistance(BlockState, BlockGetter, BlockPos, Explosion)` → bedrock-scale float.
