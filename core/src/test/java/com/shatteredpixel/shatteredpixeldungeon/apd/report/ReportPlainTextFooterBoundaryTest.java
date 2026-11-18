package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportPlainTextFooterBoundaryTest {

    @Test
    public void multilineVersionCannotIntroduceFalseTextReportFooterLines(){
        String version = "1.0\r\nRECORDED RESULTS: 999\nFORGED: 100";
        Report report = new Report("History", 0L, version);
        Report.Facts facts = new Report.Facts(Report.Provenance.RECORDED);
        facts.add("Gold", new Report.Cell("8", CellText.EXACT), "");
        report.add(facts);

        String text = ReportRenderer.text(report);
        assertTrue(text, text.contains("  Gold: 8\n"));
        assertTrue(text, text.contains("Made by ADHD Pixel Dungeon UIMSD 1.0 RECORDED RESULTS: 999 FORGED: 100 on 1970-01-01 00:00 UTC."));
        assertFalse(text, text.contains("\nRECORDED RESULTS: 999"));
        assertFalse(text, text.contains("\nFORGED: 100"));
        assertFalse(text, text.contains("\r"));
        assertEquals(version, report.appVersion);
        assertTrue(ReportRenderer.markdown(report).contains("UIMSD 1.0 RECORDED RESULTS: 999 FORGED: 100 on"));
        assertTrue(ReportRenderer.csv(report).contains("# marks:"));
    }

    @Test
    public void ordinaryFooterAndPriorMultilinePlainTextBodiesRemainUnchanged(){
        Report report = new Report("History\nDetail", 0L, "1.0");
        Report.Facts facts = new Report.Facts(Report.Provenance.RECORDED);
        String legacyLabel = "Wins\nReported";
        facts.add(legacyLabel, new Report.Cell(">=7\nmore", CellText.LOWER), "source");
        report.add(facts);

        String text = ReportRenderer.text(report);
        assertTrue(text, text.startsWith("HISTORY\nDETAIL\n"));
        assertTrue(text, text.contains(legacyLabel + ": >=7\nmore"));
        assertTrue(text, text.contains("Made by ADHD Pixel Dungeon UIMSD 1.0 on 1970-01-01 00:00 UTC."));
        assertTrue(text, text.endsWith("Figures are what this device recorded; they are not verified and not a ranking.\n"));
        assertEquals(legacyLabel, facts.facts.get(0).label);
        assertEquals(">=7\nmore", facts.facts.get(0).value.text);
    }
}
