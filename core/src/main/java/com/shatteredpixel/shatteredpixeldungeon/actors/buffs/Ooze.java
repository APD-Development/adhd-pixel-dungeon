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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveDamageOrigin;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveDamage;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

public class Ooze extends Buff implements Buff.DOTbuff {

	public static final float DURATION = 20f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	private float left;
	private OffensiveDamageOrigin origin=OffensiveDamageOrigin.UNATTRIBUTED;
	private static final String APD_ORIGIN="apd_ooze_origin";
	private boolean acted = false; //whether the debuff has done any damage at all yet

	public float apdRemaining(){return left;}
    public int apdNextRaw(){
        if (Dungeon.scalingDepth()>5)return 1+Dungeon.scalingDepth()/5;
        return Dungeon.scalingDepth()==5 || (int)left%2==0 ? 1 : 0;
    }
    private static final String LEFT	= "left";
	private static final String ACTED   = "acted";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEFT, left );
		bundle.put( ACTED, acted );
		origin.store(bundle,APD_ORIGIN);
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		left = bundle.getFloat(LEFT);
		acted = bundle.getBoolean(ACTED);
		origin=OffensiveDamageOrigin.restore(bundle,APD_ORIGIN);
	}
	
	@Override
	public int icon() {
		return BuffIndicator.OOZE;
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - left) / DURATION);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int)left);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns(left));
	}
	
	public void set(float left){
		setWithOrigin(left,OffensiveDamageOrigin.UNATTRIBUTED);
	}

	public void setAttributed(float left,Char attacker){
		setWithOrigin(left,OffensiveDamageOrigin.capture(attacker,null));
	}

	public void setWithOrigin(float left,OffensiveDamageOrigin role){
        if(role==null)throw new IllegalArgumentException("Missing ooze origin");
		origin=role;
		this.left = left;
		acted = false;
		if (target != null) target.needsIncomingDOTUpdate = true;
	}

	public void extend( float duration ) {
		left += duration;
		if (target != null) target.needsIncomingDOTUpdate = true;
	}

	@Override
	public boolean act() {
		//washing away happens before debuff effects if debuff has gotten to act
		if (acted && Dungeon.level.water[target.pos] && !target.flying){
			detach();
		} else if (target.isAlive()) {

			acted = true;
			if (Dungeon.scalingDepth() > 5) {
				OffensiveDamage.raw(1 + Dungeon.scalingDepth() / 5,this,origin).apply(target);
			} else if (Dungeon.scalingDepth() == 5){
				OffensiveDamage.raw(1,this,origin).apply(target); //1 dmg per turn vs Goo
			} else if ((int)left % 2 == 0) {
				OffensiveDamage.raw(1,this,origin).apply(target); //0.5 dmg per turn in sewers
			}

			if (!target.isAlive() && target == Dungeon.hero) {
				Dungeon.fail( this );
				GLog.n( Messages.get(this, "ondeath") );
			}
			spend( TICK );
			left -= TICK;
			if (left <= 0){
				detach();
			}
		} else {
			detach();
		}
		if (Dungeon.level.water[target.pos] && !target.flying){
			detach();
		}
		target.needsIncomingDOTUpdate = true;
		return true;
	}

	@Override
	public void detach() {
		target.needsIncomingDOTUpdate = true;
		super.detach();
	}

	@Override
	public int totalIncomingDMG() {
		if(origin.percentage()!=100){
			int total=0;
			for(float remaining=left;remaining>0;remaining-=TICK){
				int raw=Dungeon.scalingDepth()>5?1+Dungeon.scalingDepth()/5:Dungeon.scalingDepth()==5 || (int)remaining%2==0?1:0;
				total+=OffensiveDamage.raw(raw,this,origin).scaled().amount;
			}
			return total;
		}
		if (Dungeon.scalingDepth() > 5) {
			int dmg = 1 + Dungeon.scalingDepth() / 5;
			return (int)(Math.ceil(left)*dmg);
		} else if (Dungeon.scalingDepth() == 5){
			return (int)(Math.ceil(left));
		} else {
			return (int)(Math.ceil(left)/2);
		}
	}
}
