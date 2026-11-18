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

import java.util.HashSet;
import java.util.Set;

// A player's report for moderators. The note is bounded and a repeat report of the same target by the same reporter is a no-op.
public final class Report {

	public enum TargetKind { CONTENT, SUBMISSION, PROFILE, VOTE_ROUND }

	public enum Reason { CHEATING, OFFENSIVE, SPAM, BROKEN, OTHER }

	public static final int MAX_NOTE = 500;

	public final Id id, reporter;
	public final TargetKind targetKind;
	public final String target;
	public final Reason reason;
	public final String note;
	public final long atMillis;

	public Report( Id id, Id reporter, TargetKind targetKind, String target, Reason reason, String note, long atMillis ){
		if (id.kind != Id.Kind.REPORT || reporter.kind != Id.Kind.PLAYER) throw new IllegalArgumentException("wrong id kinds");
		if (target == null || target.isEmpty() || target.length() > 120) throw new IllegalArgumentException("bad target");
		this.id = id;
		this.reporter = reporter;
		this.targetKind = targetKind;
		this.target = target;
		this.reason = reason;
		this.note = note == null ? "" : (note.length() > MAX_NOTE ? note.substring(0, MAX_NOTE) : note);
		this.atMillis = atMillis;
	}

	//reports kept by reporter+target so a repeat can be recognised
	public static final class Inbox {
		private final Set<String> seen = new HashSet<>();
		private int count;

		//true if this report is new
		public boolean add( Report r ){
			if (!seen.add(r.reporter.value + "|" + r.targetKind + "|" + r.target)) return false;
			count++;
			return true;
		}

		public int count(){
			return count;
		}
	}
}
