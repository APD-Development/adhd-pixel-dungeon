/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
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


package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunTimeline;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

//Item identity and lifecycle tracking for one run. Package-private; the game reaches it only through ApdItems/ApdRecorder.
//
//How it works, in one paragraph: an item gets its identity (apd_uid) the first time the recorder meets it, never earlier, so
//items nobody has seen carry nothing. Changes to identity (split, merge, copy, transform, craft) are reported by small hooks
//in Item and become INTERNAL ITEM_LINEAGE events. Everything the player can observe is found by a cheap look at the hero's
//belongings and at the heaps in sight, done at the start of each hero turn and at save, level change and run end ("poll").
//Equipment is found the same way, because SPD has no single place where equipment slots change.
//
//Nothing here changes an item (apart from filling in apd_uid), calls the game's random numbers, or throws into the game.
final class ItemTracker {

	private final RunLogWriter w;
	private final ItemOriginHistory originHistory = new ItemOriginHistory();

	//uids that have a REGISTERED event in this run's canonical history
	private final Set<String> registered = new HashSet<>();
	//uids whose ITEM_SEEN was already written
	private final Set<String> seen = new HashSet<>();
	//uids absorbed into another stack: they no longer exist
	private final Set<String> retired = new HashSet<>();
	//items the hero holds (anywhere in belongings), as last observed
	private final Map<String, Held> held = new LinkedHashMap<>();
	//what each equipment slot held at the last observation (uids)
	private final String[] equipped = new String[ItemEvents.SLOT_COUNT];
	//items that were detached from the hero's belongings since the last poll, with what we know about why
	private final Map<String, Departure> departures = new LinkedHashMap<>();

	//hints from the game about what is happening right now
	private Item hintItem;
	private String hintAction = "";
	private int acquireHint = ItemEvents.ACQ_OTHER;

	private boolean suspended;

	private static final class Held {
		ItemEvents.VisibleState visible;
		ItemFacts.Truth truth;
	}

	private static final class Departure {
		int reason;
		String action = "";
		ItemEvents.VisibleState visible;
		int depth, branch, pos;
		boolean equipMove; //leaving the backpack only to be worn: not a real departure
	}

	ItemTracker( RunLogWriter w ){
		this.w = w;
	}

	//---- restoring the tracker's memory from the log when a run is resumed ----------------------------------------------

	//Replays the item events that still count (not abandoned, and not after 'lastIndex' when a rollback matched an earlier
	//save). 'lastIndex' < 0 means "everything that is not abandoned".
	void replay( ReadResult log, RunTimeline timeline, long lastIndex ){
		for (ApdEvent e : log.events){
			if (lastIndex >= 0 && e.index > lastIndex) break;
			if (timeline.isAbandoned(e)) continue;
			try {
				originHistory.accept(e, log);
				switch (e.type){
					case EventTypes.ITEM_REGISTERED: {
						ItemEvents.Registered r = ItemEvents.decodeRegistered(e, log);
						registered.add(r.uid);
						Held h = held.get(r.uid);
						if (h != null){
							h.truth = truthOf(r);
						}
						break;
					}
					case EventTypes.ITEM_SEEN:
						seen.add(ItemEvents.decodeSeen(e, log).uid);
						break;
					case EventTypes.ITEM_ACQUIRED: {
						ItemEvents.Acquired a = ItemEvents.decodeAcquired(e, log);
						Held h = new Held();
						h.visible = a.state;
						h.truth = truthFromRegistry(a.uid, log, timeline, lastIndex);
						held.put(a.uid, h);
						break;
					}
					case EventTypes.ITEM_LEFT:
						held.remove(ItemEvents.decodeLeft(e, log).uid);
						break;
					case EventTypes.ITEM_KNOWLEDGE: {
						ItemEvents.Knowledge k = ItemEvents.decodeKnowledge(e, log);
						Held h = held.get(k.uid);
						if (h != null) h.visible = k.state;
						break;
					}
					case EventTypes.ITEM_EQUIPPED: {
						ItemEvents.Equip q = ItemEvents.decodeEquip(e, log);
						if (q.slot >= 0 && q.slot < equipped.length) equipped[q.slot] = q.uid;
						break;
					}
					case EventTypes.ITEM_UNEQUIPPED: {
						ItemEvents.Equip q = ItemEvents.decodeEquip(e, log);
						if (q.slot >= 0 && q.slot < equipped.length && q.uid.equals(equipped[q.slot])) equipped[q.slot] = null;
						break;
					}
					case EventTypes.ITEM_LINEAGE: {
						ItemEvents.Lineage l = ItemEvents.decodeLineage(e, log);
						if (l.kind == ItemEvents.LIN_MERGE){
							for (String f : l.from){
								retired.add(f);
								held.remove(f);
							}
						}
						break;
					}
					case EventTypes.ITEM_TRUTH_CHANGED: {
						ItemEvents.TruthChanged t = ItemEvents.decodeTruthChanged(e, log);
						Held h = held.get(t.uid);
						if (h != null && h.truth != null){
							h.truth.level = t.newLevel;
							h.truth.cursed = t.newCursed;
							h.truth.enchant = t.newEnchant;
						}
						break;
					}
					default:
				}
			} catch (Throwable ignored){
				//one unreadable item event must not stop the rest
			}
		}
		originHistory.finishReplay();
	}

