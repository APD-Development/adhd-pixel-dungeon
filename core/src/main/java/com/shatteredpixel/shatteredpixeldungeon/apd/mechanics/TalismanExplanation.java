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

import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;

/** Public artifact state only: never constructs a cone or inspects any floor cell. */
public final class TalismanExplanation {
    private TalismanExplanation() {}
    public static float range(TalismanOfForesight item) {
        return item.isIdentified() ? Math.min(5+2*item.level(),(item.currentCharges()-3)/1.08f) : Float.NaN;
    }
    public static String summary(TalismanOfForesight item) {
        if(!item.isIdentified())return "";
        return "Scrying searches a cone for secrets and maps it, costing _1 turn_ and charge. It reveals creatures and item piles for _"
            +(5+2*item.level())+" turns_. The talisman's warning about nearby hidden traps is passive.";
    }
    public static String details(TalismanOfForesight item) {
        if(!item.isIdentified())return "";
        int level=item.level();
        return "_Scrying_\nWear the Talisman and have at least _5% charge_; Magic Immunity prevents casting. Choosing your own tile cancels. A cast takes _1 turn_ and ends invisibility. It maps a cone and uncovers its secrets. Eligible creatures and item piles remain revealed for _"+(5+2*level)+" turns_ (5 + 2 × upgrade level); allies and ordinary neutral creatures are excluded, but Mimics can be revealed."
            +"\n\n_Range and charge_\nLong-cast range = min(5 + 2 × upgrade level, (whole charge − 3) / 1.08). Current limit: "
            +(item.currentCharges()<5?"unavailable below 5% charge.":"_"+ObfuscationExplanation.number(range(item))+" tiles_.")
            +" Adjacent targets extend to two tiles; short casts have their own minimum-distance rule. At actual target distance d, cone angle = round(200 × 0.92^d) degrees: longer casts are narrower. Nominal charge cost = 3 + 1.08 × d percentage points, with whole-charge rounding and fractional carry."
            +"\n\n_Upgrading_\nEarn 1 progress point per newly mapped tile, 10 per hidden trap, 100 per secret door, 10 per eligible creature outside sight and 10 per unseen item pile. "+(level>=item.maximumUpgradeSteps()?"Maximum upgrade +10 reached.":"The next upgrade needs _"+(100+50*level)+" progress points_ (100 + 50 × upgrade level). One cast can award one upgrade, keeping leftover progress.")+" These points are separate from charge."
            +"\n\n_Passive warning_\nWarns about searchable hidden traps in your field of view within three tiles horizontally and vertically. It does not search for hidden doors.";

    }
}
