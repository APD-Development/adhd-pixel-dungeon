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


package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.util.ArrayList;
import java.util.List;

//Encoders and decoders for the item identity and lineage events of telemetry 1.2. Pure data, no game classes.
//
//Two kinds of event, and the difference matters:
// - PUBLIC events (SEEN, ACQUIRED, LEFT, EQUIPPED, UNEQUIPPED, KNOWLEDGE, CLASS_KNOWN) carry a VisibleState: only what the
//   player could see at that moment. The true class of an unidentified ring is never in them.
// - INTERNAL events (REGISTERED, LINEAGE, TRUTH_CHANGED) carry hidden truth and identity links, for provenance only.
//
//Field order inside a payload is part of the format. A telemetry 1.x version may only ADD fields at the END of a payload.
public final class ItemEvents {

	private ItemEvents(){}

	//flag bit on events written while reconciling the log with the game (a load, or a recorder that was behind) rather
	//than at the moment the thing happened. The time of the event is then only an upper bound.
	public static final int FLAG_RECONCILED = 4;

	//what kind of thing an item is, as visible from its look. Never the exact class.
	public static final int CAT_OTHER = 0, CAT_WEAPON = 1, CAT_MISSILE = 2, CAT_ARMOR = 3, CAT_RING = 4, CAT_ARTIFACT = 5,
			CAT_WAND = 6, CAT_POTION = 7, CAT_SCROLL = 8, CAT_SEED = 9, CAT_STONE = 10, CAT_FOOD = 11, CAT_BAG = 12,
			CAT_TRINKET = 13, CAT_SPELL = 14, CAT_BOMB = 15, CAT_TOOL = 16;

	//ITEM_ACQUIRED.context
	public static final int ACQ_OTHER = 0, ACQ_PICKUP = 1, ACQ_START_KIT = 2, ACQ_RECONCILED = 3, ACQ_SHOP = 4,
			ACQ_CRAFT = 5, ACQ_TRANSMUTE = 6;

	//ITEM_LEFT.reason
	public static final int LEFT_UNKNOWN = 0, LEFT_DROPPED = 1, LEFT_THROWN = 2, LEFT_USED = 3, LEFT_SOLD = 4,
			LEFT_RECONCILED = 5, LEFT_TRANSFORMED = 6, LEFT_CRAFTED = 7;

	//ITEM_EQUIPPED / ITEM_UNEQUIPPED slot
	public static final int SLOT_WEAPON = 0, SLOT_ARMOR = 1, SLOT_ARTIFACT = 2, SLOT_MISC = 3, SLOT_RING = 4, SLOT_SECOND_WEAPON = 5;
	public static final int SLOT_COUNT = 6;

	//ITEM_EQUIPPED / ITEM_UNEQUIPPED context
	public static final int CTX_ACTION = 0, CTX_RECONCILED = 1;

	//ITEM_REGISTERED.origin: how the recorder first met the item
	public static final int ORIGIN_OBSERVED = 0, ORIGIN_START_KIT = 1, ORIGIN_ADOPTED = 2, ORIGIN_SPLIT = 3, ORIGIN_COPY = 4,
			ORIGIN_TRANSFORMED = 5, ORIGIN_CRAFTED = 6, ORIGIN_GENERATED = 7;

	//ITEM_LINEAGE.kind
	public static final int LIN_SPLIT = 0;      //from: parent stack, to: the new piece
	public static final int LIN_MERGE = 1;      //from: the piece that was absorbed (it ceases to exist), to: the survivor
	public static final int LIN_COPY = 2;       //from: source, to: the copy (a real second object, same kind of item)
	public static final int LIN_TRANSFORM = 3;  //from: old item, to: the item that replaced it
	public static final int LIN_CRAFT = 4;      //from: every ingredient, to: the product

	//ITEM_SEEN.where
	public static final int SEEN_FLOOR = 0, SEEN_SHOP = 1;

	//---- the part of every PUBLIC item event that says what the player knew ---------------------------------------

