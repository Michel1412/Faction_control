# 1.20.1 — Capabilities (inventory blocks)

`util.BlockInventoryHelper.hasProtectableInventory`:

1. `state.hasBlockEntity()`
2. `level.getBlockEntity(pos)`
3. `blockEntity instanceof Container` **or**
4. `blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()`

This is why chests, Create inventories, and other capability-backed containers are indestructible for outsiders even when they are not vanilla `Container`.

`ForgeCapabilities` is a 1.20.1 Forge type. NeoForge 1.21.1 uses `Capabilities.ItemHandler.BLOCK` (see 1.21.1 capabilities doc).
