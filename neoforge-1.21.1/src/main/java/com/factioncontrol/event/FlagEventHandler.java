package com.factioncontrol.event;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.block.FlagBlock;
import com.factioncontrol.faction.FactionObject;
import com.factioncontrol.util.FactionChat;
import com.factioncontrol.util.FlagBreakPolicy;
import com.factioncontrol.util.FlagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = FactionControlMod.MODID)
public final class FlagEventHandler {
    private FlagEventHandler() {
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getState().getBlock() instanceof FlagBlock)) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        BlockPos pos = event.getPosition().orElse(null);
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level) || pos == null) {
            event.setNewSpeed(0.0F);
            return;
        }
        BlockPos anchor = FlagBlock.anchorPos(event.getState(), pos);
        if (!FlagBreakPolicy.canBreakFlag(level, anchor, serverPlayer)) {
            event.setNewSpeed(0.0F);
        }
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

        BlockPos anchor = FlagBlock.anchorPos(event.getState(), event.getPos());
        ServerPlayer player = event.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        FactionObject faction = FlagHelper.resolveFactionAtFlag(level, anchor);

        if (!FlagBreakPolicy.canBreakFlag(level, anchor, player)) {
            event.setCanceled(true);
            if (player != null) {
                FactionChat.sendError(player, Component.translatable("faction_control.flag.indestructible"));
            }
            return;
        }

        if (player != null && faction != null) {
            FactionChat.sendSuccess(player, faction, Component.translatable("faction_control.flag.removed"));
        }
    }
}
