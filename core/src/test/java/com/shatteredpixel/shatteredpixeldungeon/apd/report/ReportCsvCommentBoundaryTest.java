package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportCsvCommentBoundaryTest {

    @Test
    public void factAndTableHeadingsCannotInjectAnExtraCsvRow(){
        Report report = new Report("Run\r\n=HYPERLINK(1)\n+1+2", 0, "test");
        Report.Facts facts = new Report.Facts(Report.Provenance.RECORDED);
        facts.add("Outcome", new Report.Cell("finished", CellText.EXACT), "");
        report.add(facts);

        Report.Table table = new Report.Table("Items\n@SUM(1)\rDetail", Report.Provenance.RECORDED);
        table.headers.add("Item");
        table.rows.add(new Report.Cell[]{new Report.Cell("healing", CellText.EXACT)});
        report.add(table);

        String csv = ReportRenderer.csv(report);
        assertTrue(csv, csv.contains("# Run  =HYPERLINK(1) +1+2 facts [RECORDED]\r\n"));
        assertTrue(csv, csv.contains("# Items @SUM(1) Detail [RECORDED]\r\n"));
        assertFalse(csv, csv.contains("\r\n=HYPERLINK(1)"));
        assertFalse(csv, csv.contains("\r\n@SUM(1)"));
        assertFalse(csv, csv.contains("\n+1+2"));
        assertTrue(csv, csv.contains("\r\nhealing\r\n"));
    }

    @Test
    public void noTableTitleAndVersionCannotInjectMetadataRows(){
        Report report = new Report("No tables\r=1+2\n@SUM(1)", 0, "test\r\n=COMMAND");
        String csv = ReportRenderer.csv(report);

        assertTrue(csv, csv.startsWith("# No tables =1+2 @SUM(1) has no table\r\n"));
        assertTrue(csv, csv.contains("UIMSD test  =COMMAND on 1970-01-01 00:00 UTC."));
        assertFalse(csv, csv.contains("\r\n=COMMAND"));
        assertFalse(csv, csv.contains("\n@SUM(1)"));
    }

    @Test
    public void normalCommentsAndQuotedMultilineDataRemainCompatible(){
        Report report = new Report("Inventory", 0, "test");
        Report.Table table = new Report.Table("Items", Report.Provenance.RECORDED);
        table.headers.add("Item");
        table.rows.add(new Report.Cell[]{new Report.Cell("line one\nline two", CellText.EXACT)});
        report.add(table);

        String csv = ReportRenderer.csv(report);
        assertTrue(csv, csv.startsWith("# Items [RECORDED]\r\nItem\r\n"));
        assertTrue(csv, csv.contains("\"line one\nline two\"\r\n"));
        assertTrue(csv, csv.endsWith("Figures are what this device recorded; they are not verified and not a ranking.\r\n"));
    }
}
