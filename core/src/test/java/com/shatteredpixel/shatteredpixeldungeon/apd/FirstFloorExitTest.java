/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

// Regression coverage for the previously blocking tutorial entrance.
// Pins the cause of "the floor-1 start room has no exit": SPD's first-run tutorial hides the entrance door on purpose. A used install must
// not be in the tutorial (ApdIntro), and a floor generated without it must have a normal, walkable way out.
public class FirstFloorExitTest {

	private final Map<Integer, Level> seen = new HashMap<>();

	@After
	public void clear(){
		LevelGenerationHarness.levelObserver = null;
	}

	//can the hero walk from the entrance to the stairs down, without searching for anything?
	private static boolean exitWalkable( Level l ){
		boolean[] seenCell = new boolean[l.length()];
		ArrayDeque<Integer> q = new ArrayDeque<>();
		q.add(l.entrance());
		seenCell[l.entrance()] = true;
		int[] steps = { -1, 1, -l.width(), l.width() };
		while (!q.isEmpty()){
			int c = q.poll();
			if (c == l.exit()) return true;
			for (int s : steps){
				int n = c + s;
				if (n < 0 || n >= l.length() || seenCell[n]) continue;
				if (n == l.exit() || (Terrain.flags[l.map[n]] & Terrain.PASSABLE) != 0){
					seenCell[n] = true;
					q.add(n);
				}
			}
		}
		return false;
	}

	private Level floorOne( long seed, LevelGenerationHarness.Context context ) throws Exception {
		seen.clear();
		LevelGenerationHarness.levelObserver = new java.util.function.BiConsumer<Integer, Level>(){
			@Override
			public void accept( Integer depth, Level level ){
				seen.put(depth, level);
			}
		};
		LevelGenerationHarness.generate(seed, 0, HeroClass.WARRIOR, context, 2);
		return seen.get(1);
	}

	@Test
	public void freshAndReturningStartsHaveVisibleTraversableExits() throws Exception {
		for (long seed = 1; seed <= 12; seed++){
			for (LevelGenerationHarness.Context context : new LevelGenerationHarness.Context[]{LevelGenerationHarness.Context.FRESH, LevelGenerationHarness.Context.VETERAN}){
				assertTrue("floor 1 must be traversable even before reading the guide: " + context + " seed " + seed,
						exitWalkable(floorOne(seed * 7919, context)));
			}
		}
	}

	@Test
	public void everyHeroHasAnAccessibleFreshStartingRoom() throws Exception {
		for (HeroClass hero : HeroClass.values()){
			seen.clear();
			LevelGenerationHarness.levelObserver = (depth, level) -> seen.put(depth, level);
			LevelGenerationHarness.generate(7919, 0, hero, LevelGenerationHarness.Context.FRESH, 1);
			assertTrue(hero + " must be able to leave the first floor", exitWalkable(seen.get(1)));
		}
	}

	@Test
	public void challengeRunsAlsoHaveATraversableFirstEntrance() throws Exception {
		seen.clear();
		LevelGenerationHarness.levelObserver = (depth, level) -> seen.put(depth, level);
		LevelGenerationHarness.generate(4242, com.shatteredpixel.shatteredpixeldungeon.Challenges.MAX_VALUE,
				HeroClass.WARRIOR, LevelGenerationHarness.Context.FRESH, 1);
		assertTrue("all-challenges floor1 exit must remain traversable", exitWalkable(seen.get(1)));
	}

