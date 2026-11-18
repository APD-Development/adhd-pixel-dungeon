package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

public class ReportPlainTextLocaleTest {

    @Test
    public void plainTextReportTitleAndLevelOneHeadingDoNotDependOnDeviceLocale() {
        Report report = new Report("initial inspection", 0L, "1.0");
        report.about.add("indigo item data");
        report.add(new Report.Heading(1, "inventory items"));
        report.add(new Report.Heading(2, "inside notes"));

        Locale original = Locale.getDefault();
        String english;
        String turkish;
        try {
            Locale.setDefault(Locale.ENGLISH);
            english = ReportRenderer.text(report);
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            turkish = ReportRenderer.text(report);
        } finally {
            Locale.setDefault(original);
        }

        assertEquals("same report must export identical bytes regardless of locale", english, turkish);
        assertTrue(turkish, turkish.startsWith("INITIAL INSPECTION\n"));
        assertTrue(turkish, turkish.contains("\nINVENTORY ITEMS\n"));
        assertTrue(turkish, turkish.contains("\ninside notes\n"));
        assertTrue("about data must not be uppercased", turkish.contains("indigo item data\n"));
        assertEquals("initial inspection", report.title);
        assertEquals("inventory items", ((Report.Heading) report.blocks.get(0)).text);
    }

    @Test
    public void nonAsciiTitleUsesLocaleNeutralCaseWithoutChangingOtherRenderers() {
        Report report = new Report("iı", 0L, "1.0");
        report.add(new Report.Heading(1, "iı"));
        String markdown = ReportRenderer.markdown(report);
        String html = ReportRenderer.html(report);
        String csv = ReportRenderer.csv(report);

        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            String text = ReportRenderer.text(report);
            assertTrue(text, text.startsWith("II\n==\n"));
            assertTrue(text, text.contains("\nII\n"));
            assertEquals("Markdown must be independent of plain-text capitalization", markdown,
                    ReportRenderer.markdown(report));
            assertEquals("HTML must be independent of plain-text capitalization", html,
                    ReportRenderer.html(report));
            assertEquals("CSV must be independent of plain-text capitalization", csv,
                    ReportRenderer.csv(report));
        } finally {
            Locale.setDefault(original);
        }
        assertEquals("iı", report.title);
    }
}
