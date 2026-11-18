package com.shatteredpixel.shatteredpixeldungeon.items.food;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.FoodExplanation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

/** Scheduled Nov 18: each food's "Refills N hunger" equals what eating it actually removes. */
public class FoodRefillMatchesEatingTest {
	private Hero oldHero;
	private int oldChallenges;

	@Before public void setUp(){
		HeadlessTestSupport.start();
		oldHero = Dungeon.hero;
		oldChallenges = Dungeon.challenges;
	}

	@After public void tearDown(){
		Dungeon.hero = oldHero;
		Dungeon.challenges = oldChallenges;
	}

	private static Field level() throws Exception {
		Field f = Hunger.class.getDeclaredField("level");
		f.setAccessible(true);
		return f;
	}

	/** Hunger removed by the real satisfy() from a starving hero. */
	private static float eaten(Food food) throws Exception {
		Hero hero = new Hero();
		hero.HP = hero.HT = 20;
		Dungeon.hero = hero;
		Hunger hunger = Buff.affect(hero, Hunger.class);
		level().setFloat(hunger, Hunger.STARVING);
		food.satisfy(hero);
		return Hunger.STARVING - level().getFloat(hunger);
	}

	private static Food[] plainFoods(){
		return new Food[]{ new Food(), new SmallRation(), new Pasty(),
				new ChargrilledMeat(), new StewedMeat() };
	}

	private static void check(Food food) throws Exception {
		float promised = FoodExplanation.nutritionBeforeHorn(food);
		float removed = eaten(food);
		assertEquals(food.getClass().getSimpleName(), Math.min(promised, Hunger.STARVING), removed, 0.01f);
		String summary = FoodExplanation.summary(food);
		assertTrue(summary, summary.startsWith("Refills _"));
	}

	@Test public void normalRules() throws Exception {
		Dungeon.challenges = 0;
		for (Food food : plainFoods()) check(food);
	}

	@Test public void onDietReductionIsIncluded() throws Exception {
		Dungeon.challenges = Challenges.NO_FOOD;
		for (Food food : plainFoods()) check(food);
		assertTrue(FoodExplanation.summary(new Food()).contains("On Diet's reduction is included."));
	}
}