	private static ItemFacts.Truth truthOf( ItemEvents.Registered r ){
		ItemFacts.Truth t = new ItemFacts.Truth();
		t.trueClass = r.trueClass;
		t.level = r.trueLevel;
		t.cursed = r.cursed;
		t.enchant = r.enchant;
		t.quantity = r.quantity;
		return t;
	}

	//the truth at registration, then any later change, for one uid (used when a held item is replayed)
	private static ItemFacts.Truth truthFromRegistry( String uid, ReadResult log, RunTimeline timeline, long lastIndex ){
		ItemFacts.Truth t = null;
		for (ApdEvent e : log.events){
			if (lastIndex >= 0 && e.index > lastIndex) break;
			if (timeline.isAbandoned(e)) continue;
			try {
				if (e.type == EventTypes.ITEM_REGISTERED){
					ItemEvents.Registered r = ItemEvents.decodeRegistered(e, log);
					if (r.uid.equals(uid)) t = truthOf(r);
				} else if (e.type == EventTypes.ITEM_TRUTH_CHANGED && t != null){
					ItemEvents.TruthChanged c = ItemEvents.decodeTruthChanged(e, log);
					if (c.uid.equals(uid)){
						t.level = c.newLevel;
						t.cursed = c.newCursed;
						t.enchant = c.newEnchant;
					}
				}
			} catch (Throwable ignored){
			}
		}
		return t;
	}

	//---- identity ------------------------------------------------------------------------------------------------

	private String newUid(){
		for (int tries = 0; tries < 20; tries++){
			UUID u = UUID.randomUUID(); //its own generator, never the game's
			long bits = u.getMostSignificantBits() ^ u.getLeastSignificantBits();
			String hex = Long.toHexString(bits);
			String uid = "0000000000000000".substring(hex.length()) + hex;
			if (!registered.contains(uid) && !retired.contains(uid) && !held.containsKey(uid)) return uid;
		}
		return UUID.randomUUID().toString();
	}

	//Gives the item its identity if it has none, and makes sure the recorder knows it. Null for items with no identity.
	String ensureUid( Item item, int origin ){
		if (!ItemFacts.tracks(item)) return null;
		if (item.apdUid == null){
			String copyOf = item.apdCopyOf;
			item.apdUid = newUid();
			item.apdCopyOf = null;
			if (copyOf != null){
				register(item, copyOf == null ? origin : ItemEvents.ORIGIN_COPY, 0);
				lineage(ItemEvents.LIN_COPY, new String[]{copyOf}, item.apdUid, 0, "duplicate");
			} else {
				register(item, origin, 0);
			}
		} else if (!registered.contains(item.apdUid)){
			//an identity from outside this run's history: a save from before recording, a log that was lost, bones
			register(item, ItemEvents.ORIGIN_ADOPTED, 0);
		}
		com.shatteredpixel.shatteredpixeldungeon.apd.qol.ItemOrigins.record(item, originHistory.origins.get(item.apdUid));
		return item.apdUid;
	}

