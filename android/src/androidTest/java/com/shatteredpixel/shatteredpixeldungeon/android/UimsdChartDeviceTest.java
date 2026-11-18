package com.shatteredpixel.shatteredpixeldungeon.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.*;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.*;
import com.watabou.utils.Point;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UimsdChartDeviceTest {
    private static void render(Runnable task)throws Exception{
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
        Game.runOnRenderThread(()->{try{task.run();}catch(Throwable t){error.set(t);}finally{done.countDown();}});
        assertTrue(done.await(45,TimeUnit.SECONDS));if(error.get()!=null)throw new AssertionError(error.get());
    }
    private static Object field(Object o,String name)throws Exception{
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static <T> T find(Class<T> type)throws Exception{
        T result=null;
        for(Object o:(List<?>)field(Game.scene(),"members"))if(type.isInstance(o)&&((Gizmo)o).alive)result=type.cast(o);
        return result;
    }
    private static void tap(Point p)throws Exception{
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,96,PointerEvent.Type.DOWN,PointerEvent.LEFT));Thread.sleep(140);
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,96,PointerEvent.Type.UP,PointerEvent.LEFT));Thread.sleep(400);
    }
    private static UimsdModel fixture(){
        DatasetDef def=new DatasetDef("chartfixture","Chart fixture","Native chart acceptance");
        def.add(ColumnDef.text("label","Group").shown());def.add(ColumnDef.num("value","Value").shown());
        List<Row> rows=new ArrayList<>();List<Dataset.RunInfo> runs=new ArrayList<>();
        for(int i=0;i<30;i++){
            Row row=new Row("row"+i,null,"run"+i,def.size());row.finished=true;row.ruleset="rules";
            row.setText(0,String.format(java.util.Locale.ROOT,"Group %02d",i),Fact.PROV_RECORDED);
            if(i!=1)row.setNum(1,i-10,i==0 ? Fact.PROV_INCOMPLETE : Fact.PROV_RECORDED);
            rows.add(row);runs.add(new Dataset.RunInfo(row.runId,true,"rules",true,false));
        }
        Catalog cat=new Catalog();cat.add(new Dataset(def,rows,runs));
        Query q=Query.of(def.id).withScope(new Scope(Scope.Rules.ALL,"",true,true,true,true))
                .withGroup(new GroupBy(Arrays.asList("label"),Arrays.asList(Aggregate.of(Aggregate.Fn.AVG,"value"))));
        cat.add(new ViewDef(def.id,def.title,def.description,q));
        UimsdModel model=new UimsdModel("rules","Rules");model.setCatalog(cat);model.showRoot(def.id);return model;
    }
    @Test public void chartMeasureTouchesScrollRestoreAndGroupDetailsPreserveTheQuery()throws Exception{
        try(ActivityScenario<AndroidLauncher> activity=ActivityScenario.launch(AndroidLauncher.class)){
            render(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));Thread.sleep(650);
            AtomicReference<UimsdModel> model=new AtomicReference<>();AtomicReference<Query> query=new AtomicReference<>();
            render(()->{model.set(fixture());query.set(model.get().query());WndUimsdOptions.show(model.get());});
            AtomicReference<Point> touch=new AtomicReference<>();
            render(()->{try{
                WndUimsdList picker=find(WndUimsdList.class);List<?> entries=(List<?>)field(picker,"entries");
                int index=-1;for(int i=0;i<entries.size();i++)if(((WndUimsdList.Entry)entries.get(i)).label.equals("Charts"))index=i;
                assertTrue(index>=0);ScrollPane p=(ScrollPane)field(picker,"pane");float[] span=((List<float[]>)field(picker,"spans")).get(index);
                float y=span[0]+span[1]/2;p.scrollTo(0,Math.max(0,y-p.height()/2));touch.set(p.content().camera().cameraToScreen(p.width()/2,y));
            }catch(Exception e){throw new RuntimeException(e);}});tap(touch.get());
            for(int pass=0;pass<2;pass++){
                render(()->{try{
                    WndUimsdChart chart=find(WndUimsdChart.class);assertNotNull(chart);
                    UimsdChart.Series s=(UimsdChart.Series)field(chart,"series");assertEquals(24,s.points.size());assertEquals(6,s.omitted);
                    for(Object o:(List<?>)field(chart,"members"))if(o instanceof StyledButton){
                        StyledButton b=(StyledButton)o;touch.set(b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2));
                    }
                }catch(Exception e){throw new RuntimeException(e);}});tap(touch.get());
                final int pick=pass==0 ? 1 : 0;
                render(()->{try{
                    WndUimsdList picker=find(WndUimsdList.class);ScrollPane p=(ScrollPane)field(picker,"pane");
                    float[] span=((List<float[]>)field(picker,"spans")).get(pick);float y=span[0]+span[1]/2;
                    p.scrollTo(0,0);touch.set(p.content().camera().cameraToScreen(p.width()/2,y));
                }catch(Exception e){throw new RuntimeException(e);}});tap(touch.get());
                render(()->{try{
                    WndUimsdChart chart=find(WndUimsdChart.class);assertNotNull(chart);
                    UimsdChart.Series s=(UimsdChart.Series)field(chart,"series");assertEquals(pick==1 ? "avg:value" : "count",s.measure.id);
                    if(pick==1){assertEquals(CellText.LOWER,s.points.get(0).state);assertFalse(s.points.get(1).known);}
                    assertSame(query.get(),model.get().query());
                }catch(Exception e){throw new RuntimeException(e);}});
                PlayRunDeviceTest.visibilityScreenshot(pass==0 ? "uimsd-chart-values.png" : "uimsd-chart-counts.png");
            }
            render(()->{try{
                WndUimsdChart chart=find(WndUimsdChart.class);ScrollPane p=(ScrollPane)field(chart,"pane");p.scrollTo(0,200);
                float y=p.content().camera().scroll.y;Runnable restore=chart.restoreOnSceneReset();chart.hide();restore.run();
                WndUimsdChart reopened=find(WndUimsdChart.class);ScrollPane restored=(ScrollPane)field(reopened,"pane");
                assertEquals(y,restored.content().camera().scroll.y,.1f);
                float[] span=((List<float[]>)field(reopened,"spans")).get(18);float cy=span[0]+span[1]/2;
                restored.scrollTo(0,cy-restored.height()/2);touch.set(restored.content().camera().cameraToScreen(restored.width()/2,cy));
            }catch(Exception e){throw new RuntimeException(e);}});tap(touch.get());
            render(()->{try{
                WndUimsdRow row=find(WndUimsdRow.class);assertNotNull("Scrolled group receives native touch",row);
                ResultTable.Line selected=(ResultTable.Line)field(row,"selected");assertSame(model.get().result().groups.get(18),selected.group);
                assertSame(query.get(),model.get().query());row.hide();
            }catch(Exception e){throw new RuntimeException(e);}});
        }
    }
}
