package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Dataset;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;

public class RunComparisonDifferenceTest {
    private static Report.Cell difference(Long left, Long right, int provenance) {
        DatasetDef def = new DatasetDef(Datasets.RUNS, "Runs", "");
        def.add(ColumnDef.num("run.value", "Value"));
        Row a = new Row("a", null, "a", 1), b = new Row("b", null, "b", 1);
        a.finished = b.finished = true;
        if (left != null) a.setNum(0, left, provenance);
        if (right != null) b.setNum(0, right, provenance);
        Catalog catalog = new Catalog().add(new Dataset(def, Arrays.asList(a, b), null));
        return ReportBuilder.compareRuns(catalog, a, b, false, 0, "test").firstTable().rows.get(0)[3];
    }

    @Test public void positiveDifferenceBeyondLongRangeIsExact() {
        assertEquals("+18446744073709551615", difference(Long.MIN_VALUE, Long.MAX_VALUE, Fact.PROV_RECORDED).text);
    }
    @Test public void negativeDifferenceBeyondLongRangeIsExact() {
        assertEquals("-18446744073709551615", difference(Long.MAX_VALUE, Long.MIN_VALUE, Fact.PROV_RECORDED).text);
    }
    @Test public void normalPositiveNegativeAndEqualDifferencesKeepTheirFormatting() {
        assertEquals("+7", difference(3L, 10L, Fact.PROV_RECORDED).text);
        assertEquals("-7", difference(10L, 3L, Fact.PROV_RECORDED).text);
        assertEquals("0", difference(3L, 3L, Fact.PROV_RECORDED).text);
    }
    @Test public void unknownOperandsNeverBecomeAnInventedDifference() {
        assertEquals("", difference(null, 10L, Fact.PROV_RECORDED).text);
        assertEquals("", difference(10L, null, Fact.PROV_RECORDED).text);
    }
}
