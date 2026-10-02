# Faction Control — Referência de Comandos

Versão Forge 1.20.1: **1.4.1**. Versão NeoForge 1.21.1: **2.1.0**.  
Prefixo base: `/faction`

Todos os dados de facções, chunks claimados, bandeiras e safezones ficam em `config/faction_control.json`.

Gate de criação: por padrão o player **não** pode criar facção. LuckPerms, quest ou staff liberam com o comando ou o nó abaixo.

---

## Papéis (Roles)

| Papel | Quem é | Permissão Minecraft |
|-------|--------|---------------------|
| **Jogador** | Qualquer player no servidor | Padrão (0) |
| **Membro** | Player listado em `members` de uma facção | Padrão (0) |
| **Oficial** | Player definido em `official_uuid` da facção | Padrão (0) |
| **Administrador (OP)** | Operador do servidor | Nível **2+** (`/op`) |
| **Trusted (LuckPerms)** | Helper / quest / console | Nível **1** |

Nível 1 serve para LuckPerms gerir helpers sem dar OP 2. Staff 2+ bypassa o gate de criação.

### Modo Admin vs Modo Jogador (OP)

OPs têm dois modos, persistidos no personagem (`faction_control.play_as_player` — [1.20.1](implementations/1.20.1/player-data.md)):

| Modo | Comando | Comportamento |
|------|---------|---------------|
| **Modo Admin** (padrão) | `/faction playmode false` | Bypass de território: o mod **não** força Adventure/Survival nem bloqueia quebra/interação. Ideal para construir, moderar ou inspecionar bases. |
| **Modo Jogador** | `/faction playmode true` | Regras normais de território e GameMode aplicam-se ao OP como a qualquer membro/invasor. Ideal para testar ou jogar PvP/facções de verdade. |

---

## Comandos — Oficial da Facção

Requer: pertencer à facção **e** ser o **Oficial** (`official_uuid`).

### `/faction set flag`

Posiciona a **bandeira** no bloco onde o Oficial está e claima o chunk atual.

| | |
|---|---|
| **Uso** | `/faction set flag` |
| **Dimensão** | Overworld apenas |
| **Pré-requisitos** | Facção sem bandeira ativa no mundo; bloco na posição do player substituível (ar/espaço) |
| **Efeito** | Spawna `flag_block` em dois blocos de altura; o Oficial (e OP em Modo Admin) consegue quebrar; invasor não. Salva `flag_position` no JSON; claima o chunk da bandeira |
| **Erros comuns** | Não está em facção; não é Oficial; bandeira já ativa; posição bloqueada |

### `/faction invite <player>`

Envia um convite para o jogador entrar na facção. O convidado recebe uma mensagem no chat com botão **[ACEITAR]** clicável.

```
/faction invite Steve
```

| | |
|---|---|
| **Pré-requisitos** | Alvo online; alvo sem facção; não convidar a si mesmo |
| **Validade** | Convite expira em **5 minutos**; persiste em `pending_invites` no JSON (sobrevive restart) |
| **Efeito** | Alvo pode usar `/faction accept` ou clicar **[ACEITAR]** no chat |

---

## Comandos — Membro / Jogador (sem OP)

### `/faction accept`

Aceita um convite pendente e entra na facção que convidou.

```
/faction accept
```

| | |
|---|---|
| **Pré-requisitos** | Convite pendente e não expirado; jogador sem facção |
| **Alternativa** | Clicar **[ACEITAR]** na mensagem de convite no chat |

### `/faction leave` (NeoForge 1.21.1)

Membro sai da facção. O Oficial não pode sair: precisa transferir a liderança antes.

### `/faction kick <player>` (NeoForge 1.21.1)

Oficial expulsa um membro pelo nome (cache do servidor). Não expulsa a si mesmo.

### `/faction members` (NeoForge 1.21.1)

Lista os membros da própria facção, com Oficial e online/offline.

### `/faction delete` e `/faction confirm` (NeoForge 1.21.1)

O Oficial pede a exclusão e confirma em até **30 segundos**. Some a bandeira, os claims e a facção. A confirmação não sobrevive a restart.

**Itens relacionados (não são comandos):**

| Item | Quem usa | Função |
|------|----------|--------|
| `faction_upgrade_item` | **Oficial** | Shift+Clique vincula chunk; clique na bandeira expande território (chunk adjacente) |
| `faction_expand_item` | **Oficial** | Mesmo uso do upgrade. Id extra recuperado do jar 2.0.0 (só NeoForge 1.21.1) |
| `raid_controller_item` | Oficial da facção | Hack wireless 60s dentro de território inimigo → estado `RAIDED` |

**Alertas durante o hack wireless** (membros online da facção atacada):

