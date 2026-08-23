package com.factioncontrol.command;

import com.factioncontrol.FactionControlMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = FactionControlMod.MODID)
public final class FactionCommandRegistry {
    private FactionCommandRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        FactionCommands.register(event.getDispatcher());
    }
}
