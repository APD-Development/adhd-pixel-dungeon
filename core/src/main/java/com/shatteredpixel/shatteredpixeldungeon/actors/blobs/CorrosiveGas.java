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
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveDamageOrigin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;

public class CorrosiveGas extends Blob {

	//FIXME should have strength per-cell
	private int strength = 0;
	// Follows SPD's existing strongest-cloud strength/source policy.
	private OffensiveDamageOrigin origin=OffensiveDamageOrigin.UNATTRIBUTED;
	private static final String APD_ORIGIN="apd_corrosive_gas_origin";

	//used in specific cases where the source of the corrosion is important for death logic
	private Class source;

	@Override
	protected void evolve() {
		super.evolve();

		if (volume == 0){
			strength = 0;
			source = null;
			origin=OffensiveDamageOrigin.UNATTRIBUTED;
		} else {
			Char ch;
			int cell;

			for (int i = area.left; i < area.right; i++){
				for (int j = area.top; j < area.bottom; j++){
					cell = i + j*Dungeon.level.width();
					if (cur[cell] > 0 && (ch = Actor.findChar( cell )) != null) {
						if (!ch.isImmune(this.getClass()))
							Buff.affect(ch, Corrosion.class).setWithOrigin(2f, strength, source, origin);
					}
				}
			}
		}
	}

	public CorrosiveGas setStrength(int str){
		return setStrength(str, null);
	}

	public CorrosiveGas setStrength(int str, Class source){
		return setStrengthWithOrigin(str,source,com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveEffectContext.current());
	}

	public CorrosiveGas setStrengthAttributed(int str,Class source,Char attacker){
		return setStrengthWithOrigin(str,source,OffensiveDamageOrigin.capture(attacker,null));
	}

	private CorrosiveGas setStrengthWithOrigin(int str,Class source,OffensiveDamageOrigin role){
		if (str > strength) {
			strength = str;
			this.source = source;
			this.origin=role;
		}
		return this;
	}

	private static final String STRENGTH = "strength";
	private static final String SOURCE	= "source";

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		strength = bundle.getInt( STRENGTH );
		source = bundle.getClass( SOURCE );
		origin=OffensiveDamageOrigin.restore(bundle,APD_ORIGIN);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( STRENGTH, strength );
		bundle.put( SOURCE, source );
		origin.store(bundle,APD_ORIGIN);
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );

		emitter.pour( Speck.factory(Speck.CORROSION), 0.4f );
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
