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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CellDamageOrigins;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveDamageOrigin;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveEffectContext;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.TimedDamageOrigins;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.MagicalFireRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

public class Freezing extends Blob {

	private CellDamageOrigins apdOrigins = new CellDamageOrigins();
	private static final String APD_ORIGINS = "apd_freezing_origins";

	public static Freezing seedAttributed(int cell, int amount, OffensiveDamageOrigin origin) {
		try (OffensiveEffectContext ignored = OffensiveEffectContext.enter(origin)) {
			return Blob.seed(cell, amount, Freezing.class);
		}
	}

	@Override
	public void seed(com.shatteredpixel.shatteredpixeldungeon.levels.Level level, int cell, int amount) {
		apdOrigins.seed(cur, level.length(), cell, amount, OffensiveEffectContext.current());
		super.seed(level, cell, amount);
	}

	@Override public void clear(int cell) { super.clear(cell); apdOrigins.clear(cell); }
	@Override public void fullyClear() { super.fullyClear(); apdOrigins.clearAll(); }
	@Override public void storeInBundle(com.watabou.utils.Bundle bundle) {
		super.storeInBundle(bundle);
		if (volume > 0) apdOrigins.store(bundle, APD_ORIGINS, cur);
		else CellDamageOrigins.clearMetadata(bundle, APD_ORIGINS);
	}
	@Override public void restoreFromBundle(com.watabou.utils.Bundle bundle) {
		super.restoreFromBundle(bundle);
		apdOrigins = CellDamageOrigins.restore(bundle, APD_ORIGINS, cur);
	}
	
	@Override
	protected void evolve() {

		apdOrigins.synchronize(cur);
		int cell;
		
		Fire fire = (Fire)Dungeon.level.blobs.get( Fire.class );
		
		for (int i = area.left-1; i <= area.right; i++) {
			for (int j = area.top-1; j <= area.bottom; j++) {
				cell = i + j*Dungeon.level.width();
				if (cur[cell] > 0) {
					
					if (fire != null && fire.volume > 0 && fire.cur[cell] > 0){
						fire.clear(cell);
						off[cell] = cur[cell] = 0;
						apdOrigins.clear(cell);
						continue;
					}
					
					Freezing.freezeAttributed(cell, apdOrigins, cell);
					
					off[cell] = cur[cell] - 1;
					volume += off[cell];
				} else {
					off[cell] = 0;
				}
			}
		}
		apdOrigins.synchronize(off);
		apdOrigins.discardIfUnattributed();
	}

	public static void freeze( int cell ) {
		freezeWithOrigins(cell, null, -1);
	}

	public static void freezeAttributed(int cell, CellDamageOrigins origins, int originCell) {
		if (origins == null) throw new IllegalArgumentException("Missing freezing origin");
		freezeWithOrigins(cell, origins, originCell);
	}

	private static void freezeWithOrigins(int cell, CellDamageOrigins origins, int originCell) {
		Char ch = Actor.findChar( cell );
		if (ch != null && !ch.isImmune(Freezing.class)) {
			TimedDamageOrigins attach = origins == null ? null : origins.timedAt(originCell, 1f);
			if (ch.buff(Frost.class) != null){
				if (attach == null) Buff.affect(ch, Frost.class, 2f);
				else Buff.affectAttributed(ch, Frost.class, 2f, attach);
			} else {
				Chill chill = ch.buff(Chill.class);
				float turnsToAdd = Dungeon.level.water[cell] ? 5f : 3f;
				if (chill != null){
					float chillToCap = Chill.DURATION - chill.cooldown();
					chillToCap /= ch.resist(Chill.class); //account for resistance to chill
					turnsToAdd = Math.min(turnsToAdd, chillToCap);
				}
				if (turnsToAdd > 0f) {
					if (attach == null) Buff.affect(ch, Chill.class, turnsToAdd);
					else Buff.affectAttributed(ch, Chill.class, turnsToAdd, attach);
				}
				if (chill != null
						&& chill.cooldown() >= Chill.DURATION &&
						!ch.isImmune(Frost.class)){
					if (attach == null) Buff.affect(ch, Frost.class, Frost.DURATION);
					else Buff.affectAttributed(ch, Frost.class, Frost.DURATION, attach);
				}
			}
		}
		
		Heap heap = Dungeon.level.heaps.get( cell );
		if (heap != null) heap.freeze();
	}
	
	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.start( SnowParticle.FACTORY, 0.05f, 0 );
	}
	
	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
	
	//legacy functionality from before this was a proper blob. Returns true if this cell is visible
	public static boolean affect( int cell ) {
		
		Char ch = Actor.findChar( cell );
		if (ch != null) {
			if (Dungeon.level.water[ch.pos]){
				Buff.prolong(ch, Frost.class, Frost.DURATION * 3);
			} else {
				Buff.prolong(ch, Frost.class, Frost.DURATION);
			}
		}

		Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);
		if (fire != null && fire.volume > 0) {
			fire.clear( cell );
		}

		MagicalFireRoom.EternalFire eternalFire = (MagicalFireRoom.EternalFire)Dungeon.level.blobs.get(MagicalFireRoom.EternalFire.class);
		if (eternalFire != null && eternalFire.volume > 0) {
			eternalFire.clear( cell );
		}
		
		Heap heap = Dungeon.level.heaps.get( cell );
		if (heap != null) {
			heap.freeze();
		}
		
		if (Dungeon.level.heroFOV[cell]) {
			CellEmitter.get( cell ).start( SnowParticle.FACTORY, 0.2f, 6 );
			return true;
		} else {
			return false;
		}
	}
}
