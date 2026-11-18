package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportMarkdownCrBoundaryTest {

    @Test
    public void carriageReturnsInTableHeadersAndCellsStayOnOneMarkdownRow(){
        Report report = new Report("Run summary", 0, "test");
        Report.Table table = new Report.Table("Items", Report.Provenance.RECORDED);
        table.headers.add("Item\r\nname");
        table.headers.add("Effect\rdetails");
        table.rows.add(new Report.Cell[]{
                new Report.Cell("red\r\nblue", CellText.EXACT),
                new Report.Cell("one\rtwo\nthree", CellText.EXACT)
        });
        report.add(table);

        String markdown = ReportRenderer.markdown(report);
        assertTrue(markdown, markdown.contains(
                "| Item name | Effect details |\n| --- | --- |\n| red blue | one two three |\n"));
        assertFalse("no carriage return should split a Markdown table row", markdown.contains("\r"));
        assertEquals("source headers must remain unmodified", "Item\r\nname", table.headers.get(0));
        assertEquals("source cells must remain unmodified", "red\r\nblue", table.rows.get(0)[0].text);
    }

    @Test
    public void existingEscapesAndOtherReportFormatsRemainUnchanged(){
        Report report = new Report("Inventory", 0, "test");
        Report.Table table = new Report.Table("Items", Report.Provenance.RECORDED);
        String source = "path\\folder|kind\r\nnext";
        table.headers.add("Name");
        table.rows.add(new Report.Cell[]{new Report.Cell(source, CellText.EXACT)});
        report.add(table);

        String markdown = ReportRenderer.markdown(report);
        assertTrue(markdown, markdown.contains("path\\\\folder\\|kind next"));
        assertFalse(markdown, markdown.contains("\r"));
        assertTrue("CSV still quotes the original multiline data", ReportRenderer.csv(report).contains("\"" + source + "\""));
        assertTrue("HTML still includes the original data", ReportRenderer.html(report).contains(source));
        assertTrue("plain text still includes the original data", ReportRenderer.text(report).contains(source));
        assertEquals(source, table.rows.get(0)[0].text);
    }
}