	public static final class VisibleState {
		public int category;
		public String name = "";          //the name the game shows: "ruby ring" until identified, then the real name
		public int quantity = 1;
		public boolean levelKnown;
		public boolean cursedKnown;
		public int visibleLevel;          //only meaningful when levelKnown, otherwise always 0
		public boolean visiblyCursed;     //only true when the curse is known
		public boolean identified;

		//a key that changes whenever what the player knows about the item changes (quantity excluded)
		public String knowledgeKey(){
			return category + "|" + name + "|" + levelKnown + "|" + cursedKnown + "|" + visibleLevel + "|" + visiblyCursed + "|" + identified;
		}

		@Override
		public boolean equals( Object o ){
			if (!(o instanceof VisibleState)) return false;
			VisibleState v = (VisibleState) o;
			return knowledgeKey().equals(v.knowledgeKey()) && quantity == v.quantity;
		}

		@Override
		public int hashCode(){
			return knowledgeKey().hashCode() * 31 + quantity;
		}

		@Override
		public String toString(){
			return "[" + name + " x" + quantity + (levelKnown ? " +" + visibleLevel : "") + (visiblyCursed ? " cursed" : "") + (identified ? " identified" : "") + "]";
		}
	}

	private static void putVisible( EventBuilder b, VisibleState v ){
		b.unsigned(v.category).string(v.name).unsigned(Math.max(0, v.quantity)).bool(v.levelKnown).bool(v.cursedKnown)
				.signed(v.levelKnown ? v.visibleLevel : 0).bool(v.cursedKnown && v.visiblyCursed).bool(v.identified);
	}

	private static VisibleState readVisible( PayloadReader p ){
		VisibleState v = new VisibleState();
		v.category = p.unsignedInt();
		v.name = str(p.string());
		v.quantity = p.unsignedInt();
		v.levelKnown = p.bool();
		v.cursedKnown = p.bool();
		v.visibleLevel = (int) p.signed();
		v.visiblyCursed = p.bool();
		v.identified = p.bool();
		return v;
	}

	//where and when, the start of every item event payload after the uid
	public static final class Where {
		public int depth;
		public int branch;
		public int pos = -1;   //map cell, -1 if not applicable
	}

	private static void putWhere( EventBuilder b, Where w ){
		b.unsigned(Math.max(0, w.depth)).unsigned(Math.max(0, w.branch)).signed(w.pos);
	}

	private static Where readWhere( PayloadReader p ){
		Where w = new Where();
		w.depth = p.unsignedInt();
		w.branch = p.unsignedInt();
		w.pos = (int) p.signed();
		return w;
	}

	//---- ITEM_SEEN (PUBLIC) ----------------------------------------------------------------------------------------

	//The item became visible to the player where it lay (not picked up yet).
	public static final class Seen {
		public String uid = "";
		public Where where = new Where();
		public int seenWhere;
		public VisibleState state = new VisibleState();
	}

	public static boolean emitSeen( RunLogWriter w, long clock, int flags, Seen s ){
		EventBuilder b = w.event(EventTypes.ITEM_SEEN).clock(clock).flags(flags).string(s.uid);
		putWhere(b, s.where);
		b.unsigned(s.seenWhere);
		putVisible(b, s.state);
		return b.emit();
	}

	public static Seen decodeSeen( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Seen s = new Seen();
		s.uid = str(p.string());
		s.where = readWhere(p);
		s.seenWhere = p.unsignedInt();
		s.state = readVisible(p);
		return s;
	}

	//---- ITEM_ACQUIRED (PUBLIC) ------------------------------------------------------------------------------------

	public static final class Acquired {
		public String uid = "";
		public Where where = new Where();
		public int context;
		public VisibleState state = new VisibleState();
	}

	public static boolean emitAcquired( RunLogWriter w, long clock, int flags, Acquired a ){
		EventBuilder b = w.event(EventTypes.ITEM_ACQUIRED).clock(clock).flags(flags).string(a.uid);
		putWhere(b, a.where);
		b.unsigned(a.context);
		putVisible(b, a.state);
		return b.emit();
	}

