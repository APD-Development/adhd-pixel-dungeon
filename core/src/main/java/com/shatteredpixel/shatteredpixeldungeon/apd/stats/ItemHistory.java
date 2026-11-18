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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Item histories DERIVED from the raw log. Nothing here is stored anywhere: every answer is rebuilt from the events each
//time, so a better reader can always replace this one, and the raw events stay the evidence.
//
//Callers pass the events of the canonical timeline (RunTimeline.canonical()), so abandoned branches are left out by
//default. Passing every event reads the abandoned branches as well.
public final class ItemHistory {

	private ItemHistory(){}

	//what the player knew about an item at the moment of the last PUBLIC event about it at or before 'atEventIndex'
	//(use Long.MAX_VALUE for "now"). Null if the player had not met it by then. Later events never change an earlier answer.
	public static ItemEvents.VisibleState visibleStateAt( List<ApdEvent> events, StringLookup strings, String uid, long atEventIndex ){
		ItemEvents.VisibleState last = null;
		for (ApdEvent e : events){
			if (e.index > atEventIndex) break;
			ItemEvents.VisibleState v = publicStateOf(e, strings, uid);
			if (v != null) last = v;
		}
		return last;
	}

	private static ItemEvents.VisibleState publicStateOf( ApdEvent e, StringLookup strings, String uid ){
		try {
			switch (e.type){
				case EventTypes.ITEM_SEEN: {
					ItemEvents.Seen s = ItemEvents.decodeSeen(e, strings);
					return s.uid.equals(uid) ? s.state : null;
				}
				case EventTypes.ITEM_ACQUIRED: {
					ItemEvents.Acquired a = ItemEvents.decodeAcquired(e, strings);
					return a.uid.equals(uid) ? a.state : null;
				}
				case EventTypes.ITEM_LEFT: {
					ItemEvents.Left l = ItemEvents.decodeLeft(e, strings);
					return l.uid.equals(uid) ? l.state : null;
				}
				case EventTypes.ITEM_EQUIPPED:
				case EventTypes.ITEM_UNEQUIPPED: {
					ItemEvents.Equip q = ItemEvents.decodeEquip(e, strings);
					return q.uid.equals(uid) ? q.state : null;
				}
				case EventTypes.ITEM_KNOWLEDGE: {
					ItemEvents.Knowledge k = ItemEvents.decodeKnowledge(e, strings);
					return k.uid.equals(uid) ? k.state : null;
				}
				default:
					return null;
			}
		} catch (RuntimeException ex){
			return null;
		}
	}

	//one stretch of time an item was worn
	public static final class Interval {
		public final int slot;
		public final long startClock;
		public final long endClock;      //-1 while it is still worn at the end of the log
		public final int startDepth;
		public final int endDepth;       //-1 while open
		public final boolean startReconciled;

		Interval( int slot, long startClock, int startDepth, boolean startReconciled, long endClock, int endDepth ){
			this.slot = slot;
			this.startClock = startClock;
			this.startDepth = startDepth;
			this.startReconciled = startReconciled;
			this.endClock = endClock;
			this.endDepth = endDepth;
		}

		public boolean isOpen(){
			return endClock < 0;
		}
	}

