package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportMarkdownEmphasisBoundaryTest {

    @Test
    public void reportFieldsDisplayEmphasisDelimitersLiterally() {
        final String literal = "*italic* __bold__ ~~crossed~~";
        final String encoded = "&#42;italic&#42; &#95;&#95;bold&#95;&#95; &#126;&#126;crossed&#126;&#126;";
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

        String md = ReportRenderer.markdown(report);
        assertTrue(md, md.startsWith("# " + encoded + "\n\n"));
        assertTrue(md, md.contains("## " + encoded + "\n\n"));
        assertTrue(md, md.contains("**" + encoded + "**"));
        assertTrue(md, md.contains("| " + encoded + " |"));
        assertTrue(md, md.contains("Known out of listed (per column): " + encoded + " " + encoded));
        assertTrue(md, md.contains("Made by ADHD Pixel Dungeon UIMSD " + encoded));
        assertFalse("Recorded data must not introduce italic markup", md.contains("*italic*"));
        assertFalse("Recorded data must not introduce bold markup", md.contains("__bold__"));
        assertFalse("Recorded data must not introduce strike markup", md.contains("~~crossed~~"));

        assertEquals(literal, report.title);
        assertEquals(literal, report.about.get(0));
        assertEquals(literal, facts.facts.get(1).value.text);
        assertEquals(literal, table.rows.get(0)[0].text);
        assertTrue(ReportRenderer.text(report).contains(literal));
        assertTrue(ReportRenderer.html(report).contains(literal));
        assertTrue(ReportRenderer.csv(report).contains(literal));
    }

    @Test
    public void multilineNotesRetainExistingInlineEmphasisAndSafeLinks() {
        final String note = "first *italic*\r\n__bold__ and ~~strike~~\r[last](link)\nfinal";
        Report report = new Report("Normal", 0L, "1.0");
        report.add(new Report.Note(Report.NoteKind.WARN, note));

        String md = ReportRenderer.markdown(report);
        assertTrue(md, md.contains(
                "> **Warning:** first *italic*\n> __bold__ and ~~strike~~\n> &#91;last&#93;(link)\n> final\n\n"));
        assertTrue(md, md.contains("*italic*"));
        assertTrue(md, md.contains("__bold__"));
        assertTrue(md, md.contains("~~strike~~"));
        assertEquals(note, ((Report.Note) report.blocks.get(0)).text);
        assertTrue(ReportRenderer.text(report).contains(note));
        assertTrue(ReportRenderer.html(report).contains(note));
    }
}
