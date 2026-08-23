# ADR-0010: Java 17 for 1.20.1, Java 21 for 1.21.1

- **Status:** Accepted
- **Date:** 2026-08-21
- **Applies to:** toolchains / CI

## Context

- Minecraft 1.20.1 / Forge 47 ships **Java 17**.
- Minecraft 1.21.1 / NeoForge 21.1 ships **Java 21**.

A single `JavaLanguageVersion.of(17)` at the repo root cannot compile both modules correctly. Using Java 21 to compile the 1.20.1 module with `--release 17` is possible but easy to get wrong (APIs).

## Decision

Each version module sets its own toolchain:

- `forge-1.20.1` → Java 17
- `neoforge-1.21.1` → Java 21

Gradle Foojay resolver (already in `settings.gradle`) downloads the missing JDK. CI must run both `:*:build` tasks, not only the root leftover Forge project.

## Consequences

- Developers need both JDKs locally (or let Gradle provision them).
- Do not bump the 1.20.1 module to Java 21 “for convenience”.
