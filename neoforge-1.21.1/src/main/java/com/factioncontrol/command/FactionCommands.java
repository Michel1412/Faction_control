package com.factioncontrol.command;

import com.factioncontrol.config.FactionConfigManager;
import com.factioncontrol.faction.FactionInviteManager;
import com.factioncontrol.faction.FactionObject;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.network.ModNetwork;
import com.factioncontrol.util.FactionChat;
import com.factioncontrol.util.FactionDebugSettings;
import com.factioncontrol.util.FactionSync;
import com.factioncontrol.util.FlagHelper;
import com.factioncontrol.util.FactionPlayerData;
import com.factioncontrol.util.PlayerPlayModeHelper;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FactionCommands {
    private static final Map<UUID, DeleteConfirmation> DELETE_CONFIRMATIONS = new HashMap<>();

    private static final SimpleCommandExceptionType NOT_A_PLAYER =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.not_a_player"));
    private static final SimpleCommandExceptionType NOT_IN_FACTION =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.not_in_faction"));
    private static final SimpleCommandExceptionType NOT_OFFICIAL =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.not_official"));
    private static final SimpleCommandExceptionType FACTION_NOT_FOUND =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.faction_not_found"));
    private static final SimpleCommandExceptionType FLAG_ALREADY_ACTIVE =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.flag_already_active"));
    private static final SimpleCommandExceptionType TARGET_NOT_IN_FACTION =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.target_not_in_faction"));
    private static final SimpleCommandExceptionType TARGET_HAS_NO_FACTION =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.target_has_no_faction"));
    private static final SimpleCommandExceptionType NOT_IN_OVERWORLD =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.not_in_overworld"));
    private static final SimpleCommandExceptionType ADMIN_CHUNK_ALREADY_CLAIMED =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.admin_chunk_already_claimed"));
    private static final SimpleCommandExceptionType NOT_ADMIN_CHUNK =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.not_admin_chunk"));
    private static final SimpleCommandExceptionType CHUNK_NOT_CLAIMED_BY_FACTION =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.chunk_not_claimed_by_faction"));
    private static final SimpleCommandExceptionType FACTION_NAME_TAKEN =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.faction_name_taken"));
    private static final SimpleCommandExceptionType INVALID_HEX_COLOR =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.invalid_hex_color"));
    private static final SimpleCommandExceptionType TARGET_ALREADY_IN_FACTION =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.target_already_in_faction"));
    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.player_not_found"));
    private static final SimpleCommandExceptionType NO_PENDING_INVITE =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.no_pending_invite"));
    private static final SimpleCommandExceptionType INVITE_EXPIRED =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.invite_expired"));
    private static final SimpleCommandExceptionType CANNOT_CREATE_FACTION =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.cannot_create_faction"));
    private static final SimpleCommandExceptionType OFFICIAL_CANNOT_LEAVE =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.official_cannot_leave"));
    private static final SimpleCommandExceptionType CANNOT_KICK_SELF =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.cannot_kick_self"));
    private static final SimpleCommandExceptionType NO_DELETE_CONFIRMATION =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.no_delete_confirmation"));
    private static final SimpleCommandExceptionType DELETE_CONFIRMATION_EXPIRED =
            new SimpleCommandExceptionType(Component.translatable("command.faction_control.delete_confirmation_expired"));

    private FactionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("faction")
                .then(Commands.literal("set")
                        .then(Commands.literal("flag")
                                .executes(context -> setFlag(context.getSource()))))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> invitePlayer(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")
                                ))))
                .then(Commands.literal("accept")
                        .executes(context -> acceptInvite(context.getSource())))
                .then(Commands.literal("leave")
                        .executes(context -> leaveFaction(context.getSource())))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> kickMember(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "player")
                                ))))
                .then(Commands.literal("members")
                        .executes(context -> listMembers(context.getSource())))
                .then(Commands.literal("delete")
                        .executes(context -> requestFactionDeletion(context.getSource())))
                .then(Commands.literal("confirm")
                        .executes(context -> confirmFactionDeletion(context.getSource())))
                .then(Commands.literal("create")
                        .requires(FactionPermissions::canSeeCreateCommand)
                        .then(Commands.argument("nome", StringArgumentType.word())
                                .then(Commands.argument("cor", StringArgumentType.string())
                                        .executes(context -> createFaction(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "nome"),
                                                StringArgumentType.getString(context, "cor")
                                        )))))
                .then(Commands.literal("cancreate")
                        .requires(FactionPermissions::isTrusted)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> showCanCreate(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")
                                ))
                                .then(Commands.literal("toggle")
                                        .executes(context -> toggleCanCreate(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player")
                                        )))
                                .then(Commands.argument("ativar", BoolArgumentType.bool())
                                        .executes(context -> setCanCreate(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                BoolArgumentType.getBool(context, "ativar")
                                        )))))
                .then(Commands.literal("delete_force")
                        .requires(FactionPermissions::isAdmin)
                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                .executes(context -> deleteForce(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "nome")
                                ))))
                .then(Commands.literal("join_forced")
                        .requires(FactionPermissions::isAdmin)
                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                .executes(context -> joinForced(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "nome")
                                ))))
                .then(Commands.literal("leave_force")
                        .requires(FactionPermissions::isAdmin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> leaveForce(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")
                                ))))
                .then(Commands.literal("set_leader")
                        .requires(FactionPermissions::isAdmin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(context -> setLeader(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "nome")
                                        )))))
                .then(Commands.literal("admin_claim")
                        .requires(FactionPermissions::isAdmin)
                        .executes(context -> adminClaim(context.getSource())))
                .then(Commands.literal("admin_unclaim")
                        .requires(FactionPermissions::isAdmin)
                        .executes(context -> adminUnclaim(context.getSource())))
                .then(Commands.literal("admin_setchunk")
                        .requires(FactionPermissions::isAdmin)
                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                .executes(context -> adminSetChunk(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "nome")
                                ))))
                .then(Commands.literal("admin_removechunk")
                        .requires(FactionPermissions::isAdmin)
                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                .executes(context -> adminRemoveChunk(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "nome")
                                ))))
                .then(Commands.literal("list")
                        .requires(FactionPermissions::isAdmin)
                        .executes(context -> listFactions(context.getSource())))
                .then(Commands.literal("info")
                        .requires(FactionPermissions::isAdmin)
                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                .executes(context -> factionInfo(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "nome")
                                ))))
                .then(Commands.literal("reload")
                        .requires(FactionPermissions::isAdmin)
                        .executes(context -> reloadConfig(context.getSource())))
                .then(Commands.literal("playmode")
                        .requires(FactionPermissions::isAdmin)
                        .executes(context -> showPlayMode(context.getSource()))
                        .then(Commands.literal("toggle")
                                .executes(context -> togglePlayMode(context.getSource())))
                        .then(Commands.argument("ativar", BoolArgumentType.bool())
                                .executes(context -> setPlayMode(
                                        context.getSource(),
                                        BoolArgumentType.getBool(context, "ativar")
                                ))))
                .then(Commands.literal("debug")
                        .requires(FactionPermissions::isAdmin)
                        .then(Commands.literal("clicks")
                                .executes(context -> showClickLogging(context.getSource()))
                                .then(Commands.literal("toggle")
                                        .executes(context -> toggleClickLogging(context.getSource())))
                                .then(Commands.argument("ativar", BoolArgumentType.bool())
                                        .executes(context -> setClickLogging(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "ativar")
                                        ))))));
    }

    private static int invitePlayer(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        FactionManager manager = FactionManager.get(source.getServer());
        FactionObject faction = manager.getFactionOfMember(player.getUUID());

        if (faction == null) {
            throw NOT_IN_FACTION.create();
        }
        if (!faction.isLeader(player.getUUID())) {
            throw NOT_OFFICIAL.create();
        }
        if (player.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.translatable("command.faction_control.cannot_invite_self")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        if (manager.getFactionOfMember(target.getUUID()) != null) {
            throw TARGET_ALREADY_IN_FACTION.create();
        }

        FactionInviteManager.createInvite(target.getUUID(), faction.getFactionId(), player.getUUID());
        FactionChat.sendInviteMessage(target, faction, player.getGameProfile().getName());
        FactionChat.sendSuccess(player, faction,
                Component.translatable("command.faction_control.invite_sent", target.getGameProfile().getName()));
        return 1;
    }

    private static int acceptInvite(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        FactionManager manager = FactionManager.get(source.getServer());

        if (manager.getFactionOfMember(player.getUUID()) != null) {
            throw TARGET_ALREADY_IN_FACTION.create();
        }

        FactionInviteManager.Invite invite = FactionInviteManager.accept(player.getUUID());
        if (invite == null) {
            throw NO_PENDING_INVITE.create();
        }
        if (invite.isExpired()) {
            throw INVITE_EXPIRED.create();
        }

        FactionObject faction = manager.getFaction(invite.factionId());
        if (faction == null) {
            throw FACTION_NOT_FOUND.create();
        }

        if (!manager.addMember(faction.getFactionId(), player.getUUID())) {
            source.sendFailure(Component.translatable("command.faction_control.join_failed")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        FactionChat.sendSuccess(player, faction, Component.translatable("faction_control.chat.joined"));
        FactionSync.sendTo(player);

        ServerPlayer inviter = source.getServer().getPlayerList().getPlayer(invite.inviterId());
        if (inviter != null) {
            FactionChat.sendSuccess(inviter, faction,
                    Component.translatable("faction_control.chat.invite_accepted", player.getGameProfile().getName()));
        }

        return 1;
    }

    private static int leaveFaction(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        FactionManager manager = FactionManager.get(source.getServer());
        FactionObject faction = manager.getFactionOfMember(player.getUUID());
        if (faction == null) {
            throw NOT_IN_FACTION.create();
        }
        if (faction.isLeader(player.getUUID())) {
            throw OFFICIAL_CANNOT_LEAVE.create();
        }
        if (!manager.kickMember(faction.getFactionId(), player.getUUID())) {
            throw NOT_IN_FACTION.create();
        }

        FactionChat.sendSuccess(player, faction, Component.translatable("faction_control.chat.left"));
        FactionSync.sendTo(player);
        return 1;
    }

    private static int kickMember(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        FactionManager manager = FactionManager.get(source.getServer());
        FactionObject faction = manager.getFactionOfMember(player.getUUID());
        if (faction == null) {
            throw NOT_IN_FACTION.create();
        }
        if (!faction.isLeader(player.getUUID())) {
            throw NOT_OFFICIAL.create();
        }

        var cache = source.getServer().getProfileCache();
        if (cache == null) {
            throw PLAYER_NOT_FOUND.create();
        }
        GameProfile profile = cache.get(name).orElse(null);
        if (profile == null) {
            throw PLAYER_NOT_FOUND.create();
        }

        UUID targetId = profile.getId();
        if (player.getUUID().equals(targetId)) {
            throw CANNOT_KICK_SELF.create();
        }
        if (!faction.isMember(targetId)) {
            throw TARGET_NOT_IN_FACTION.create();
        }
        if (!manager.kickMember(faction.getFactionId(), targetId)) {
            throw TARGET_NOT_IN_FACTION.create();
        }

        FactionChat.sendSuccess(player, faction,
                Component.translatable("faction_control.chat.kicked_name", profile.getName()));
        ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetId);
        if (target != null) {
            FactionChat.sendError(target, Component.translatable("faction_control.chat.you_were_kicked", faction.getName()));
            FactionSync.sendTo(target);
        }
        return 1;
    }

    private static int listMembers(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        FactionManager manager = FactionManager.get(source.getServer());
        FactionObject faction = manager.getFactionOfMember(player.getUUID());
        if (faction == null) {
            throw NOT_IN_FACTION.create();
        }

        MinecraftServer server = source.getServer();
        List<UUID> members = new ArrayList<>(faction.getMembers());
        members.sort(Comparator.comparing(memberId -> resolvePlayerName(server, memberId), String.CASE_INSENSITIVE_ORDER));
        source.sendSuccess(() -> Component.translatable("command.faction_control.members_header", faction.getName())
                .withStyle(ChatFormatting.GOLD), false);

        for (UUID memberId : members) {
            boolean online = server.getPlayerList().getPlayer(memberId) != null;
            boolean official = faction.isLeader(memberId);
            MutableComponent line = Component.literal("- " + resolvePlayerName(server, memberId))
                    .withStyle(online ? ChatFormatting.GREEN : ChatFormatting.GRAY);
            if (official) {
                line.append(Component.translatable("command.faction_control.member_official")
                        .withStyle(ChatFormatting.GOLD));
            }
            line.append(Component.translatable(online
                            ? "command.faction_control.member_online"
                            : "command.faction_control.member_offline")
                    .withStyle(online ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            source.sendSuccess(() -> line, false);
        }
        return members.size();
    }

    private static int requestFactionDeletion(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        FactionManager manager = FactionManager.get(source.getServer());
        FactionObject faction = manager.getFactionOfMember(player.getUUID());
        if (faction == null) {
            throw NOT_IN_FACTION.create();
        }
        if (!faction.isLeader(player.getUUID())) {
            throw NOT_OFFICIAL.create();
        }

        DELETE_CONFIRMATIONS.put(
                player.getUUID(),
                new DeleteConfirmation(faction.getFactionId(), System.currentTimeMillis() + 30_000L)
        );
        source.sendSuccess(
                () -> Component.translatable("command.faction_control.delete_warning", faction.getName())
                        .withStyle(ChatFormatting.RED),
                false
        );
        return 1;
    }

    private static int confirmFactionDeletion(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        DeleteConfirmation confirmation = DELETE_CONFIRMATIONS.remove(player.getUUID());
        if (confirmation == null) {
            throw NO_DELETE_CONFIRMATION.create();
        }
        if (confirmation.expiresAtMs() < System.currentTimeMillis()) {
            throw DELETE_CONFIRMATION_EXPIRED.create();
        }

        MinecraftServer server = source.getServer();
        FactionManager manager = FactionManager.get(server);
        FactionObject faction = manager.getFactionOfMember(player.getUUID());
        if (faction == null || !faction.getFactionId().equals(confirmation.factionId())) {
            throw NO_DELETE_CONFIRMATION.create();
        }
        if (!faction.isLeader(player.getUUID())) {
            throw NOT_OFFICIAL.create();
        }

        String factionName = faction.getName();
        Set<UUID> members = Set.copyOf(faction.getMembers());
        FlagHelper.stripFactionFlag(server, faction);
        manager.deleteFaction(faction.getFactionId());

        for (UUID memberId : members) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member == null) {
                continue;
            }
            FactionSync.sendTo(member);
            if (!member.getUUID().equals(player.getUUID())) {
                FactionChat.sendError(member,
                        Component.translatable("faction_control.chat.deleted_by_official", factionName));
            }
        }

        source.sendSuccess(() -> Component.translatable("command.faction_control.deleted_by_you", factionName)
                .withStyle(ChatFormatting.RED), false);
        return 1;
    }

    private static int setFlag(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        requireOverworld(player);
        FactionManager manager = FactionManager.get(source.getServer());
        FactionObject faction = manager.getFactionOfMember(player.getUUID());

        if (faction == null) {
            throw NOT_IN_FACTION.create();
        }
        if (!faction.isLeader(player.getUUID())) {
            throw NOT_OFFICIAL.create();
        }
        if (FlagHelper.hasActiveFlagInWorld(source.getServer(), faction)) {
            throw FLAG_ALREADY_ACTIVE.create();
        }

        BlockPos pos = player.blockPosition();
        BlockPos above = pos.above();
        if (!source.getServer().overworld().getBlockState(pos).canBeReplaced()) {
            source.sendFailure(Component.translatable("command.faction_control.flag_no_space")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!source.getServer().overworld().getBlockState(above).canBeReplaced()
                && !source.getServer().overworld().getBlockState(above).isAir()) {
            source.sendFailure(Component.translatable("command.faction_control.flag_needs_height")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!FlagHelper.spawnFactionFlag(player, faction)) {
            source.sendFailure(Component.translatable("command.faction_control.flag_spawn_failed")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        ChunkPos chunkPos = player.chunkPosition();
        FactionChat.sendSuccess(player, faction, Component.translatable(
                "faction_control.chat.flag_placed",
                pos.getX(), pos.getY(), pos.getZ(), chunkPos.x, chunkPos.z));
        FactionSync.sendTo(player);
        return 1;
    }

    private static int createFaction(CommandSourceStack source, String name, String hexColor) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        FactionManager manager = FactionManager.get(source.getServer());

        if (!FactionPermissions.canCreateFaction(player)) {
            throw CANNOT_CREATE_FACTION.create();
        }
        if (manager.getFactionOfMember(player.getUUID()) != null) {
            throw TARGET_ALREADY_IN_FACTION.create();
        }
        if (manager.isNameTaken(name)) {
            throw FACTION_NAME_TAKEN.create();
        }
        if (!FactionConfigManager.isValidColorHex(hexColor)) {
            throw INVALID_HEX_COLOR.create();
        }

        int color = FactionConfigManager.parseColorHex(hexColor);
        FactionObject faction = manager.createFaction(name, color, player.getUUID());

        FactionChat.sendSuccess(player, faction, Component.translatable("faction_control.chat.faction_created"));
        FactionSync.sendTo(player);
        return 1;
    }

    private static int showCanCreate(CommandSourceStack source, ServerPlayer target) {
        boolean allowed = FactionPlayerData.getCanCreateFaction(target);
        boolean effective = FactionPermissions.canCreateFaction(target);
        source.sendSuccess(
                () -> Component.translatable(
                                "command.faction_control.cancreate_status",
                                target.getGameProfile().getName(),
                                allowed,
                                effective)
                        .withStyle(ChatFormatting.AQUA),
                false
        );
        return 1;
    }

    private static int toggleCanCreate(CommandSourceStack source, ServerPlayer target) {
        return sendCanCreateResult(source, target, FactionPlayerData.toggleCanCreateFaction(target));
    }

    private static int setCanCreate(CommandSourceStack source, ServerPlayer target, boolean allowed) {
        FactionPlayerData.setCanCreateFaction(target, allowed);
        return sendCanCreateResult(source, target, allowed);
    }

    private static int sendCanCreateResult(CommandSourceStack source, ServerPlayer target, boolean allowed) {
        ChatFormatting color = allowed ? ChatFormatting.GREEN : ChatFormatting.GRAY;
        Component status = Component.translatable(allowed
                ? "command.faction_control.status.allowed"
                : "command.faction_control.status.blocked");
        source.sendSuccess(
                () -> Component.translatable(
                                "command.faction_control.cancreate_result",
                                target.getGameProfile().getName(),
                                status)
                        .withStyle(color),
                true
        );
        target.sendSystemMessage(Component.translatable(allowed
                ? "command.faction_control.cancreate_allowed"
                : "command.faction_control.cancreate_denied"
        ).withStyle(color));
        return 1;
    }

    private static int deleteForce(CommandSourceStack source, String name) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        FactionManager manager = FactionManager.get(server);

        FactionObject faction = manager.getFactionByName(name);
        if (faction == null) {
            throw FACTION_NOT_FOUND.create();
        }

        String factionName = faction.getName();
        UUID factionId = faction.getFactionId();
        Set<UUID> members = faction.getMembers();

        FlagHelper.stripFactionFlag(server, faction);
        manager.deleteFaction(factionId);

        for (UUID memberId : members) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                FactionSync.sendTo(member);
            }
        }

        source.sendSuccess(() -> Component.translatable("command.faction_control.deleted_permanent", factionName)
                .withStyle(ChatFormatting.RED), true);
        return 1;
    }

    private static int leaveForce(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        FactionManager manager = FactionManager.get(source.getServer());
        FactionObject faction = manager.getFactionOfMember(target.getUUID());
        if (faction == null) {
            throw TARGET_HAS_NO_FACTION.create();
        }

        if (!manager.forceRemoveMember(target.getUUID())) {
            throw TARGET_HAS_NO_FACTION.create();
        }

        source.sendSuccess(() -> Component.translatable(
                        "command.faction_control.leave_force",
                        Component.literal(target.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW),
                        FactionChat.factionPrefix(faction))
                .withStyle(ChatFormatting.GRAY), true);

        FactionChat.sendSuccess(target, faction, Component.translatable("faction_control.chat.removed_by_admin"));
        FactionSync.sendTo(target);
        return 1;
    }

    private static int joinForced(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        FactionManager manager = FactionManager.get(source.getServer());

        FactionObject faction = manager.getFactionByName(name);
        if (faction == null) {
            throw FACTION_NOT_FOUND.create();
        }

        manager.forceJoinFaction(player.getUUID(), faction.getFactionId());

        source.sendSuccess(() -> Component.translatable(
                        "command.faction_control.joined_forced",
                        Component.literal(faction.getName())
                                .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(faction.getColor()))))
                , false);

        FactionSync.sendTo(player);
        return 1;
    }

    private static int setLeader(CommandSourceStack source, ServerPlayer target, String factionName)
            throws CommandSyntaxException {
        FactionManager manager = FactionManager.get(source.getServer());
        FactionObject faction = manager.getFactionByName(factionName);
        if (faction == null) {
            throw FACTION_NOT_FOUND.create();
        }

        if (!faction.isMember(target.getUUID())) {
            throw TARGET_NOT_IN_FACTION.create();
        }

        if (!manager.setFactionLeader(faction.getFactionId(), target.getUUID())) {
            throw FACTION_NOT_FOUND.create();
        }

        source.sendSuccess(() -> Component.translatable(
                        "command.faction_control.set_leader",
                        Component.literal(target.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW),
                        FactionChat.factionPrefix(faction))
                .withStyle(ChatFormatting.GRAY), true);

        FactionChat.sendSuccess(target, faction, Component.translatable("faction_control.chat.promoted"));
        FactionSync.sendTo(target);
        return 1;
    }

    private static int adminClaim(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        requireOverworld(player);
        ChunkPos chunkPos = player.chunkPosition();
        FactionManager manager = FactionManager.get(source.getServer());

        if (manager.isAdminChunk(chunkPos)) {
            throw ADMIN_CHUNK_ALREADY_CLAIMED.create();
        }

        manager.claimAdminChunk(chunkPos);
        source.sendSuccess(() -> Component.translatable(
                        "command.faction_control.admin_claim",
                        chunkPos.x + ", " + chunkPos.z)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int adminUnclaim(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        requireOverworld(player);
        ChunkPos chunkPos = player.chunkPosition();
        FactionManager manager = FactionManager.get(source.getServer());

        if (!manager.isAdminChunk(chunkPos)) {
            throw NOT_ADMIN_CHUNK.create();
        }

        manager.unclaimAdminChunk(chunkPos);
        source.sendSuccess(() -> Component.translatable(
                        "command.faction_control.admin_unclaim",
                        chunkPos.x + ", " + chunkPos.z)
                .withStyle(ChatFormatting.GRAY), true);
        return 1;
    }

    private static int adminSetChunk(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        ChunkPos chunkPos = player.chunkPosition();
        FactionManager manager = FactionManager.get(source.getServer());

        FactionObject faction = manager.getFactionByName(name);
        if (faction == null) {
            throw FACTION_NOT_FOUND.create();
        }

        manager.forceClaimChunk(faction.getFactionId(), chunkPos);

        source.sendSuccess(() -> Component.translatable(
                        "command.faction_control.admin_setchunk",
                        chunkPos.x + ", " + chunkPos.z,
                        FactionChat.factionPrefix(faction))
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int adminRemoveChunk(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        ChunkPos chunkPos = player.chunkPosition();
        FactionManager manager = FactionManager.get(source.getServer());

        FactionObject faction = manager.getFactionByName(name);
        if (faction == null) {
            throw FACTION_NOT_FOUND.create();
        }

        if (!manager.removeSingleChunk(faction.getFactionId(), chunkPos)) {
            throw CHUNK_NOT_CLAIMED_BY_FACTION.create();
        }

        source.sendSuccess(() -> Component.translatable(
                        "command.faction_control.admin_removechunk",
                        chunkPos.x + ", " + chunkPos.z,
                        FactionChat.factionPrefix(faction))
                .withStyle(ChatFormatting.GRAY), true);
        return 1;
    }

    private static int listFactions(CommandSourceStack source) {
        FactionManager manager = FactionManager.get(source.getServer());
        MinecraftServer server = source.getServer();
        List<FactionObject> factions = new ArrayList<>(manager.getAllFactions());
        factions.sort(Comparator.comparing(faction -> faction.getName().toLowerCase()));

        if (factions.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.faction_control.no_factions")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.faction_control.faction_list_header")
                .withStyle(ChatFormatting.GOLD), false);

        for (FactionObject faction : factions) {
            int totalMembers = faction.getMembers().size();
            int onlineMembers = countOnlineMembers(server, faction);
            MutableComponent line = FactionChat.factionPrefix(faction)
                    .append(Component.translatable("command.faction_control.member_count", onlineMembers, totalMembers)
                            .withStyle(ChatFormatting.GRAY));
            source.sendSuccess(() -> line, false);
        }

        return factions.size();
    }

    private static int factionInfo(CommandSourceStack source, String name) throws CommandSyntaxException {
        FactionManager manager = FactionManager.get(source.getServer());
        FactionObject faction = manager.getFactionByName(name);
        if (faction == null) {
            throw FACTION_NOT_FOUND.create();
        }

        MinecraftServer server = source.getServer();
        source.sendSuccess(() -> Component.translatable("command.faction_control.info_header", faction.getName())
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> FactionChat.factionPrefix(faction)
                .append(Component.literal(faction.getName())), false);
        source.sendSuccess(() -> Component.translatable("command.faction_control.info_official")
                .withStyle(ChatFormatting.GRAY)
                .append(formatLeader(server, faction)), false);
        source.sendSuccess(() -> Component.translatable("command.faction_control.info_flag")
                .withStyle(ChatFormatting.GRAY)
                .append(formatFlagLocation(server, faction).withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.translatable("command.faction_control.info_state")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(faction.getFlagState().name()).withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.translatable("command.faction_control.info_chunks")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(manager.getFactionClaims(faction.getFactionId()).size()))
                        .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static MutableComponent formatLeader(MinecraftServer server, FactionObject faction) {
        UUID leaderId = faction.getLeaderId();
        if (leaderId == null) {
            return Component.translatable("command.faction_control.none").withStyle(ChatFormatting.DARK_GRAY);
        }
        return Component.literal(resolvePlayerName(server, leaderId)).withStyle(ChatFormatting.YELLOW);
    }

    private static MutableComponent formatFlagLocation(MinecraftServer server, FactionObject faction) {
        BlockPos pos = faction.getFlagBlockPos();
        if (pos != null) {
            return Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        }
        ChunkPos chunk = faction.getFlagChunk();
        if (chunk != null) {
            BlockPos found = FlagHelper.findFlagBlockPos(server.overworld(), chunk);
            if (found != null) {
                return Component.literal(found.getX() + ", " + found.getY() + ", " + found.getZ());
            }
            return Component.translatable("command.faction_control.flag_missing_block", chunk.x, chunk.z);
        }
        return Component.translatable("command.faction_control.no_flag");
    }

    private static int countOnlineMembers(MinecraftServer server, FactionObject faction) {
        int online = 0;
        for (UUID memberId : faction.getMembers()) {
            if (server.getPlayerList().getPlayer(memberId) != null) {
                online++;
            }
        }
        return online;
    }

    private static int reloadConfig(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        FactionManager.reload(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetwork.sendPlayerFactionSync(player);
        }

        source.sendSuccess(
                () -> Component.translatable("command.faction_control.reloaded"),
                true
        );
        return 1;
    }

    private static int showPlayMode(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        source.sendSuccess(
                () -> PlayerPlayModeHelper.describeMode(player).copy().withStyle(ChatFormatting.AQUA),
                false
        );
        return 1;
    }

    private static int togglePlayMode(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        boolean enabled = PlayerPlayModeHelper.togglePlayAsPlayer(player);
        return sendPlayModeResult(source, enabled);
    }

    private static int setPlayMode(CommandSourceStack source, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        PlayerPlayModeHelper.setPlayAsPlayer(player, enabled);
        return sendPlayModeResult(source, enabled);
    }

    private static int sendPlayModeResult(CommandSourceStack source, boolean playAsPlayer) {
        if (playAsPlayer) {
            source.sendSuccess(
                    () -> Component.translatable("command.faction_control.playmode_player")
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> Component.translatable("command.faction_control.playmode_admin")
                            .withStyle(ChatFormatting.GOLD),
                    true
            );
        }
        return 1;
    }

    private static int showClickLogging(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.translatable(
                                "command.faction_control.click_logging",
                                Component.translatable(FactionDebugSettings.isClickLoggingEnabled()
                                        ? "command.faction_control.logging_on"
                                        : "command.faction_control.logging_off"))
                        .withStyle(ChatFormatting.AQUA),
                false
        );
        return 1;
    }

    private static int toggleClickLogging(CommandSourceStack source) {
        return sendClickLoggingResult(source, FactionDebugSettings.setClickLoggingEnabled(
                !FactionDebugSettings.isClickLoggingEnabled()));
    }

    private static int setClickLogging(CommandSourceStack source, boolean enabled) {
        FactionDebugSettings.setClickLoggingEnabled(enabled);
        return sendClickLoggingResult(source, enabled);
    }

    private static int sendClickLoggingResult(CommandSourceStack source, boolean enabled) {
        ChatFormatting color = enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY;
        Component status = Component.translatable(enabled
                ? "command.faction_control.logging_on"
                : "command.faction_control.logging_off");
        source.sendSuccess(
                () -> Component.translatable("command.faction_control.click_logging", status)
                        .withStyle(color),
                true
        );
        return 1;
    }

    private static void requireOverworld(ServerPlayer player) throws CommandSyntaxException {
        if (player.level().dimension() != Level.OVERWORLD) {
            throw NOT_IN_OVERWORLD.create();
        }
    }

    private static ServerPlayer requirePlayer(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            throw NOT_A_PLAYER.create();
        }
        return player;
    }

    private static String resolvePlayerName(MinecraftServer server, UUID playerId) {
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerId);
        if (onlinePlayer != null) {
            return onlinePlayer.getGameProfile().getName();
        }
        @Nullable GameProfile profile = server.getProfileCache().get(playerId).orElse(null);
        return profile != null ? profile.getName() : playerId.toString();
    }

    private record DeleteConfirmation(UUID factionId, long expiresAtMs) {
    }
}
