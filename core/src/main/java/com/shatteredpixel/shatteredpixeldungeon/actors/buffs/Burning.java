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

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Thief;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Brimstone;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.food.ChargrilledMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveDamageOrigin;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.TimedDamageOrigins;

public class Burning extends Buff implements Hero.Doom, Buff.DOTbuff {
	
	private static final float DURATION = 8f;
	
	private float left;
	private TimedDamageOrigins origins;
	private boolean acted = false; //whether the debuff has done any damage at all yet
	private int burnIncrement = 0; //for tracking burning of hero items
	private int nextHit = 0; //we pre-caulcate the incoming hit for a bit more accuracy in totalIncomingDMG()
	
	/** Display-only view; never calls the RNG unlike legacy totalIncomingDMG(). */
    public float apdRemaining(){return left;}
    public int apdNextRaw(){return nextHit;}
    public int apdRandomMaximum(){return 3+Dungeon.scalingDepth()/4;}
    private static final String LEFT	= "left";
	private static final String ACTED	= "acted";
	private static final String BURN	= "burnIncrement";
	private static final String NEXT_DMG= "next_dmg";

	{
		type = buffType.NEGATIVE;
		announced = true;
	}
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEFT, left );
		bundle.put( ACTED, acted );
		bundle.put( BURN, burnIncrement );
		bundle.put( NEXT_DMG, nextHit );
		if(origins!=null && left>=0) origins.store(bundle,"apd_burn_origins"); else TimedDamageOrigins.clear(bundle,"apd_burn_origins");
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		left = bundle.getFloat( LEFT );
		acted = bundle.getBoolean( ACTED );
		burnIncrement = bundle.getInt( BURN );
		nextHit = bundle.getInt( NEXT_DMG );
		origins=TimedDamageOrigins.restore(bundle,"apd_burn_origins",left);
	}

	@Override
	public boolean attachTo(Char target) {
		Buff.detach( target, Chill.class);

		return super.attachTo(target);
	}

	@Override
	public boolean act() {

		if (acted && Dungeon.level.water[target.pos] && !target.flying){
			detach();
		} else if (target.isAlive() && !target.isImmune(getClass())) {

			acted = true;
			if (nextHit == 0){
				nextHit = Random.NormalIntRange( 1, 3 + Dungeon.scalingDepth()/4 );
			}
			int damage = origins==null ? nextHit : origins.scale(nextHit);
			nextHit = Random.NormalIntRange( 1, 3 + Dungeon.scalingDepth()/4 );
			Buff.detach( target, Chill.class);

			if (target instanceof Hero
					&& target.buff(TimekeepersHourglass.timeStasis.class) == null
					&& target.buff(TimeStasis.class) == null) {
				
				Hero hero = (Hero)target;

				hero.damage( damage, this );
				burnIncrement++;

				//at 4+ turns, there is a (turns-3)/3 chance an item burns
				//...except in the vault level, as the player can't access the scroll holder there
				if (Random.Int(3) < (burnIncrement - 3) && !(Dungeon.level instanceof VaultLevel)){
					burnIncrement = 0;

					ArrayList<Item> burnable = new ArrayList<>();
					//does not reach inside of containers
					if (!hero.belongings.lostInventory()) {
						for (Item i : hero.belongings.backpack.items) {
							if (!i.unique && (i instanceof Scroll || i instanceof MysteryMeat || i instanceof FrozenCarpaccio)) {
								burnable.add(i);
							}
						}
					}

					if (!burnable.isEmpty()){
						Item toBurn = Random.element(burnable).detach(hero.belongings.backpack);
						GLog.w( Messages.capitalize(Messages.get(this, "burnsup", toBurn.title())) );
						if (toBurn instanceof MysteryMeat || toBurn instanceof FrozenCarpaccio){
							ChargrilledMeat steak = new ChargrilledMeat();
							if (!steak.collect( hero.belongings.backpack )) {
								Dungeon.level.drop( steak, hero.pos ).sprite.drop();
							}
						}
						Heap.burnFX( hero.pos );
					}
				}
				
			} else {
				target.damage( damage, this );
			}

			if (target instanceof Thief && ((Thief) target).item != null) {

				Item item = ((Thief) target).item;

				if (!item.unique && item instanceof Scroll) {
					target.sprite.emitter().burst( ElmoParticle.FACTORY, 6 );
					((Thief)target).item = null;
				} else if (item instanceof MysteryMeat) {
					target.sprite.emitter().burst( ElmoParticle.FACTORY, 6 );
					((Thief)target).item = new ChargrilledMeat();
				}

			}

		} else {

			detach();
		}
		
		if (Dungeon.level.flamable[target.pos] && Blob.volumeAt(target.pos, Fire.class) == 0) {
			if (origins == null) GameScene.add( Blob.seed( target.pos, 4, Fire.class ) );
			else GameScene.add( Fire.seedAttributedMixture( target.pos, 4, origins.currentShares() ) );
		}
		
		spend( TICK );
		if(origins!=null) origins.consume(TICK);
		left -= TICK;
		
		if (left <= 0 ||
			(Dungeon.level.water[target.pos] && !target.flying)) {
			
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
	
	public static Burning affectAttributed(Char ch, Char attacker) {
		return affectAttributed(ch, DURATION, attacker);
	}

	public static Burning affectAttributed(Char ch, float duration, Char attacker) {
		return affectAttributedOrigin(ch, duration, OffensiveDamageOrigin.capture(attacker, null));
	}

	public static Burning affectAttributedOrigin(Char ch, OffensiveDamageOrigin origin) {
		return affectAttributedOrigin(ch, DURATION, origin);
	}

	public static Burning affectAttributedOrigin(Char ch, float duration, OffensiveDamageOrigin origin) {
		return affectPrepared(ch, duration, TimedDamageOrigins.single(duration,
				origin == null ? OffensiveDamageOrigin.UNATTRIBUTED : origin));
	}

	public static Burning affectAttributed(Char ch,
			com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CellDamageOrigins field, int cell) {
		return affectPrepared(ch, DURATION, field.timedAt(cell, DURATION));
	}

	private static Burning affectPrepared(Char ch, float duration, TimedDamageOrigins cause) {
		Burning burning = ch.buff(Burning.class);
		if (burning == null) {
			burning = new Burning();
			burning.reigniteWithOrigins(ch, duration, cause);
			burning.attachTo(ch);
		} else {
			burning.reigniteWithOrigins(ch, duration, cause);
		}
		return burning;
	}

	@Override
	public int apdScaleAttachDamage(int amount) {
		return origins == null ? super.apdScaleAttachDamage(amount) : origins.scale(amount);
	}

	public void reignite( Char ch ) {
		reignite( ch, DURATION );
	}
	
	public void reignite( Char ch, float duration ) {
		reigniteWithOrigin(ch,duration,OffensiveDamageOrigin.UNATTRIBUTED);
	}
	public void reigniteAttributed(Char ch,Char attacker){reigniteAttributed(ch,DURATION,attacker);}
	public void reigniteAttributedOrigin(Char ch,OffensiveDamageOrigin origin){reigniteWithOrigin(ch,DURATION,origin==null?OffensiveDamageOrigin.UNATTRIBUTED:origin);}
	public void reigniteAttributed(Char ch,float duration,Char attacker){reigniteWithOrigin(ch,duration,OffensiveDamageOrigin.capture(attacker,null));}
	public void reigniteAttributed(Char ch,com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CellDamageOrigins field,int cell){
		reigniteWithOrigins(ch,DURATION,field.timedAt(cell,DURATION));
	}
	private void reigniteWithOrigin(Char ch,float duration,OffensiveDamageOrigin origin){
		reigniteWithOrigins(ch,duration,TimedDamageOrigins.single(duration,origin));
	}
	private void reigniteWithOrigins(Char ch,float duration,TimedDamageOrigins cause){
		if (ch.isImmune(Burning.class)){
			if (ch.glyphLevel(Brimstone.class) >= 0){
				//generate avg of 1 shield per turn per 50% boost, to a max of 4x boost
				float shieldChance = 2*(Armor.Glyph.genericProcChanceMultiplier(ch) - 1f);
				int shieldCap = Math.round(shieldChance*4f);
				int shieldGain = (int)shieldChance;
				if (Random.Float() < shieldChance%1) shieldGain++;
				if (shieldCap > 0 && shieldGain > 0){
					Barrier barrier = Buff.affect(ch, Barrier.class);
					if (barrier.shielding() < shieldCap){
						barrier.incShield(Math.min(shieldGain, shieldCap - barrier.shielding()));
					}
				}
			}
		}
		if (left < duration) {
			left = duration;
			origins=cause;
			ch.needsIncomingDOTUpdate = true;
		}
		acted = false;
	}

	public void extend( float duration ) {extendWithOrigin(duration,OffensiveDamageOrigin.UNATTRIBUTED);}
	public void extendAttributed(float duration,Char attacker){extendWithOrigin(duration,OffensiveDamageOrigin.capture(attacker,null));}
	private void extendWithOrigin(float duration,OffensiveDamageOrigin origin){
		if(origins==null) origins=TimedDamageOrigins.single(Math.max(0,left),OffensiveDamageOrigin.UNATTRIBUTED);
		if(duration>0) origins.extend(duration,origin);
		else if(duration<0) origins.shorten(-duration);
		left += duration;
		if (target != null) target.needsIncomingDOTUpdate = true;
	}
	
	@Override
	public int icon() {
		return BuffIndicator.FIRE;
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
	public void fx(boolean on) {
		if (on) target.sprite.add(CharSprite.State.BURNING);
		else target.sprite.remove(CharSprite.State.BURNING);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns(left));
	}

	@Override
	public void onDeath() {
		
		Badges.validateDeathFromFire();
		
		Dungeon.fail( this );
		GLog.n( Messages.get(this, "ondeath") );
	}

	@Override
	public int totalIncomingDMG() {
		if (nextHit == 0){
			nextHit = Random.NormalIntRange( 1, 3 + Dungeon.scalingDepth()/4 );
		}
		float avgDmg = 2 + Dungeon.scalingDepth()/8f;
		if(origins==null || !origins.modifiesDamage()) return (int)Math.round(nextHit + Math.ceil(left-1)*avgDmg);
		TimedDamageOrigins forecast=origins.copy();
		double expected=forecast.scale(nextHit);forecast.consume(TICK);
		int max=3+Dungeon.scalingDepth()/4, outcomes=max;
		for(int ticks=(int)Math.ceil(left-1);ticks>0;ticks--){
			for(int raw=1;raw<=max;raw++){
				double low=(raw-1)/(double)outcomes,high=raw/(double)outcomes;
				expected+=(triangularCdf(high)-triangularCdf(low))*forecast.scale(raw);
			}
			forecast.consume(TICK);
		}
		return (int)Math.min(Integer.MAX_VALUE,Math.round(expected));
	}

	private static double triangularCdf(double x){return x<=0?0:x>=1?1:x<=.5?2*x*x:1-2*(1-x)*(1-x);}
}

