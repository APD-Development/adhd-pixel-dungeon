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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

/** Primary ability rules only; does not select targets, roll attacks or spend energy. */
public final class MonkAbilityExplanation {
    private MonkAbilityExplanation() {}
    public static String details(MonkEnergy energy,Hero hero){
        if(hero==null)return "";
        boolean empowered=energy!=null&&energy.abilitiesEmpowered(hero);
        Integer strength=ArmorExplanation.knownStrength(hero);
        String range=strength==null?"Identify your strength bonuses to see the damage ranges."
                :"With "+strength+" strength, the basic unarmed roll is 1–"+Math.max(1,strength-8)+". Flurry deals 2–"+Math.round(1.5f*Math.max(1,strength-8))+" per strike; Dragon Kick deals "+(empowered?9:6)+"–"+((empowered?9:6)*Math.max(1,strength-8))+" before other bonuses and defenses.";
        return "_Flurry: two instant strikes_\nMakes two attacks at 1.5× basic unarmed damage each, stopping if the first kills. Basic unarmed damage = 1 to max(strength − 8, 1), without Force-ring damage. "+range+" Ignores ordinary armor; special dodges can still avoid a hit. Uses energy but no attack turn and cannot be repeated immediately. Empowerment also applies your weapon enchantment.\n\n"
            +"_Focus: one ready defense_\nReadies a guaranteed dodge against the next normal attack, including attacks that normally cannot miss. Direct damage and status effects can still affect you. Costs one turn to ready, or no turn when empowered, plus energy. Cannot be readied again until used.\n\n"
            +"_Dash: a clear route_\nMoves up to 4 tiles, or 8 when empowered; current range: "+(empowered?8:4)+" tiles. Uses energy but no movement turn. Rooting prevents it. The destination must be empty, with a clear projectile path; walls and other obstacles still block it. Arriving can trigger the tile’s normal effects.\n\n"
            +"_Dragon Kick: damage and knockback_\nDeals 6× basic unarmed damage, or 9× when empowered, ignoring ordinary armor. Attempts to push the enemy up to 6 tiles. Paralysis lasts min(6, push-path distance) turns if the target is still active; obstacles and immunity can limit actual movement. Empowerment also pushes adjacent enemies, without another damage roll. Uses the normal unarmed attack time and spends energy.\n\n"
            +"_Meditate: five turns of recovery_\nRemoves most harmful statuses, preserving allegiance changes and Lost Inventory. Spends five separate turns and, after a delay, grants 8 turns of wand and artifact recharging, including the Horn. This is recharge time, not eight charges. Empowered meditation also restores round(missing HP / 5), currently "+Math.round((hero.HT-hero.HP)/5f)+" HP before healing modifiers, over later turns. Only empowered meditation reduces incoming damage to 20% during the meditation; other defenses still apply."
            +(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.disabled()?" Your current rules prevent this healing.":"");
    }
}
