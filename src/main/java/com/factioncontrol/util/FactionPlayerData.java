package com.factioncontrol.util;

import com.factioncontrol.FactionControlMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Single NBT compound {@code faction_control} on the player. Reused by play-mode and create-faction gates.
 */
@Mod.EventBusSubscriber(modid = FactionControlMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FactionPlayerData {
    public static final String NBT_ROOT = "faction_control";
    public static final String NBT_PLAY_AS_PLAYER = "play_as_player";
    public static final String NBT_CAN_CREATE_FACTION = "can_create_faction";

    private FactionPlayerData() {
    }

    public static boolean getFlag(Player player, String key) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(NBT_ROOT)) {
            return false;
        }
        return root.getCompound(NBT_ROOT).getBoolean(key);
    }

    public static void setFlag(Player player, String key, boolean value) {
        CompoundTag root = player.getPersistentData();
        CompoundTag data = root.contains(NBT_ROOT) ? root.getCompound(NBT_ROOT) : new CompoundTag();
        data.putBoolean(key, value);
        root.put(NBT_ROOT, data);
    }

    public static boolean toggleFlag(Player player, String key) {
        boolean next = !getFlag(player, key);
        setFlag(player, key, next);
        return next;
    }

    /**
     * Forge recreates the player entity on death; copy our compound so unlocks and play-mode survive.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        original.reviveCaps();
        try {
            CompoundTag source = original.getPersistentData();
            if (!source.contains(NBT_ROOT)) {
                return;
            }
            clone.getPersistentData().put(NBT_ROOT, source.getCompound(NBT_ROOT).copy());
        } finally {
            original.invalidateCaps();
        }
    }

    public static boolean getCanCreateFaction(ServerPlayer player) {
        return getFlag(player, NBT_CAN_CREATE_FACTION);
    }

    public static void setCanCreateFaction(ServerPlayer player, boolean allowed) {
        setFlag(player, NBT_CAN_CREATE_FACTION, allowed);
    }

    public static boolean toggleCanCreateFaction(ServerPlayer player) {
        return toggleFlag(player, NBT_CAN_CREATE_FACTION);
    }
}
