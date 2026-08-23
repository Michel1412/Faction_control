# 1.20.1 — Permissions

`command.FactionPermissions`:

| Constant | Vanilla level | Use |
|----------|---------------|-----|
| `TRUSTED` | 1 | LuckPerms-visible helper commands (`cancreate`, create visibility) |
| `ADMIN` | 2 | `/faction` admin tree; OP bypass of create gate |

Forge node:

```java
new PermissionNode<>(MODID, "create_faction", PermissionTypes.BOOLEAN, (player, uuid, ctx) -> false)
```

Registered on `PermissionGatherEvent.Nodes`. Runtime: `PermissionAPI.getPermission(player, CREATE_FACTION)`.

Full node string for LuckPerms: `faction_control.create_faction`.

Packages: `net.minecraftforge.server.permission.*`.

Command registration: `RegisterCommandsEvent` → `FactionCommands.register`.
