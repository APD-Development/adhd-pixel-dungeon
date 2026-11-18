package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundle;

/** Immutable ownership snapshot for damage that outlives its attacker. Not a damage-source replacement. */
public enum OffensiveDamageOrigin {
    UNATTRIBUTED, PLAYER, ENEMY, BOSS;

    /** A summon explicitly inherits its creator's origin; friendliness alone does not prove ownership. */
    public static OffensiveDamageOrigin capture(Char attacker, OffensiveDamageOrigin creator) {
        if(creator==null){
            OffensiveDamageOrigin captured=OffensiveInvocation.captured(attacker);
            if(captured!=null)return captured;
            if(attacker!=null)creator=attacker.apdCreatorOffensiveOrigin();
        }
        if (creator != null && creator != UNATTRIBUTED) return creator;
        if (attacker instanceof Hero) return PLAYER;
        if (attacker == null || attacker.alignment != Char.Alignment.ENEMY) return UNATTRIBUTED;
        return Char.hasProp(attacker, Char.Property.BOSS) ? BOSS : ENEMY;
    }

    public int percentage() {
        switch (this) {
            case PLAYER: return CustomCombatHooks.percentage(CustomDungeonRules.Slider.PLAYER_DAMAGE);
            case ENEMY: return CustomCombatHooks.percentage(CustomDungeonRules.Slider.ENEMY_DAMAGE);
            case BOSS: return CustomCombatHooks.percentage(CustomDungeonRules.Slider.BOSS_DAMAGE);
            default: return 100;
        }
    }

    /** Additive metadata: old saves never become invented player/boss ownership. */
    public void store(Bundle bundle, String key) {
        if (this != UNATTRIBUTED) bundle.put(key, name());
        else bundle.remove(key);
    }

    public static OffensiveDamageOrigin restore(Bundle bundle, String key) {
        if (!bundle.contains(key)) return UNATTRIBUTED;
        try { return valueOf(bundle.getString(key)); }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported offensive damage origin; preserve this save for recovery", ex);
        }
    }
}
