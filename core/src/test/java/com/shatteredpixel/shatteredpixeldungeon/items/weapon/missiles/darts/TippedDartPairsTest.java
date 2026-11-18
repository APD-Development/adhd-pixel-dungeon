package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import org.junit.Test;

import java.util.HashSet;

/** Every seed the dungeon drops tips its own dart, and random tipped darts come out in the asked quantity. */
public class TippedDartPairsTest {
	@Test public void everyDroppableSeedTipsItsOwnDart(){
		HeadlessTestSupport.start();
		for (Class<?> c : Generator.Category.SEED.classes){
			assertTrue(c.getSimpleName() + " has no dart", TippedDart.types.containsKey(c));
			TippedDart d = TippedDart.getTipped((Plant.Seed) Reflection.newInstance(c), 3);
			assertEquals(3, d.quantity());
			assertEquals(TippedDart.types.get(c), d.getClass());
		}
		assertEquals(TippedDart.types.size(), new HashSet<>(TippedDart.types.values()).size());
	}

	@Test public void randomTippedDartsAreRealTypes(){
		HeadlessTestSupport.start();
		Random.pushGenerator(99L);
		try {
			for (int i = 0; i < 50; i++){
				TippedDart d = TippedDart.randomTipped(2);
				assertTrue(TippedDart.types.containsValue(d.getClass()));
				assertEquals(2, d.quantity());
			}
		} finally { Random.popGenerator(); }
	}
}
