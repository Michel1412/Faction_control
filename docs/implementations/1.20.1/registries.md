# 1.20.1 — Registries

All registries use Forge `DeferredRegister` + `RegistryObject`.

| Registry | Class | Forge API |
|----------|-------|-----------|
| Blocks | `registry.ModBlocks` | `DeferredRegister.create(ForgeRegistries.BLOCKS, MODID)` |
| Items | `registry.ModItems` | `DeferredRegister.create(ForgeRegistries.ITEMS, MODID)` |
| Creative tabs | `registry.ModCreativeTabs` | `DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID)` |

Registered ids:

- Block/item `flag_block` — `FlagBlock` / `FlagBlockItem` (`BlockItem` wrapping `ModBlocks.FLAG_BLOCK.get()`)
- Item `faction_upgrade_item` — `stacksTo(1)`
- Item `raid_controller_item` — `stacksTo(1)`
- Tab `faction_control` — icon is the flag item

`FlagBlock.properties()`: `BlockBehaviour.Properties.of()`, metal sound, `strength(-1, 3600000)`, light 15, `noOcclusion()`. Shape is two blocks tall (`Block.box(4, 0, 4, 18, 32, 12)`).

Lookups elsewhere: `ForgeRegistries.BLOCKS.getKey` / `ForgeRegistries.ITEMS.getKey` (`CreateIntegration`, `ModItemHelper`).
