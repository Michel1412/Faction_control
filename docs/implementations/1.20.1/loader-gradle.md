# 1.20.1 — Loader and Gradle

## Files

| File | Role |
|------|------|
| `gradle.properties` | `minecraft_version=1.20.1`, `forge_version=47.4.10`, `mapping_channel=official`, `mod_version` |
| `build.gradle` | `net.minecraftforge.gradle`, `java.toolchain` 17, `minecraft { }` runs, `reobfJar` |
| `settings.gradle` | Forge maven + Foojay toolchain resolver |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle **8.8** |
| `src/main/resources/META-INF/mods.toml` | `modLoader="javafml"`, dep on `forge` + `minecraft` + optional `create` / `tacz` |

## Bootstrap

`FactionControlMod` constructor takes `FMLJavaModLoadingContext`:

- `context.getModEventBus()` → register `ModBlocks`, `ModItems`, `ModCreativeTabs`
- `FMLCommonSetupEvent.enqueueWork` → `ModNetwork.register()` + `ModCompatibility.init()`
- `MinecraftForge.EVENT_BUS.register(this)` (handlers also use `@Mod.EventBusSubscriber` on `Bus.FORGE`)

## Resource expansion

`processResources` expands `${minecraft_version}`, `${forge_version_range}`, `${mod_id}`, etc. into `mods.toml` and `pack.mcmeta`.

## Do not change for 1.21.1 in this tree

Leave `minecraft_version_range=[1.20.1,1.21)` as-is on the 1.20.1 artifact. The 1.21.1 build is a **separate module** (ADR-0001 / ADR-0002).
