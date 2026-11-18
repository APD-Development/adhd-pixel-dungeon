package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.report.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.util.*;
import static org.junit.Assert.*;
public class CustomRuleQueryIntegrationTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    private Row row(int count,String id) throws Exception {
        File file=new File(tmp.getRoot(),id+".apdlog");
        RunLogWriter w=RunLogWriter.create(file,TelemetryVersion.CURRENT,id,"test",1);
        LifecycleEvents.RunStart s=new LifecycleEvents.RunStart();s.runId=id;
        s.rulesetId="rules";s.heroClass="WARRIOR";
        if(count>0){CustomDungeonRules selected=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,count);s.customRules=selected.preferenceText();com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomScoreSnapshot.forNewRun(selected).recordTo(s);}
        assertTrue(LifecycleEvents.emitRunStart(w,0,false,s));
        LifecycleEvents.RunEnd end=new LifecycleEvents.RunEnd();end.score=523818;if(count==15){end.customAdjustedKnown=true;end.customAdjustedScore=end.score;}
        assertTrue(LifecycleEvents.emitRunEnd(w,1,end));assertTrue(w.close());
        return RowBuilders.fromLog(RunLogReader.read(file),id).of(Datasets.RUNS).get(0);
    }
    @Test public void realRecordedSettingsAreFilterableAndHistoricalUnknownRemainsSeparate() throws Exception {
        DatasetDef def=Datasets.runs();
        List<Row> rows=Arrays.asList(row(1,"minimum"),row(15,"neutral"),row(60,"maximum"),row(0,"historical"));
        Dataset ds=new Dataset(def,rows,Collections.emptyList());
        Query q=Query.of(Datasets.RUNS).withScope(new Scope(Scope.Rules.ALL,"",true,true,true,true));
        String key=CustomRuleFacts.key(CustomDungeonRules.Slider.UPGRADE_SCROLLS.name());
        for(int n:new int[]{1,15,60}) {
            QueryResult r=QueryEngine.run(ds,q.addFilter(Filter.num(key,Op.EQ,n)),"rules");
            assertEquals(1,r.rows.size());assertEquals(n,r.rows.get(0).num(def.indexOf(key)),0);
        }
        QueryResult unknown=QueryEngine.run(ds,q.addFilter(Filter.unknown(key)),"rules");
        assertEquals(1,unknown.rows.size());assertEquals("historical",unknown.rows.get(0).runId);
        assertEquals("6.67",CellFormatter.cellOpen(rows.get(0),def.column(CustomRuleFacts.SCROLL_PERCENT)).text);
    }
    @Test public void allSettingsAreAvailableToReportsAndExports() throws Exception {
        DatasetDef def=Datasets.runs();
        Row row=row(60,"maximum");
        List<String> cols=new ArrayList<>();
        for(CustomDungeonRules.Slider s:CustomDungeonRules.Slider.values()) cols.add(CustomRuleFacts.key(s.name()));
        cols.add(CustomRuleFacts.SCROLL_PERCENT);cols.add(CustomRuleFacts.SCORE_STATUS);cols.add(CustomRuleFacts.ORIGINAL_SCORE);cols.add(CustomRuleFacts.MULTIPLIER);cols.add(CustomRuleFacts.CALCULATION_VERSION);cols.add(CustomRuleFacts.ADJUSTED_SCORE);cols.add(CustomRuleFacts.MULTIPLIER_EXACT);cols.add(CustomRuleFacts.SCORING_RECORD_VERSION);cols.add(CustomRuleFacts.SCROLL_PERCENT_NUMERATOR);cols.add(CustomRuleFacts.SCROLL_PERCENT_DENOMINATOR);
        Query q=Query.of(Datasets.RUNS).withColumns(cols);
        QueryResult result=QueryEngine.run(new Dataset(def,Collections.singletonList(row),Collections.emptyList()),q,"rules");
        Report report=ReportBuilder.view(result,"Custom rules","",0,"test");
        String csv=ReportRenderer.csv(report);
        assertTrue(csv,csv.contains("Upgrade scrolls count"));
        assertTrue(csv,csv.contains("400"));assertTrue(csv,csv.contains("experimental provisional"));
        for(String col:cols)assertTrue(def.indexOf(col)>=0);
        assertEquals(523818,row.num(def.indexOf(FactKeys.SCORE)),0);assertEquals(523818,row.num(def.indexOf(CustomRuleFacts.ORIGINAL_SCORE)),0);assertTrue(row.isKnown(def.indexOf(CustomRuleFacts.MULTIPLIER)));
    }
}