| Progresso | Canal | Mensagem |
|-----------|-------|----------|
| Início | Chat + actionbar (Oficial) | `SISTEMAS EXPOSTOS: Voce esta sendo hackeado via wireless!` |
| 30% | Chat | `ALERTA: Sua faccao esta sendo hackeada! Progresso: 30%` |
| 50% | Chat | `... Progresso: 50%` |
| 80% | Chat | `... Progresso: 80%` |
| 90% | Tela (actionbar) | `PERIGO! Hack em 90% - Territorio quase comprometido!` |
| 100% | Chat | `Sua bandeira foi hackeada! O territorio esta exposto a invasores.` |

---

## Comandos — Administrador (OP nível 2+)

### Modo de jogo do OP

#### `/faction playmode`

Mostra se você está em **Modo Jogador** ou **Modo Admin**.

```
/faction playmode
```

#### `/faction playmode toggle`

Alterna entre Modo Admin e Modo Jogador.

```
/faction playmode toggle
```

#### `/faction playmode <true|false>`

Define explicitamente o modo.

```
/faction playmode true   → Modo Jogador (regras de território ativas)
/faction playmode false  → Modo Admin (bypass)
```

---

### Debug e diagnóstico

#### `/faction debug clicks`

Mostra se os logs de clique do mod estão ativos no console do servidor.

```
/faction debug clicks
```

| | |
|---|---|
| **Padrão** | **DESATIVADOS** (melhor TPS em produção) |
| **Persistência** | Somente em memória — volta a desligado após restart |

#### `/faction debug clicks toggle`

Alterna logs `[FACTION CLICK]` e `[FACTION GAMEMODE]`.

```
/faction debug clicks toggle
```

#### `/faction debug clicks <true|false>`

```
/faction debug clicks true   → ativa logs de clique
/faction debug clicks false  → desativa logs de clique
```

---

### Gestão de facções

### Comandos — Jogador desbloqueado / LuckPerms nível 1

### `/faction create <nome> <cor_hex>`

Cria uma facção e define quem executou o comando como **Oficial** e primeiro membro.

```
/faction create Guerreiros #FF0000
```

| | |
|---|---|
| **Visibilidade** | Nível **1** (LuckPerms) **ou** player já desbloqueado |
| **Gate** | NBT `can_create_faction=true` **ou** nó `faction_control.create_faction` **ou** OP 2+ |
| **cor_hex** | Formato `#RRGGBB` (ex.: `#00FF00`) |
| **Bloqueios** | Já pertence a uma facção; nome repetido; cor inválida; gate fechado |

### `/faction cancreate <player>`

Mostra a flag NBT e o resultado efetivo (flag / LuckPerms / OP).

```
/faction cancreate Steve
```

| | |
|---|---|
| **Permissão** | Nível **1** (LuckPerms, quest, console) |

#### `/faction cancreate <player> toggle`

Inverte a flag NBT.

```
/faction cancreate Steve toggle
```

#### `/faction cancreate <player> <true|false>`

Define a flag. Quest (FTB Quests e similares) ou LuckPerms podem executar:

```
/faction cancreate Steve true
/faction cancreate Steve false
```

LuckPerms (nó direto, sem comando):

```
lp user Steve permission set faction_control.create_faction true
lp group cidadao permission set faction_control.create_faction true
```

---

### Gestão de facções (OP 2+)

#### `/faction delete_force <nome>`

Remove uma facção **permanentemente**.

```
/faction delete_force Guerreiros
```

| | |
|---|---|
| **Efeito** | Remove bandeira do mundo, libera claims, apaga facção do JSON |
| **Atenção** | Irreversível (sem backup manual do JSON) |

#### `/faction join_forced <nome>`

Força o **próprio OP** a entrar na facção indicada (remove de facção anterior se houver).

```
/faction join_forced Guerreiros
```

#### `/faction leave_force <player>`

Remove um jogador da facção atual dele.

```
/faction leave_force Steve
```

#### `/faction set_leader <player> <nome_faccao>`

Promove um membro existente a **Oficial** da facção.

```
/faction set_leader Steve Guerreiros
```

| | |
|---|---|
| **Pré-requisito** | `<player>` já deve ser membro da facção |

---

### Chunks e safezones

#### `/faction admin_claim`

Registra o chunk onde o OP está como **Safezone de Administradores** (`admin_chunks` no JSON).

```
/faction admin_claim
```

| | |
|---|---|
| **Dimensão** | Overworld |
| **Efeito na gameplay** | GameMode **Adventure**; PvP desativado se atacante **ou** vítima estiver no chunk; mobs hostis não nascem; baús/containers liberados; quebra de blocos bloqueada (vanilla Adventure); **não** permite upgrade de facção nesse chunk; **TaCZ bloqueado** no chunk admin |

