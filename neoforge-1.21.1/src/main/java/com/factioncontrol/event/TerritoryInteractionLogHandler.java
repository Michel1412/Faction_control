package com.factioncontrol.event;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.config.FactionConfigManager;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.util.FactionDebugSettings;
import com.factioncontrol.util.TerritoryProtectionHelper;
import com.factioncontrol.util.TerritoryProtectionHelper.FactionRole;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Debug logging for block interactions: current vs expected game mode and permission outcome.
 */
@EventBusSubscriber(modid = FactionControlMod.MODID)
public final class TerritoryInteractionLogHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private TerritoryInteractionLogHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        logInteraction(player, "LeftClickBlock", event.getPos(), event.isCanceled());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        logInteraction(player, "RightClickBlock", event.getPos(), event.isCanceled());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide() || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        logInteraction(player, "BreakEvent", event.getPos(), event.isCanceled());
    }

    private static void logInteraction(ServerPlayer player, String eventName, BlockPos pos, boolean canceled) {
        if (!FactionDebugSettings.isClickLoggingEnabled()) {
            return;
        }
        if (player.level().isClientSide() || !FactionManager.isServerDataReady()) {
            return;
        }
        if (player.serverLevel().dimension() != Level.OVERWORLD) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(pos);
        GameType currentMode = player.gameMode.getGameModeForPlayer();
        GameType expectedMode = TerritoryProtectionHelper.resolveExpectedGameMode(player, chunkPos);
        boolean permitted = TerritoryProtectionHelper.hasPermission(player, chunkPos);
        boolean breakPermitted = TerritoryProtectionHelper.canBreakBlock(
                player,
                chunkPos,
                player.serverLevel(),
                pos,
                player.serverLevel().getBlockState(pos)
        );

        UUID ownerId = FactionConfigManager.getChunkOwner(chunkPos);
        FactionRole role = TerritoryProtectionHelper.getRole(player.getUUID(), ownerId);
        UUID playerFactionId = FactionConfigManager.getPlayerFactionId(player.getUUID());

        LOGGER.info(
                "[FACTION CLICK] Evento: " + eventName
                        + " | Player: " + player.getName().getString()
                        + " | GameMode Atual: " + currentMode.getName().toUpperCase()
                        + " | GameMode Esperado: " + expectedMode.getName().toUpperCase()
                        + " | Bloco Pos: " + pos
                        + " | Chunk: " + chunkPos
                        + " | Dono do Chunk: " + TerritoryProtectionHelper.describeChunkOwner(chunkPos)
                        + " | Papel no Chunk: " + role
                        + " | Faction do Player: " + playerFactionId
                        + " | Permitido: " + permitted
                        + " | Quebra Permitida: " + breakPermitted
                        + " | Cancelado: " + canceled
        );
    }
}
