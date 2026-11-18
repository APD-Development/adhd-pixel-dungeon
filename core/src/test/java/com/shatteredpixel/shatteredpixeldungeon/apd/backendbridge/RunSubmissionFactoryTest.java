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

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts;

import org.junit.Test;

public class RunSubmissionFactoryTest {

	private static final String DIGEST = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	private static RunFacts good(){
		RunFacts f = new RunFacts(RunFacts.SOURCE_APD, "run-2026-10-08-a");
		f.text(FactKeys.STATUS, "sealed", Fact.PROV_RECORDED);
		f.text(FactKeys.RUN_ID, "run-2026-10-08-a", Fact.PROV_RECORDED);
		f.text(FactKeys.TELEMETRY, "1.8", Fact.PROV_RECORDED);
		f.num(FactKeys.OUTCOME, LifecycleEvents.OUTCOME_WON, Fact.PROV_RECORDED);
		f.text(FactKeys.HERO_CLASS, "MAGE", Fact.PROV_RECORDED);
		f.text(FactKeys.RULESET, "exotic30", Fact.PROV_RECORDED);
		f.num(FactKeys.DIFFICULTY, 3, Fact.PROV_RECORDED);
		f.num(FactKeys.DIFFICULTY_BALANCE, 0, Fact.PROV_RECORDED);
		f.num(FactKeys.CHALLENGES, 2, Fact.PROV_RECORDED);
		f.num(FactKeys.DEPTH, 26, Fact.PROV_RECORDED);
		f.num(FactKeys.TURNS, 31000, Fact.PROV_RECORDED);
		f.num(FactKeys.SCORE, 123456, Fact.PROV_RECORDED);
		return f;
	}

	@Test
	public void aCompleteFinishedRunBecomesASubmission(){
		RunSubmissionFactory.Outcome o = RunSubmissionFactory.from(good(), DIGEST, "4.0.8-apd");
		assertNull(o.detail, o.refusal);
		assertEquals("MAGE", o.submission.heroClass);
		assertEquals(26, o.submission.depth);
		assertEquals(1, o.submission.telemetryMajor);
		assertEquals(8, o.submission.telemetryMinor);
		assertEquals("play time is not invented", 0, o.submission.durationMillis);
	}

	@Test
	public void everythingThatIsNotCompleteAndRecordedIsRefusedWithAReason(){
		RunFacts imported = new RunFacts(RunFacts.SOURCE_LEGACY, "legacy:1");
		assertEquals(RunSubmissionFactory.Refusal.NOT_AN_APD_RUN, RunSubmissionFactory.from(imported, DIGEST, "b").refusal);

		RunFacts open = good();
		open.text(FactKeys.STATUS, "open", Fact.PROV_RECORDED);
		assertEquals(RunSubmissionFactory.Refusal.LOG_NOT_COMPLETE, RunSubmissionFactory.from(open, DIGEST, "b").refusal);
		RunFacts damaged = good();
		damaged.text(FactKeys.STATUS, "damaged", Fact.PROV_RECORDED);
		assertEquals(RunSubmissionFactory.Refusal.LOG_NOT_COMPLETE, RunSubmissionFactory.from(damaged, DIGEST, "b").refusal);

		RunFacts partial = good();
		partial.num(FactKeys.PARTIAL, 1, Fact.PROV_RECORDED);
		assertEquals(RunSubmissionFactory.Refusal.HISTORY_STARTS_MID_RUN, RunSubmissionFactory.from(partial, DIGEST, "b").refusal);

		RunFacts running = new RunFacts(RunFacts.SOURCE_APD, "r");
		running.text(FactKeys.STATUS, "sealed", Fact.PROV_RECORDED);
		assertEquals("no result: still going (hidden information stays hidden)", RunSubmissionFactory.Refusal.NOT_FINISHED, RunSubmissionFactory.from(running, DIGEST, "b").refusal);

		RunFacts inferred = good();
		inferred.num(FactKeys.DIFFICULTY, 3, Fact.PROV_INFERRED);
		assertEquals("an inferred value is not submitted as if recorded", RunSubmissionFactory.Refusal.VALUE_NOT_RECORDED, RunSubmissionFactory.from(inferred, DIGEST, "b").refusal);
		RunFacts noDifficulty = new RunFacts(RunFacts.SOURCE_APD, "run-x");
		for (String k : good().all().keySet()) if (!k.equals(FactKeys.DIFFICULTY)) noDifficulty.put(k, good().get(k));
		assertEquals("older runs never recorded their difficulty", RunSubmissionFactory.Refusal.VALUE_NOT_RECORDED, RunSubmissionFactory.from(noDifficulty, DIGEST, "b").refusal);

		RunFacts unknownRules = good();
		unknownRules.text(FactKeys.RULESET, "unknown", Fact.PROV_RECORDED);
		assertEquals(RunSubmissionFactory.Refusal.RULESET_UNKNOWN, RunSubmissionFactory.from(unknownRules, DIGEST, "b").refusal);

		assertEquals("a bad digest is caught by the shared policy", RunSubmissionFactory.Refusal.POLICY, RunSubmissionFactory.from(good(), "nothex", "b").refusal);
	}

	@Test
	public void anAbandonedRunIsAQuitNotAWin(){
		RunFacts f = good();
		f.num(FactKeys.OUTCOME, LifecycleEvents.OUTCOME_ABANDONED, Fact.PROV_RECORDED);
		assertEquals(com.shatteredpixel.shatteredpixeldungeon.apd.backend.RunSubmission.Outcome.QUIT, RunSubmissionFactory.from(f, DIGEST, "b").submission.outcome);
	}
}
