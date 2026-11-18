package com.shatteredpixel.shatteredpixeldungeon.apd.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ReplayFloor;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

//Floor replay: it matches the game's own generation, refuses when unsafe, and leaves everything it borrowed as it found it.
public class FloorReplayTest {

	@Before
	public void setUp(){
		HeadlessTestSupport.start();
		FloorReplay.setLiveGameCheck(null);
	}

	@After
	public void tearDown(){
		FloorReplay.setLiveGameCheck(null);
	}

	private static List<String> replayFingerprints( long seed, int challenges, HeroClass cls, int depth ) throws Exception {
		final List<String> prints = new ArrayList<>();
		FloorReplay.replay(seed, challenges, cls, depth, new FloorReplay.Observer(){
			@Override
			public void level( int d, Level level ){
				try {
					prints.add(LevelGenerationHarness.fingerprint(level));
				} catch (Exception e){
					throw new RuntimeException(e);
				}
			}
		});
		return prints;
	}

	@Test
	public void replayMatchesTheGamesOwnGenerationFloorForFloor() throws Exception {
		//the game breaks shop-bag ties by object identity (different every run); both sides use the one deterministic order here
		ReplayContext.forceDeterministicBags = true;
		try {
			matchesGame();
		} finally {
			ReplayContext.forceDeterministicBags = false;
		}
	}

	private void matchesGame() throws Exception {
		long[] seeds = { 12345L, 987654321L, 55555L, 2024L, 31L, 777L };
		int[] challenges = { 0, 0, 3, 0, 0, 0 };
		HeroClass[] classes = { HeroClass.WARRIOR, HeroClass.MAGE, HeroClass.ROGUE, HeroClass.HUNTRESS, HeroClass.DUELIST, HeroClass.CLERIC };
		for (int i = 0; i < seeds.length; i++){
			List<LevelGenerationHarness.LevelPrint> game = LevelGenerationHarness.generate(seeds[i], challenges[i], classes[i],
					LevelGenerationHarness.Context.REFERENCE, FloorReplay.LAST_FLOOR);
			List<String> replay = replayFingerprints(seeds[i], challenges[i], classes[i], FloorReplay.LAST_FLOOR);
			assertEquals(game.size(), replay.size());
			for (int d = 0; d < game.size(); d++){
				assertEquals("seed " + seeds[i] + " floor " + (d + 1), game.get(d).hash, replay.get(d));
			}
		}
	}

	@Test
	public void replayUsesTheRunsOwnDifficultyAndMatchesTheGameAtEveryLevel() throws Exception {
		ReplayContext.forceDeterministicBags = true;
		int was = SPDSettings.apdDifficulty();
		try {
			List<String> threeReplay = replayFingerprints(8642L, 0, HeroClass.MAGE, FloorReplay.LAST_FLOOR);
			for (int level : new int[]{ 1, 2, 4, 5 }){
				SPDSettings.apdDifficulty(level);
				List<LevelGenerationHarness.LevelPrint> game = LevelGenerationHarness.generate(8642L, 0, HeroClass.MAGE,
						LevelGenerationHarness.Context.REFERENCE, FloorReplay.LAST_FLOOR);
				final List<String> replay = new ArrayList<>();
				final int lvl = level;
				FloorReplay.replay(8642L, 0, 0, level, HeroClass.MAGE, FloorReplay.LAST_FLOOR, new FloorReplay.Observer(){
					@Override
					public void level( int d, Level l ){
						try {
							replay.add(LevelGenerationHarness.fingerprint(l));
						} catch (Exception e){
							throw new RuntimeException(e);
						}
					}
				});
				boolean differsFromThree = false;
				for (int d = 0; d < game.size(); d++){
					assertEquals("level " + lvl + " floor " + (d + 1), game.get(d).hash, replay.get(d));
					if (!game.get(d).hash.equals(threeReplay.get(d))) differsFromThree = true;
				}
				assertTrue("level " + level + " must really differ from level 3", differsFromThree);
			}
		} finally {
			SPDSettings.apdDifficulty(was);
			ReplayContext.forceDeterministicBags = false;
		}
	}

