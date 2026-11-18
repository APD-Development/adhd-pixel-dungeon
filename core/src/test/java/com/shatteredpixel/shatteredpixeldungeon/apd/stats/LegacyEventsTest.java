package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class LegacyEventsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void everyLegacyEventRoundTripsAndUnknownStaysUnknown() throws Exception {
		File f = new File(tmp.getRoot(), "l.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "l", "test", 1);
		LegacyEvents.Record r = new LegacyEvents.Record();
		r.gameId = "abc"; r.source = "rankings.dat"; r.importedAtMillis = 99;
		r.putNum("result.score", 1234);
		r.putNum("legacy.negative", -5);
		r.putText("run.heroClass", "ROGUE");
		r.putUnknown("legacy.itemsCrafted");
		r.putNum("legacy.zero", 0);
		LegacyEvents.emitRecord(w, 0, r);
		LegacyEvents.Totals t = new LegacyEvents.Totals();
		t.source = "rankings.dat"; t.importedAtMillis = 99;
		t.fields.put("legacy.totalNumber", LegacyEvents.Value.num(40));
		t.fields.put("legacy.wonNumber", LegacyEvents.Value.UNKNOWN);
		LegacyEvents.emitTotals(w, 1, t);
		LegacyEvents.DailyScore d = new LegacyEvents.DailyScore();
		d.day = 12345; d.score = 777; d.importedAtMillis = 99;
		LegacyEvents.emitDailyScore(w, 2, d);
		LegacyEvents.RankingLink l = new LegacyEvents.RankingLink();
		l.gameId = "abc"; l.daily = true;
		LegacyEvents.emitRankingLink(w, 3, l);
		w.close(true);

		ReadResult rd = RunLogReader.read(f);
		assertEquals(4, rd.events.size());
		LegacyEvents.Record r2 = LegacyEvents.decodeRecord(rd.events.get(0), rd);
		assertEquals("abc", r2.gameId);
		assertEquals(1234, r2.get("result.score").num);
		assertEquals(-5, r2.get("legacy.negative").num);
		assertEquals("ROGUE", r2.get("run.heroClass").text);
		assertFalse(r2.get("legacy.itemsCrafted").isKnown());
		assertTrue("a recorded zero is known", r2.get("legacy.zero").isKnown());
		assertFalse("a field that was never listed is unknown", r2.get("legacy.neverMentioned").isKnown());
		assertEquals(r.fields, r2.fields);
		LegacyEvents.Totals t2 = LegacyEvents.decodeTotals(rd.events.get(1), rd);
		assertEquals(40, t2.fields.get("legacy.totalNumber").num);
		assertFalse(t2.fields.get("legacy.wonNumber").isKnown());
		assertEquals(777, LegacyEvents.decodeDailyScore(rd.events.get(2), rd).score);
		LegacyEvents.RankingLink l2 = LegacyEvents.decodeRankingLink(rd.events.get(3), rd);
		assertEquals("abc", l2.gameId);
		assertTrue(l2.daily);
	}

	@Test
	public void legacyEventsAreOnlyHistoryAndTheLinkIsInternal(){
		assertEquals(EventTypes.Visibility.PUBLIC, EventTypes.visibility(EventTypes.LEGACY_RECORD));
		assertEquals(EventTypes.Visibility.PUBLIC, EventTypes.visibility(EventTypes.LEGACY_TOTALS));
		assertEquals(EventTypes.Visibility.INTERNAL, EventTypes.visibility(EventTypes.RANKING_LINK));
		assertTrue(EventTypes.isKnown(EventTypes.LEGACY_DAILY_SCORE));
	}
}
