package com.factioncontrol.item;

import com.factioncontrol.registry.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FlagBlockItem extends BlockItem {
    private static final Component TOOLTIP = Component.translatable("item.faction_control.flag_block.tooltip");

    public FlagBlockItem(Properties properties) {
        super(ModBlocks.FLAG_BLOCK.get(), properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(TOOLTIP);
    }
}
