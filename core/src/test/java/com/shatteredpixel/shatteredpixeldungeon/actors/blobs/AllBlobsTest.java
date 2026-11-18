package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;
import com.watabou.utils.SparseArray;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;

/** Every blob type: seeding adds volume where it was placed, clearing removes it, and saving keeps it. */
public class AllBlobsTest {
	@SuppressWarnings("unchecked")
	static final Class<? extends Blob>[] BLOBS = new Class[]{
		Alchemy.class,
		Blizzard.class,
		ConfusionGas.class,
		CorrosiveGas.class,
		Electricity.class,
		Fire.class,
		Foliage.class,
		Freezing.class,
		GooWarn.class,
		Inferno.class,
		ParalyticGas.class,
		Regrowth.class,
		SacrificialFire.class,
		SmokeScreen.class,
		StenchGas.class,
		StormCloud.class,
		ToxicGas.class,
		VaultFlameTraps.class,
		WaterOfAwareness.class,
		WaterOfHealth.class,
		Web.class
	};

	private Level oldLevel;
	private SewerLevel level;

	@Before public void setUp(){
		HeadlessTestSupport.start();
		oldLevel = Dungeon.level;
		level = new SewerLevel();
		level.setSize(12, 12);
		level.mobs = new HashSet<>();
		level.traps = new SparseArray<>();
		level.blobs = new HashMap<>();
		level.heaps = new SparseArray<>();
		level.plants = new SparseArray<>();
		Dungeon.level = level;
	}

	@After public void tearDown(){ Dungeon.level = oldLevel; }

	@Test public void seedingAddsVolumeAtThatCellOnly(){
		for (Class<? extends Blob> c : BLOBS){
			level.blobs.clear();
			Blob b = Blob.seed(30, 7, c, level);
			assertNotNull(c.getSimpleName(), b);
			assertSame(b, level.blobs.get(c));
			assertEquals(c.getSimpleName(), 7, b.volume);
			assertEquals(7, Blob.volumeAt(30, c));
			assertEquals(0, Blob.volumeAt(31, c));
			Blob.seed(30, 3, c, level);
			assertTrue(c.getSimpleName() + " seeding twice never loses gas", b.volume >= 7);
			b.clear(30);
			assertEquals(0, b.volume);
		}
	}

	@Test public void savedVolumeComesBack(){
		for (Class<? extends Blob> c : BLOBS){
			level.blobs.clear();
			Blob b = Blob.seed(40, 5, c, level);
			Blob.seed(50, 2, c, level);
			Bundle bundle = new Bundle();
			b.storeInBundle(bundle);
			Blob back = Reflection.newInstance(c);
			back.restoreFromBundle(bundle);
			assertEquals(c.getSimpleName(), 7, back.volume);
			assertEquals(5, back.cur[40]);
			assertEquals(2, back.cur[50]);
		}
	}

	@Test public void tileDescriptionsHaveText(){
		for (Class<? extends Blob> c : BLOBS){
			if (c == VaultFlameTraps.class) continue; //internal vault mechanism, never described on a tile
			String d = Reflection.newInstance(c).tileDesc();
			if (d != null) assertFalse(c.getSimpleName(), d.isEmpty() || d.contains(Messages.NO_TEXT_FOUND));
		}
	}

	@Test public void missingBlobsReadAsZero(){
		level.blobs.clear();
		assertEquals(0, Blob.volumeAt(10, ToxicGas.class));
	}
}
