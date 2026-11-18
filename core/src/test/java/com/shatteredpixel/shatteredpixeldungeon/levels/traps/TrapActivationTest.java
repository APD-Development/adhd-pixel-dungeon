package com.shatteredpixel.shatteredpixeldungeon.levels.traps;

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

/** Traps that work without a screen, triggered for real out of the hero's sight: they do what their text says. */
public class TrapActivationTest {
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

	private Rat ratAt(int cell){
		Rat rat = new Rat();
		rat.pos = cell;
		rat.HP = rat.HT;
		level.mobs.add(rat);
		Actor.add(rat);
		return rat;
	}

	@Test public void gasTrapsFillTheirCellWithGas(){
		Class<?>[][] cases = {{ToxicTrap.class, ToxicGas.class}, {ConfusionTrap.class, ConfusionGas.class}, {CorrosionTrap.class, CorrosiveGas.class}};
		for (Class<?>[] c : cases){
			level.blobs.clear();
			Trap t = (Trap) com.watabou.utils.Reflection.newInstance(c[0]);
			t.set(CELL).activate();
			@SuppressWarnings("unchecked") Class<? extends Blob> gas = (Class<? extends Blob>) c[1];
			assertTrue(c[0].getSimpleName(), Blob.volumeAt(CELL, gas) > 0);
		}
	}

	@Test public void weakeningTrapWeakensWhoeverStandsOnIt(){
		Rat rat = ratAt(CELL);
		new WeakeningTrap().set(CELL).activate();
		assertNotNull(rat.buff(Weakness.class));
	}

	@Test public void weakeningTrapOnAnEmptyCellDoesNothing(){
		Rat rat = ratAt(CELL + 2);
		new WeakeningTrap().set(CELL).activate();
		assertNull(rat.buff(Weakness.class));
	}

}
