package com.factioncontrol.client;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.registry.ModBlocks;
import com.factioncontrol.registry.ModItems;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Item tint registration. Pixel-art textures are fully colored; handlers return {@link #NO_TINT}.
 */
@Mod.EventBusSubscriber(modid = FactionControlMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private static final int NO_TINT = 0xFFFFFF;
    private static final String DEBUG_LOG_PATH = "E:\\Arquivos_Mods\\FactionControl\\debug-ad4d18.log";

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.FLAG_BLOCK.get(),
                    RenderType.cutout()
            );
            logFlagResourceProbe();
        });
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? NO_TINT : -1,
                ModItems.FACTION_UPGRADE.get()
        );
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? NO_TINT : -1,
                ModItems.RAID_CONTROLLER.get()
        );
    }

    // #region agent log
    private static void logFlagResourceProbe() {
        boolean modelPresent = ClientModEvents.class.getClassLoader().getResource(
                "assets/faction_control/models/block/flag_block.json") != null;
        boolean texturePresent = ClientModEvents.class.getClassLoader().getResource(
                "assets/faction_control/textures/block/marcadordebasetextura.png") != null;
        String payload = "{\"sessionId\":\"ad4d18\",\"runId\":\"flag-model-debug\","
                + "\"hypothesisId\":\"H1-H5\",\"location\":\"ClientModEvents.java:onClientSetup\","
                + "\"message\":\"Flag model resource probe\","
                + "\"data\":{\"modelPresent\":" + modelPresent
                + ",\"texturePresent\":" + texturePresent + "},"
                + "\"timestamp\":" + System.currentTimeMillis() + "}";
        try {
            Files.writeString(
                    Path.of(DEBUG_LOG_PATH),
                    payload + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }
    // #endregion
}
