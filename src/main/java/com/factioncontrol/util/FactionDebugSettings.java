package com.factioncontrol.util;

/**
 * In-memory debug toggles (not persisted across server restarts).
 */
public final class FactionDebugSettings {
    private static volatile boolean clickLoggingEnabled;

    private FactionDebugSettings() {
    }

    public static boolean isClickLoggingEnabled() {
        return clickLoggingEnabled;
    }

    public static boolean setClickLoggingEnabled(boolean enabled) {
        clickLoggingEnabled = enabled;
        return enabled;
    }

    public static String describeClickLogging() {
        return clickLoggingEnabled ? "ATIVADOS" : "DESATIVADOS";
    }
}
