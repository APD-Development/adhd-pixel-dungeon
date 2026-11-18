package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ItemTestSupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.watabou.utils.Bundle;

import org.junit.After;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class ItemIdentityTest {

	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	@Test
	public void theStartKitGetsUniqueIdentitiesAndIsRecordedAsTheStartKit() throws Exception {
		RecorderTestSupport.Session s = startRun(11L);
		Set<String> uids = new HashSet<>();
		int n = 0;
		for (Item i : Dungeon.hero.belongings){
			if (i.quantity() <= 0) continue;
			assertNotNull(i.getClass().getSimpleName() + " has no identity", i.apdUid);
			assertTrue("duplicate uid " + i.apdUid, uids.add(i.apdUid));
			n++;
		}
		assertTrue(n >= 5);
		ReadResult r = finish(s);
		int kit = 0;
		for (ItemEvents.Acquired a : acquired(r)){
			assertEquals(ItemEvents.ACQ_START_KIT, a.context);
			kit++;
		}
		assertEquals(n, kit);
	}

	@Test
	public void hundredsOfObservedItemsAllGetDifferentIdentities() throws Exception {
		RecorderTestSupport.Session s = startRun(12L);
		Set<String> uids = new HashSet<>();
		for (Item i : Dungeon.hero.belongings) if (i.quantity() > 0) uids.add(i.apdUid);
		for (int k = 0; k < 150; k++){
			Item it = k % 2 == 0 ? new Food() : new PotionOfHealing();
			putInSight(it);
			turn();
			assertNotNull(it.apdUid);
			assertTrue("uid collision " + it.apdUid, uids.add(it.apdUid));
		}
		finish(s);
	}

	@Test
	public void identityIsSavedWithTheItemAndSurvivesAnAppRestart() throws Exception {
		RecorderTestSupport.Session s = startRun(13L);
		Item food = new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor();
		food.collect();
		turn();
		String uid = food.apdUid;
		assertNotNull(uid);
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.saveAll();
		String saved = new String(java.nio.file.Files.readAllBytes(RecorderTestSupport.saveFile(s.slot).toPath()), "ISO-8859-1");
		assertNotNull(RecorderTestSupport.saveBundle(s.slot));

		s.restartApp();
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.loadGame(s.slot);
		boolean found = false;
		for (Item i : Dungeon.hero.belongings) if (uid.equals(i.apdUid) && i instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor) found = true;
		assertTrue("the armor lost its identity through save and load", found);
		turn();
		ReadResult r = finish(s);
		//a clean load adds nothing that says the log and the game disagreed
		for (ItemEvents.Acquired a : acquired(r)) assertNotEquals(ItemEvents.ACQ_RECONCILED, a.context);
		for (ItemEvents.Left l : left(r)) assertNotEquals(ItemEvents.LEFT_RECONCILED, l.reason);
	}

	@Test
	public void aSaveFromBeforeItemIdentityLoadsAndItsItemsGetIdentitiesWithoutInventedHistory() throws Exception {
		//play without the recorder: the save has no apd_uid and no apd_run_id
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		ApdRecorder.configure(s.root, false);
		RecorderTestSupport.newRun(s, 14L);
		RecorderTestSupport.enterLevel(1);
		new Food().collect();
		Dungeon.saveAll();
		assertFalse(bundleText(RecorderTestSupport.saveBundle(s.slot)).contains("apd_uid"));

		ApdRecorder.configure(s.root, true);
		Dungeon.loadGame(s.slot);
		for (Item i : Dungeon.hero.belongings) if (i.quantity() > 0) assertNotNull(i.apdUid);
		ReadResult r = finish(s);
		assertTrue(registered(r).size() >= 5);
		//everything it found it reports as caught up, nothing is claimed to have happened at a particular moment
		for (ItemEvents.Acquired a : acquired(r)) assertEquals(ItemEvents.ACQ_RECONCILED, a.context);
	}

	@Test
	public void aSplitPieceIsANewItemLinkedToItsParent() throws Exception {
		RecorderTestSupport.Session s = startRun(15L);
		Item stack = new PotionOfHealing();
		stack.quantity(5);
		stack.collect();
		turn();
		String parent = stack.apdUid;
		Item piece = stack.split(2);
		assertNotNull(piece);
		assertNotNull(piece.apdUid);
		assertNotEquals(parent, piece.apdUid);
		assertEquals(parent, stack.apdUid);
		assertEquals(3, stack.quantity());
		assertEquals(2, piece.quantity());

		//both survive a bundle round trip as different identities
		Bundle b = new Bundle();
		b.put("a", stack);
		b.put("b", piece);
		assertEquals(parent, ((Item) b.get("a")).apdUid);
		assertEquals(piece.apdUid, ((Item) b.get("b")).apdUid);

		ReadResult r = finish(s);
		boolean found = false;
		for (ItemEvents.Lineage l : lineage(r)){
			if (l.kind == ItemEvents.LIN_SPLIT && l.to.equals(piece.apdUid)){
				assertEquals(1, l.from.size());
				assertEquals(parent, l.from.get(0));
				assertEquals(2, l.amount);
				found = true;
			}
		}
		assertTrue(found);
	}

	@Test
	public void mergingAPieceBackKeepsTheSurvivorsIdentityAndRetiresTheOther() throws Exception {
		RecorderTestSupport.Session s = startRun(16L);
		Item stack = new PotionOfHealing();
		stack.quantity(5);
		stack.collect();
		turn();
		String parent = stack.apdUid;
		Item piece = stack.split(2);
		String pieceUid = piece.apdUid;
		assertTrue(piece.collect()); //merges into the stack
		assertEquals(5, stack.quantity());
		assertEquals(parent, stack.apdUid);
		turn();
		ReadResult r = finish(s);
		boolean merge = false;
		for (ItemEvents.Lineage l : lineage(r)){
			if (l.kind == ItemEvents.LIN_MERGE && l.to.equals(parent) && l.from.contains(pieceUid)){
				assertEquals(2, l.amount);
				merge = true;
			}
		}
		assertTrue("no MERGE lineage", merge);
		//the merged-away piece never shows up as something the hero left or holds
		for (ItemEvents.Left l : left(r)) assertNotEquals(pieceUid, l.uid);
	}

	@Test
	public void aDuplicateIsADifferentIdentityAndPreviewCopiesNeverBecomeItems() throws Exception {
		RecorderTestSupport.Session s = startRun(17L);
		Item potion = new PotionOfHealing();
		potion.collect();
		turn();
		String uid = potion.apdUid;
		int registeredBefore = registeredCount(s);

		//the alchemy screen makes throwaway copies like this on every refresh
		for (int i = 0; i < 20; i++){
			Item preview = potion.duplicate();
			assertNull("a copy must not share the source's identity", preview.apdUid);
			assertEquals(uid, preview.apdCopyOf);
		}
		turn();
		assertEquals("throwaway copies must not be registered", registeredBefore, registeredCount(s));

		//a copy that really enters the game gets its own identity and a COPY link
		Item real = potion.duplicate();
		assertTrue(real.collect());
		turn();
		assertNotNull(real.apdUid);
		assertNotEquals(uid, real.apdUid);
		ReadResult r = finish(s);
		boolean copy = false;
		for (ItemEvents.Lineage l : lineage(r)){
			if (l.kind == ItemEvents.LIN_COPY && l.to.equals(real.apdUid) && l.from.contains(uid)) copy = true;
		}
		assertTrue("no COPY lineage", copy);
	}

	private static String bundleText( Bundle b ) throws Exception {
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		Bundle.write(b, out, false);
		return new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
	}

	private static int registeredCount( RecorderTestSupport.Session s ) throws Exception {
		ApdRecorder.flushForTests();
		ReadResult r = com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader.read(s.log(ApdRecorder.currentRunId()));
		return registered(r).size();
	}

	@Test
	public void identityOfItemsOnTheFloorSurvivesSavingAndLoadingTheLevel() throws Exception {
		RecorderTestSupport.Session s = startRun(18L);
		Item armor = new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor();
		Heap heap = putInSight(armor);
		turn();
		String uid = armor.apdUid;
		assertNotNull(uid);
		int cell = heap.pos;
		Dungeon.saveAll();
		com.shatteredpixel.shatteredpixeldungeon.levels.Level reloaded = Dungeon.loadLevel(s.slot);
		Heap loaded = reloaded.heaps.get(cell);
		assertNotNull(loaded);
		assertEquals(uid, loaded.peek().apdUid);
		finish(s);
	}

	@Test
	public void stackingOnTheFloorIsAMergeAndKeepsTheFirstItemsIdentity() throws Exception {
		RecorderTestSupport.Session s = startRun(19L);
		Item first = new PotionOfHealing();
		int cell = freeCell();
		putInSight(first, cell);
		turn();
		String firstUid = first.apdUid;
		Item second = new PotionOfHealing();
		putInSight(second, cell); //the heap merges the new potion into the one lying there
		turn();
		ReadResult r = finish(s);
		boolean merged = false;
		for (ItemEvents.Lineage l : lineage(r)){
			if (l.kind == ItemEvents.LIN_MERGE && l.from.contains(second.apdUid)){
				assertEquals(firstUid, l.to);
				merged = true;
			}
		}
		assertTrue(merged);
	}

	@Test
	public void theSourceOfAnAcquisitionIsRecordedOnceThenForgotten() throws Exception {
		RecorderTestSupport.Session s = startRun(20L);
		Item a = new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor();
		ApdItems.noteAcquireContext(ItemEvents.ACQ_PICKUP);
		a.collect();
		turn();
		Item b = new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor();
		b.collect();
		turn();
		ReadResult r = finish(s);
		for (ItemEvents.Acquired x : acquired(r)){
			if (x.uid.equals(a.apdUid)) assertEquals(ItemEvents.ACQ_PICKUP, x.context);
			if (x.uid.equals(b.apdUid)) assertEquals(ItemEvents.ACQ_OTHER, x.context);
		}
	}
}