	public static Acquired decodeAcquired( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Acquired a = new Acquired();
		a.uid = str(p.string());
		a.where = readWhere(p);
		a.context = p.unsignedInt();
		a.state = readVisible(p);
		return a;
	}

	//---- ITEM_LEFT (PUBLIC) ----------------------------------------------------------------------------------------

	public static final class Left {
		public String uid = "";
		public Where where = new Where();
		public int reason;
		public String action = "";   //the action the player took when known, e.g. "DRINK", "READ", "THROW"
		public VisibleState state = new VisibleState();  //what it was, quantity = how many left
	}

	public static boolean emitLeft( RunLogWriter w, long clock, int flags, Left l ){
		EventBuilder b = w.event(EventTypes.ITEM_LEFT).clock(clock).flags(flags).string(l.uid);
		putWhere(b, l.where);
		b.unsigned(l.reason).string(l.action);
		putVisible(b, l.state);
		return b.emit();
	}

	public static Left decodeLeft( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Left l = new Left();
		l.uid = str(p.string());
		l.where = readWhere(p);
		l.reason = p.unsignedInt();
		l.action = str(p.string());
		l.state = readVisible(p);
		return l;
	}

	//---- ITEM_EQUIPPED / ITEM_UNEQUIPPED (PUBLIC) ---------------------------------------------------------------------

	public static final class Equip {
		public String uid = "";
		public Where where = new Where();
		public int slot;
		public int context;
		public String action = "";
		public VisibleState state = new VisibleState();  //right after the change: equipping can reveal a curse
	}

	public static boolean emitEquip( RunLogWriter w, long clock, int flags, boolean equipped, Equip q ){
		EventBuilder b = w.event(equipped ? EventTypes.ITEM_EQUIPPED : EventTypes.ITEM_UNEQUIPPED).clock(clock).flags(flags).string(q.uid);
		putWhere(b, q.where);
		b.unsigned(q.slot).unsigned(q.context).string(q.action);
		putVisible(b, q.state);
		return b.emit();
	}

	public static Equip decodeEquip( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Equip q = new Equip();
		q.uid = str(p.string());
		q.where = readWhere(p);
		q.slot = p.unsignedInt();
		q.context = p.unsignedInt();
		q.action = str(p.string());
		q.state = readVisible(p);
		return q;
	}

	//---- ITEM_KNOWLEDGE (PUBLIC) -----------------------------------------------------------------------------------

	//What the player knows about a held item changed (level revealed, curse revealed, identified). The old state stays in
	//the earlier events untouched: history is never rewritten.
	public static final class Knowledge {
		public String uid = "";
		public Where where = new Where();
		public VisibleState state = new VisibleState();   //the new state
	}

	public static boolean emitKnowledge( RunLogWriter w, long clock, int flags, Knowledge k ){
		EventBuilder b = w.event(EventTypes.ITEM_KNOWLEDGE).clock(clock).flags(flags).string(k.uid);
		putWhere(b, k.where);
		putVisible(b, k.state);
		return b.emit();
	}

	public static Knowledge decodeKnowledge( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Knowledge k = new Knowledge();
		k.uid = str(p.string());
		k.where = readWhere(p);
		k.state = readVisible(p);
		return k;
	}

	//---- ITEM_CLASS_KNOWN (PUBLIC) ---------------------------------------------------------------------------------

	//The player learned what a whole kind of unidentified item is (this potion colour, scroll rune or ring gem). From now
	//on every item of that kind shows its real name. 'label' is what they were called before.
	public static final class ClassKnown {
		public Where where = new Where();
		public int category;
		public String label = "";       //e.g. "crimson potion"
		public String className = "";   //now public knowledge, e.g. "PotionOfHealing"
	}

	public static boolean emitClassKnown( RunLogWriter w, long clock, ClassKnown c ){
		EventBuilder b = w.event(EventTypes.ITEM_CLASS_KNOWN).clock(clock);
		putWhere(b, c.where);
		b.unsigned(c.category).string(c.label).string(c.className);
		return b.emit();
	}

