# Faction Control — NeoForge 1.21.1

Standalone Gradle project (own wrapper). The Forge **1.20.1** tree at the repo root is unchanged.

| Property | Value |
|----------|-------|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.248 |
| Java | 21 (toolchain) |
| Gradle | 8.12.1 |

## Build

Requires a JDK 21 on the machine **or** let Gradle download one via Foojay.

```bat
cd neoforge-1.21.1
gradlew.bat build
```

Output JAR: `neoforge-1.21.1/build/libs/faction_control-1.21.1-1.4.1.jar`

Dev server:

```bat
gradlew.bat runServer
```

Do **not** run this module with the root `gradlew` (ForgeGradle 6 / Gradle 8.8).

API mapping: [docs/implementations/1.21.1/](../docs/implementations/1.21.1/).
