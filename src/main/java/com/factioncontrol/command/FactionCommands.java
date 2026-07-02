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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class FactionCommands {
    private static final SimpleCommandExceptionType NOT_A_PLAYER =
            new SimpleCommandExceptionType(Component.literal("Este comando so pode ser usado por jogadores."));
    private static final SimpleCommandExceptionType NOT_IN_FACTION =
            new SimpleCommandExceptionType(Component.literal("Voce nao pertence a nenhuma faccao."));
    private static final SimpleCommandExceptionType NOT_OFFICIAL =
            new SimpleCommandExceptionType(Component.literal("Apenas o Oficial da faccao pode executar este comando."));
    private static final SimpleCommandExceptionType FACTION_NOT_FOUND =
            new SimpleCommandExceptionType(Component.literal("Faccao nao encontrada."));
    private static final SimpleCommandExceptionType FLAG_ALREADY_ACTIVE =
            new SimpleCommandExceptionType(Component.literal("Sua faccao ja possui uma bandeira ativa no mundo."));
    private static final SimpleCommandExceptionType TARGET_NOT_IN_FACTION =
            new SimpleCommandExceptionType(Component.literal("Este jogador nao pertence a faccao especificada."));
    private static final SimpleCommandExceptionType TARGET_HAS_NO_FACTION =
            new SimpleCommandExceptionType(Component.literal("Este jogador nao pertence a nenhuma faccao."));
    private static final SimpleCommandExceptionType NOT_IN_OVERWORLD =
            new SimpleCommandExceptionType(Component.literal("Este comando so pode ser usado no Overworld."));
    private static final SimpleCommandExceptionType ADMIN_CHUNK_ALREADY_CLAIMED =
            new SimpleCommandExceptionType(Component.literal("Este chunk ja e uma Safezone de administradores."));
    private static final SimpleCommandExceptionType NOT_ADMIN_CHUNK =
            new SimpleCommandExceptionType(Component.literal("Este chunk nao e uma Safezone de administradores."));
    private static final SimpleCommandExceptionType CHUNK_NOT_CLAIMED_BY_FACTION =
            new SimpleCommandExceptionType(Component.literal("Este chunk nao pertence a faccao especificada."));
    private static final SimpleCommandExceptionType FACTION_NAME_TAKEN =
            new SimpleCommandExceptionType(Component.literal("Ja existe uma faccao com este nome."));
    private static final SimpleCommandExceptionType INVALID_HEX_COLOR =
            new SimpleCommandExceptionType(Component.literal("Cor HEX invalida. Use o formato #RRGGBB (ex: #FF0000)."));
    private static final SimpleCommandExceptionType TARGET_ALREADY_IN_FACTION =
            new SimpleCommandExceptionType(Component.literal("Este jogador ja pertence a uma faccao."));
    private static final SimpleCommandExceptionType NO_PENDING_INVITE =
            new SimpleCommandExceptionType(Component.literal("Voce nao possui convite pendente."));
    private static final SimpleCommandExceptionType INVITE_EXPIRED =
            new SimpleCommandExceptionType(Component.literal("Seu convite expirou. Peça um novo convite ao Oficial."));

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
                .then(Commands.literal("create")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("nome", StringArgumentType.word())
                                .then(Commands.argument("cor", StringArgumentType.string())
                                        .executes(context -> createFaction(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "nome"),
                                                StringArgumentType.getString(context, "cor")
                                        )))))
                .then(Commands.literal("delete_force")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                .executes(context -> deleteForce(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "nome")
                                ))))
                .then(Commands.literal("join_forced")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                .executes(context -> joinForced(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "nome")
                                ))))
                .then(Commands.literal("leave_force")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> leaveForce(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")
                                ))))
                .then(Commands.literal("set_leader")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(context -> setLeader(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "nome")
                                        )))))
                .then(Commands.literal("admin_claim")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> adminClaim(context.getSource())))
                .then(Commands.literal("admin_unclaim")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> adminUnclaim(context.getSource())))
                .then(Commands.literal("admin_setchunk")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                .executes(context -> adminSetChunk(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "nome")
                                ))))
                .then(Commands.literal("admin_removechunk")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                .executes(context -> adminRemoveChunk(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "nome")
                                ))))
                .then(Commands.literal("list")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> listFactions(context.getSource())))
                .then(Commands.literal("info")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                .executes(context -> factionInfo(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "nome")
                                ))))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reloadConfig(context.getSource())))
                .then(Commands.literal("playmode")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> showPlayMode(context.getSource()))
                        .then(Commands.literal("toggle")
                                .executes(context -> togglePlayMode(context.getSource())))
                        .then(Commands.argument("ativar", BoolArgumentType.bool())
                                .executes(context -> setPlayMode(
                                        context.getSource(),
                                        BoolArgumentType.getBool(context, "ativar")
                                ))))
                .then(Commands.literal("debug")
                        .requires(source -> source.hasPermission(2))
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
            source.sendFailure(Component.literal("Voce nao pode convidar a si mesmo.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        if (manager.getFactionOfMember(target.getUUID()) != null) {
            throw TARGET_ALREADY_IN_FACTION.create();
        }

        FactionInviteManager.createInvite(target.getUUID(), faction.getFactionId(), player.getUUID());
        FactionChat.sendInviteMessage(target, faction, player.getGameProfile().getName());
        FactionChat.sendSuccess(player, faction,
                "Convite enviado para " + target.getGameProfile().getName() + ".");
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
            source.sendFailure(Component.literal("Nao foi possivel entrar na faccao.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        manager.forceSave();
        FactionChat.sendSuccess(player, faction, "Voce entrou na faccao!");
        FactionSync.sendTo(player);

        ServerPlayer inviter = source.getServer().getPlayerList().getPlayer(invite.inviterId());
        if (inviter != null) {
            FactionChat.sendSuccess(inviter, faction,
                    player.getGameProfile().getName() + " aceitou o convite e entrou na faccao.");
        }

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
            source.sendFailure(Component.literal("Nao ha espaco para spawnar a bandeira nesta posicao.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!source.getServer().overworld().getBlockState(above).canBeReplaced()
                && !source.getServer().overworld().getBlockState(above).isAir()) {
            source.sendFailure(Component.literal("A bandeira precisa de 2 blocos de altura livre acima da posicao.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!FlagHelper.spawnFactionFlag(player, faction)) {
            source.sendFailure(Component.literal("Falha ao spawnar a bandeira.").withStyle(ChatFormatting.RED));
            return 0;
        }

        ChunkPos chunkPos = player.chunkPosition();
        FactionChat.sendSuccess(player, faction,
                "Bandeira posicionada em [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                        + "]! Chunk [" + chunkPos.x + ", " + chunkPos.z + "] claimado.");
        FactionSync.sendTo(player);
        return 1;
    }

    private static int createFaction(CommandSourceStack source, String name, String hexColor) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        FactionManager manager = FactionManager.get(source.getServer());

        if (manager.isNameTaken(name)) {
            throw FACTION_NAME_TAKEN.create();
        }
        if (!FactionConfigManager.isValidColorHex(hexColor)) {
            throw INVALID_HEX_COLOR.create();
        }

        int color = FactionConfigManager.parseColorHex(hexColor);
        FactionObject faction = manager.createFaction(name, color, player.getUUID());
        manager.forceSave();

        FactionChat.sendSuccess(player, faction, "Faccao criada! Voce e o Oficial.");
        FactionSync.sendTo(player);
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
        ChunkPos flagChunk = faction.getFlagChunk();
        if (flagChunk != null) {
            FlagHelper.removeFlagsInChunk(server.overworld(), flagChunk);
        }
        manager.deleteFaction(factionId);
        manager.forceSave();

        for (UUID memberId : members) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                FactionSync.sendTo(member);
            }
        }

        source.sendSuccess(() -> Component.literal(
                        "A faccao " + factionName + " foi deletada permanentemente.")
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

        source.sendSuccess(() -> Component.literal("Jogador ")
                .append(Component.literal(target.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" foi removido da faccao "))
                .append(FactionChat.factionPrefix(faction))
                .append(Component.literal(".").withStyle(ChatFormatting.GRAY)), true);

        FactionChat.sendSuccess(target, faction, "Voce foi removido da faccao por um administrador.");
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

        source.sendSuccess(() -> Component.literal("Voce entrou na faccao ")
                .append(Component.literal(faction.getName())
                        .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(faction.getColor()))))
                .append(Component.literal(".")), false);

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

        source.sendSuccess(() -> Component.literal("Jogador ")
                .append(Component.literal(target.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" foi definido como Oficial de "))
                .append(FactionChat.factionPrefix(faction))
                .append(Component.literal(".").withStyle(ChatFormatting.GRAY)), true);

        FactionChat.sendSuccess(target, faction, "Voce foi promovido a Oficial da faccao!");
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
        source.sendSuccess(() -> Component.literal("Chunk ")
                .append(Component.literal(chunkPos.x + ", " + chunkPos.z).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" registrado como Safezone de administradores.").withStyle(ChatFormatting.GREEN)), true);
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
        source.sendSuccess(() -> Component.literal("Chunk ")
                .append(Component.literal(chunkPos.x + ", " + chunkPos.z).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" removido da Safezone.").withStyle(ChatFormatting.GRAY)), true);
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
        manager.forceSave();

        source.sendSuccess(() -> Component.literal("Chunk ")
                .append(Component.literal(chunkPos.x + ", " + chunkPos.z).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" claimado para "))
                .append(FactionChat.factionPrefix(faction))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)), true);
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

        manager.forceSave();
        source.sendSuccess(() -> Component.literal("Chunk ")
                .append(Component.literal(chunkPos.x + ", " + chunkPos.z).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" removido de "))
                .append(FactionChat.factionPrefix(faction))
                .append(Component.literal(".").withStyle(ChatFormatting.GRAY)), true);
        return 1;
    }

    private static int listFactions(CommandSourceStack source) {
        FactionManager manager = FactionManager.get(source.getServer());
        MinecraftServer server = source.getServer();
        List<FactionObject> factions = new ArrayList<>(manager.getAllFactions());
        factions.sort(Comparator.comparing(faction -> faction.getName().toLowerCase()));

        if (factions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Nenhuma faccao registrada.")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("=== Faccoes ===").withStyle(ChatFormatting.GOLD), false);

        for (FactionObject faction : factions) {
            int totalMembers = faction.getMembers().size();
            int onlineMembers = countOnlineMembers(server, faction);
            MutableComponent line = FactionChat.factionPrefix(faction)
                    .append(Component.literal("Membros: " + onlineMembers + "/" + totalMembers)
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
        source.sendSuccess(() -> Component.literal("=== Info: " + faction.getName() + " ===")
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> FactionChat.factionPrefix(faction)
                .append(Component.literal(faction.getName())), false);
        source.sendSuccess(() -> Component.literal("Oficial: ")
                .withStyle(ChatFormatting.GRAY)
                .append(formatLeader(server, faction)), false);
        source.sendSuccess(() -> Component.literal("Bandeira: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(formatFlagLocation(server, faction)).withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("Estado: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(faction.getFlagState().name()).withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("Chunks: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(manager.getFactionClaims(faction.getFactionId()).size()))
                        .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static MutableComponent formatLeader(MinecraftServer server, FactionObject faction) {
        UUID leaderId = faction.getLeaderId();
        if (leaderId == null) {
            return Component.literal("Nenhum").withStyle(ChatFormatting.DARK_GRAY);
        }
        return Component.literal(resolvePlayerName(server, leaderId)).withStyle(ChatFormatting.YELLOW);
    }

    private static String formatFlagLocation(MinecraftServer server, FactionObject faction) {
        BlockPos pos = faction.getFlagBlockPos();
        if (pos != null) {
            return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        }
        ChunkPos chunk = faction.getFlagChunk();
        if (chunk != null) {
            BlockPos found = FlagHelper.findFlagBlockPos(server.overworld(), chunk);
            if (found != null) {
                return found.getX() + ", " + found.getY() + ", " + found.getZ();
            }
            return "Chunk " + chunk.x + ", " + chunk.z + " (bloco ausente)";
        }
        return "Nenhuma bandeira registrada";
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
                () -> Component.literal("Faction Control recarregado de config/faction_control.json."),
                true
        );
        return 1;
    }

    private static int showPlayMode(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        source.sendSuccess(
                () -> Component.literal(PlayerPlayModeHelper.describeMode(player))
                        .withStyle(ChatFormatting.AQUA),
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
                    () -> Component.literal("Modo Jogador ATIVADO: regras de territorio e GameMode do mod se aplicam a voce.")
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal("Modo Admin ATIVADO: bypass de territorio; GameMode nao e alterado pelo mod.")
                            .withStyle(ChatFormatting.GOLD),
                    true
            );
        }
        return 1;
    }

    private static int showClickLogging(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal("Logs de clique [FACTION CLICK / GAMEMODE]: "
                                + FactionDebugSettings.describeClickLogging())
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
        String status = enabled ? "ATIVADOS" : "DESATIVADOS";
        source.sendSuccess(
                () -> Component.literal("Logs de clique [FACTION CLICK / GAMEMODE]: " + status)
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
}