	@Test
	public void replayIsRepeatableAndPrefixStable() throws Exception {
		List<String> a = replayFingerprints(777L, 0, HeroClass.WARRIOR, 12);
		List<String> b = replayFingerprints(777L, 0, HeroClass.WARRIOR, 26);
		assertEquals(a, b.subList(0, 12));
		assertEquals(a, replayFingerprints(777L, 0, HeroClass.WARRIOR, 12));
		assertFalse(a.equals(replayFingerprints(778L, 0, HeroClass.WARRIOR, 12)));
	}

	@Test
	public void exoticChanceBelongsToTheRunNotToTheInstall() throws Exception {
		List<String> none = new ArrayList<>(), many = new ArrayList<>();
		for (ReplayFloor f : FloorReplay.replay(2121L, 0, 0, HeroClass.WARRIOR, 12, null)) none.add(items(f));
		for (ReplayFloor f : FloorReplay.replay(2121L, 0, 80, HeroClass.WARRIOR, 12, null)) many.add(items(f));
		assertFalse("exotic chance 0 and 80 must give different floors", none.equals(many));
		//the player's own setting never leaks in
		int installValue = SPDSettings.apdExoticSetting();
		try {
			SPDSettings.apdExoticChance(80);
			List<String> again = new ArrayList<>();
			for (ReplayFloor f : FloorReplay.replay(2121L, 0, 0, HeroClass.WARRIOR, 12, null)) again.add(items(f));
			assertEquals(none, again);
			assertEquals(80, SPDSettings.apdExoticSetting());
		} finally {
			SPDSettings.apdExoticChance(installValue);
		}
		List<String> repeat = new ArrayList<>();
		for (ReplayFloor f : FloorReplay.replay(2121L, 0, 80, HeroClass.WARRIOR, 12, null)) repeat.add(items(f));
		assertEquals(many, repeat);
	}

	private static String items( ReplayFloor f ){
		StringBuilder sb = new StringBuilder();
		for (ReplayFloor.Item i : f.items) sb.append(i.trueClass).append(i.pos).append(';');
		return sb.toString();
	}

	@Test
	public void otherThreadsUsingRandomNumbersCannotDisturbAReplay() throws Exception {
		//a device's render thread draws random numbers (and pushes/pops generators) all the time while a replay runs
		List<String> quiet = replayFingerprints(1357L, 0, HeroClass.ROGUE, 14);
		final java.util.concurrent.atomic.AtomicBoolean stop = new java.util.concurrent.atomic.AtomicBoolean(false);
		Thread noise = new Thread(new Runnable(){
			@Override
			public void run(){
				while (!stop.get()){
					com.watabou.utils.Random.Float();
					com.watabou.utils.Random.Int(100);
					com.watabou.utils.Random.pushGenerator(5L);
					com.watabou.utils.Random.Int(10);
					com.watabou.utils.Random.popGenerator();
				}
			}
		});
		noise.start();
		try {
			for (int i = 0; i < 3; i++) assertEquals(quiet, replayFingerprints(1357L, 0, HeroClass.ROGUE, 14));
		} finally {
			stop.set(true);
			noise.join();
		}
	}

	@Test
	public void floorsHaveContent() throws Exception {
		List<ReplayFloor> floors = FloorReplay.replay(4242L, 0, HeroClass.MAGE, FloorReplay.LAST_FLOOR);
		assertEquals(26, floors.size());
		for (int i = 0; i < floors.size(); i++){
			ReplayFloor f = floors.get(i);
			assertEquals(i + 1, f.depth);
			assertFalse(f.levelClass.isEmpty());
			assertTrue(f.width > 0 && f.height > 0);
			assertFalse("floor " + f.depth + " has no creatures", f.mobs.isEmpty() && f.depth % 5 != 0 && f.depth < 26);
		}
		assertEquals("levels.SewerLevel", floors.get(0).levelClass);
		assertEquals("levels.SewerBossLevel", floors.get(4).levelClass);
		assertEquals("levels.LastLevel", floors.get(25).levelClass);
		int items = 0;
		for (ReplayFloor f : floors) items += f.items.size();
		assertTrue("items " + items, items > 50);
	}

