package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.watabou.utils.Bundle;
import java.util.Arrays;

/** Versioned immutable run configuration. Bounds are provisional alpha specifications. */
public final class CustomDungeonRules {
    public static final int VERSION = 1;
    public enum Slider {
        ENEMY_HP(25,500), ENEMY_DAMAGE(25,500), ENEMY_DEFENSE(0,500),
        ENEMY_ACCURACY(25,500), ENEMY_EVASION(0,500), ENEMY_SPAWN(25,300),
        BOSS_HP(25,500), BOSS_DAMAGE(25,500), PLAYER_HP(25,500),
        PLAYER_DAMAGE(25,500), PLAYER_ACCURACY(25,500), PLAYER_EVASION(0,500),
        PLAYER_DEFENSE(0,500), FOOD(25,200), HUNGER(25,400), HEALING(0,200),
        EQUIPMENT(25,200), UPGRADE_SCROLLS(1,60);
        public final int min, max;
        Slider(int min, int max) { this.min=min; this.max=max; }
        public int step() { return this == UPGRADE_SCROLLS ? 1 : 25; }
        public int defaultValue() { return this == UPGRADE_SCROLLS ? 15 : 100; }
        public boolean valid(int value) {
            return value >= min && value <= max && (value-min)%step()==0;
        }
    }
    private final int[] values;
    public CustomDungeonRules(int[] values) {
        if (values == null || values.length != Slider.values().length)
            throw new IllegalArgumentException("Incomplete custom rules");
        this.values=values.clone();
        for (Slider s : Slider.values()) if (!s.valid(this.values[s.ordinal()]))
            throw new IllegalArgumentException("Invalid custom rule: " + s.name());
    }
    public static CustomDungeonRules defaults() {
        int[] values=new int[Slider.values().length];
        for (Slider s : Slider.values()) values[s.ordinal()]=s.defaultValue();
        return new CustomDungeonRules(values);
    }
    public int get(Slider slider) { return values[slider.ordinal()]; }
    public int[] values() { return values.clone(); }
    public double percentage(Slider slider) {
        return slider == Slider.UPGRADE_SCROLLS ? get(slider)*100.0/15 : get(slider);
    }
    public boolean isNeutral() { return Arrays.equals(values, defaults().values); }
    public CustomDungeonRules with(Slider slider, int value) {
        int[] copy=values(); copy[slider.ordinal()]=value; return new CustomDungeonRules(copy);
    }
    public String preferenceText() {
        StringBuilder text=new StringBuilder(Integer.toString(VERSION));
        for (int value : values) text.append(',').append(value);
        return text.toString();
    }
    public static CustomDungeonRules fromPreferenceText(String text) {
        String[] parts=text.split(",",-1);
        if (parts.length!=Slider.values().length+1 || !parts[0].equals(Integer.toString(VERSION)))
            throw new IllegalArgumentException("Unsupported or incomplete custom preferences");
        int[] values=new int[Slider.values().length];
        for (int i=0;i<values.length;i++) values[i]=Integer.parseInt(parts[i+1]);
        return new CustomDungeonRules(values);
    }
    public String identityText() {
        StringBuilder out=new StringBuilder("custom-v"+VERSION);
        for (Slider s : Slider.values()) out.append(';').append(s.name()).append('=').append(get(s));
        return out.toString();
    }
    public void store(Bundle bundle) {
        bundle.put("custom_rules_version", VERSION);
        bundle.put("custom_rules_values", values());
    }
    /** Missing historical settings remain unknown. Invalid/future records are never replaced with defaults. */
    public static CustomDungeonRules restore(Bundle bundle) {
        if (!bundle.contains("custom_rules_version") && !bundle.contains("custom_rules_values")) return null;
        if (!bundle.contains("custom_rules_values") || !bundle.contains("custom_rules_version"))
            throw new IllegalArgumentException("Incomplete custom rules record");
        if (bundle.getInt("custom_rules_version") != VERSION)
            throw new IllegalArgumentException("Unsupported custom rules version");
        return new CustomDungeonRules(bundle.getIntArray("custom_rules_values"));
    }
}
