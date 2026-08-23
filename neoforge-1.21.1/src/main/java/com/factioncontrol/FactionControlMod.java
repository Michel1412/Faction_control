package com.factioncontrol;

import com.factioncontrol.client.ClientModEvents;
import com.factioncontrol.compat.ModCompatibility;
import com.factioncontrol.network.ModNetwork;
import com.factioncontrol.registry.ModBlocks;
import com.factioncontrol.registry.ModCreativeTabs;
import com.factioncontrol.registry.ModDataComponents;
import com.factioncontrol.registry.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(FactionControlMod.MODID)
public class FactionControlMod {
    public static final String MODID = "faction_control";

    public FactionControlMod(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModNetwork::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientModEvents::registerItemColors);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModCompatibility::init);
    }
}
