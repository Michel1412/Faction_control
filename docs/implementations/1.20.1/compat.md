# 1.20.1 — Optional compat

Entry: `compat.ModCompatibility.init()` from `FMLCommonSetupEvent`. Gate: `net.minecraftforge.fml.ModList.get().isLoaded(id)`.

## TaCZ (`tacz`)

`compat.tacz.TaczIntegration`:

- Class: `com.tacz.guns.api.event.common.GunFireEvent`
- Register: `MinecraftForge.EVENT_BUS.addListener(HIGH, false, Event → …)` then `eventClass.isInstance`
- Cancel: reflection `isCanceled` / `setCanceled(boolean)`
- Shooter: try `getShooter`, `getEntity`, `getGunOperator`
- Target: try `getTarget`, `getHitEntity`
- Rule: cancel if shooter or target is in admin chunk (Overworld)

`ModItemHelper.isTaczItem` uses `ForgeRegistries.ITEMS.getKey` namespace `tacz` (territory interact exceptions).

## Create (`create`)

`CreateIntegration.register()` sets a boolean; event methods no-op until then.

- Contraption class: `com.simibubi.create.content.contraptions.AbstractContraptionEntity`
- Owner tracking: `ContraptionOwnerTracker` keyed by chunk + tick around assemble
- Block id check: `ForgeRegistries.BLOCKS.getKey` namespace `create`
- Fake player: class name contains `FakePlayer` and `create`

If Create’s 1.20.1 package moves, only this folder’s class names change — territory helper stays.
