package com.factioncontrol.command;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.util.FactionPlayerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * Vanilla levels for LuckPerms groups, plus a NeoForge node LuckPerms can grant directly.
 *
 * <p>{@code TRUSTED} (1) is the helper tier — visible to LuckPerms without OP 2.
 * Staff ({@code ADMIN}, 2+) always bypass the create-faction gate.
 */
@EventBusSubscriber(modid = FactionControlMod.MODID)
public final class FactionPermissions {
    /** LuckPerms helper / quest command runner. Vanilla permission level 1. */
    public static final int TRUSTED = 1;
    /** Server operator. Vanilla permission level 2. */
    public static final int ADMIN = 2;

    /**
     * LuckPerms node: {@code faction_control.create_faction}.
     * Grant with {@code /lp user Steve permission set faction_control.create_faction true}.
     */
    public static final PermissionNode<Boolean> CREATE_FACTION = new PermissionNode<>(
            FactionControlMod.MODID,
            "create_faction",
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> false
    );

    private FactionPermissions() {
    }

    @SubscribeEvent
    public static void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(CREATE_FACTION);
    }

    public static boolean isAdmin(CommandSourceStack source) {
        return source.hasPermission(ADMIN);
    }

    public static boolean isTrusted(CommandSourceStack source) {
        return source.hasPermission(TRUSTED);
    }

    /**
     * Command tree visibility: level 1+ (LuckPerms) or an already-unlocked player.
     */
    public static boolean canSeeCreateCommand(CommandSourceStack source) {
        if (source.hasPermission(TRUSTED)) {
            return true;
        }
        ServerPlayer player = source.getPlayer();
        return player != null && canCreateFaction(player);
    }

    /**
     * Runtime gate. OP 2+ always pass. Everyone else needs the NBT flag or the LuckPerms node.
     */
    public static boolean canCreateFaction(ServerPlayer player) {
        if (player.hasPermissions(ADMIN)) {
            return true;
        }
        if (FactionPlayerData.getCanCreateFaction(player)) {
            return true;
        }
        return PermissionAPI.getPermission(player, CREATE_FACTION);
    }
}
