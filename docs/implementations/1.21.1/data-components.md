# 1.21.1 — Data Components (upgrade item)

1.20.1 twin: [../1.20.1/items-nbt.md](../1.20.1/items-nbt.md). ADR: [0005](../../architecture/adr/0005-item-state-nbt-vs-components.md).

## Register

New `DataComponentType<SelectedChunk>` (record `int x, int z`) on a `DeferredRegister<DataComponentType<?>>` for `Registries.DATA_COMPONENT_TYPE`.

Persistent + network: `.persistent(Codec.INT` pair or a small `RecordCodecBuilder`) `.networkSynchronized(ByteBufCodecs` / composite).

Suggested id: `faction_control:selected_chunk`.

## `FactionUpgradeItem` mapping

| 1.20.1 | 1.21.1 |
|--------|--------|
| `stack.getTag()` / `contains("SelectedChunkX")` | `stack.get(ModDataComponents.SELECTED_CHUNK)` |
| `stack.getOrCreateTag().putInt` | `stack.set(ModDataComponents.SELECTED_CHUNK, new SelectedChunk(x, z))` |
| `appendHoverText(..., Level, ...)` | `appendHoverText(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)` |
| `use` / `useOn` | same flow; `InteractionResult` may be `ItemInteractionResult` on block use — **compile-check** |

Do not write a custom `CompoundTag` onto the stack.

## Other item signatures

`RaidControllerItem.getUseDuration(ItemStack, LivingEntity)` — extra entity argument.

`UseAnim.SPYGLASS` still valid for the 60s hold.

## Migration

Inventories copied from a 1.20.1 world will not show a bound chunk until the Official Shift+clicks again. No JSON migration.
