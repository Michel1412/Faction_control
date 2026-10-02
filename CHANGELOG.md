# Changelog

All notable changes to Faction Control are documented in this file.

## [2.1.0] — 2026-10-02 — NeoForge 1.21.1

O módulo Forge 1.20.1 continua em **1.4.1**. Esta versão é só do `neoforge-1.21.1/`.

### Recuperado do jar publicado `faction_control-1.21.1-2.0.0.jar`

- Comandos de membro/Oficial: `/faction leave`, `/faction kick <player>`, `/faction members`, `/faction delete` e `/faction confirm` (confirmação de 30 segundos, só na memória).
- Item `faction_expand_item` (mesmo comportamento de `faction_upgrade_item`), para mundos que já tinham esse id.

### Corrigido

- Loot da bandeira em `data/faction_control/loot_table/` (singular, data pack 1.21.1). Só a metade de baixo dropa o item.
- Mensagens de jogo passam por `Component.translatable`, com `en_us.json` e `pt_br.json`. A aba criativa usa `itemGroup.faction_control.faction_control`.
- Dureza da bandeira deixou de ser -1. Oficial (e OP fora do modo jogador) consegue minerar; quem a policy recusa fica com velocidade de quebra 0. Resistência a explosão continua a de bedrock.
- A bandeira ocupa dois blocos (`half=lower` / `half=upper`). A hitbox de cada metade fica dentro do bloco (o modelo ainda desenha a ponta que passa de x=16). Bandeiras antigas ganham a metade de cima no start do servidor, se o bloco acima estiver livre.
- Notas em `docs/` que diziam que `loot_tables` só viraria `loot_table` no 1.21.2.

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
