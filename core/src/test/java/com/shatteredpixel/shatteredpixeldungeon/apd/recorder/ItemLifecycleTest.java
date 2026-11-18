package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ItemTestSupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemHistory;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunTimeline;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;

import org.junit.After;
import org.junit.Test;

import java.util.List;

//Seeing, acquiring, leaving, equipping and learning about items, through the real game classes.
public class ItemLifecycleTest {

	@After
	public void cleanUp(){
		ApdRecorder.reset();
	}

	private static Hero hero(){
		return Dungeon.hero;
	}

	//---- ITEM_SEEN: only what the player can legitimately observe ------------------------------------------------

	@Test
	public void anUnidentifiedRingOnTheFloorIsSeenAsAnUnidentifiedRing() throws Exception {
		RecorderTestSupport.Session s = startRun(21L);
		RecorderTestSupport.enterLevel(4);
		RingOfWealth ring = new RingOfWealth();
		ring.upgrade(3);
		String shown = ring.name();
		assertFalse("test precondition: the ring must be unidentified", ring.isIdentified());
		putInSight(ring);
		turn();

		ReadResult r = finish(s);
		List<ItemEvents.Seen> seen = seen(r);
		ItemEvents.Seen mine = null;
		for (ItemEvents.Seen x : seen) if (x.uid.equals(ring.apdUid)) mine = x;
		assertNotNull("ITEM_SEEN missing", mine);
		assertEquals(shown, mine.state.name);
		assertEquals(ItemEvents.CAT_RING, mine.state.category);
		assertFalse(mine.state.identified);
		assertFalse(mine.state.levelKnown);
		assertEquals("an unknown level must never leak into SEEN", 0, mine.state.visibleLevel);
		assertEquals(4, mine.where.depth);
		assertFalse(mine.state.name.toLowerCase().contains("wealth"));
	}


	@Test
	public void visibleShopStockIsClassifiedWithoutLeakingHiddenTruth() throws Exception {
		RecorderTestSupport.Session s = startRun(198L);
		RecorderTestSupport.enterLevel(6);
		RingOfWealth ring = new RingOfWealth();
		ring.upgrade(4);
		ring.cursed = true;
		String shown = ring.name();
		assertFalse("test precondition: shop ring must be unidentified", ring.isIdentified());
		assertFalse("test precondition: shop ring curse must be unknown", ring.cursedKnown);
		Heap shop = putInSight(ring);
		shop.type = Heap.Type.FOR_SALE;
		turn();

		ReadResult r = finish(s);
		ItemEvents.Seen mine = null;
		int count = 0;
		for (ItemEvents.Seen x : seen(r)){
			if (x.uid.equals(ring.apdUid)){
				mine = x;
				count++;
			}
		}
		assertNotNull("visible shop stock was not recorded as seen", mine);
		assertEquals("shop stock must be recorded exactly once", 1, count);
		assertEquals(ItemEvents.SEEN_SHOP, mine.seenWhere);
		assertEquals(6, mine.where.depth);
		assertEquals(shop.pos, mine.where.pos);
		assertEquals(shown, mine.state.name);
		assertEquals(ItemEvents.CAT_RING, mine.state.category);
		assertFalse(mine.state.identified);
		assertFalse(mine.state.levelKnown);
		assertFalse(mine.state.cursedKnown);
		assertEquals("unknown shop level must remain hidden", 0, mine.state.visibleLevel);
		assertFalse("unknown shop curse must remain hidden", mine.state.visiblyCursed);
		assertFalse("true shop item class leaked", mine.state.name.toLowerCase().contains("wealth"));

		ItemEvents.Registered truth = null;
		for (ItemEvents.Registered x : registered(r)) if (x.uid.equals(ring.apdUid)) truth = x;
		assertNotNull("internal shop truth was not retained", truth);
		assertTrue(truth.trueClass.endsWith("RingOfWealth"));
		assertEquals(4, truth.trueLevel);
		assertTrue(truth.cursed);
		assertEquals(EventTypes.Visibility.PUBLIC, EventTypes.visibility(EventTypes.ITEM_SEEN));
		assertEquals(EventTypes.Visibility.INTERNAL, EventTypes.visibility(EventTypes.ITEM_REGISTERED));
	}

