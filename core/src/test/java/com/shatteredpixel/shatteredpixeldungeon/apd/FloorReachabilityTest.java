package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.watabou.utils.PathFinder;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Real generated floors 1-24 for a few seeds: every floor has an entrance, every non-boss floor's exit can be
 * reached from it (counting doors, locked doors and secret doors as openable), and every room fits on the map.
 */
public class FloorReachabilityTest {
	@After public void tearDown(){ LevelGenerationHarness.levelObserver = null; }

	private static boolean[] openable(Level level){
		boolean[] p = new boolean[level.length()];
		for (int i = 0; i < p.length; i++){
			int t = level.map[i];
			p[i] = level.passable[i] || (Terrain.flags[t] & Terrain.SOLID) == 0 || t == Terrain.LOCKED_DOOR || t == Terrain.CRYSTAL_DOOR
					|| t == Terrain.SECRET_DOOR || t == Terrain.BARRICADE || t == Terrain.LOCKED_EXIT
					|| (Terrain.flags[t] & Terrain.SECRET) != 0;
		}
		return p;
	}

	private static boolean bossFloor(int depth){ return depth % 5 == 0; }

	@Test public void everyFloorIsWalkableFromEntranceToExit() throws Exception {
		check(new long[]{918271L, 20261118L}, 0, HeroClass.WARRIOR);
	}

	@Test public void stillWalkableWithEveryChallengeAndOtherClasses() throws Exception {
		check(new long[]{4242L}, com.shatteredpixel.shatteredpixeldungeon.Challenges.MAX_VALUE, HeroClass.ROGUE);
		check(new long[]{777L}, 0, HeroClass.HUNTRESS);
	}

	private static void check(long[] seeds, int challenges, HeroClass heroClass) throws Exception {
		List<String> problems = new ArrayList<>();
		LevelGenerationHarness.levelObserver = (depth, level) -> {
			int in = level.entrance(), out = level.exit();
			if (in < 0) { problems.add("floor " + depth + ": no entrance"); return; }
			if (bossFloor(depth)) return;
			if (out < 0) { problems.add("floor " + depth + ": no exit"); return; }
			boolean[] p = openable(level);
			p[out] = true;
			PathFinder.setMapSize(level.width(), level.height());
			PathFinder.buildDistanceMap(out, p);
			if (PathFinder.distance[in] == Integer.MAX_VALUE) problems.add("floor " + depth + ": exit unreachable");
			if (level instanceof RegularLevel){
				for (Room r : ((RegularLevel) level).rooms()){
					if (r.left < 0 || r.top < 0 || r.right >= level.width() || r.bottom >= level.height())
						problems.add("floor " + depth + ": " + r.getClass().getSimpleName() + " off the map");
				}
			}
		};
		for (long seed : seeds){
			LevelGenerationHarness.generate(seed, challenges, heroClass, LevelGenerationHarness.Context.VETERAN, 24);
		}
		assertTrue(problems.toString(), problems.isEmpty());
	}
}
