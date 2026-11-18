package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;

// One explicit early-game player profile, not a claim to model every human player.
// Selectors inspect only known inventory properties and never execute actions or roll RNG.
public final class NormalWarriorPreparation {
    public static final String PROFILE = "warrior-normal-loot-critical-heal-v1";
    private NormalWarriorPreparation() {}
    private static final Talent[] PRIORITY = {Talent.IRON_WILL, Talent.HEARTY_MEAL,
            Talent.VETERANS_INTUITION, Talent.PROVOKED_ANGER, Talent.IRON_STOMACH,
            Talent.LIQUID_WILLPOWER, Talent.LETHAL_MOMENTUM, Talent.RUNIC_TRANSFERENCE,
            Talent.IMPROVISED_PROJECTILES};
    public static Talent talent(Hero hero) {
        for (Talent talent : PRIORITY) for (int i=0; i<Math.min(2, hero.talents.size()); i++)
            if (hero.talentPointsAvailable(i+1)>0 && hero.talents.get(i).containsKey(talent)
                    && hero.pointsInTalent(talent)<talent.maxPoints()) return talent;
        return null;
    }
    public static MeleeWeapon weapon(Hero hero) {
        if (hero.belongings.weapon != null && hero.belongings.weapon.cursed) return null;
        MeleeWeapon best = null;
        float current = hero.belongings.weapon instanceof MeleeWeapon
                ? score((MeleeWeapon)hero.belongings.weapon) : 0;
        for (MeleeWeapon weapon : hero.belongings.getAllItems(MeleeWeapon.class))
            if (!weapon.isEquipped(hero) && weapon.isIdentified() && !weapon.cursed
                    && weapon.STRReq()<=hero.STR() && score(weapon)>current) {
                best=weapon; current=score(weapon);
            }
        return best;
    }
    private static float score(MeleeWeapon weapon) {
        int level=weapon.levelKnown ? weapon.level() : 0;
        return weapon.augment.damageFactor((weapon.min(level)+weapon.max(level))/2f)
                / weapon.augment.delayFactor(weapon.DLY);
    }
    public static Armor armor(Hero hero) {
        Armor current=hero.belongings.armor;
        if (current != null && current.cursed) return null;
        int score=current==null ? 0 : current.DRMin(current.levelKnown ? current.level() : 0)
                +current.DRMax(current.levelKnown ? current.level() : 0);
        Armor best=null;
        for (Armor armor : hero.belongings.getAllItems(Armor.class))
            if (!armor.isEquipped(hero) && armor.isIdentified() && !armor.cursed
                    && armor.STRReq()<=hero.STR() && armor.DRMin()+armor.DRMax()>score) {
                best=armor; score=armor.DRMin()+armor.DRMax();
            }
        return best;
    }
    public static MissileWeapon throwable(Hero hero) {
        for (MissileWeapon item : hero.belongings.getAllItems(MissileWeapon.class))
            if (item.isIdentified() && !item.cursed && item.STRReq()<=hero.STR()) return item;
        return null;
    }
}
