package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings;
import com.watabou.noosa.*;
import com.watabou.utils.Point;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ScaleChangeDeviceTest {
    private static CountDownLatch creating,release;
    private static final AtomicInteger clicks=new AtomicInteger();
    public static class BlockedTitle extends TitleScene {
        @Override public void create(){
            super.create();
            RedButton target=new RedButton("Input target"){
                @Override protected void onClick(){clicks.incrementAndGet();}
            };
            target.setRect(0,0,Camera.main.width,Camera.main.height);addToFront(target);
            creating.countDown();
            try{if(!release.await(20,TimeUnit.SECONDS))throw new AssertionError("Test did not release blocked title");}
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
        }
    }
    private static Object field(Object object,String name)throws Exception {
        for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass()){
            try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException ignored){}
        }
        throw new NoSuchFieldException(name);
    }
    private static void waitFor(Callable<Boolean> ready)throws Exception{
        long end=SystemClock.uptimeMillis()+120000;
        while(SystemClock.uptimeMillis()<end){if(ready.call())return;Thread.sleep(100);}
        fail("Timed out waiting for completed scene");
    }
    private static void completedTitle()throws Exception{
        waitFor(()->{
            boolean[] ready={false};TopScoresDeviceTest.render(()->{try{
                ready[0]=Game.scene() instanceof TitleScene&&!Game.switchingScene()
                    &&!((Boolean)field(Game.instance,"awaitingScenePresentation"));
            }catch(Exception e){throw new RuntimeException(e);}});return ready[0];
        });
        TopScoresDeviceTest.render(()->{});
    }
    private static void tap(int x,int y)throws Exception{gesture(x,y,false);}
    private static void gesture(int x,int y,boolean cancel)throws Exception{
        long down=SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,cancel?MotionEvent.ACTION_CANCEL:MotionEvent.ACTION_UP}){
            MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            try{assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(event,true));}
            finally{event.recycle();}
            if(action==MotionEvent.ACTION_DOWN)Thread.sleep(80);
        }
    }
    @Test public void nativeTapQueuedDuringTitleBuildIsDiscardedButFreshTapWorks()throws Exception{
        creating=new CountDownLatch(1);release=new CountDownLatch(1);clicks.set(0);
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            waitFor(()->Game.instance!=null&&Game.scene()!=null);
            TopScoresDeviceTest.render(()->Game.switchScene(BlockedTitle.class));
            assertTrue(creating.await(60,TimeUnit.SECONDS));
            try{tap(Game.width/2,Game.height/2);}finally{release.countDown();}
            completedTitle();assertEquals("No tap from the invisible/loading scene may activate its controls",0,clicks.get());
            tap(Game.width/2,Game.height/2);
            waitFor(()->clicks.get()>0);assertEquals("A fresh tap executes once",1,clicks.get());
        }finally{release.countDown();}
    }
    /** Dedicated high-resolution display workflow runs this actual 7x -> 6x control path. */
    @Test public void nativeSevenToSixScaleChangePreservesSettingsAndCoversViewport()throws Exception{
        int oldScale=0,oldIndex=WndSettings.last_index;boolean havePreferences=false;
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            waitFor(()->Game.instance!=null&&Game.scene()!=null);
            oldScale=SPDSettings.scale();havePreferences=true;
            assertTrue("Use the explicit 1216x2688 profile",PixelScene.maxDefaultZoom>=7);
            TopScoresDeviceTest.render(()->{SPDSettings.scale(7);ShatteredPixelDungeon.switchNoFade(TitleScene.class);});
            completedTitle();assertEquals(7,PixelScene.defaultZoom);
            TopScoresDeviceTest.screenshot("scale-phone-seven.png");
            Point[] target={null};
            TopScoresDeviceTest.render(()->{try{
                WndSettings.last_index=1;WndSettings settings=new WndSettings();Game.scene().addToFront(settings);
                OptionSlider slider=(OptionSlider)field(field(settings,"ui"),"optUIScale");assertNotNull(slider);
                int min=((Number)field(slider,"minVal")).intValue();
                float tick=((Number)field(slider,"tickDist")).floatValue();
                target[0]=slider.camera().cameraToScreen(slider.left()+tick*(6-min)+1,slider.bottom()-5);
            }catch(Exception e){throw new RuntimeException(e);}});
            gesture(target[0].x,target[0].y,true);
            TopScoresDeviceTest.render(()->{});TopScoresDeviceTest.render(()->{});
            assertEquals("Cancellation must not commit another scale",7,SPDSettings.scale());assertEquals(7,PixelScene.defaultZoom);
            tap(target[0].x,target[0].y);
            waitFor(()->PixelScene.defaultZoom==6);completedTitle();assertEquals(6,SPDSettings.scale());
            TopScoresDeviceTest.render(()->{try{
                java.util.List<?> children=(java.util.List<?>)field(Game.scene(),"members");
                for(Object child:new java.util.ArrayList<Object>(children))if(child instanceof WndSettings)((WndSettings)child).hide();
                Image plate=(Image)children.get(1);
                assertTrue(plate.x<=.01f&&plate.y<=.01f);
                assertTrue(plate.x+plate.width()+.01f>=Camera.main.width&&plate.y+plate.height()+.01f>=Camera.main.height);
                assertEquals(Math.round(plate.scale.x*Camera.main.zoom),plate.scale.x*Camera.main.zoom,.001f);
            }catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot("scale-phone-six.png");
        }finally{if(havePreferences)SPDSettings.scale(oldScale);WndSettings.last_index=oldIndex;}
    }
}
