package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ItemTestSupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemHistory;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunTimeline;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.watabou.utils.Random;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class ItemRollbackAndSafetyTest {

	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	//---- rollback ---------------------------------------------------------------------------------------------------

	@Test
	public void itemEventsAfterTheLoadedSaveBecomeAnAbandonedBranchThatIsKeptButNormallyExcluded() throws Exception {
		RecorderTestSupport.Session s = startRun(41L);
		String runId = ApdRecorder.currentRunId();
		LeatherArmor a = new LeatherArmor();
		a.collect();
		turn();
		String aUid = a.apdUid;
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.saveAll();   //the game the player will come back to

		//play that never reaches a save, then the app is killed
		LeatherArmor b = new LeatherArmor();
		b.collect();
		turn();
		String bUid = b.apdUid;
		assertNotNull(bUid);
		b.detachAll(Dungeon.hero.belongings.backpack);
		Dungeon.hero.belongings.armor = b;
		turn();

		s.restartApp();
		Dungeon.loadGame(s.slot);
		//the loaded game never had B
		for (Item i : Dungeon.hero.belongings) assertNotEquals(bUid, i.apdUid);
		turn();
		//play goes on: a new item gets an identity of its own, never B's
		LeatherArmor c = new LeatherArmor();
		c.collect();
		turn();
		assertNotNull(c.apdUid);
		assertNotEquals(bUid, c.apdUid);
		assertNotEquals(aUid, c.apdUid);

		ReadResult r = s.read(runId);
		RunTimeline t = RunTimeline.of(r.events);
		assertEquals(1, t.rollbackCount());

		//B's events exist in the log (nothing is deleted) ...
		boolean bInAll = false;
		for (ItemEvents.Acquired x : acquired(r)) if (x.uid.equals(bUid)) bInAll = true;
		assertTrue(bInAll);
		assertTrue(ItemHistory.allUids(r.events, r).contains(bUid));
		//... but the canonical view leaves them out, by default
		assertFalse(ItemHistory.allUids(t.canonical(), r).contains(bUid));
		assertTrue(ItemHistory.allUids(t.canonical(), r).contains(aUid));
		assertTrue(ItemHistory.allUids(t.canonical(), r).contains(c.apdUid));
		assertTrue(ItemHistory.equipIntervals(t.canonical(), r, bUid).isEmpty());
		assertEquals("the abandoned wearing is still readable on request", 1, ItemHistory.equipIntervals(r.events, r, bUid).size());
		assertNull(ItemHistory.visibleStateAt(t.canonical(), r, bUid, Long.MAX_VALUE));
		assertNotNull(ItemHistory.visibleStateAt(r.events, r, bUid, Long.MAX_VALUE));

		//the load agreed with the log about A, so it wrote no "caught up" events for it
		for (ApdEvent e : t.canonical()){
			if (e.type == EventTypes.ITEM_ACQUIRED){
				ItemEvents.Acquired x = ItemEvents.decodeAcquired(e, r);
				if (x.uid.equals(aUid)) assertNotEquals(ItemEvents.ACQ_RECONCILED, x.context);
			}
		}
	}

	@Test
	public void aCleanRestartDoesNotInventEquipmentIntervals() throws Exception {
		RecorderTestSupport.Session s = startRun(42L);
		String runId = ApdRecorder.currentRunId();
		Item weapon = Dungeon.hero.belongings.weapon;
		String uid = weapon.apdUid;
		Dungeon.saveAll();
		s.restartApp();
		Dungeon.loadGame(s.slot);
		turn();
		ReadResult r = s.read(runId);
		RunTimeline t = RunTimeline.of(r.events);
		List<ItemHistory.Interval> iv = ItemHistory.equipIntervals(t.canonical(), r, uid);
		assertEquals("one continuous interval across the restart", 1, iv.size());
		assertTrue(iv.get(0).isOpen());
		assertFalse(iv.get(0).startReconciled);
	}

	//---- derived histories ---------------------------------------------------------------------------------------

	@Test
	public void derivedHistoriesAreRebuiltFromTheRawEventsAndAlwaysAgree() throws Exception {
		RecorderTestSupport.Session s = startRun(43L);
		LeatherArmor a = new LeatherArmor();
		a.collect();
		turn();
		Item piece = a.duplicate();
		piece.collect();
		turn();
		ReadResult r = finish(s);
		RunTimeline t = RunTimeline.of(r.events);
		for (String uid : ItemHistory.allUids(t.canonical(), r)){
			assertEquals(ItemHistory.ancestors(t.canonical(), r, uid), ItemHistory.ancestors(t.canonical(), r, uid));
			assertEquals(ItemHistory.equipIntervals(t.canonical(), r, uid).size(), ItemHistory.equipIntervals(t.canonical(), r, uid).size());
		}
		assertTrue(ItemHistory.ancestors(t.canonical(), r, piece.apdUid).contains(a.apdUid));
	}

	//---- the recorder never harms the game -----------------------------------------------------------------------

	@Test
	public void aFailingRecorderNeverStopsItemsFromWorking() throws Exception {
		RecorderTestSupport.Session s = startRun(44L);
		Item stack = new PotionOfHealing();
		stack.quantity(5);
		stack.collect();
		turn();
		ApdRecorder.failureInjection = new RuntimeException("injected");
		for (int i = 0; i < ApdRecorder.MAX_ERRORS + 3; i++){
			Item p = stack.split(1);                      //these must keep working while every hook throws inside
			assertNotNull(p);
			assertTrue(p.collect());                      //merge
			Item d = stack.duplicate();
			assertNotNull(d);
			turn();
		}
		assertEquals(5, stack.quantity());
		assertTrue(ApdRecorder.status().errorCount >= ApdRecorder.MAX_ERRORS);
		assertTrue("after repeated failures the recorder switches itself off", ApdRecorder.status().sessionDisabled);
		assertFalse(ApdItems.active);
		//with the recorder off, pieces simply have no identity and nothing breaks
		Item p = stack.split(2);
		assertNull(p.apdUid);
		assertEquals(3, stack.quantity());
		//the original identity is untouched
		assertNotNull(stack.apdUid);
	}

	@Test
	public void aNullOrOddItemNeverThrowsOutOfAHook() throws Exception {
		RecorderTestSupport.Session s = startRun(45L);
		ApdItems.noteAction(null, null);
		ApdItems.onSplit(null, null, 1);
		ApdItems.onMerge(null, null, 1);
		ApdItems.onDetached(null, null);
		ApdItems.onTransformed(null, null, null);
		ApdItems.onCrafted(new java.util.ArrayList<Item>(), null);
		ApdItems.onClassKnown(Item.class, null, null);
		ApdItems.noteAcquireContext(-5);
		turn();
		assertEquals("odd input is not a recorder failure", 0, ApdRecorder.status().errorCount);
		finish(s);
	}

	@Test
	public void itemTrackingNeverConsumesTheGamesRandomNumbers() throws Exception {
		int[] off = drawAfterItemWork(false);
		int[] on = drawAfterItemWork(true);
		assertEquals(off[0], on[0]);
		assertEquals(off[1], on[1]);
	}

	//does a lot of item work under a seeded generator, then reports the next two numbers it produces
	private static int[] drawAfterItemWork( boolean record ) throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		ApdRecorder.configure(s.root, record);
		RecorderTestSupport.newRun(s, 777L);
		RecorderTestSupport.enterLevel(1);
		Random.pushGenerator(123456789L);
		try {
			Item stack = new PotionOfHealing();
			stack.quantity(6);
			stack.collect();
			turn();
			Item piece = stack.split(2);
			piece.collect();
			Item dupe = stack.duplicate();
			putInSight(dupe);
			putInSight(new LeatherArmor());
			turn();
			ApdItems.noteAction(stack, "DRINK");
			stack.detach(Dungeon.hero.belongings.backpack);
			turn();
			Dungeon.saveAll();
			turn();
			return new int[]{Random.Int(), Random.Int()};
		} finally {
			Random.popGenerator();
			ApdRecorder.reset();
		}
	}

	@Test
	public void identitiesAreGeneratedWithoutTheGamesGenerator() throws Exception {
		//two identical runs get completely different identities: they come from outside the seeded random numbers
		RecorderTestSupport.Session s1 = startRun(46L);
		String u1 = Dungeon.hero.belongings.weapon.apdUid;
		ApdRecorder.reset();
		RecorderTestSupport.Session s2 = startRun(46L);
		String u2 = Dungeon.hero.belongings.weapon.apdUid;
		assertNotEquals(u1, u2);
	}
}
