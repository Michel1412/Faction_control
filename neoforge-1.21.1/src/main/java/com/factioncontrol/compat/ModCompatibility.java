package com.factioncontrol.compat;

import com.factioncontrol.compat.create.CreateIntegration;
import com.factioncontrol.compat.tacz.TaczIntegration;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Registers optional mod integrations when the corresponding mods are present.
 */
public final class ModCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;

    private ModCompatibility() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (ModList.get().isLoaded("tacz")) {
            if (TaczIntegration.register()) {
                LOGGER.info("[Faction Control] Integracao TaCZ registrada.");
            } else {
                LOGGER.warn("[Faction Control] TaCZ detectado, mas GunFireEvent nao foi registrado.");
            }
        }

        if (ModList.get().isLoaded("create")) {
            CreateIntegration.register();
            LOGGER.info("[Faction Control] Integracao Create registrada.");
        }
    }
}
