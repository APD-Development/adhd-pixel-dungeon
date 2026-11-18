package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReportRunIdentityTest {
    private static Row row(DatasetDef d,String id,String run,boolean finished) {
        Row r=new Row(id,null,run,d.size()); r.finished=finished;r.ruleset="rules";
        r.setText(0,run,Fact.PROV_RECORDED);r.setText(1,id,Fact.PROV_RECORDED);
        return r;
    }
    private static Catalog catalog() {
        assertTrue(StatsStore.isValidRunId("Run-A"));assertTrue(StatsStore.isValidRunId("run-a"));
        Catalog cat=new Catalog();
        DatasetDef runs=new DatasetDef(Datasets.RUNS,"Runs","")
            .add(ColumnDef.text("run.id","Run id")).add(ColumnDef.text("label","Label").shown());
        cat.add(new Dataset(runs,Arrays.asList(row(runs,"upper","Run-A",true),row(runs,"lower","run-a",false)),Collections.emptyList()));
        for(String[] spec:new String[][]{{Datasets.ITEMS,"item.run"},{Datasets.ENEMIES,"enemy.run"},{Datasets.DEATHS,"death.run"}}) {
            DatasetDef d=new DatasetDef(spec[0],spec[0],"").add(ColumnDef.text(spec[1],"Run id"))
                .add(ColumnDef.text("label","Label").shown());
            Row unknown=new Row("unknown",null,"Run-A",d.size());unknown.finished=true;unknown.ruleset="rules";
            unknown.setText(1,"unattributed",Fact.PROV_RECORDED);
            cat.add(new Dataset(d,Arrays.asList(row(d,"upper-only", "Run-A",true),row(d,"lower-only","run-a",false),unknown),Collections.emptyList()));
        }
        return cat;
    }
    @Test public void individualRunReportNeverIncludesAnotherCaseDistinctRun() {
        Catalog cat=catalog();
        for(Row run:cat.dataset(Datasets.RUNS).rows) {
            Report report=ReportBuilder.run(cat,run,true,"rules",0,"test");int tables=0;
            for(Report.Block b:report.blocks)if(b instanceof Report.Table) {
                Report.Table t=(Report.Table)b;tables++;
                assertEquals("Each section must contain only the selected run",1,t.rows.size());
                assertEquals(run.runId.equals("Run-A")?"upper-only":"lower-only",t.rows.get(0)[0].text);
            }
            assertEquals("Items, enemies and deaths all retain separate identities",3,tables);
            String expected=run.runId.equals("Run-A")?"upper-only":"lower-only";
            String rejected=run.runId.equals("Run-A")?"lower-only":"upper-only";
            for(String out:new String[]{ReportRenderer.markdown(report),ReportRenderer.text(report),ReportRenderer.html(report),ReportRenderer.csv(report)}) {
                assertTrue(out,out.contains(expected));assertFalse(out,out.contains(rejected));assertFalse(out,out.contains("unattributed"));
            }
        }
    }
    @Test public void ordinaryPlayerTextSearchRemainsCaseInsensitive() {
        Catalog cat=catalog();Dataset ds=cat.dataset(Datasets.ITEMS);
        Query q=Query.of(ds.def.id).withScope(Scope.DEFAULT.withRules(Scope.Rules.ALL,"").withUnfinished(true))
                .addFilter(Filter.text("item.run",Op.EQ,"run-a"));
        QueryResult result=QueryEngine.run(ds,q,"rules");assertTrue(result.problems.toString(),result.problems.isEmpty());
        assertEquals(2,result.rows.size());
    }
}
