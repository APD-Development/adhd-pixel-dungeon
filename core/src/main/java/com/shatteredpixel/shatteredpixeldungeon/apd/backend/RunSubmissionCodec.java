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

// Wire form of a RunSubmission: key=value lines. Unknown keys are ignored (later minors may add fields); a missing key is refused.
public final class RunSubmissionCodec {

	private RunSubmissionCodec(){}

	public static Envelope toEnvelope( RunSubmission s ){
		StringBuilder sb = new StringBuilder();
		put(sb, "runId", s.runId.value);
		put(sb, "rulesetId", s.rulesetId);
		put(sb, "balanceRevision", String.valueOf(s.balanceRevision));
		put(sb, "difficulty", String.valueOf(s.difficulty));
		put(sb, "challenges", String.valueOf(s.challenges));
		put(sb, "heroClass", s.heroClass);
		put(sb, "outcome", s.outcome.name());
		put(sb, "depth", String.valueOf(s.depth));
		put(sb, "turns", String.valueOf(s.turns));
		put(sb, "durationMillis", String.valueOf(s.durationMillis));
		put(sb, "scoreClaim", String.valueOf(s.scoreClaim));
		put(sb, "appBuild", s.appBuild);
		put(sb, "telemetryMajor", String.valueOf(s.telemetryMajor));
		put(sb, "telemetryMinor", String.valueOf(s.telemetryMinor));
		put(sb, "evidenceDigestHex", s.evidenceDigestHex);
		return new Envelope(RunSubmission.SCHEMA, RunSubmission.MAJOR, RunSubmission.MINOR, sb.toString());
	}

	private static void put( StringBuilder sb, String k, String v ){
		if (v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0) throw new IllegalArgumentException("line break in " + k);
		sb.append(k).append('=').append(v).append('\n');
	}

	//null if the envelope is not a readable run submission (wrong schema, unsupported major version, a missing or malformed field)
	public static RunSubmission fromEnvelope( Envelope e ){
		if (!RunSubmission.SCHEMA.equals(e.schema) || e.compatibleWith(RunSubmission.MAJOR, RunSubmission.MINOR) == Envelope.Compat.UNSUPPORTED_MAJOR) return null;
		Map<String, String> m = new HashMap<>();
		for (String line : e.payload.split("\n")){
			int i = line.indexOf('=');
			if (i > 0) m.put(line.substring(0, i), line.substring(i + 1));
		}
		try {
			return new RunSubmission(Id.of(Id.Kind.RUN, req(m, "runId")), req(m, "rulesetId"), Integer.parseInt(req(m, "balanceRevision")), Integer.parseInt(req(m, "difficulty")),
					Integer.parseInt(req(m, "challenges")), req(m, "heroClass"), RunSubmission.Outcome.valueOf(req(m, "outcome")), Integer.parseInt(req(m, "depth")),
					Long.parseLong(req(m, "turns")), Long.parseLong(req(m, "durationMillis")), Long.parseLong(req(m, "scoreClaim")), req(m, "appBuild"),
					Integer.parseInt(req(m, "telemetryMajor")), Integer.parseInt(req(m, "telemetryMinor")), req(m, "evidenceDigestHex"));
		} catch (RuntimeException ex){
			return null;
		}
	}

	private static String req( Map<String, String> m, String k ){
		String v = m.get(k);
		if (v == null) throw new IllegalArgumentException("missing " + k);
		return v;
	}
}
