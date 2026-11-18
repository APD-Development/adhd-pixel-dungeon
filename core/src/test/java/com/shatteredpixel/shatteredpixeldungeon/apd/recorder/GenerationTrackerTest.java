package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

/** Generator observation only: nested generator calls count as one draw, and nothing is drawn for a null result. */
public class GenerationTrackerTest {
	@Before public void setUp(){ HeadlessTestSupport.start(); }

	private static int intField(GenerationTracker t, String name) throws Exception {
		Field f = GenerationTracker.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(t);
	}

	@Test public void onlyTheOutermostCallIsADraw() throws Exception {
		GenerationTracker t = new GenerationTracker(null, null);
		int outer = t.begin();
		int inner = t.begin();
		assertEquals(0, outer);
		assertEquals(1, inner);
		t.end(inner, 0, Generator.Category.WEAPON, new Sword());
		assertEquals("inner call is not a draw", 0, intField(t, "draws"));
		t.end(outer, 0, null, new Sword());
		assertEquals(1, intField(t, "draws"));
		assertEquals(0, intField(t, "depth"));
	}

	@Test public void aNullResultIsNotADraw() throws Exception {
		GenerationTracker t = new GenerationTracker(null, null);
		t.end(t.begin(), 0, Generator.Category.FOOD, null);
		assertEquals(0, intField(t, "draws"));
		t.end(t.begin(), 0, Generator.Category.FOOD, new Food());
		assertEquals(1, intField(t, "draws"));
	}

	@Test public void depthNeverGoesNegative() throws Exception {
		GenerationTracker t = new GenerationTracker(null, null);
		t.end(-5, 0, null, null);
		assertEquals(0, intField(t, "depth"));
	}
}
