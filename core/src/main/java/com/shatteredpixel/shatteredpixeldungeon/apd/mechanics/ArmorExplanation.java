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
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight;

/** Armor's own contribution, without rolling defense or querying hidden ring levels. */
public final class ArmorExplanation {
    private ArmorExplanation() {}

    public static Integer knownStrength(Hero hero) {
        Integer might = RingItemEffects.knownBonus(hero, RingOfMight.class, false);
        return might == null ? null : hero.previewStrength(might);
    }

    public static String details(Armor armor, Hero hero) {
        String rules = "_Armor, strength and augmentation_\nProtection is rolled between this armor's minimum and maximum, favoring middle values. Subtract 2 × max(0, requirement − strength), then keep only a positive contribution. Other armor-piercing, magical or special attacks can use different rules.\n"
                + "For each missing strength point, divide the incoming evasion score by 1.5 and movement speed by 1.2. Thus an otherwise unchanged step takes 1.2^missing times as long. Momentum can add evasion afterward; augmentation then adds its evasion points. Stone can replace ordinary dodging entirely. These factors are not dodge percentages.\n"
                + "Armor's strength requirement decreases at upgrade levels +1, +3, +6, +10 and so on; negative levels count as zero for this reduction. A Potion of Mastery reduces the requirement by another 2. Temporary effective-level changes affect protection and augmentation, while the strength requirement uses the armor's own upgrade level.\n";
        if (!armor.levelKnown) return rules + "Upgrade-dependent armor numbers stay hidden until identified.";
        int level = armor.buffedLvl();
        String result = rules + "For ordinary armor, start the maximum-protection calculation at tier × (2 + effective level), then add the augmentation adjustment. Faith Is My Armor instead starts at 1 + tier + effective level. Call that preliminary maximum M and the effective level L. Normally the maximum stays M, except when L > M: then it becomes floor((L − M + 1) / 2). The minimum is L when L < that final maximum, otherwise L minus that maximum. Faith Is My Armor instead keeps its own calculated maximum and sets the minimum to zero.\n" + "Current effective armor level: " + level + "; base protection: " + armor.DRMin() + "–" + armor.DRMax()
                + " damage points; strength requirement: " + armor.STRReq() + " points.\n"
                + "At one higher effective level with the same challenge and augmentation: " + armor.DRMin(level + 1) + "–" + armor.DRMax(level + 1) + " base protection points. This comparison alone does not change the armor's own upgrade level or strength requirement.\n"
                + "Current augmentation adds " + armor.augment.defenseFactor(level) + " to the maximum-protection calculation and "
                + armor.augment.evasionFactor(level) + " evasion points after the strength penalty. It does not add those evasion points to a dodge percentage. Challenge rules and the low-level protection safeguards are already included in the displayed protection range.\n";
        Integer strength = knownStrength(hero);
        if (strength == null) return result + "Personal strength-dependent numbers are withheld because contributing bonuses are unknown or unavailable.";
        int missing = Math.max(0, armor.STRReq() - strength);
        return result + "Known strength: " + strength + "; missing strength: " + missing + "; protection-roll penalty: " + 2 * missing
                + " points. Armor contribution after that penalty: " + Math.max(0, armor.DRMin() - 2 * missing) + "–" + Math.max(0, armor.DRMax() - 2 * missing)
                + " points. Evasion-score factor before later effects: " + ObfuscationExplanation.number((float)Math.pow(1.5, -missing))
                + "; walking-time factor before other movement effects: " + ObfuscationExplanation.number((float)Math.pow(1.2, missing)) + ".";
    }
}
