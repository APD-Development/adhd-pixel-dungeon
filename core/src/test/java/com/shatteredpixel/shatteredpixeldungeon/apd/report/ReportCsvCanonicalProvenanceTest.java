package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportCsvCanonicalProvenanceTest {

    private static Report report( Report.Provenance provenance ){
        Report r = new Report("Seed floors", 0, "test");
        Report.Table table = new Report.Table("Items", provenance);
        table.headers.add("Basis");
        table.rows.add(new Report.Cell[]{new Report.Cell("Recorded floor 1", CellText.EXACT)});
        table.rows.add(new Report.Cell[]{new Report.Cell("~Canonical floor 2", CellText.INFERRED)});
        r.add(table);
        return r;
    }

    @Test
    public void canonicalAndMixedCsvExposeReplayWarningWithoutChangingRows(){
        for (Report.Provenance provenance : new Report.Provenance[]{
                Report.Provenance.CANONICAL, Report.Provenance.MIXED}){
            Report r = report(provenance);
            String csv = ReportRenderer.csv(r);
            String warning = "# " + ReportRenderer.CANONICAL_NOTICE + "\r\n";
            assertTrue(csv, csv.startsWith(warning));
            assertTrue(csv, csv.contains("They are NOT recorded history"));
            assertTrue(csv, csv.contains("NOT what this run would have met"));
            assertEquals("exactly one canonical warning", csv.indexOf(warning), csv.lastIndexOf(warning));
            assertTrue(csv, csv.contains("\r\nBasis\r\nRecorded floor 1\r\n~Canonical floor 2\r\n"));
            assertTrue(csv, csv.contains("# Items ["));
            assertTrue(csv, csv.endsWith("Figures are what this device recorded; they are not verified and not a ranking.\r\n"));
        }
    }

    @Test
    public void recordedOnlyCsvStaysByteForByteCompatible(){
        Report r = new Report("Inventory", 0, "test");
        Report.Table table = new Report.Table("Items", Report.Provenance.RECORDED);
        table.headers.add("Item");
        table.rows.add(new Report.Cell[]{new Report.Cell("healing", CellText.EXACT)});
        r.add(table);

        assertEquals("# Items [RECORDED]\r\n"
                + "Item\r\n"
                + "healing\r\n"
                + "# marks: ? unknown, >=N at least N, ~N inferred or canonical, hidden = hidden information. "
                + "Made by ADHD Pixel Dungeon UIMSD test on 1970-01-01 00:00 UTC. "
                + "Figures are what this device recorded; they are not verified and not a ranking.\r\n",
                ReportRenderer.csv(r));
    }

    @Test
    public void syntheticCsvKeepsDemoWarningBeforeCanonicalWarning(){
        Report r = report(Report.Provenance.CANONICAL);
        r.synthetic = true;
        String csv = ReportRenderer.csv(r);
        assertTrue(csv, csv.startsWith("# " + ReportRenderer.SYNTHETIC_NOTICE + "\r\n"
                + "# " + ReportRenderer.CANONICAL_NOTICE + "\r\n"));
        assertTrue(csv, csv.contains("\r\nBasis\r\nRecorded floor 1\r\n"));
        assertFalse(csv, csv.contains("\r\nCANONICAL DATA:"));
    }
}