	@Test
	public void walkingThroughTheEntranceEndsIntroWithoutResettingGuideAcknowledgement() throws Exception {
		com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel level = (com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel) floorOne(7919, LevelGenerationHarness.Context.FRESH);
		com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro(true);
		ApdIntro.finishIfPastEntrance(level, 1, 0, level.entrance());
		assertTrue("hints remain for a fresh starting room", com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro());
		for (com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room room : level.rooms()) if (room.isEntrance()){
			for (int y = room.top; y <= room.bottom; y++) for (int x = room.left; x <= room.right; x++){
				if (x != room.left && x != room.right && y != room.top && y != room.bottom) continue;
				int cell = x + y * level.width();
				if (level.map[cell] == Terrain.DOOR){
					ApdIntro.finishIfPastEntrance(level, 1, 0, cell);
					assertFalse("leaving cannot retain the tutorial hunger exemption", com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro());
					return;
				}
			}
		}
		fail("expected visible entrance door");
	}

	@Test
	public void anOldHiddenEntranceIsRepairedOnLoadWithoutRevealingOtherSecrets() throws Exception {
		com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel level =
				(com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel) floorOne(7919, LevelGenerationHarness.Context.FRESH);
		com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room entrance = null;
		for (com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room room : level.rooms()) if (room.isEntrance()) entrance = room;
		assertNotNull(entrance);
		int changed = 0;
		for (int y = entrance.top; y <= entrance.bottom; y++) for (int x = entrance.left; x <= entrance.right; x++){
			if (x != entrance.left && x != entrance.right && y != entrance.top && y != entrance.bottom) continue;
			int cell = x + y * level.width();
			if (level.map[cell] == Terrain.DOOR){ level.map[cell] = Terrain.SECRET_DOOR; changed++; }
		}
		assertTrue("the fixture seals a real entrance door", changed > 0);
		int other = -1;
		for (int y = 1; y < level.height()-1 && other < 0; y++) for (int x = 1; x < level.width()-1; x++){
			if (x >= entrance.left && x <= entrance.right && y >= entrance.top && y <= entrance.bottom) continue;
			int cell = x + y * level.width();
			if (level.map[cell] == Terrain.WALL){ other = cell; break; }
		}
		assertTrue(other >= 0);
		level.map[other] = Terrain.SECRET_DOOR;
		int[] before = level.map.clone();
		com.watabou.utils.Bundle bundle = new com.watabou.utils.Bundle();
		bundle.put("level", level);
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.depth = 2;
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.branch = 0;
		assertArrayEquals("other floors are not migrated", before, ((Level) bundle.get("level")).map);
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.depth = 1;
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.branch = 1;
		assertArrayEquals("side branches are not migrated", before, ((Level) bundle.get("level")).map);
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.depth = 1;
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.branch = 0;
		com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel restored =
				(com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel) bundle.get("level");
		assertTrue("the existing save is playable without discarding it", exitWalkable(restored));
		assertEquals("unrelated secret stays secret", Terrain.SECRET_DOOR, restored.map[other]);
		int repaired = 0;
		for (int cell = 0; cell < before.length; cell++) if (before[cell] != restored.map[cell]){
			assertEquals(Terrain.SECRET_DOOR, before[cell]);
			assertEquals(Terrain.DOOR, restored.map[cell]);
			assertTrue(restored.passable[cell]);
			assertFalse(restored.secret[cell]);
			repaired++;
		}
		assertEquals("only forced entrance doors change", changed, repaired);
		int[] once = restored.map.clone();
		restored.repairTutorialEntrance();
		assertArrayEquals("repair is idempotent", once, restored.map);
	}

	@Test
	public void aReturningPlayerWithNoGuidePagesGetsAnOpenFloorOneAndWeSeeWhatFloorTwoDoes() throws Exception {
		int open1 = 0, open2 = 0;
		for (long seed = 1; seed <= 12; seed++){
			Level one = floorOne(seed * 104729, LevelGenerationHarness.Context.RETURNING);
			if (exitWalkable(one)) open1++;
			if (exitWalkable(seen.get(2))) open2++;
		}
		System.out.println("APD-RETURNING floor1 walkable " + open1 + "/12, floor2 walkable " + open2 + "/12");
		assertEquals("floor 1 must be open once the tutorial is over", 12, open1);
	}
}
