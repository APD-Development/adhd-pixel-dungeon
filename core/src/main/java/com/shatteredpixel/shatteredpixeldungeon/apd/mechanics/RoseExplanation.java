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
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;

/** Potential base ghost statistics; never locates a ghost or inspects hidden equipment. */
public final class RoseExplanation {
    private RoseExplanation() {}
    public static int maximumHealth(DriedRose rose){return rose.isIdentified()?40+10*rose.level():-1;}
    public static int unarmedMaximum(DriedRose rose){return rose.isIdentified()?rose.ghostStrength()-8:-1;}
    public static String summary(DriedRose rose){
        if(!rose.isIdentified())return "";
        return "After completing the Ghost's quest, equip the rose and fill its charge to summon him. His base stats are _"+maximumHealth(rose)+" HP_ and _"+rose.ghostStrength()+" strength_. While summoned, the rose's charge display shows his health and recharging heals him instead. Keep the Rose equipped; magic immunity also makes him fade.";
    }
    public static String details(DriedRose rose,Hero hero){
        if(!rose.isIdentified())return "";
        int level=rose.level();
        return "_Summoning_\nComplete the Ghost’s quest, wear an uncursed Rose and fill its _"+rose.maximumCharges()+"% charge_. You also need no Magic Immunity and an open neighboring tile. Summoning takes _1 turn_, empties the meter and ends your invisibility. Only one ghost can be summoned at a time."
            +"\n\n_Base statistics_\nMaximum HP = 40 + 10 × Rose level = _"+maximumHealth(rose)+" HP_. Strength = 13 + floor(Rose level / 2) = _"+rose.ghostStrength()+"_. Unarmed damage: _1–"+unarmedMaximum(rose)+"_ (maximum = Strength − 8), with middle rolls more common. A weapon replaces that roll and can add 0 to (ghost Strength − weapon requirement) damage from excess Strength."
            +"\nAccuracy = hero level + 9; evasion = hero level + 4."
            +(hero==null?" Personal scores are withheld without a loaded hero.":" Current base scores: _"+(hero.lvl+9)+" accuracy_ and _"+(hero.lvl+4)+" evasion_.")
            +" Equipment and statuses change these scores; they are not hit or dodge percentages."
            +"\n\n_Equipment_\nGive him known-uncursed melee weapons or armor that his Strength supports. Unknown upgrades must also pass the +0 Strength check. Unique items and armor with an attached seal cannot be given. This transfers your actual gear."
            +"\n\n_Petals and upkeep_\n"+(level>=rose.maximumUpgradeSteps()?"Maximum Rose upgrade reached. ":"Each upgrade adds 10 maximum HP at the ghost’s next stat refresh. It also heals up to 8 HP, limited by his maximum at that moment. ")
            +"With a living ghost, recharge heals him instead of refilling the summoning meter. If the Rose is unequipped or you are magic-immune, he takes _1 damage before each action_.";

    }
}
