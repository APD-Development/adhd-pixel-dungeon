package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RowMergerTest {

	private final DatasetDef def = QuerySupport.def();

	private Row part( String key, Long score, Long depth, Long kind, boolean finished, String telemetry ){
		Row r = new Row("id-" + key, key, "run", def.size());
		r.finished = finished;
		r.telemetry = telemetry;
		r.ruleset = "r";
		if (score != null) r.setNum(SCORE, score, Fact.PROV_RECORDED);
		if (depth != null) r.setNum(DEPTH, depth, Fact.PROV_RECORDED);
		if (kind != null) r.setNum(KIND, kind, Fact.PROV_RECORDED);
		return r;
	}

	@Test
	public void sumsAddUpWhenEverySegmentKnowsAndStayExact(){
		Row m = RowMerger.merge(def, Arrays.asList(part("k", 10L, 3L, 1L, false, "1.3"), part("k", 5L, 7L, 1L, true, "1.4")));
		assertEquals(15, m.num(SCORE));
		assertEquals(Fact.PROV_RECORDED, m.prov(SCORE));
		assertEquals(7, m.num(DEPTH));     //max
		assertEquals(Fact.PROV_RECORDED, m.prov(DEPTH));
		assertEquals("1.3+1.4", m.telemetry);
		assertTrue(m.finished);            //the newest segment ended
	}

	@Test
	public void aSegmentThatCouldNotRecordMakesTheTotalALowerBound(){
		Row m = RowMerger.merge(def, Arrays.asList(part("k", null, null, 1L, false, "1.1"), part("k", 5L, 7L, 1L, true, "1.4")));
		assertEquals(5, m.num(SCORE));
		assertEquals("some of the run was recorded by a version that could not count this", Fact.PROV_INCOMPLETE, m.prov(SCORE));
		assertEquals(Fact.PROV_INCOMPLETE, m.prov(DEPTH));
		//nothing known anywhere stays unknown
		Row none = RowMerger.merge(def, Arrays.asList(part("k", null, null, null, false, "1.1"), part("k", null, null, null, true, "1.1")));
		assertFalse(none.isKnown(SCORE));
	}

	@Test
	public void aPartThatWasAlreadyAnEstimateKeepsTheTotalAnEstimate(){
		Row a = part("k", null, null, 1L, false, "1.4");
		a.setNum(SCORE, 4, Fact.PROV_INFERRED);
		Row m = RowMerger.merge(def, Arrays.asList(a, part("k", 5L, 1L, 1L, true, "1.4")));
		assertEquals(9, m.num(SCORE));
		assertEquals(Fact.PROV_INFERRED, m.prov(SCORE));
		Row lb = part("k", null, null, 1L, false, "1.4");
		lb.setNum(SCORE, 4, Fact.PROV_INCOMPLETE);
		assertEquals(Fact.PROV_INCOMPLETE, RowMerger.merge(def, Arrays.asList(a, lb)).prov(SCORE));
	}

	@Test
	public void sameAndLastTakeTheRightSegment(){
		Row first = part("k", 1L, 1L, 0L, false, "1.3");
		first.setText(NAME, "early", Fact.PROV_RECORDED);
		Row second = part("k", 1L, 1L, null, true, "1.4");
		second.setText(NAME, "late", Fact.PROV_RECORDED);
		Row m = RowMerger.merge(def, Arrays.asList(first, second));
		assertEquals("early", m.text(NAME));          //NAME merges as SAME: first known
		//KIND is SAME as well: the first known value
		assertEquals(0, m.num(KIND));
	}

	@Test
	public void lastNeverBorrowsFromAnEarlierSegment(){
		DatasetDef d = new DatasetDef("x", "x", "x");
		d.add(ColumnDef.num("outcome", "Outcome").merge(ColumnDef.Merge.LAST));
		Row a = new Row("a", "k", "run", 1);
		a.setNum(0, 1, Fact.PROV_RECORDED);
		Row b = new Row("a", "k", "run", 1);        //the last segment does not know the outcome
		Row m = RowMerger.merge(d, Arrays.asList(a, b));
		assertFalse(m.isKnown(0));
	}

	@Test
	public void anyIsTrueIfAnySegmentSaysSoAndFalseOnlyWhenEveryoneKnows(){
		DatasetDef d = new DatasetDef("x", "x", "x");
		d.add(ColumnDef.enumeration("flag", "Flag").merge(ColumnDef.Merge.ANY));
		Row yes = new Row("a", "k", "run", 1);
		yes.setNum(0, 1, Fact.PROV_RECORDED);
		Row no = new Row("a", "k", "run", 1);
		no.setNum(0, 0, Fact.PROV_RECORDED);
		Row unknown = new Row("a", "k", "run", 1);
		assertEquals(1, RowMerger.merge(d, Arrays.asList(unknown, yes)).num(0));
		assertEquals(0, RowMerger.merge(d, Arrays.asList(no, no)).num(0));
		assertTrue(RowMerger.merge(d, Arrays.asList(no, no)).isKnown(0));
		assertFalse("false cannot be claimed while a segment is unknown", RowMerger.merge(d, Arrays.asList(no, unknown)).isKnown(0));
	}

	@Test
	public void mergeAllKeepsUnkeyedRowsAndMergesTheRest(){
		List<Row> rows = new ArrayList<>();
		rows.add(part("a", 1L, 1L, 0L, false, "1.3"));
		Row loose = new Row("loose", null, "run", def.size());
		rows.add(loose);
		rows.add(part("a", 2L, 2L, 0L, true, "1.3"));
		rows.add(part("b", 5L, 1L, 0L, true, "1.3"));
		List<Row> merged = RowMerger.mergeAll(def, rows);
		assertEquals(3, merged.size());
		assertEquals(3, merged.get(0).num(SCORE));
		assertTrue(merged.get(1) == loose);
		assertEquals(5, merged.get(2).num(SCORE));
	}

	@Test
	public void aMergedRowIsPublicIfAnyPartIs(){
		Row a = part("k", 1L, 1L, 0L, false, "1.3");
		a.publicRow = false;
		Row b = part("k", 1L, 1L, 0L, false, "1.4");
		assertTrue(RowMerger.merge(def, Arrays.asList(a, b)).publicRow);
		Row c = part("k", 1L, 1L, 0L, false, "1.4");
		c.publicRow = false;
		assertFalse(RowMerger.merge(def, Arrays.asList(a, c)).publicRow);
	}
}
