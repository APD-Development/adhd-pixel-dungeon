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

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

public class StormCloud extends Blob {

	private com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CellDamageOrigins apdOrigins =
			new com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CellDamageOrigins();
	private static final String APD_ORIGINS = "apd_storm_origins";

	@Override
	public void seed(com.shatteredpixel.shatteredpixeldungeon.levels.Level level, int cell, int amount) {
		apdOrigins.seed(cur, level.length(), cell, amount,
				com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveEffectContext.current());
		super.seed(level, cell, amount);
	}

	@Override
	public void clear(int cell) {
		super.clear(cell);
		apdOrigins.clear(cell);
	}

	@Override
	public void fullyClear() {
		super.fullyClear();
		apdOrigins.clearAll();
	}

	@Override
	public void storeInBundle(com.watabou.utils.Bundle bundle) {
		super.storeInBundle(bundle);
		if (volume > 0) apdOrigins.store(bundle, APD_ORIGINS, cur);
		else com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CellDamageOrigins.clearMetadata(bundle, APD_ORIGINS);
	}

	@Override
	public void restoreFromBundle(com.watabou.utils.Bundle bundle) {
		super.restoreFromBundle(bundle);
		apdOrigins = com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CellDamageOrigins.restore(bundle, APD_ORIGINS, cur);
	}
	
	@Override
	protected void evolve() {
		apdOrigins.synchronize(cur);
		super.evolve();
		apdOrigins.prepareDiffusion(cur, off, Dungeon.level.solid, Dungeon.level.width());
		
		int cell;

		Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*Dungeon.level.width();
				if (cur[cell] > 0) {
					Dungeon.level.setCellToWater(true, cell);
					if (fire != null){
						fire.clear(cell);
					}

					//fiery enemies take damage as if they are in toxic gas
					Char ch = Actor.findChar(cell);
					if (ch != null
							&& !ch.isImmune(getClass())
							&& Char.hasProp(ch, Char.Property.FIERY)){
						ch.damage(apdOrigins.scale(1 + Dungeon.scalingDepth()/5, cell), this);
					}
				}
			}
		}
		apdOrigins.finishDiffusion();
	}
	
	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.pour( Speck.factory( Speck.STORM ), 0.4f );
	}
	
	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
	
}
