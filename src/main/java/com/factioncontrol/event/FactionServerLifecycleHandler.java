package com.factioncontrol.event;



import com.factioncontrol.FactionControlMod;

import com.factioncontrol.config.FactionConfigManager;

import com.factioncontrol.faction.FactionManager;

import com.factioncontrol.network.ModNetwork;

import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;

import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraftforge.event.server.ServerStartedEvent;

import net.minecraftforge.event.server.ServerStartingEvent;

import net.minecraftforge.event.server.ServerStoppingEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;



@Mod.EventBusSubscriber(modid = FactionControlMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)

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
                FactionConfigManager.factionsMap.size(),

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

    public static void onServerTick(TickEvent.ServerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {

            return;

        }

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

