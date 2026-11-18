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

import com.shatteredpixel.shatteredpixeldungeon.apd.ArcanaInfoDisplay;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Obfuscation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** Explanation of public values only. No combat rolls, hidden rings, mutations or enemy scans. */
public final class ObfuscationExplanation {
    private ObfuscationExplanation(){}
    public static String number(float value){return Float.isFinite(value)?Messages.decimalFormat((value!=0&&Math.abs(value)<.001f)||Math.abs(value)>=1000000?"0.###E0":"0.###",value):"unavailable";}
    public static boolean applies(Armor armor){return armor!=null&&armor.glyph instanceof Obfuscation;}
    public static float base(int level){return level<0?0:1+level/3f;}
    public static float aura(Hero hero){return hero!=null&&hero.buff(AuraOfProtection.AuraBuff.class)!=null?.25f+.25f*hero.pointsInTalent(Talent.AURA_OF_PROTECTION):0;}
    public static String summary(Armor armor,Hero hero){
        if(!applies(armor))return "";
        String meaning="Makes unaware enemies less likely to notice you.";
        if(hero!=null&&hero.buff(MagicImmune.class)!=null)return "Magic immunity currently disables this glyph.";
        if(hero!=null&&!armor.hasGlyph(Obfuscation.class,hero))return "Your current effects replace this glyph.";
        if(!armor.levelKnown)return meaning;
        float value=base(Math.max(-1,armor.buffedLvl()));
        if(value==0)return "This glyph currently adds no stealth.";
        float power=ArcanaInfoDisplay.knownArcanaPower(hero),aura=aura(hero);
        if(!Float.isFinite(power))return meaning;
        // Same eligible sleeping-enemy check as Mob.Sleeping.detectionChance, fixed three-tile example.
        float chance=100f/(3f+value*(power+aura));
        return meaning.substring(0,meaning.length()-1)+": a sleeping enemy 3 tiles away notices you _"+Messages.decimalFormat("0.#",chance)+"%_ of the time.";
    }
    public static String details(Armor armor,Hero hero){
        if(!applies(armor))return "";
        String rules="_How detection works_\nFor a standard sleeping enemy's eligible check: chance = 1 / (distance in tiles + your stealth points).\nFor standard wandering behavior: chance = 1 / (distance in tiles / 2 + your stealth points). The random check cannot exceed 100%. These are separate enemy checks, not one universal chance per player turn.\n\nAn enemy must meet its visibility/awareness rules before a normal check applies. Alerts, damage, invisibility, Silent Steps, flying, special enemies and special stealth-gameplay behavior can change or bypass these rules. No fixed detection percentage is implied.";
        if(hero!=null&&hero.buff(MagicImmune.class)!=null)return "The glyph is currently suppressed by magic immunity.\n\n"+rules;
        if(hero!=null&&!armor.hasGlyph(Obfuscation.class,hero))return "This glyph is currently overridden or suppressed by your active effects.\n\n"+rules;
        if(!armor.levelKnown)return "The armor's level-dependent numbers remain hidden until identified.\n\n"+rules;
        int level=Math.max(-1,armor.buffedLvl());float value=base(level),power=ArcanaInfoDisplay.knownArcanaPower(hero);
        if(value==0)return "The glyph is inactive at the current effective armor level.\n\n"+rules;
        String result="_This armor's contribution_\nEffective armor level: "+level+". Before bonuses: 1 + armor level / 3 = "+number(value)+" stealth points. This is an added score, not a percentage or a chance to stay hidden.\n";
        result+="One more effective armor level adds 1/3 stealth point before bonuses. At the next level the base score would be "+number(base(level+1))+" points.\n";
        if(Float.isFinite(power)){
            result+="Your identified Arcana ring effect multiplies this score by "+number(power)+": "+number(value)+" × "+number(power)+" = "+number(value*power)+" stealth points. At the next armor level: "+number(base(level+1)*power)+" points with the same ring bonuses.\n";
            result+="Each additional effective Arcana ring bonus increases its multiplier by a factor of 1.175. Multiple Arcana ring bonuses combine before that exponent is calculated; curses and temporary ring bonuses can change the result.\n";
            if(aura(hero)>0)result+="Your current protective aura adds "+number(aura(hero))+" to the ring multiplier before it multiplies the armor score: "+number(value)+" × ("+number(power)+" + "+number(aura(hero))+") = "+number(value*(power+aura(hero)))+" points.\n";
        }else result+="The ring-enhanced score is withheld because a contributing bonus is unidentified or unavailable.\n";
        return result+"Displayed decimals are rounded; gameplay uses the original full-precision calculation.\n\n"+rules;
    }
}