#### `/faction admin_unclaim`

Remove o chunk atual da lista de safezones admin.

```
/faction admin_unclaim
```

#### `/faction admin_setchunk <nome_faccao>`

Claima **forçadamente** o chunk atual para a facção (ignora adjacência e dono anterior).

```
/faction admin_setchunk Guerreiros
```

#### `/faction admin_removechunk <nome_faccao>`

Remove o chunk atual dos claims da facção.

```
/faction admin_removechunk Guerreiros
```

---

### Consulta e manutenção

#### `/faction list`

Lista todas as facções com contagem de membros online/total.

```
/faction list
```

#### `/faction info <nome_faccao>`

Detalhes: Oficial, posição da bandeira, estado (`ACTIVE` / `RAIDED`), quantidade de chunks.

```
/faction info Guerreiros
```

#### `/faction reload`

Recarrega `config/faction_control.json` do disco e sincroniza facção com todos os players online.

```
/faction reload
```

| | |
|---|---|
| **Uso** | Após editar o JSON manualmente no painel do servidor |
| **Efeito** | Rebuild dos mapas em memória (`chunkToFactionMap`, `playerToFactionMap`, etc.) |

---

## Resumo rápido por role

| Comando | Jogador | Membro | Oficial | OP (Admin) | OP (Modo Jogador) |
|---------|:-------:|:------:|:-------:|:----------:|:-----------------:|
| `accept` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `create` | ✓† | | | ✓ | ✓ |
| `cancreate` | | | | ✓‡ | ✓‡ |
| `set flag` | | | ✓ | ✓* | ✓* |
| `invite` | | | ✓ | ✓* | ✓* |
| `delete_force` | | | | ✓ | ✓ |
| `join_forced` | | | | ✓ | ✓ |
| `leave_force` | | | | ✓ | ✓ |
| `set_leader` | | | | ✓ | ✓ |
| `admin_claim` / `admin_unclaim` | | | | ✓ | ✓ |
| `admin_setchunk` / `admin_removechunk` | | | | ✓ | ✓ |
| `list` / `info` / `reload` | | | | ✓ | ✓ |
| `playmode` | | | | ✓ | ✓ |
| `debug clicks` | | | | ✓ | ✓ |

\*OP precisa ser Oficial da facção **e** estar em Modo Jogador para sentir as mesmas restrições de território; em Modo Admin pode ignorar proteções.  
†Exige flag NBT, nó LuckPerms `faction_control.create_faction`, ou OP 2. Nível 1 só deixa o comando visível.  
‡Nível **1** (LuckPerms / quest / console), não precisa ser OP 2.

---

## Regras de território (referência)

| Zona | GameMode (Modo Jogador) | Quebra | Colocação / interação (inimigo) |
|------|---------------------------|--------|----------------------------------|
| Chunk da própria facção | Survival | Permitido (incluindo containers) | Permitido |
| Zona livre (sem dono) | Adventure | Permitido (vanilla Adventure restringe quebra) | Permitido |
| Território inimigo ACTIVE | Survival | Blocos **sem** inventário: permitido; **containers**: indestrutíveis | Bloqueado + actionbar (exceto tiro TaCZ) |
| Território RAIDED | Survival | Blocos sem inventário: permitido; containers: indestrutíveis para inimigos | Permitido (invasão) |
| Safezone admin | Adventure | Bloqueada (vanilla Adventure); baús liberados para interação | Liberada; PvP e TaCZ bloqueados se **atacante ou vítima** estiver no chunk admin; upgrade de facção **bloqueado** |

OP em **Modo Admin**: o mod não altera GameMode nem cancela eventos de território.

---

## Integrações opcionais

### Create (`create`)

Quando o mod Create está instalado:

- Contrapções seguem o **regimento do jogador que montou/ativou** o mecanismo.
- Efeitos no mundo (quebra/colocação por contrapção) só funcionam dentro dos **chunks claimados da facção de quem montou**.
- Fora do domínio da facção (wilderness, território inimigo, outras facções): **sem efetividade**.

### Timeless and Classics Zero (`tacz`)

Quando o mod TaCZ está instalado:

- Armas podem ser usadas em **todo o Overworld**, inclusive território inimigo protegido.
- **Exceção:** safezones admin (`admin_chunks`) — tiro bloqueado se o atirador **ou** o alvo estiver no chunk admin, com mensagem de zona segura.

---

Arquitetura e port 1.21.1: [ARCHITECTURE.md](ARCHITECTURE.md), [architecture.yml](architecture/architecture.yml), [conversão 1.21.1](implementations/1.21.1/conversion-inventory.md).
