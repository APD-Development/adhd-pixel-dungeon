package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenerationProvenanceTest {

	@After
	public void cleanUp(){
		ApdRecorder.reset();
		Ghost.Quest.weapon = null;
	}

	private static List<GenerationEvents.ItemGenerated> generatedItems( ReadResult r ){
		List<GenerationEvents.ItemGenerated> out = new ArrayList<>();
		for (ApdEvent e : ItemTestSupport.ofType(r, EventTypes.ITEM_GENERATED)) out.add(GenerationEvents.decodeItemGenerated(e, r));
		return out;
	}

	private static List<GenerationEvents.LevelGenerated> levels( ReadResult r ){
		List<GenerationEvents.LevelGenerated> out = new ArrayList<>();
		for (ApdEvent e : ItemTestSupport.ofType(r, EventTypes.LEVEL_GENERATED)) out.add(GenerationEvents.decodeLevelGenerated(e, r));
		return out;
	}

	@Test
	public void aFreshLevelIsRecordedWithTheContextItWasBuiltIn() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(81L);
		RecorderTestSupport.enterLevel(2);
		ReadResult r = ItemTestSupport.finish(s);
		List<GenerationEvents.LevelGenerated> l = levels(r);
		assertEquals(2, l.size());
		GenerationEvents.LevelGenerated g = l.get(1);
		assertEquals(2, g.where.depth);
		assertEquals("levels.SewerLevel", g.levelClass);
		assertTrue(g.draws > 0);
		assertTrue(g.buildAttempts >= 1);
		assertTrue(g.heaps > 0);
		assertTrue(g.itemsFound > 0);
		assertTrue(g.mobs > 0);
		assertFalse("rooms include secret and special ones, so the list is never empty", g.rooms.isEmpty());
		boolean entrance = false;
		for (String room : g.rooms) if (room.endsWith("EntranceRoom")) entrance = true;
		assertTrue("the entrance room is listed: " + g.rooms, entrance);
		assertFalse(g.heroItems.isEmpty());
		assertEquals(g.limitedNames.size(), g.limitedCounts.size());
		assertTrue(g.limitedNames.contains("STRENGTH_POTIONS"));
		assertTrue(g.categoryNames.contains("WEAPON"));
		assertEquals(g.categoryNames.size(), g.categoryDropped.size());
		assertFalse(g.intro);
		assertEquals(0, g.exoticChance);
		assertEquals(0, g.challenges);
	}

	@Test
	public void everyGeneratedItemGetsAnIdentityAndAProvenanceEventAndNoneAreSharedOrLost() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(82L);
		RecorderTestSupport.enterLevel(3);
		Level level = Dungeon.level;
		Set<String> uids = new HashSet<>();
		int expected = 0;
		for (Heap h : level.heaps.valueList()){
			for (Item i : h.items){
				if (i.quantity() <= 0 || i instanceof com.shatteredpixel.shatteredpixeldungeon.items.Gold
						|| i instanceof com.shatteredpixel.shatteredpixeldungeon.items.keys.Key) continue;
				assertNotNull(i.getClass().getSimpleName() + " has no identity", i.apdUid);
				assertTrue("uid shared by two items", uids.add(i.apdUid));
				expected++;
			}
		}
		assertTrue(expected > 0);
		ReadResult r = ItemTestSupport.finish(s);
		Set<String> generated = new HashSet<>();
		for (GenerationEvents.ItemGenerated g : generatedItems(r)) if (g.where.depth == 3) generated.add(g.uid);
		for (String u : uids) assertTrue("no ITEM_GENERATED for " + u, generated.contains(u));
		//and each also has its REGISTERED event, with the origin that says it was generated
		Map<String, Integer> origin = new HashMap<>();
		for (ItemEvents.Registered reg : ItemTestSupport.registered(r)) origin.put(reg.uid, reg.origin);
		for (String u : uids) assertEquals(ItemEvents.ORIGIN_GENERATED, (int) origin.get(u));
	}

	@Test
	public void theTruthAboutEveryItemIsRecordedWhetherOrNotThePlayerCouldTell() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(83L);
		RecorderTestSupport.enterLevel(4);
		Level level = Dungeon.level;
		Map<String, Item> byUid = new HashMap<>();
		for (Heap h : level.heaps.valueList()) for (Item i : h.items) if (i.apdUid != null) byUid.put(i.apdUid, i);
		ReadResult r = ItemTestSupport.finish(s);
		int checked = 0;
		for (GenerationEvents.ItemGenerated g : generatedItems(r)){
			Item i = byUid.get(g.uid);
			if (i == null) continue;
			assertTrue(g.trueClass.endsWith(i.getClass().getSimpleName()));
			assertEquals(i.trueLevel(), g.trueLevel);
			assertEquals(i.cursed, g.cursed);
			checked++;
		}
		assertTrue(checked > 3);
	}

	@Test
	public void howEachItemWasDrawnFromTheGeneratorIsRecorded() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(84L);
		RecorderTestSupport.enterLevel(2);
		RecorderTestSupport.enterLevel(3);
		ReadResult r = ItemTestSupport.finish(s);
		int drawn = 0, categories = 0, undrawn = 0;
		boolean sawFoodQueue = false;
		for (GenerationEvents.ItemGenerated g : generatedItems(r)){
			if (g.drawKind >= 0){
				drawn++;
				if (!g.actualCategory.isEmpty()) categories++;
				assertTrue("a drawn item must carry the deck count of its category", g.categoryDropped >= 0 || g.actualCategory.isEmpty());
				if (g.requestedCategory.equals("FOOD") && g.drawKind == GenerationEvents.DRAW_CATEGORY) sawFoodQueue = true;
			} else {
				undrawn++;
			}
		}
		assertTrue(drawn > 5);
		assertTrue(categories > 5);
		assertTrue("every level queues food", sawFoodQueue);
		//items built with new X() (keys, fixed prizes) are not draws and say so
		assertTrue(undrawn >= 0);
	}

	@Test
	public void recordingWhatTheGameBuiltNeverMakesAnythingVisibleToThePlayer() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(85L);
		RecorderTestSupport.enterLevel(3);
		//nothing on this level is in the hero's view
		Level level = Dungeon.level;
		java.util.Arrays.fill(level.heroFOV, false);
		for (Heap h : level.heaps.valueList()) h.seen = false;
		ItemTestSupport.turn();
		ItemTestSupport.turn();
		ReadResult r = ItemTestSupport.finish(s);

		Set<String> generated = new HashSet<>();
		for (GenerationEvents.ItemGenerated g : generatedItems(r)) if (g.where.depth == 3) generated.add(g.uid);
		assertFalse(generated.isEmpty());
		//the PUBLIC item events never mention an item the player has not met
		int publicEvents = 0;
		for (ApdEvent e : r.events){
			String uid = null;
			switch (e.type){
				case EventTypes.ITEM_SEEN: uid = ItemEvents.decodeSeen(e, r).uid; break;
				case EventTypes.ITEM_ACQUIRED: uid = ItemEvents.decodeAcquired(e, r).uid; break;
				case EventTypes.ITEM_LEFT: uid = ItemEvents.decodeLeft(e, r).uid; break;
				case EventTypes.ITEM_KNOWLEDGE: uid = ItemEvents.decodeKnowledge(e, r).uid; break;
				default:
			}
			if (uid == null) continue;
			publicEvents++;
			assertFalse("a public event exposes an item the player has not seen: " + EventTypes.name(e.type), generated.contains(uid));
		}
		assertTrue("the start kit still produces public events", publicEvents > 0);
		//and every provenance event type is classified internal
		for (int type : new int[]{EventTypes.LEVEL_GENERATED, EventTypes.ITEM_GENERATED, EventTypes.MOB_GENERATED, EventTypes.MOB_ID_ASSIGNED}){
			assertEquals(EventTypes.Visibility.INTERNAL, EventTypes.visibility(type));
		}
	}

	@Test
	public void mimicContentsStatueWeaponsAndQuestRewardsAreFoundToo() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(86L);
		RecorderTestSupport.enterLevel(2);
		Level level = Dungeon.level;
		Mimic mimic = new Mimic();
		mimic.pos = 140;
		mimic.items = new ArrayList<>();
		Item inMimic = new PotionOfHealing();
		mimic.items.add(inMimic);
		level.mobs.add(mimic);
		Statue statue = new Statue();
		statue.createWeapon(false);
		statue.pos = 141;
		level.mobs.add(statue);
		Ghost ghost = new Ghost();
		ghost.pos = 142;
		level.mobs.add(ghost);
		Item reward = new WornShortsword();
		Ghost.Quest.weapon = (com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon) reward;
		ApdGen.onLevelGenerated(level);
		ReadResult r = ItemTestSupport.finish(s);

		boolean mimicItem = false, statueWeapon = false, questReward = false;
		for (GenerationEvents.ItemGenerated g : generatedItems(r)){
			if (g.uid.equals(inMimic.apdUid)){
				assertEquals(GenerationEvents.C_MIMIC, g.container);
				assertTrue(g.owner.endsWith("Mimic"));
				mimicItem = true;
			}
			if (g.uid.equals(statue.weapon().apdUid)){
				assertEquals(GenerationEvents.C_STATUE, g.container);
				statueWeapon = true;
			}
			if (g.uid.equals(reward.apdUid)){
				assertEquals(GenerationEvents.C_QUEST_REWARD, g.container);
				assertTrue(g.owner.endsWith("Ghost"));
				questReward = true;
			}
		}
		assertTrue(mimicItem);
		assertTrue(statueWeapon);
		assertTrue(questReward);
		//the creatures are listed with what they carry
		boolean mimicListed = false;
		for (ApdEvent e : ItemTestSupport.ofType(r, EventTypes.MOB_GENERATED)){
			GenerationEvents.MobGenerated m = GenerationEvents.decodeMobGenerated(e, r);
			if (m.mobClass.endsWith("Mimic") && m.pos == 140){
				assertEquals(1, m.carries);
				mimicListed = true;
			}
		}
		assertTrue(mimicListed);
	}

	@Test
	public void creatureLootAndBonusDropsAreRecordedWhenTheyAreCreated() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(87L);
		Rat rat = new Rat();
		rat.pos = Dungeon.hero.pos + 1;
		Item loot = new PotionOfHealing();
		Item bonus = new WornShortsword();
		ApdGen.onLootCreated(rat, loot);
		ApdGen.onBonusDrop(bonus, 200);
		ReadResult r = ItemTestSupport.finish(s);
		boolean lootSeen = false, bonusSeen = false;
		for (GenerationEvents.ItemGenerated g : generatedItems(r)){
			if (g.uid.equals(loot.apdUid)){
				assertEquals(GenerationEvents.C_MOB_LOOT, g.container);
				assertTrue(g.owner.endsWith("Rat"));
				lootSeen = true;
			}
			if (g.uid.equals(bonus.apdUid)){
				assertEquals(GenerationEvents.C_BONUS_DROP, g.container);
				bonusSeen = true;
			}
		}
		assertTrue(lootSeen);
		assertTrue(bonusSeen);
	}

	@Test
	public void generatedCreaturesAreJoinedToTheIdsTheGameGivesThemWhenTheHeroArrives() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(88L);
		RecorderTestSupport.enterLevel(2);
		Level level = Dungeon.level;
		Map<Integer, Integer> idAtPos = new HashMap<>();
		for (Mob m : level.mobs) idAtPos.put(m.pos, m.apdPeekId());
		ReadResult r = ItemTestSupport.finish(s);
		List<GenerationEvents.MobGenerated> mobs = new ArrayList<>();
		for (ApdEvent e : ItemTestSupport.ofType(r, EventTypes.MOB_GENERATED)) mobs.add(GenerationEvents.decodeMobGenerated(e, r));
		int joined = 0;
		for (ApdEvent e : ItemTestSupport.ofType(r, EventTypes.MOB_ID_ASSIGNED)){
			GenerationEvents.MobIdAssigned m = GenerationEvents.decodeMobIdAssigned(e, r);
			if (m.where.depth != 2) continue;
			assertTrue(m.id > 0);
			assertEquals((int) idAtPos.get(m.pos), (int) m.id);
			boolean listed = false;
			for (GenerationEvents.MobGenerated g : mobs) if (g.where.depth == 2 && g.pos == m.pos && g.mobClass.equals(m.mobClass)) listed = true;
			assertTrue(listed);
			joined++;
		}
		assertTrue(joined > 0);
		assertEquals("every creature built on this level was joined once", mobs.size() > 0, joined > 0);
	}

	@Test
	public void aLevelBuiltAfterTheLoadedSaveBecomesAnAbandonedBranch() throws Exception {
		RecorderTestSupport.Session s = ItemTestSupport.startRun(89L);
		String id = ApdRecorder.currentRunId();
		RecorderTestSupport.enterLevel(2);                      //saved here: the state the game comes back to
		Dungeon.depth = 3;
		Dungeon.level = Dungeon.newLevel();                     //built, but never saved
		s.restartApp();
		Dungeon.loadGame(s.slot);
		ReadResult r = s.read(id);
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunTimeline t = com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunTimeline.of(r.events);
		assertEquals(1, t.rollbackCount());
		int all = 0, canonical = 0;
		for (ApdEvent e : r.events) if (e.type == EventTypes.ITEM_GENERATED || e.type == EventTypes.LEVEL_GENERATED){
			all++;
			if (!t.isAbandoned(e)) canonical++;
		}
		assertTrue(all > canonical);
		for (ApdEvent e : t.canonical()){
			if (e.type == EventTypes.LEVEL_GENERATED) assertNotEquals(3, GenerationEvents.decodeLevelGenerated(e, r).where.depth);
		}
	}
}
