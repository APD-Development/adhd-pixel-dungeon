package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.*;
import java.nio.file.*;
import java.util.HashMap;
import static org.junit.Assert.*;

public class InfernoDamageProvenanceTest {
	private static final int CELL = 312;
	private static final int NEXT = 313;
	private final FireCallerProvenanceTest fixture = new FireCallerProvenanceTest();

	@Before public void setup() throws Exception {
		fixture.setup();
		Dungeon.level.flamable[CELL] = false;
		Dungeon.level.flamable[NEXT] = false;
	}

	@After public void cleanup() throws Exception { fixture.cleanup(); }

	private void rules(OffensiveDamageOrigin role, int percentage) {
		fixture.fixture.rules(role == OffensiveDamageOrigin.PLAYER ? percentage : 275,
				role == OffensiveDamageOrigin.ENEMY ? percentage : 325,
				role == OffensiveDamageOrigin.BOSS ? percentage : 425);
	}

	private int tick(Burning burning, int raw) {
		Bundle state = new Bundle();
		burning.storeInBundle(state);
		state.put("next_dmg", raw);
		burning.restoreFromBundle(state);
		burning.act();
		return raw;
	}

	private int infernoHit(OffensiveDamageOrigin role, int percentage) {
		rules(role, percentage);
		Dungeon.level.blobs = new HashMap<>();
		SpecialBombDamageProvenanceTest.Target target = new SpecialBombDamageProvenanceTest.Target(false);
		Actor.add(target);
		try {
			Inferno inferno = Inferno.seedAttributed(CELL, 120, role);
			inferno.act();
			Burning burning = target.buff(Burning.class);
			assertNotNull(burning);
			tick(burning, 4);
			assertEquals(1, target.hits.size());
			assertSame(burning, target.sources.get(0));
			return target.hits.get(0);
		} finally {
			Actor.remove(target);
		}
	}

	@Test public void infernoContactUsesEveryDamageSliderExactlyOnceAcrossTheRange() {
		for (OffensiveDamageOrigin role : new OffensiveDamageOrigin[]{OffensiveDamageOrigin.PLAYER, OffensiveDamageOrigin.ENEMY, OffensiveDamageOrigin.BOSS}) {
			for (int percentage = 25; percentage <= 500; percentage += 25) {
				assertEquals(Math.round(4 * percentage / 100f), infernoHit(role, percentage));
			}
		}
	}

	@Test public void infernoSaveReloadRetainsCauseWhileLegacyRemainsEnvironmental() {
		rules(OffensiveDamageOrigin.BOSS, 425);
		Inferno original = Inferno.seedAttributed(CELL, 120, OffensiveDamageOrigin.BOSS);
		Bundle saved = new Bundle();
		original.storeInBundle(saved);
		Inferno restored = new Inferno();
		restored.restoreFromBundle(saved);
		SpecialBombDamageProvenanceTest.Target target = new SpecialBombDamageProvenanceTest.Target(false);
		Actor.add(target);
		try {
			restored.act();
			tick(target.buff(Burning.class), 4);
			assertEquals(17, (int)target.hits.get(0));
		} finally { Actor.remove(target); }

		CellDamageOrigins.clearMetadata(saved, "apd_inferno_origins");
		Inferno legacy = new Inferno();
		legacy.restoreFromBundle(saved);
		SpecialBombDamageProvenanceTest.Target unknown = new SpecialBombDamageProvenanceTest.Target(false);
		Actor.add(unknown);
		try {
			legacy.act();
			tick(unknown.buff(Burning.class), 4);
			assertEquals(4, (int)unknown.hits.get(0));
		} finally { Actor.remove(unknown); }
	}

	@Test public void infernoIgnitedNeighbourFireRetainsCreator() {
		rules(OffensiveDamageOrigin.ENEMY, 325);
		Dungeon.level.flamable[NEXT] = true;
		Inferno inferno = Inferno.seedAttributed(CELL, 120, OffensiveDamageOrigin.ENEMY);
		SpecialBombDamageProvenanceTest.Target target = new SpecialBombDamageProvenanceTest.Target(false);
		target.pos = NEXT;
		Actor.add(target);
		try {
			inferno.act();
			Fire fire = (Fire)Dungeon.level.blobs.get(Fire.class);
			assertNotNull(fire);
			fire.act();
			Burning burning = target.buff(Burning.class);
			assertNotNull(burning);
			tick(burning, 4);
			assertEquals(13, (int)target.hits.get(0));
			assertSame(burning, target.sources.get(0));
		} finally { Actor.remove(target); }
	}

