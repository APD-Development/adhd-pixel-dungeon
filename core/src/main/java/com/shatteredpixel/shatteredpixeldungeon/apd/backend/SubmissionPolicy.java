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

// Checks that need no secrets. Runs on the device (for a clear refusal) and must run again on the server.
public final class SubmissionPolicy {

	public enum Verdict { OK, NOT_FINISHED, BAD_FIELD, UNKNOWN_RULESET, DIFFICULTY_OUT_OF_RANGE, IMPOSSIBLE_VALUES, BAD_EVIDENCE_DIGEST }

	private SubmissionPolicy(){}

	public static Verdict check( RunSubmission s, boolean runIsFinished ){
		if (!runIsFinished) return Verdict.NOT_FINISHED;
		if (s.outcome == null || s.heroClass == null || s.heroClass.isEmpty() || s.appBuild == null || s.rulesetId == null) return Verdict.BAD_FIELD;
		if (s.rulesetId.isEmpty() || s.rulesetId.startsWith("unknown")) return Verdict.UNKNOWN_RULESET;
		if (s.difficulty < 1 || s.difficulty > 5) return Verdict.DIFFICULTY_OUT_OF_RANGE;
		if (s.depth < 0 || s.depth > 100 || s.turns < 0 || s.durationMillis < 0 || s.scoreClaim < 0 || s.challenges < 0 || s.balanceRevision < 0) return Verdict.IMPOSSIBLE_VALUES;
		if (s.outcome == RunSubmission.Outcome.WON && s.depth < 1) return Verdict.IMPOSSIBLE_VALUES;
		if (s.evidenceDigestHex == null || !s.evidenceDigestHex.matches("[0-9a-f]{64}")) return Verdict.BAD_EVIDENCE_DIGEST;
		return Verdict.OK;
	}
}