	@Test
	public void itemsThePlayerCannotSeeAreNotSeen() throws Exception {
		RecorderTestSupport.Session s = startRun(22L);

		Item outOfSight = new PotionOfHealing();
		Heap h1 = putInSight(outOfSight);
		Dungeon.level.heroFOV[h1.pos] = false; //not in view right now
		Item notYetSeenHeap = new PotionOfHealing();
		Heap h2 = putInSight(notYetSeenHeap);
		h2.seen = false;
		Item faint = new PotionOfHealing();
		Heap h3 = putInSight(faint);
		h3.hidden = true; //only a 15% outline
		Item inChest = new PotionOfHealing();
		Heap h4 = putInSight(inChest);
		h4.type = Heap.Type.CHEST; //a chest shows no contents
		Item inLocked = new PotionOfHealing();
		Heap h5 = putInSight(inLocked);
		h5.type = Heap.Type.LOCKED_CHEST;
		Item inCrystal = new PotionOfHealing();
		Heap h6 = putInSight(inCrystal);
		h6.type = Heap.Type.CRYSTAL_CHEST;
		Item inTomb = new PotionOfHealing();
		Heap h7 = putInSight(inTomb);
		h7.type = Heap.Type.TOMB;
		turn();
		for (Item i : new Item[]{outOfSight, notYetSeenHeap, faint, inChest, inLocked, inCrystal, inTomb}){
			assertNull(i.getClass().getSimpleName() + " was recorded as seen but could not be seen", i.apdUid);
		}

		//once the player can see one, it is seen
		Dungeon.level.heroFOV[h1.pos] = true;
		turn();
		assertNotNull(outOfSight.apdUid);

		//and opening the chest makes its contents visible
		h4.type = Heap.Type.HEAP;
		turn();
		assertNotNull(inChest.apdUid);
		ReadResult r = finish(s);
		int mine = 0;
		for (ItemEvents.Seen x : seen(r)) if (x.uid.equals(outOfSight.apdUid) || x.uid.equals(inChest.apdUid)) mine++;
		assertEquals(2, mine);
	}

	@Test
	public void onlyTheTopOfAPileIsSeenUntilItIsUncovered() throws Exception {
		RecorderTestSupport.Session s = startRun(23L);
		Item under = new LeatherArmor();
		Item top = new PotionOfHealing();
		int cell = freeCell();
		putInSight(under, cell);
		Heap pile = putInSight(top, cell);
		assertEquals(top, pile.peek());
		turn();
		assertNotNull(top.apdUid);
		assertNull("an item under another is not visible", under.apdUid);

		assertEquals(top, pile.pickUp()); //the player takes the top item
		turn();
		assertNotNull("the item underneath becomes visible once uncovered", under.apdUid);
		ReadResult r = finish(s);
		int mine = 0;
		for (ItemEvents.Seen x : seen(r)) if (x.uid.equals(top.apdUid) || x.uid.equals(under.apdUid)) mine++;
		assertEquals(2, mine);
	}

	@Test
	public void anItemIsSeenOnceNotEveryTurn() throws Exception {
		RecorderTestSupport.Session s = startRun(24L);
		Item potion = new PotionOfHealing();
		putInSight(potion);
		for (int i = 0; i < 10; i++) turn();
		ReadResult r = finish(s);
		int mine = 0;
		for (ItemEvents.Seen x : seen(r)) if (x.uid.equals(potion.apdUid)) mine++;
		assertEquals(1, mine);
	}

	//---- knowledge is never rewritten ------------------------------------------------------------------------------

