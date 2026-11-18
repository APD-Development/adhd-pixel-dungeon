package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReportMarkdownEscapeTest {
    private static Report report(String header, String... values) {
        Report report = new Report("Escaping", 0, "test");
        Report.Table table = new Report.Table("Values", Report.Provenance.RECORDED);
        table.headers.add(header);
        for (String value : values) {
            table.rows.add(new Report.Cell[]{new Report.Cell(value, CellText.EXACT)});
        }
        report.add(table);
        return report;
    }

    @Test public void literalBackslashesSurviveInMarkdownHeadersAndValues() {
        Report report = report("Kind\\|Name", "one\\|two", "folder\\\\file\\*x");
        String markdown = ReportRenderer.markdown(report);
        // GFM removes a table-pipe escape before interpreting ordinary inline escapes.
        assertTrue(markdown, markdown.contains("| Kind\\\\\\|Name |\n"));
        assertTrue(markdown, markdown.contains("| one\\\\\\|two |\n"));
        assertTrue(markdown, markdown.contains("| folder\\\\\\\\file\\\\*x |\n"));
        // Export escaping must not mutate the report or leak into other formats.
        assertEquals("one\\|two", report.firstTable().rows.get(0)[0].text);
        assertTrue(ReportRenderer.text(report).contains("one\\|two"));
        assertTrue(ReportRenderer.html(report).contains("one\\|two"));
        assertTrue(ReportRenderer.csv(report).contains("one\\|two\r\n"));
    }

    @Test public void ordinaryPipesLineBreaksAndEmptyValuesKeepExistingBehavior() {
        Report report = report("Value", "a|b\nc", "", null, "?");
        String markdown = ReportRenderer.markdown(report);
        assertTrue(markdown, markdown.contains("| a\\|b c |\n|  |\n|  |\n| ? |\n"));
        assertTrue(ReportRenderer.csv(report).contains("\"a|b\nc\"\r\n"));
        assertTrue(ReportRenderer.html(report).contains("a|b\nc"));
    }
}