	//every time the item was equipped, in order. Wearing, taking off and wearing again gives separate intervals.
	public static List<Interval> equipIntervals( List<ApdEvent> events, StringLookup strings, String uid ){
		List<Interval> out = new ArrayList<>();
		int openSlot = -1;
		long openClock = 0;
		int openDepth = 0;
		boolean openReconciled = false;
		for (ApdEvent e : events){
			if (e.type != EventTypes.ITEM_EQUIPPED && e.type != EventTypes.ITEM_UNEQUIPPED) continue;
			ItemEvents.Equip q;
			try {
				q = ItemEvents.decodeEquip(e, strings);
			} catch (RuntimeException ex){
				continue;
			}
			if (!q.uid.equals(uid)) continue;
			if (e.type == EventTypes.ITEM_EQUIPPED){
				if (openSlot >= 0){
					//it was equipped again without being unequipped in the log (a gap): close the old one where the new starts
					out.add(new Interval(openSlot, openClock, openDepth, openReconciled, e.clock, q.where.depth));
				}
				openSlot = q.slot;
				openClock = e.clock;
				openDepth = q.where.depth;
				openReconciled = (e.flags & ItemEvents.FLAG_RECONCILED) != 0;
			} else if (openSlot >= 0){
				out.add(new Interval(openSlot, openClock, openDepth, openReconciled, e.clock, q.where.depth));
				openSlot = -1;
			}
		}
		if (openSlot >= 0) out.add(new Interval(openSlot, openClock, openDepth, openReconciled, -1, -1));
		return out;
	}

	//All uids this item descends from through splits, copies, transformations and crafting (not merges: a merge retires
	//the absorbed piece, it does not make the survivor its child). Closest parents first.
	public static List<String> ancestors( List<ApdEvent> events, StringLookup strings, String uid ){
		Map<String, List<String>> parents = new LinkedHashMap<>();
		for (ApdEvent e : events){
			if (e.type != EventTypes.ITEM_LINEAGE) continue;
			try {
				ItemEvents.Lineage l = ItemEvents.decodeLineage(e, strings);
				if (l.kind == ItemEvents.LIN_MERGE) continue;
				List<String> p = parents.get(l.to);
				if (p == null){
					p = new ArrayList<>();
					parents.put(l.to, p);
				}
				for (String f : l.from) if (!p.contains(f)) p.add(f);
			} catch (RuntimeException ex){
				//skip an unreadable event
			}
		}
		List<String> out = new ArrayList<>();
		Set<String> done = new HashSet<>();
		done.add(uid); // A cycle cannot make the queried item its own ancestor.
		List<String> queue = new ArrayList<>();
		queue.add(uid);
		for (int i = 0; i < queue.size(); i++){
			List<String> p = parents.get(queue.get(i));
			if (p == null) continue;
			for (String a : p){
				if (done.add(a)){
					out.add(a);
					queue.add(a);
				}
			}
		}
		return out;
	}

	//every uid that appears in any item event, in order of first appearance
	public static List<String> allUids( List<ApdEvent> events, StringLookup strings ){
		Set<String> seen = new java.util.LinkedHashSet<>();
		for (ApdEvent e : events){
			try {
				switch (e.type){
					case EventTypes.ITEM_REGISTERED: seen.add(ItemEvents.decodeRegistered(e, strings).uid); break;
					case EventTypes.ITEM_SEEN: seen.add(ItemEvents.decodeSeen(e, strings).uid); break;
					case EventTypes.ITEM_ACQUIRED: seen.add(ItemEvents.decodeAcquired(e, strings).uid); break;
					case EventTypes.ITEM_LEFT: seen.add(ItemEvents.decodeLeft(e, strings).uid); break;
					case EventTypes.ITEM_EQUIPPED:
					case EventTypes.ITEM_UNEQUIPPED: seen.add(ItemEvents.decodeEquip(e, strings).uid); break;
					case EventTypes.ITEM_KNOWLEDGE: seen.add(ItemEvents.decodeKnowledge(e, strings).uid); break;
					case EventTypes.ITEM_TRUTH_CHANGED: seen.add(ItemEvents.decodeTruthChanged(e, strings).uid); break;
					case EventTypes.ITEM_LINEAGE: {
						ItemEvents.Lineage lineage = ItemEvents.decodeLineage(e, strings);
						// Destination precedes the parent references in the recorded payload.
						seen.add(lineage.to);
						seen.addAll(lineage.from);
						break;
					}
					default:
				}
			} catch (RuntimeException ex){
				//skip
			}
		}
		return new ArrayList<>(seen);
	}
}
