package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportMarkdownCodeSpanBoundaryTest {

    @Test
    public void literalBackticksInReportFieldsDoNotCreateMarkdownCodeSpans() {
        final String literal = "`reported`";
        Report report = new Report(literal, 0L, literal);
        report.about.add(literal);
        report.add(new Report.Heading(1, literal));

        Report.Facts facts = new Report.Facts(Report.Provenance.RECORDED);
        facts.section(literal).add(literal, new Report.Cell(literal, CellText.EXACT), literal);
        report.add(facts);

        Report.Table table = new Report.Table(literal, Report.Provenance.RECORDED);
        table.headers.add(literal);
        table.coverage.add(literal);
        table.rows.add(new Report.Cell[]{new Report.Cell(literal, CellText.EXACT)});
        report.add(table);

        String markdown = ReportRenderer.markdown(report);
        assertFalse("No active code-span delimiter should come from report data", markdown.contains("`"));
        assertTrue(markdown, markdown.startsWith("# &#96;reported&#96;\n\n"));
        assertTrue(markdown, markdown.contains("## &#96;reported&#96;\n\n"));
        assertTrue(markdown, markdown.contains("**&#96;reported&#96;**"));
        assertTrue(markdown, markdown.contains("| &#96;reported&#96; |"));
        assertTrue(markdown, markdown.contains("Known out of listed (per column): &#96;reported&#96; &#96;reported&#96;"));
        assertTrue(markdown, markdown.contains("Made by ADHD Pixel Dungeon UIMSD &#96;reported&#96;"));

        assertEquals(literal, report.title);
        assertEquals(literal, report.about.get(0));
        assertEquals(literal, facts.facts.get(1).value.text);
        assertEquals(literal, table.rows.get(0)[0].text);
        assertTrue(ReportRenderer.text(report).contains(literal));
        assertTrue(ReportRenderer.html(report).contains(literal));
        assertTrue(ReportRenderer.csv(report).contains(literal));
    }

    @Test
    public void multilineQuotedNotesPreserveLinesAndLiteralCodeMarkers() {
        final String note = "first\r\n`literal code`\r[not a link](target)\nlast";
        Report report = new Report("Normal", 0L, "1.0");
        report.add(new Report.Note(Report.NoteKind.WARN, note));

        String markdown = ReportRenderer.markdown(report);
        assertTrue(markdown, markdown.contains(
                "> **Warning:** first\n> &#96;literal code&#96;\n> &#91;not a link&#93;(target)\n> last\n\n"));
        assertFalse(markdown, markdown.contains("`literal code`"));
        assertEquals(note, ((Report.Note) report.blocks.get(0)).text);
        assertTrue(ReportRenderer.text(report).contains(note));
        assertTrue(ReportRenderer.html(report).contains(note));
    }
}
