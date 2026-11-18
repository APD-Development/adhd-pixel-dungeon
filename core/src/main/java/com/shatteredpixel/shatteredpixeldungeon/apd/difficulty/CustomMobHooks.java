package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
/** Apply the selected enemy or boss health once, including later summons, without scaling allies. */
public final class CustomMobHooks {
    private CustomMobHooks() { }
    public static void applyHealth(Mob mob) {
        if(mob.apdCustomHealthApplied || mob.alignment!=Char.Alignment.ENEMY) return;
        int p=CustomCombatHooks.percentage(Char.hasProp(mob,Char.Property.BOSS)
            ? CustomDungeonRules.Slider.BOSS_HP : CustomDungeonRules.Slider.ENEMY_HP);
        if(p==100)return;
        mob.HT=(int)Math.max(1,Math.min(Integer.MAX_VALUE,Math.round((double)mob.HT*p/100.0)));
        mob.HP=(int)Math.max(0,Math.min(mob.HT,Math.round((double)mob.HP*p/100.0)));
        mob.apdCustomHealthApplied=true;
    }
}
