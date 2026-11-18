package com.shatteredpixel.shatteredpixeldungeon.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdSearch;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdAsk;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.Point;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** UI gesture acceptance for APD's entirely local searchable UIMSD. */
@RunWith(AndroidJUnit4.class)
public class UimsdSearchDeviceTest {
    private static void onRender(Runnable work) throws Exception {
        CountDownLatch latch=new CountDownLatch(1);
        AtomicReference<Throwable> thrown=new AtomicReference<>();
        Game.runOnRenderThread(()->{
            try{work.run();}catch(Throwable t){thrown.set(t);}finally{latch.countDown();}
        });
        assertTrue("render thread timed out",latch.await(45,TimeUnit.SECONDS));
        if(thrown.get()!=null)throw new AssertionError(thrown.get());
    }
    private static Object value(Object target,String name)throws Exception {
        for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass()){
            try {Field field=c.getDeclaredField(name);field.setAccessible(true);return field.get(target);}
            catch(NoSuchFieldException ignored){}
        }
        throw new NoSuchFieldException(name);
    }
    @SuppressWarnings("unchecked")
    private static <T> T find(Group group,Class<T> type)throws Exception {
        for(Gizmo g:(List<Gizmo>)value(group,"members"))
            if(type.isInstance(g)&&g.alive)return type.cast(g);
        return null;
    }
    @Test public void searchDialogOpensNativePickerAndRowTouchOpensOnlyTheChosenTable()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            onRender(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));
            Thread.sleep(500);
            AtomicReference<String> selected=new AtomicReference<>();
            AtomicReference<Point> touch=new AtomicReference<>();
            onRender(()->{try {
                Catalog c=new Catalog();
                DatasetDef deaths=new DatasetDef("deaths","Deaths","How characters died");
                deaths.add(ColumnDef.num("death.floor","Floor").shown());
                c.add(new Dataset(deaths,new ArrayList<>(),new ArrayList<>()));
                c.add(new ViewDef("deaths","Deaths","All recorded deaths",Query.of("deaths")));
                WndUimsdSearch.open(c,selected::set);
                WndTextInput entry=find(Game.scene(),WndTextInput.class);
                assertNotNull("native search text input must open",entry);
                entry.onSelect(true,"muerte");
                entry.hide();
                WndUimsdList results=find(Game.scene(),WndUimsdList.class);
                assertNotNull("localized query must have a result",results);
                List<WndUimsdList.Entry> rows=(List<WndUimsdList.Entry>)value(results,"entries");
                assertEquals(1,rows.size());
                assertEquals("Deaths",rows.get(0).label);
                ScrollPane pane=(ScrollPane)value(results,"pane");
                List<float[]> spans=(List<float[]>)value(results,"spans");
                float[] span=spans.get(0);
                float yy=span[0]+span[1]/2;
                pane.scrollTo(0,0);
                touch.set(pane.content().camera().cameraToScreen(pane.width()/2,yy));
            }catch(Exception e){throw new RuntimeException(e);}});
            assertNotNull(touch.get());
            Point p=touch.get();
            PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,95,PointerEvent.Type.DOWN,PointerEvent.LEFT));
            Thread.sleep(150);
            PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,95,PointerEvent.Type.UP,PointerEvent.LEFT));
            for(int i=0;i<35 && selected.get()==null;i++)Thread.sleep(100);
            assertEquals("Native result click must use original UIMSD view", "deaths",selected.get());
        }
    }
    @SuppressWarnings("unchecked")
    private static WndUimsdList topList()throws Exception{
        WndUimsdList out=null;
        for(Gizmo g:(List<Gizmo>)value(Game.scene(),"members"))
            if(g instanceof WndUimsdList&&g.alive)out=(WndUimsdList)g;
        return out;
    }
    @SuppressWarnings("unchecked")
    private static void tapListLabel(String label)throws Exception{
        long deadline=System.currentTimeMillis()+15000;
        AtomicReference<Point> where=new AtomicReference<>();
        while(System.currentTimeMillis()<deadline&&where.get()==null){
            onRender(()->{try{
                WndUimsdList list=topList();
                if(list==null)return;
                List<WndUimsdList.Entry> entries=(List<WndUimsdList.Entry>)value(list,"entries");
                List<float[]> spans=(List<float[]>)value(list,"spans");
                int index=-1;
                for(int i=0;i<entries.size();i++)if(label.equals(entries.get(i).label)){index=i;break;}
                if(index<0)return;
                float[] span=spans.get(index);
                ScrollPane pane=(ScrollPane)value(list,"pane");
                float y=span[0]+span[1]/2;
                pane.scrollTo(0,Math.max(0,y-pane.height()/2));
                where.set(pane.content().camera().cameraToScreen(pane.width()/2,y));
            }catch(Exception e){throw new RuntimeException(e);}});
            if(where.get()==null)Thread.sleep(100);
        }
        assertNotNull("No native UIMSD row labelled "+label,where.get());
        Point p=where.get();
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,96,PointerEvent.Type.DOWN,PointerEvent.LEFT));
        Thread.sleep(140);
        PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,96,PointerEvent.Type.UP,PointerEvent.LEFT));
        Thread.sleep(450);
    }
    @SuppressWarnings("unchecked")
    private static void closeWindows()throws Exception{
        onRender(()->{try{
            for(Gizmo g:new ArrayList<>((List<Gizmo>)value(Game.scene(),"members")))
                if(g instanceof Window)((Window)g).hide();
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void setField(Object target,String name,Object replacement)throws Exception{
        for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass()){
            try{Field field=c.getDeclaredField(name);field.setAccessible(true);field.set(target,replacement);return;}
            catch(NoSuchFieldException ignored){}
        }
        throw new NoSuchFieldException(name);
    }

    @Test public void exactQuestionSavesAndReopensThroughNativeMenus()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            onRender(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));
            Thread.sleep(500);
            final java.io.File root=new java.io.File(
                    androidx.test.core.app.ApplicationProvider.getApplicationContext().getCacheDir(),
                    "uimsd-saved-question-"+System.nanoTime());
            assertTrue(root.mkdirs());

            final DatasetDef damage=new DatasetDef(EventDatasets.DAMAGE,"Damage","Question persistence fixture");
            damage.countLabel="Damage records";
            damage.add(ColumnDef.text("name","Name").shown());
            damage.add(ColumnDef.num("value","Value").shown());
            final DatasetDef floors=new DatasetDef(EventDatasets.FLOORS,"Floors","Per-unit fixture");
            floors.countLabel="Floor visits";
            floors.add(ColumnDef.num("floor.turns","Turns").shown());
            final Catalog catalog=new Catalog();
            catalog.synthetic=true;
            catalog.add(new Dataset(damage,new ArrayList<>(),new ArrayList<>()));
            catalog.add(new Dataset(floors,new ArrayList<>(),new ArrayList<>()));
            catalog.add(new ViewDef(EventDatasets.DAMAGE,"Damage","Question persistence fixture",Query.of(EventDatasets.DAMAGE)));
            final UimsdModel model=new UimsdModel("rules","Rules");
            setField(model,"root",root);
            model.setCatalog(catalog);
            model.showRoot(EventDatasets.DAMAGE);
            final Question question=Question.about(EventDatasets.DAMAGE)
                    .withMeasure(Question.Measure.TOTAL,"value")
                    .withPer(PerUnit.Unit.RUN)
                    .withGroupBy(Arrays.asList("name"))
                    .addFilter(Filter.text("name",Op.EQ,"rat"))
                    .withScope(new Scope(Scope.Rules.ALL,"",true,true,true,true));

            onRender(()->WndUimsdAsk.answer(catalog,"rules",new WndUimsdAsk.Host(){
                @Override public void openAsTable(Query query,Runnable backToAnswer){model.showQuery(query);}
                @Override public java.io.File root(){return root;}
            },question));
            tapListLabel("Save this question...");
            onRender(()->{try{
                WndTextInput input=find(Game.scene(),WndTextInput.class);
                assertNotNull("native saved-question name input",input);
                input.onSelect(true,"Native exact question");
                input.hide();
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(400);

            SavedViews.View stored=SavedViews.load(SavedViews.fileIn(root)).get("Native exact question");
            assertNotNull("named question must be written",stored);
            Question restored=stored.question();
            assertNotNull("saved view must retain friendly question",restored);
            assertEquals(Question.Measure.TOTAL,restored.measure);
            assertEquals("value",restored.column);
            assertEquals(PerUnit.Unit.RUN,restored.per);
            assertEquals(Arrays.asList("name"),restored.groupBy);
            assertEquals(Arrays.asList(Filter.text("name",Op.EQ,"rat")),restored.filters);
            assertEquals(Scope.Rules.ALL,restored.scope.rules);

            closeWindows();
            onRender(()->WndUimsdOptions.show(model));
            tapListLabel("Open a saved view...");
            tapListLabel("Native exact question");
            tapListLabel("Open question");
            onRender(()->{try{
                List<WndUimsdList.Entry> entries=(List<WndUimsdList.Entry>)value(topList(),"entries");
                Set<String> labels=new HashSet<>();
                for(WndUimsdList.Entry entry:entries)labels.add(entry.label);
                assertTrue(labels.contains("Measure: the total value"));
                assertTrue(labels.contains("Per: run"));
                assertTrue(labels.contains("Split by: name"));
                assertTrue(labels.contains("Only where: 1 condition"));
            }catch(Exception e){throw new RuntimeException(e);}});
            PlayRunDeviceTest.visibilityScreenshot("uimsd-saved-question.png");
        }
    }

}
