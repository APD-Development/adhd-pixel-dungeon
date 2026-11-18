package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StormCloud;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ChaoticCenser;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.Assert.*;

public class StormCloudDamageProvenanceTest {

	private static final int CELL = 112;

	private static class Quiet extends CharSprite {
		@Override public void showStatus(int color, String text, Object... args) {}
		@Override public void showStatusWithIcon(int color, String text, int icon, Object... args) {}
	}

	private static class FieryTarget extends Char {
		Object source;
		int calls;
		FieryTarget() {
			HP = HT = 1000;
			pos = CELL;
			sprite = new Quiet();
			properties.add(Property.FIERY);
		}
		@Override public void damage(int damage, Object source) {
			this.source = source;
			calls++;
			super.damage(damage, source);
		}
	}

	private Hero hero;
	private Level level;
	private int depth;
	private Bundle rules;
	private Set<Actor> actors;

	@Before
	public void setup() throws Exception {
		HeadlessTestSupport.start();
		hero = Dungeon.hero;
		level = Dungeon.level;
		depth = Dungeon.depth;
		rules = new Bundle();
		Dungeon.apdStoreCustomRules(rules);
		actors = Actor.all();
		Dungeon.hero = new Hero();
		Dungeon.hero.HP = Dungeon.hero.HT = 100;
		Dungeon.hero.pos = 18;
		Dungeon.hero.sprite = new Quiet();
		new Group().add(Dungeon.hero.sprite);
		Dungeon.depth = 14;
		Dungeon.level = new SewerLevel();
		java.lang.reflect.Method size = Level.class.getDeclaredMethod("setSize", int.class, int.class);
		size.setAccessible(true);
		size.invoke(Dungeon.level, 15, 15);
		java.util.Arrays.fill(Dungeon.level.map, Terrain.EMPTY);
		java.util.Arrays.fill(Dungeon.level.passable, true);
		// Real generated levels initialize these collections before Storm Cloud
		// can convert a cell to water. Keep the headless fixture on that real path.
		Dungeon.level.transitions = new java.util.ArrayList<>();
		Dungeon.level.mobs = new java.util.HashSet<>();
		Dungeon.level.heaps = new com.watabou.utils.SparseArray<>();
		Dungeon.level.blobs = new java.util.HashMap<>();
		Dungeon.level.plants = new com.watabou.utils.SparseArray<>();
		Dungeon.level.traps = new com.watabou.utils.SparseArray<>();
		Dungeon.level.customTiles = new java.util.ArrayList<>();
		Dungeon.level.customTerrain = new java.util.ArrayList<>();
		Dungeon.level.customWalls = new java.util.ArrayList<>();
	}

	@After
	public void cleanup() {
		for (Actor actor : Actor.all()) if (!actors.contains(actor)) Actor.remove(actor);
		Dungeon.hero = hero;
		Dungeon.level = level;
		Dungeon.depth = depth;
		Dungeon.apdRestoreCustomRules(rules);
		assertEquals(OffensiveDamageOrigin.UNATTRIBUTED, OffensiveEffectContext.current());
	}

	private void rules(OffensiveDamageOrigin origin, int percent) {
		Bundle saved = new Bundle();
		Bundle values = new Bundle();
		CustomDungeonRules.defaults()
				.with(CustomDungeonRules.Slider.PLAYER_DAMAGE, origin == OffensiveDamageOrigin.PLAYER ? percent : 100)
				.with(CustomDungeonRules.Slider.ENEMY_DAMAGE, origin == OffensiveDamageOrigin.ENEMY ? percent : 100)
				.with(CustomDungeonRules.Slider.BOSS_DAMAGE, origin == OffensiveDamageOrigin.BOSS ? percent : 100)
				.store(values);
		saved.put("apd_custom_rules", values);
		Dungeon.apdRestoreCustomRules(saved);
	}

	private StormCloud seed(int amount, OffensiveDamageOrigin origin) {
		try (OffensiveEffectContext ignored = OffensiveEffectContext.enter(origin)) {
			return Blob.seed(CELL, amount, StormCloud.class);
		}
	}

	private int rawDamage() {
		return 1 + Dungeon.scalingDepth() / 5;
	}

	@Test
	public void actualTicksUseEveryCreatorAndDamagePercentageExactlyOnce() {
		for (OffensiveDamageOrigin origin : new OffensiveDamageOrigin[]{OffensiveDamageOrigin.PLAYER, OffensiveDamageOrigin.ENEMY, OffensiveDamageOrigin.BOSS}) {
			for (int percent = 25; percent <= 500; percent += 25) {
				rules(origin, percent);
				FieryTarget target = new FieryTarget();
				Actor.add(target);
				StormCloud cloud = seed(300, origin);
				cloud.act();
				assertEquals(1000 - Math.round(rawDamage() * percent / 100f), target.HP);
				assertSame(cloud, target.source);
				assertEquals(1, target.calls);
				Actor.remove(target);
				cloud.fullyClear();
			}
		}
	}