	private void register( Item item, int origin, int flags ){
		String uid = item.apdUid;
		if (!registered.add(uid)) return;
		ItemFacts.Truth t = ItemFacts.truth(item);
		ItemEvents.Registered r = new ItemEvents.Registered();
		r.uid = uid;
		r.where = here(-1);
		r.origin = origin;
		r.trueClass = t.trueClass;
		r.trueLevel = t.level;
		r.cursed = t.cursed;
		r.enchant = t.enchant;
		r.quantity = t.quantity;
		ItemEvents.emitRegistered(w, clock(), flags, r);
	}

	private void lineage( int kind, String[] from, String to, int amount, String cause ){
		ItemEvents.Lineage l = new ItemEvents.Lineage();
		l.kind = kind;
		for (String f : from) l.from.add(f);
		l.to = to;
		l.amount = amount;
		l.cause = cause == null ? "" : cause;
		l.where = here(-1);
		ItemEvents.emitLineage(w, clock(), l);
	}

	//---- hooks from Item -----------------------------------------------------------------------------------------

	void noteAction( Item item, String action ){
		hintItem = item;
		hintAction = action == null ? "" : action;
	}

	void noteAcquireContext( int context ){
		acquireHint = context;
	}

	void onSplit( Item parent, Item child, int amount ){
		if (suspended || Belongings.bundleRestoring) return;
		String pu = ensureUid(parent, ItemEvents.ORIGIN_OBSERVED);
		if (pu == null || child == null) return;
		child.apdUid = newUid();
		child.apdCopyOf = null;
		register(child, ItemEvents.ORIGIN_SPLIT, 0);
		lineage(ItemEvents.LIN_SPLIT, new String[]{pu}, child.apdUid, amount, "split");
	}

	void onMerge( Item survivor, Item absorbed, int amount ){
		if (suspended || Belongings.bundleRestoring) return;
		String su = ensureUid(survivor, ItemEvents.ORIGIN_OBSERVED);
		String au = ensureUid(absorbed, ItemEvents.ORIGIN_OBSERVED);
		if (su == null || au == null || su.equals(au)) return;
		lineage(ItemEvents.LIN_MERGE, new String[]{au}, su, amount, "merge");
		retired.add(au);
		held.remove(au);
		departures.remove(au);
	}

	void onDetached( Item original, Item piece ){
		if (suspended || Belongings.bundleRestoring || piece == null) return;
		String uid = ensureUid(piece, ItemEvents.ORIGIN_OBSERVED);
		if (uid == null) return;
		Departure d = new Departure();
		d.visible = ItemFacts.visible(piece);
		d.depth = Dungeon.depth;
		d.branch = Dungeon.branch;
		d.pos = heroPos();
		if (hintItem == original && hintAction != null){
			String a = hintAction;
			if (a.equals(ApdItems.ACTION_DROP)){
				d.reason = ItemEvents.LEFT_DROPPED;
			} else if (a.equals(ApdItems.ACTION_THROW)){
				d.reason = ItemEvents.LEFT_THROWN;
			} else if (a.equals("EQUIP") || a.equals("UNEQUIP")){
				d.equipMove = true;
			} else {
				d.reason = ItemEvents.LEFT_USED;
				d.action = a;
			}
		}
		departures.put(uid, d);
	}

