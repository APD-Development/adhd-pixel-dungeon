package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportMarkdownMetadataBoundaryTest {

    @Test
    public void multilineMetadataCannotCreateExtraMarkdownSections(){
        String title = "Report\r\n## forged|title\\part";
        String about = "Scope\r## injected\n- fake|scope\\path";
        String heading = "Actual\n## fabricated|heading\\path";
        String tableTitle = "Loot\r\n## fake|table\\part";
        String version = "v1\r## forged-footer|v\\2";
        Report report = new Report(title, 0L, version);
        report.about.add(about);
        report.add(new Report.Heading(1, heading));
        Report.Table table = new Report.Table(tableTitle, Report.Provenance.RECORDED);
        table.headers.add("Item");
        table.rows.add(new Report.Cell[]{new Report.Cell("Coin", CellText.EXACT)});
        report.add(table);

        String md = ReportRenderer.markdown(report);
        assertTrue(md, md.startsWith("# Report ## forged\\|title\\\\part\n\n"));
        assertTrue(md, md.contains("Scope ## injected - fake\\|scope\\\\path  \n"));
        assertTrue(md, md.contains("\n## Actual ## fabricated\\|heading\\\\path\n\n"));
        assertTrue(md, md.contains("**Loot ## fake\\|table\\\\part**  \n"));
        assertTrue(md, md.contains("Made by ADHD Pixel Dungeon UIMSD v1 ## forged-footer\\|v\\\\2 on 1970-01-01 00:00 UTC."));
        assertFalse(md, md.contains("\r"));
        assertFalse(md, md.contains("\n## forged"));
        assertFalse(md, md.contains("\n## injected"));
        assertFalse(md, md.contains("\n## fabricated"));
        assertFalse(md, md.contains("\n## fake"));
        assertFalse(md, md.contains("\n## forged-footer"));
        assertTrue(md, md.contains("| Item |\n| --- |\n| Coin |\n"));

        // Sanitization is only at Markdown output time, not a mutation of the report.
        assertEquals(title, report.title);
        assertEquals(about, report.about.get(0));
        assertEquals(heading, ((Report.Heading) report.blocks.get(0)).text);
        assertEquals(tableTitle, table.title);
        assertEquals(version, report.appVersion);
        assertTrue("plain text preserves its preexisting multiline representation",
                ReportRenderer.text(report).contains(heading.toUpperCase()));
        assertTrue("HTML retains the literal title text in escaped HTML",
                ReportRenderer.html(report).contains("Report\r\n## forged|title\\part"));
        assertTrue("CSV comment sanitization remains unchanged",
                ReportRenderer.csv(report).contains("# Loot  ## fake|table\\part [RECORDED]\r\n"));
    }

    @Test
    public void ordinaryMetadataAndCanonicalProvenanceStayUnchanged(){
        Report report = new Report("Ordinary report", 0L, "1.0");
        report.about.add("Only finished recorded runs");
        report.add(new Report.Heading(2, "Summary"));
        Report.Table table = new Report.Table("Statistics", Report.Provenance.CANONICAL);
        table.headers.add("Gold");
        table.rows.add(new Report.Cell[]{new Report.Cell("~5", CellText.INFERRED)});
        report.add(table);

        String md = ReportRenderer.markdown(report);
        assertTrue(md, md.startsWith("# Ordinary report\n\nOnly finished recorded runs  \n\n"));
        assertTrue(md, md.contains("\n### Summary\n\n"));
        assertTrue(md, md.contains("**Statistics**  \n"));
        assertTrue(md, md.contains(ReportRenderer.CANONICAL_NOTICE));
        assertTrue(md, md.contains("| Gold |\n| --- |\n| ~5 |\n"));
        assertTrue(md, md.contains("Made by ADHD Pixel Dungeon UIMSD 1.0 on 1970-01-01 00:00 UTC."));
        assertTrue(ReportRenderer.csv(report).contains("# Statistics [CANONICAL (seed replay, not recorded history)]\r\n"));
        assertTrue(report.statesUsed()[CellText.INFERRED]);
    }
}
