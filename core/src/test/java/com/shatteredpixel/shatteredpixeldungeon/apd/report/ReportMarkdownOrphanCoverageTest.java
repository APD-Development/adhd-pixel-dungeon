package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportMarkdownOrphanCoverageTest {

    @Test
    public void extraCoverageWithoutHeaderIsIgnoredRatherThanCrashing(){
        Report report = new Report("Run summary", 0, "test");
        Report.Table table = new Report.Table("Totals", Report.Provenance.RECORDED);
        table.headers.add("Floors");
        table.coverage.add("3/5");
        table.coverage.add("orphan 99/99");
        table.rows.add(new Report.Cell[]{new Report.Cell("3", CellText.EXACT)});
        report.add(table);

        String markdown = ReportRenderer.markdown(report);

        assertTrue(markdown, markdown.contains("| Floors |\n| --- |\n| 3 |\n"));
        assertTrue(markdown, markdown.contains("_Known out of listed (per column): Floors 3/5._\n"));
        assertFalse("unlabelled coverage cannot be represented as a column", markdown.contains("orphan 99/99"));
        assertEquals("3/5", table.coverage.get(0));
        assertEquals("orphan 99/99", table.coverage.get(1));
        assertEquals(2, table.coverage.size());
    }

    @Test
    public void orphanCoverageWhenNoHeadersDoesNotCrashOrCreateSummary(){
        Report report = new Report("Empty columns", 0, "test");
        Report.Table table = new Report.Table("", Report.Provenance.RECORDED);
        table.coverage.add("unlabelled");
        report.add(table);

        String markdown = ReportRenderer.markdown(report);

        assertFalse(markdown, markdown.contains("Known out of listed"));
        assertEquals("unlabelled", table.coverage.get(0));
    }

    @Test
    public void matchingCoverageAndAbsentCoverageKeepExistingOutput(){
        Report report = new Report("Normal", 0, "test");
        Report.Table table = new Report.Table("Run counts", Report.Provenance.RECORDED);
        table.headers.add("Floors");
        table.headers.add("Items");
        table.coverage.add("3/5");
        table.coverage.add("7/10");
        table.rows.add(new Report.Cell[]{
                new Report.Cell("3", CellText.EXACT),
                new Report.Cell("7", CellText.EXACT)
        });
        report.add(table);

        String markdown = ReportRenderer.markdown(report);
        assertTrue(markdown, markdown.contains("| Floors | Items |\n| --- | --- |\n| 3 | 7 |\n"));
        assertTrue(markdown, markdown.contains(
                "_Known out of listed (per column): Floors 3/5; Items 7/10._\n"));

        table.coverage.clear();
        assertFalse(ReportRenderer.markdown(report).contains("Known out of listed"));
    }
}
