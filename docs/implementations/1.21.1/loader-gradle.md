# 1.21.1 — Loader and Gradle

Do **not** bump the root Forge project. Add a sibling module (ADR-0001).

## Target properties (from NeoForge 1.21.1 MDK; bump patch as needed)

```properties
minecraft_version=1.21.1
minecraft_version_range=[1.21.1]
neo_version=21.1.248
loader_version_range=[1,)
# Java 21 toolchain in the module build.gradle
```

Plugin: `id 'net.neoforged.moddev' version '2.0.126'` (or the version pinned by the current MDK).

`settings.gradle` pluginManagement must add `https://maven.neoforged.net/releases`.

## Metadata

`META-INF/neoforge.mods.toml`:

- `modLoader="javafml"`
- dependency `modId="neoforge"` (not `forge`)
- `modId="minecraft"` versionRange `[1.21.1]`
- optional `create` / `tacz` still `mandatory=false` after VERIFY

## Bootstrap sketch

```java
@Mod(FactionControlMod.MODID)
public class FactionControlMod {
    public FactionControlMod(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
        // Data components register here too
        modBus.addListener(this::commonSetup);
        // Game bus subscribers via @EventBusSubscriber(bus = Bus.GAME)
    }
}
```

NeoForge uses `Bus.GAME` where Forge 1.20.1 used `Bus.FORGE`.

`FMLPaths.CONFIGDIR` package: `net.neoforged.fml.loading.FMLPaths`.
