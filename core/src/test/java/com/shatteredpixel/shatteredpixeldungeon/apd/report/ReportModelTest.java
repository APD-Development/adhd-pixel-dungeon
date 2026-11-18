package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReportModelTest {

	@Test
	public void constructorsKeepDisplayDefaultsAndHeadingBounds(){
		Report.Cell cell = new Report.Cell(null, CellText.EXACT, true);
		assertEquals("", cell.text);
		assertEquals(CellText.EXACT, cell.state);
		assertTrue(cell.small);

		assertEquals(1, new Report.Heading(-10, "low").level);
		assertEquals(2, new Report.Heading(2, "middle").level);
		assertEquals(3, new Report.Heading(99, "high").level);

		assertEquals("", new Report.Fact("x", cell, null).detail);
		assertEquals("", new Report("title", 123L, null).appVersion);
	}

	@Test
	public void provenanceAndFirstTableInspectionTraverseOnlyDataBlocks(){
		Report recorded = new Report("recorded", 0L, "test");
		recorded.add(new Report.Heading(1, "heading"));
		recorded.add(new Report.Note(Report.NoteKind.INFO, "note"));
		recorded.add(new Report.Facts(Report.Provenance.RECORDED));
		Report.Table first = new Report.Table("first", Report.Provenance.RECORDED);
		recorded.add(first);
		recorded.add(new Report.Table("second", Report.Provenance.RECORDED));
		assertFalse(recorded.hasCanonical());
		assertSame(first, recorded.firstTable());

		Report canonicalFacts = new Report("facts", 0L, "test");
		canonicalFacts.add(new Report.Facts(Report.Provenance.MIXED));
		assertTrue(canonicalFacts.hasCanonical());
		assertNull(canonicalFacts.firstTable());

		Report canonicalTable = new Report("table", 0L, "test");
		canonicalTable.add(new Report.Table("canonical", Report.Provenance.CANONICAL));
		assertTrue(canonicalTable.hasCanonical());
	}

	@Test
	public void statesUsedIncludesFactsAndTablesButIgnoresSectionsNullsAndInvalidStates(){
		Report report = new Report("states", 0L, "test");
		Report.Facts facts = new Report.Facts(Report.Provenance.RECORDED);
		facts.add("exact", new Report.Cell("1", CellText.EXACT), null);
		facts.section("section");
		facts.add("negative", new Report.Cell("x", -1), null);
		facts.add("too high", new Report.Cell("x", 6), null);
		report.add(facts);

		Report.Table table = new Report.Table("states", Report.Provenance.RECORDED);
		table.rows.add(new Report.Cell[]{
				null,
				new Report.Cell(">=1", CellText.LOWER),
				new Report.Cell("~1", CellText.INFERRED),
				new Report.Cell("legacy", CellText.LEGACY),
				new Report.Cell("?", CellText.UNKNOWN),
				new Report.Cell("hidden", CellText.HIDDEN)
		});
		report.add(table);

		assertArrayEquals(new boolean[]{ true, true, true, true, true, true }, report.statesUsed());
	}
}
