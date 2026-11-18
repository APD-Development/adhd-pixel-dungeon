package com.shatteredpixel.shatteredpixeldungeon.apd.settings;

/** Stable navigation categories; contains no preferences and changes no defaults. */
public enum ApdSettingsSection {
    GAMEPLAY("Gameplay", "Game speed, healing confirmations and humor."),
    RULES("Difficulty & Rules", "Healing-to-Shielding chance and Custom Dungeon Rules for new runs; inspect current rules."),
    HOTBAR("Hotbar & Controls", "Hotbar layouts, starting Waterskin position and Cleric quick-casts."),
    VISIBILITY("Visibility & Navigation", "Tile visibility, damage-over-time details and stair shortcuts."),
    HISTORY("Statistics & History", "UIMSD, recording, imports and history backups."),
    ADVANCED("Advanced", "Recording diagnostics, storage, self-tests and available developer tools.");
    public final String title,description;
    ApdSettingsSection(String title,String description){this.title=title;this.description=description;}
}
