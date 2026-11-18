package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.util.ArrayList;
import java.io.File;
import static org.junit.Assert.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
public class UimsdHomePresentationTest {
 @Rule public TemporaryFolder tmp = new TemporaryFolder();
 @Test public void zeroAbandonedRunsRemainVisibleAndOutcomeDrilldownsMatchRecords() throws Exception {
  File root = com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData.sampleRootFor(new File(tmp.newFolder("data"),"apd")); Ruleset rules = Ruleset.of(30,0);
  DemoGenerator.generateSample(root,rules,1800000000000L);
  Catalog original = new DatasetStore(root).load(), cat = new Catalog();
  for(Dataset ds : original.datasets()){
   if(!ds.def.id.equals(Datasets.RUNS)) {cat.add(ds);continue;}
   ArrayList<Row> keep = new ArrayList<>();
   for(Row row : ds.rows) if(row.isKnown(ds.def.indexOf(FactKeys.OUTCOME))&&row.num(ds.def.indexOf(FactKeys.OUTCOME))!=2)keep.add(row);
   cat.add(new Dataset(ds.def,keep,ds.runs));
  }
  for(ViewDef view : original.views())cat.add(view);
  cat.rulesets.addAll(original.rulesets);
  UimsdHome home=UimsdHome.build(cat,rules.id,true);
  UimsdHome.Line abandoned=null;
  for(UimsdHome.Line line:home.card("runs").lines)if(line.label.equals("Abandoned"))abandoned=line;
  assertNotNull(abandoned);assertEquals("0",abandoned.value);assertEquals(CellText.EXACT,abandoned.state);
  for(int outcome=0;outcome<3;outcome++){
   QueryResult result=QueryEngine.run(cat.dataset(Datasets.RUNS),UimsdHomeNavigation.outcome("!outcome:"+outcome,true),rules.id);
   for(Row row:result.rows)assertEquals(outcome,row.num(result.def.indexOf(FactKeys.OUTCOME)));
   if(outcome==2)assertTrue(result.rows.isEmpty());
  }
 }
 @Test public void homeScopeExcludesUnfinishedRunsAndCountsOtherFinishedRulesExactly() throws Exception {
  File root=tmp.newFolder("scope"); Ruleset current=Ruleset.of(30,0), other=Ruleset.of(40,0);
  new SyntheticLog(root,"current-finished",com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion.CURRENT,100L)
    .start("run-current",0,"WARRIOR",current.id,0,30,false).enter(1).end(0,"Rat",1,1,10).close(true);
  new SyntheticLog(root,"current-open",com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion.CURRENT,200L)
    .start("run-open",0,"MAGE",current.id,0,30,false).enter(1).close(true);
  new SyntheticLog(root,"other-finished",com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion.CURRENT,300L)
    .start("run-other",0,"ROGUE",other.id,0,40,false).enter(1).end(2,"",1,1,20).close(true);
  Catalog catalog=new DatasetStore(root).load();
  UimsdHome mine=UimsdHome.build(catalog,current.id,false);
  assertEquals(1,mine.runs);assertEquals(1,mine.runsElsewhere);
  UimsdHome all=UimsdHome.build(catalog,current.id,true);
  assertEquals(2,all.runs);assertEquals(0,all.runsElsewhere);
  UimsdHome.Line finished=null;
  for(UimsdHome.Line line:all.card("runs").lines)if(line.label.equals("Finished runs"))finished=line;
  assertNotNull(finished);assertEquals("2",finished.value);assertEquals(CellText.EXACT,finished.state);
  assertFalse("unfinished class must not appear anywhere on Home",all.text().contains("MAGE"));
 }

}
