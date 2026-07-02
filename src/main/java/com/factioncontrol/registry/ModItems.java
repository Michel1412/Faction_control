package com.factioncontrol.registry;

import com.factioncontrol.FactionControlMod;
import com.factioncontrol.item.FactionUpgradeItem;
import com.factioncontrol.item.FlagBlockItem;
import com.factioncontrol.item.RaidControllerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FactionControlMod.MODID);

    public static final RegistryObject<FlagBlockItem> FLAG_BLOCK = ITEMS.register(
            "flag_block",
            () -> new FlagBlockItem(new Item.Properties())
    );

    public static final RegistryObject<FactionUpgradeItem> FACTION_UPGRADE = ITEMS.register(
            "faction_upgrade_item",
            () -> new FactionUpgradeItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<RaidControllerItem> RAID_CONTROLLER = ITEMS.register(
            "raid_controller_item",
            () -> new RaidControllerItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }
}
