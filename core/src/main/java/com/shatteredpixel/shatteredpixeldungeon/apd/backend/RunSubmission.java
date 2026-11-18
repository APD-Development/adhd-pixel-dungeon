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

// Summary of ONE finished run, nothing more: no raw log, no UIMSD history, no hidden information, no identity (the server takes the player from the session).
// The field set is pinned by RunSubmissionTest. Everything in it is a claim until the server says otherwise. durationMillis 0 means not recorded.
public final class RunSubmission {

	public static final String SCHEMA = "apd.run-submission";
	public static final int MAJOR = 1, MINOR = 0;

	public enum Outcome { WON, DIED, QUIT }

	public final Id runId;
	public final String rulesetId;
	public final int balanceRevision, difficulty, challenges;
	public final String heroClass;
	public final Outcome outcome;
	public final int depth;
	public final long turns, durationMillis, scoreClaim;
	public final String appBuild;
	public final int telemetryMajor, telemetryMinor;
	//SHA-256 of the run's local log: lets the server later ask for that exact log as evidence and know it was not swapped
	public final String evidenceDigestHex;

	public RunSubmission( Id runId, String rulesetId, int balanceRevision, int difficulty, int challenges, String heroClass, Outcome outcome, int depth,
	                      long turns, long durationMillis, long scoreClaim, String appBuild, int telemetryMajor, int telemetryMinor, String evidenceDigestHex ){
		if (runId.kind != Id.Kind.RUN) throw new IllegalArgumentException("not a run id");
		this.runId = runId;
		this.rulesetId = rulesetId;
		this.balanceRevision = balanceRevision;
		this.difficulty = difficulty;
		this.challenges = challenges;
		this.heroClass = heroClass;
		this.outcome = outcome;
		this.depth = depth;
		this.turns = turns;
		this.durationMillis = durationMillis;
		this.scoreClaim = scoreClaim;
		this.appBuild = appBuild;
		this.telemetryMajor = telemetryMajor;
		this.telemetryMinor = telemetryMinor;
		this.evidenceDigestHex = evidenceDigestHex;
	}

	public byte[] canonical(){
		return new Canon(SCHEMA, MAJOR).id(runId).str(rulesetId).num(balanceRevision).num(difficulty).num(challenges).str(heroClass).str(outcome.name())
				.num(depth).num(turns).num(durationMillis).num(scoreClaim).str(appBuild).num(telemetryMajor).num(telemetryMinor).str(evidenceDigestHex).toBytes();
	}
}
