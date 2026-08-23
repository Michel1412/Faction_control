# Changelog

All notable changes to Faction Control are documented in this file.

## [1.4.1] — 2026-08-18

### Corrigido

- Bandeira: lookup só em `flag_position` salvo. Removido o scan vertical do chunk (16×altura×16).
- Mapas de território encapsulados (getters). Handlers e TaCZ não mutam HashMaps direto.
- Safezone/TaCZ não passam mais por `FactionManager.get` no tick de spawn/tiro.
- Convites persistem em `pending_invites` no JSON (TTL 5 min sobrevive restart).
- Clique esquerdo de mineração: uma checagem (`canBreakBlock`). Place/interact: um `evaluate`.

## [1.4.0] — 2026-08-18

### Adicionado

- Gate de criação de facção: NBT `can_create_faction` (default bloqueado), comando `/faction cancreate <player> [true|false|toggle]` no **nível 1**, e nó LuckPerms `faction_control.create_faction`.
- OP 2+ continua podendo criar sem o flag. Quest/console: `/faction cancreate <nick> true`.
- `docs/ARCHITECTURE.md` — camadas, prioridade de fluxos e pontos de falha.

### Alterado

- `/faction create` deixa de exigir OP 2: visível no nível **1** (LuckPerms) ou se o player já estiver desbloqueado.
- GameMode no tick só sincroniza quando chunk, dimensão ou modo mudam.
- Raid Controller não tica mais todos os players: só quem usa o item ou tem sessão ativa.
- Lookups de nome e chunk da bandeira em HashMap; persistência em lote (`withSinglePersist`) para não gravar o JSON várias vezes na mesma ação.

### Corrigido

- `/faction create` recusa player que já pertence a uma facção.
- Flags NBT (`play_as_player`, `can_create_faction`) sobrevivem à morte (`PlayerEvent.Clone`).

## [1.3.6] — 2026-07-18

### Alterado

- Raid Controller: uso restrito ao **Oficial** da facção. Membros e jogadores sem facção recebem mensagem de bloqueio e não iniciam o hack.

## [1.3.5] — 2026-07-18

### Corrigido

- Raid Controller: Lentidão 10 (Slowness X) agora afeta **apenas** o jogador que está usando o item. O Oficial inimigo e demais jogadores próximos não recebem mais o debuff.

## [1.3.4] — 2026-07-07

### Corrigido

- Texturas 16x16 dos itens **Faction Upgrade** (`faction_upgrade_item`) e **Raid Controller** (`raid_controller_item`) aplicadas corretamente no inventário e na aba criativa.
- Ícone GUI da **Bandeira** (`flag_block`): textura plana dedicada no inventário/creative, mantendo o modelo 3D ao segurar o item ou colocar no mundo (`forge:separate_transforms`).
- Ícone da aba criativa **Faction Control** atualizado para usar o item da bandeira em vez do banner vanilla.

## [1.3.3] — 2026-07-02

### Alterado

- Balanceamento de território: inimigos podem quebrar blocos sem inventário em chunks claimados; containers indestrutíveis para forasteiros.
- Safezone admin: PvP desativado se atacante ou vítima estiver no chunk admin.

### Adicionado

- Documentação em `docs/COMMANDS.md`, `docs/UTILITIES.md` e `README.md`.

## [1.3.2] — versões anteriores

- Convites com clique no chat, alertas de hack wireless, integrações opcionais Create/TaCZ, modelo da bandeira Blockbench.
