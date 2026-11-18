/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//APD shield, tier 2. Same shape as Round Shield (tier 3) and Greatshield (tier 5):
//low damage, blocking that scales with upgrades, and the Duelist's guard.
public class Targe extends MeleeWeapon {

	{
		image = ItemSpriteSheet.TARGE;

		tier = 2;
	}

	@Override
	public int max(int lvl) {
		return  Math.round(3f*(tier+1)) +   //9 base
				lvl*1;                   //+1 per level
	}

	@Override
	public int defenseFactor( Char owner ) {
		return DRMax();
	}

	public int DRMax(){
		return DRMax(buffedLvl());
	}

	//3 extra defence, plus 3 per 4 levels
	public int DRMax(int lvl){
		return 3 + (3*Math.max(0, lvl)) / 4;
	}

	@Override
	public String statsInfo(){
		if (isIdentified()){
			return Messages.get(this, "stats_desc", DRMax());
		} else {
			return Messages.get(this, "typical_stats_desc", DRMax(0));
		}
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		RoundShield.guardAbility(hero, 6+buffedLvl(), this);
	}

	@Override
	public String abilityInfo() {
		if (levelKnown){
			return Messages.get(this, "ability_desc", 6+buffedLvl());
		} else {
			return Messages.get(this, "typical_ability_desc", 6);
		}
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString(6 + level);
	}
}
