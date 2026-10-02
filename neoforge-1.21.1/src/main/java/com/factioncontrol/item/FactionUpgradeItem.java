package com.factioncontrol.item;

import com.factioncontrol.block.FlagBlock;
import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.faction.FactionObject;
import com.factioncontrol.registry.ModDataComponents;
import com.factioncontrol.registry.ModDataComponents.SelectedChunk;
import com.factioncontrol.util.FactionChat;
import com.factioncontrol.util.FlagHelper;
import com.factioncontrol.util.TerritoryProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class FactionUpgradeItem extends Item {
    private static final Component TOOLTIP_DESCRIPTION = Component.translatable(
            "item.faction_control.faction_upgrade_item.tooltip"
    );
    private static final Component TOOLTIP_NO_CHUNK = Component.translatable(
            "item.faction_control.faction_upgrade_item.no_chunk"
    );

    public FactionUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(TOOLTIP_DESCRIPTION);

        if (hasSelectedChunk(stack)) {
            ChunkPos chunk = getSelectedChunk(stack);
            tooltip.add(Component.translatable("item.faction_control.faction_upgrade_item.chunk", chunk.x, chunk.z));
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
                    Component.translatable("faction_control.upgrade.admin_zone_bind")
            );
            return;
        }

        setSelectedChunk(stack, chunkPos);
        FactionChat.sendSuccessActionBar(
                serverPlayer,
                Component.translatable("faction_control.upgrade.chunk_selected", chunkPos.x, chunkPos.z)
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
            FactionChat.sendError(player, Component.translatable("faction_control.upgrade.unlinked_flag"));
            return InteractionResult.FAIL;
        }

        FactionManager manager = FactionManager.get(level);

        if (!faction.isLeader(player.getUUID())) {
            FactionChat.sendError(player, Component.translatable("faction_control.upgrade.not_official"));
            return InteractionResult.FAIL;
        }

        if (!hasSelectedChunk(stack)) {
            FactionChat.sendErrorActionBar(player, Component.translatable("faction_control.upgrade.need_chunk"));
            return InteractionResult.FAIL;
        }

        ChunkPos selectedChunk = getSelectedChunk(stack);
        UUID factionId = faction.getFactionId();

        if (!TerritoryProtectionHelper.canRegisterChunkForUpgrade(selectedChunk)) {
            FactionChat.sendErrorActionBar(
                    player,
                    Component.translatable("faction_control.upgrade.admin_zone_claim")
            );
            return InteractionResult.FAIL;
        }

        if (manager.isChunkOwnedByFaction(factionId, selectedChunk)) {
            FactionChat.sendErrorActionBar(player, Component.translatable("faction_control.upgrade.already_yours"));
            return InteractionResult.FAIL;
        }

        if (manager.isChunkClaimed(selectedChunk)) {
            FactionChat.sendErrorActionBar(player, Component.translatable("faction_control.upgrade.other_faction"));
            return InteractionResult.FAIL;
        }

        if (!manager.isChunkAdjacentToFactionTerritory(factionId, selectedChunk)) {
            FactionChat.sendErrorActionBar(player, Component.translatable("faction_control.upgrade.not_adjacent"));
            return InteractionResult.FAIL;
        }

        if (!manager.claimSingleChunk(factionId, selectedChunk)) {
            return InteractionResult.FAIL;
        }

        stack.shrink(1);
        spawnSuccessParticles(level, flagPos);
        FactionChat.sendSuccess(player, faction,
                Component.translatable("faction_control.upgrade.expanded", selectedChunk.x, selectedChunk.z));
        return InteractionResult.CONSUME;
    }

    private static void spawnSuccessParticles(ServerLevel level, BlockPos flagPos) {
        double x = flagPos.getX() + 0.5D;
        double y = flagPos.getY() + 1.0D;
        double z = flagPos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 12, 0.4D, 0.4D, 0.4D, 0.02D);
    }

    public static boolean hasSelectedChunk(ItemStack stack) {
        return stack.has(ModDataComponents.SELECTED_CHUNK);
    }

    @Nullable
    public static ChunkPos getSelectedChunk(ItemStack stack) {
        SelectedChunk selected = stack.get(ModDataComponents.SELECTED_CHUNK);
        if (selected == null) {
            return null;
        }
        return new ChunkPos(selected.x(), selected.z());
    }

    public static void setSelectedChunk(ItemStack stack, ChunkPos chunkPos) {
        stack.set(ModDataComponents.SELECTED_CHUNK, new SelectedChunk(chunkPos.x, chunkPos.z));
    }
}
