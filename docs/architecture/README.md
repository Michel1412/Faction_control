# Architecture docs

This folder is the **architecture configuration** for Faction Control: which Minecraft versions we support, which decisions are locked, and where to look when an API detail is version-specific.

| File | Role |
|------|------|
| [architecture.yml](architecture.yml) | Machine-readable index: versions, loaders, ADR list, doc paths |
| [adr/](adr/) | Architecture Decision Records (why we chose X) |
| [../ARCHITECTURE.md](../ARCHITECTURE.md) | Domain layers (version-agnostic) |
| [../implementations/1.20.1/](../implementations/1.20.1/) | How it is implemented on **Forge 1.20.1** |
| [../implementations/1.21.1/](../implementations/1.21.1/) | What must change for **NeoForge 1.21.1** |

## How to use this when changing code

1. Domain rule (who can break a chest, JSON shape, command meaning) → `docs/ARCHITECTURE.md` + `docs/UTILITIES.md`.
2. “Which loader API do we call?” → `docs/implementations/<mc-version>/`.
3. “Why is 1.21.1 on NeoForge / why two Gradle modules?” → `docs/architecture/adr/`.
4. Adding a supported version → update `architecture.yml` first, then add `docs/implementations/<version>/`.

Do not put Forge class names in the domain docs. Put them in the 1.20.1 implementation folder so a 1.21.1 change does not require rewriting the product rules.