	@Test
	public void identifyingARingLaterNeverRewritesWhatWasKnownOnTheFloor() throws Exception {
		RecorderTestSupport.Session s = startRun(25L);
		RecorderTestSupport.enterLevel(4);
		RingOfWealth ring = new RingOfWealth();
		ring.upgrade(3);
		String floorName = ring.name();
		Heap heap = putInSight(ring);
		turn();                         //seen on floor 4, unidentified
		heap.pickUp();
		assertTrue(ring.collect());
		turn();                         //picked up, still unidentified
		ring.identify();                //much later it is identified as a +3 Ring of Wealth
		String identifiedName = ring.name();
		assertNotEquals(floorName, identifiedName);
		turn();

		ReadResult r = finish(s);
		RunTimeline t = RunTimeline.of(r.events);
		String uid = ring.apdUid;

		ItemEvents.Seen seen = null;
		long seenIndex = -1;
		for (ApdEvent e : ofType(r, EventTypes.ITEM_SEEN)){
			ItemEvents.Seen x = ItemEvents.decodeSeen(e, r);
			if (x.uid.equals(uid)){ seen = x; seenIndex = e.index; }
		}
		assertNotNull(seen);
		assertEquals(floorName, seen.state.name);
		assertFalse(seen.state.identified);

		//the identification is its own, later event
		ItemEvents.Knowledge k = null;
		long knowledgeIndex = -1;
		for (ApdEvent e : ofType(r, EventTypes.ITEM_KNOWLEDGE)){
			ItemEvents.Knowledge x = ItemEvents.decodeKnowledge(e, r);
			if (x.uid.equals(uid)){ k = x; knowledgeIndex = e.index; }
		}
		assertNotNull("ITEM_KNOWLEDGE missing", k);
		assertTrue(knowledgeIndex > seenIndex);
		assertEquals(identifiedName, k.state.name);
		assertTrue(k.state.identified && k.state.levelKnown);
		assertEquals(3, k.state.visibleLevel);

		//a derived view asked "what did the player know at the moment it was seen" still says unidentified
		ItemEvents.VisibleState then = ItemHistory.visibleStateAt(t.canonical(), r, uid, seenIndex);
		assertEquals(floorName, then.name);
		assertFalse(then.identified);
		assertEquals(0, then.visibleLevel);
		ItemEvents.VisibleState now = ItemHistory.visibleStateAt(t.canonical(), r, uid, Long.MAX_VALUE);
		assertEquals(identifiedName, now.name);
		assertEquals(3, now.visibleLevel);

		//and the class-level discovery is recorded: the player learned what that gem means
		boolean classKnown = false;
		for (ApdEvent e : ofType(r, EventTypes.ITEM_CLASS_KNOWN)){
			ItemEvents.ClassKnown c = ItemEvents.decodeClassKnown(e, r);
			if (c.className.endsWith("RingOfWealth")){
				assertEquals(floorName, c.label);
				classKnown = true;
			}
		}
		assertTrue(classKnown);

		//the hidden truth is recorded separately, as INTERNAL information
		boolean truth = false;
		for (ItemEvents.Registered reg : registered(r)){
			if (reg.uid.equals(uid)){
				assertTrue(reg.trueClass.endsWith("RingOfWealth"));
				assertEquals(3, reg.trueLevel);
				truth = true;
			}
		}
		assertTrue(truth);
		assertEquals(EventTypes.Visibility.INTERNAL, EventTypes.visibility(EventTypes.ITEM_REGISTERED));
	}

	@Test
	public void noPublicEventLeaksWhatTheItemTrulyIs() throws Exception {
		RecorderTestSupport.Session s = startRun(26L);
		RingOfWealth ring = new RingOfWealth();
		ring.upgrade(2);
		ring.cursed = true;
		LeatherArmor armor = new LeatherArmor();
		armor.upgrade(2);
		putInSight(ring);
		putInSight(armor);
		putInSight(new PotionOfHealing());
		turn();
		ring.collect();
		armor.collect();
		turn();
		ReadResult r = finish(s);

		int checked = 0;
		for (ApdEvent e : r.events){
			if (EventTypes.visibility(e.type) != EventTypes.Visibility.PUBLIC) continue;
			ItemEvents.VisibleState v = null;
			switch (e.type){
				case EventTypes.ITEM_SEEN: v = ItemEvents.decodeSeen(e, r).state; break;
				case EventTypes.ITEM_ACQUIRED: v = ItemEvents.decodeAcquired(e, r).state; break;
				case EventTypes.ITEM_LEFT: v = ItemEvents.decodeLeft(e, r).state; break;
				case EventTypes.ITEM_KNOWLEDGE: v = ItemEvents.decodeKnowledge(e, r).state; break;
				case EventTypes.ITEM_EQUIPPED: case EventTypes.ITEM_UNEQUIPPED: v = ItemEvents.decodeEquip(e, r).state; break;
				default:
			}
			if (v == null) continue;
			checked++;
			if (!v.levelKnown) assertEquals("unknown level leaked in " + EventTypes.name(e.type), 0, v.visibleLevel);
			if (!v.cursedKnown) assertFalse("unknown curse leaked in " + EventTypes.name(e.type), v.visiblyCursed);
			if (!v.identified){
				String n = v.name.toLowerCase();
				assertFalse("true class leaked: " + v.name, n.contains("wealth") || n.contains("healing"));
			}
		}
		assertTrue(checked > 5);
		//the true facts are in INTERNAL events only
		boolean sawTruth = false;
		for (ItemEvents.Registered reg : registered(r)){
			if (reg.trueClass.endsWith("RingOfWealth")){
				assertTrue(reg.cursed);
				assertEquals(2, reg.trueLevel);
				sawTruth = true;
			}
		}
		assertTrue(sawTruth);
	}

