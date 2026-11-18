/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.shatteredpixel.shatteredpixeldungeon.apd.mechanics;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SnipersMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfFuror;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.RingItemEffects;

/** Does not call damageRoll, canKO, attackSkill, attackDelay or resolve a marked enemy. */
public final class PreparedShotExplanation {
    private PreparedShotExplanation(){}
    private static String n(float x){return ObfuscationExplanation.number(x);}
    public static String preparation(Preparation preparation){
        if(Dungeon.hero==null)return "";
        Preparation.AttackLevel level=preparation.currentAttackLevel();
        String result="_Prepared attack calculation_\nRecorded invisible preparation updates: "+preparation.preparedUpdates()+". Starting tier is one; higher tiers unlock at "+Preparation.AttackLevel.LVL_2.minimumUpdates()+", "+Preparation.AttackLevel.LVL_3.minimumUpdates()+" and "+Preparation.AttackLevel.LVL_4.minimumUpdates()+" updates. Current tier: "+(level.ordinal()+1)+". These are game updates, not real seconds on the phone.\nFor the current tier, roll the attack's damage "+level.damageRollCount()+" time(s), keep the greatest result, multiply by 1 + damage bonus = "+n(1+level.damageBonusFraction())+", then round to a whole damage point. Taking the best of several rolls is not the same as multiplying the maximum possible damage or averaging the rolls. The actual rolls can consume ready attack benefits normally; viewing this panel never rolls them.\nAn ordinary eligible target can be executed only when current HP / maximum HP is strictly below "+n(100*level.KOThreshold())+"%; at equality it is not below the threshold. Bosses and minibosses use one-fifth of that threshold: "+n(100*level.KOThreshold()/5f)+"%. These are health-fraction thresholds, not per-hit kill probabilities. Normal hit, invulnerability and other eligibility rules still apply. No enemy's hidden HP is queried here.\nCurrent blink range: "+level.blinkDistance()+" tiles, using current Assassin's Reach rank. It still requires a valid visible target, legal path and suitable landing cell. Preparing does not grant movement through walls or guarantee that a chosen target is reachable.";
        return result;
    }
    public static String sniper(SnipersMark mark){
        String result="_Marked special shot_\nThe bow's normal rolled damage is first multiplied by 1 + the Shared Upgrades contribution recorded when this mark was created, then rounded. ";
        if(mark.bonusIsKnown())result+="Known recorded contribution: "+n(100*mark.percentDmgBonus)+"%; first factor "+n(1+mark.percentDmgBonus)+". ";
        else result+="That upgrade-derived contribution is withheld when its source knowledge was not recorded, so an unidentified weapon or ring is not exposed. ";
        result+="The selected bow mode applies a separate step: snapshot rounds damage ×0.667; each of a three-arrow volley rounds damage ×0.5; a damage-augmented shot rounds damage × min(3, 1.2 × 1.125 raised to (tile distance − 1)). This last factor is capped at3, not an unlimited bonus per tile. Each step rounds separately, before later target defenses. No hidden marked-target position or future damage roll is queried for this inspector.\nSnapshot base firing time is zero; volley base time is one for the sequence; damage-augmented base time is two game-time units. The resulting normal delay divides that base time by the bow's attack-speed factor. Free-action and time-stopping effects resolve separately; they are not simulated by inspection.";
        Hero hero=Dungeon.hero;
        if(hero!=null){Integer furor=RingItemEffects.knownBonus(hero,RingOfFuror.class,true);if(furor!=null){float speed=RingOfFuror.attackSpeedMultiplier(furor);if(hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar.SwordDance.class)!=null)speed+=.6f;if(hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower.naturesPowerTracker.class)!=null)speed+=(8+hero.pointsInTalent(Talent.GROWING_POWER))/24f;result+=" Current known attack-speed factor: "+n(speed)+". Normal volley delay: "+n(1/speed)+"; damage-augmented delay: "+n(2/speed)+" game-time units before separate free-action effects.";}else result+=" Combined current firing delays are withheld while contributing ring bonuses are unidentified or unavailable.";}
        return result;
    }
}
