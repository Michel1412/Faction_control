package com.factioncontrol;

import com.factioncontrol.network.ModNetwork;
import com.factioncontrol.compat.ModCompatibility;
import com.factioncontrol.registry.ModBlocks;
import com.factioncontrol.registry.ModCreativeTabs;
import com.factioncontrol.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FactionControlMod.MODID)
public class FactionControlMod {
    public static final String MODID = "faction_control";

    public FactionControlMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            ModCompatibility.init();
        });
    }
}
