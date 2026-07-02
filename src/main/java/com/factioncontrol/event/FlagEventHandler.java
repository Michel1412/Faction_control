package com.factioncontrol.event;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.block.FlagBlock;
import com.factioncontrol.faction.FactionObject;
import com.factioncontrol.util.FactionChat;
import com.factioncontrol.util.FlagBreakPolicy;
import com.factioncontrol.util.FlagHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FactionControlMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FlagEventHandler {
    private FlagEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFlagBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getState().getBlock() instanceof FlagBlock)) {
            return;
        }

        ServerPlayer player = event.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        FactionObject faction = FlagHelper.resolveFactionAtFlag(level, event.getPos());

        if (!FlagBreakPolicy.canBreakFlag(level, event.getPos(), player)) {
            event.setCanceled(true);
            if (player != null) {
                FactionChat.sendError(player,
                        "A bandeira e indestrutivel. Invasores devem usar o Raid Controller por 60 segundos.");
            }
            return;
        }

        if (player != null && faction != null) {
            FactionChat.sendSuccess(player, faction, "Bandeira removida. Territorio da faccao liberado.");
        }
    }
}
