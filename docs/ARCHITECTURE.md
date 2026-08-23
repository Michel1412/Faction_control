# Faction Control — Arquitetura

Versão de produto: **1.4.1**  
Versões de Minecraft: ver [architecture/architecture.yml](architecture/architecture.yml)

Este documento descreve **regras de domínio** (camadas, mapas, gate, prioridade). Nomes de classe Forge/NeoForge ficam em `docs/implementations/<mc>/`.

---

## Mapa da documentação de arquitetura

```
docs/
  ARCHITECTURE.md                 ← você está aqui (domínio)
  architecture/
    architecture.yml              ← versões suportadas + índice de ADRs
    adr/                          ← decisões (por que)
  implementations/
    1.20.1/                       ← como está no Forge 47 (produção)
    1.21.1/                       ← o que converter para NeoForge 21.1
```

| Precisa de… | Abra |
|-------------|------|
| Por que duas versões / NeoForge / JSON único | [architecture/adr/](architecture/adr/) |
| Eventos, NBT, `SimpleChannel`, `mods.toml` | [implementations/1.20.1/](implementations/1.20.1/) |
| Inventário de conversão 1.21.1 | [implementations/1.21.1/conversion-inventory.md](implementations/1.21.1/conversion-inventory.md) |
| Schema JSON, raid 60s, tabela de território | [UTILITIES.md](UTILITIES.md) |
| Superfície `/faction` | [COMMANDS.md](COMMANDS.md) / [COMANDOS.md](COMANDOS.md) |

ADRs aceitos: dual-module Gradle (**0001**), NeoForge em 1.21.1 (**0002**), domínio vs plataforma (**0003**), JSON compartilhado (**0004**), NBT vs Data Components (**0005**), flags no player NBT (**0006**), compat por reflexão (**0007**), GameMode Adventure/Survival (**0008**), layout dos docs (**0009**), Java 17/21 (**0010**).

---

## Camadas (de fora para dentro)

```
Comandos / Itens / Integrações opcionais
        ↓
FactionManager          (fachada de ciclo de vida)
        ↓
FactionConfigManager    (fonte da verdade: JSON + mapas O(1))
        ↓
FactionObject           (registro em memória de uma facção)

Hot path (todo tick / clique):
TerritoryProtectionHelper + mapas derivados
        ↑
GameModeSyncHandler / TerritoryProtectionHandler / AdminSafezoneHandler
```

| Camada | Pacote | Responsabilidade |
|--------|--------|------------------|
| Entrada | `command`, `item`, `block` | Jogador age; validação de role e permissão |
| Permissão | `command.FactionPermissions`, `util.FactionPlayerData` | Nível vanilla 1/2, flags do player, nó LuckPerms |
| Domínio | `faction` | Facção, convite, estado da bandeira |
| Persistência | `config.FactionConfigManager` | JSON + HashMaps derivados |
| Território | `util.TerritoryProtectionHelper` | Única regra de “pode ou não” no chunk |
| Eventos | `event` | Aplicam a regra (GameMode, cancel, safezone, raid) |
| Compat | `compat` | Create / TaCZ via reflexão — sem dependência compile |

**Regra de reuso:** handlers de evento não consultam JSON e não duplicam regra de território. Tudo passa por `TerritoryProtectionHelper` + mapas em memória.

O *como* o handler se inscreve no bus é específico da versão: [1.20.1 events](implementations/1.20.1/events.md) vs [1.21.1 events](implementations/1.21.1/events.md).

---

## Prioridade dos fluxos