	void onTransformed( Item oldItem, Item newItem, String cause ){
		if (suspended || Belongings.bundleRestoring || oldItem == newItem) return;
		String ou = ensureUid(oldItem, ItemEvents.ORIGIN_OBSERVED);
		if (ou == null || newItem == null) return;
		String nu = ensureUid(newItem, ItemEvents.ORIGIN_TRANSFORMED);
		if (nu == null) return;
		lineage(ItemEvents.LIN_TRANSFORM, new String[]{ou}, nu, 0, cause);
		Departure d = departures.get(ou);
		if (d != null && !d.equipMove){
			d.reason = ItemEvents.LEFT_TRANSFORMED;
			d.action = cause == null ? "" : cause;
		} else if (d == null){
			Departure x = new Departure();
			x.reason = ItemEvents.LEFT_TRANSFORMED;
			x.action = cause == null ? "" : cause;
			x.visible = ItemFacts.visible(oldItem);
			x.depth = Dungeon.depth;
			x.branch = Dungeon.branch;
			x.pos = heroPos();
			x.equipMove = false;
			//only a departure if the old item really ends up out of the hero's hands: the next poll decides
			departures.put(ou, x);
		}
		acquireHint = ItemEvents.ACQ_TRANSMUTE;
	}

	void onCrafted( List<Item> ingredients, Item result ){
		if (suspended || Belongings.bundleRestoring || result == null) return;
		List<String> from = new ArrayList<>();
		for (Item i : ingredients){
			String u = ensureUid(i, ItemEvents.ORIGIN_OBSERVED);
			if (u != null && !from.contains(u)) from.add(u);
		}
		String ru = ensureUid(result, ItemEvents.ORIGIN_CRAFTED);
		if (ru == null || from.isEmpty()) return;
		lineage(ItemEvents.LIN_CRAFT, from.toArray(new String[0]), ru, 0, "alchemy");
		for (String u : from){
			if (u.equals(ru)) continue;
			Departure d = departures.get(u);
			if (d != null) {
				d.reason = ItemEvents.LEFT_CRAFTED;
				d.action = "alchemy";
				d.equipMove = false;
			}
		}
		acquireHint = ItemEvents.ACQ_CRAFT;
	}

	void onClassKnown( Class<?> cls, String label, int category ){
		if (suspended || Belongings.bundleRestoring) return;
		ItemEvents.ClassKnown c = new ItemEvents.ClassKnown();
		c.where = here(-1);
		c.category = category;
		c.label = label == null ? "" : label;
		c.className = GameSnapshot.shortName(cls);
		ItemEvents.emitClassKnown(w, clock(), c);
	}

	//---- observation ---------------------------------------------------------------------------------------------

