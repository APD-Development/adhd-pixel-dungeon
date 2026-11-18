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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.ArcanaInfoDisplay;

/** Reviewed built-in enchantment contributions. Never invokes a proc, damage roll or mutating ring getter. */
public final class EnchantmentExplanation {
    private EnchantmentExplanation(){}
    private static String n(float x){return ObfuscationExplanation.number(x);}
    private static String pct(float x){return n(100*Math.max(0,Math.min(1,x)))+"%";}
    public static float knownPower(Hero hero){
        return withRingPower(hero,ArcanaInfoDisplay.knownArcanaPower(hero));
    }
    public static float knownPowerWithoutArcana(Hero hero){
        if(!Float.isFinite(ArcanaInfoDisplay.knownArcanaPower(hero)))return Float.NaN;
        return withRingPower(hero,1f);
    }
    private static float withRingPower(Hero hero,float power){
        if(!Float.isFinite(power))return Float.NaN;
        Berserk rage=hero.buff(Berserk.class);if(rage!=null)power=rage.enchantFactor(power);
        RunicBlade.RunicSlashTracker slash=hero.buff(RunicBlade.RunicSlashTracker.class);if(slash!=null)power+=slash.boost;
        if(hero.buff(Smite.SmiteTracker.class)!=null)power+=3;
        ElementalStrike.DirectedPowerTracker directed=hero.buff(ElementalStrike.DirectedPowerTracker.class);if(directed!=null)power+=directed.enchBoost;
        if(hero.buff(Talent.SpiritBladesTracker.class)!=null&&hero.pointsInTalent(Talent.SPIRIT_BLADES)==4)power+=.1f;
        if(hero.buff(Talent.StrikingWaveTracker.class)!=null&&hero.pointsInTalent(Talent.STRIKING_WAVE)==4)power+=.2f;
        return power;
    }
    private static float chance(String name,int level,Hero hero){
        switch(name){
            case "Blazing":case "Blooming":return (level+1f)/(level+3f);
            case "Chilling":return (level+1f)/(level+4f);
            case "Shocking":case "Venomous":return 1/3f;
            case "Blocking":case "Lucky":return (level+4f)/(level+40f);
            case "Eldritch":case "Elastic":return (level+1f)/(level+5f);
            case "Vorpal":return 1/4f;
            case "Corrupting":return (level+5f)/(level+25f);
            case "Vampiric":return hero==null||hero.HT<=0?Float.NaN:.05f+.25f*(hero.HT-hero.HP)/(float)hero.HT;
            default:return Float.NaN;
        }
    }
    private static String chanceFormula(String name){
        switch(name){
            case "Blazing":case "Blooming":return "(level + 1) / (level + 3)";
            case "Chilling":return "(level + 1) / (level + 4)";
            case "Shocking":case "Venomous":return "1 / 3";
            case "Blocking":case "Lucky":return "(level + 4) / (level + 40)";
            case "Eldritch":case "Elastic":return "(level + 1) / (level + 5)";
            case "Vorpal":return "1 / 4";
            case "Corrupting":return "(level + 5) / (level + 25)";
            case "Vampiric":return "0.05 + 0.25 × your fraction of HP missing";
            default:return "";
        }
    }
    private static String playerNumber(float value){return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.decimalFormat(value!=0&&Math.abs(value)<.01f?"0.##E0":"0.##",value);}
    private static String playerChance(float base,float power,Hero hero){
        String value="_"+playerNumber(100*Math.max(0,Math.min(1,base*power)))+"%_";
        float without=knownPowerWithoutArcana(hero);
        if(ArcanaInfoDisplay.hasKnownArcana(hero)&&Float.isFinite(without)&&Math.abs(Math.min(1,base*power)-Math.min(1,base*without))>.00001f)
            {} //APD: current value only
        return value;
    }
    /** Complete ordinary enchantment text; do not append the old generic Arcana breakdown. */
    public static String playerDescription(Weapon weapon,Hero hero){
        return com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspect(()->playerDescriptionInInspection(weapon,hero));
    }
    private static String playerDescriptionInInspection(Weapon weapon,Hero hero){
        if(weapon.enchantment==null)return "";
        String fallback=weapon.enchantment.desc();
        if(weapon.enchantment.curse()||!weapon.enchantment.getClass().getPackage().getName().equals(Blazing.class.getPackage().getName()))return fallback;
        if(hero!=null&&hero.buff(MagicImmune.class)!=null)return fallback+" Magic immunity currently disables this enchantment.";
        if(hero!=null&&!weapon.hasEnchant(weapon.enchantment.getClass(),hero))return fallback+" Your current effects replace this enchantment.";
        if(weapon.enchantment instanceof Vampiric&&hero!=null&&com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.disabled())return "Your current rules prevent this enchantment's healing.";
        if(!weapon.levelKnown)return fallback;
        float power=knownPower(hero);if(!Float.isFinite(power))return fallback;
        String name=weapon.enchantment.getClass().getSimpleName();int level=Math.max(0,weapon.buffedLvl());float base=chance(name,level,hero),strength=Math.max(1,base*power);
        String chance=Float.isFinite(base)?playerChance(base,power,hero):"";
        switch(name){
            case "Elastic":return "Has a "+chance+" chance to knock an enemy back up to _"+Math.round(2*strength)+" tiles_. Walls and obstacles can shorten the push.";
            case "Blazing":return "Has a "+chance+" chance to ignite an enemy or deal extra fire damage if it is already burning.";
            case "Chilling":return "Has a "+chance+" chance to slow an enemy with _"+playerNumber(3*strength)+" turns_ of chill. Repeated hits build up to _"+playerNumber(6*strength)+" turns_.";
            case "Venomous":return "Has a "+chance+" chance to poison an enemy for _"+playerNumber((3+level/2f)*strength)+" turns_. Fresh poison starts after a _3-turn_ delay.";
            case "Blocking":return "Has a "+chance+" chance to grant _"+Math.max(0,Math.round((2+weapon.buffedLvl())*strength))+" shielding_. It normally lasts up to _5 turns_ unless used up by damage; Hold Fast can make it last longer.";
            case "Blooming":return "Has a "+chance+" chance to grow grass around the enemy. Higher weapon upgrades grow more grass, if space allows.";
            case "Shocking":return "Has a "+chance+" chance to arc lightning to other nearby creatures for _"+playerNumber(50*strength)+"%_ of the hit's damage. Spares you, your allies and the target you struck. Water helps it spread.";
            case "Eldritch":return "Has a "+chance+" chance to frighten creatures that witness the attack for _"+playerNumber(5*strength)+" turns_. Spares you and your allies; normally spares the target you struck.";
            case "Vorpal":return "Has a "+chance+" chance to replace a hit's immediate damage with bleeding. Harder hits cause heavier bleeding, which weakens over time.";
            case "Lucky":return "Has a "+chance+" chance to make a killing blow produce extra loot unless you have outlevelled that enemy."+(base*power>1?" At 100%, more enchantment power can improve the reward instead.":"");
            case "Corrupting":return "Has a "+chance+" chance to turn an enemy you kill into a corrupted ally. Some enemies cannot be corrupted."+(strength>1.1f?" The new ally also gains _"+Math.round(5*(strength-1))+" turns_ of adrenaline.":"");
            case "Vampiric":{
                int healing=com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomCombatHooks.percentage(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.HEALING);
                return "Has a "+chance+" chance to heal you for about _"+playerNumber(50*strength*healing/100f)+"%_ of the attack’s damage, limited by missing health. It activates more often when you are low on health.";
            }
            case "Kinetic":{
                Kinetic.ConservedDamage stored=hero==null?null:hero.buff(Kinetic.ConservedDamage.class);
                String amount=Math.abs(power-1f)<.00001f?"excess damage":"_"+playerNumber(100*power)+"%_ of new excess damage";
                float without=knownPowerWithoutArcana(hero);
                
                return "Stores "+amount+" from killing blows for your next Kinetic hit. It fades over time. Stored bonus: _"+(stored==null?0:stored.damageBonus())+" damage_.";
            }
            case "Projecting":{
                boolean ranged=weapon instanceof MissileWeapon||weapon instanceof SpiritBow;
                int current=ranged?Math.round(4*power):Projecting.additionalReach(power,weapon.RCH);
                float without=knownPowerWithoutArcana(hero);int baseReach=ranged?Math.round(4*without):Projecting.additionalReach(without,weapon.RCH);
                String comparison="";
                return ranged?"Shots can pass through walls at targets within _"+current+" tiles_ of you"+comparison+".":"Adds up to _"+current+" "+(current==1?"tile":"tiles")+"_ to melee reach"+comparison+".";
            }
            case "Crystal":return "Deals an extra _"+playerNumber(25*power)+"%_ magic damage. "+(weapon instanceof MissileWeapon?"":"Can shatter if worn out; repairs slowly over time. ")+((Crystal)weapon.enchantment).conditionDescription();
            case "Grim":return "Can instantly kill an enemy weakened by your hit. It becomes more likely as the enemy loses health; some enemies resist or are immune.";
            case "Unstable":return "Uses a random enchantment on each attack. Each chosen enchantment works in its usual way.";
            default:return fallback;
        }
    }
    public static String itemSummary(Weapon weapon,Hero hero){String text=describe(weapon,hero,false);if(text.isEmpty())text=CurseExplanation.weapon(weapon,hero,false);return (text.isEmpty()?"":"\n\n"+text)+ArcanaInfoDisplay.weapon(weapon);}
    public static String describe(Weapon weapon,Hero hero,boolean detailed){
        return com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.inspect(()->describeInInspection(weapon,hero,detailed));
    }
    private static String describeInInspection(Weapon weapon,Hero hero,boolean detailed){
        if(weapon.enchantment==null||weapon.enchantment.curse()||!weapon.enchantment.getClass().getPackage().getName().equals(Blazing.class.getPackage().getName()))return "";
        if(hero!=null&&(!weapon.hasEnchant(weapon.enchantment.getClass(),hero)||hero.buff(MagicImmune.class)!=null))return "This enchantment is currently suppressed or overridden; no active benefit is claimed.";
        if(!weapon.levelKnown)return "Identify the weapon's upgrades to view its level-dependent enchantment calculations. Hidden upgrade values are not used in this preview.";
        String name=weapon.enchantment.getClass().getSimpleName();int level=Math.max(0,weapon.buffedLvl());float power=knownPower(hero);
        if(!Float.isFinite(power))return "Combined enchantment values are withheld while contributing bonuses are unidentified or unavailable.";
        float base=chance(name,level,hero),multiplied=base*power,strength=Math.max(1,multiplied);String effect;
        switch(name){
            case "Blazing":effect="Can ignite an eligible enemy for a base 8 turns. Against an already-burning target, it also deals a depth-based fire damage roll scaled by "+n(.67f*strength)+" before rounding and target defenses.";break;
            case "Chilling":effect="Adds up to "+n(3*strength)+" chill turns per activation, limited to "+n(6*strength)+" accumulated turns before target resistance and timing. More chill slows actions more, up to its normal cap.";break;
            case "Venomous":effect="Adds "+n((3+level/2f)*strength)+" poison-duration turns. If the target had no poison, its first poison action is delayed by 3 turns. This is duration, not a fixed damage amount; poison damage depends on its remaining duration and source rules.";break;
            case "Blocking":{int shield=Math.round((2+weapon.buffedLvl())*strength);effect=shield>0?"Grants "+shield+" shield points with a 5-turn timer. Hold Fast can slow or pause the timer. Taking damage can use up the shield sooner.":"At this effective weapon level, activation does not add shield points. An existing Blocking shield can still have its baseline duration refreshed.";break;}
            case "Blooming":effect="Attempts "+n((1+.1f*level)*strength)+" grass tiles before randomly rounding the fractional tile count up or down. Terrain and available positions can reduce actual growth.";break;
            case "Shocking":effect="Other creatures reached by the lightning take "+n(50*strength)+"% of the hit's damage before their defenses and rounding, including neutral creatures. You, your allies and the creature you struck are spared. Water can extend the chain.";break;
            case "Eldritch":effect="Creatures that can see the attacker or target receive "+n(5*strength)+" turns of fear before resistance. Spares the attacker, the struck creature and creatures allied with the attacker. If the hero witnesses someone else's strike, its target gets 5 turns of vertigo instead.";break;
            case "Elastic":effect="Attempts to knock the target back up to "+Math.round(2*strength)+" tiles, limited by terrain, collisions and target restrictions.";break;
            case "Vorpal":effect="Replaces immediate hit damage with bleeding when it activates. Bigger hits cause heavier bleeding, which weakens over time. Bleeding-immune targets take the ordinary hit instead.";break;
            case "Lucky":effect="An eligible kill can produce extra loot, including gold or supplies. Extra power after a guaranteed activation can change the loot generator's quality setting; this is not a guarantee of a particular item or an increase to every normal loot source.";break;
            case "Corrupting":effect="An allied attack can replace an eligible killed enemy with a corrupted ally if the duplication rules allow it. Already-corrupted, immune, unsuitable or still-living targets do not become allies. With sufficient excess activation power, the ally can also receive adrenaline.";break;
            case "Vampiric":effect="On activation, offers healing equal to "+n(50*strength)+"% of the attack's supplied damage, rounded to whole HP before healing rules and limited by your missing HP. Only opposing targets qualify; neutral non-mimics do not. It cannot heal a dead attacker. Chance rises as your HP falls, not with weapon upgrades.";break;
            case "Kinetic":{Kinetic.ConservedDamage stored=hero==null?null:hero.buff(Kinetic.ConservedDamage.class);effect="Adds the currently stored "+(stored==null?0:stored.damageBonus())+" damage points to the next eligible hit. New hostile overkill, after subtracting reused stored damage, is multiplied by current enchantment power and rounded to form a new stored bonus. Arcana scales new storage, not damage already stored. The stored amount fades over time.";break;}
            case "Projecting":effect=weapon instanceof MissileWeapon||weapon instanceof SpiritBow?"Allows qualifying projectiles to pierce near-target obstructions within a target radius of "+Math.round(4*power)+" tiles before any separate Shared Enchantment contribution. The destination still needs to be eligible. This is a wall-piercing target radius, not extra melee reach.":"Adds "+Projecting.additionalReach(power,weapon.RCH)+" melee reach tiles, for total base reach "+(weapon.RCH+Projecting.additionalReach(power,weapon.RCH))+" before any other reach rules. Reach rounds to whole tiles and is capped by the finite floor; extra power can round to no additional tile.";break;
            case "Crystal":effect="Adds "+n(25*power)+"% of the attack's supplied damage as magic damage, rounded up to a whole damage point before the target's defenses. This enchantment also has durability and repair rules; a melee weapon can shatter. It is not free permanent bonus damage without that risk.";break;
            case "Grim":effect="Execution becomes more likely as an eligible enemy loses health. It checks the target after the hit and only if that target is still alive; immunity and resistance can prevent a kill. No fixed execution chance against every enemy is implied.";break;
            case "Unstable":effect="Chooses one of its supported enchantments at random on each attack. The chosen effect follows its own chance, strength and target rules; Projecting and Crystal are not in its normal random list. Stored Kinetic damage can be carried into the attack. There is no single activation chance or damage amount for all outcomes.";break;
            default:return "";
        }
        if(name.equals("Projecting")&&weapon instanceof RunicBlade&&hero!=null&&hero.heroClass==HeroClass.DUELIST){float slash=((RunicBlade)weapon).runicSlashEnchantBoost();RunicBlade.RunicSlashTracker active=hero.buff(RunicBlade.RunicSlashTracker.class);float other=active==null?power:power-active.boost;effect+=" With Runic Slash's offered enchantment bonus and the other current known effects, total base reach would be "+(weapon.RCH+Projecting.additionalReach(other+slash,weapon.RCH))+" tiles.";}
        String result=Float.isFinite(base)?"Current activation chance: "+pct(multiplied)+" on an eligible hit. "+effect:effect;
        if(!detailed)return result;
        String formula=chanceFormula(name);String special="";
        if(name.equals("Grim"))special="Execution chance = (0.5 + 0.05 × level) × power × (target missing-HP fraction after the hit)², capped by a probability of 100%. Current factor before target HP: "+n((.5f+.05f*level)*power)+".\n\n";
        if(name.equals("Blazing"))special="An already-burning target's extra fire damage = round(a depth-based roll × 0.67 × strength). If a new Burning status is applied, subtract one from strength for this immediate extra-damage step. It is not an extra full fire hit on every new ignition.\n\n";
        if(name.equals("Kinetic"))special="Stored force decays by max(2.5% of the stored amount, 0.1 damage point) per tick. The public bonus rounds the stored amount upward.\n\n";
        if(name.equals("Vorpal"))special="Bleeding input = "+n(strength)+" × (2 + attack damage at the conversion stage / 2), before initial random reduction and subsequent ticks.\n\n";
        if(name.equals("Vampiric"))special="Healing offered = round(attack supplied damage × 0.5 × "+n(strength)+"), then normal healing rules and the missing-HP limit apply.\n\n";
        if(name.equals("Venomous"))special="One more effective weapon level adds half a poison-duration turn before activation strength; the flat base trigger chance does not increase with weapon level.\n\n";
        if(name.equals("Projecting")||name.equals("Crystal")||name.equals("Kinetic")||name.equals("Unstable"))special+="Weapon upgrades do not directly enter this enchantment's power formula. They can still affect the weapon's ordinary damage and other equipment values, which can affect damage-dependent outcomes.\n\n";
        if(name.equals("Corrupting"))special="When activation strength is greater than 1.1 and a valid ally is created, adrenaline duration offered = round(5 × (strength − 1)) turns.\n\n";
        return "_"+name+" calculations_\nKnown current enchantment power: "+n(power)+". Identified Arcana ring bonuses supply their ring factor; Enraged Catalyst and active spell/ability trackers add their stated contributions. Temporary bonuses can expire or be spent on an eligible attack.\nUse level = max(0, effective weapon level), currently "+level+". "+(formula.isEmpty()?"":"Base chance = "+formula+" = "+pct(base)+". Multiply by current power, then cap the activation probability at 100%. Excess power uses strength = max(1, multiplied chance). Keeping other conditions unchanged, the next effective weapon level would give "+pct(chance(name,level+1,hero)*power)+" activation chance.\n\n")+special+result+"\n\nThe attack's supplied damage is a damage-point amount at this effect's stage, not a promise of final HP loss. Other damage, resistance, shielding, time and target-specific rules still apply.";
    }
}