	@Test
	public void refusesWhileAGameIsLiveOrRecording() throws Exception {
		FloorReplay.setLiveGameCheck(new FloorReplay.LiveGameCheck(){
			@Override
			public String liveGame(){
				return "game scene";
			}
		});
		assertEquals("game scene", FloorReplay.refusal());
		try {
			FloorReplay.replay(1L, 0, HeroClass.WARRIOR, 3);
			fail("must refuse");
		} catch (FloorReplay.Refused expected){
			assertTrue(expected.getMessage().contains("game scene"));
		}
		FloorReplay.setLiveGameCheck(null);
		assertNull(FloorReplay.refusal());
	}

	@Test
	public void playersJournalAndTutorialAreNeverReadOrChanged() throws Exception {
		//a journal state unlike the reference: nothing found, tutorial on
		SPDSettings.intro(true);
		final Document doc = Document.ADVENTURERS_GUIDE;
		final String page = doc.pageNames().iterator().next();
		final boolean foundBefore = doc.isPageFound(page);
		final AtomicReference<String> seen = new AtomicReference<>("");
		FloorReplay.replay(99L, 0, HeroClass.ROGUE, 6, new FloorReplay.Observer(){
			@Override
			public void level( int d, Level level ){
				if (d != 1) return;
				//the replay thread sees the reference journal and no tutorial...
				seen.set(doc.isPageFound(page) + "," + SPDSettings.intro());
				//...any other thread sees the player's own
				final AtomicReference<String> other = new AtomicReference<>("");
				Thread t = new Thread(new Runnable(){
					@Override
					public void run(){
						other.set(doc.isPageFound(page) + "," + SPDSettings.intro());
					}
				});
				t.start();
				try {
					t.join();
				} catch (InterruptedException e){
					throw new RuntimeException(e);
				}
				seen.set(seen.get() + "|" + other.get());
			}
		});
		assertEquals("true,false|" + foundBefore + ",true", seen.get());
		assertEquals(foundBefore, doc.isPageFound(page));
		assertTrue(SPDSettings.intro());
		assertFalse(ReplayContext.active());
	}

	//the player's discoveries and badges: a replay builds a hero kit and generates every item, none of which may count as discovered
	private static int discoveries() throws Exception {
		int n = 0;
		java.lang.reflect.Field f = com.shatteredpixel.shatteredpixeldungeon.journal.Catalog.class.getDeclaredField("seen");
		f.setAccessible(true);
		for (com.shatteredpixel.shatteredpixeldungeon.journal.Catalog c : com.shatteredpixel.shatteredpixeldungeon.journal.Catalog.values()){
			for (Object v : ((java.util.Map<?, ?>) f.get(c)).values()) if (Boolean.TRUE.equals(v)) n++;
		}
		return n;
	}

	private static java.lang.reflect.Field saveNeededField() throws Exception {
		java.lang.reflect.Field f = com.shatteredpixel.shatteredpixeldungeon.journal.Journal.class.getDeclaredField("saveNeeded");
		f.setAccessible(true);
		return f;
	}

	private static boolean saveNeeded() throws Exception {
		return saveNeededField().getBoolean(null);
	}

	private static void saveNeeded( boolean v ) throws Exception {
		saveNeededField().setBoolean(null, v);
	}

