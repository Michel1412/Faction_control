package com.factioncontrol.event;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.util.FactionChat;
import com.factioncontrol.util.SafezoneHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FactionControlMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AdminSafezoneHandler {
    private AdminSafezoneHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMobFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }

        ServerLevel level = SafezoneHelper.asServerLevel(event.getEntity().level());
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return;
        }

        ChunkPos chunkPos = event.getEntity().chunkPosition();
        if (SafezoneHelper.isAdminChunk(level, chunkPos)) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewTarget();
        if (!(newTarget instanceof Player player)) {
            return;
        }

        ServerLevel level = SafezoneHelper.asServerLevel(player.level());
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return;
        }

        if (SafezoneHelper.isAdminChunk(level, player.chunkPosition())) {
            event.setNewTarget(null);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        if (attacker.getUUID().equals(victim.getUUID())) {
            return;
        }

        ServerLevel level = SafezoneHelper.asServerLevel(victim.level());
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return;
        }

        if (!SafezoneHelper.isAdminChunk(level, victim.chunkPosition())
                && !SafezoneHelper.isAdminChunk(level, attacker.chunkPosition())) {
            return;
        }

        event.setCanceled(true);
        if (attacker instanceof ServerPlayer serverAttacker) {
            FactionChat.sendSafezonePvpDeniedActionBar(serverAttacker);
        }
    }
}
