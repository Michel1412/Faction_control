package com.factioncontrol.event;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.util.FactionDebugSettings;
import com.factioncontrol.util.PlayerPlayModeHelper;
import com.factioncontrol.util.TerritoryProtectionHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Syncs player game mode to territory context: SURVIVAL when allowed, ADVENTURE when protected.
 * Ignores Creative and Spectator players.
 */
@Mod.EventBusSubscriber(modid = FactionControlMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GameModeSyncHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<UUID> MOD_CONTROLLED_ADVENTURE = ConcurrentHashMap.newKeySet();

    private GameModeSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!FactionManager.isServerDataReady()) {
            return;
        }

        syncForChunk(player, player.chunkPosition(), "PlayerTick");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        syncForChunk(player, new ChunkPos(event.getPos()), "LeftClickBlock");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        syncForChunk(player, new ChunkPos(event.getPos()), "RightClickBlock");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide() || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        syncForChunk(player, new ChunkPos(event.getPos()), "BreakEvent");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        syncForChunk(player, new ChunkPos(event.getPos()), "EntityPlaceEvent");
    }

    public static void syncForChunk(ServerPlayer player, ChunkPos chunkPos, String source) {
        if (!FactionManager.isServerDataReady()) {
            return;
        }

        if (!PlayerPlayModeHelper.isSubjectToTerritoryRules(player)) {
            removeModControl(player);
            return;
        }

        GameType currentMode = player.gameMode.getGameModeForPlayer();
        if (currentMode == GameType.CREATIVE || currentMode == GameType.SPECTATOR) {
            removeModControl(player);
            return;
        }

        if (player.serverLevel().dimension() != Level.OVERWORLD) {
            applyTargetGameMode(player, GameType.SURVIVAL, source);
            return;
        }

        GameType targetMode = TerritoryProtectionHelper.resolveExpectedGameMode(player, chunkPos);
        applyTargetGameMode(player, targetMode, source);
    }

    private static void applyTargetGameMode(ServerPlayer player, GameType targetMode, String source) {
        GameType currentMode = player.gameMode.getGameModeForPlayer();
        if (currentMode == GameType.CREATIVE || currentMode == GameType.SPECTATOR) {
            return;
        }

        if (currentMode == targetMode) {
            if (targetMode == GameType.ADVENTURE) {
                markModControl(player);
            } else {
                removeModControl(player);
            }
            return;
        }

        player.setGameMode(targetMode);
        if (targetMode == GameType.ADVENTURE) {
            markModControl(player);
        } else {
            removeModControl(player);
        }

        if (FactionDebugSettings.isClickLoggingEnabled()) {
            LOGGER.info(
                    "[FACTION GAMEMODE] " + source
                            + " | Player: " + player.getName().getString()
                            + " | " + currentMode.getName().toUpperCase()
                            + " -> " + targetMode.getName().toUpperCase()
            );
        }
    }

    private static void markModControl(ServerPlayer player) {
        MOD_CONTROLLED_ADVENTURE.add(player.getUUID());
    }

    private static void removeModControl(ServerPlayer player) {
        MOD_CONTROLLED_ADVENTURE.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        restoreSurvivalIfModControlled(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        restoreSurvivalIfModControlled(player);
    }

    private static void restoreSurvivalIfModControlled(ServerPlayer player) {
        GameType currentMode = player.gameMode.getGameModeForPlayer();
        if (currentMode != GameType.CREATIVE && currentMode != GameType.SPECTATOR && currentMode != GameType.SURVIVAL) {
            player.setGameMode(GameType.SURVIVAL);
        }
        removeModControl(player);
    }
}
