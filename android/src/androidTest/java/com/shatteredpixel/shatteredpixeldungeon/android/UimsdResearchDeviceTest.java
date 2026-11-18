package com.shatteredpixel.shatteredpixeldungeon.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.*;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.*;
import com.watabou.utils.Point;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** Actual menu/pointer acceptance for every research capability, with visible evidence drill-down. */
@RunWith(AndroidJUnit4.class)
public class UimsdResearchDeviceTest {
    private static void recordedExperimentRun(java.io.File root,String id,int outcome,long score,boolean ascended)throws Exception{
        RunLogWriter w=RunLogWriter.create(new java.io.File(root,"runs/"+id+".apdlog"),TelemetryVersion.CURRENT,id,"test",1);
        LifecycleEvents.RunStart start=new LifecycleEvents.RunStart();start.runId=id;start.heroClass="WARRIOR";start.rulesetId="rules";start.apdVersionName="experiment-test";LifecycleEvents.emitRunStart(w,1,false,start);
        LifecycleEvents.RunEnd end=new LifecycleEvents.RunEnd();end.outcome=outcome;end.score=score;end.turns=10;end.ascendedKnown=true;end.ascended=ascended;LifecycleEvents.emitRunEnd(w,10,end);assertTrue(w.close(true));
    }
    private static UimsdModel readExperimentHistory(java.io.File root)throws Exception{
        UimsdModel m=new UimsdModel("rules","Rules");m.load(root);m.awaitLoad(15000);assertEquals(UimsdModel.State.READY,m.state());return m;
    }
    @SuppressWarnings("unchecked")
    private static void typeAndConfirm(String text,String button)throws Exception{
        AtomicReference<Point> where=new AtomicReference<>();render(()->{try{
            WndUimsdTextInput input=top(WndUimsdTextInput.class);assertNotNull("Experiment naming input",input);
            Object box=field(input,"textBox");java.lang.reflect.Method set=box.getClass().getMethod("setText",String.class);set.setAccessible(true);set.invoke(box,text);
            for(Object child:(List<?>)field(input,"members"))if(child instanceof com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton){
                com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton b=(com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton)child;
                if(button.equals(((com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock)field(b,"text")).text()))where.set(b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2));
            }
            assertNotNull("Native confirmation button "+button,where.get());
        }catch(Exception e){throw new RuntimeException(e);}});
        Point p=where.get();PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,98,PointerEvent.Type.DOWN,PointerEvent.LEFT));Thread.sleep(140);
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,98,PointerEvent.Type.UP,PointerEvent.LEFT));Thread.sleep(400);
    }
    private static void metricValue(String label,String value)throws Exception{
        await(label);render(()->{try{boolean found=false;for(WndUimsdList.Entry e:entries())if(e.label.equals(label)){assertEquals(value,e.detail);found=true;}assertTrue(found);}catch(Exception e){throw new RuntimeException(e);}});
    }
    @Test public void namedExperimentPersistsReevaluatesFutureRunsAndKeepsExactEvidence()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            render(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));Thread.sleep(650);
            java.io.File root=new java.io.File(androidx.test.core.app.ApplicationProvider.getApplicationContext().getCacheDir(),"uimsd-experiment-"+System.nanoTime());
            recordedExperimentRun(root,"win",LifecycleEvents.OUTCOME_WON,10,false);
            recordedExperimentRun(root,"dead",LifecycleEvents.OUTCOME_DIED,30,false);
            recordedExperimentRun(root,"abandoned",LifecycleEvents.OUTCOME_ABANDONED,999,false);
            byte[] original=java.nio.file.Files.readAllBytes(new java.io.File(root,"runs/abandoned.apdlog").toPath());
            Query all=Query.of(Datasets.RUNS).withScope(new Scope(Scope.Rules.ALL,"",false,false,false,false));
            UimsdModel first=readExperimentHistory(root);
            render(()->{first.showQuery(all.addFilter(Filter.num("result.outcome",Op.EQ,LifecycleEvents.OUTCOME_WON)));WndUimsdResearch.studyMenu(first);});
            tap("Capture current query as population A");closeWindows();
            render(()->{first.showQuery(all.addFilter(Filter.num("result.outcome",Op.EQ,LifecycleEvents.OUTCOME_DIED)));WndUimsdResearch.studyMenu(first);});
            tap("Capture current query as population B");closeWindows();
            render(()->WndUimsdResearch.studyMenu(first));tap("Save named experiment");tap("Score");typeAndConfirm("Scores by outcome","Save");await("View current results");
            java.io.File file=com.shatteredpixel.shatteredpixeldungeon.apd.query.research.SavedExperiments.file(root);
            byte[] definition=java.nio.file.Files.readAllBytes(file.toPath());
            assertEquals(1,com.shatteredpixel.shatteredpixeldungeon.apd.query.research.SavedExperiments.load(file).size());
            closeWindows();UimsdModel reopened=readExperimentHistory(root);render(()->WndUimsdResearch.savedExperiments(reopened));tap("Scores by outcome");tap("View current results");metricValue("Score: A minus B","-20.00");
            PlayRunDeviceTest.visibilityScreenshot("uimsd-experiment-initial.png");
            tap("Score: A minus B");tap("Population A contributors");render(()->{try{assertEquals(1,entries().size());}catch(Exception e){throw new RuntimeException(e);}});tap(0);
            render(()->{try{ResultTable.Line line=(ResultTable.Line)field(top(WndUimsdRow.class),"selected");assertEquals("win",line.row.runId);assertTrue(reopened.canGoBack());}catch(Exception e){throw new RuntimeException(e);}});
            closeWindows();recordedExperimentRun(root,"future-ascended",LifecycleEvents.OUTCOME_WON,50,true);
            UimsdModel future=readExperimentHistory(root);render(()->WndUimsdResearch.savedExperiments(future));tap("Scores by outcome");tap("View current results");metricValue("Score: A minus B","0.00");
            PlayRunDeviceTest.visibilityScreenshot("uimsd-experiment-future.png");
            tap("Score: A minus B");tap("Population A contributors");render(()->{try{assertEquals("New qualifying ascension joins without changing the definition",2,entries().size());}catch(Exception e){throw new RuntimeException(e);}});
            assertArrayEquals(definition,java.nio.file.Files.readAllBytes(file.toPath()));assertArrayEquals(original,java.nio.file.Files.readAllBytes(new java.io.File(root,"runs/abandoned.apdlog").toPath()));
            closeWindows();Query beforePopulation=future.query();render(()->WndUimsdResearch.savedExperiments(future));tap("Scores by outcome");tap("Open population B");
            render(()->{assertEquals("One navigation entry per population link",1,future.depth());assertEquals(1,future.result().rows.size());assertEquals("dead",future.result().rows.get(0).runId);assertTrue(future.back());assertEquals(beforePopulation,future.query());});
            closeWindows();render(()->WndUimsdResearch.savedExperiments(future));tap("Scores by outcome");tap("Rename experiment");typeAndConfirm("Renamed study","Rename");await("View current results");
            com.shatteredpixel.shatteredpixeldungeon.apd.query.research.SavedExperiments.Experiment renamed=com.shatteredpixel.shatteredpixeldungeon.apd.query.research.SavedExperiments.load(file).get(0);
            assertEquals("Renamed study",renamed.name);assertEquals("Scores by outcome",renamed.question);
            tap("Delete experiment");tap("Keep experiment");await("View current results");tap("Delete experiment");tap("Delete this definition");
            assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.query.research.SavedExperiments.load(file).isEmpty());
            assertArrayEquals(original,java.nio.file.Files.readAllBytes(new java.io.File(root,"runs/abandoned.apdlog").toPath()));
        }
    }
    @Test public void exactOccurrenceRowOpensRecordedTurnAndRestoresReadOnlyTimeline()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            Fixture f=begin();
            java.io.File root=new java.io.File(androidx.test.core.app.ApplicationProvider.getApplicationContext().getCacheDir(),"uimsd-occurrence-"+System.nanoTime());
            java.io.File raw=new java.io.File(root,"runs/occurrence.apdlog");
            RunLogWriter w=RunLogWriter.create(raw,TelemetryVersion.CURRENT,"occurrence","test",100);
            LifecycleEvents.RunStart start=new LifecycleEvents.RunStart();start.runId="occurrence-run";start.heroClass="WARRIOR";start.rulesetId="rules";start.apdVersionName="test";
            LifecycleEvents.emitRunStart(w,1,false,start);
            CombatEvents.Damage damage=new CombatEvents.Damage();damage.where=new CombatEvents.Where();damage.where.depth=2;
            damage.target.kind=CombatEvents.KIND_HERO;damage.source.kind=CombatEvents.SRC_CHAR;damage.source.fighterKind=CombatEvents.KIND_ENEMY;damage.source.label="rat";damage.shown=7;
            CombatEvents.emitDamage(w,21,damage);
            LifecycleEvents.RunEnd end=new LifecycleEvents.RunEnd();end.outcome=LifecycleEvents.OUTCOME_DIED;end.turns=30;
            LifecycleEvents.emitRunEnd(w,30,end);assertTrue(w.close(true));
            byte[] original=java.nio.file.Files.readAllBytes(raw.toPath());
            Catalog catalog=new DatasetStore(root).load();
            Query query=Query.of(EventDatasets.DAMAGE).withScope(new Scope(Scope.Rules.ALL,"",false,false,false,false));
            render(()->{
                f.model.setCatalog(catalog);f.model.showQuery(query);
                ResultTable table=ResultTable.build(f.model.result(),0,50);
                assertEquals("One exact recorded damage occurrence",1,table.lines.size());
                WndUimsdRow.show(f.model,table.lines.get(0));
            });
            tapRowAction("Run history at this turn");
            render(()->{try{
                WndRunTurnBrowser window=top(WndRunTurnBrowser.class);assertNotNull("Exact occurrence timeline",window);
                RunTurnBrowser browser=(RunTurnBrowser)field(window,"model");
                assertEquals("occurrence-run",browser.runId);assertEquals(21,browser.turn());assertEquals(30,browser.lastTurn);
                assertFalse("Recorded turn contains the selected occurrence",browser.events().isEmpty());
                assertTrue(browser.summary().startsWith("Turn 21 / 30"));
                Runnable restore=window.restoreOnSceneReset();window.hide();restore.run();
                WndRunTurnBrowser restored=top(WndRunTurnBrowser.class);assertNotNull("Restored occurrence timeline",restored);
                RunTurnBrowser restoredBrowser=(RunTurnBrowser)field(restored,"model");
                assertEquals(21,restoredBrowser.turn());assertEquals(browser.summary(),restoredBrowser.summary());
            }catch(Exception e){throw new RuntimeException(e);}});
            PlayRunDeviceTest.visibilityScreenshot("uimsd-occurrence-history.png");
            assertArrayEquals("Read-only occurrence navigation preserves raw history",original,java.nio.file.Files.readAllBytes(raw.toPath()));
        }
    }
    @Test public void recordedEquipmentWindowSurvivesReloadAndOpensExactDamage()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            Fixture f=begin();
            java.io.File root=new java.io.File(androidx.test.core.app.ApplicationProvider.getApplicationContext().getCacheDir(),"uimsd-window-"+System.nanoTime());
            java.io.File raw=new java.io.File(root,"runs/window.apdlog");
            RunLogWriter w=RunLogWriter.create(raw,TelemetryVersion.CURRENT,"window","test",100);
            LifecycleEvents.RunStart start=new LifecycleEvents.RunStart();start.runId="window-run";start.heroClass="WARRIOR";start.rulesetId="rules";start.apdVersionName="test";
            LifecycleEvents.emitRunStart(w,1,false,start);
            ItemEvents.Equip equip=new ItemEvents.Equip();equip.uid="armor";equip.where=new ItemEvents.Where();equip.where.depth=1;equip.state=new ItemEvents.VisibleState();equip.state.name="Leather armor";equip.state.category=ItemEvents.CAT_ARMOR;
            ItemEvents.emitEquip(w,10,ItemEvents.SLOT_ARMOR,true,equip);
            for(int i=0;i<3;i++){
                CombatEvents.Damage damage=new CombatEvents.Damage();damage.where=new CombatEvents.Where();damage.where.depth=1;
                damage.target.kind=CombatEvents.KIND_HERO;damage.source.kind=CombatEvents.SRC_CHAR;damage.source.label="rat";damage.shown=new int[]{4,3,9}[i];
                CombatEvents.emitDamage(w,new int[]{11,20,21}[i],damage);
            }
            LifecycleEvents.RunEnd end=new LifecycleEvents.RunEnd();end.outcome=LifecycleEvents.OUTCOME_DIED;end.turns=30;
            LifecycleEvents.emitRunEnd(w,30,end);assertTrue(w.close(true));
            byte[] original=java.nio.file.Files.readAllBytes(raw.toPath());
            Catalog catalog=new DatasetStore(root).load(); // read only after persistence; no fabricated table rows
            render(()->{f.model.setCatalog(catalog);f.model.showQuery(Query.of(EventDatasets.EQUIPMENT).withScope(new Scope(Scope.Rules.ALL,"",false,false,false,false)));WndUimsdResearch.equipmentWindows(f.model);});
            tap("Leather armor / turn 10");tap("Next 10 turns");await("Observed damage, next 10 turns");
            render(()->{try{for(WndUimsdList.Entry e:entries())if(e.label.equals("Observed damage, next 10 turns"))assertEquals("7 observed (at least)",e.detail);
                WndUimsdList list=top(WndUimsdList.class);Runnable restore=list.restoreOnSceneReset();list.hide();restore.run();
            }catch(Exception e){throw new RuntimeException(e);}});
            PlayRunDeviceTest.visibilityScreenshot("uimsd-equipment-window.png");
            tap("Observed damage, next 10 turns");tap("Contributing damage records");
            render(()->{try{assertEquals(2,entries().size());}catch(Exception e){throw new RuntimeException(e);}});tap(0);
            render(()->{try{assertNotNull(top(WndUimsdRow.class));assertEquals(2,f.model.result().rows.size());assertTrue(f.model.canGoBack());assertEquals(f.model.query(),QueryCodec.decode(QueryCodec.encode(f.model.query())));}catch(Exception e){throw new RuntimeException(e);}});
            assertArrayEquals(original,java.nio.file.Files.readAllBytes(raw.toPath()));
            closeWindows();render(()->{assertTrue(f.model.back());WndUimsdResearch.equipmentWindows(f.model);});tap("Leather armor / turn 10");tap("Next 5 turns");await("Observed damage, next 5 turns");
            render(()->{try{for(WndUimsdList.Entry e:entries())if(e.label.equals("Observed damage, next 5 turns"))assertEquals("4 observed (at least)",e.detail);}catch(Exception e){throw new RuntimeException(e);}});
        }
    }
    @Test public void recordedDamageRecoveryWindowSurvivesReloadAndRowAction()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            Fixture f=begin();
            java.io.File root=new java.io.File(androidx.test.core.app.ApplicationProvider.getApplicationContext().getCacheDir(),"uimsd-recovery-"+System.nanoTime());
            java.io.File raw=new java.io.File(root,"runs/recovery.apdlog");
            RunLogWriter w=RunLogWriter.create(raw,TelemetryVersion.CURRENT,"recovery","test",100);
            LifecycleEvents.RunStart start=new LifecycleEvents.RunStart();start.runId="recovery-run";start.heroClass="WARRIOR";start.rulesetId="rules";start.apdVersionName="test";
            LifecycleEvents.emitRunStart(w,1,false,start);
            CombatEvents.Damage damage=new CombatEvents.Damage();damage.where=new CombatEvents.Where();damage.where.depth=1;
            damage.target.kind=CombatEvents.KIND_HERO;damage.source.kind=CombatEvents.SRC_CHAR;damage.source.fighterKind=CombatEvents.KIND_ENEMY;damage.source.label="rat";damage.shown=8;
            CombatEvents.emitDamage(w,10,damage);
            int[] turns={10,11,20,21},amounts={99,4,3,9};
            for(int i=0;i<turns.length;i++){
                CombatEvents.Heal heal=new CombatEvents.Heal();heal.where=new CombatEvents.Where();heal.where.depth=1;
                heal.amount=amounts[i];heal.hpAfter=10+amounts[i];heal.hpMax=30;
                CombatEvents.emitHeal(w,turns[i],heal,true);
            }
            LifecycleEvents.RunEnd end=new LifecycleEvents.RunEnd();end.outcome=LifecycleEvents.OUTCOME_DIED;end.turns=30;
            LifecycleEvents.emitRunEnd(w,30,end);assertTrue(w.close(true));
            byte[] original=java.nio.file.Files.readAllBytes(raw.toPath());
            Catalog catalog=new DatasetStore(root).load();
            Query damageQuery=Query.of(EventDatasets.DAMAGE).withScope(new Scope(Scope.Rules.ALL,"",false,false,false,false));
            render(()->{f.model.setCatalog(catalog);f.model.showQuery(damageQuery);WndUimsdResearch.show(f.model);});
            tap("Healing after damage");tap("rat / turn 10");tap("Next 10 turns");await("Observed healing, next 10 turns");
            render(()->{try{for(WndUimsdList.Entry e:entries())if(e.label.equals("Observed healing, next 10 turns"))assertEquals("7 observed (at least)",e.detail);
                WndUimsdList list=top(WndUimsdList.class);Runnable restore=list.restoreOnSceneReset();list.hide();restore.run();
            }catch(Exception e){throw new RuntimeException(e);}});
            PlayRunDeviceTest.visibilityScreenshot("uimsd-damage-recovery-window.png");
            tap("Observed healing, next 10 turns");tap("Contributing healing records");
            render(()->{try{assertEquals(2,entries().size());}catch(Exception e){throw new RuntimeException(e);}});tap(0);
            render(()->{try{assertNotNull(top(WndUimsdRow.class));assertEquals(2,f.model.result().rows.size());assertTrue(f.model.canGoBack());assertEquals(f.model.query(),QueryCodec.decode(QueryCodec.encode(f.model.query())));}catch(Exception e){throw new RuntimeException(e);}});
            assertArrayEquals(original,java.nio.file.Files.readAllBytes(raw.toPath()));
            closeWindows();
            render(()->{f.model.showQuery(damageQuery);ResultTable table=ResultTable.build(f.model.result(),0,50);
                assertTrue("Exact hero-damage row exposes the recovery action",WndUimsdResearch.canDamageRecoveryWindow(f.model.result(),table.lines.get(0).row));
                WndUimsdRow.show(f.model,table.lines.get(0));});
            tapRowAction("Healing after damage");tap("Next 5 turns");await("Observed healing, next 5 turns");
            render(()->{try{for(WndUimsdList.Entry e:entries())if(e.label.equals("Observed healing, next 5 turns"))assertEquals("4 observed (at least)",e.detail);}catch(Exception e){throw new RuntimeException(e);}});
        }
    }


    @Test public void recordedDeathContextShowsFinalAndRevivedEvidenceFromRealHistory()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            Fixture f=begin();
            java.io.File root=new java.io.File(androidx.test.core.app.ApplicationProvider.getApplicationContext().getCacheDir(),"uimsd-death-context-"+System.nanoTime());
            DeviceLogs log=new DeviceLogs(root,"death-context",TelemetryVersion.CURRENT,100)
                    .start("death-context-run",0,"WARRIOR","rules",0,30,false).enter(1).tick(4)
                    .damageToHero("rat","actors.mobs.Rat",7).heroDies("rat",CombatEvents.SRC_CHAR).revived().tick(4)
                    .damageToHero("fire","Burning",9).heroDies("fire",CombatEvents.SRC_BLOB)
                    .end(LifecycleEvents.OUTCOME_DIED,"fire",1,1,0);
            java.io.File raw=log.close(true);
            byte[] original=java.nio.file.Files.readAllBytes(raw.toPath());
            Catalog catalog=new DatasetStore(root).load();
            Dataset deaths=catalog.dataset(Datasets.DEATHS);assertNotNull(deaths);assertEquals(2,deaths.rows.size());
            int revivedColumn=deaths.def.indexOf("death.revived"),finalColumn=deaths.def.indexOf("death.final");
            Row revivedDeath=null,finalDeath=null;
            for(Row row:deaths.rows){
                if(row.isKnown(revivedColumn)&&row.num(revivedColumn)==1)revivedDeath=row;
                if(row.isKnown(finalColumn)&&row.num(finalColumn)==1)finalDeath=row;
            }
            assertNotNull("Explicit revived death",revivedDeath);assertNotNull("Explicit final death",finalDeath);
            Query deathQuery=Query.of(Datasets.DEATHS).withScope(new Scope(Scope.Rules.ALL,"",false,false,false,false));
            final Row finalRow=finalDeath;
            render(()->{f.model.setCatalog(catalog);f.model.showQuery(deathQuery);ResultTable table=ResultTable.build(f.model.result(),0,50);
                for(ResultTable.Line line:table.lines)if(line.row==finalRow){WndUimsdRow.show(f.model,line);return;}fail("Final death row");});
            tapRowAction("Death context");tap("Last 10 turns");
            render(()->{try{
                WndUimsdText text=top(WndUimsdText.class);assertNotNull("Final death context text",text);
                String summary=(String)field(text,"savedText");
                assertTrue(summary.contains("Killed by: fire"));assertTrue(summary.contains("Outcome: This death ended the run."));
                assertTrue(summary.contains("this does not prove survival, causation, or absence of other events."));
                Runnable restore=text.restoreOnSceneReset();text.hide();restore.run();
                assertEquals(summary,field(top(WndUimsdText.class),"savedText"));
            }catch(Exception e){throw new RuntimeException(e);}});
            PlayRunDeviceTest.visibilityScreenshot("uimsd-death-context.png");
            closeWindows();
            final Row revivedRow=revivedDeath;
            render(()->{f.model.showQuery(deathQuery);ResultTable table=ResultTable.build(f.model.result(),0,50);
                for(ResultTable.Line line:table.lines)if(line.row==revivedRow){WndUimsdRow.show(f.model,line);return;}fail("Revived death row");});
            tapRowAction("Death context");tap("Last 10 turns");
            render(()->{try{
                String summary=(String)field(top(WndUimsdText.class),"savedText");
                assertTrue(summary.contains("Killed by: rat"));assertTrue(summary.contains("Outcome: Revived after this death."));
            }catch(Exception e){throw new RuntimeException(e);}});
            assertArrayEquals("Read-only context must preserve raw history",original,java.nio.file.Files.readAllBytes(raw.toPath()));
        }
    }

    private static void render(Runnable task)throws Exception{
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
        Game.runOnRenderThread(()->{try{task.run();}catch(Throwable t){error.set(t);}finally{done.countDown();}});
        assertTrue(done.await(45,TimeUnit.SECONDS));if(error.get()!=null)throw new AssertionError(error.get());
    }
    private static Object field(Object o,String name)throws Exception{
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static <T> T top(Class<T> type)throws Exception{
        T out=null;for(Object o:(List<?>)field(Game.scene(),"members"))if(type.isInstance(o)&&((Gizmo)o).alive)out=type.cast(o);return out;
    }
    private static List<WndUimsdList.Entry> entries()throws Exception{
        WndUimsdList list=top(WndUimsdList.class);return list==null ? Collections.emptyList():(List<WndUimsdList.Entry>)field(list,"entries");
    }
    private static void await(String label)throws Exception{
        long end=System.currentTimeMillis()+30000;
        while(System.currentTimeMillis()<end){AtomicBoolean found=new AtomicBoolean();render(()->{try{for(WndUimsdList.Entry e:entries())if(e.label.equals(label))found.set(true);}catch(Exception e){throw new RuntimeException(e);}});if(found.get())return;Thread.sleep(100);}
        fail("Research menu did not show "+label);
    }
    private static void tap(int index)throws Exception{
        AtomicReference<Point> where=new AtomicReference<>();render(()->{try{
            WndUimsdList list=top(WndUimsdList.class);assertNotNull(list);ScrollPane p=(ScrollPane)field(list,"pane");
            float[] span=((List<float[]>)field(list,"spans")).get(index);float y=span[0]+span[1]/2;p.scrollTo(0,Math.max(0,y-p.height()/2));
            where.set(p.content().camera().cameraToScreen(p.width()/2,y));
        }catch(Exception e){throw new RuntimeException(e);}});
        Point p=where.get();PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,97,PointerEvent.Type.DOWN,PointerEvent.LEFT));Thread.sleep(140);
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,97,PointerEvent.Type.UP,PointerEvent.LEFT));Thread.sleep(400);
    }
    private static void tap(String label)throws Exception{await(label);AtomicInteger index=new AtomicInteger(-1);render(()->{try{List<WndUimsdList.Entry> e=entries();for(int i=0;i<e.size();i++)if(e.get(i).label.equals(label))index.set(i);}catch(Exception e){throw new RuntimeException(e);}});assertTrue(index.get()>=0);tap(index.get());}
    private static void tapRowAction(String label)throws Exception{
        AtomicReference<Point> where=new AtomicReference<>();render(()->{try{
            WndUimsdRow row=top(WndUimsdRow.class);assertNotNull("Native row window",row);
            ScrollPane pane=null;for(Object child:(List<?>)field(row,"members"))if(child instanceof ScrollPane)pane=(ScrollPane)child;
            assertNotNull("Native row scroll pane",pane);
            RenderedTextBlock action=null;for(Object child:(List<?>)field(pane.content(),"members"))if(child instanceof RenderedTextBlock){
                RenderedTextBlock text=(RenderedTextBlock)child;if(label.equals(text.text()))action=text;
            }
            assertNotNull("Native row action "+label,action);float y=action.top()+action.height()/2;
            pane.scrollTo(0,Math.max(0,y-pane.height()/2));where.set(pane.content().camera().cameraToScreen(pane.width()/2,y));
        }catch(Exception e){throw new RuntimeException(e);}});
        Point p=where.get();PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,99,PointerEvent.Type.DOWN,PointerEvent.LEFT));Thread.sleep(140);
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,99,PointerEvent.Type.UP,PointerEvent.LEFT));Thread.sleep(400);
    }
    private static void closeWindows()throws Exception{render(()->{try{for(Object o:new ArrayList<>((List<?>)field(Game.scene(),"members")))if(o instanceof Window)((Window)o).hide();}catch(Exception e){throw new RuntimeException(e);}});}
    private static final class Fixture {
        DatasetDef def;final List<Row> rows=new ArrayList<>();final UimsdModel model=new UimsdModel("rules","Rules");
        Fixture(){for(DatasetDef d:Datasets.defs())if(d.id.equals(Datasets.RUNS))def=d;assertNotNull(def);
            for(int i=0;i<60;i++){
                Row row=new Row("record-"+i,null,"run-"+i,def.size());row.finished=true;row.ruleset="rules";row.telemetry="1.12";
                text(row,"run.id",row.runId);text(row,"run.heroClass",i<30?"mage":"warrior");text(row,"run.apdVersion","version-1");text(row,CustomRuleFacts.IDENTITY,"default");
                num(row,"result.outcome",i<30?1:(i%2==0?0:2));num(row,"result.score",i<30?100+i:i);num(row,"result.turns",100);num(row,"result.deepest",5);num(row,"journey.levelsEntered",5);
                for(String key:new String[]{"combat.heroAttacks","combat.attacksOnHero"})num(row,key,20);
                num(row,"combat.heroHits",10);num(row,"combat.hitsOnHero",5);num(row,"combat.damageDealt",200);num(row,"combat.damageTaken",100);num(row,"combat.healed",50);num(row,"combat.heroKills",5);num(row,"items.equipEvents",2);num(row,"items.acquired",10);
                num(row,"gen.levels",5);num(row,"run.seedType",0);num(row,"run.seed",i+1);num(row,CustomRuleFacts.GENERATED_UPGRADES,i==0?9:i%6);num(row,CustomRuleFacts.GENERATED_EQUIPMENT,2);num(row,"gen.items",20);num(row,"gen.mobs",30);rows.add(row);
            }
            Catalog cat=new Catalog();cat.synthetic=true;List<Dataset.RunInfo> info=new ArrayList<>();for(Row r:rows)info.add(new Dataset.RunInfo(r.runId,true,r.ruleset,true,false));cat.add(new Dataset(def,rows,info));cat.add(new ViewDef(Datasets.RUNS,"Runs","Research acceptance fixture",query()));model.setCatalog(cat);model.showQuery(query());
        }
        Query query(){return Query.of(Datasets.RUNS).withScope(new Scope(Scope.Rules.ALL,"",false,false,false,false)).withShowInternal(true);}
        void num(Row r,String key,long value){assertTrue(key,def.indexOf(key)>=0);r.setNum(def.indexOf(key),value,Fact.PROV_RECORDED);}
        void text(Row r,String key,String value){assertTrue(key,def.indexOf(key)>=0);r.setText(def.indexOf(key),value,Fact.PROV_RECORDED);}
    }
    private static Fixture begin()throws Exception{
        render(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));Thread.sleep(650);AtomicReference<Fixture> f=new AtomicReference<>();render(()->f.set(new Fixture()));return f.get();
    }
    @Test public void runDnaNativeMetricsAndSourceRunAreInspectable()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            Fixture f=begin();Query original=f.model.query();render(()->WndUimsdResearch.show(f.model));tap("Run DNA");await("Mage / Won / floor 5");tap(0);await("Hero hit rate %");
            render(()->{try{for(WndUimsdList.Entry e:entries())if(e.label.equals("Hero hit rate %"))assertEquals("50.00",e.detail);assertEquals(original,f.model.query());}catch(Exception e){throw new RuntimeException(e);}});
            render(()->{try{WndUimsdList w=top(WndUimsdList.class);Runnable restore=w.restoreOnSceneReset();w.hide();restore.run();assertNotNull(top(WndUimsdList.class));}catch(Exception e){throw new RuntimeException(e);}});
            PlayRunDeviceTest.visibilityScreenshot("uimsd-run-dna.png");tap("Hero hit rate %");tap("Source run");tap(0);
            render(()->{try{WndUimsdRow row=top(WndUimsdRow.class);assertNotNull(row);ResultTable.Line line=(ResultTable.Line)field(row,"selected");assertEquals("record-0",line.row.id);assertTrue(f.model.canGoBack());}catch(Exception e){throw new RuntimeException(e);}});
            PlayRunDeviceTest.visibilityScreenshot("uimsd-research-evidence.png");
        }
    }
    @Test public void luckLaboratoryUsesMatchedPeerEvidenceAndKeepsScores()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            Fixture f=begin();long score=f.rows.get(0).num(f.def.indexOf("result.score"));render(()->{f.model.showQuery(f.query().withShowInternal(false));WndUimsdResearch.show(f.model);});tap("Luck laboratory");await("Mage / Won / floor 5");tap(0);tap("Use generation details for finished runs");await("Generated upgrade scrolls");
            render(()->{try{for(WndUimsdList.Entry e:entries())if(e.label.equals("Generated upgrade scrolls"))assertEquals("9",e.detail);assertEquals(score,f.rows.get(0).num(f.def.indexOf("result.score")));}catch(Exception e){throw new RuntimeException(e);}});
            PlayRunDeviceTest.visibilityScreenshot("uimsd-luck-lab.png");tap("Generated upgrade scrolls");tap("Comparable measured peers");
            render(()->{try{assertEquals(29,entries().size());}catch(Exception e){throw new RuntimeException(e);}});tap(0);
            render(()->{try{assertNotNull(top(WndUimsdRow.class));}catch(Exception e){throw new RuntimeException(e);}});
            closeWindows();render(()->WndUimsdResearch.luckRun(f.model,f.rows.get(0)));await("Generated upgrade scrolls");
            render(()->{try{WndUimsdList w=top(WndUimsdList.class);Runnable restore=w.restoreOnSceneReset();f.model.setShowInternal(false);w.hide();restore.run();assertNull("Changed disclosure cannot restore old generation values",top(WndUimsdList.class));assertNotNull(top(WndUimsdText.class));}catch(Exception e){throw new RuntimeException(e);}});
        }
    }
    @Test public void automaticDiscoveryOpensTheActualContributingGroup()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            Fixture f=begin();render(()->WndUimsdResearch.show(f.model));tap("Automatic discoveries");
            long end=System.currentTimeMillis()+30000;AtomicBoolean ready=new AtomicBoolean();while(System.currentTimeMillis()<end&&!ready.get()){render(()->{try{for(WndUimsdList.Entry e:entries())if(e.label.contains("percentage points"))ready.set(true);}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(100);}assertTrue(ready.get());
            PlayRunDeviceTest.visibilityScreenshot("uimsd-discoveries.png");tap(0);tap("Group B contributors");render(()->{try{assertEquals(30,entries().size());}catch(Exception e){throw new RuntimeException(e);}});tap(0);
            render(()->{try{ResultTable.Line selected=(ResultTable.Line)field(top(WndUimsdRow.class),"selected");assertEquals("warrior",selected.row.text(f.def.indexOf("run.heroClass")));assertTrue(selected.row.num(f.def.indexOf("result.outcome"))!=1);}catch(Exception e){throw new RuntimeException(e);}});
        }
    }
    @Test public void investigationPinsTwoPopulationsAndDrillsIntoTheChosenSide()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            Fixture f=begin();render(()->{f.model.showQuery(f.query().addFilter(Filter.text("run.heroClass",Op.GROUP_TEXT,"mage")));WndUimsdResearch.studyMenu(f.model);});tap("Capture current query as population A");closeWindows();
            render(()->{f.model.showQuery(f.query().addFilter(Filter.text("run.heroClass",Op.GROUP_TEXT,"warrior")));WndUimsdResearch.studyMenu(f.model);});tap("Capture current query as population B");closeWindows();
            render(()->WndUimsdResearch.studyMenu(f.model));tap("Compare populations A and B");tap("Score");await("Score: A minus B");
            render(()->{try{for(WndUimsdList.Entry e:entries())if(e.label.equals("Score: A minus B"))assertEquals("70.00",e.detail);}catch(Exception e){throw new RuntimeException(e);}});
            PlayRunDeviceTest.visibilityScreenshot("uimsd-investigation.png");tap("Score: A minus B");tap("Population A contributors");tap(0);
            render(()->{try{ResultTable.Line line=(ResultTable.Line)field(top(WndUimsdRow.class),"selected");assertEquals("mage",line.row.text(f.def.indexOf("run.heroClass")));assertTrue(f.model.canGoBack());}catch(Exception e){throw new RuntimeException(e);}});
        }
    }
    @Test public void comparableProgressCapturesExactCohortsAndOpensContributors()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            Fixture f=begin();
            render(()->{
                for(int i=0;i<f.rows.size();i++){
                    Row row=f.rows.get(i);
                    f.text(row,FactKeys.HERO_CLASS,"mage");
                    f.num(row,FactKeys.START_MILLIS,i<30?1_000+i:2_000+i);
                    f.num(row,FactKeys.DIFFICULTY,3);
                    f.num(row,FactKeys.DIFFICULTY_BALANCE,0);
                    f.num(row,FactKeys.CHALLENGES,0);
                    f.num(row,"result.score",i<30?10:30);
                }
            });
            Query earlier=f.query().addFilter(Filter.num(FactKeys.START_MILLIS,Op.LE,1_029));
            Query recent=f.query().addFilter(Filter.num(FactKeys.START_MILLIS,Op.GE,2_000));

            render(()->{f.model.showQuery(earlier);WndUimsdResearch.show(f.model);});
            tap("Comparable personal progress");
            tap("Capture current query as earlier cohort (A)");
            closeWindows();

            render(()->{f.model.showQuery(recent);WndUimsdResearch.show(f.model);});
            tap("Comparable personal progress");
            tap("Capture current query as recent cohort (B)");
            closeWindows();

            render(()->WndUimsdResearch.show(f.model));
            tap("Comparable personal progress");
            tap("Compare earlier A with recent B");
            tap("Score");
            metricValue("Score: recent minus earlier","20.00");
            render(()->{try{
                assertEquals(recent,f.model.query());
                WndUimsdList w=top(WndUimsdList.class);assertNotNull(w);
                Runnable restore=w.restoreOnSceneReset();w.hide();restore.run();
                metricValueOnRender("Score: recent minus earlier","20.00");
                assertEquals(10,f.rows.get(0).num(f.def.indexOf("result.score")));
                assertEquals(30,f.rows.get(30).num(f.def.indexOf("result.score")));
            }catch(Exception e){throw new RuntimeException(e);}});
            PlayRunDeviceTest.visibilityScreenshot("uimsd-comparable-progress.png");

            tap("Score: recent minus earlier");
            tap("Recent exact metric contributors");
            render(()->{try{assertEquals(30,entries().size());}catch(Exception e){throw new RuntimeException(e);}});
            tap(0);
            render(()->{try{
                ResultTable.Line selected=(ResultTable.Line)field(top(WndUimsdRow.class),"selected");
                assertEquals("run-30",selected.row.runId);
                assertTrue(f.model.canGoBack());
            }catch(Exception e){throw new RuntimeException(e);}});
        }
    }

    private static void metricValueOnRender(String label,String value)throws Exception{
        boolean found=false;
        for(WndUimsdList.Entry entry:entries())if(entry.label.equals(label)){
            assertEquals(value,entry.detail);
            found=true;
        }
        assertTrue(found);
    }

}