	@Test
	public void aChangeInTheTrueLevelThePlayerDoesNotKnowIsRecordedAsInternalTruthOnly() throws Exception {
		RecorderTestSupport.Session s = startRun(27L);
		LeatherArmor armor = new LeatherArmor();
		armor.collect();
		turn();
		armor.upgrade(); //an unseen upgrade: the player still does not know the level
		turn();
		ReadResult r = finish(s);
		boolean truth = false;
		for (ApdEvent e : ofType(r, EventTypes.ITEM_TRUTH_CHANGED)){
			ItemEvents.TruthChanged t = ItemEvents.decodeTruthChanged(e, r);
			if (t.uid.equals(armor.apdUid) && t.oldLevel == 0 && t.newLevel == 1) truth = true;
		}
		assertTrue(truth);
		for (ApdEvent e : ofType(r, EventTypes.ITEM_KNOWLEDGE)){
			assertNotEquals("the player's knowledge did not change", armor.apdUid, ItemEvents.decodeKnowledge(e, r).uid);
		}
	}

	//---- equip / unequip -------------------------------------------------------------------------------------------

	@Test
	public void everyEquipIntervalIsKeptAcrossRepeatedCycles() throws Exception {
		RecorderTestSupport.Session s = startRun(28L);
		Hero h = hero();
		Item startWeapon = h.belongings.weapon;
		String startUid = startWeapon.apdUid;
		Sword sword = new Sword();
		sword.collect();
		turn();
		String swordUid = sword.apdUid;

		//sword in, shortsword out; sword out, shortsword in; sword in again
		wield(h, sword, startWeapon);
		turn();
		wield(h, startWeapon, sword);
		turn();
		wield(h, sword, startWeapon);
		turn();

		ReadResult r = finish(s);
		RunTimeline t = RunTimeline.of(r.events);
		List<ItemHistory.Interval> swordIv = ItemHistory.equipIntervals(t.canonical(), r, swordUid);
		assertEquals("the sword was wielded twice", 2, swordIv.size());
		assertFalse(swordIv.get(0).isOpen());
		assertTrue(swordIv.get(1).isOpen());
		assertTrue(swordIv.get(0).endClock <= swordIv.get(1).startClock);
		assertEquals(ItemEvents.SLOT_WEAPON, swordIv.get(0).slot);

		List<ItemHistory.Interval> startIv = ItemHistory.equipIntervals(t.canonical(), r, startUid);
		assertEquals("the starting weapon was wielded from the start, and again in between", 2, startIv.size());
		assertFalse(startIv.get(0).isOpen());
		assertFalse(startIv.get(1).isOpen());
		assertTrue(startIv.get(0).endClock < startIv.get(1).startClock);

		//each equip and unequip is its own event, in the order it happened
		int equips = 0, unequips = 0;
		for (ApdEvent e : r.events){
			if (e.type == EventTypes.ITEM_EQUIPPED) equips++;
			if (e.type == EventTypes.ITEM_UNEQUIPPED) unequips++;
		}
		assertEquals(5, equips);   //start kit (weapon, armor) + three swaps' new weapons
		assertEquals(3, unequips);
	}

	//what the game does when the player picks "equip": the real doEquip, announced the way Item.execute announces it
	private static void wield( Hero h, Item now, Item before ){
		ApdItems.noteAction(now, "EQUIP");
		assertTrue(((com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem) now).doEquip(h));
		assertEquals(now, h.belongings.weapon);
	}

