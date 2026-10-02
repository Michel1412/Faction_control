package com.factioncontrol.util;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import com.factioncontrol.faction.FactionObject;

public final class FactionChat {
    private FactionChat() {
    }

    public static MutableComponent factionPrefix(FactionObject faction) {
        return Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(faction.getName())
                        .withStyle(style -> style.withColor(TextColor.fromRgb(faction.getColor()))))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static MutableComponent factionMessage(FactionObject faction, Component message) {
        return factionPrefix(faction)
                .append(message.copy().withStyle(style -> style.withColor(TextColor.fromRgb(faction.getColor()))));
    }

    public static void sendSuccess(CommandSourceStack source, FactionObject faction, Component message) {
        source.sendSuccess(() -> factionMessage(faction, message), false);
    }

    public static void sendSuccess(ServerPlayer player, FactionObject faction, Component message) {
        player.sendSystemMessage(factionMessage(faction, message));
    }

    public static void sendError(CommandSourceStack source, Component message) {
        source.sendFailure(message.copy().withStyle(ChatFormatting.RED));
    }

    public static void sendError(ServerPlayer player, Component message) {
        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.RED));
    }

    public static void sendAdminZoneActionBar(ServerPlayer player) {
        player.displayClientMessage(
                Component.translatable("faction_control.chat.admin_zone")
                        .withStyle(ChatFormatting.BLUE),
                true
        );
    }

    public static void sendErrorActionBar(ServerPlayer player, Component message) {
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);
    }

    public static void sendSuccessActionBar(ServerPlayer player, Component message) {
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.GREEN), true);
    }

    public static void sendSafezonePvpDeniedActionBar(ServerPlayer player) {
        player.displayClientMessage(
                Component.translatable("faction_control.chat.safezone_pvp")
                        .withStyle(ChatFormatting.RED),
                true
        );
    }

    private static final int PROTECTED_TERRITORY_MESSAGE_COOLDOWN_TICKS = 40;
    private static final java.util.Map<java.util.UUID, Long> LAST_PROTECTED_MESSAGE_TICK = new java.util.concurrent.ConcurrentHashMap<>();

    public static void sendInviteMessage(ServerPlayer target, FactionObject faction, String inviterName) {
        MutableComponent acceptButton = Component.translatable("faction_control.chat.accept")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/faction accept"))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("faction_control.chat.accept_hover")
                        )));

        target.sendSystemMessage(
                factionPrefix(faction)
                        .append(Component.translatable("faction_control.chat.invited_by",
                                        Component.literal(inviterName).withStyle(ChatFormatting.YELLOW))
                                .withStyle(style -> style.withColor(TextColor.fromRgb(faction.getColor()))))
                        .append(acceptButton)
        );
    }

    public static void sendProtectedTerritoryActionBar(ServerPlayer player, String ownerName) {
        sendThrottledActionBar(
                player,
                LAST_PROTECTED_MESSAGE_TICK,
                Component.translatable("faction_control.chat.protected_by", ownerName)
                        .withStyle(ChatFormatting.RED)
        );
    }

    public static void sendProtectedContainerActionBar(ServerPlayer player, Component ownerName) {
        sendThrottledActionBar(
                player,
                LAST_PROTECTED_MESSAGE_TICK,
                Component.translatable("faction_control.chat.containers_protected", ownerName)
                        .withStyle(ChatFormatting.RED)
        );
    }

    private static void sendThrottledActionBar(
            ServerPlayer player,
            java.util.Map<java.util.UUID, Long> lastTickMap,
            Component message
    ) {
        long currentTick = player.serverLevel().getGameTime();
        Long lastTick = lastTickMap.get(player.getUUID());
        if (lastTick != null && currentTick - lastTick < PROTECTED_TERRITORY_MESSAGE_COOLDOWN_TICKS) {
            return;
        }
        lastTickMap.put(player.getUUID(), currentTick);
        player.displayClientMessage(message, true);
    }
}
