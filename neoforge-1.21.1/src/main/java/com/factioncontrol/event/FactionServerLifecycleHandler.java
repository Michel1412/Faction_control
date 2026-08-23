package com.factioncontrol.event;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.config.FactionConfigManager;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = FactionControlMod.MODID)
public final class FactionServerLifecycleHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CONFIG_POLL_INTERVAL_TICKS = 100;

    private FactionServerLifecycleHandler() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        FactionConfigManager.load(event.getServer());
        LOGGER.info(
                "Faction Control loaded {} faction(s) from {}",
                FactionConfigManager.factionCount(),
                FactionConfigManager.getConfigPath()
        );
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        FactionManager.markServerDataReady();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        FactionManager.shutdown(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CONFIG_POLL_INTERVAL_TICKS != 0) {
            return;
        }
        FactionConfigManager.checkAndReloadIfModified(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FactionManager.get(player.server);
        ModNetwork.sendPlayerFactionSync(player);
    }
}
