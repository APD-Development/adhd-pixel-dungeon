package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

//Synthetic statistics stress coverage, not an installed-mod or physical-device FPS test.
public class ExpandedContentStatsTest {
    @Test(timeout = 15000) public void thousandsOfDistinctContentTypesKeepAccurateGroupsAndBoundedPages() {
        DatasetDef def = def();
        final int types = 2000, perType = 25;
        List<Row> records = new ArrayList<>(types * perType);
        for (int type = 0; type < types; type++) {
            for (int occurrence = 0; occurrence < perType; occurrence++) {
                records.add(row(def, type + "-" + occurrence, "community-item-" + type,
                        (long) occurrence, (long) (type % 3), null, true, "rules"));
            }
        }
        Dataset data = dataset(def, records);
        Query query = Query.of("things").withGroup(new GroupBy(Arrays.asList("name"),
                Arrays.asList(Aggregate.count(), Aggregate.of(Aggregate.Fn.SUM, "score"))));
        long started = System.nanoTime();
        QueryResult result = QueryEngine.run(data, query, "rules");
        long elapsedMillis = (System.nanoTime() - started) / 1000000;
        assertTrue(result.problems.toString(), result.problems.isEmpty());
        assertEquals(types, result.groups.size());
        Set<String> allTypes = new HashSet<>();
        for (GroupRow group : result.groups) {
            assertTrue("distinct names never collapse", allTypes.add(group.sample.text(NAME)));
            assertEquals(perType, group.n);
            assertEquals(25.0, group.aggregates.get(0).value, 0.0);
            assertEquals("0 through 24 sum independently to 300", 300.0, group.aggregates.get(1).value, 0.0);
        }
        Set<String> pageTypes = new HashSet<>();
        for (int page = 0; page < 40; page++) {
            ResultTable table = ResultTable.build(result, page, 50);
            assertEquals(types, table.total);
            assertEquals(40, table.pageCount());
            assertEquals(50, table.lines.size());
            assertEquals("geometry builds only the current page", 50, TableGeometry.of(table).rows);
            for (ResultTable.Line line : table.lines) assertTrue("no missing/duplicate page entries",
                    pageTypes.add(line.group.sample.text(NAME)));
        }
        assertEquals(allTypes, pageTypes);
        assertEquals("past-end requests clamp to the last page", 39, ResultTable.build(result, Integer.MAX_VALUE, 50).page());
        assertEquals(types * perType, records.size());
        System.out.println("APD_EXPANDED_STATS rows=" + records.size() + " types=" + types
                + " groupingMillis=" + elapsedMillis + " pageRows=50");
    }
}
