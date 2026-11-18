package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

import org.junit.Test;

import java.util.Arrays;

public class AmuletRevealTest {
	@Test public void nothingToRevealWithoutALevelOrHero(){
		assertEquals(0, AmuletReveal.reveal(null));
		assertFalse(AmuletReveal.applies(null));
	}

	@Test public void onlyAppliesWhileCarryingTheAmulet(){
		HeadlessTestSupport.start();
		Hero hero = new Hero();
		assertFalse(AmuletReveal.applies(hero));
		hero.belongings.backpack.items.add(new Amulet());
		assertTrue(AmuletReveal.applies(hero));
	}

	@Test public void mapsEveryDiscoverableCellButLeavesSecretsHidden(){
		HeadlessTestSupport.start();
		SewerLevel level = new SewerLevel();
		level.setSize(8, 8);
		level.discoverable = new boolean[level.length()];
		Arrays.fill(level.discoverable, true);
		level.discoverable[0] = false;
		level.map[10] = Terrain.SECRET_DOOR;
		level.mapped[5] = true;
		int added = AmuletReveal.reveal(level);
		assertEquals(64 - 1 - 1, added);
		assertFalse("undiscoverable cells stay unmapped", level.mapped[0]);
		assertTrue(level.mapped[10]);
		assertEquals("secret door is not opened", Terrain.SECRET_DOOR, level.map[10]);
		assertEquals("second visit adds nothing", 0, AmuletReveal.reveal(level));
	}
}
