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
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;

/** Public known-item state only. Does not activate, tick, recharge or upgrade a Cloak. */
public final class CloakExplanation {
    private CloakExplanation() {}
    public static int experiencePerPayment(CloakOfShadows cloak,Hero hero){
        if(!cloak.isIdentified()||hero==null)return -1;
        int level=cloak.level(),difference=hero.lvl-(1+2*level);
        if(level>=7)difference-=level-6;
        return (int)Math.round(10f*Math.pow(difference>=0?1.1f:0.75f,Math.abs(difference)));
    }
    public static String summary(CloakOfShadows cloak){
        if(!cloak.isIdentified())return "";
        return "Activation takes _1 turn_; staying cloaked uses _1 charge every 4 turns_, starting on the first turn. Attacking or using magic can end invisibility early. Natural recharge pauses while cloaked; recharge effects can still add charge.";
    }
    public static String details(CloakOfShadows cloak,Hero hero){
        if(!cloak.isIdentified())return "";
        int remaining=cloak.remainingStealthUpdates(),level=cloak.level();
        String text="_Stealth and charges_\n"+summary(cloak)
            +" You need an uncursed Cloak, a charge and no Magic Immunity. Wear it unless Light Cloak permits carried use. Cancelling takes no extra time or charge."
            +(remaining<0?"\nStealth is currently off.":remaining==0?"\nThe next stealth update needs 1 charge.":"\nNext charge payment in about _"+remaining+(remaining==1?" turn":" turns")+"_.")
            +"\n\nThe first stealth update spends a charge; each successful payment resets the countdown to four turns. If a payment cannot be made, invisibility ends and interrupts you. An attack or spell that breaks invisibility also spends a charge if a payment was already due and a charge remains. Other invisibility sources can remain active."
            +"\n\n_Upgrading_\nEach successful charge payment earns Cloak progress, not hero experience. Target hero level = 1 + 2 × Cloak level + max(0, Cloak level − 6). Let D = hero level − target. Progress = round(10 × 1.1^D) for D ≥ 0, or round(10 × 0.75^(−D)) for D < 0."
            +(hero==null?" Personal progress is withheld without a loaded hero.":" Your current payment earns _"+experiencePerPayment(cloak,hero)+" Cloak progress points_.")
            +(level>=cloak.maximumUpgradeSteps()?" Maximum upgrade reached; further use cannot upgrade it.":" Next upgrade: _"+((level+1)*50)+" progress points_ ((Cloak level + 1) × 50). A payment can award one upgrade and keeps leftover progress.")
            +" Each upgrade adds one charge of capacity, up to 10, without filling it. Current capacity: _"+cloak.maximumCharges()+" charges_.";
        return text;
    }
}
