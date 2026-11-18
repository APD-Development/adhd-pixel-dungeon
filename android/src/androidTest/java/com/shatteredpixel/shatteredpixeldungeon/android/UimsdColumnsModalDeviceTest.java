package com.shatteredpixel.shatteredpixeldungeon.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdColumns;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.Point;
import com.watabou.input.PointerEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Regression: selecting Columns repeatedly must not let clicks go through the modal. */
@RunWith(AndroidJUnit4.class)
public class UimsdColumnsModalDeviceTest {
    private static void openReadyUimsd()throws Exception{
        until(()->Game.instance!=null&&Game.scene()!=null);
        render(()->{
            com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdUimsdGuideSeen(true);
            ShatteredPixelDungeon.switchNoFade(TitleScene.class);
        });
        until(()->Game.scene() instanceof TitleScene&&!Game.switchingScene());
        render(()->UimsdScene.openNow(TitleScene.class));
        until(()->{
            if(!(Game.scene() instanceof UimsdScene)||Game.switchingScene())return false;
            UimsdModel m=(UimsdModel)field(UimsdScene.class,"model");
            return m!=null&&m.state()==UimsdModel.State.READY
                    &&((Number)field(Game.scene(),"firstReadyHomeDrawNanos")).longValue()>0;
        });
    }

    private static ScrollPane tableBody()throws Exception{
        return (ScrollPane)field(field(Game.scene(),"activeTable"),"body");
    }

