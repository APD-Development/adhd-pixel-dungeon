package com.shatteredpixel.shatteredpixeldungeon.android;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoPile;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Gizmo;
import com.watabou.utils.Point;
import com.watabou.input.PointerEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PileExaminationDeviceTest {
    private static void onRender(Runnable work)throws Exception{
        CountDownLatch done=new CountDownLatch(1);
        AtomicReference<Throwable> failure=new AtomicReference<>();
        Game.runOnRenderThread(()->{try{work.run();}catch(Throwable t){failure.set(t);}finally{done.countDown();}});
        assertTrue("render thread timed out",done.await(60,TimeUnit.SECONDS));
        if(failure.get()!=null)throw new AssertionError(failure.get());
    }
    private static Object get(Object o,String name)throws Exception{
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass()){
            try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}
            catch(NoSuchFieldException ignored){}
        }
        throw new NoSuchFieldException(name);
    }
    @SuppressWarnings("unchecked")
    private static <T>T find(Group group,Class<T> type)throws Exception{
        for(Gizmo child:(List<Gizmo>)get(group,"members")){
            if(type.isInstance(child)&&child.alive)return type.cast(child);
        }
        return null;
    }
    @Test public void hugePileScrollsAndRealTouchOpensSelectedItemDescription()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            onRender(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));
            Thread.sleep(700);
            AtomicReference<Point> point=new AtomicReference<>();
            onRender(()->{try{
                Heap pile=new Heap();pile.seen=true;
                for(int i=0;i<100;i++)pile.items.add(new Food().quantity(i+1));
                WndInfoPile window=new WndInfoPile(pile);
                Game.scene().add(window);
                ScrollPane pane=find(window,ScrollPane.class);
                assertNotNull("native pile has scrollable content",pane);
                assertTrue("100 items must scroll",pane.content().height()>pane.height());
                assertTrue("native window fits current screen",pane.height()<com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera.height);
                pane.scrollTo(0,0);
                point.set(pane.content().camera().cameraToScreen(pane.width()/2,12));
            }catch(Exception e){throw new RuntimeException(e);}});
            assertNotNull(point.get());
            Point p=point.get();
            PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,72,PointerEvent.Type.DOWN,PointerEvent.LEFT));
            Thread.sleep(140);
            PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,72,PointerEvent.Type.UP,PointerEvent.LEFT));
            Thread.sleep(600);
            onRender(()->{try{
                assertNotNull("tapping an item opens its ordinary description",find(Game.scene(),WndInfoItem.class));
            }catch(Exception e){throw new RuntimeException(e);}});
        }
    }
}
