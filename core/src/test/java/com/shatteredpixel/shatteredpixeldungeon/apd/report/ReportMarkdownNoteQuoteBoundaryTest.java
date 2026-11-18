package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReportMarkdownNoteQuoteBoundaryTest {

    @Test
    public void multilineWarningRetainsEveryLogicalLineInsideOneBlockquote(){
        Report report = new Report("History notice", 0, "test");
        String original = "first line\r\nsecond line\rthird line\n\n## text from saved note";
        report.add(new Report.Note(Report.NoteKind.WARN, original));

        String markdown = ReportRenderer.markdown(report);
        assertTrue(markdown, markdown.contains(
                "> **Warning:** first line\n> second line\n> third line\n> \n> ## text from saved note\n\n"));
        assertFalse("no saved carriage return may split a quote", markdown.contains("\r"));
        assertFalse("saved note lines must not become independent report headings",
                markdown.contains("\n## text from saved note\n"));
        assertEquals("renderer must not mutate saved note text",
                original, ((Report.Note) report.blocks.get(0)).text);
        assertTrue("plain text must retain source line breaks",
                ReportRenderer.text(report).contains("WARNING: " + original + "\n"));
        assertTrue("HTML must retain source line breaks",
                ReportRenderer.html(report).contains(original));
    }

    @Test
    public void ordinaryNotesAndExistingNonNoteMarkdownAreUnchanged(){
        Report report = new Report("Ordinary", 0, "test");
        report.add(new Report.Note(Report.NoteKind.NOTE, "Keep **inline emphasis**."));
        report.add(new Report.Note(Report.NoteKind.INFO, "One\nTwo"));
        report.add(new Report.Heading(1, "After notes"));
        String markdown = ReportRenderer.markdown(report);

        assertTrue(markdown, markdown.contains("> Note: Keep **inline emphasis**.\n\n"));
        assertTrue(markdown, markdown.contains("> One\n> Two\n\n"));
        assertTrue(markdown, markdown.contains("\n## After notes\n\n"));
        assertFalse("informational continuation must remain quoted",
                markdown.contains("\nTwo\n"));
        assertTrue("CSV table-less export remains unchanged",
                ReportRenderer.csv(report).contains("# Ordinary has no table\r\n"));
    }
}