	@Test public void burningCreatedTerrainFireRetainsEveryCreator() {
		for (OffensiveDamageOrigin role : new OffensiveDamageOrigin[]{OffensiveDamageOrigin.PLAYER, OffensiveDamageOrigin.ENEMY, OffensiveDamageOrigin.BOSS}) {
			rules(role, role == OffensiveDamageOrigin.PLAYER ? 200 : role == OffensiveDamageOrigin.ENEMY ? 300 : 500);
			Dungeon.level.blobs = new HashMap<>();
			Dungeon.level.flamable[CELL] = true;
			SpecialBombDamageProvenanceTest.Target source = new SpecialBombDamageProvenanceTest.Target(false);
			Actor.add(source);
			Burning first = Buff.affect(source, Burning.class);
			first.reigniteAttributedOrigin(source, role);
			tick(first, 4);
			Fire fire = (Fire)Dungeon.level.blobs.get(Fire.class);
			assertNotNull(fire);
			Actor.remove(source);
			Dungeon.level.flamable[CELL] = false;
			SpecialBombDamageProvenanceTest.Target target = new SpecialBombDamageProvenanceTest.Target(false);
			Actor.add(target);
			try {
				fire.act();
				Burning transferred = target.buff(Burning.class);
				assertNotNull(transferred);
				tick(transferred, 4);
				int percentage = role == OffensiveDamageOrigin.PLAYER ? 200 : role == OffensiveDamageOrigin.ENEMY ? 300 : 500;
				assertEquals(Math.round(4 * percentage / 100f), (int)target.hits.get(0));
				assertSame(transferred, target.sources.get(0));
			} finally { Actor.remove(target); }
		}
	}

	@Test public void neutralAttributionPreservesInfernoDensityVolumeAndRng() {
		fixture.fixture.rules(100, 100, 100);
		int[] density; int volume; long next;
		Random.pushGenerator(1490);
		try {
			Dungeon.level.blobs = new HashMap<>();
			Inferno plain = Inferno.seedAttributed(CELL, 120, OffensiveDamageOrigin.UNATTRIBUTED);
			plain.act(); density = plain.cur.clone(); volume = plain.volume; next = Random.Long();
		} finally { Random.popGenerator(); }
		Random.pushGenerator(1490);
		try {
			Dungeon.level.blobs = new HashMap<>();
			Inferno attributed = Inferno.seedAttributed(CELL, 120, OffensiveDamageOrigin.PLAYER);
			attributed.act(); assertArrayEquals(density, attributed.cur); assertEquals(volume, attributed.volume); assertEquals(next, Random.Long());
		} finally { Random.popGenerator(); }
	}

	@Test public void realBrewAndDelayedCenserUseCapturedCreator() throws Exception {
		Path root = Paths.get(System.getProperty("apd.game.src"));
		String brew = Files.readString(root.resolve("items/potions/brews/InfernalBrew.java"));
		String censer = Files.readString(root.resolve("items/trinkets/ChaoticCenser.java"));
		assertTrue(brew.contains("final OffensiveDamageOrigin apdOrigin = com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.OffensiveEffectContext.current();"));
		assertTrue(brew.contains("Inferno.seedAttributed( cell+i, 120, apdOrigin )"));
		assertTrue(brew.contains("Inferno.seedAttributed( cell, centerVolume, apdOrigin )"));
		assertFalse(brew.contains("Blob.seed( cell+i, 120, Inferno.class )"));
		assertTrue(censer.contains("OffensiveEffectContext.enter(apdGasOrigin)"));
		assertTrue(censer.contains("GameScene.add(Blob.seed(targetCell, gasQuantity, gasType))"));
	}
}

