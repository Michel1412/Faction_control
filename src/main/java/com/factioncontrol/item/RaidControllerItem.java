package com.factioncontrol.item;

import com.factioncontrol.faction.FactionManager;
import com.factioncontrol.faction.FactionObject;
import com.factioncontrol.util.FactionChat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class RaidControllerItem extends Item {
    private static final int USE_DURATION_TICKS = 72000;

    private static final Component TOOLTIP = Component.literal(
            "Apenas o Oficial: segure o clique direito em territorio inimigo por 60s para hackear a bandeira."
    );

    public RaidControllerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(TOOLTIP);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        if (!FactionManager.isServerDataReady()) {
            return InteractionResultHolder.fail(serverPlayer.getItemInHand(hand));
        }

        FactionObject faction = FactionManager.get(serverPlayer.serverLevel())
                .getFactionOfMember(serverPlayer.getUUID());
        if (faction == null || !faction.isLeader(serverPlayer.getUUID())) {
            FactionChat.sendErrorActionBar(serverPlayer, "Apenas o Oficial da faccao pode usar o Controle de Hack.");
            return InteractionResultHolder.fail(serverPlayer.getItemInHand(hand));
        }

        serverPlayer.startUsingItem(hand);
        return InteractionResultHolder.consume(serverPlayer.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }
}
