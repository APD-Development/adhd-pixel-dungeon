package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
/** Player HP recovery only. Call once on the source's requested heal, before its final HP cap. */
public final class CustomHealingHooks {
    private CustomHealingHooks() { }
    public static boolean disabled(){return CustomCombatHooks.percentage(CustomDungeonRules.Slider.HEALING)==0;}
    public static int display(Char target,int before,int original) {
        return target instanceof Hero && CustomCombatHooks.percentage(CustomDungeonRules.Slider.HEALING)!=100
            ? Math.max(0,target.HP-before) : original;
    }
    public static int fullHealth(Char target) {
        if(!(target instanceof Hero) || CustomCombatHooks.percentage(CustomDungeonRules.Slider.HEALING)==100)return target.HT;
        return (int)Math.min(target.HT,(long)target.HP+amount(target,target.HT));
    }
    /** Read-only HP gain preview; never spends the saved fractional healing credit. */
    public static int preview(Hero hero,int requested) {
        if(requested<=0 || hero.HP>=hero.HT)return 0;
        int percent=CustomCombatHooks.percentage(CustomDungeonRules.Slider.HEALING);
        if(percent==0)return 0;
        long whole=percent==100?requested:(long)Math.floor((double)requested*percent/100.0+hero.apdCustomHealingFraction);
        return (int)Math.max(0,Math.min(Integer.MAX_VALUE,Math.min((long)hero.HT-hero.HP,whole)));
    }
    public static int amount(Char target,int requested) {
        if(!(target instanceof Hero))return requested;
        int percent=CustomCombatHooks.percentage(CustomDungeonRules.Slider.HEALING);
        if(percent==100 || requested<=0)return requested;
        Hero hero=(Hero)target;
        if(percent==0 || hero.HP>=hero.HT){hero.apdCustomHealingFraction=0;return 0;}
        double scaled=(double)requested*percent/100.0+hero.apdCustomHealingFraction;
        long whole=(long)Math.floor(scaled);
        long room=(long)hero.HT-hero.HP;
        // Do not bank overhealing for a future injury. Quarter-step fractions need no extra RNG.
        hero.apdCustomHealingFraction=whole>=room?0:(float)(scaled-whole);
        return (int)Math.max(0,Math.min(Integer.MAX_VALUE,Math.min(room,whole)));
    }
}
