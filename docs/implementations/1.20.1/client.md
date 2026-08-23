# 1.20.1 — Client

`client.ClientModEvents` — `@Mod.EventBusSubscriber(..., bus = Bus.MOD, value = Dist.CLIENT)`.

| Event | Action |
|-------|--------|
| `FMLClientSetupEvent` | `ItemBlockRenderTypes.setRenderLayer(flag, RenderType.cutout())` |
| `RegisterColorHandlersEvent.Item` | no-op tint `0xFFFFFF` on upgrade + raid controller |

`ClientFactionData` holds the last S2C faction snapshot (no Forge types).

`ItemTextureColors` is documentation of the PNG palette only.

There is a leftover debug file probe in `ClientModEvents` (`DEBUG_LOG_PATH`). It is not part of the 1.21.1 port contract — drop it on the new module.
