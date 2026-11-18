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

package com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge;

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.RunSubmission;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.SubmissionPolicy;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts;

// The only place local run facts become a RunSubmission. Copies a fixed set of facts and refuses (with a reason) anything that isn't a complete, finished, recorded APD run.
// Nothing calls it yet.
public final class RunSubmissionFactory {

	public enum Refusal {
		NOT_AN_APD_RUN, LOG_NOT_COMPLETE, HISTORY_STARTS_MID_RUN, NOT_FINISHED, VALUE_NOT_RECORDED, RULESET_UNKNOWN, POLICY
	}

	public static final class Outcome {
		public final RunSubmission submission;   //null if refused
		public final Refusal refusal;
		public final String detail;

		Outcome( RunSubmission s, Refusal r, String detail ){
			this.submission = s;
			this.refusal = r;
			this.detail = detail;
		}
	}

	private RunSubmissionFactory(){}

	//'evidenceDigestHex' is the SHA-256 of the run's local log (computed by the caller from the log file); 'appBuild' the running build's name
	public static Outcome from( RunFacts f, String evidenceDigestHex, String appBuild ){
		if (f.source != RunFacts.SOURCE_APD) return refuse(Refusal.NOT_AN_APD_RUN, "imported ranking records cannot be submitted");
		if (!"sealed".equals(f.textOr(FactKeys.STATUS, ""))) return refuse(Refusal.LOG_NOT_COMPLETE, "the run log is " + f.textOr(FactKeys.STATUS, "unknown"));
		if (f.isKnown(FactKeys.PARTIAL)) return refuse(Refusal.HISTORY_STARTS_MID_RUN, "earlier history of this run is missing");
		if (!f.isKnown(FactKeys.OUTCOME)) return refuse(Refusal.NOT_FINISHED, "the run has no result yet");
		int outcome = (int) f.numOr(FactKeys.OUTCOME, -1);
		RunSubmission.Outcome o;
		if (outcome == LifecycleEvents.OUTCOME_WON) o = RunSubmission.Outcome.WON;
		else if (outcome == LifecycleEvents.OUTCOME_DIED) o = RunSubmission.Outcome.DIED;
		else if (outcome == LifecycleEvents.OUTCOME_ABANDONED) o = RunSubmission.Outcome.QUIT;
		else return refuse(Refusal.NOT_FINISHED, "the run has no result yet");
		String[] needNum = { FactKeys.DIFFICULTY, FactKeys.DIFFICULTY_BALANCE, FactKeys.CHALLENGES, FactKeys.DEPTH, FactKeys.TURNS, FactKeys.SCORE };
		for (String k : needNum) if (!recorded(f, k, true)) return refuse(Refusal.VALUE_NOT_RECORDED, k + " was not recorded for this run");
		if (!recorded(f, FactKeys.HERO_CLASS, false) || !recorded(f, FactKeys.RULESET, false) || !recorded(f, FactKeys.RUN_ID, false)){
			return refuse(Refusal.VALUE_NOT_RECORDED, "the run's class, rules or id were not recorded");
		}
		String ruleset = f.textOr(FactKeys.RULESET, "");
		if (ruleset.isEmpty() || ruleset.startsWith("unknown")) return refuse(Refusal.RULESET_UNKNOWN, "the rules this run was played under are not recorded");
		int telMajor = 1, telMinor = 0;
		String tel = f.textOr(FactKeys.TELEMETRY, "");
		int dot = tel.indexOf('.');
		try {
			if (dot > 0){
				telMajor = Integer.parseInt(tel.substring(0, dot));
				telMinor = Integer.parseInt(tel.substring(dot + 1));
			}
		} catch (NumberFormatException e){
			return refuse(Refusal.VALUE_NOT_RECORDED, "the log's format version is unreadable");
		}
		RunSubmission s;
		try {
			s = new RunSubmission(Id.of(Id.Kind.RUN, f.textOr(FactKeys.RUN_ID, "")), ruleset, (int) f.numOr(FactKeys.DIFFICULTY_BALANCE, 0), (int) f.numOr(FactKeys.DIFFICULTY, 0),
					(int) f.numOr(FactKeys.CHALLENGES, 0), f.textOr(FactKeys.HERO_CLASS, ""), o, (int) f.numOr(FactKeys.DEPTH, 0), f.numOr(FactKeys.TURNS, 0),
					//play time is not part of the facts yet: 0 means "not recorded" (a submission never invents it)
					0, f.numOr(FactKeys.SCORE, 0), appBuild, telMajor, telMinor, evidenceDigestHex);
		} catch (IllegalArgumentException e){
			return refuse(Refusal.VALUE_NOT_RECORDED, "the run id is not usable");
		}
		SubmissionPolicy.Verdict v = SubmissionPolicy.check(s, true);
		if (v != SubmissionPolicy.Verdict.OK) return refuse(Refusal.POLICY, v.name());
		return new Outcome(s, null, "");
	}

	//known, and counted or read from what the game recorded (not inferred, not from a log with gaps, not copied from a legacy record)
	private static boolean recorded( RunFacts f, String key, boolean numeric ){
		Fact x = f.get(key);
		return x != null && x.isNum() == numeric && x.prov == Fact.PROV_RECORDED;
	}

	private static Outcome refuse( Refusal r, String detail ){
		return new Outcome(null, r, detail);
	}
}
