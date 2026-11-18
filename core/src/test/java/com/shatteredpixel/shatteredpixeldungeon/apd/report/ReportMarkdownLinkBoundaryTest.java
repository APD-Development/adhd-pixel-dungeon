package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportMarkdownLinkBoundaryTest {

    @Test
    public void reportTextCannotBecomeClickableMarkdownLinksOrExternalImages() {
        String link = "[view run](javascript:alert(1))";
        String image = "![external screenshot](https://example.invalid/tracker.png)";
        Report report = new Report(link, 0L, image);
        report.about.add(image);
        report.add(new Report.Heading(1, link));

        Report.Facts facts = new Report.Facts(Report.Provenance.RECORDED);
        facts.section(link).add(image, new Report.Cell(link, CellText.EXACT), image);
        report.add(facts);

        Report.Table table = new Report.Table(link, Report.Provenance.RECORDED);
        table.headers.add(image);
        table.coverage.add(link);
        table.rows.add(new Report.Cell[]{new Report.Cell(link, CellText.EXACT)});
        report.add(table);

        String markdown = ReportRenderer.markdown(report);
        assertFalse(markdown, markdown.contains(link));
        assertFalse(markdown, markdown.contains(image));
        assertTrue(markdown, markdown.startsWith("# &#91;view run&#93;(javascript:alert(1))\n\n"));
        assertTrue(markdown, markdown.contains("!&#91;external screenshot&#93;(https://example.invalid/tracker.png)"));
        assertTrue(markdown, markdown.contains("| !&#91;external screenshot&#93;(https://example.invalid/tracker.png) |"));
        assertTrue(markdown, markdown.contains("Known out of listed (per column): !&#91;external screenshot&#93;"));
        assertTrue(markdown, markdown.contains("Made by ADHD Pixel Dungeon UIMSD !&#91;external screenshot&#93;"));

        // The renderer must never edit the original history/statistics report model or other formats.
        assertEquals(link, report.title);
        assertEquals(image, report.about.get(0));
        assertEquals(link, table.rows.get(0)[0].text);
        assertTrue(ReportRenderer.text(report).contains(link));
        assertTrue(ReportRenderer.html(report).contains(link));
        assertTrue(ReportRenderer.csv(report).contains(link));
    }

    @Test
    public void multilineQuotedNotesNeutralizeLinkDelimitersWithoutLosingLinesOrExistingEscapes() {
        String note = "first\r\n[other](https://example.invalid/away)\r![pixel](https://example.invalid/pixel.png)\nlast";
        Report report = new Report("Normal", 0L, "1.0");
        report.add(new Report.Note(Report.NoteKind.NOTE, note));
        Report.Table table = new Report.Table("Items", Report.Provenance.RECORDED);
        table.headers.add("Found|total");
        table.rows.add(new Report.Cell[]{new Report.Cell(">=7", CellText.LOWER)});
        report.add(table);

        String markdown = ReportRenderer.markdown(report);
        assertTrue(markdown, markdown.contains(
                "> Note: first\n> &#91;other&#93;(https://example.invalid/away)\n> !&#91;pixel&#93;(https://example.invalid/pixel.png)\n> last\n\n"));
        assertFalse(markdown, markdown.contains("[other](https://example.invalid/away)"));
        assertFalse(markdown, markdown.contains("![pixel](https://example.invalid/pixel.png)"));
        assertTrue(markdown, markdown.contains("| Found\\|total |\n| --- |\n| >=7 |\n"));
        assertEquals(note, ((Report.Note) report.blocks.get(0)).text);
        assertTrue(ReportRenderer.text(report).contains(note));
        assertTrue(ReportRenderer.html(report).contains(note.replace("&", "&amp;")));
    }
}