	@Test
	public void equippingCanRevealACurseAndTheEventShowsWhatWasKnownAfterwards() throws Exception {
		RecorderTestSupport.Session s = startRun(29L);
		Hero h = hero();
		Sword sword = new Sword();
		sword.cursed = true;
		sword.cursedKnown = false;
		sword.collect();
		turn();
		Item old = h.belongings.weapon;
		//the slot change the game makes (its own doEquip needs the hero's sprite, which does not exist headless)
		sword.detachAll(h.belongings.backpack);
		h.belongings.weapon = sword;
		old.collect();
		sword.cursedKnown = true; //wielding a cursed weapon shows the curse
		turn();
		ReadResult r = finish(s);
		for (ApdEvent e : ofType(r, EventTypes.ITEM_EQUIPPED)){
			ItemEvents.Equip q = ItemEvents.decodeEquip(e, r);
			if (q.uid.equals(sword.apdUid)){
				assertTrue(q.state.cursedKnown && q.state.visiblyCursed);
				return;
			}
		}
		throw new AssertionError("no ITEM_EQUIPPED for the sword");
	}

	//---- leaving the hero's hands ----------------------------------------------------------------------------------

	@Test
	public void aThrownPieceIsSplitOffLeavesAsThrownAndIsSeenWhereItLands() throws Exception {
		RecorderTestSupport.Session s = startRun(30L);
		ThrowingStone stack = new ThrowingStone();
		stack.quantity(4);
		stack.collect();
		turn();
		String parent = stack.apdUid;

		ApdItems.noteAction(stack, ApdItems.ACTION_THROW);
		Item piece = stack.detach(hero().belongings.backpack);
		assertNotNull(piece);
		String pieceUid = piece.apdUid;
		assertNotNull(pieceUid);
		assertNotEquals(parent, pieceUid);
		putInSight(piece);
		turn();

		ReadResult r = finish(s);
		boolean thrown = false;
		for (ItemEvents.Left l : left(r)){
			if (l.uid.equals(pieceUid)){
				assertEquals(ItemEvents.LEFT_THROWN, l.reason);
				thrown = true;
			}
		}
		assertTrue("the thrown piece never left", thrown);
		boolean landed = false;
		for (ItemEvents.Seen x : seen(r)) if (x.uid.equals(pieceUid)) landed = true;
		assertTrue(landed);
	}

	@Test
	public void dropUseAndSellAreToldApartByWhatTheGameWasDoing() throws Exception {
		RecorderTestSupport.Session s = startRun(31L);
		Item dropped = new LeatherArmor();
		Item used = new PotionOfHealing();
		Item lost = new LeatherArmor();
		dropped.collect(); used.collect(); lost.collect();
		turn();
		String d = dropped.apdUid, u = used.apdUid, l = lost.apdUid;

		ApdItems.noteAction(dropped, ApdItems.ACTION_DROP);
		dropped.detachAll(hero().belongings.backpack);
		turn();
		ApdItems.noteAction(used, "DRINK");
		used.detach(hero().belongings.backpack);
		turn();
		lost.detachAll(hero().belongings.backpack); //no one said why
		turn();

		ReadResult r = finish(s);
		for (ItemEvents.Left e : left(r)){
			if (e.uid.equals(d)) assertEquals(ItemEvents.LEFT_DROPPED, e.reason);
			if (e.uid.equals(l)) assertEquals(ItemEvents.LEFT_UNKNOWN, e.reason);
		}
		boolean usedSeen = false;
		for (ItemEvents.Left e : left(r)){
			if (e.state.name.equals(used.name()) || e.action.equals("DRINK")){
				assertEquals(ItemEvents.LEFT_USED, e.reason);
				assertEquals("DRINK", e.action);
				usedSeen = true;
			}
		}
		assertTrue(usedSeen);
	}

