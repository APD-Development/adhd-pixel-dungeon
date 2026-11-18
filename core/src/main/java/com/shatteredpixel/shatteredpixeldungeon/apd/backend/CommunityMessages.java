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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

// Wire forms for what a signed-in player sends to take part: a ballot and a report. Neither carries the sender: the server takes that from the
// session, so a client cannot vote or report as someone else.
public final class CommunityMessages {

	public static final String BALLOT_SCHEMA = "apd.vote-ballot";
	public static final String REPORT_SCHEMA = "apd.report";
	public static final int MAJOR = 1, MINOR = 0;

	public static final class BallotRequest {
		public final Id round;
		public final Set<String> choices;

		public BallotRequest( Id round, Set<String> choices ){
			if (round.kind != Id.Kind.VOTE_ROUND) throw new IllegalArgumentException("not a vote round id");
			this.round = round;
			this.choices = new TreeSet<>(choices);
		}

		//a new ballot for the same round replaces the earlier one, so the key is per round
		public String idempotencyKey(){
			return "vote_" + round.value;
		}
	}

	public static final class ReportRequest {
		public final Id id;
		public final Report.TargetKind targetKind;
		public final String target;
		public final Report.Reason reason;
		public final String note;

		public ReportRequest( Id id, Report.TargetKind targetKind, String target, Report.Reason reason, String note ){
			if (id.kind != Id.Kind.REPORT) throw new IllegalArgumentException("not a report id");
			if (target == null || target.isEmpty() || target.length() > 120 || target.indexOf('\n') >= 0) throw new IllegalArgumentException("bad target");
			this.id = id;
			this.targetKind = targetKind;
			this.target = target;
			this.reason = reason;
			String n = note == null ? "" : note.replace('\r', ' ').replace('\n', ' ').trim();
			this.note = n.length() > Report.MAX_NOTE ? n.substring(0, Report.MAX_NOTE) : n;
		}

		public String idempotencyKey(){
			return "rep_" + id.value;
		}
	}

	private CommunityMessages(){}

	public static Envelope ballotToEnvelope( BallotRequest b ){
		StringBuilder sb = new StringBuilder("round=").append(b.round.value).append('\n');
		for (String c : b.choices){
			if (c.isEmpty() || c.indexOf('\n') >= 0 || c.indexOf(',') >= 0) throw new IllegalArgumentException("bad choice");
		}
		sb.append("choices=").append(String.join(",", b.choices)).append('\n');
		return new Envelope(BALLOT_SCHEMA, MAJOR, MINOR, sb.toString());
	}

	//null if the envelope is not a readable ballot
	public static BallotRequest ballotFromEnvelope( Envelope e ){
		Map<String, String> m = parse(e, BALLOT_SCHEMA);
		if (m == null) return null;
		try {
			String c = m.get("choices");
			Set<String> choices = new TreeSet<>();
			if (c != null && !c.isEmpty()) for (String x : c.split(",")) choices.add(x);
			return new BallotRequest(Id.of(Id.Kind.VOTE_ROUND, m.get("round")), choices);
		} catch (RuntimeException ex){
			return null;
		}
	}

	public static Envelope reportToEnvelope( ReportRequest r ){
		return new Envelope(REPORT_SCHEMA, MAJOR, MINOR, "id=" + r.id.value + "\ntargetKind=" + r.targetKind.name() + "\ntarget=" + r.target + "\nreason=" + r.reason.name()
				+ "\nnote=" + r.note + "\n");
	}

	public static ReportRequest reportFromEnvelope( Envelope e ){
		Map<String, String> m = parse(e, REPORT_SCHEMA);
		if (m == null) return null;
		try {
			return new ReportRequest(Id.of(Id.Kind.REPORT, m.get("id")), Report.TargetKind.valueOf(m.get("targetKind")), m.get("target"), Report.Reason.valueOf(m.get("reason")),
					m.containsKey("note") ? m.get("note") : "");
		} catch (RuntimeException ex){
			return null;
		}
	}

	private static Map<String, String> parse( Envelope e, String schema ){
		if (!schema.equals(e.schema) || e.compatibleWith(MAJOR, MINOR) == Envelope.Compat.UNSUPPORTED_MAJOR) return null;
		Map<String, String> m = new HashMap<>();
		for (String line : e.payload.split("\n")){
			int i = line.indexOf('=');
			if (i > 0) m.put(line.substring(0, i), line.substring(i + 1));
		}
		return m;
	}
}
