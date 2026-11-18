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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//APD katana family, tier 2. Same rules as the tier 4 Katana: reduced damage, a guard
//that blocks 1 + half the weapon level, and the Duelist's lunge.
public class Wakizashi extends MeleeWeapon {

	{
		image = ItemSpriteSheet.WAKIZASHI;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.2f;

		tier = 2;
	}

	@Override
	public int max(int lvl) {
		return  4*(tier+1) +    //12 base, like Katana's 4*(tier+1)
				lvl*(tier+1);
	}

	@Override
	public int defenseFactor( Char owner ) {
		return DRMax();
	}

	public int DRMax(){
		return DRMax(buffedLvl());
	}

	public int DRMax(int lvl){
		return 1 + Math.max(0, lvl) / 2;
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
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	private int boost(int lvl){
		//Katana: +(8+2*lvl) at tier 4, i.e. 2*tier + tier/2 per level
		return 2*tier + Math.round(tier/2f*lvl);
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		int dmgBoost = augment.damageFactor(boost(buffedLvl()));
		Rapier.lungeAbility(hero, target, 1, dmgBoost, this);
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = levelKnown ? boost(buffedLvl()) : boost(0);
		if (levelKnown){
			return Messages.get(this, "ability_desc", (augment.damageFactor(min()) + augment.damageFactor(dmgBoost)), (augment.damageFactor(max()) + augment.damageFactor(dmgBoost)));
		} else {
			return Messages.get(this, "typical_ability_desc", augment.damageFactor(min(0)) + augment.damageFactor(dmgBoost), augment.damageFactor(max(0)) + augment.damageFactor(dmgBoost));
		}
	}

	public String upgradeAbilityStat(int level){
		int dmgBoost = boost(level);
		return (augment.damageFactor(min(level)) + augment.damageFactor(dmgBoost)) + "-" + (augment.damageFactor(max(level)) + augment.damageFactor(dmgBoost));
	}
}
