package com.factioncontrol.item;

import com.factioncontrol.block.FlagBlock;
import com.factioncontrol.faction.FactionObject;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.util.FactionChat;
import com.factioncontrol.util.FlagHelper;
import com.factioncontrol.util.TerritoryProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class FactionUpgradeItem extends Item {
    private static final String NBT_CHUNK_X = "SelectedChunkX";
    private static final String NBT_CHUNK_Z = "SelectedChunkZ";

    private static final Component TOOLTIP_DESCRIPTION = Component.literal(
            "Vincule um chunk (Shift+Clique) e aplique na Bandeira para expandir o territorio."
    );
    private static final Component TOOLTIP_NO_CHUNK = Component.literal(
            "Nenhum chunk selecionado (Shift+Clique para configurar)"
    );

    public FactionUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(TOOLTIP_DESCRIPTION);

        if (hasSelectedChunk(stack)) {
            ChunkPos chunk = getSelectedChunk(stack);
            tooltip.add(Component.literal("Chunk: [" + chunk.x + ", " + chunk.z + "]"));
        } else {
            tooltip.add(TOOLTIP_NO_CHUNK);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        ItemStack stack = player.getItemInHand(hand);
        bindPlayerChunk(stack, player);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            bindPlayerChunk(context.getItemInHand(), player);
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FlagBlock)) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        return applyUpgradeToFlag(serverPlayer, serverLevel, pos, context.getItemInHand());
    }

    private static void bindPlayerChunk(ItemStack stack, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ChunkPos chunkPos = player.chunkPosition();
        if (!TerritoryProtectionHelper.canRegisterChunkForUpgrade(chunkPos)) {
            FactionChat.sendErrorActionBar(
                    serverPlayer,
                    "Este chunk e uma Zona de Administradores e nao pode ser registrado para upgrade."
            );
            return;
        }

        setSelectedChunk(stack, chunkPos);
        FactionChat.sendSuccessActionBar(
                serverPlayer,
                "Chunk selecionado: [" + chunkPos.x + ", " + chunkPos.z + "]"
        );
    }

    private static InteractionResult applyUpgradeToFlag(
            ServerPlayer player,
            ServerLevel level,
            BlockPos flagPos,
            ItemStack stack
    ) {
        FactionObject faction = FlagHelper.resolveFactionAtFlag(level, flagPos);
        if (faction == null) {
            FactionChat.sendError(player, "Esta bandeira nao esta vinculada a uma faccao.");
            return InteractionResult.FAIL;
        }

        FactionManager manager = FactionManager.get(level);

        if (!faction.isLeader(player.getUUID())) {
            FactionChat.sendError(player, "Apenas o Oficial da faccao pode aplicar upgrades.");
            return InteractionResult.FAIL;
        }

        if (!hasSelectedChunk(stack)) {
            FactionChat.sendErrorActionBar(player, "Configure um chunk no item antes (Shift+Clique).");
            return InteractionResult.FAIL;
        }

        ChunkPos selectedChunk = getSelectedChunk(stack);
        UUID factionId = faction.getFactionId();

        if (!TerritoryProtectionHelper.canRegisterChunkForUpgrade(selectedChunk)) {
            FactionChat.sendErrorActionBar(
                    player,
                    "Este chunk e uma Zona de Administradores e nao pode ser claimado pela faccao."
            );
            return InteractionResult.FAIL;
        }

        if (manager.isChunkOwnedByFaction(factionId, selectedChunk)) {
            FactionChat.sendErrorActionBar(player, "Este chunk ja pertence a sua faccao.");
            return InteractionResult.FAIL;
        }

        if (manager.isChunkClaimed(selectedChunk)) {
            FactionChat.sendErrorActionBar(player, "Este chunk ja pertence a outra faccao.");
            return InteractionResult.FAIL;
        }

        if (!manager.isChunkAdjacentToFactionTerritory(factionId, selectedChunk)) {
            FactionChat.sendErrorActionBar(player, "Esse chunk nao e adjacente ao territorio da sua Faccao!");
            return InteractionResult.FAIL;
        }

        if (!manager.claimSingleChunk(factionId, selectedChunk)) {
            return InteractionResult.FAIL;
        }

        manager.forceSave();
        stack.shrink(1);
        spawnSuccessParticles(level, flagPos);
        FactionChat.sendSuccess(player, faction,
                "Territorio expandido! Chunk [" + selectedChunk.x + ", " + selectedChunk.z + "] claimado.");
        return InteractionResult.CONSUME;
    }

    private static void spawnSuccessParticles(ServerLevel level, BlockPos flagPos) {
        double x = flagPos.getX() + 0.5D;
        double y = flagPos.getY() + 1.0D;
        double z = flagPos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 12, 0.4D, 0.4D, 0.4D, 0.02D);
    }

    public static boolean hasSelectedChunk(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_CHUNK_X) && tag.contains(NBT_CHUNK_Z);
    }

    @Nullable
    public static ChunkPos getSelectedChunk(ItemStack stack) {
        if (!hasSelectedChunk(stack)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        return new ChunkPos(tag.getInt(NBT_CHUNK_X), tag.getInt(NBT_CHUNK_Z));
    }

    public static void setSelectedChunk(ItemStack stack, ChunkPos chunkPos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NBT_CHUNK_X, chunkPos.x);
        tag.putInt(NBT_CHUNK_Z, chunkPos.z);
    }
}
