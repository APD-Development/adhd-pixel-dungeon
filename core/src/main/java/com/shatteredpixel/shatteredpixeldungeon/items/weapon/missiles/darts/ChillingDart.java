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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class ChillingDart extends TippedDart {
	
	{
		image = ItemSpriteSheet.CHILLING_DART;
	}
	
	@Override
	public int proc(Char attacker, Char defender, int damage) {

		//when processing charged shot, only chill enemies
		if (!processingChargedShot || attacker.alignment != defender.alignment) {
			if (Dungeon.level.water[defender.pos]) {
				Buff.prolongAttributed(defender, Chill.class, Chill.DURATION,
						com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveDamageOrigin.capture(attacker, null));
			} else {
				Buff.prolongAttributed(defender, Chill.class, 6f,
						com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveDamageOrigin.capture(attacker, null));
			}
		}
		
		return super.proc(attacker, defender, damage);
	}
}
