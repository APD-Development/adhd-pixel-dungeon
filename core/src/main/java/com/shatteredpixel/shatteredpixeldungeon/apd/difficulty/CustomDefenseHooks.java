package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
/** Scales an already-rolled DR once. Shields, source resistances and invulnerability remain separate. */
public final class CustomDefenseHooks {
    private CustomDefenseHooks() { }
    public static int scale(Char target,int roll) {
        int percent;
        if(target instanceof Hero) percent=CustomCombatHooks.percentage(CustomDungeonRules.Slider.PLAYER_DEFENSE);
        else if(target!=null && target.alignment==Char.Alignment.ENEMY)percent=CustomCombatHooks.percentage(CustomDungeonRules.Slider.ENEMY_DEFENSE);
        else return roll;
        if(percent==100)return roll;
        return (int)Math.max(0,Math.min(Integer.MAX_VALUE,Math.round((double)roll*percent/100.0)));
    }
}
