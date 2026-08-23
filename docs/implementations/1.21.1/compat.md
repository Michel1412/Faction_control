# 1.21.1 — Compat (verify before coding)

1.20.1 twins: [../1.20.1/compat.md](../1.20.1/compat.md). ADR-0007: still no compile dependency.

Before writing 1.21.1 `compat` classes, open the actual NeoForge jars:

## TaCZ

- Confirm mod id is still `tacz`.
- Confirm `com.tacz.guns.api.event.common.GunFireEvent` exists and is a cancellable NeoForge `Event`.
- Register on `NeoForge.EVENT_BUS`.
- If the 1.21.1 port uses a different event name or a custom bus, log a warning and skip (Safezone guns degrade).

## Create

- Confirm mod id `create` on NeoForge 1.21.1 (Create 6.x).
- Re-check `com.simibubi.create.content.contraptions.AbstractContraptionEntity`.
- Block namespace lookup: `BuiltInRegistries.BLOCK.getKey` (or NeoForge wrapper), namespace `create`.

`ContraptionOwnerTracker` can be copied as-is.

If either mod is not available for 1.21.1 NeoForge at port time, ship without that integration and keep the reflection stubs compiling.
