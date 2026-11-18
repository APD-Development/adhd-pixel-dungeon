/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
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

package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class ExoticCrystals extends Trinket {

	{
		image = ItemSpriteSheet.EXOTIC_CRYSTALS;
	}

	@Override
	protected int upgradeEnergyCost() {
		//6 -> 6(12) -> 8(20) -> 10(30)
		return 6+2*level();
	}

	@Override
	public String statsDesc() {
		if (isIdentified()){
			return Messages.get(this, "stats_desc", Messages.decimalFormat("#.##", 100*consumableExoticChance(buffedLvl()))) + healingRuleDescription();
		} else {
			return Messages.get(this, "typical_stats_desc", Messages.decimalFormat("#.##", 100*consumableExoticChance(0))) + healingRuleDescription();
		}
	}

    private String healingRuleDescription(){
        return "\n\nPotions of Healing follow the run's selected Healing-to-Shielding chance instead: " + SPDSettings.apdExoticChance() + "%. At 0%, they never convert.";
    }

	public static float consumableExoticChance(){
		return consumableExoticChance(trinketLevel(ExoticCrystals.class));
	}

	//APD's next-run override applies only to Healing -> Shielding, not other consumables.
	//The saved run/replay setting is resolved by SPDSettings; this method consumes no RNG.
	public static float consumableExoticChance( Class<?> regularType ){
        return regularType == com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing.class
                ? SPDSettings.apdExoticChance() / 100f : consumableExoticChance();
	}

	public static float consumableExoticChance( int level ){
		if (level == -1){
			return 0f;
		} else {
			return 0.2f + 0.2f*level;
		}
	}

}
