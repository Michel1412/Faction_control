# 1.21.1 — Permissions

Same vanilla levels (`TRUSTED=1`, `ADMIN=2`) and LuckPerms node **`faction_control.create_faction`**.

Packages:

- `net.neoforged.neoforge.server.permission.PermissionAPI`
- `PermissionGatherEvent.Nodes`
- `PermissionNode` / `PermissionTypes`

`PermissionNode` constructor still takes `(namespace, nodeName, type, defaultResolver)`.

`RegisterCommandsEvent` lives in `net.neoforged.neoforge.event`.
