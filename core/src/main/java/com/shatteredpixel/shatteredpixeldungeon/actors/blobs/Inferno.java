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
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CellDamageOrigins;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveDamageOrigin;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveEffectContext;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;

public class Inferno extends Blob {

	private CellDamageOrigins apdOrigins = new CellDamageOrigins();
	private static final String APD_ORIGINS = "apd_inferno_origins";

	public static Inferno seedAttributed(int cell, int amount, OffensiveDamageOrigin origin) {
		try (OffensiveEffectContext ignored = OffensiveEffectContext.enter(origin)) {
			return Blob.seed(cell, amount, Inferno.class);
		}
	}

	@Override
	public void seed(com.shatteredpixel.shatteredpixeldungeon.levels.Level level, int cell, int amount) {
		apdOrigins.seed(cur, level.length(), cell, amount, OffensiveEffectContext.current());
		super.seed(level, cell, amount);
	}

	@Override public void clear(int cell) { super.clear(cell); apdOrigins.clear(cell); }
	@Override public void fullyClear() { super.fullyClear(); apdOrigins.clearAll(); }
	@Override public void storeInBundle(Bundle bundle) { super.storeInBundle(bundle); if (volume > 0) apdOrigins.store(bundle, APD_ORIGINS, cur); else CellDamageOrigins.clearMetadata(bundle, APD_ORIGINS); }
	@Override public void restoreFromBundle(Bundle bundle) { super.restoreFromBundle(bundle); apdOrigins = CellDamageOrigins.restore(bundle, APD_ORIGINS, cur); }
	
	@Override
	protected void evolve() {
		apdOrigins.synchronize(cur);
		super.evolve();
		apdOrigins.prepareDiffusion(cur, off, Dungeon.level.solid, Dungeon.level.width());
		
		int cell;
		boolean observe = false;
		
		Fire fire = (Fire)Dungeon.level.blobs.get( Fire.class );
		Freezing freeze = (Freezing)Dungeon.level.blobs.get( Freezing.class );
		
		Blizzard bliz = (Blizzard)Dungeon.level.blobs.get( Blizzard.class );
		
		for (int i = area.left-1; i <= area.right; i++) {
			for (int j = area.top-1; j <= area.bottom; j++) {
				cell = i + j * Dungeon.level.width();
				if (cur[cell] > 0) {
					
					if (fire != null)   fire.clear(cell);
					if (freeze != null) freeze.clear(cell);
					
					if (bliz != null && bliz.volume > 0 && bliz.cur[cell] > 0){
						bliz.clear(cell);
						off[cell] = cur[cell] = 0;
						continue;
					}
					
					Fire.burnAttributed(cell, apdOrigins, cell);

					if (Dungeon.level.flamable[cell]){
						Dungeon.level.destroy( cell );

						observe = true;
						GameScene.updateMap( cell );
					}
					
				} else if (Dungeon.level.flamable[cell]
						&& (cur[cell-1] > 0
						|| cur[cell+1] > 0
						|| cur[cell-Dungeon.level.width()] > 0
						|| cur[cell+Dungeon.level.width()] > 0)) {

					if (fire == null || fire.cur == null || fire.cur[cell] == 0) {
						double[] shares = apdOrigins.neighbourShares(cur, cell, Dungeon.level.width());
						GameScene.add(Fire.seedAttributedMixture(cell, 4, shares));
					}

				}
			}
		}

		apdOrigins.finishDiffusion();
		apdOrigins.synchronize(off);
		apdOrigins.discardIfUnattributed();
		
		if (observe) {
			Dungeon.observe();
		}
	}
	
	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		
		emitter.pour( Speck.factory( Speck.INFERNO, true ), 0.4f );
	}
	
	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
	
}
