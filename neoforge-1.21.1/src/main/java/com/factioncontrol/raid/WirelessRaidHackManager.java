package com.factioncontrol.raid;

import com.factioncontrol.faction.FactionObject;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.faction.FlagState;
import com.factioncontrol.registry.ModItems;
import com.factioncontrol.util.FactionChat;
import com.factioncontrol.util.TerritoryProtectionHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WirelessRaidHackManager {
    public static final int HACK_DURATION_TICKS = 20 * 60;
    /** Short refresh window; reapplied every tick only on the hacker. */
    private static final int HACKER_SLOWNESS_DURATION_TICKS = 40;
    private static final int HACKER_SLOWNESS_AMPLIFIER = 9;
    private static final Component LEADER_ALERT = Component.translatable("faction_control.raid.leader_alert");

    private static final Map<UUID, HackSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    private WirelessRaidHackManager() {
    }

    public static void tick(ServerPlayer player) {
        if (!shouldTick(player)) {
            return;
        }

        if (!player.isUsingItem() || !player.getUseItem().is(ModItems.RAID_CONTROLLER.get())) {
            cancelSession(player.getUUID(), CancelReason.RELEASED);
            return;
        }

        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD) {
            cancelSession(player.getUUID(), CancelReason.INVALID);
            player.stopUsingItem();
            return;
        }

        HackSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null) {
            session = tryStartSession(player, level);
            if (session == null) {
                player.stopUsingItem();
                return;
            }
        }

        if (!TerritoryProtectionHelper.isPlayerInsideFactionTerritory(
                player, session.targetFactionId())) {
            cancelSession(player.getUUID(), CancelReason.LEFT_TERRITORY);
            player.stopUsingItem();
            return;
        }

        if (!player.isAlive()) {
            cancelSession(player.getUUID(), CancelReason.DEATH);
            return;
        }

        session.incrementTicks();
        // Debuff applies only to the player holding the Raid Controller — never to nearby players or the target faction.
        refreshHackerDebuff(player);
        sendProgressActionBar(player, session);

        int percent = Math.min(100, (int) ((session.ticksElapsed() * 100L) / HACK_DURATION_TICKS));
        checkHackMilestones(level, session, percent);

        if (session.ticksElapsed() >= HACK_DURATION_TICKS) {
            completeHack(player, level, session);
        }
    }

    public static boolean shouldTick(ServerPlayer player) {
        if (!ACTIVE_SESSIONS.isEmpty() && ACTIVE_SESSIONS.containsKey(player.getUUID())) {
            return true;
        }
        return player.isUsingItem() && player.getUseItem().is(ModItems.RAID_CONTROLLER.get());
    }

    public static void cancelSession(UUID playerId, CancelReason reason) {
        HackSession session = ACTIVE_SESSIONS.remove(playerId);
        if (session == null) {
            return;
        }
        removeHackerDebuff(session.server(), playerId);
    }

    @Nullable
    private static HackSession tryStartSession(ServerPlayer player, ServerLevel level) {
        FactionManager manager = FactionManager.get(level);
        FactionObject attackerFaction = manager.getFactionOfMember(player.getUUID());
        if (attackerFaction == null || !attackerFaction.isLeader(player.getUUID())) {
            FactionChat.sendErrorActionBar(player, Component.translatable("faction_control.raid.official_only"));
            return null;
        }

        FactionObject targetFaction = TerritoryProtectionHelper.getEnemyFactionAtChunk(
                player, player.chunkPosition());
        if (targetFaction == null) {
            return null;
        }

        HackSession session = new HackSession(
                player.server,
                player.getUUID(),
                targetFaction.getFactionId(),
                targetFaction.getOfficialUuid()
        );
        ACTIVE_SESSIONS.put(player.getUUID(), session);
        notifyLeaderInvasion(session);
        return session;
    }

    private static void completeHack(ServerPlayer player, ServerLevel level, HackSession session) {
        FactionManager manager = FactionManager.get(level);
        manager.setFactionFlagState(session.targetFactionId(), FlagState.RAIDED);

        FactionObject targetFaction = manager.getFaction(session.targetFactionId());
        if (targetFaction != null) {
            broadcastToFaction(level, targetFaction,
                    Component.translatable("faction_control.raid.hacked"));
        }

        player.displayClientMessage(
                Component.translatable("faction_control.raid.complete")
                        .withStyle(ChatFormatting.GREEN),
                true
        );

        ItemStack heldItem = player.getUseItem();
        if (heldItem.is(ModItems.RAID_CONTROLLER.get())) {
            heldItem.shrink(1);
        }

        player.stopUsingItem();
        cancelSession(player.getUUID(), CancelReason.COMPLETED);
    }

    private static void sendProgressActionBar(ServerPlayer player, HackSession session) {
        int percent = Math.min(100, (int) ((session.ticksElapsed() * 100L) / HACK_DURATION_TICKS));
        player.displayClientMessage(
                Component.translatable("faction_control.raid.progress", percent)
                        .withStyle(ChatFormatting.GOLD),
                true
        );
    }

    private static void checkHackMilestones(ServerLevel level, HackSession session, int percent) {
        FactionManager manager = FactionManager.get(level);
        FactionObject targetFaction = manager.getFaction(session.targetFactionId());
        if (targetFaction == null) {
            return;
        }

        if (percent >= 30 && !session.milestone30Sent()) {
            session.setMilestone30Sent(true);
            broadcastHackAlertChat(level, targetFaction,
                    Component.translatable("faction_control.raid.alert", 30));
        }
        if (percent >= 50 && !session.milestone50Sent()) {
            session.setMilestone50Sent(true);
            broadcastHackAlertChat(level, targetFaction,
                    Component.translatable("faction_control.raid.alert", 50));
        }
        if (percent >= 80 && !session.milestone80Sent()) {
            session.setMilestone80Sent(true);
            broadcastHackAlertChat(level, targetFaction,
                    Component.translatable("faction_control.raid.alert", 80));
        }
        if (percent >= 90 && !session.milestone90Sent()) {
            session.setMilestone90Sent(true);
            broadcastHackAlertScreen(level, targetFaction,
                    Component.translatable("faction_control.raid.danger"));
        }
    }

    private static void broadcastHackAlertChat(ServerLevel level, FactionObject faction, Component message) {
        for (UUID memberId : faction.getMembers()) {
            ServerPlayer member = level.getServer().getPlayerList().getPlayer(memberId);
            if (member != null) {
                member.sendSystemMessage(
                        FactionChat.factionPrefix(faction)
                                .append(message.copy()
                                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                );
            }
        }
    }

    private static void broadcastHackAlertScreen(ServerLevel level, FactionObject faction, Component message) {
        for (UUID memberId : faction.getMembers()) {
            ServerPlayer member = level.getServer().getPlayerList().getPlayer(memberId);
            if (member != null) {
                member.displayClientMessage(
                        message.copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        true
                );
            }
        }
    }

    private static void notifyLeaderInvasion(HackSession session) {
        if (session.leaderId() == null || session.leaderAlertSent()) {
            return;
        }

        ServerPlayer leader = session.server().getPlayerList().getPlayer(session.leaderId());
        if (leader == null) {
            return;
        }

        session.setLeaderAlertSent(true);
        leader.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0F, 0.85F);
        FactionChat.sendErrorActionBar(leader, LEADER_ALERT);
        leader.sendSystemMessage(LEADER_ALERT.copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    /**
     * Applies Slowness only to {@code hacker} — the player actively using the Raid Controller.
     * Does not affect nearby entities, faction members, or the enemy Official.
     */
    private static void refreshHackerDebuff(ServerPlayer hacker) {
        hacker.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                HACKER_SLOWNESS_DURATION_TICKS,
                HACKER_SLOWNESS_AMPLIFIER,
                false,
                true,
                true
        ));
    }

    private static void removeHackerDebuff(net.minecraft.server.MinecraftServer server, UUID hackerId) {
        ServerPlayer hacker = server.getPlayerList().getPlayer(hackerId);
        if (hacker != null && hacker.isAlive()) {
            hacker.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    private static void broadcastToFaction(ServerLevel level, FactionObject faction, Component message) {
        for (UUID memberId : faction.getMembers()) {
            ServerPlayer member = level.getServer().getPlayerList().getPlayer(memberId);
            if (member != null) {
                FactionChat.sendSuccess(member, faction, message);
            }
        }
    }

    public enum CancelReason {
        RELEASED,
        LEFT_TERRITORY,
        DEATH,
        INVALID,
        COMPLETED
    }

    public static final class HackSession {
        private final net.minecraft.server.MinecraftServer server;
        private final UUID hackerId;
        private final UUID targetFactionId;
        @Nullable
        private final UUID leaderId;
        private int ticksElapsed;
        private boolean leaderAlertSent;
        private boolean milestone30Sent;
        private boolean milestone50Sent;
        private boolean milestone80Sent;
        private boolean milestone90Sent;

        HackSession(
                net.minecraft.server.MinecraftServer server,
                UUID hackerId,
                UUID targetFactionId,
                @Nullable UUID leaderId
        ) {
            this.server = server;
            this.hackerId = hackerId;
            this.targetFactionId = targetFactionId;
            this.leaderId = leaderId;
        }

        net.minecraft.server.MinecraftServer server() {
            return server;
        }

        UUID hackerId() {
            return hackerId;
        }

        UUID targetFactionId() {
            return targetFactionId;
        }

        @Nullable
        UUID leaderId() {
            return leaderId;
        }

        int ticksElapsed() {
            return ticksElapsed;
        }

        void incrementTicks() {
            ticksElapsed++;
        }

        boolean leaderAlertSent() {
            return leaderAlertSent;
        }

        void setLeaderAlertSent(boolean leaderAlertSent) {
            this.leaderAlertSent = leaderAlertSent;
        }

        boolean milestone30Sent() {
            return milestone30Sent;
        }

        void setMilestone30Sent(boolean milestone30Sent) {
            this.milestone30Sent = milestone30Sent;
        }

        boolean milestone50Sent() {
            return milestone50Sent;
        }

        void setMilestone50Sent(boolean milestone50Sent) {
            this.milestone50Sent = milestone50Sent;
        }

        boolean milestone80Sent() {
            return milestone80Sent;
        }

        void setMilestone80Sent(boolean milestone80Sent) {
            this.milestone80Sent = milestone80Sent;
        }

        boolean milestone90Sent() {
            return milestone90Sent;
        }

        void setMilestone90Sent(boolean milestone90Sent) {
            this.milestone90Sent = milestone90Sent;
        }
    }
}