	//Looks at the hero's belongings, equipment and the heaps in sight, and writes what changed. 'startKit' marks the very
	//first look of a new run, 'reconciled' marks a look that catches up with the game after a load.
	void poll( boolean startKit, boolean reconciled ){
		if (suspended || Belongings.bundleRestoring) return;
		Hero hero = Dungeon.hero;
		if (hero == null || hero.belongings == null) return;

		int flags = reconciled ? ItemEvents.FLAG_RECONCILED : 0;
		long clock = clock();

		//1. who holds what now
		Map<String, Item> present = new LinkedHashMap<>();
		Iterator<Item> it = hero.belongings.iterator();
		ArrayList<Item> all = new ArrayList<>();
		while (it.hasNext()) all.add(it.next());
		for (Item item : all){
			if (!ItemFacts.tracks(item)) continue;
			String uid = ensureUid(item, startKit ? ItemEvents.ORIGIN_START_KIT : ItemEvents.ORIGIN_OBSERVED);
			if (uid == null) continue;
			Item other = present.get(uid);
			if (other != null && other != item){
				//two different objects with one identity (a clone made by a bundle round-trip): the second becomes a copy
				item.apdCopyOf = uid;
				item.apdUid = null;
				uid = ensureUid(item, ItemEvents.ORIGIN_COPY);
				if (uid == null) continue;
			}
			present.put(uid, item);
		}

		//2. things that arrived, and things whose visible state or truth changed
		for (Map.Entry<String, Item> en : present.entrySet()){
			String uid = en.getKey();
			Item item = en.getValue();
			ItemEvents.VisibleState vis = ItemFacts.visible(item);
			ItemFacts.Truth truth = ItemFacts.truth(item);
			Held h = held.get(uid);
			if (h == null){
				ItemEvents.Acquired a = new ItemEvents.Acquired();
				a.uid = uid;
				a.where = here(heroPos());
				a.context = reconciled ? ItemEvents.ACQ_RECONCILED : (startKit ? ItemEvents.ACQ_START_KIT : acquireHint);
				a.state = vis;
				ItemEvents.emitAcquired(w, clock, flags, a);
				h = new Held();
				h.visible = vis;
				h.truth = truth;
				held.put(uid, h);
				departures.remove(uid);
				continue;
			}
			if (h.visible == null || !h.visible.knowledgeKey().equals(vis.knowledgeKey())){
				ItemEvents.Knowledge k = new ItemEvents.Knowledge();
				k.uid = uid;
				k.where = here(heroPos());
				k.state = vis;
				ItemEvents.emitKnowledge(w, clock, flags, k);
			}
			if (h.truth != null && !h.truth.sameAs(truth)){
				ItemEvents.TruthChanged t = new ItemEvents.TruthChanged();
				t.uid = uid;
				t.where = here(heroPos());
				t.oldLevel = h.truth.level;
				t.newLevel = truth.level;
				t.oldCursed = h.truth.cursed;
				t.newCursed = truth.cursed;
				t.oldEnchant = h.truth.enchant;
				t.newEnchant = truth.enchant;
				ItemEvents.emitTruthChanged(w, clock, flags, t);
			}
			h.visible = vis;
			h.truth = truth;
			departures.remove(uid); //still here: it only moved (worn, put in a bag)
		}

		//3. equipment, unequip first so a swap reads in the order it happened
		pollEquipment(hero, present, flags, reconciled);

		//4. things that are gone
		ArrayList<String> gone = new ArrayList<>();
		for (String uid : held.keySet()){
			if (!present.containsKey(uid)) gone.add(uid);
		}
		for (String uid : gone){
			Held h = held.remove(uid);
			Departure d = departures.remove(uid);
			emitLeft(uid, d, h.visible, reconciled, flags);
		}
		//pieces that were split off and left without ever being held (a thrown dart, a drunk potion from a stack)
		for (Map.Entry<String, Departure> en : new ArrayList<>(departures.entrySet())){
			String uid = en.getKey();
			Departure d = en.getValue();
			departures.remove(uid);
			if (present.containsKey(uid) || retired.contains(uid) || d.equipMove) continue;
			emitLeft(uid, d, d.visible, false, 0);
		}

		//5. what lies in sight on the floor. Not when the run is just being set up or caught up with a load: Dungeon.level
		//may still be the previous game's level then, and nothing the player has not looked at yet can be "seen".
		if (!startKit && !reconciled) scanFloor(flags);

		hintItem = null;
		hintAction = "";
		acquireHint = ItemEvents.ACQ_OTHER;
	}

	private void emitLeft( String uid, Departure d, ItemEvents.VisibleState vis, boolean reconciled, int flags ){
		ItemEvents.Left l = new ItemEvents.Left();
		l.uid = uid;
		l.where = d != null ? whereOf(d) : here(heroPos());
		l.reason = d != null && !d.equipMove ? d.reason : (reconciled ? ItemEvents.LEFT_RECONCILED : ItemEvents.LEFT_UNKNOWN);
		l.action = d != null ? d.action : "";
		l.state = d != null && d.visible != null ? d.visible : (vis != null ? vis : new ItemEvents.VisibleState());
		ItemEvents.emitLeft(w, clock(), flags, l);
	}

	private static ItemEvents.Where whereOf( Departure d ){
		ItemEvents.Where w = new ItemEvents.Where();
		w.depth = d.depth;
		w.branch = d.branch;
		w.pos = d.pos;
		return w;
	}

