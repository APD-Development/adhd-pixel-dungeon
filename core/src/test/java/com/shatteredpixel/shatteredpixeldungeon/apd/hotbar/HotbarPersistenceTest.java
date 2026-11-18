package com.shatteredpixel.shatteredpixeldungeon.apd.hotbar;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;

import org.junit.Test;

/** A manual hotbar change only saves when there is a live, ready hero in a real save slot. */
public class HotbarPersistenceTest {
	@Test public void refusesWithoutALiveReadyHeroInASlot(){
		HeadlessTestSupport.start();
		Hero oldHero = Dungeon.hero;
		int oldSlot = GamesInProgress.curSlot;
		try {
			Dungeon.hero = null;
			assertFalse(HotbarPersistence.saveManualAssignment());
			Hero hero = new Hero();
			hero.HP = hero.HT = 10;
			hero.ready = false;
			Dungeon.hero = hero;
			assertFalse("not ready", HotbarPersistence.saveManualAssignment());
			hero.ready = true;
			GamesInProgress.curSlot = 0;
			assertFalse("no save slot", HotbarPersistence.saveManualAssignment());
		} finally {
			Dungeon.hero = oldHero;
			GamesInProgress.curSlot = oldSlot;
		}
	}
}
