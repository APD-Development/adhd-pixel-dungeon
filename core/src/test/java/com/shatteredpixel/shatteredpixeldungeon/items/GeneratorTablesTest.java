package com.shatteredpixel.shatteredpixeldungeon.items;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;

import org.junit.Test;

/** Every loot table lines up: one weight per item, no negative weights, and every listed item belongs to its category. */
public class GeneratorTablesTest {
	private static void checkDeck(Generator.Category c, float[] probs, String name){
		if (probs == null) return;
		assertEquals(c + " " + name + " length", c.classes.length, probs.length);
		for (float p : probs) assertTrue(c + " " + name + " has a negative weight", p >= 0);
	}

	@Test public void tablesLineUp(){
		HeadlessTestSupport.start();
		Generator.fullReset();
		for (Generator.Category c : Generator.Category.values()){
			assertNotNull(c + " has no classes", c.classes);
			if (c.classes.length == 0) continue; // WEAPON / MISSILE parents hand off to their tier tables
			checkDeck(c, c.probs, "probs");
			checkDeck(c, c.defaultProbs, "defaultProbs");
			checkDeck(c, c.defaultProbs2, "defaultProbs2");
			checkDeck(c, c.defaultProbsTotal, "defaultProbsTotal");
			for (Class<?> k : c.classes){
				assertTrue(k.getSimpleName() + " listed under " + c, c.superClass.isAssignableFrom(k));
			}
		}
	}

	@Test public void drawsMatchTheirCategory(){
		HeadlessTestSupport.start();
		Generator.fullReset();
		com.watabou.utils.Random.pushGenerator(1118L);
		try {
			for (Generator.Category c : Generator.Category.values()){
				if (c == Generator.Category.GOLD) continue;
				for (int i = 0; i < 20; i++){
					Item it = Generator.randomUsingDefaults(c);
					assertNotNull(c + " gave nothing", it);
					// once artifacts run out the game hands out a ring instead (SPD rule)
					boolean ok = c.superClass.isInstance(it) || (c == Generator.Category.ARTIFACT && it instanceof com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring);
					assertTrue(it.getClass().getSimpleName() + " drawn from " + c, ok);
				}
			}
		} finally { com.watabou.utils.Random.popGenerator(); }
	}
}
