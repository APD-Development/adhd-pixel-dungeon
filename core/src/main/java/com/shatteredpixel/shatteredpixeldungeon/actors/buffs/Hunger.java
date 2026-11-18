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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdCombat;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SaltCube;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

public class Hunger extends Buff implements Hero.Doom {

	public static final float HUNGRY	= 300f;
	public static final float STARVING	= 450f;

	private float level;
	private float partialDamage;

	private static final String LEVEL			= "level";
	private static final String PARTIALDAMAGE 	= "partialDamage";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put( LEVEL, level );
		bundle.put( PARTIALDAMAGE, partialDamage );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		level = bundle.getFloat( LEVEL );
		partialDamage = bundle.getFloat(PARTIALDAMAGE);
	}

	@Override
	public boolean act() {

		if (Dungeon.level.locked
				|| target.buff(WellFed.class) != null
				|| SPDSettings.intro()
				|| target.buff(ScrollOfChallenge.ChallengeArena.class) != null){
			spend(TICK);
			return true;
		}

		if (target.isAlive() && target instanceof Hero) {

			Hero hero = (Hero)target;

			if (isStarving()) {

				partialDamage += target.HT/1000f;

				if (partialDamage > 1){
					target.damage( (int)partialDamage, this);
					partialDamage -= (int)partialDamage;
				}
				
			} else {

				float hungerDelay = 1f;
				if (target.buff(Shadows.class) != null){
					hungerDelay *= 1.5f;
				}
				hungerDelay /= SaltCube.hungerGainMultiplier();

				float newLevel = level + com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomResourceHooks.hungerAdvance(1f/hungerDelay);
				if (newLevel >= STARVING) {

					GLog.n( Messages.get(this, "onstarving") );
					hero.damage( 1, this );

                    if(com.shatteredpixel.shatteredpixeldungeon.apd.qol.WaitingPolicy.interruptOnStarving(hero))hero.interrupt();
					newLevel = STARVING;

				} else if (newLevel >= HUNGRY && level < HUNGRY) {

					GLog.w( Messages.get(this, "onhungry") );

					if (!Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_FOOD)){
						GameScene.flashForDocument(Document.ADVENTURERS_GUIDE, Document.GUIDE_FOOD);
					}

				}
				level = newLevel;

			}
			
			spend( TICK );

		} else {

			diactivate();

		}

		return true;
	}

	public void satisfy( float energy ) {
		affectHunger( energy, false );
	}

	public void affectHunger(float energy ){
		affectHunger( energy, false );
	}

	public void affectHunger(float energy, boolean overrideLimits ) {
		energy = com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomResourceHooks.hungerEnergy(energy);

		if (energy < 0 && target.buff(WellFed.class) != null){
			target.buff(WellFed.class).left += energy;
			BuffIndicator.refreshHero();
			return;
		}

		float oldLevel = level;

		level -= energy;
		if (level < 0 && !overrideLimits) {
			level = 0;
		} else if (level > STARVING) {
			float excess = level - STARVING;
			level = STARVING;
			partialDamage += excess * (target.HT/1000f);
			if (partialDamage > 1f){
				target.damage( (int)partialDamage, this );
				partialDamage -= (int)partialDamage;
			}
		}

		if (oldLevel < HUNGRY && level >= HUNGRY){
			GLog.w( Messages.get(this, "onhungry") );
		} else if (oldLevel < STARVING && level >= STARVING){
			GLog.n( Messages.get(this, "onstarving") );
			target.damage( 1, this );
		}

		ApdCombat.onHungerChanged( energy, (int)Math.ceil(oldLevel), hunger() );
		BuffIndicator.refreshHero();
	}

    /** Public status values only; preview never advances starvation or requests damage. */
    public float hungerPoints(){return level;}
    public float starvationDamageProgress(){return partialDamage;}
    public int nextStarvationInput(){
        if(target==null || !isStarving())return 0;
        float progress=partialDamage+target.HT/1000f;
        return progress>1 ? (int)progress : 0;
    }

    public boolean isHungry(){return level>=HUNGRY;}

	public boolean isStarving() {
		return level >= STARVING;
	}

	/** APD: hunger gained per normal turn right now (Salt Cube and custom rules; ignores temporary Shadows). */
	public static float perTurn(){
		return com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomResourceHooks.hungerAdvance(SaltCube.hungerGainMultiplier());
	}

	/** APD: turns until Hungry at the current rate; 0 when already Hungry, -1 when hunger never rises. */
	public int turnsUntilHungry(){
		if (level >= HUNGRY) return 0;
		//the same pauses act() makes: a locked boss arena, the tutorial, a challenge arena
		if ((Dungeon.level != null && Dungeon.level.locked) || SPDSettings.intro()
				|| (target != null && target.buff(ScrollOfChallenge.ChallengeArena.class) != null)) return -1;
		float rate = perTurn();
		if (!(rate > 0)) return -1;
		int turns = (int)Math.ceil((HUNGRY - level) / rate);
		//Well Fed holds hunger still until it runs out
		WellFed fed = target == null ? null : target.buff(WellFed.class);
		if (fed != null) turns += Math.max(0, fed.remainingNutritionUpdates());
		return turns;
	}

	public int hunger() {
		return (int)Math.ceil(level);
	}

	@Override
	public int icon() {
		if (level < HUNGRY) {
			return BuffIndicator.NONE;
		} else if (level < STARVING) {
			return BuffIndicator.HUNGER;
		} else {
			return BuffIndicator.STARVATION;
		}
	}

	@Override
	public String name() {
		if (level < STARVING) {
			return Messages.get(this, "hungry");
		} else {
			return Messages.get(this, "starving");
		}
	}

	@Override
	public String desc() {
		String result;
		if (level < STARVING) {
			result = Messages.get(this, "desc_intro_hungry");
		} else {
			result = Messages.get(this, "desc_intro_starving");
		}

		result += Messages.get(this, "desc");

		return result;
	}

	@Override
	public void onDeath() {

		Badges.validateDeathFromHunger();

		Dungeon.fail( this );
		GLog.n( Messages.get(this, "ondeath") );
	}
}

