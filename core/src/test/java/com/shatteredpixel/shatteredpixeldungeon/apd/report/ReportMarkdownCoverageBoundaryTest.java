package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportMarkdownCoverageBoundaryTest {

    @Test
    public void coverageSummaryEscapesPipesBackslashesAndAllLineEndings(){
        Report report = new Report("Coverage", 0, "test");
        Report.Table table = new Report.Table("Counts", Report.Provenance.RECORDED);
        table.headers.add("Ignored");
        table.coverage.add("");
        String header = "Name|type\r\nnext\\path";
        String coverage = "12|20\rdone\\total\nmore";
        table.headers.add(header);
        table.coverage.add(coverage);
        table.rows.add(new Report.Cell[]{
                new Report.Cell("a", CellText.EXACT),
                new Report.Cell("b", CellText.EXACT)
        });
        report.add(table);

        String markdown = ReportRenderer.markdown(report);
        assertTrue(markdown, markdown.contains(
                "_Known out of listed (per column): Name\\|type next\\\\path 12\\|20 done\\\\total more._\n"));
        assertFalse("coverage summary must not create physical carriage-return rows", markdown.contains("\r"));
        assertFalse("empty-coverage columns stay absent from the summary", markdown.contains("column): Ignored "));
        assertEquals("do not modify the source header", header, table.headers.get(1));
        assertEquals("do not modify the source coverage", coverage, table.coverage.get(1));
        assertTrue("HTML keeps original coverage text", ReportRenderer.html(report).contains("known " + coverage));
    }

    @Test
    public void ordinaryNumericCoverageAndAbsentCoverageRemainUnchanged(){
        Report report = new Report("Summary", 0, "test");
        Report.Table table = new Report.Table("Runs", Report.Provenance.RECORDED);
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
        assertTrue(markdown, markdown.contains("_Known out of listed (per column): Floors 3/5; Items 7/10._\n"));
        assertTrue("data table stays intact", markdown.contains("| Floors | Items |\n| --- | --- |\n| 3 | 7 |\n"));
        assertEquals("3/5", table.coverage.get(0));
        assertEquals("7/10", table.coverage.get(1));

        table.coverage.clear();
        assertFalse("empty coverage list must omit the note",
                ReportRenderer.markdown(report).contains("_Known out of listed"));
    }
}
