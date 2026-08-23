# 1.21.1 — Client

| 1.20.1 | 1.21.1 |
|--------|--------|
| `FMLClientSetupEvent` + `ItemBlockRenderTypes.setRenderLayer` | Prefer `"render_type": "minecraft:cutout"` on the block model; skip the API if possible |
| `RegisterColorHandlersEvent.Item` | `RegisterColorHandlersEvent.Item` in `net.neoforged.neoforge.client.event` |
| `Dist.CLIENT` | `Dist.CLIENT` (`net.neoforged.api.distmarker.Dist`) |
| debug `DEBUG_LOG_PATH` probe | **DROP** |

`ClientFactionData.apply` stays the payload consumer; only the packet type that calls it changes.
