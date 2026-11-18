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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Wayward;
import com.shatteredpixel.shatteredpixeldungeon.apd.ArcanaInfoDisplay;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomResourceHooks;

/** Known curses only. Private heat counters, unknown curse identities and hidden levels stay hidden. */
public final class CurseExplanation {
    private CurseExplanation(){}
    private static String n(float x){return ObfuscationExplanation.number(x);}
    private static String pct(float x){return n(100*Math.max(0,Math.min(1,x)))+"%";}
    private static String chance(float base,float power,boolean detailed){return "Current chance: "+pct(base*power)+" on an eligible hit. "+(detailed?"Base chance "+pct(base)+" × known current effect power "+n(power)+"; probability cannot exceed 100%. These curses do not all gain extra strength beyond a guaranteed trigger. ":"");}
    public static String weapon(Weapon weapon,Hero hero,boolean detailed){
        if(weapon.enchantment==null||!weapon.enchantment.curse()||!weapon.cursedKnown||!weapon.enchantment.getClass().getPackage().getName().equals(Wayward.class.getPackage().getName()))return "";
        if(hero!=null&&(!weapon.hasEnchant(weapon.enchantment.getClass(),hero)||hero.buff(MagicImmune.class)!=null))return "This known weapon curse is currently suppressed or overridden.";
        float power=EnchantmentExplanation.knownPower(hero);if(!Float.isFinite(power))return "Combined curse values are withheld while contributing bonuses are unidentified or unavailable.";
        String name=weapon.enchantment.getClass().getSimpleName(),effect;float base;
        switch(name){
            case "Annoying":base=1/20f;effect="Alerts floor monsters toward the attacker and dispels invisibility. It does not create a new monster or guarantee an immediate hit.";break;
            case "Dazzling":base=1/10f;effect="Characters that can see the struck target can be blinded. The attacker is offered "+n(Blindness.DURATION)+" blindness turns; other qualifying witnesses are offered half that duration before resistance. Allies are not automatically excluded.";break;
            case "Displacing":base=1/12f;effect="Can randomly teleport the struck target if it is movable and a legal teleport destination exists. It does not guarantee the enemy remains in reach; a hunting monster that teleports switches to wandering.";break;
            case "Friendly":base=1/10f;effect="Can charm attacker and target toward each other: attacker duration offered "+n(Charm.DURATION)+" turns, target duration half that, before resistance. A hit against someone already charming the attacker deals zero from this curse's damage step.";break;
            case "Polarized":base=.5f;effect="A successful curse roll changes this step's attack damage to 1.5 times its supplied amount, rounded to whole damage points. Otherwise this step deals zero. More activation power changes that split, not the fixed 1.5 damage factor.";if(detailed)effect+=" Before rounding, armor and later effects, the average raw factor is 1.5 × capped activation chance = "+n(1.5f*Math.max(0,Math.min(1,base*power)))+". This is not average final HP loss.";break;
            case "Pressurized":base=1/8f;effect="Triggers a geyser at the struck target. Geyser damage, water and knockback follow their own terrain and target rules; a melee attack can direct its central knockback away from the attacker.";break;
            case "Sacrificial":{
                base=1/10f;float input=hero==null||hero.HT<=0?0:(float)Math.pow(hero.HP/(float)hero.HT,2)*hero.HT/8f;
                return "Current combined chance to apply bleeding: "+pct(Math.max(0,Math.min(1,base*power))*Math.max(0,Math.min(1,input)))+" per eligible hit at your current HP. It can make you bleed; higher current HP increases this second check and its bleeding input. This is not a fixed amount of HP lost."+(detailed?"\n\nFirst roll chance = 0.1 × power, capped at 100%. Second roll chance = min(1, (current HP / maximum HP)² × maximum HP / 8), currently "+pct(input)+". Both rolls must pass. Bleeding input offered on success = max(1, that second-check amount), currently "+n(Math.max(1,input))+" points before Bleeding's initial random reduction, source-specific damage and diminishing ticks. Weapon upgrade level does not enter this curse formula.":"");
            }
            case "Wayward":base=.25f;effect="Can apply a "+n(Wayward.WaywardBuff.DURATION)+"-turn accuracy penalty. While present with a Wayward weapon, its attack accuracy contribution is divided by 5; that is a score factor, not a fixed hit chance. The next eligible weapon activation removes an existing Wayward penalty instead of rolling a new one.";if(hero!=null&&hero.buff(Wayward.WaywardBuff.class)!=null)return "The Wayward penalty is currently active: this weapon's accuracy contribution is one fifth normal. Its next eligible activation clears that penalty; it does not roll to add another one. "+(detailed?effect:"");break;
            case "Wondrous":base=1/8f;effect="Chooses a valid cursed-wand effect. The result can help or harm, and Wondrous Resin can restrict the selection. There is no one fixed damage amount, duration or target outcome for every possible effect.";break;
            case "Explosive":return "Uses a hidden instability meter rather than one fixed explosion chance per hit. Each eligible activation reduces that meter by a random amount; heat warnings show broad danger states, not an exact safe number of attacks. Explosions can harm the wielder and others nearby."+(detailed?"\n\nMeter loss = round(a random integer from 0 to 10 × known effect power), currently power "+n(power)+". Warning thresholds are handled before the depleted-meter explosion branch, so an exact next-hit explosion probability is not claimed. A blast replenishes the meter by 100; missile explosions can also spend extra missile durability. The warmth warnings do not tell you an exact safe number of attacks.":"");
            default:return "";
        }
        return chance(base,power,detailed)+effect+(detailed?"\n\nThese base curse chances do not scale directly with weapon upgrades. Other equipment and the curse's stated conditions still affect actual outcomes.":"");
    }
    public static String armor(Armor armor,Hero hero,boolean detailed){
        if(armor.glyph==null||!armor.glyph.curse()||!armor.cursedKnown||!armor.glyph.getClass().getPackage().getName().equals(com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Bulk.class.getPackage().getName()))return "";
        if(hero!=null&&(!armor.hasGlyph(armor.glyph.getClass(),hero)||hero.buff(MagicImmune.class)!=null))return "This known armor curse is currently suppressed or overridden.";
        float power=ArcanaInfoDisplay.knownArcanaPower(hero);if(Float.isFinite(power))power+=ObfuscationExplanation.aura(hero);if(!Float.isFinite(power))return "Combined curse values are withheld while contributing bonuses are unidentified or unavailable.";
        String name=armor.glyph.getClass().getSimpleName(),effect;float base;
        switch(name){
            case "Bulk":if(armor.levelKnown&&armor.buffedLvl()<0)return "This passive curse is inactive at the current effective armor level.";return "While this curse is active on an ordinary closed or open door tile, its movement-rate factor is "+n(power/3f)+" before other movement effects. A smaller rate makes steps take more game time; this is not an attack-speed penalty."+(detailed?"\n\nDoor-tile movement rate = power / 3. Movement time is divided by this factor. Armor level does not scale that rate, but the passive requires a nonnegative effective glyph level. Other terrain does not trigger this specific penalty.":"");
            case "AntiEntropy":base=1/8f;effect="Freezes the surrounding eight tiles and can ignite the wearer for a base 4 turns if the wearer is not on a water tile. Those are separate effects, not immunity to fire or freezing.";break;
            case "Corrosion":base=1/10f;effect="Applies Ooze to characters on the wearer's tile and eight neighboring tiles, including allies. Duration offered is "+n(Ooze.DURATION/2)+" turns before resistance. This applies the Ooze status, not the separately named Corrosion status; Ooze can be washed off with water.";break;
            case "Displacement":base=1/20f;effect="Attempts to teleport the wearer and returns zero damage from this armor-curse step on activation, even if no destination is available. Other separate damage/effects can still occur.";break;
            case "Metabolism":{
                base=1/6f;int preliminary=hero==null?0:Math.max(0,Math.min((int)Hunger.STARVING/100,hero.HT-hero.HP));float foodCost=CustomResourceHooks.hungerAdvance(preliminary*10);
                effect="Can spend nourishment to heal a living hero who is missing HP and not already starving. At current HP, the preliminary heal is "+preliminary+" HP and the hunger cost is "+n(foodCost)+" hunger points under current hunger rules. Healing rules then adjust the actual HP gain; spending food does not guarantee that full preliminary amount is healed.";
                if(detailed)effect+=" Preliminary amount = min(floor(starvation threshold / 100), missing HP). Cost uses that preliminary amount × 10 hunger points, scaled by the hunger rule. The healing rule applies separately to floor(starvation threshold / 100), capped by missing HP.";break;
            }
            case "Multiplicity":base=1/20f;effect="If an adjacent legal spawn tile exists, it can create a hero mirror image or a monster. For a hero wearer, the mirror-versus-monster choice is 50/50; unsuitable attackers use floor monster generation instead of duplicating a boss, NPC or other excluded creature. Space and large-creature restrictions can prevent creation.";break;
            case "Overgrowth":base=1/20f;effect="Creates and activates a random plant effect on the wearer. The Warden's normally beneficial plant interpretation is temporarily bypassed for this curse. The random plant's own damage, duration and other rules apply.";break;
            case "Stench":base=1/8f;effect="Creates toxic gas around the wearer. Gas volume, spread and terrain determine exposure; the seed amount is not a guaranteed number of damage points or turns for every nearby character.";if(detailed)effect+=" The new gas seed supplies 250 gas-volume units, which are distributed and decay using the gas field's rules.";break;
            default:return "";
        }
        return chance(base,power,detailed)+effect+(detailed?"\n\nThese base trigger chances do not directly increase with armor upgrades. Known Arcana and the current protective aura modify power; immunity, target and terrain conditions still apply.":"");
    }
}
