package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.replay.ReplayContext;

/** Custom-only factors. Difficulty 1–5 and existing status/challenge factors remain independent. */
public final class CustomCombatHooks {
    private CustomCombatHooks() { }
    public static int percentage(CustomDungeonRules.Slider slider) {
        // Existing floor replays do not yet carry custom rules. Never borrow the live hero's snapshot.
        if (ReplayContext.active()) return slider.defaultValue();
        CustomDungeonRules rules=Dungeon.customRules();
        return rules==null ? slider.defaultValue() : rules.get(slider);
    }
    public static float accuracy(Char actor,float skill) {
        if (skill>=Char.INFINITE_ACCURACY) return skill;
        if (actor instanceof Hero) return scale(skill,percentage(CustomDungeonRules.Slider.PLAYER_ACCURACY));
        if (actor!=null && actor.alignment==Char.Alignment.ENEMY)
            return scale(skill,percentage(CustomDungeonRules.Slider.ENEMY_ACCURACY));
        return skill;
    }
    public static float evasion(Char actor,float skill) {
        if (skill>=Char.INFINITE_EVASION) return skill;
        if (actor instanceof Hero) return scale(skill,percentage(CustomDungeonRules.Slider.PLAYER_EVASION));
        if (actor!=null && actor.alignment==Char.Alignment.ENEMY)
            return scale(skill,percentage(CustomDungeonRules.Slider.ENEMY_EVASION));
        return skill;
    }
    private static float scale(float value,int percentage) {
        return percentage==100 ? value : (float)((double)value*percentage/100.0);
    }
    public static int playerMaximumHp(int value) {
        int percent=percentage(CustomDungeonRules.Slider.PLAYER_HP);
        if (percent==100) return value;
        long scaled=Math.round((double)value*percent/100.0);
        return (int)Math.max(1,Math.min(Integer.MAX_VALUE,scaled));
    }
}
