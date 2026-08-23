package com.factioncontrol.compat.create;

import org.jetbrains.annotations.Nullable;

/**
 * Reflection helpers for Create classes (optional mod).
 */
public final class CreateReflection {
    private static final String CONTRAPTION_ENTITY = "com.simibubi.create.content.contraptions.AbstractContraptionEntity";
    @Nullable
    private static Class<?> contraptionEntityClass;

    private CreateReflection() {
    }

    @Nullable
    public static Class<?> contraptionEntityClass() {
        if (contraptionEntityClass != null) {
            return contraptionEntityClass;
        }
        try {
            contraptionEntityClass = Class.forName(CONTRAPTION_ENTITY);
            return contraptionEntityClass;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static boolean isContraptionEntity(Object entity) {
        Class<?> clazz = contraptionEntityClass();
        return clazz != null && clazz.isInstance(entity);
    }

    public static boolean isCreateFakePlayer(Object player) {
        if (player == null) {
            return false;
        }
        String name = player.getClass().getName();
        return name.contains("FakePlayer") && name.contains("create");
    }
}
