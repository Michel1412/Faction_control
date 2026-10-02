# 1.21.1 — Resources

| Asset | Change |
|-------|--------|
| `pack.mcmeta` | `pack_format`: **34** |
| `models/item/flag_block.json` | loader `forge:separate_transforms` → **`neoforge:separate_transforms`** (same `base` / `perspectives` structure) |
| `models/block/flag_block.json` | add `"render_type": "minecraft:cutout"` so we can drop `ItemBlockRenderTypes.setRenderLayer` |
| other models, textures, lang | copy |
| `data/.../loot_table/blocks/flag_block.json` | `loot_table` (singular) on 1.21.1; only the lower half drops the item |

1.21.1 has **not** switched to the 1.21.4 item-model rewrite (`items/*.json`). Stay on `models/item`.
