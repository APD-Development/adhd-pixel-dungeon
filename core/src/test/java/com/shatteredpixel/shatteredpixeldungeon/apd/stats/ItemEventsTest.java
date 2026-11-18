package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ItemEventsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private ReadResult roundTrip( java.util.function.Consumer<RunLogWriter> body ) throws Exception {
		File f = new File(tmp.getRoot(), "t" + System.nanoTime() + ".apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "t", "test", 1);
		body.accept(w);
		w.close();
		return RunLogReader.read(f);
	}

	private static ItemEvents.VisibleState vis( String name, boolean levelKnown, int level, boolean cursedKnown, boolean cursed ){
		ItemEvents.VisibleState v = new ItemEvents.VisibleState();
		v.category = ItemEvents.CAT_RING;
		v.name = name;
		v.quantity = 3;
		v.levelKnown = levelKnown;
		v.visibleLevel = level;
		v.cursedKnown = cursedKnown;
		v.visiblyCursed = cursed;
		v.identified = levelKnown && cursedKnown;
		return v;
	}

	@Test
	public void everyItemEventRoundTrips() throws Exception {
		final ItemEvents.Where where = new ItemEvents.Where();
		where.depth = 7; where.branch = 1; where.pos = 345;
		ReadResult r = roundTrip(w -> {
			ItemEvents.Seen s = new ItemEvents.Seen();
			s.uid = "u1"; s.where = where; s.seenWhere = ItemEvents.SEEN_SHOP; s.state = vis("ruby ring", false, 0, false, false);
			ItemEvents.emitSeen(w, 10, 0, s);
			ItemEvents.Acquired a = new ItemEvents.Acquired();
			a.uid = "u1"; a.where = where; a.context = ItemEvents.ACQ_CRAFT; a.state = vis("ruby ring", false, 0, false, false);
			ItemEvents.emitAcquired(w, 11, ItemEvents.FLAG_RECONCILED, a);
			ItemEvents.Left l = new ItemEvents.Left();
			l.uid = "u1"; l.where = where; l.reason = ItemEvents.LEFT_USED; l.action = "DRINK"; l.state = vis("x", true, -2, true, true);
			ItemEvents.emitLeft(w, 12, 0, l);
			ItemEvents.Equip q = new ItemEvents.Equip();
			q.uid = "u1"; q.where = where; q.slot = ItemEvents.SLOT_RING; q.context = ItemEvents.CTX_RECONCILED; q.action = "EQUIP"; q.state = vis("x", true, 1, true, false);
			ItemEvents.emitEquip(w, 13, 0, true, q);
			ItemEvents.emitEquip(w, 14, 0, false, q);
			ItemEvents.Knowledge k = new ItemEvents.Knowledge();
			k.uid = "u1"; k.where = where; k.state = vis("Ring of Wealth", true, 3, true, false);
			ItemEvents.emitKnowledge(w, 15, 0, k);
			ItemEvents.ClassKnown c = new ItemEvents.ClassKnown();
			c.where = where; c.category = ItemEvents.CAT_POTION; c.label = "crimson potion"; c.className = "PotionOfHealing";
			ItemEvents.emitClassKnown(w, 16, c);
			ItemEvents.Registered g = new ItemEvents.Registered();
			g.uid = "u1"; g.where = where; g.origin = ItemEvents.ORIGIN_SPLIT; g.trueClass = "items.rings.RingOfWealth"; g.trueLevel = -4; g.cursed = true; g.enchant = "Vorpal"; g.quantity = 2;
			ItemEvents.emitRegistered(w, 17, 0, g);
			ItemEvents.Lineage n = new ItemEvents.Lineage();
			n.kind = ItemEvents.LIN_CRAFT; n.from.add("a"); n.from.add("b"); n.to = "c"; n.amount = 4; n.cause = "alchemy"; n.where = where;
			ItemEvents.emitLineage(w, 18, n);
			ItemEvents.TruthChanged t = new ItemEvents.TruthChanged();
			t.uid = "u1"; t.where = where; t.oldLevel = 1; t.newLevel = 2; t.oldCursed = true; t.newCursed = false; t.oldEnchant = "A"; t.newEnchant = "B";
			ItemEvents.emitTruthChanged(w, 19, 0, t);
		});
		assertEquals(10, r.events.size() - 0);
		ItemEvents.Seen s = ItemEvents.decodeSeen(r.events.get(0), r);
		assertEquals("u1", s.uid);
		assertEquals(345, s.where.pos);
		assertEquals(ItemEvents.SEEN_SHOP, s.seenWhere);
		assertEquals("ruby ring", s.state.name);
		assertEquals(3, s.state.quantity);
		ItemEvents.Acquired a = ItemEvents.decodeAcquired(r.events.get(1), r);
		assertEquals(ItemEvents.ACQ_CRAFT, a.context);
		assertEquals(ItemEvents.FLAG_RECONCILED, r.events.get(1).flags);
		ItemEvents.Left l = ItemEvents.decodeLeft(r.events.get(2), r);
		assertEquals("DRINK", l.action);
		assertEquals(-2, l.state.visibleLevel);
		assertTrue(l.state.visiblyCursed);
		assertEquals(EventTypes.ITEM_EQUIPPED, r.events.get(3).type);
		assertEquals(EventTypes.ITEM_UNEQUIPPED, r.events.get(4).type);
		assertEquals(ItemEvents.SLOT_RING, ItemEvents.decodeEquip(r.events.get(3), r).slot);
		assertEquals(3, ItemEvents.decodeKnowledge(r.events.get(5), r).state.visibleLevel);
		ItemEvents.ClassKnown c = ItemEvents.decodeClassKnown(r.events.get(6), r);
		assertEquals("crimson potion", c.label);
		assertEquals("PotionOfHealing", c.className);
		ItemEvents.Registered g = ItemEvents.decodeRegistered(r.events.get(7), r);
		assertEquals(-4, g.trueLevel);
		assertEquals("Vorpal", g.enchant);
		ItemEvents.Lineage n = ItemEvents.decodeLineage(r.events.get(8), r);
		assertEquals(ItemEvents.LIN_CRAFT, n.kind);
		assertEquals(2, n.from.size());
		assertEquals("c", n.to);
		ItemEvents.TruthChanged t = ItemEvents.decodeTruthChanged(r.events.get(9), r);
		assertEquals(2, t.newLevel);
		assertFalse(t.newCursed);
	}

	@Test
	public void theEncoderItselfRefusesToWriteAnUnknownLevelOrCurse() throws Exception {
		//even if a caller passes a level or curse for something the player does not know about, the event cannot hold it
		ReadResult r = roundTrip(w -> {
			ItemEvents.Seen s = new ItemEvents.Seen();
			s.uid = "u";
			s.state = vis("ruby ring", false, 5, false, true); //levelKnown=false but visibleLevel=5; cursedKnown=false but cursed
			ItemEvents.emitSeen(w, 1, 0, s);
		});
		ItemEvents.Seen s = ItemEvents.decodeSeen(r.events.get(0), r);
		assertEquals(0, s.state.visibleLevel);
		assertFalse(s.state.visiblyCursed);
	}

	@Test
	public void everyEventTypeHasAnExplicitVisibility() throws Exception {
		List<Integer> internal = new ArrayList<>();
		internal.add(EventTypes.CHECKPOINT);
		internal.add(EventTypes.ITEM_REGISTERED);
		internal.add(EventTypes.ITEM_LINEAGE);
		internal.add(EventTypes.ITEM_TRUTH_CHANGED);
		internal.add(EventTypes.ATTACK_DETAIL);
		internal.add(EventTypes.DAMAGE_DETAIL);
		internal.add(EventTypes.KILL_DETAIL);
		internal.add(EventTypes.LEVEL_GENERATED);
		internal.add(EventTypes.ITEM_GENERATED);
		internal.add(EventTypes.MOB_GENERATED);
		internal.add(EventTypes.MOB_ID_ASSIGNED);
		internal.add(EventTypes.RANKING_LINK);
		internal.add(EventTypes.HEAL_DETAIL);
		internal.add(EventTypes.FACT_INTERNAL);
		internal.add(EventTypes.FACT_SERIES_INTERNAL);
		for (int type : EventTypes.all().keySet()){
			EventTypes.Visibility v = EventTypes.visibility(type);
			assertEquals(EventTypes.name(type), internal.contains(type) ? EventTypes.Visibility.INTERNAL : EventTypes.Visibility.PUBLIC, v);
		}
		//a type nobody has classified yet is hidden until someone decides
		assertEquals(EventTypes.Visibility.INTERNAL, EventTypes.visibility(60000));
	}

	@Test
	public void theKnowledgeKeyChangesWhenWhatThePlayerKnowsChanges() {
		ItemEvents.VisibleState a = vis("ruby ring", false, 0, false, false);
		ItemEvents.VisibleState b = vis("ruby ring", false, 0, false, false);
		b.quantity = 9; //quantity alone is not knowledge
		assertEquals(a.knowledgeKey(), b.knowledgeKey());
		assertNotEquals(a.knowledgeKey(), vis("ruby ring", true, 3, false, false).knowledgeKey());
		assertNotEquals(a.knowledgeKey(), vis("ruby ring", false, 0, true, true).knowledgeKey());
		assertNotEquals(a.knowledgeKey(), vis("Ring of Wealth", false, 0, false, false).knowledgeKey());
	}
}
