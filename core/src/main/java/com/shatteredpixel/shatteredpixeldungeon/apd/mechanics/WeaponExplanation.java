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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.RingItemEffects;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfFuror;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar;

/** Standard timing and passive blocking; never calls consuming combat helpers or damage rolls. */
public final class WeaponExplanation {
    private WeaponExplanation() {}

    public static Float normalAttackDelay(Weapon weapon, Hero hero) {
        if (!weapon.levelKnown || weapon instanceof SpiritBow || weapon instanceof SpiritBow.SpiritArrow) return null;
        Integer strength = ArmorExplanation.knownStrength(hero);
        Integer furor = RingItemEffects.knownBonus(hero, RingOfFuror.class, true);
        if (strength == null || furor == null) return null;
        float base = weapon.augment.delayFactor(weapon.DLY);
        int missing = Math.max(0, weapon.STRReq() - strength);
        if (missing > 0) base *= Math.pow(1.2, missing);
        float speed = RingOfFuror.attackSpeedMultiplier(furor);
        if (hero.buff(Scimitar.SwordDance.class) != null) speed += .6f;
        return base * (1f / speed);
    }

    public static boolean passiveBlockWeapon(Weapon weapon){
        return weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier
            ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff
            ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana
            ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield
            ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield
            ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Tanto
            ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Wakizashi
            ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Tachi
            ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Nodachi
            ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Buckler
            ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Targe
            ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.KiteShield;
    }
    public static String blockSummary(Weapon weapon,Hero hero){
        if(!passiveBlockWeapon(weapon))return "";
        String upgrades=weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield
                ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield
                ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana
                ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Tanto
                ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Wakizashi
                ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Tachi
                ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Nodachi
                ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Buckler
                ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Targe
                ||weapon instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.KiteShield?" Upgrades improve this protection.":"";
        if(!weapon.levelKnown)return "Can block incoming attack damage while wielded normally."+upgrades;
        Integer strength=ArmorExplanation.knownStrength(hero);
        if(strength==null)return "Can block incoming attack damage while wielded normally."+upgrades;
        int maximum=weapon.defenseFactor(hero),missing=Math.max(0,weapon.STRReq()-strength);
        return "Blocks _0–"+Math.max(0,maximum-2*missing)+" damage_ while wielded normally."+upgrades;
    }
    public static String blockDetails(Weapon weapon,Hero hero){
        if(!passiveBlockWeapon(weapon))return "";
        String text="_Passive weapon protection_\nFor a positive block maximum, the normal wielded-weapon defense path rolls from 0 to that maximum, favoring middle results, subtracts 2 for each missing strength point and keeps only a positive contribution. This is separate from worn armor's roll, active Guard or other ability protection. Unarmed fighting rules can disable the weapon's contribution even while it is equipped. Total-defense modifiers still apply afterward.\nRapier's maximum is 1 and Quarterstaff's 2 damage points; Katana's is 3 + half its effective weapon level, rounded down. Round Shield uses 4 + effective weapon level; Greatshield uses 6 + 2×effective weapon level. Their upgrades and temporary effective-level changes affect those shield maxima under the item rules; strength requirements use their own level rule.\n";
        if(!weapon.levelKnown)return text+"This item's upgrade-dependent values stay hidden until identified.";
        int maximum=weapon.defenseFactor(hero);text+="Current positive passive block maximum: "+Math.max(0,maximum)+" points before missing-strength penalties. A non-positive formula result gives no positive passive contribution. ";Integer strength=ArmorExplanation.knownStrength(hero);
        if(strength==null)return text+"Personal penalties are withheld while contributing strength bonuses are unknown or unavailable.";
        int missing=Math.max(0,weapon.STRReq()-strength);
        return text+"Known strength: "+strength+"; missing strength: "+missing+"; subtract "+(2*missing)+" points from each roll, with a zero lower bound. Current normal weapon contribution: 0–"+Math.max(0,maximum-2*missing)+" protection points.";
    }

    public static String summary(Weapon weapon, Hero hero) {
        return com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspect(()->summaryInInspection(weapon,hero));
    }
    private static String summaryInInspection(Weapon weapon, Hero hero) {
        Float delay = normalAttackDelay(weapon, hero);
        String time = delay == null ? "" : com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat("0.##",delay);
        String result = delay == null ? "" : "Attack time: _" + time + (time.equals("1") ? " turn" : " turns") + " per attack_.";
        String block=blockSummary(weapon,hero);if(!block.isEmpty())result+=(result.isEmpty()?"":" ")+block;
        return result;
    }

    public static String details(Weapon weapon, Hero hero) {
        return com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspect(()->detailsInInspection(weapon,hero));
    }
    private static String detailsInInspection(Weapon weapon, Hero hero) {
        String result = "_Weapon damage, accuracy and time_\nThe displayed damage range describes a damage roll, not guaranteed HP lost. The item description states its upgrade and augmentation effects; excess strength, talents, rings, enchantments, abilities and the target's defenses can change the result. Do not treat the range as a flat percentage.\n"
                + "In the standard weapon calculation, each missing strength point divides the weapon's accuracy factor by 1.5 and multiplies its attack time by 1.2. Upgrades reduce strength requirements at +1, +3, +6, +10 and so on; negative levels count as zero for this reduction. Mastery reduces the requirement by 2. Temporary effective levels and permanent upgrades need not affect the requirement in the same way.\n"
                + "Speed augmentation multiplies rolled damage by 0.7 and base attack time by 2/3. Damage augmentation multiplies rolled damage by 1.5 and base attack time by 5/3. Damage rounds to whole points. These are separate factors: faster does not mean a matching percentage reduction in damage.\n"
                + "Standard attack time = augmented base time × 1.2^missing strength / attack-speed factor. Furor contributes 1.09051^effective ring bonus to that factor. Sword Dance adds 0.6 to the factor before division; it does not multiply final time by 0.6. Other all-action effects, free attacks, unarmed stances and special abilities apply their own rules.\n";
        String block=blockDetails(weapon,hero);if(!block.isEmpty())result=block+"\n\n"+result;
        if (weapon instanceof SpiritBow || weapon instanceof SpiritBow.SpiritArrow)
            return result + "Spirit Bow shots also have special Sniper-shot timing and Nature's Power bonuses; the standard timing formula alone is not a complete shot preview.";
        if (!weapon.levelKnown) return result + "Personal upgrade-dependent timing stays hidden until this weapon's upgrades are identified.";
        Float delay = normalAttackDelay(weapon, hero);
        if (delay == null) return result + "Personal attack timing is withheld while contributing ring bonuses are unknown or unavailable.";
        int missing = Math.max(0, weapon.STRReq() - ArmorExplanation.knownStrength(hero));
        return result + "This weapon's base attack time: " + ObfuscationExplanation.number(weapon.DLY)
                + " turns; after augmentation: " + ObfuscationExplanation.number(weapon.augment.delayFactor(weapon.DLY))
                + " turns; missing strength: " + missing + " points. Normal weapon attack time with your known bonuses: "
                + ObfuscationExplanation.number(delay) + " turns before the other exceptions above. This does not consume a waiting free attack or ability.";
    }
}
