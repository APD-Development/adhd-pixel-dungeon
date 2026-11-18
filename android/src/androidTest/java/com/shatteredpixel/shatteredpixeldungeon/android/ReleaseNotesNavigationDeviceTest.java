package com.shatteredpixel.shatteredpixeldungeon.android;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.apd.history.ApdHistory;
import com.shatteredpixel.shatteredpixeldungeon.apd.history.ApdReleaseNotes;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AboutScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.*;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Point;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual Android MotionEvents and system Back; no save, score or preference changes. */
@RunWith(AndroidJUnit4.class)
public class ReleaseNotesNavigationDeviceTest {
    private static void render(Runnable task)throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
        Game.runOnRenderThread(()->{try{task.run();}catch(Throwable t){failure.set(t);}finally{done.countDown();}});
        assertTrue(done.await(45,TimeUnit.SECONDS));if(failure.get()!=null)throw new AssertionError(failure.get());
    }
    @SuppressWarnings("unchecked") private static List<Gizmo> children(Group group)throws Exception {
        Field field=Group.class.getDeclaredField("members");field.setAccessible(true);
        return new ArrayList<>((List<Gizmo>)field.get(group));
    }
    private static Component labelled(Group group,String wanted)throws Exception {
        List<Gizmo> ordered=children(group);Collections.reverse(ordered);
        for(Gizmo child:ordered) {
            if(child instanceof StyledButton&&wanted.equals(((StyledButton)child).text()))return (Component)child;
            if(child instanceof RenderedTextBlock&&wanted.equals(((RenderedTextBlock)child).text()))return (Component)child;
            if(child instanceof Group){Component match=labelled((Group)child,wanted);if(match!=null)return match;}
        }
        return null;
    }
    private static void tap(ActivityScenario<AndroidLauncher> scenario,String label)throws Exception {
        AtomicReference<Point> position=new AtomicReference<>();
        render(()->{try {
            Component control=labelled(Game.scene(),label);assertNotNull(label,control);
            assertTrue(control.visible);assertTrue(control.width()>0&&control.height()>0);
            position.set(control.camera().cameraToScreen(control.centerX(),control.centerY()));
        }catch(Exception e){throw new RuntimeException(e);}});
        Point point=position.get();AtomicBoolean accepted=new AtomicBoolean();long down=SystemClock.uptimeMillis();
        scenario.onActivity(activity->{MotionEvent event=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,point.x,point.y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);accepted.set(activity.dispatchTouchEvent(event));event.recycle();});
        assertTrue("Android accepted touch down on "+label,accepted.get());Thread.sleep(150);
        accepted.set(false);
        scenario.onActivity(activity->{MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,point.x,point.y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);accepted.set(activity.dispatchTouchEvent(event));event.recycle();});
        assertTrue("Android accepted touch up on "+label,accepted.get());Thread.sleep(600);
    }
    private static void scene(Class<?> type)throws Exception {
        long end=System.currentTimeMillis()+15000;while(!type.isInstance(Game.scene())&&System.currentTimeMillis()<end)Thread.sleep(100);
        assertTrue(type.getName(),type.isInstance(Game.scene()));Thread.sleep(400);
    }
    private static void mode(boolean technical)throws Exception {
        render(()->{try {Field field=APDTextScene.class.getDeclaredField("technicalHistory");field.setAccessible(true);assertEquals(technical,field.getBoolean(null));}catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void back()throws Exception {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(600);
    }
    private static void noWindows()throws Exception {
        render(()->{try {for(Gizmo child:children(Game.scene()))assertFalse("Back must close the menu",child instanceof Window&&child.alive);}catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void screenshot(String name)throws Exception {
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
        android.content.ContentValues values=new android.content.ContentValues();values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,name);values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"image/png");values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Pictures/APD-Test-Visuals");
        android.net.Uri destination=context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);assertNotNull(destination);
        try(OutputStream out=context.getContentResolver().openOutputStream(destination)){assertNotNull(out);assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}
    }
    @Test public void nativeTouchSwitchesHistoryModesAndSystemBackReturnsToAboutWithoutLosingRecords()throws Exception {
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)) {
            long ready=System.currentTimeMillis()+90000;while((Game.scene()==null||PixelScene.uiCamera==null)&&System.currentTimeMillis()<ready)Thread.sleep(100);
            assertNotNull(Game.scene());Thread.sleep(1500);
            final ApdHistory.Build latest=ApdHistory.builds(Gdx.files.internal("apd/history/index.tsv").readString("UTF-8")).get(0);
            final String technicalLabel=ApdReleaseNotes.technicalLabel(latest);
            render(()->{try {
                Field technical=APDTextScene.class.getDeclaredField("technicalHistory");technical.setAccessible(true);technical.setBoolean(null,false);
                Field selected=APDTextScene.class.getDeclaredField("buildSelected");selected.setAccessible(true);selected.setInt(null,0);
                ShatteredPixelDungeon.switchScene(AboutScene.class);
            }catch(Exception e){throw new RuntimeException(e);}});scene(AboutScene.class);
            tap(scenario,"APD Changes");scene(APDTextScene.class);mode(false);
            final com.watabou.noosa.Scene originalTextScene=Game.scene();
            tap(scenario,"Versions");screenshot("release-notes-navigation.png");
            tap(scenario,"Build records (technical)");scene(APDTextScene.class);mode(true);
            assertSame("technical mode must not recreate the text scene",originalTextScene,Game.scene());
            render(()->{try {assertNotNull(labelled(Game.scene(),technicalLabel));if(!latest.label.equals(technicalLabel))assertNull(labelled(Game.scene(),latest.label));}catch(Exception e){throw new RuntimeException(e);}});
            tap(scenario,"Versions");tap(scenario,"Release notes");scene(APDTextScene.class);mode(false);
            assertSame("release mode must not recreate the text scene",originalTextScene,Game.scene());
            tap(scenario,"Versions");tap(scenario,"Choose version");
            tap(scenario,"APD Alpha Preview - APD Alpha Preview");tap(scenario,"APD Alpha Preview");scene(APDTextScene.class);mode(false);noWindows();
            assertSame("version selection must not recreate the text scene",originalTextScene,Game.scene());
            tap(scenario,"Versions");back();scene(APDTextScene.class);noWindows();
            tap(scenario,"Versions");tap(scenario,"Build records (technical)");scene(APDTextScene.class);mode(true);
            back();scene(AboutScene.class);noWindows();mode(false);
            tap(scenario,"APD Changes");scene(APDTextScene.class);mode(false);
            render(()->{try {assertNotNull(labelled(Game.scene(),"APD Alpha Preview"));assertNull(labelled(Game.scene(),technicalLabel));}catch(Exception e){throw new RuntimeException(e);}});
            screenshot("release-notes-reopened.png");back();scene(AboutScene.class);
        }
    }
}
