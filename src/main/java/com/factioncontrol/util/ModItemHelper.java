package com.factioncontrol.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

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
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && TACZ_NAMESPACE.equals(id.getNamespace());
    }
}
