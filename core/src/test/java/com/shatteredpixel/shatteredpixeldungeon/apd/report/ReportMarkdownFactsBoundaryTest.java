package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportMarkdownFactsBoundaryTest {

    @Test
    public void multilineFactSectionsLabelsValuesAndDetailsCannotCreateNewMarkdownEntries(){
        Report report = new Report("Fact boundaries", 0L, "test");
        Report.Facts facts = new Report.Facts(Report.Provenance.RECORDED);
        String section = "Section\r## inserted";
        String label = "Wins\n## duplicate";
        String value = ">=7\r\n- invented|raw\\path";
        String detail = "source\rreview\nnext";
        facts.section(section).add(label, new Report.Cell(value, CellText.LOWER), detail);
        report.add(facts);

        String markdown = ReportRenderer.markdown(report);
        assertTrue(markdown, markdown.contains("\n**Section ## inserted**\n\n"));
        assertTrue(markdown, markdown.contains(
                "- Wins ## duplicate: >=7 - invented\\|raw\\\\path (source review next)\n"));
        assertFalse(markdown, markdown.contains("\n## inserted\n"));
        assertFalse(markdown, markdown.contains("\n## duplicate\n"));
        assertFalse(markdown, markdown.contains("\n- invented|raw"));
        assertFalse(markdown, markdown.contains("\r"));

        assertEquals(section, facts.facts.get(0).label);
        assertEquals(label, facts.facts.get(1).label);
        assertEquals(value, facts.facts.get(1).value.text);
        assertEquals(detail, facts.facts.get(1).detail);
        assertTrue(report.statesUsed()[CellText.LOWER]);
        assertFalse(report.statesUsed()[CellText.UNKNOWN]);

        assertTrue("plain text remains unmodified", ReportRenderer.text(report).contains(label + ": " + value));
        assertTrue("HTML retains the original logical text after escaping", ReportRenderer.html(report).contains("invented|raw\\path"));
        assertTrue("CSV retains multiline fact data", ReportRenderer.csv(report).contains("\"" + value + "\""));
    }

    @Test
    public void ordinaryFactsAndCanonicalNoticeStayIdentical(){
        Report report = new Report("Ordinary facts", 0L, "test");
        Report.Facts facts = new Report.Facts(Report.Provenance.CANONICAL);
        facts.section("Loot").add("Gold", new Report.Cell(">=9", CellText.LOWER), "collected");
        report.add(facts);

        String markdown = ReportRenderer.markdown(report);
        assertTrue(markdown, markdown.contains("\n**Loot**\n\n- Gold: >=9 (collected)\n"));
        assertTrue(markdown, markdown.contains(ReportRenderer.CANONICAL_NOTICE));
        assertTrue(ReportRenderer.csv(report).contains("# " + ReportRenderer.CANONICAL_NOTICE + "\r\n"));
        assertEquals("Gold", facts.facts.get(1).label);
    }
}
