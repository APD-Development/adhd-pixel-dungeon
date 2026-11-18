package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Real generated floors 1-24: monsters stand on walkable cells and never share one, item piles are on the map and
 * not inside solid walls, traps sit on trap terrain, and room interiors never overlap each other.
 */
public class FloorContentsPlacementTest {
	@After public void tearDown(){ LevelGenerationHarness.levelObserver = null; }

	@Test public void contentsArePlacedSensibly() throws Exception {
		List<String> problems = new ArrayList<>();
		LevelGenerationHarness.levelObserver = (depth, level) -> check(depth, level, problems);
		for (long seed : new long[]{5150L, 20261118L}){
			LevelGenerationHarness.generate(seed, 0, HeroClass.MAGE, LevelGenerationHarness.Context.VETERAN, 24);
		}
		LevelGenerationHarness.generate(31337L, com.shatteredpixel.shatteredpixeldungeon.Challenges.MAX_VALUE, HeroClass.DUELIST, LevelGenerationHarness.Context.VETERAN, 24);
		assertTrue(problems.toString(), problems.isEmpty());
	}

	private static void check(int depth, Level level, List<String> problems){
		Set<Integer> taken = new HashSet<>();
		for (Mob m : level.mobs){
			String n = "floor " + depth + ": " + m.getClass().getSimpleName() + " at " + m.pos;
			if (m.pos < 0 || m.pos >= level.length()) { problems.add(n + " off the map"); continue; }
			if (!taken.add(m.pos)) problems.add(n + " shares a cell");
			if (!level.passable[m.pos] && !level.avoid[m.pos] && (Terrain.flags[level.map[m.pos]] & Terrain.SOLID) != 0)
				problems.add(n + " inside a wall");
		}
		for (Heap h : level.heaps.valueList()){
			String n = "floor " + depth + ": heap at " + h.pos;
			if (h.pos < 0 || h.pos >= level.length()) { problems.add(n + " off the map"); continue; }
			if (h.isEmpty()) problems.add(n + " is empty");
			if ((Terrain.flags[level.map[h.pos]] & Terrain.SOLID) != 0) problems.add(n + " inside a wall (terrain " + level.map[h.pos] + ")");
		}
		for (int pos : level.traps.keyArray()){
			int t = level.map[pos];
			if (t != Terrain.TRAP && t != Terrain.SECRET_TRAP && t != Terrain.INACTIVE_TRAP)
				problems.add("floor " + depth + ": trap at " + pos + " on terrain " + t);
		}
		if (level instanceof RegularLevel){
			List<Room> rooms = new ArrayList<>(((RegularLevel) level).rooms());
			for (int a = 0; a < rooms.size(); a++) for (int b = a + 1; b < rooms.size(); b++){
				Room r = rooms.get(a), s = rooms.get(b);
				int l = Math.max(r.left, s.left) , rt = Math.min(r.right, s.right);
				int tp = Math.max(r.top, s.top), bt = Math.min(r.bottom, s.bottom);
				if (rt - l >= 2 && bt - tp >= 2)
					problems.add("floor " + depth + ": " + r.getClass().getSimpleName() + " overlaps " + s.getClass().getSimpleName());
			}
		}
	}
}
