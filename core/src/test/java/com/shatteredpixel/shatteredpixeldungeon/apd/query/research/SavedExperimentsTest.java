package com.shatteredpixel.shatteredpixeldungeon.apd.query.research;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class SavedExperimentsTest {
    private File file()throws IOException{return SavedExperiments.file(Files.createTempDirectory("apd-experiment-").toFile());}
    private SavedExperiments.Experiment create(File file,ResearchFixture f,String name)throws IOException{
        return SavedExperiments.create(file,name,"Does score differ?",f.forClass("mage"),f.forClass("warrior"),"result.score","rules",123);
    }
    @Test public void definitionsRoundTripSeparatelyFromViewsAndKeepExactQueries()throws Exception{
        ResearchFixture f=new ResearchFixture();File file=file();byte[] original={1,2,3};File views=new File(file.getParentFile(),SavedViews.FILE_NAME);Files.write(views.toPath(),original);
        com.watabou.utils.Random.pushGenerator(98127L);long expectedRandom=com.watabou.utils.Random.Long();com.watabou.utils.Random.popGenerator();
        SavedExperiments.Experiment e;
        com.watabou.utils.Random.pushGenerator(98127L);try{e=create(file,f,"Armor comparison");assertEquals("Saving definitions must not consume gameplay RNG",expectedRandom,com.watabou.utils.Random.Long());}finally{com.watabou.utils.Random.popGenerator();}
        SavedExperiments.Experiment loaded=SavedExperiments.load(file).get(0);
        assertEquals(e.id,loaded.id);assertEquals(e.a,loaded.a);assertEquals(e.b,loaded.b);assertEquals("Does score differ?",loaded.question);assertEquals("result.score",loaded.measure);assertEquals(123,loaded.createdAtMillis);assertArrayEquals(original,Files.readAllBytes(views.toPath()));
        try{create(file,f,"ARMOR COMPARISON");fail();}catch(IOException expected){}assertEquals(1,SavedExperiments.load(file).size());
    }
    @Test public void invalidNamesQuestionsAndOversizedFilesKeepOriginalBytes()throws Exception{
        ResearchFixture f=new ResearchFixture();File file=file();create(file,f,"Original");byte[] original=Files.readAllBytes(file.toPath());
        for(String bad:Arrays.asList("","\n",String.join("",Collections.nCopies(61,"x")))){
            try{create(file,f,bad);fail();}catch(IOException expected){}assertArrayEquals(original,Files.readAllBytes(file.toPath()));
        }
        try{SavedExperiments.create(file,"Question","line\nbreak",f.query(),f.query(),"result.score","rules",1);fail();}catch(IOException expected){}assertArrayEquals(original,Files.readAllBytes(file.toPath()));
        byte[] large=new byte[1048577];Files.write(file.toPath(),large);try{SavedExperiments.load(file);fail();}catch(IOException expected){}assertArrayEquals(large,Files.readAllBytes(file.toPath()));
    }
    @Test public void currentRulesAreFrozenAndGroupingNeverLimitsTheFuturePopulation()throws Exception{
        ResearchFixture f=new ResearchFixture();f.add("a","mage",0,10);f.add("b","warrior",0,30);
        Query a=f.forClass("mage").withScope(Scope.DEFAULT).withGroup(new GroupBy(Collections.singletonList("run.heroClass"),Collections.singletonList(Aggregate.count()))).withMinSample(999);
        SavedExperiments.Experiment e=SavedExperiments.create(file(),"Rules fixed","Question",a,f.forClass("warrior"),"result.score","rules",1);
        assertNull(e.a.group);assertEquals(0,e.a.minSample);assertEquals(Scope.Rules.SPECIFIC,e.a.scope.rules);assertEquals("rules",e.a.scope.ruleset);
        assertEquals(1,e.evaluate(f.catalog(),"different-new-run-rules",true).comparison.a.n);
    }
    @Test public void newMatchingRunsAreReevaluatedWithoutEditingTheDefinition()throws Exception{
        ResearchFixture f=new ResearchFixture();f.add("a","mage",0,10);f.add("b","warrior",0,30);File file=file();SavedExperiments.Experiment e=create(file,f,"Future runs");byte[] definition=Files.readAllBytes(file.toPath());
        assertEquals(-20,e.evaluate(f.catalog(),"rules",true).comparison.difference,0);
        f.add("future","mage",1,50);f.add("outside","rogue",0,999);
        SavedExperiments.Evaluation now=SavedExperiments.load(file).get(0).evaluate(f.catalog(),"rules",true);assertEquals(2,now.comparison.a.n);assertEquals(1,now.comparison.b.n);assertEquals(0,now.comparison.difference,0);assertArrayEquals(definition,Files.readAllBytes(file.toPath()));
        long started=System.nanoTime();for(int i=0;i<100;i++)assertEquals(2,e.evaluate(f.catalog(),"rules",true).comparison.a.n);
        System.out.println("UIMSD experiment fixture: definition_bytes="+definition.length+", evaluations=100, rows="+f.rows.size()+", elapsed_ns="+(System.nanoTime()-started));
        assertArrayEquals(definition,Files.readAllBytes(file.toPath()));
    }
    @Test public void writerCloseReloadThenFutureVictoryIncludesAscensionAndPreservesAbandonment()throws Exception{
        File root=Files.createTempDirectory("apd-experiment-recording-").toFile();record(root,"win",1,10,false);record(root,"dead",0,30,false);record(root,"abandoned",2,999,false);
        Query all=Query.of(Datasets.RUNS).withScope(new Scope(Scope.Rules.ALL,"",false,false,false,false));
        File file=SavedExperiments.file(root);SavedExperiments.Experiment e=SavedExperiments.create(file,"Recorded question","Scores by outcome",all.addFilter(Filter.num("result.outcome",Op.EQ,1)),all.addFilter(Filter.num("result.outcome",Op.EQ,0)),"result.score","rules",1);
        assertEquals(-20,e.evaluate(new DatasetStore(root).load(),"rules",false).comparison.difference,0);
        byte[] raw=Files.readAllBytes(new File(root,"runs/abandoned.apdlog").toPath());record(root,"future-ascended",1,50,true);
        SavedExperiments.Evaluation result=SavedExperiments.load(file).get(0).evaluate(new DatasetStore(root).load(),"rules",false);
        assertEquals(2,result.comparison.a.n);assertEquals(1,result.comparison.b.n);assertEquals(0,result.comparison.difference,0);
        assertArrayEquals(raw,Files.readAllBytes(new File(root,"runs/abandoned.apdlog").toPath()));assertFalse(result.comparison.intervalKnown);
    }
    private void record(File root,String id,int outcome,long score,boolean ascended)throws Exception{
        RunLogWriter w=RunLogWriter.create(new File(root,"runs/"+id+".apdlog"),TelemetryVersion.CURRENT,id,"test",1);
        LifecycleEvents.RunStart start=new LifecycleEvents.RunStart();start.runId=id;start.heroClass="WARRIOR";start.rulesetId="rules";start.apdVersionName="test";LifecycleEvents.emitRunStart(w,1,false,start);
        LifecycleEvents.RunEnd end=new LifecycleEvents.RunEnd();end.outcome=outcome;end.score=score;end.turns=10;end.ascendedKnown=true;end.ascended=ascended;LifecycleEvents.emitRunEnd(w,10,end);assertTrue(w.close(true));
    }
    @Test public void missingUncertainAndOverlappingValuesNeverInventConfidence()throws Exception{
        ResearchFixture f=new ResearchFixture();f.add("a","mage",0,10);Row unknown=f.add("unknown","mage",0,999);unknown.clear(f.def.indexOf("result.score"));Row partial=f.add("partial","mage",0,999);f.num(partial,"result.score",999);partial.setNum(f.def.indexOf("result.score"),999,Fact.PROV_INCOMPLETE);f.add("b","warrior",0,30);
        SavedExperiments.Evaluation result=create(file(),f,"Unknowns").evaluate(f.catalog(),"rules",true);assertEquals(1,result.comparison.a.n);assertEquals(2,result.comparison.a.excluded);assertEquals(-20,result.comparison.difference,0);assertFalse(result.comparison.intervalKnown);
        SavedExperiments.Experiment overlap=SavedExperiments.create(file(),"Overlap","Question",f.query(),f.query(),"result.score","rules",1);assertTrue(overlap.evaluate(f.catalog(),"rules",true).comparison.overlappingRuns>0);assertFalse(overlap.evaluate(f.catalog(),"rules",true).comparison.intervalKnown);
    }
    @Test public void unavailableSchemaAndDisabledDisclosureFailClosed()throws Exception{
        ResearchFixture f=new ResearchFixture();f.add("a","mage",0,10);f.add("b","warrior",0,30);SavedExperiments.Experiment hidden=create(file(),f,"Disclosure");assertNull(hidden.evaluate(f.catalog(),"rules",false).comparison);assertFalse(hidden.evaluate(f.catalog(),"rules",false).problem.isEmpty());
        SavedExperiments.Experiment removed=SavedExperiments.create(file(),"Removed","Question",f.query().addFilter(Filter.num("removed",Op.GT,2)),f.query(),"result.score","rules",1);assertNull(removed.evaluate(f.catalog(),"rules",true).comparison);
        SavedExperiments.Experiment metric=SavedExperiments.create(file(),"Metric","Question",f.query(),f.query(),"removed","rules",1);assertNull(metric.evaluate(f.catalog(),"rules",true).comparison);
    }
    @Test public void damagedOrFutureFilesAreNotOverwrittenByAnyMutation()throws Exception{
        ResearchFixture f=new ResearchFixture();File file=file();create(file,f,"Original");Properties p=new Properties();try(InputStream in=new FileInputStream(file)){p.load(in);}p.setProperty("0.name","Tampered");try(OutputStream out=new FileOutputStream(file)){p.store(out,"");}
        byte[] damaged=Files.readAllBytes(file.toPath());try{create(file,f,"Next");fail();}catch(IOException expected){}try{SavedExperiments.delete(file,"unknown");fail();}catch(IOException expected){}assertArrayEquals(damaged,Files.readAllBytes(file.toPath()));
        p.setProperty("version","999");try(OutputStream out=new FileOutputStream(file)){p.store(out,"");}byte[] future=Files.readAllBytes(file.toPath());try{SavedExperiments.rename(file,"unknown","Name");fail();}catch(IOException expected){}assertArrayEquals(future,Files.readAllBytes(file.toPath()));
    }
    @Test public void renameDeleteAndConcurrentCreatorsPreserveOtherDefinitions()throws Exception{
        ResearchFixture f=new ResearchFixture();File file=file();SavedExperiments.Experiment first=create(file,f,"First");SavedExperiments.Experiment renamed=SavedExperiments.rename(file,first.id,"Renamed");assertEquals(first.question,renamed.question);assertEquals(first.a,renamed.a);assertEquals(first.createdAtMillis,renamed.createdAtMillis);
        ExecutorService pool=Executors.newFixedThreadPool(2);try{Future<?> a=pool.submit(()->{try{create(file,f,"A");}catch(IOException e){throw new RuntimeException(e);}}),b=pool.submit(()->{try{create(file,f,"B");}catch(IOException e){throw new RuntimeException(e);}});a.get(5,TimeUnit.SECONDS);b.get(5,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
        assertEquals(3,SavedExperiments.load(file).size());SavedExperiments.delete(file,first.id);assertEquals(2,SavedExperiments.load(file).size());
    }
    @Test public void reportUsesTheSharedEvidenceAndShowsVersionRuleAndUnknownConditions()throws Exception{
        ResearchFixture f=new ResearchFixture();for(int i=0;i<20;i++){f.add("a"+i,"mage",0,10+i);Row b=f.add("b"+i,"warrior",1,30+i);f.text(b,"run.apdVersion","version-2");}
        SavedExperiments.Experiment e=create(file(),f,"Version comparison");SavedExperiments.Evaluation result=e.evaluate(f.catalog(),"rules",true);assertFalse(result.comparison.intervalKnown);
        ResearchDocument report=ExperimentReport.build(e,result);assertEquals(e.name,report.title);assertTrue(report.notes.get(0).contains(e.question));assertEquals(5,report.cards.size());assertEquals(20,report.cards.get(0).evidence.get(0).rows.size());
        assertTrue(report.cards.get(4).detail.contains("version-2"));assertTrue(report.cards.get(3).detail.contains("unknown/uncertain="));assertTrue(report.cards.get(1).detail.contains(QueryCodec.encode(e.a)));
    }
}
