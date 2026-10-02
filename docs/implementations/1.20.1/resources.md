# 1.20.1 — Resources

Pack format **15** (`src/main/resources/pack.mcmeta`).

## Assets

| Path | Role |
|------|------|
| `assets/faction_control/blockstates/flag_block.json` | blockstate |
| `assets/faction_control/models/block/flag_block.json` | world / hand 3D |
| `assets/faction_control/models/item/flag_block.json` | **`forge:separate_transforms`**: 3D in hand, generated GUI/fixed |
| `assets/faction_control/models/item/faction_upgrade_item.json` | `item/generated` |
| `assets/faction_control/models/item/raid_controller_item.json` | `item/generated` |
| `assets/faction_control/textures/item/*.png` | item textures |
| `assets/faction_control/lang/en_us.json` | lang |

`forge:separate_transforms` is a **Forge 1.20.1 loader**. 1.21.1 NeoForge uses `neoforge:separate_transforms` (or equivalent). Do not change this JSON in the 1.20.1 tree to the NeoForge key.

## Data

`data/faction_control/loot_tables/blocks/flag_block.json` — vanilla `minecraft:block` loot with `survives_explosion`.

On 1.21.1 the folder is already `loot_table` (singular). The plural name is only correct for this 1.20.1 tree.

## Client render

Cutout layer is set in code: `ItemBlockRenderTypes.setRenderLayer(ModBlocks.FLAG_BLOCK.get(), RenderType.cutout())` during `FMLClientSetupEvent`. The block model JSON does not set `render_type` on 1.20.1.
