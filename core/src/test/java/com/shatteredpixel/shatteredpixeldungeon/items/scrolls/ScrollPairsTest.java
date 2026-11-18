package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.watabou.utils.Reflection;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/** Every regular scroll has one exotic twin; every SPD scroll turns into its own runestone, and exotic scrolls never do. */
public class ScrollPairsTest {
	@Test public void everyScrollHasExactlyOneExoticTwin(){
		HeadlessTestSupport.start();
		// 12 SPD scrolls plus APD's prototype Scroll of Gathering (twin: Forwarding)
		assertEquals(13, ExoticScroll.regToExo.size());
		assertEquals(13, new HashSet<>(ExoticScroll.regToExo.values()).size());
		assertEquals(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfForwarding.class, ExoticScroll.regToExo.get(ScrollOfGathering.class));
		for (Map.Entry<Class<? extends Scroll>, Class<? extends ExoticScroll>> e : ExoticScroll.regToExo.entrySet())
			assertEquals(e.getKey(), ExoticScroll.exoToReg.get(e.getValue()));
	}

	@SuppressWarnings("unchecked")
	@Test public void everyScrollMakesItsOwnStone() throws Exception {
		HeadlessTestSupport.start();
		Field f = Scroll.ScrollToStone.class.getDeclaredField("stones");
		f.setAccessible(true);
		Map<Class<? extends Scroll>, Class<? extends Runestone>> stones = (Map<Class<? extends Scroll>, Class<? extends Runestone>>) f.get(null);
		// Scroll of Gathering is a prototype with no runestone yet (not approved), so it is the only scroll without one
		HashSet<Class<? extends Scroll>> expected = new HashSet<>(ExoticScroll.regToExo.keySet());
		expected.remove(ScrollOfGathering.class);
		assertEquals(expected, stones.keySet());
		assertEquals(12, new HashSet<>(stones.values()).size());
		Scroll.ScrollToStone r = new Scroll.ScrollToStone();
		for (Class<? extends Scroll> c : stones.keySet()){
			ArrayList<Item> in = new ArrayList<>();
			in.add(Reflection.newInstance(c));
			assertTrue(c.getSimpleName(), r.testIngredients(in));
			assertEquals(0, r.cost(in));
		}
		for (Class<? extends ExoticScroll> c : ExoticScroll.exoToReg.keySet()){
			ArrayList<Item> in = new ArrayList<>();
			in.add(Reflection.newInstance(c));
			assertFalse(c.getSimpleName() + " should not make a stone", r.testIngredients(in));
		}
	}
}
