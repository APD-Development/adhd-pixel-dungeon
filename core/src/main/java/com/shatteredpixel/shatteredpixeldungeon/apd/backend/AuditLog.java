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

package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Append-only log of decisions. Each entry carries the hash of the previous one, so edits or removals are detectable.
public final class AuditLog {

	public static final class Entry {
		public final long seq, atMillis;
		public final String actor, action, target, detail, previousHashHex, hashHex;

		Entry( long seq, long atMillis, String actor, String action, String target, String detail, String previousHashHex ){
			this.seq = seq;
			this.atMillis = atMillis;
			this.actor = actor;
			this.action = action;
			this.target = target;
			this.detail = detail;
			this.previousHashHex = previousHashHex;
			this.hashHex = compute(seq, atMillis, actor, action, target, detail, previousHashHex);
		}
	}

	private static final String GENESIS = "0000000000000000000000000000000000000000000000000000000000000000";

	private final List<Entry> entries = new ArrayList<>();

	public Entry append( long atMillis, String actor, String action, String target, String detail ){
		Entry e = new Entry(entries.size(), atMillis, actor, action, target, detail, entries.isEmpty() ? GENESIS : entries.get(entries.size() - 1).hashHex);
		entries.add(e);
		return e;
	}

	public List<Entry> entries(){
		return entries;
	}

	public String headHashHex(){
		return entries.isEmpty() ? GENESIS : entries.get(entries.size() - 1).hashHex;
	}

	private static String compute( long seq, long at, String actor, String action, String target, String detail, String prev ){
		return new Canon("apd.audit-entry", 1).num(seq).num(at).str(actor).str(action).str(target).str(detail).str(prev).sha256Hex();
	}

	//true if the entries are an unbroken chain from the start (and, if given, end at the expected head)
	public static boolean verify( List<Entry> chain, String expectedHeadHex ){
		String prev = GENESIS;
		for (int i = 0; i < chain.size(); i++){
			Entry e = chain.get(i);
			if (e.seq != i || !e.previousHashHex.equals(prev) || !e.hashHex.equals(compute(e.seq, e.atMillis, e.actor, e.action, e.target, e.detail, e.previousHashHex))) return false;
			prev = e.hashHex;
		}
		return expectedHeadHex == null || expectedHeadHex.equals(prev);
	}

	static byte[] utf8( String s ){
		return s.getBytes(StandardCharsets.UTF_8);
	}
}
