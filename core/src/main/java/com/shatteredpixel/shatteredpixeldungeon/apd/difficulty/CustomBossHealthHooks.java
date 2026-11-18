package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
/** Retain vanilla boss phase fractions when their original thresholds use fixed HP amounts. */
public final class CustomBossHealthHooks {
    private CustomBossHealthHooks() { }
    public static int threshold(int original) {
        int p=CustomCombatHooks.percentage(CustomDungeonRules.Slider.BOSS_HP);
        if(p==100 || original<=0)return original;
        return (int)Math.max(1,Math.min(Integer.MAX_VALUE,Math.round((double)original*p/100.0)));
    }
}
