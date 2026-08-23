package com.factioncontrol.client;

/**
 * Reference palette for faction upgrade and raid controller pixel-art textures.
 * Textures are authored in full color; {@link ClientModEvents} uses {@code NO_TINT}.
 */
public final class ItemTextureColors {
    /** Industrial PCB green ({@code #1B5E3B}). */
    public static final int FACTION_UPGRADE = 0x1B5E3B;
    /** Military pager dark gray ({@code #1E1E1E}). */
    public static final int RAID_CONTROLLER = 0x1E1E1E;

    private ItemTextureColors() {
    }
}
