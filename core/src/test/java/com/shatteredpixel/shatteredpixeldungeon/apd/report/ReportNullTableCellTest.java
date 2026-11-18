package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportNullTableCellTest {

    private static Report sparseReport(){
        Report r = new Report("Sparse", 0L, "test");
        Report.Table t = new Report.Table("Rows", Report.Provenance.RECORDED);
        t.headers.add("Left");
        t.headers.add("Right");
        t.rows.add(new Report.Cell[]{null, new Report.Cell("kept", CellText.EXACT)});
        r.add(t);
        return r;
    }

    @Test
    public void nullTableCellIsBlankInAllFourFormatsWithoutMutatingData(){
        Report r = sparseReport();
        assertTrue(ReportRenderer.markdown(r).contains("|  | kept |\n"));
        assertTrue(ReportRenderer.text(r).contains("  Left  | Right kept\n"));
        assertTrue(ReportRenderer.html(r).contains("<td class=\"exact\"></td><td class=\"exact\">kept</td>"));
        assertTrue(ReportRenderer.csv(r).contains("Left,Right\r\n,kept\r\n"));
        assertNull(r.firstTable().rows.get(0)[0]);
        assertEquals("kept", r.firstTable().rows.get(0)[1].text);
    }

    @Test
    public void sparseCanonicalRowKeepsProvenanceButDoesNotInventUnknownValue(){
        Report r = new Report("Seed floors", 0L, "test");
        Report.Table t = new Report.Table("Seed", Report.Provenance.MIXED);
        t.headers.add("Basis");
        t.rows.add(new Report.Cell[]{null});
        r.add(t);

        assertTrue(r.hasCanonical());
        assertFalse(r.statesUsed()[CellText.UNKNOWN]);
        String md = ReportRenderer.markdown(r);
        assertTrue(md.contains(ReportRenderer.CANONICAL_NOTICE));
        assertTrue(md.contains("|  |\n"));
        assertTrue(ReportRenderer.text(r).contains("  Basis \n"));
        assertTrue(ReportRenderer.html(r).contains("<td class=\"exact\"></td>"));
        String csv = ReportRenderer.csv(r);
        assertTrue(csv.startsWith("# " + ReportRenderer.CANONICAL_NOTICE + "\r\n"));
        assertTrue(csv.contains("Basis\r\n\r\n"));
        assertNull(t.rows.get(0)[0]);
    }

    @Test
    public void neighboringRealCellRetainsItsLowerBoundAndSmallSampleMarkers(){
        Report r = sparseReport();
        Report.Table t = r.firstTable();
        t.rows.get(0)[1] = new Report.Cell(">=3", CellText.LOWER, true);
        assertTrue(r.statesUsed()[CellText.LOWER]);
        assertTrue(ReportRenderer.markdown(r).contains("|  | >=3 (small sample) |"));
        assertTrue(ReportRenderer.text(r).contains("Right >=3"));
        assertTrue(ReportRenderer.html(r).contains("<td class=\"exact\"></td><td class=\"lower small\">&gt;=3</td>"));
        assertTrue(ReportRenderer.csv(r).contains("Left,Right\r\n,>=3\r\n"));
    }
}
