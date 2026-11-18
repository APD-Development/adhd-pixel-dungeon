package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;

import org.junit.Test;

import java.util.List;

public class LevelGenerationSmokeTest {

	@Test
	public void headlessLevelGenerationWorks() throws Exception {
		List<LevelGenerationHarness.LevelPrint> prints = LevelGenerationHarness.generate(12345L, 0, HeroClass.WARRIOR, LevelGenerationHarness.Context.FRESH, 3);
		assertEquals(3, prints.size());
		for (LevelGenerationHarness.LevelPrint p : prints){
			System.out.println("APD-SMOKE " + p.line());
			assertFalse(p.hash.isEmpty());
		}
	}
}
