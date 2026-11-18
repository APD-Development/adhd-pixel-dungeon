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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.RingItemEffects;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfFuror;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;

/** Weapon ability explanations; never resolves targets, spends charges or rolls an attack. */
public final class WeaponAbilityExplanation {
    private WeaponAbilityExplanation(){}
    private static String n(float value){return ObfuscationExplanation.number(value);}
    private static boolean known(MeleeWeapon weapon,Hero hero){
        if(!weapon.levelKnown||!weapon.cursedKnown)return false;
        if(hero==null)return true;
        if(RingItemEffects.knownBonus(hero,RingOfFuror.class,true)==null)return false;
        if(hero.hasTalent(Talent.TWIN_UPGRADES))for(KindOfWeapon held:new KindOfWeapon[]{hero.belongings.weapon(),hero.belongings.secondWep()})if(held!=null&&!held.levelKnown)return false;
        return true;
    }
    public static String details(MeleeWeapon weapon,Hero hero){
        String type=weapon.getClass().getSimpleName(),effect,formula="";
        int base=0;float scale=0;boolean rounded=false;
        switch(type){
            case "WornShortsword":base=3;scale=1;break;
            case "Shortsword":base=4;scale=1;break;
            case "Sword":base=5;scale=1;break;
            case "Longsword":base=6;scale=1;break;
            case "Greatsword":base=7;scale=1;break;
            case "Cudgel":base=3;scale=1.5f;rounded=true;break;
            case "HandAxe":base=4;scale=1.5f;rounded=true;break;
            case "Mace":case "BattleAxe":base=5;scale=1.5f;rounded=true;break;
            case "WarHammer":base=6;scale=1.5f;rounded=true;break;
            case "Spear":base=9;scale=2;rounded=true;break;
            case "Glaive":base=12;scale=2.5f;rounded=true;break;
            case "Rapier":base=5;scale=1.5f;rounded=true;break;
            case "Katana":base=8;scale=2;rounded=true;break;
            case "Tanto":base=2;scale=0.5f;rounded=true;break;
            case "Wakizashi":base=4;scale=1;rounded=true;break;
            case "Tachi":base=6;scale=1.5f;rounded=true;break;
            case "Nodachi":base=10;scale=2.5f;rounded=true;break;
            case "Gloves":base=3;scale=1;break;
            case "Sai":base=4;scale=1;break;
            case "Gauntlet":base=5;scale=1;break;
            case "Greataxe":base=15;scale=2;break;
            case "Flail":base=8;scale=2;break;
            case "Pickaxe":base=8;scale=2;break;
        }
        boolean visible=known(weapon,hero);Integer level=visible?weapon.buffedLvl():null;
        if(base!=0){
            formula="Bonus input: "+base+" + "+(rounded?"round(":"")+n(scale)+" × effective weapon level"+(rounded?")":"")+"; apply the weapon's damage augmentation to that bonus separately.";
            if(level!=null){int input=base+(rounded?Math.round(scale*level):(int)(scale*level));formula+=" Current effective level: "+level+"; resulting added amount: "+weapon.augment.damageFactor(input)+" damage points.";}
            else formula+=" This item's upgrade-dependent result is withheld while contributing levels or ring effects are unknown.";
        }
        switch(type){
            case "WornShortsword":case "Shortsword":case "Sword":case "Longsword":case "Greatsword":
                effect="Cleave adds its bonus to a normal attack input and uses guaranteed-hit accuracy in the ordinary contest. A kill takes no ordinary attack time and alternates the Cleave benefit: an existing benefit is consumed, otherwise a four-unit scheduled benefit is readied. That benefit makes the next supported Cleave charge-free. A surviving target costs normal attack time and clears that benefit. Special dodges and defenses still apply.";break;
            case "Cudgel":case "HandAxe":case "Mace":case "BattleAxe":case "WarHammer":
                effect="Heavy Blow adds its bonus only when the monster is surprised by the hero; against an unsurprised monster the added bonus is zero. A successful hit on a survivor offers Daze with its normal duration and resistance rules. The attack uses guaranteed-hit accuracy in the ordinary contest and normal attack time; it is not guaranteed final damage or a bonus on every ordinary attack.";break;
            case "Spear":case "Glaive":
                effect="Spike requires a nonadjacent visible target in legal attack reach. It adds its bonus and uses guaranteed-hit accuracy in the ordinary contest. On a successful hit, an eligible surviving target that has not already moved can be pushed with force 1 along the existing projectile path. Collision and immovability can limit movement; normal attack time and defenses remain.";break;
            case "Rapier":case "Katana":case "Tanto":case "Wakizashi":case "Tachi":case "Nodachi":
                effect="Lunge requires an unrooted hero, a target at least two tiles away and a legal landing cell within the ability's reach conditions. It moves toward the target before its supported attack, adding its damage bonus with normal defenses and attack time. If movement completes without a valid attack, the existing branch still spends one charge and movement time of 1 divided by movement speed; it does not process a completed weapon ability or grant a free hit through an obstacle.";break;
            case "Gloves":case "Sai":case "Gauntlet":
                effect="Combo Strike multiplies its added bonus by the recorded recent-hit count. Its normal attack damage factor remains 1; the added amount is not a multiplier of all final damage. The attempt consumes the recorded hit benefit and uses normal attack time. Guaranteed-hit accuracy in the ordinary contest does not bypass special dodges, immunity or shielding.";break;
            case "Greataxe":
                effect="Retribution is available only strictly below 50% HP; exactly half health does not qualify. It adds its bonus to the attack input with guaranteed-hit accuracy in the ordinary contest. A kill takes no ordinary attack time; a survivor costs normal attack time. Charges, target eligibility and defenses keep their normal rules.";break;
            case "Flail":
                effect="Each Spin adds one spin up to three, spends one game-time unit and keeps at least a three-unit scheduled window. The first spin costs a charge; further spins while the tracker exists cost zero charges. A supported next attack consumes the spin benefit and adds spins × the bonus below, even if a special dodge defeats the attempt. Inspection does not trigger the Flail's mutating accuracy query.";break;
            case "Dagger":case "Dirk":case "AssassinsBlade":
                int range=type.equals("Dagger")?5:type.equals("Dirk")?4:3;
                effect="Sneak moves to a visible unoccupied cell reachable by the existing passable-or-avoidable path within "+range+" tiles; rooting prevents it. It spends no ordinary movement time and offers at least 1 + effective weapon level scheduled units of Invisibility (the ordinary displayed counter includes its additional unit). Existing longer Invisibility is preserved. "+(level==null?"Its upgrade-dependent duration remains hidden.":"Current offered scheduled duration: "+(1+level)+" game-time units.");break;
            case "Sickle":case "WarScythe":
                float start=type.equals("Sickle")?15:30,increment=type.equals("Sickle")?2.5f:4.5f;
                effect="Harvest replaces the attack's direct damage contribution with an offered bleeding input: damage augmentation applied to round("+n(start)+" + "+n(increment)+" × effective level). Bleeding is a damage-over-time strength, not guaranteed total HP loss; its later ticks, random reduction and defenses keep their normal rules. The attack uses normal attack time. "+(level==null?"Upgrade-dependent results remain hidden.":"Current offered input before subsequent defenses: "+weapon.augment.damageFactor(Math.round(start+increment*level))+" points.");break;
            case "RoundShield":case "Greatshield":case "Buckler":case "Targe":case "KiteShield":
                int durationBase=type.equals("Buckler")?7:type.equals("Targe")?6:type.equals("RoundShield")?5:type.equals("KiteShield")?4:3;
                effect="Guard offers at least "+durationBase+" + effective level scheduled time units and spends one game-time unit to set up. It supplies guaranteed evasion in the ordinary attack contest while active, rather than a fixed shield-point reserve. After it has blocked, a supported invisibility-breaking action can end the Guard early; its normal expiry and direct-effect rules still apply. "+(level==null?"Its upgrade-dependent duration remains hidden.":"Current offered scheduled duration: "+(durationBase+level)+" game-time units.");break;
            case "Quarterstaff":case "Scimitar":
                effect="The stance is readied without ordinary setup time, keeping at least 3 + effective weapon level scheduled units. "+(type.equals("Quarterstaff")?"Defensive Stance multiplies the applicable evasion contest score by 3; it is not a fixed dodge percentage.":"Sword Dance adds 0.6 to the attack-speed factor and multiplies the applicable accuracy score by 1.5. These are separate scores, not a guaranteed hit chance.")+(level==null?" Its upgrade-dependent duration remains hidden.":" Current offered scheduled duration: "+(3+level)+" game-time units.");break;
            case "RunicBlade":
                effect="Runic Slash uses a temporary enchantment-power boost of 3 + 0.5 × effective weapon level. "+(level==null?"The upgrade-dependent boost remains hidden.":"Current boost: "+n(((RunicBlade)weapon).runicSlashEnchantBoost())+".")+" Each enchantment uses that boost through its own probability, strength and cap rules. It is not that many extra physical damage points or one universal proc percentage. The supported attack uses normal attack time and normal defenses.";break;
            case "Whip":effect="Lash attacks eligible visible, non-charming enemy targets already in the hero's legal attack reach, using one normal attack delay for the sequence. Each target receives its own normal attack processing and defenses with guaranteed-hit accuracy in the ordinary contest. This is not unlimited area damage through walls; inspection does not count hidden enemies or attempt an attack.";break;
            case "Pickaxe":
                effect="Pierce uses guaranteed-hit accuracy in the ordinary attack contest and normal attack time. Its added damage input below applies only to inorganic targets, Swarms, Bees, Crabs, Spinners and Scorpios; other eligible targets get zero added damage from this ability. A successful hit on a surviving target offers three scheduled game-time units of Vulnerable through the usual buff rules. Vulnerable makes supported subsequent attacks deal more damage while it lasts. The normal target defenses, special dodges and ability charge rules still apply. This is a conditional flat damage bonus, not a universal 100% damage multiplier. Mining keeps its separate action rules.";break;
            case "Crossbow":effect="Charged Shot readies a one-use benefit without ordinary setup time and cannot be readied again while already active. The supported dart shot applies its separate damage, extra-tip-use and nearby on-hit-effect rules. It does not fire a dart immediately, grant free ammunition or remove the subsequent shot's normal time cost.";break;
            default:return "";
        }
        return "_Duelist weapon ability_\n"+effect+"\n"+formula+" Ability access, charge capacity, target-specific costs and other talents remain governed by the existing class and weapon rules. Viewing this panel never readies an ability or spends a charge.";
    }
}
