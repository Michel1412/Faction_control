package com.factioncontrol.event;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.block.FlagBlock;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.util.FactionChat;
import com.factioncontrol.util.ModItemHelper;
import com.factioncontrol.util.TerritoryProtectionHelper;
import com.factioncontrol.util.TerritoryProtectionHelper.ProtectionDenial;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FactionControlMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TerritoryProtectionHandler {
    private TerritoryProtectionHandler() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (event.getState().getBlock() instanceof FlagBlock) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!FactionManager.isServerDataReady()) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(event.getPos());
        if (TerritoryProtectionHelper.canBreakBlock(
                player,
                chunkPos,
                event.getLevel(),
                event.getPos(),
                event.getState()
        )) {
            return;
        }

        event.setCanceled(true);
        FactionChat.sendProtectedContainerActionBar(player, TerritoryProtectionHelper.describeChunkOwner(chunkPos));
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        denyIfProtected(player, event.getPos(), event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        denyIfProtected(player, event.getPos(), event);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // TaCZ guns use left click to fire; allow shooting even in protected enemy territory.
        if (ModItemHelper.isTaczItem(player.getMainHandItem()) || ModItemHelper.isTaczItem(player.getOffhandItem())) {
            return;
        }

        if (!FactionManager.isServerDataReady()) {
            return;
        }
        if (player.serverLevel().dimension() != Level.OVERWORLD) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(event.getPos());
        var blockState = event.getLevel().getBlockState(event.getPos());
        if (TerritoryProtectionHelper.canBreakBlock(
                player,
                chunkPos,
                event.getLevel(),
                event.getPos(),
                blockState
        )) {
            return;
        }

        event.setCanceled(true);
        FactionChat.sendProtectedContainerActionBar(player, TerritoryProtectionHelper.describeChunkOwner(chunkPos));
    }

    private static void denyIfProtected(ServerPlayer player, BlockPos pos, net.minecraftforge.eventbus.api.Event event) {
        if (!FactionManager.isServerDataReady()) {
            return;
        }
        if (player.serverLevel().dimension() != Level.OVERWORLD) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(pos);
        ProtectionDenial denial = TerritoryProtectionHelper.evaluate(player, chunkPos);
        if (denial == null) {
            return;
        }

        event.setCanceled(true);
        FactionChat.sendProtectedTerritoryActionBar(player, denial.ownerDisplayName());
    }
}
