package com.factioncontrol.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Helpers for detecting items from optional mods without hard compile dependencies.
 */
public final class ModItemHelper {
    private static final String TACZ_NAMESPACE = "tacz";

    private ModItemHelper() {
    }

    public static boolean isTaczItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && TACZ_NAMESPACE.equals(id.getNamespace());
    }
}
