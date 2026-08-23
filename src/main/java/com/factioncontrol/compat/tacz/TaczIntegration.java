package com.factioncontrol.compat.tacz;

import com.factioncontrol.util.FactionChat;
import com.factioncontrol.util.TerritoryProtectionHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Blocks TaCZ gun fire inside admin safezones via reflection (no compile-time TaCZ dependency).
 */
public final class TaczIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GUN_FIRE_EVENT = "com.tacz.guns.api.event.common.GunFireEvent";

    private TaczIntegration() {
    }

    public static boolean register() {
        try {
            Class<?> eventClass = Class.forName(GUN_FIRE_EVENT);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, false, (Event event) -> {
                if (eventClass.isInstance(event)) {
                    handleGunFire(event);
                }
            });
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.warn("[Faction Control] Classe GunFireEvent nao encontrada no classpath TaCZ.");
            return false;
        }
    }

    private static void handleGunFire(Object event) {
        try {
            Method isCanceled = event.getClass().getMethod("isCanceled");
            if ((boolean) isCanceled.invoke(event)) {
                return;
            }

            ServerPlayer shooter = resolveShooter(event);
            if (shooter == null) {
                return;
            }

            if (shooter.level().dimension() != Level.OVERWORLD) {
                return;
            }

            if (!TerritoryProtectionHelper.isAdminChunk(shooter.chunkPosition())
                    && !isTargetInAdminChunk(event, shooter)) {
                return;
            }

            Method setCanceled = event.getClass().getMethod("setCanceled", boolean.class);
            setCanceled.invoke(event, true);
            FactionChat.sendSafezonePvpDeniedActionBar(shooter);
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("[Faction Control] Falha ao processar GunFireEvent TaCZ", e);
        }
    }

    @Nullable
    private static ServerPlayer resolveShooter(Object event) {
        for (String methodName : new String[]{"getShooter", "getEntity", "getGunOperator"}) {
            try {
                Method method = event.getClass().getMethod(methodName);
                Object shooterObj = method.invoke(event);
                if (shooterObj instanceof ServerPlayer serverPlayer) {
                    return serverPlayer;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static boolean isTargetInAdminChunk(Object event, ServerPlayer shooter) {
        for (String methodName : new String[]{"getTarget", "getHitEntity"}) {
            try {
                Method method = event.getClass().getMethod(methodName);
                Object targetObj = method.invoke(event);
                if (targetObj instanceof Player targetPlayer) {
                    if (targetPlayer.getUUID().equals(shooter.getUUID())) {
                        continue;
                    }
                    return TerritoryProtectionHelper.isAdminChunk(targetPlayer.chunkPosition());
                }
                if (targetObj instanceof Entity entity) {
                    if (entity.getUUID().equals(shooter.getUUID())) {
                        continue;
                    }
                    return TerritoryProtectionHelper.isAdminChunk(new ChunkPos(entity.blockPosition()));
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return false;
    }
}
