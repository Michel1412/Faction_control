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

    public static MutableComponent factionMessage(FactionObject faction, String message) {
        return factionPrefix(faction)
                .append(Component.literal(message)
                        .withStyle(style -> style.withColor(TextColor.fromRgb(faction.getColor()))));
    }

    public static void sendSuccess(CommandSourceStack source, FactionObject faction, String message) {
        source.sendSuccess(() -> factionMessage(faction, message), false);
    }

    public static void sendSuccess(ServerPlayer player, FactionObject faction, String message) {
        player.sendSystemMessage(factionMessage(faction, message));
    }

    public static void sendError(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    public static void sendError(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    public static void sendAdminZoneActionBar(ServerPlayer player) {
        player.displayClientMessage(
                Component.literal("Zona de Administradores")
                        .withStyle(ChatFormatting.BLUE),
                true
        );
    }

    public static void sendErrorActionBar(ServerPlayer player, String message) {
        player.displayClientMessage(
                Component.literal(message).withStyle(ChatFormatting.RED),
                true
        );
    }

    public static void sendSuccessActionBar(ServerPlayer player, String message) {
        player.displayClientMessage(
                Component.literal(message).withStyle(ChatFormatting.GREEN),
                true
        );
    }

    public static void sendSafezonePvpDeniedActionBar(ServerPlayer player) {
        player.displayClientMessage(
                Component.literal("O PvP esta desativado nesta Zona Segura!")
                        .withStyle(ChatFormatting.RED),
                true
        );
    }

    private static final int PROTECTED_TERRITORY_MESSAGE_COOLDOWN_TICKS = 40;
    private static final java.util.Map<java.util.UUID, Long> LAST_PROTECTED_MESSAGE_TICK = new java.util.concurrent.ConcurrentHashMap<>();

    public static void sendInviteMessage(ServerPlayer target, FactionObject faction, String inviterName) {
        MutableComponent acceptButton = Component.literal("[ACEITAR]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/faction accept"))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Clique para entrar na faccao")
                        )));

        target.sendSystemMessage(
                factionPrefix(faction)
                        .append(Component.literal("Voce foi convidado por ")
                                .withStyle(style -> style.withColor(TextColor.fromRgb(faction.getColor()))))
                        .append(Component.literal(inviterName).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(". ").withStyle(style -> style.withColor(TextColor.fromRgb(faction.getColor()))))
                        .append(acceptButton)
        );
    }

    public static void sendProtectedTerritoryActionBar(ServerPlayer player, String ownerName) {
        sendThrottledActionBar(
                player,
                LAST_PROTECTED_MESSAGE_TICK,
                Component.literal("Territorio protegido por " + ownerName)
                        .withStyle(ChatFormatting.RED)
        );
    }

    public static void sendProtectedContainerActionBar(ServerPlayer player, String ownerName) {
        sendThrottledActionBar(
                player,
                LAST_PROTECTED_MESSAGE_TICK,
                Component.literal("Containers sao indestrutiveis no territorio de " + ownerName)
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