	private void pollEquipment( Hero hero, Map<String, Item> present, int flags, boolean reconciled ){
		Belongings b = hero.belongings;
		Item[] now = { b.weapon(), b.armor(), b.artifact(), b.misc(), b.ring(), b.secondWep() };
		String[] nowUid = new String[now.length];
		for (int s = 0; s < now.length; s++){
			nowUid[s] = now[s] == null ? null : ensureUid(now[s], ItemEvents.ORIGIN_OBSERVED);
		}
		long clock = clock();
		//unequip first
		for (int s = 0; s < now.length; s++){
			String old = equipped[s];
			if (old != null && !old.equals(nowUid[s])){
				ItemEvents.Equip q = new ItemEvents.Equip();
				q.uid = old;
				q.where = here(heroPos());
				q.slot = s;
				q.context = reconciled ? ItemEvents.CTX_RECONCILED : ItemEvents.CTX_ACTION;
				q.action = actionFor(present.get(old));
				Held h = held.get(old);
				Item still = present.get(old);
				q.state = still != null ? ItemFacts.visible(still) : (h != null && h.visible != null ? h.visible : new ItemEvents.VisibleState());
				ItemEvents.emitEquip(w, clock, flags, false, q);
				equipped[s] = null;
			}
		}
		for (int s = 0; s < now.length; s++){
			if (nowUid[s] != null && !nowUid[s].equals(equipped[s])){
				ItemEvents.Equip q = new ItemEvents.Equip();
				q.uid = nowUid[s];
				q.where = here(heroPos());
				q.slot = s;
				q.context = reconciled ? ItemEvents.CTX_RECONCILED : ItemEvents.CTX_ACTION;
				q.action = actionFor(now[s]);
				q.state = ItemFacts.visible(now[s]);
				ItemEvents.emitEquip(w, clock, flags, true, q);
				equipped[s] = nowUid[s];
			}
		}
	}

	private String actionFor( Item item ){
		if (item != null && item == hintItem && hintAction != null) return hintAction;
		return "";
	}

	private void scanFloor( int flags ){
		Level level = Dungeon.level;
		if (level == null || level.heaps == null || level.heroFOV == null) return;
		long clock = clock();
		for (Heap heap : level.heaps.valueList()){
			//only what the player can actually make out: the heap has been seen, is not just a faint outline, shows its
			//top item (chests, tombs and remains show no contents), and its cell is in sight right now
			if (!heap.seen || heap.hidden) continue;
			if (heap.type != Heap.Type.HEAP && heap.type != Heap.Type.FOR_SALE) continue;
			if (heap.pos < 0 || heap.pos >= level.heroFOV.length || !level.heroFOV[heap.pos]) continue;
			Item top = heap.peek();
			if (top == null || !ItemFacts.tracks(top)) continue;
			String uid = ensureUid(top, ItemEvents.ORIGIN_OBSERVED);
			if (uid == null || !seen.add(uid)) continue;
			ItemEvents.Seen s = new ItemEvents.Seen();
			s.uid = uid;
			s.where = here(heap.pos);
			s.seenWhere = heap.type == Heap.Type.FOR_SALE ? ItemEvents.SEEN_SHOP : ItemEvents.SEEN_FLOOR;
			s.state = ItemFacts.visible(top);
			ItemEvents.emitSeen(w, clock, flags, s);
		}
	}

	//---- helpers -------------------------------------------------------------------------------------------------

	private static long clock(){
		return GameSnapshot.turn();
	}

	private static int heroPos(){
		Hero h = Dungeon.hero;
		return h == null ? -1 : h.pos;
	}

	private static ItemEvents.Where here( int pos ){
		ItemEvents.Where w = new ItemEvents.Where();
		w.depth = Dungeon.depth;
		w.branch = Dungeon.branch;
		w.pos = pos;
		return w;
	}

	//what the game said it was doing with an item this turn (read by the health tracker to explain a change)
	String hintedAction(){
		return hintAction == null ? "" : hintAction;
	}

	String hintedItemName(){
		try {
			return hintItem == null ? "" : String.valueOf(hintItem.name());
		} catch (Throwable t){
			return "";
		}
	}

	String hintedItemUid(){
		return hintItem == null || hintItem.apdUid == null ? "" : hintItem.apdUid;
	}

	//test and diagnostic access
	int heldCount(){
		return held.size();
	}

	boolean isRegistered( String uid ){
		return registered.contains(uid);
	}
}
