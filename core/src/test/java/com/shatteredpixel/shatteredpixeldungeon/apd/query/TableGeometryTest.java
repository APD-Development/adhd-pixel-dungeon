package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TableGeometryTest {

	private ResultTable table( int rows ){
		DatasetDef def = QuerySupport.def();
		List<Row> list = new ArrayList<>();
		for (int i = 0; i < rows; i++){
			Row r = row(def, "r" + i, "a-rather-long-name-that-must-be-cut-" + i, (long) i * 1000, (long) (i % 3), null, true, "r");
			list.add(r);
		}
		QueryResult q = QueryEngine.run(dataset(def, list), Query.of("things"), "r");
		return ResultTable.build(q, 0, 50);
	}

	@Test
	public void columnWidthsStayWithinTheLimitsAndTheFirstColumnIsKeptApart(){
		TableGeometry g = TableGeometry.of(table(10));
		for (int i = 0; i < g.widths.length; i++){
			assertTrue(g.widths[i] >= TableGeometry.MIN_COLUMN);
			assertTrue(g.widths[i] <= TableGeometry.MAX_COLUMN);
		}
		assertTrue(g.firstWidth() <= TableGeometry.MAX_FIRST);
		assertEquals(g.totalWidth, g.firstWidth() + g.scrollingWidth(), 0.001f);
	}

	@Test
	public void somethingBesidesTheFrozenColumnFitsOnAPortraitScreen(){
		TableGeometry g = TableGeometry.of(table(10));
		float portrait = 135 - 8;   //the narrowest layout the game supports, minus margins
		float first = Math.min(g.firstWidth(), portrait * 0.5f);
		assertTrue("at least one more column must be visible without scrolling", portrait - first >= g.widths[1]);
	}

	@Test
	public void tappingFindsTheRightColumnAndRow(){
		TableGeometry g = TableGeometry.of(table(10));
		assertEquals(0, g.columnAt(1));
		assertEquals(1, g.columnAt(g.lefts[1] + 1));
		assertEquals(g.widths.length - 1, g.columnAt(g.totalWidth - 0.5f));
		assertEquals(-1, g.columnAt(g.totalWidth + 1));
		assertEquals(0, g.rowAt(0));
		assertEquals(9, g.rowAt(TableGeometry.ROW_HEIGHT * 9 + 1));
		assertEquals(-1, g.rowAt(TableGeometry.ROW_HEIGHT * 10 + 1));
		assertEquals(-1, g.rowAt(-3));
	}

	@Test
	public void longTextIsCutToTheColumnNotLeftToSpillOver(){
		assertEquals("abcdefgh", TableGeometry.fit("abcdefgh", 8));
		assertEquals("abcdef..", TableGeometry.fit("abcdefghij", 8));
		assertEquals("ab", TableGeometry.fit("abcdef", 2));
		TableGeometry g = TableGeometry.of(table(3));
		assertTrue(g.maxChars(0) >= 3);
	}

	@Test
	public void anEmptyTableHasNoGeometryToBreak(){
		TableGeometry g = TableGeometry.of(table(0));
		assertEquals(0, g.rows);
		assertEquals(0, g.bodyHeight(), 0.001f);
		assertEquals(-1, g.rowAt(0));
	}

	@Test
	public void theHeaderHasRoomForTheLabelAndTheCoverageLine(){
		ResultTable t = table(5);
		for (ResultTable.Header h : t.headers){
			assertTrue(h.chars >= h.label.length());
			assertTrue(h.coverage.length() > 0);
		}
		assertTrue(TableGeometry.HEADER_HEIGHT >= 22);
	}

	@Test
	public void aLongHeadingBreaksAtASpaceInsteadOfBeingCut() {
		String[] one = TableGeometry.twoLines("Score", 18);
		assertEquals("Score", one[0]);
		assertEquals("", one[1]);
		String[] two = TableGeometry.twoLines("True enchantment when generated", 18);
		assertEquals("True enchantment", two[0]);
		assertEquals("when generated", two[1]);
		String[] word = TableGeometry.twoLines("Supercalifragilisticexpialidocious", 10);
		assertEquals(10, word[0].length());
		assertTrue(word[1].length() <= 10);
		for (String label : new String[]{ "Healed (worked-out part)", "Cursed when generated", "Same kind, state not recorded" }){
			String[] l = TableGeometry.twoLines(label, 14);
			assertTrue(label, l[0].length() <= 14 && l[1].length() <= 14);
		}
	}

	@Test
	public void touchBoundariesUseHalfOpenCells(){
		TableGeometry g = TableGeometry.of(table(3));
		assertEquals(-1, g.columnAt(-0.001f));
		for (int i = 0; i < g.widths.length; i++){
			assertEquals(i, g.columnAt(g.lefts[i]));
			assertEquals(i, g.columnAt(g.lefts[i] + g.widths[i] - 0.001f));
		}
		assertEquals(-1, g.columnAt(g.totalWidth));
		assertEquals(0, g.rowAt(0));
		assertEquals(1, g.rowAt(TableGeometry.ROW_HEIGHT));
		assertEquals(2, g.rowAt(2 * TableGeometry.ROW_HEIGHT));
		assertEquals(-1, g.rowAt(3 * TableGeometry.ROW_HEIGHT));
	}

}
