package com.factioncontrol.faction;

public enum FlagState {
    ACTIVE,
    RAIDED;

    public static FlagState fromName(String name) {
        for (FlagState state : values()) {
            if (state.name().equalsIgnoreCase(name)) {
                return state;
            }
        }
        return ACTIVE;
    }
}
