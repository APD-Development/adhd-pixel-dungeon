package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CustomBossHealthAndDefenseHooksTest {
	@Before public void setUp(){ HeadlessTestSupport.start(); }
	@After public void tearDown(){ rules(CustomDungeonRules.defaults()); }

	private static void rules(CustomDungeonRules r){
		Bundle b = new Bundle(), inner = new Bundle();
		r.store(inner);
		b.put("apd_custom_rules", inner);
		Dungeon.apdRestoreCustomRules(b);
	}

	@Test public void defaultRulesChangeNothing(){
		rules(CustomDungeonRules.defaults());
		assertEquals(250, CustomBossHealthHooks.threshold(250));
		assertEquals(7, CustomDefenseHooks.scale(new Hero(), 7));
		Rat rat = new Rat(); rat.alignment = Char.Alignment.ENEMY;
		assertEquals(7, CustomDefenseHooks.scale(rat, 7));
	}

	@Test public void bossHealthScalesButNeverDropsToZero(){
		rules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.BOSS_HP, 500));
		assertEquals(1250, CustomBossHealthHooks.threshold(250));
		assertEquals("non-positive thresholds pass through", 0, CustomBossHealthHooks.threshold(0));
		assertEquals(-3, CustomBossHealthHooks.threshold(-3));
	}

	@Test public void defenseScalesHeroAndEnemiesSeparatelyAndLeavesAlliesAlone(){
		rules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_DEFENSE, 500));
		Rat enemy = new Rat(); enemy.alignment = Char.Alignment.ENEMY;
		Rat ally = new Rat(); ally.alignment = Char.Alignment.ALLY;
		assertEquals(35, CustomDefenseHooks.scale(enemy, 7));
		assertEquals("allies use their own roll", 7, CustomDefenseHooks.scale(ally, 7));
		assertEquals("player defense unchanged", 7, CustomDefenseHooks.scale(new Hero(), 7));
		assertEquals(7, CustomDefenseHooks.scale(null, 7));
	}
}