	@Test
	public void neverRecordsDiscoveriesOrBadgesForThePlayer() throws Exception {
		saveNeeded(false);
		int seen = discoveries();
		int badges = com.shatteredpixel.shatteredpixeldungeon.Badges.totalUnlocked(false);
		int globalBadges = com.shatteredpixel.shatteredpixeldungeon.Badges.totalUnlocked(true);
		for (HeroClass c : HeroClass.values()){
			FloorReplay.replay(8080L, 0, c, FloorReplay.LAST_FLOOR);
		}
		assertEquals(seen, discoveries());
		assertEquals(badges, com.shatteredpixel.shatteredpixeldungeon.Badges.totalUnlocked(false));
		assertEquals(globalBadges, com.shatteredpixel.shatteredpixeldungeon.Badges.totalUnlocked(true));
		assertFalse("a replay must not ask for the journal to be saved", saveNeeded());
	}

	@Test
	public void leavesTheDungeonStateAsFound() throws Exception {
		//a game state in memory that replay must not disturb (the title screen after a run)
		LevelGenerationHarness.generate(2468L, 0, HeroClass.MAGE, LevelGenerationHarness.Context.FRESH, 4);
		Object hero = Dungeon.hero;
		int depth = Dungeon.depth;
		long seed = Dungeon.seed;
		int challenges = Dungeon.challenges;
		int limited = Dungeon.LimitedDrops.STRENGTH_POTIONS.count;
		int souCount = Dungeon.LimitedDrops.UPGRADE_SCROLLS.count;
		Object scroll = com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.apdHandler();
		List<String> generatorBefore = bundleText();

		FloorReplay.replay(13579L, 1, HeroClass.HUNTRESS, FloorReplay.LAST_FLOOR);

		assertTrue(hero == Dungeon.hero);
		assertEquals(depth, Dungeon.depth);
		assertEquals(seed, Dungeon.seed);
		assertEquals(challenges, Dungeon.challenges);
		assertEquals(limited, Dungeon.LimitedDrops.STRENGTH_POTIONS.count);
		assertEquals(souCount, Dungeon.LimitedDrops.UPGRADE_SCROLLS.count);
		assertTrue(scroll == com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.apdHandler());
		assertSameBundles(generatorBefore, bundleText());
	}

	//the item generator, statistics, quests, limited drops and notes, as the text the game would save (one entry per part)
	private static List<String> bundleText() throws Exception {
		List<String> out = new ArrayList<>();
		for (int part = 0; part < 5; part++){
			com.watabou.utils.Bundle b = new com.watabou.utils.Bundle();
			switch (part){
				case 0: com.shatteredpixel.shatteredpixeldungeon.items.Generator.storeInBundle(b); break;
				case 1: com.shatteredpixel.shatteredpixeldungeon.Statistics.storeInBundle(b); break;
				case 2:
					com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost.Quest.storeInBundle(b);
					com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker.Quest.storeInBundle(b);
					com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.Quest.storeInBundle(b);
					com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.storeInBundle(b);
					break;
				case 3: Dungeon.LimitedDrops.store(b); break;
				default: com.shatteredpixel.shatteredpixeldungeon.journal.Notes.storeInBundle(b);
			}
			java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
			com.watabou.utils.Bundle.write(b, o, false);
			out.add(o.toString("UTF-8"));
		}
		return out;
	}

	private static void assertSameBundles( List<String> a, List<String> b ) throws Exception {
		String[] names = { "generator", "statistics", "quests", "limited drops", "notes" };
		for (int i = 0; i < a.size(); i++){
			assertTrue(names[i] + " changed:\n" + a.get(i) + "\n" + b.get(i), new org.json.JSONObject(a.get(i)).similar(new org.json.JSONObject(b.get(i))));
		}
	}

	@Test
	public void replayWorksWhenNothingWasEverLoaded() throws Exception {
		//a fresh start: no game was ever played in this process
		assertNotNull(FloorReplay.replay(31337L, 0, HeroClass.WARRIOR, 3));
	}
}