| Prioridade | Quando corre | Custo alvo |
|------------|--------------|------------|
| **P0 — clique / quebra / tiro** | Evento do jogador | Lookup HashMap O(1); cancelar cedo (client, dimensão, dados não prontos) |
| **P1 — GameMode** | Tick só se chunk/dimensão/modo mudou; eventos de bloco sempre | Evitar `setGameMode` e lookups quando o jogador não andou de chunk |
| **P2 — Raid wireless** | Tick só se o player usa o item **ou** tem sessão ativa | Zero trabalho para o resto do servidor |
| **P3 — Persistência JSON** | Após mutação (uma escrita por ação, via `withSinglePersist`) | Nunca no hot path |
| **P4 — Poll do config** | A cada 100 ticks | `lastModified` em disco; reload só se o arquivo mudou |

---

## Gate de criação de facção

Padrão: **bloqueado**. Três formas de liberar (OR), mais bypass de OP 2.

| Origem | O que é | Quem altera |
|--------|---------|-------------|
| Flag `faction_control.can_create_faction` no player | Boolean (default `false`) | `/faction cancreate <player> true\|false\|toggle` — nível **1** |
| Nó `faction_control.create_faction` | PermissionAPI (LuckPerms) | `lp user <nick> permission set faction_control.create_faction true` |
| OP nível **2+** | Bypass | Operador do servidor |

`/faction create` fica visível com nível **1** (LuckPerms) **ou** se o player já está desbloqueado. A execução ainda exige o gate acima e que o player não esteja em outra facção.

Armazenamento das flags: [1.20.1 player-data](implementations/1.20.1/player-data.md) (1.21.1 mantém as mesmas chaves — [ADR-0006](architecture/adr/0006-player-persistent-data.md)).

---

## Mapas em memória (`FactionConfigManager`)

| Mapa | Lookup |
|------|--------|
| `factionsMap` | UUID da facção → registro |
| `chunkToFactionMap` | Chunk → dono |
| `playerToFactionMap` | Player → facção |
| `nameToFactionMap` | Nome normalizado → UUID |
| `flagChunkToFactionMap` | Chunk da bandeira → UUID |
| `adminChunksSet` | Safezone admin |

Rebuild só no load e em `persist()`. Mutações em lote usam `withSinglePersist` para **uma** escrita JSON. Schema: [ADR-0004](architecture/adr/0004-shared-json-schema.md) — **igual** em 1.20.1 e 1.21.1.

---

## Pontos de falha

### Mitigados em 1.4.0–1.4.1

- Tick de GameMode só se chunk/dimensão/modo mudou.
- Raid Controller só tica sessão ativa ou item em uso.
- Lookups de nome/bandeira em HashMap; persistência em lote.
- Scan vertical da bandeira removido — só `flag_position`.
- Mapas privados + getters; hot path de território em `TerritoryProtectionHelper`.
- Safezone não usa `FactionManager.get` (evita bind no spawn).
- Convites em `pending_invites` no JSON.
- LeftClick/place: um evaluate, sem double-check.

### Ainda abertos

1. **`rebuildDerivedMaps` completo a cada save** — simples e correto; incremental só se o JSON crescer muito.
2. **SafezoneHelper** — `isAdminChunk` e `isStaffProtectedZone` são a mesma checagem.
3. **Cooldown de action bar** não limpa UUID no logout.
4. **Create por reflexão** — bind falha em silêncio se o Create mudar (revalidar no port 1.21.1).
5. **Create nível 1 + flag** — nível 1 só deixa o comando visível; o gate continua flag/nó/OP 2.

---

## Como adicionar feature sem inflar o mod

1. Regra de território nova → método em `TerritoryProtectionHelper`, não em mais um handler.
2. Flag persistida no player → chave em `FactionPlayerData` (mesmo compound).
3. Comando admin → `FactionPermissions.ADMIN` (2). Comando LuckPerms/quest → `TRUSTED` (1) + set/toggle no mesmo padrão de `playmode` / `cancreate`.
4. Não persistir JSON dentro de loops; agrupar com `withSinglePersist`.
5. Não trabalhar em tick de player sem early-return (chunk igual, item errado, sessão vazia).
6. Detalhe de loader (evento, packet, item data) → documentar em `docs/implementations/<mc>/`, não aqui.