	@Test
	public void movingAnItemIntoAnEquipmentSlotIsNotLeaving() throws Exception {
		RecorderTestSupport.Session s = startRun(32L);
		Hero h = hero();
		LeatherArmor armor = new LeatherArmor();
		armor.collect();
		turn();
		//the game takes it out of the bag and wears it, the old armor goes back in (Armor.doEquip needs the hero sprite)
		Armor old = h.belongings.armor;
		ApdItems.noteAction(armor, "EQUIP");
		armor.detachAll(h.belongings.backpack);
		h.belongings.armor = armor;
		old.collect();
		turn();
		ReadResult r = finish(s);
		for (ItemEvents.Left l : left(r)){
			assertNotEquals("wearing is not leaving", armor.apdUid, l.uid);
			assertNotEquals("taking off is not leaving", old.apdUid, l.uid);
		}
	}

	//---- transformations and crafting ----------------------------------------------------------------------------

	@Test
	public void classArmorUpgradeIsATransformationLinkedToTheOldArmor() throws Exception {
		RecorderTestSupport.Session s = startRun(33L);
		Hero h = hero();
		Armor cloth = h.belongings.armor;
		String clothUid = cloth.apdUid;
		assertNotNull(clothUid);
		ClassArmor ca = ClassArmor.upgrade(h, cloth);
		assertNotNull(ca);
		h.belongings.armor = ca;
		turn();
		assertNotNull(ca.apdUid);
		assertNotEquals(clothUid, ca.apdUid);
		ReadResult r = finish(s);
		boolean found = false;
		for (ItemEvents.Lineage l : lineage(r)){
			if (l.kind == ItemEvents.LIN_TRANSFORM && l.to.equals(ca.apdUid) && l.from.contains(clothUid)){
				assertEquals("CLASS_ARMOR", l.cause);
				found = true;
			}
		}
		assertTrue(found);
		RunTimeline t = RunTimeline.of(r.events);
		assertTrue(ItemHistory.ancestors(t.canonical(), r, ca.apdUid).contains(clothUid));
	}

	@Test
	public void craftingLinksEveryIngredientToTheProduct() throws Exception {
		RecorderTestSupport.Session s = startRun(34L);
		Item a = new PotionOfHealing();
		Item b = new LeatherArmor();
		a.collect(); b.collect();
		turn();
		java.util.ArrayList<Item> ingredients = new java.util.ArrayList<>();
		ingredients.add(a);
		ingredients.add(b);
		Item product = new PotionOfHealing();
		product.quantity(1);
		a.detachAll(hero().belongings.backpack);
		b.detachAll(hero().belongings.backpack);
		ApdItems.onCrafted(ingredients, product);
		product.collect();
		turn();
		ReadResult r = finish(s);
		boolean found = false;
		for (ItemEvents.Lineage l : lineage(r)){
			if (l.kind == ItemEvents.LIN_CRAFT && l.to.equals(product.apdUid)){
				assertTrue(l.from.contains(a.apdUid) && l.from.contains(b.apdUid));
				found = true;
			}
		}
		assertTrue(found);
		boolean craftedLeft = false;
		for (ItemEvents.Left l : left(r)) if (l.reason == ItemEvents.LEFT_CRAFTED) craftedLeft = true;
		assertTrue("ingredients leave as crafted", craftedLeft);
		boolean acquired = false;
		for (ItemEvents.Acquired x : acquired(r)) if (x.uid.equals(product.apdUid)) acquired = x.context == ItemEvents.ACQ_CRAFT;
		assertTrue(acquired);
	}

	//---- same identity on two live objects ---------------------------------------------------------------------------

	@Test
	public void twoObjectsCarryingOneIdentityAreSeparatedIntoAnOriginalAndACopy() throws Exception {
		RecorderTestSupport.Session s = startRun(35L);
		LeatherArmor a = new LeatherArmor();
		a.collect();
		turn();
		//a clone made by a bundle round trip (as monster duplication does) carries the same uid
		com.watabou.utils.Bundle b = new com.watabou.utils.Bundle();
		b.put("x", a);
		LeatherArmor clone = (LeatherArmor) b.get("x");
		assertEquals(a.apdUid, clone.apdUid);
		clone.collect();
		turn();
		assertNotEquals("each live object needs its own identity", a.apdUid, clone.apdUid);
		ReadResult r = finish(s);
		boolean copy = false;
		for (ItemEvents.Lineage l : lineage(r)) if (l.kind == ItemEvents.LIN_COPY && l.to.equals(clone.apdUid)) copy = true;
		assertTrue(copy);
	}
}
