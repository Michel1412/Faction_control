package com.factioncontrol.item;

import com.factioncontrol.registry.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class FlagBlockItem extends BlockItem {
    private static final Component TOOLTIP = Component.literal(
            "Bandeira da faccao. Use /faction set flag para posicionar no mundo."
    );

    public FlagBlockItem(Properties properties) {
        super(ModBlocks.FLAG_BLOCK.get(), properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(TOOLTIP);
    }
}
