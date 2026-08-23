package com.factioncontrol.registry;

import com.factioncontrol.FactionControlMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FactionControlMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FACTION_CONTROL = CREATIVE_MODE_TABS.register(
            "faction_control",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Faction Control"))
                    .icon(() -> new ItemStack(ModItems.FLAG_BLOCK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.FLAG_BLOCK.get());
                        output.accept(ModItems.FACTION_UPGRADE.get());
                        output.accept(ModItems.RAID_CONTROLLER.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }
}
