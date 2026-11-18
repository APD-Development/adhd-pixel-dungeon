package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.lifecycle.Lifecycle;
import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.os.SystemClock;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.windows.*;
import com.watabou.noosa.Game;
import com.watabou.utils.Point;
import com.watabou.utils.SupportDiagnostics;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DiagnosticReportDeviceTest {
    private static void render(Runnable action)throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
        Game.runOnRenderThread(()->{try{action.run();}catch(Throwable t){error.set(t);}finally{done.countDown();}});
        assertTrue(done.await(60,TimeUnit.SECONDS));if(error.get()!=null)throw new AssertionError(error.get());
    }
    private static void waitFor(Callable<Boolean> ready)throws Exception {
        long end=SystemClock.uptimeMillis()+120000;while(SystemClock.uptimeMillis()<end){if(ready.call())return;Thread.sleep(100);}fail("Timed out waiting for rendered application");
    }
    private static void awaitResumedTitle()throws Exception {
        AtomicReference<com.watabou.noosa.Scene> observed=new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger frames=new java.util.concurrent.atomic.AtomicInteger();
        java.lang.reflect.Field firstDraw=Game.class.getDeclaredField("awaitingFirstDraw");firstDraw.setAccessible(true);
        waitFor(()->{
            render(()->{try{
                boolean stable=Game.scene() instanceof TitleScene&&!Game.switchingScene()
                        &&Game.scene()==observed.get()&&!firstDraw.getBoolean(Game.instance)
                        &&Game.width==com.badlogic.gdx.Gdx.graphics.getWidth()
                        &&Game.height==com.badlogic.gdx.Gdx.graphics.getHeight();
                frames.set(stable?frames.get()+1:0);observed.set(Game.scene());
            }catch(Exception e){throw new RuntimeException(e);}});
            return frames.get()>=2;
        });
    }
    @Test public void actualReportButtonSavesDeviceEvidenceAndMenusSurvivePauseResume()throws Exception {
        ApdFiles.Provider original=ApdFiles.provider;Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Uri[] saved={null};CountDownLatch exported=new CountDownLatch(1);AtomicReference<String> failure=new AtomicReference<>();
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)) {
            waitFor(()->Game.instance!=null&&Game.scene()!=null);
            render(()->Game.switchScene(TitleScene.class));waitFor(()->Game.scene() instanceof TitleScene);
            awaitResumedTitle();
            for(int i=0;i<3;i++){
                scenario.moveToState(Lifecycle.State.CREATED);scenario.moveToState(Lifecycle.State.RESUMED);
                awaitResumedTitle();
            }
            ApdFiles.provider=new ApdFiles.Provider(){
                public void chooseExport(String n,ApdFiles.ExportTarget t){throw new AssertionError("Expected text MIME");}
                public void chooseExport(String n,String mime,ApdFiles.ExportTarget t){
                    try{
                        assertEquals("text/plain",mime);ContentValues values=new ContentValues();
                        values.put(MediaStore.MediaColumns.DISPLAY_NAME,n);values.put(MediaStore.MediaColumns.MIME_TYPE,mime);
                        saved[0]=context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);assertNotNull(saved[0]);
                        AndroidFileProvider.deliverExport(context.getContentResolver(),new ApdFiles.ExportTarget(){
                            public void onStream(OutputStream out,String name){
                                t.onStream(new java.io.FilterOutputStream(out){@Override public void close()throws IOException{try{super.close();}finally{exported.countDown();}}},name);
                            }
                            public void onCancelled(){failure.set("unexpected cancellation");exported.countDown();t.onCancelled();}
                            public void onFailed(String why){failure.set(why);exported.countDown();t.onFailed(why);}
                        },Activity.RESULT_OK,new Intent().setData(saved[0]));
                    }catch(Throwable e){failure.set(e.toString());exported.countDown();}
                }
                public void chooseImport(ApdFiles.ImportSource s){throw new AssertionError("No imports");}
                public String where(){return "the test's own Downloads document";}
            };
            AtomicReference<Point> touch=new AtomicReference<>();
            AtomicReference<WndAPDSettings> opened=new AtomicReference<>();
            render(()->{try{
                WndAPDSettings window=new WndAPDSettings();Game.scene().addToFront(window);
                opened.set(window);
                java.lang.reflect.Field field=WndAPDSettings.class.getDeclaredField("pane");field.setAccessible(true);
                assertNull(((APDSettingsPane)field.get(window)).section());
                for(RedButton button:TopScoresDeviceTest.find(window,RedButton.class))if("Create bug report".equals(button.text())){
                    assertTrue(button.active&&button.visible);
                    java.lang.reflect.Field scrollField=WndAPDSettings.class.getDeclaredField("scroll");scrollField.setAccessible(true);
                    ScrollPane scroll=(ScrollPane)scrollField.get(window);
                    scroll.scrollTo(0,Math.max(0,button.top()-20));
                    float top=scroll.content().camera().scroll.y,bottom=top+scroll.height();
                    assertTrue(button.centerY()>=top&&button.centerY()<=bottom);
                    touch.set(button.camera().cameraToScreen(button.centerX(),button.centerY()));
                }
                assertNotNull("Diagnostic report button is visible at APD Settings root",touch.get());
            }catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(400);
            render(()->{try{
                assertTrue("Settings must still be attached after its completed draw",TopScoresDeviceTest.find(Game.scene(),WndAPDSettings.class).contains(opened.get()));
                for(RedButton button:TopScoresDeviceTest.find(opened.get(),RedButton.class))if("Create bug report".equals(button.text())){
                    assertTrue(button.active&&button.visible);touch.set(button.camera().cameraToScreen(button.centerX(),button.centerY()));
                    assertTrue(touch.get().x>=0&&touch.get().x<Game.width&&touch.get().y>=0&&touch.get().y<Game.height);
                }
            }catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot("diagnostic-report-button.png");
            long now=SystemClock.uptimeMillis();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
                MotionEvent event=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,touch.get().x,touch.get().y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                try{assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(event,true));}finally{event.recycle();}
                if(action==MotionEvent.ACTION_DOWN)Thread.sleep(100);
            }
            assertTrue("Diagnostic export finishes",exported.await(60,TimeUnit.SECONDS));assertNull(failure.get());
            String report;
            try(InputStream in=context.getContentResolver().openInputStream(saved[0]);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                assertNotNull(in);byte[] block=new byte[4096];int n;while((n=in.read(block))!=-1)bytes.write(block,0,n);report=bytes.toString(StandardCharsets.UTF_8.name());
            }
            assertTrue(report.startsWith("APD SUPPORT REPORT v1"));assertTrue(report.contains(Game.version));
            assertTrue(report.contains(android.os.Build.MODEL));assertTrue(report.contains("Application ID: "+context.getPackageName()));
            assertTrue(report.matches("(?s).*APK SHA-256 \\(installed base\\): [0-9a-f]{64}.*"));
            assertTrue(report.contains("game.pause"));assertTrue(report.contains("game.resume"));
            assertTrue(report.contains("input.queued"));assertTrue(report.contains("input.dispatched"));assertTrue(report.contains("app_queue_ms="));
            assertTrue(report.contains("Java heap bytes"));assertTrue(report.contains("ANDROID MEMORY AND PROCESS EXIT EVIDENCE"));
            TopScoresDeviceTest.screenshot("diagnostic-report-saved.png");
        }finally{
            ApdFiles.provider=original;
            if(saved[0]!=null)context.getContentResolver().delete(saved[0],null,null); // Only this test-created document.
        }
    }
}