    private static void pointer(Point p)throws Exception{
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,85,PointerEvent.Type.DOWN,PointerEvent.LEFT));
        Thread.sleep(120);
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,85,PointerEvent.Type.UP,PointerEvent.LEFT));
        Thread.sleep(400);
    }

    @SuppressWarnings("unchecked")
    private static void followOnlyRowLink()throws Exception{
        AtomicReference<Point> at=new AtomicReference<>();
        render(()->{try{
            ScrollPane body=tableBody();
            at.set(body.content().camera().cameraToScreen(body.content().camera().scroll.x+body.width()/2,
                    body.content().camera().scroll.y+5));
        }catch(Exception e){throw new RuntimeException(e);}});
        pointer(at.get());
        render(()->{try{
            com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdRow row=null;
            for(Gizmo g:(List<Gizmo>)field(Game.scene(),"members"))
                if(g instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdRow && g.alive)
                    row=(com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdRow)g;
            assertNotNull("Pointer opened the inspected record",row);
            ScrollPane pane=null;
            for(Gizmo g:(List<Gizmo>)field(row,"members"))if(g instanceof ScrollPane)pane=(ScrollPane)g;
            assertNotNull(pane);
            // Fixture records each expose exactly one link, after their details.
            float y=pane.content().height()-12;
            pane.scrollTo(0,Math.max(0,y-pane.height()/2));
            at.set(pane.content().camera().cameraToScreen(pane.width()/2,y));
        }catch(Exception e){throw new RuntimeException(e);}});
        pointer(at.get());
        until(()->{try{return !(Boolean)field(Game.scene(),"dirty");}catch(Exception e){return false;}});
    }

    private static void backThroughVisibleExit()throws Exception{
        AtomicReference<Point> at=new AtomicReference<>();
        render(()->{try{
            com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton b=
                    (com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton)field(Game.scene(),"btnExit");
            at.set(b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2));
        }catch(Exception e){throw new RuntimeException(e);}});
        pointer(at.get());
        until(()->{try{return !(Boolean)field(Game.scene(),"dirty");}catch(Exception e){return false;}});
    }

    private static void assertPosition(float x,float y)throws Exception{
        // A direct fixture scroll occurs outside ScrollPane.update. Allow the
        // normal table update to synchronize its frozen panes before capture.
        Thread.sleep(200);
        render(()->{try{
            ScrollPane body=tableBody();assertEquals(x,body.content().camera().scroll.x,.1f);
            assertEquals(y,body.content().camera().scroll.y,.1f);
            Object table=field(Game.scene(),"activeTable");
            assertEquals(x,((ScrollPane)field(table,"header")).content().camera().scroll.x,.1f);
            assertEquals(y,((ScrollPane)field(table,"firstCol")).content().camera().scroll.y,.1f);
        }catch(Exception e){throw new RuntimeException(e);}});
    }

    @Test public void evidenceBackRestoresEachTableAndSurvivesSceneRecreation()throws Exception{
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            openReadyUimsd();
            AtomicReference<UimsdModel> model=new AtomicReference<>();AtomicReference<Query> original=new AtomicReference<>();
            render(()->{try{
                UimsdModel m=(UimsdModel)field(UimsdScene.class,"model");model.set(m);
                Catalog cat=new Catalog();
                for(String name:Arrays.asList("parent","child")){
                    DatasetDef d=new DatasetDef(name,name,"Navigation acceptance fixture");
                    for(int c=0;c<12;c++)d.add(ColumnDef.num("c"+c,"Statistic "+c).shown());
                    d.add(ColumnDef.text("join","Shared record key"));
                    d.link(new LinkDef("Follow recorded evidence",name.equals("parent")?"child":"parent","join","join"));
                    List<Row> rows=new ArrayList<>();List<Dataset.RunInfo> runs=new ArrayList<>();
                    for(int i=0;i<130;i++){
                        Row r=new Row(name+i,null,"run"+i,d.size());r.finished=true;r.ruleset="rules";
                        for(int c=0;c<12;c++)r.setNum(c,i+c,com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact.PROV_RECORDED);
                        r.setText(12,"shared",com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact.PROV_RECORDED);
                        rows.add(r);runs.add(new Dataset.RunInfo(r.runId,true,r.ruleset,true,false));
                    }
                    cat.add(new Dataset(d,rows,runs));cat.add(new ViewDef(name,name,"Linked evidence",
                            Query.of(name).withScope(new Scope(Scope.Rules.ALL,"",true,true,true,true))));
                }
                m.setCatalog(cat);m.showRoot("parent");m.addFilter(Filter.num("c1",Op.GE,10));
                m.setSorts(Collections.singletonList(new Sort("c8",true)));m.setPage(1);original.set(m.query());
                Field h=UimsdScene.class.getDeclaredField("atHome");h.setAccessible(true);h.set(null,false);
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(450);
            render(()->{try{tableBody().scrollTo(300,66);}catch(Exception e){throw new RuntimeException(e);}});
            assertPosition(300,66);PlayRunDeviceTest.visibilityScreenshot("uimsd-navigation-parent.png");
            followOnlyRowLink();assertEquals("child",model.get().query().dataset);assertEquals(1,model.get().depth());assertPosition(0,0);
            render(()->{try{tableBody().scrollTo(150,44);}catch(Exception e){throw new RuntimeException(e);}});
            followOnlyRowLink();assertEquals("parent",model.get().query().dataset);assertEquals(2,model.get().depth());assertPosition(0,0);
            render(()->{try{tableBody().scrollTo(80,22);}catch(Exception e){throw new RuntimeException(e);}});
            backThroughVisibleExit();assertEquals("child",model.get().query().dataset);assertPosition(150,44);
            PlayRunDeviceTest.visibilityScreenshot("uimsd-navigation-child-back.png");
            backThroughVisibleExit();assertEquals(original.get(),model.get().query());assertEquals(1,model.get().page());assertPosition(300,66);
            Object previous=Game.scene();render(()->ShatteredPixelDungeon.switchNoFade(UimsdScene.class));
            until(()->Game.scene() instanceof UimsdScene && Game.scene()!=previous);
            until(()->{try{return field(Game.scene(),"activeTable")!=null && !(Boolean)field(Game.scene(),"dirty");}catch(Exception e){return false;}});
            assertPosition(300,66);assertEquals(original.get(),model.get().query());assertEquals(1,model.get().page());
            PlayRunDeviceTest.visibilityScreenshot("uimsd-navigation-restored.png");
        }
    }

    private static void render(Runnable task)throws Exception {
        CountDownLatch finished=new CountDownLatch(1);
        AtomicReference<Throwable> exception=new AtomicReference<>();
        Game.runOnRenderThread(()->{
            try{task.run();}catch(Throwable t){exception.set(t);}finally{finished.countDown();}
        });
        assertTrue("Render thread",finished.await(45,TimeUnit.SECONDS));
        if(exception.get()!=null)throw new AssertionError(exception.get());
    }

    private static Object field(Object value,String name)throws Exception{
        for(Class<?> c=value instanceof Class ? (Class<?>)value : value.getClass();c!=null;c=c.getSuperclass()) {
            try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(value instanceof Class ? null:value);}
            catch(NoSuchFieldException ignored){}
        }
        throw new NoSuchFieldException(name);
    }

    @SuppressWarnings("unchecked")
    private static WndUimsdList picker()throws Exception{
        WndUimsdList top=null;
        for(Gizmo g:(List<Gizmo>)field(Game.scene(),"members"))
            if(g instanceof WndUimsdList && g.alive)top=(WndUimsdList)g;
        return top;
    }

    private static void until(Callable<Boolean> check)throws Exception{
        long limit=System.currentTimeMillis()+30000;
        while(System.currentTimeMillis()<limit){
            if(check.call())return;
            Thread.sleep(100);
        }
        fail("Native UIMSD did not reach the expected state");
    }

    @SuppressWarnings("unchecked")
    private static void tapRow(int index)throws Exception {
        AtomicReference<Point> at=new AtomicReference<>();
        render(()->{try{
            WndUimsdList w=picker();
            assertNotNull("Columns picker is still open",w);
            ScrollPane pane=(ScrollPane)field(w,"pane");
            List<float[]> spans=(List<float[]>)field(w,"spans");
            float[] row=spans.get(index);
            assertNotNull(row);
            float y=row[0]+row[1]/2;
            pane.scrollTo(0,Math.max(0,y-pane.height()/2));
            at.set(pane.content().camera().cameraToScreen(pane.width()/2,y));
        }catch(Exception e){throw new RuntimeException(e);}});
        Point p=at.get();
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,82,PointerEvent.Type.DOWN,PointerEvent.LEFT));
        Thread.sleep(120);
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,82,PointerEvent.Type.UP,PointerEvent.LEFT));
        Thread.sleep(450);
    }

    @SuppressWarnings("unchecked")
    private static void tapLabel(String label)throws Exception {
        final int[] index={-1};
        render(()->{try{
            List<WndUimsdList.Entry> entries=(List<WndUimsdList.Entry>)field(picker(),"entries");
            for(int i=0;i<entries.size();i++)if(entries.get(i).label.equals(label))index[0]=i;
            assertTrue("Menu entry: "+label,index[0]>=0);
        }catch(Exception e){throw new RuntimeException(e);}});
        tapRow(index[0]);
    }

    @SuppressWarnings("unchecked")
    private static void tapSort()throws Exception {
        AtomicReference<Point> at=new AtomicReference<>();
        render(()->{try{
            float[] r=((Map<String,float[]>)field(Game.scene(),"targets")).get("sort");
            at.set(com.watabou.noosa.Camera.main.cameraToScreen(r[0]+r[2]/2,r[1]+r[3]/2));
        }catch(Exception e){throw new RuntimeException(e);}});
        Point p=at.get();
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,83,PointerEvent.Type.DOWN,PointerEvent.LEFT));
        Thread.sleep(120);
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,83,PointerEvent.Type.UP,PointerEvent.LEFT));
        Thread.sleep(400);
    }

    @Test public void sortingThroughNativeMenusKeepsRightHandColumnAndDirectionVisible()throws Exception {
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            openReadyUimsd();
            AtomicReference<UimsdModel> model=new AtomicReference<>();
            render(()->{try{
                UimsdModel m=(UimsdModel)field(UimsdScene.class,"model");model.set(m);
                DatasetDef d=new DatasetDef("viewportfixture","Viewport fixture","Native sorting regression");
                for(int c=0;c<12;c++)d.add(ColumnDef.num("c"+c,"Statistic "+c).shown());
                List<Row> rows=new ArrayList<>();List<Dataset.RunInfo> runs=new ArrayList<>();
                for(int i=0;i<60;i++){
                    Row r=new Row("r"+i,null,"run"+i,d.size());r.finished=true;r.ruleset="r1-test";
                    for(int c=0;c<12;c++)r.setNum(c,i+c,com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact.PROV_RECORDED);
                    rows.add(r);runs.add(new Dataset.RunInfo(r.runId,true,r.ruleset,true,false));
                }
                Catalog cat=new Catalog();cat.add(new Dataset(d,rows,runs));
                cat.add(new ViewDef("viewportfixture","Viewport fixture","Wide table",Query.of("viewportfixture")
                        .withScope(new Scope(Scope.Rules.ALL,"",true,true,true,true))));
                m.setCatalog(cat);m.showRoot("viewportfixture");
                Field home=UimsdScene.class.getDeclaredField("atHome");home.setAccessible(true);home.set(null,false);
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(400);
            final float[] expected={0};
            render(()->{try{
                Object t=field(Game.scene(),"activeTable");ScrollPane body=(ScrollPane)field(t,"body");
                TableGeometry g=TableGeometry.of(model.get().table());expected[0]=g.lefts[6]-g.firstWidth();
                body.scrollTo(expected[0],88);
                assertEquals(expected[0],body.content().camera().scroll.x,0.1f);
            }catch(Exception e){throw new RuntimeException(e);}});
            tapSort();tapLabel("Add a sort key");tapLabel("Statistic 6");tapLabel("Highest first");
            until(()->{try{return picker()==null && !(Boolean)field(Game.scene(),"dirty");}catch(Exception e){return false;}});
            assertViewport(model.get(),expected[0],-1);
            tapSort();tapLabel("1. Statistic 6, highest first");tapLabel("Reverse it");
            until(()->{try{return picker()==null && !(Boolean)field(Game.scene(),"dirty");}catch(Exception e){return false;}});
            assertViewport(model.get(),expected[0],1);
            render(()->{try{
                ScrollPane body=(ScrollPane)field(field(Game.scene(),"activeTable"),"body");
                TableGeometry g=TableGeometry.of(model.get().table());expected[0]=g.lefts[9]-g.firstWidth();
                body.scrollTo(expected[0],55);
            }catch(Exception e){throw new RuntimeException(e);}});
            tapSort();tapLabel("Add a sort key");tapLabel("Statistic 9");tapLabel("Highest first");
            until(()->{try{return picker()==null && !(Boolean)field(Game.scene(),"dirty");}catch(Exception e){return false;}});
            render(()->{try{
                ScrollPane body=(ScrollPane)field(field(Game.scene(),"activeTable"),"body");
                assertEquals("Secondary sort must not jump to the primary column",expected[0],body.content().camera().scroll.x,0.1f);
                assertEquals(-1,model.get().table().headers.get(9).sortDirection);
                assertEquals(2,model.get().table().headers.get(9).sortRank);
            }catch(Exception e){throw new RuntimeException(e);}});
            render(()->{try{
                ScrollPane body=(ScrollPane)field(field(Game.scene(),"activeTable"),"body");body.scrollTo(expected[0],55);
                model.get().setShowInternal(!model.get().query().showInternal);
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(400);
            render(()->{try{
                ScrollPane body=(ScrollPane)field(field(Game.scene(),"activeTable"),"body");
                assertEquals(expected[0],body.content().camera().scroll.x,0.1f);
                assertEquals(55,body.content().camera().scroll.y,0.1f);
                model.get().addFilter(Filter.num("c6",Op.GE,10));
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(400);
            render(()->{try{
                ScrollPane body=(ScrollPane)field(field(Game.scene(),"activeTable"),"body");
                assertEquals("Filter refresh retains right-hand column",expected[0],body.content().camera().scroll.x,0.1f);
                assertEquals("Filter refresh retains vertical offset where possible",55,body.content().camera().scroll.y,0.1f);
            }catch(Exception e){throw new RuntimeException(e);}});
        }
    }

    private static void assertViewport(UimsdModel model,float x,int direction)throws Exception {
        render(()->{try{
            Object t=field(Game.scene(),"activeTable");ScrollPane body=(ScrollPane)field(t,"body");
            assertEquals("Sorting retains horizontal viewport",x,body.content().camera().scroll.x,0.1f);
            assertEquals("New sort starts at the first result",0,body.content().camera().scroll.y,0.1f);
            ResultTable table=model.table();assertEquals(direction,table.headers.get(6).sortDirection);
            assertTrue(TableViewport.heading(table.headers.get(6)).startsWith(direction>0?"^ ":"v "));
            long expected=direction>0?6:65;
            assertEquals("Actual sorted result",expected,table.lines.get(0).row.num(6));
        }catch(Exception e){throw new RuntimeException(e);}});
    }

    @Test public void multipleColumnTapsRemainAboveTableUntilDone()throws Exception {
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            openReadyUimsd();
            until(()->{try{return field(UimsdScene.class,"model")!=null;}catch(Exception e){return false;}});
            final AtomicReference<UimsdModel> model=new AtomicReference<>();
            final AtomicReference<Group> background=new AtomicReference<>();
            render(()->{try{
                UimsdModel m=(UimsdModel)field(UimsdScene.class,"model");
                model.set(m);
                DatasetDef d=new DatasetDef("modalfixture","Modal fixture","Column toggle regression");
                d.add(ColumnDef.text("m.first","First").shown());
                d.add(ColumnDef.text("m.second","Second").shown());
                d.add(ColumnDef.text("m.third","Third"));
                Catalog cat=new Catalog();
                cat.add(new Dataset(d,new ArrayList<>(),new ArrayList<>()));
                cat.add(new ViewDef("modalfixture","Modal fixture","Three independent columns",Query.of("modalfixture")));
                m.setCatalog(cat);
                m.showRoot("modalfixture");
                Field f=UimsdScene.class.getDeclaredField("atHome");f.setAccessible(true);f.set(null,false);
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(350);
            render(()->{try{
                background.set((Group)field(Game.scene(),"ui"));
                WndUimsdColumns.show(model.get());
                assertNotNull(picker());
            }catch(Exception e){throw new RuntimeException(e);}});
            // First column off; changing query sets dirty=true but must not
            // rebuild UIMSD's underlying pointer controls until the modal closes.
            tapRow(1);
            render(()->{try{
                assertSame("No UI replacement while popup is open",background.get(),field(Game.scene(),"ui"));
                assertTrue((Boolean)field(Game.scene(),"dirty"));
                assertNotNull("First selection did not dismiss popup",picker());
            }catch(Exception e){throw new RuntimeException(e);}});
            tapRow(3); // third column on
            render(()->{try {
                List<String> active=model.get().query().columns;
                assertFalse("first column toggled off",active.contains("m.first"));
                assertTrue("second column stays",active.contains("m.second"));
                assertTrue("third column toggled on by second tap",active.contains("m.third"));
                assertSame("Popup still has input priority",background.get(),field(Game.scene(),"ui"));
            }catch(Exception e){throw new RuntimeException(e);}});
            tapRow(4); // Done
            until(()->{try{return picker()==null;}catch(Exception e){return false;}});
            until(()->{try{return field(Game.scene(),"ui")!=background.get();}catch(Exception e){return false;}});
        }
    }
}

