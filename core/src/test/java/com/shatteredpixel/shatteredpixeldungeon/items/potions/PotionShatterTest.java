package com.shatteredpixel.shatteredpixeldungeon.items.potions;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ConfusionGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** Thrown potions out of the hero's sight leave the cloud or fire their text promises, and every potion has one exotic twin. */
public class PotionShatterTest {
	private static final int W = 15, CELL = 7 * W + 7;
	private final Map<Field, Object> oldActors = new LinkedHashMap<>();
	private Level oldLevel;
	private int oldDepth;
	private SewerLevel level;

	private static Field field(String name) throws Exception {
		Field f = Actor.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	@Before public void setUp() throws Exception {
		HeadlessTestSupport.start();
		oldLevel = Dungeon.level;
		oldDepth = Dungeon.depth;
		for (String n : new String[]{"all", "chars", "ids", "current", "now", "nextID"}) oldActors.put(field(n), field(n).get(null));
		field("all").set(null, new HashSet<Actor>());
		field("chars").set(null, new HashSet<Char>());
		field("ids").set(null, new SparseArray<Actor>());
		field("current").set(null, null);
		field("now").setFloat(null, 0f);
		field("nextID").setInt(null, 1);
		Dungeon.depth = 3;
		level = new SewerLevel();
		level.setSize(W, W);
		level.mobs = new HashSet<>();
		level.traps = new SparseArray<>();
		level.blobs = new HashMap<>();
		level.heaps = new SparseArray<>();
		level.plants = new SparseArray<>();
		Arrays.fill(level.heroFOV, false); // out of sight: no sounds or particles needed
		Arrays.fill(level.passable, true);
		Dungeon.level = level;
		PathFinder.setMapSize(W, W);
	}

	@After public void tearDown() throws Exception {
		for (Map.Entry<Field, Object> e : oldActors.entrySet()) e.getKey().set(null, e.getValue());
		Dungeon.level = oldLevel;
		Dungeon.depth = oldDepth;
		if (oldLevel != null) PathFinder.setMapSize(oldLevel.width(), oldLevel.height());
	}

	private int volumeOf(Class<? extends Blob> type){
		Blob b = level.blobs.get(type);
		return b == null ? 0 : b.volume;
	}

	private void shatterMakes(Potion p, Class<? extends Blob> type){
		p.shatter(CELL);
		assertTrue(p.getClass().getSimpleName() + " should leave " + type.getSimpleName(), volumeOf(type) > 0);
	}

	@Test public void toxic(){ shatterMakes(new PotionOfToxicGas(), ToxicGas.class); }
	@Test public void paralytic(){ shatterMakes(new PotionOfParalyticGas(), com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas.class); }
	@Test public void flame(){ shatterMakes(new PotionOfLiquidFlame(), com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire.class); }
	@Test public void frost(){ shatterMakes(new PotionOfFrost(), com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing.class); }
	@Test public void corrosive(){ shatterMakes(new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCorrosiveGas(), CorrosiveGas.class); }
	@Test public void fog(){ shatterMakes(new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShroudingFog(), com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SmokeScreen.class); }
	@Test public void storm(){ shatterMakes(new com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStormClouds(), com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StormCloud.class); }

	@Test public void everyPotionHasExactlyOneExoticTwin(){
		java.util.Map<Class<? extends Potion>, Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion>> r = com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion.regToExo;
		assertEquals(12, r.size());
		assertEquals(12, new HashSet<>(r.values()).size());
		for (Map.Entry<Class<? extends Potion>, Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion>> e : r.entrySet())
			assertEquals(e.getKey(), com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion.exoToReg.get(e.getValue()));
	}
}