	public static ClassKnown decodeClassKnown( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		ClassKnown c = new ClassKnown();
		c.where = readWhere(p);
		c.category = p.unsignedInt();
		c.label = str(p.string());
		c.className = str(p.string());
		return c;
	}

	//---- ITEM_REGISTERED (INTERNAL) --------------------------------------------------------------------------------

	//An item got its identity (apd_uid). Records what it truly is, which the player may not know.
	public static final class Registered {
		public String uid = "";
		public Where where = new Where();
		public int origin;
		public String trueClass = "";
		public int trueLevel;
		public boolean cursed;
		public String enchant = "";
		public int quantity = 1;
	}

	public static boolean emitRegistered( RunLogWriter w, long clock, int flags, Registered r ){
		EventBuilder b = w.event(EventTypes.ITEM_REGISTERED).clock(clock).flags(flags).string(r.uid);
		putWhere(b, r.where);
		b.unsigned(r.origin).string(r.trueClass).signed(r.trueLevel).bool(r.cursed).string(r.enchant).unsigned(Math.max(0, r.quantity));
		return b.emit();
	}

	public static Registered decodeRegistered( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Registered r = new Registered();
		r.uid = str(p.string());
		r.where = readWhere(p);
		r.origin = p.unsignedInt();
		r.trueClass = str(p.string());
		r.trueLevel = (int) p.signed();
		r.cursed = p.bool();
		r.enchant = str(p.string());
		r.quantity = p.unsignedInt();
		return r;
	}

	//---- ITEM_LINEAGE (INTERNAL) -----------------------------------------------------------------------------------

	public static final class Lineage {
		public int kind;
		public List<String> from = new ArrayList<>();
		public String to = "";
		public int amount;
		public String cause = "";
		public Where where = new Where();
	}

	public static boolean emitLineage( RunLogWriter w, long clock, Lineage l ){
		EventBuilder b = w.event(EventTypes.ITEM_LINEAGE).clock(clock).unsigned(l.kind).string(l.to).unsigned(Math.max(0, l.amount)).string(l.cause);
		putWhere(b, l.where);
		b.unsigned(l.from.size());
		for (String f : l.from) b.string(f);
		return b.emit();
	}

	public static Lineage decodeLineage( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Lineage l = new Lineage();
		l.kind = p.unsignedInt();
		l.to = str(p.string());
		l.amount = p.unsignedInt();
		l.cause = str(p.string());
		l.where = readWhere(p);
		int n = p.unsignedInt();
		for (int i = 0; i < n; i++) l.from.add(str(p.string()));
		return l;
	}

	//---- ITEM_TRUTH_CHANGED (INTERNAL) -----------------------------------------------------------------------------

	public static final class TruthChanged {
		public String uid = "";
		public Where where = new Where();
		public int oldLevel, newLevel;
		public boolean oldCursed, newCursed;
		public String oldEnchant = "", newEnchant = "";
	}

	public static boolean emitTruthChanged( RunLogWriter w, long clock, int flags, TruthChanged t ){
		EventBuilder b = w.event(EventTypes.ITEM_TRUTH_CHANGED).clock(clock).flags(flags).string(t.uid);
		putWhere(b, t.where);
		b.signed(t.oldLevel).signed(t.newLevel).bool(t.oldCursed).bool(t.newCursed).string(t.oldEnchant).string(t.newEnchant);
		return b.emit();
	}

	public static TruthChanged decodeTruthChanged( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		TruthChanged t = new TruthChanged();
		t.uid = str(p.string());
		t.where = readWhere(p);
		t.oldLevel = (int) p.signed();
		t.newLevel = (int) p.signed();
		t.oldCursed = p.bool();
		t.newCursed = p.bool();
		t.oldEnchant = str(p.string());
		t.newEnchant = str(p.string());
		return t;
	}

	private static String str( String s ){
		return s == null ? "" : s;
	}
}
