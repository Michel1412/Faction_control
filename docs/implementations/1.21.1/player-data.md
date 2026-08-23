# 1.21.1 — Player persistent data

Keep keys (ADR-0006):

- compound `faction_control`
- `play_as_player`, `can_create_faction`

`Player.getPersistentData()` still exists.

## Clone

Subscribe to `net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone`.

On NeoForge 1.21.1, try copying **without** `reviveCaps()` first (caps API changed). If the original compound is empty, restore the Forge pattern only if the entity still exposes it.

Do not introduce Attachments in the first port.

Bus: `Bus.GAME`.
