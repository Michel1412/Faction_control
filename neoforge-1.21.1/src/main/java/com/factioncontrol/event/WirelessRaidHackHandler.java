package com.factioncontrol.event;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.raid.WirelessRaidHackManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = FactionControlMod.MODID)
public final class WirelessRaidHackHandler {
    private WirelessRaidHackHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!WirelessRaidHackManager.shouldTick(player)) {
            return;
        }

        WirelessRaidHackManager.tick(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WirelessRaidHackManager.cancelSession(player.getUUID(), WirelessRaidHackManager.CancelReason.RELEASED);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WirelessRaidHackManager.cancelSession(player.getUUID(), WirelessRaidHackManager.CancelReason.DEATH);
        }
    }
}
