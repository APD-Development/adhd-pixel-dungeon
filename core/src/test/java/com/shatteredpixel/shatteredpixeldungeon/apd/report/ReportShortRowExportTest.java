package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportShortRowExportTest {

    @Test
    public void missingTrailingPositionsStayBlankAcrossFourExports(){
        Report report = new Report("Short row", 0L, "test");
        Report.Table table = new Report.Table("Counts", Report.Provenance.RECORDED);
        table.headers.add("A");
        table.headers.add("B");
        table.headers.add("C");
        Report.Cell kept = new Report.Cell(">=7", CellText.LOWER, true);
        Report.Cell[] row = new Report.Cell[]{ kept };
        table.rows.add(row);
        report.add(table);

        assertTrue(ReportRenderer.markdown(report).contains("| A | B | C |\n| --- | --- | --- |\n| >=7 (small sample) |  |  |\n"));
        assertTrue(ReportRenderer.text(report).contains("  A >=7 | B  | C \n"));
        assertTrue(ReportRenderer.html(report).contains("<td class=\"lower small\">&gt;=7</td><td class=\"exact\"></td><td class=\"exact\"></td>"));
        assertTrue(ReportRenderer.csv(report).contains("A,B,C\r\n>=7,,\r\n"));
        assertEquals(1, row.length);
        assertSame(kept, row[0]);
        assertTrue(report.statesUsed()[CellText.LOWER]);
        assertFalse(report.statesUsed()[CellText.UNKNOWN]);
    }

    @Test
    public void zeroLengthRowHasAllHeaderColumnsWithoutInventedData(){
        Report report = new Report("Empty", 0L, "");
        Report.Table table = new Report.Table("Slots", Report.Provenance.MIXED);
        table.headers.add("Basis");
        table.headers.add("Score");
        table.rows.add(new Report.Cell[0]);
        report.add(table);

        assertTrue(ReportRenderer.markdown(report).contains("| Basis | Score |\n| --- | --- |\n|  |  |\n"));
        assertTrue(ReportRenderer.html(report).contains("<tr><td class=\"exact\"></td><td class=\"exact\"></td></tr>"));
        assertTrue(ReportRenderer.text(report).contains("  Basis  | Score \n"));
        String csv = ReportRenderer.csv(report);
        assertTrue(csv.contains("Basis,Score\r\n,\r\n"));
        assertTrue(csv.contains(ReportRenderer.CANONICAL_NOTICE));
        assertFalse(report.statesUsed()[CellText.UNKNOWN]);
        assertEquals(0, table.rows.get(0).length);
    }

    @Test
    public void existingFullAndOverwideRowsKeepTheirData(){
        Report report = new Report("Existing", 0L, "test");
        Report.Table table = new Report.Table("Rows", Report.Provenance.RECORDED);
        table.headers.add("One");
        table.rows.add(new Report.Cell[]{new Report.Cell("first", CellText.EXACT), new Report.Cell("extra", CellText.EXACT)});
        table.rows.add(new Report.Cell[]{new Report.Cell("complete", CellText.EXACT)});
        report.add(table);

        assertTrue(ReportRenderer.markdown(report).contains("| first | extra |\n| complete |\n"));
        assertTrue(ReportRenderer.text(report).contains("  One first | extra\n  One complete\n"));
        assertTrue(ReportRenderer.html(report).contains("<td class=\"exact\">first</td><td class=\"exact\">extra</td>"));
        assertTrue(ReportRenderer.csv(report).contains("One\r\nfirst,extra\r\ncomplete\r\n"));
    }
}
