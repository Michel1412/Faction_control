package com.factioncontrol.client;

import com.factioncontrol.registry.ModItems;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Item tint registration. Pixel-art textures are fully colored; handlers return {@link #NO_TINT}.
 */
public final class ClientModEvents {
    private static final int NO_TINT = 0xFFFFFF;

    private ClientModEvents() {
    }

    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? NO_TINT : -1,
                ModItems.FACTION_UPGRADE.get()
        );
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? NO_TINT : -1,
                ModItems.RAID_CONTROLLER.get()
        );
    }
}