	@Test
	public void mixedAndEnvironmentalCloudsKeepOneOriginalDamageEvent() {
		rules(OffensiveDamageOrigin.PLAYER, 200);
		Bundle saved = new Bundle();
		Bundle values = new Bundle();
		CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE, 200)
				.with(CustomDungeonRules.Slider.BOSS_DAMAGE, 500).store(values);
		saved.put("apd_custom_rules", values);
		Dungeon.apdRestoreCustomRules(saved);
		FieryTarget mixedTarget = new FieryTarget();
		Actor.add(mixedTarget);
		StormCloud mixed = seed(100, OffensiveDamageOrigin.PLAYER);
		seed(300, OffensiveDamageOrigin.BOSS);
		mixed.act();
		assertEquals(1000 - Math.round(rawDamage() * (100 * 200 + 300 * 500) / (400f * 100f)), mixedTarget.HP);
		assertEquals(1, mixedTarget.calls);
		Actor.remove(mixedTarget);
		mixed.fullyClear();

		FieryTarget environmentalTarget = new FieryTarget();
		Actor.add(environmentalTarget);
		StormCloud environmental = seed(300, OffensiveDamageOrigin.UNATTRIBUTED);
		environmental.act();
		assertEquals(1000 - rawDamage(), environmentalTarget.HP);
		assertEquals(1, environmentalTarget.calls);
	}

	@Test
	public void saveReloadKeepsCreatorWhileLegacySaveStaysUnknown() {
		rules(OffensiveDamageOrigin.BOSS, 500);
		StormCloud original = seed(300, OffensiveDamageOrigin.BOSS);
		Bundle saved = new Bundle();
		original.storeInBundle(saved);
		Dungeon.level.blobs.clear();
		StormCloud restored = new StormCloud();
		restored.restoreFromBundle(saved);
		FieryTarget target = new FieryTarget();
		Actor.add(target);
		restored.act();
		assertEquals(1000 - rawDamage() * 5, target.HP);
		Actor.remove(target);

		CellDamageOrigins.clearMetadata(saved, "apd_storm_origins");
		StormCloud legacy = new StormCloud();
		legacy.restoreFromBundle(saved);
		FieryTarget legacyTarget = new FieryTarget();
		Actor.add(legacyTarget);
		legacy.act();
		assertEquals(1000 - rawDamage(), legacyTarget.HP);
	}

	@Test
	public void neutralTrackingPreservesCloudWaterFireAndRng() {
		rules(OffensiveDamageOrigin.PLAYER, 100);
		FieryTarget taggedTarget = new FieryTarget();
		Actor.add(taggedTarget);
		long next;
		int[] density;
		boolean[] water;
		int volume;
		Random.pushGenerator(19087);
		try {
			StormCloud tagged = seed(300, OffensiveDamageOrigin.PLAYER);
			tagged.act();
			density = tagged.cur.clone();
			water = Dungeon.level.water.clone();
			volume = tagged.volume;
			next = Random.Long();
		} finally {
			Random.popGenerator();
		}
		Actor.remove(taggedTarget);

		Dungeon.level.blobs = new java.util.HashMap<>();
		java.util.Arrays.fill(Dungeon.level.map, Terrain.EMPTY);
		Dungeon.level.buildFlagMaps();
		FieryTarget plainTarget = new FieryTarget();
		Actor.add(plainTarget);
		Random.pushGenerator(19087);
		try {
			StormCloud plain = seed(300, OffensiveDamageOrigin.UNATTRIBUTED);
			plain.act();
			assertArrayEquals(density, plain.cur);
			assertArrayEquals(water, Dungeon.level.water);
			assertEquals(volume, plain.volume);
			assertEquals(next, Random.Long());
			assertEquals(taggedTarget.HP, plainTarget.HP);
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void delayedCenserCloudRetainsSavedPlayerCreator() {
		Bundle savedRules = new Bundle();
		Bundle values = new Bundle();
		CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE, 200).store(values);
		savedRules.put("apd_custom_rules", values);
		Dungeon.apdRestoreCustomRules(savedRules);
		ChaoticCenser.GasSpewer current = new ChaoticCenser.GasSpewer();
		assertTrue(current.attachTo(Dungeon.hero));
		current.set(CELL, StormCloud.class, 300);
		Bundle state = new Bundle();
		current.storeInBundle(state);
		assertEquals("PLAYER", state.getString("apd_censer_origin"));
		current.detach();
		ChaoticCenser.GasSpewer restored = new ChaoticCenser.GasSpewer();
		restored.restoreFromBundle(state);
		assertTrue(restored.attachTo(Dungeon.hero));
		FieryTarget target = new FieryTarget();
		Actor.add(target);
		restored.act();
		StormCloud cloud = (StormCloud) Dungeon.level.blobs.get(StormCloud.class);
		assertNotNull(cloud);
		cloud.act();
		assertEquals(1000 - rawDamage() * 2, target.HP);
	}

	@Test
	public void everyCurrentCallerProvidesTheActualEffectContext() throws Exception {
		Path root = Paths.get(System.getProperty("apd.game.src"));
		String potion = Files.readString(root.resolve("items/potions/Potion.java"));
		String clouds = Files.readString(root.resolve("items/potions/exotic/PotionOfStormClouds.java"));
		String censer = Files.readString(root.resolve("items/trinkets/ChaoticCenser.java"));
		assertTrue(potion.contains("OffensiveEffectContext.enter(apdThrownOrigin())"));
		assertTrue(clouds.contains("Blob.seed( cell+i, 120, StormCloud.class )"));
		assertTrue(clouds.contains("Blob.seed( cell, centerVolume, StormCloud.class )"));
		assertTrue(censer.contains("OffensiveEffectContext.enter(apdGasOrigin)"));
		assertTrue(censer.contains("OffensiveDamageOrigin.capture(target,null)"));
	}
}
