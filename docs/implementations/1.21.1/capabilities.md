# 1.21.1 — Capabilities

1.20.1 twin: [../1.20.1/capabilities.md](../1.20.1/capabilities.md).

`ForgeCapabilities.ITEM_HANDLER` is gone.

```java
IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, be, null);
return be instanceof Container || handler != null;
```

Use `net.neoforged.neoforge.capabilities.Capabilities` and the block-entity-aware lookup so Create-style inventories still count as protectable.

Vanilla `Container` check stays first (cheap).
