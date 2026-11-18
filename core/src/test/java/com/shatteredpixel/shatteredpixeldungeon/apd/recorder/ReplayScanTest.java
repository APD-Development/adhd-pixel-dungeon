package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Gnoll;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ReplayFloor;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.AlarmTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap;
import com.watabou.utils.SparseArray;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;

/** The replay floor snapshot: counts traps by visibility and lists mobs and items in position order. */
public class ReplayScanTest {
	private int oldDepth;

	@Before public void setUp(){ HeadlessTestSupport.start(); oldDepth = Dungeon.depth; Dungeon.depth = 2; }
	@After public void tearDown(){ Dungeon.depth = oldDepth; }

	private static SewerLevel level(){
		SewerLevel level = new SewerLevel();
		level.setSize(10, 10);
		level.mobs = new HashSet<>();
		level.traps = new SparseArray<>();
		level.blobs = new HashMap<>();
		level.heaps = new SparseArray<>();
		level.plants = new SparseArray<>();
		try {
			java.lang.reflect.Field rooms = com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel.class.getDeclaredField("rooms");
			rooms.setAccessible(true);
			rooms.set(level, new java.util.ArrayList<>());
		} catch (ReflectiveOperationException e){ throw new AssertionError(e); }
		return level;
	}

	@Test public void emptyFloorRecordsItsShape(){
		ReplayFloor f = ReplayScan.scan(level());
		assertEquals(2, f.depth);
		assertEquals(10, f.width);
		assertEquals(10, f.height);
		assertTrue(f.levelClass, f.levelClass.endsWith("SewerLevel"));
		assertEquals(0, f.trapsVisible + f.trapsHidden);
		assertTrue(f.mobs.isEmpty());
		assertTrue(f.items.isEmpty());
	}

	@Test public void trapsMobsAndItemsAreCountedAndOrdered(){
		SewerLevel level = level();
		AlarmTrap shown = new AlarmTrap(); shown.pos = 11; shown.visible = true;
		WornDartTrap hidden = new WornDartTrap(); hidden.pos = 12; hidden.visible = false;
		level.traps.put(11, shown);
		level.traps.put(12, hidden);
		Gnoll late = new Gnoll(); late.pos = 40;
		Rat early = new Rat(); early.pos = 5;
		level.mobs.add(late);
		level.mobs.add(early);
		Heap heap = new Heap();
		heap.pos = 33;
		heap.items.add(new Sword());
		level.heaps.put(33, heap);

		ReplayFloor f = ReplayScan.scan(level);
		assertEquals(1, f.trapsVisible);
		assertEquals(1, f.trapsHidden);
		assertEquals(2, f.mobs.size());
		assertEquals("mobs sorted by position", 5, f.mobs.get(0).pos);
		assertEquals(40, f.mobs.get(1).pos);
		assertTrue(f.mobs.get(0).mobClass.endsWith("Rat"));
		assertEquals(1, f.items.size());
		assertEquals(33, f.items.get(0).pos);
		assertEquals("HEAP", f.items.get(0).container);
		assertTrue(f.items.get(0).trueClass, f.items.get(0).trueClass.endsWith("Sword"));
	}
}
