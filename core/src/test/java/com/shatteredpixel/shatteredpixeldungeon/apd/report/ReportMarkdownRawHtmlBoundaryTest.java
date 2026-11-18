package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportMarkdownRawHtmlBoundaryTest {

    @Test
    public void rawHtmlInAllMarkdownFieldsStaysLiteralWithoutMutatingReportData() {
        final String raw = "<img src=x onerror=alert(1)>";
        Report report = new Report(raw, 0L, raw);
        report.about.add(raw);
        report.add(new Report.Heading(1, raw));
        Report.Facts facts = new Report.Facts(Report.Provenance.RECORDED);
        facts.section(raw).add(raw, new Report.Cell(raw, CellText.EXACT), raw);
        report.add(facts);
        Report.Table table = new Report.Table(raw, Report.Provenance.RECORDED);
        table.headers.add(raw);
        table.coverage.add(raw);
        table.rows.add(new Report.Cell[]{new Report.Cell(raw, CellText.EXACT)});
        report.add(table);

        String md = ReportRenderer.markdown(report);
        assertFalse("No raw HTML opener may survive as report markup", md.contains(raw));
        assertFalse(md.contains("<img"));
        assertTrue(md, md.startsWith("# &lt;img src=x onerror=alert(1)>\n\n"));
        assertTrue(md, md.contains("**&lt;img src=x onerror=alert(1)>**"));
        assertTrue(md, md.contains("| &lt;img src=x onerror=alert(1)> |"));
        assertTrue(md, md.contains("Known out of listed (per column): &lt;img"));
        assertTrue(md, md.contains("Made by ADHD Pixel Dungeon UIMSD &lt;img"));

        assertEquals(raw, report.title);
        assertEquals(raw, report.about.get(0));
        assertEquals(raw, facts.facts.get(1).value.text);
        assertEquals(raw, table.rows.get(0)[0].text);
        assertTrue("Plain-text output remains unchanged", ReportRenderer.text(report).contains(raw));
        assertTrue("HTML already escapes the source text", ReportRenderer.html(report).contains("&lt;img src=x"));
        assertTrue("CSV data stays untouched", ReportRenderer.csv(report).contains(raw));
    }

    @Test
    public void multilineNotesCannotInjectHtmlAndOrdinaryMarkdownOutputIsRetained() {
        Report report = new Report("Normal", 0L, "1.0");
        String note = "first\r\n<iframe src=evil></iframe>\rthird";
        report.add(new Report.Note(Report.NoteKind.WARN, note));
        Report.Table table = new Report.Table("Items", Report.Provenance.RECORDED);
        table.headers.add("Found|total");
        table.rows.add(new Report.Cell[]{new Report.Cell(">=7", CellText.LOWER)});
        report.add(table);

        String md = ReportRenderer.markdown(report);
        assertTrue(md, md.contains("> **Warning:** first\n> &lt;iframe src=evil>&lt;/iframe>\n> third\n\n"));
        assertFalse(md.contains("<iframe"));
        assertFalse(md.contains("</iframe"));
        assertTrue(md, md.contains("| Found\\|total |\n| --- |\n| >=7 |\n"));
        assertTrue(md, md.contains(">=: at least this much") || md.contains(">=:"));
        assertEquals(note, ((Report.Note) report.blocks.get(0)).text);
        assertTrue(ReportRenderer.text(report).contains(note));
        assertTrue(ReportRenderer.html(report).contains("&lt;iframe src=evil&gt;"));
    }
}
