package com.factioncontrol.compat.create;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.util.TerritoryProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Create contraptions only affect the world inside chunks claimed by the assembler's faction.
 */
@EventBusSubscriber(modid = FactionControlMod.MODID)
public final class CreateIntegration {
    private static final String CREATE_NAMESPACE = "create";
    private static boolean registered;

    private CreateIntegration() {
    }

    public static void register() {
        registered = true;
    }

    private static boolean isActive() {
        return registered && CreateReflection.contraptionEntityClass() != null;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!isActive() || event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().dimension() != Level.OVERWORLD) {
            return;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(
                event.getLevel().getBlockState(event.getPos()).getBlock()
        );
        if (blockId == null || !CREATE_NAMESPACE.equals(blockId.getNamespace())) {
            return;
        }

        ContraptionOwnerTracker.setPendingOwner(
                player.getUUID(),
                player.chunkPosition(),
                player.serverLevel().getGameTime()
        );
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!isActive() || event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        Entity entity = event.getEntity();
        if (!CreateReflection.isContraptionEntity(entity)) {
            return;
        }

        long tick = level.getGameTime();
        UUID owner = ContraptionOwnerTracker.consumePendingOwner(entity.chunkPosition(), tick);
        if (owner != null) {
            ContraptionOwnerTracker.bindContraption(entity.getId(), owner);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!isActive()) {
            return;
        }
        if (CreateReflection.isContraptionEntity(event.getEntity())) {
            ContraptionOwnerTracker.unbindContraption(event.getEntity().getId());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!isActive() || event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!FactionManager.isServerDataReady()) {
            return;
        }

        UUID owner = resolveContraptionOwner(event.getPlayer(), level, event.getPos());
        if (owner == null) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(event.getPos());
        if (!TerritoryProtectionHelper.isPlayerFactionChunk(owner, chunkPos)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!isActive() || event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!FactionManager.isServerDataReady()) {
            return;
        }

        Entity placer = event.getEntity();
        UUID owner = null;
        if (placer instanceof ServerPlayer player && !CreateReflection.isCreateFakePlayer(player)) {
            return;
        }
        if (CreateReflection.isContraptionEntity(placer)) {
            owner = ContraptionOwnerTracker.getOwnerForContraption(placer.getId());
        }
        if (owner == null) {
            owner = ContraptionOwnerTracker.resolveOwnerAt(level, new ChunkPos(event.getPos()));
        }
        if (owner == null && CreateReflection.isCreateFakePlayer(placer)) {
            owner = ContraptionOwnerTracker.resolveOwnerAt(level, new ChunkPos(event.getPos()));
        }
        if (owner == null) {
            return;
        }

        if (!TerritoryProtectionHelper.isPlayerFactionChunk(owner, new ChunkPos(event.getPos()))) {
            event.setCanceled(true);
        }
    }

    @Nullable
    private static UUID resolveContraptionOwner(
            @Nullable net.minecraft.world.entity.player.Player player,
            ServerLevel level,
            BlockPos pos
    ) {
        if (player instanceof ServerPlayer serverPlayer && !CreateReflection.isCreateFakePlayer(serverPlayer)) {
            return null;
        }
        if (player != null && CreateReflection.isCreateFakePlayer(player)) {
            UUID atPos = ContraptionOwnerTracker.resolveOwnerAt(level, new ChunkPos(pos));
            if (atPos != null) {
                return atPos;
            }
        }
        return ContraptionOwnerTracker.resolveOwnerAt(level, new ChunkPos(pos));
    }
}
