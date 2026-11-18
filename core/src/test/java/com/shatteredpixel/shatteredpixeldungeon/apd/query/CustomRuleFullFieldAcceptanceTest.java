package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomScoreSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.apd.report.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.util.*;
import static org.junit.Assert.*;

/** Acceptance of the entire recorded slider configuration through player-facing query/report paths. */
public class CustomRuleFullFieldAcceptanceTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private CustomDungeonRules bounds(boolean maximum) {
        CustomDungeonRules rules = CustomDungeonRules.defaults();
        for (CustomDungeonRules.Slider slider : CustomDungeonRules.Slider.values())
            rules = rules.with(slider, maximum ? slider.max : slider.min);
        return rules;
    }

    private Row recorded(String id, CustomDungeonRules rules) throws Exception {
        File file = new File(tmp.getRoot(), id + ".apdlog");
        RunLogWriter writer = RunLogWriter.create(file, TelemetryVersion.CURRENT, id, "acceptance", 1);
        LifecycleEvents.RunStart start = new LifecycleEvents.RunStart();
        start.runId = id; start.rulesetId = "rules"; start.heroClass = "MAGE";
        if (rules != null) {
            start.customRules = rules.preferenceText();
            CustomScoreSnapshot.forNewRun(rules).recordTo(start);
        }
        assertTrue(LifecycleEvents.emitRunStart(writer, 0, false, start));
        LifecycleEvents.RunEnd end = new LifecycleEvents.RunEnd();
        end.score = 523818;
        if (rules != null && rules.isNeutral()) {
            end.customAdjustedKnown = true; end.customAdjustedScore = end.score;
        }
        assertTrue(LifecycleEvents.emitRunEnd(writer, 1, end));
        assertTrue(writer.close());
        return RowBuilders.fromLog(RunLogReader.read(file), id).of(Datasets.RUNS).get(0);
    }

    private Query all() {
        return Query.of(Datasets.RUNS).withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
    }

    private void matches(Dataset data, Query base, Filter filter, String... ids) {
        QueryResult result = QueryEngine.run(data, base.addFilter(filter), "rules");
        assertTrue(result.problems.toString(), result.problems.isEmpty());
        Set<String> actual = new TreeSet<>();
        for (Row row : result.rows) actual.add(row.runId);
        assertEquals(filter.toString(), new TreeSet<>(Arrays.asList(ids)), actual);
    }

    @Test public void everyRecordedSliderSupportsEveryNumericOperatorAndCombinedFilters() throws Exception {
        DatasetDef def = Datasets.runs();
        Dataset data = new Dataset(def, Arrays.asList(recorded("minimum", bounds(false)),
                recorded("neutral", CustomDungeonRules.defaults()), recorded("maximum", bounds(true)),
                recorded("historical", null)), Collections.emptyList());
        Query query = all();
        for (CustomDungeonRules.Slider slider : CustomDungeonRules.Slider.values()) {
            String key = CustomRuleFacts.key(slider.name());
            assertEquals(ColumnDef.Type.NUM, def.column(key).type);
            assertEquals(9, Op.ops(def.column(key).type).length);
            matches(data, query, Filter.num(key, Op.EQ, slider.min), "minimum");
            matches(data, query, Filter.num(key, Op.NE, slider.defaultValue()), "minimum", "maximum");
            matches(data, query, Filter.num(key, Op.LT, slider.defaultValue()), "minimum");
            matches(data, query, Filter.num(key, Op.LE, slider.defaultValue()), "minimum", "neutral");
            matches(data, query, Filter.num(key, Op.GT, slider.defaultValue()), "maximum");
            matches(data, query, Filter.num(key, Op.GE, slider.defaultValue()), "neutral", "maximum");
            matches(data, query, Filter.num(key, Op.BETWEEN, slider.min, slider.defaultValue()), "minimum", "neutral");
            matches(data, query, Filter.known(key), "minimum", "neutral", "maximum");
            matches(data, query, Filter.unknown(key), "historical");
        }
        String hp = CustomRuleFacts.key(CustomDungeonRules.Slider.ENEMY_HP.name());
        String upgrades = CustomRuleFacts.key(CustomDungeonRules.Slider.UPGRADE_SCROLLS.name());
        matches(data, query.addFilter(Filter.num(hp, Op.GT, 100)), Filter.num(upgrades, Op.EQ, 60), "maximum");
        matches(data, query.addFilter(Filter.num(hp, Op.GT, 100)), Filter.num(upgrades, Op.EQ, 1));
    }

    @Test public void allEighteenValuesSurviveRunDetailsComparisonAndEveryExportFormat() throws Exception {
        DatasetDef def = Datasets.runs();
        Row low = recorded("minimum", bounds(false)), high = recorded("maximum", bounds(true));
        Dataset data = new Dataset(def, Arrays.asList(low, high), Collections.emptyList());
        Catalog catalog = new Catalog().add(data);
        List<String> columns = new ArrayList<>();
        for (CustomDungeonRules.Slider slider : CustomDungeonRules.Slider.values()) columns.add(CustomRuleFacts.key(slider.name()));
        Report view = ReportBuilder.view(QueryEngine.run(data, all().withColumns(columns), "rules"), "Custom rules", "", 0, "test");
        Report comparison = ReportBuilder.compareRuns(catalog, low, high, false, 0, "test");
        Map<String, Report.Cell[]> compared = new HashMap<>();
        for (Report.Cell[] row : comparison.firstTable().rows) compared.put(row[0].text, row);
        Report run = ReportBuilder.run(catalog, high, false, "rules", 0, "test");
        Map<String, String> details = new HashMap<>();
        for (Report.Block block : run.blocks) if (block instanceof Report.Facts)
            for (Report.Fact fact : ((Report.Facts) block).facts) if (fact.value != null) details.put(fact.label, fact.value.text);
        String[] exports = {ReportRenderer.markdown(view), ReportRenderer.text(view), ReportRenderer.html(view), ReportRenderer.csv(view)};
        for (CustomDungeonRules.Slider slider : CustomDungeonRules.Slider.values()) {
            ColumnDef column = def.column(CustomRuleFacts.key(slider.name()));
            int index = columns.indexOf(column.id);
            assertEquals(Integer.toString(slider.min), view.firstTable().rows.get(0)[index].text);
            assertEquals(Integer.toString(slider.max), view.firstTable().rows.get(1)[index].text);
            assertEquals(Integer.toString(slider.max), details.get(column.label));
            Report.Cell[] pair = compared.get(column.label);
            assertNotNull(column.label, pair);
            assertEquals(Integer.toString(slider.min), pair[1].text);
            assertEquals(Integer.toString(slider.max), pair[2].text);
            assertEquals("+" + (slider.max - slider.min), pair[3].text);
            for (int format = 0; format < exports.length; format++) assertTrue(column.label + " format " + format, exports[format].contains(column.label));
        }
        for (Row row : data.rows) {
            assertEquals(523818, row.num(def.indexOf(FactKeys.SCORE)));
            assertTrue("Exact provisional multiplier is recorded", row.isKnown(def.indexOf(CustomRuleFacts.MULTIPLIER)));
            assertFalse(row.isKnown(def.indexOf(CustomRuleFacts.ADJUSTED_SCORE)));
        }
    }
}
