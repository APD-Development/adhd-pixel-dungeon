package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Point;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.watabou.input.PointerEvent;
import com.watabou.utils.Signal;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TitleMenuLayoutDeviceTest {
    private static Object field(Object object,String name)throws Exception {
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass()){
            try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}
            catch(NoSuchFieldException inherited){}
        }
        throw new NoSuchFieldException(name);
    }
    private static void waitFor(java.util.concurrent.Callable<Boolean> condition)throws Exception {
        long end=SystemClock.uptimeMillis()+120000;
        while(SystemClock.uptimeMillis()<end){if(condition.call())return;Thread.sleep(150);}
        fail("Native title condition did not become true");
    }
    private static void nativeTap(Point p)throws Exception {
        // Camera points are local to the live GDX content surface. Native injection is in
        // absolute display coordinates, which differ when Android exposes a cutout/inset.
        int[] contentOrigin=new int[2];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            android.view.View content=AndroidLauncher.instance.findViewById(android.R.id.content);
            assertNotNull("Android content view",content);
            content.getLocationOnScreen(contentOrigin);
        });
        Point devicePoint=new Point(p.x+contentOrigin[0],p.y+contentOrigin[1]);
        Log.i("APDTitleNative","native-tap local="+p.x+","+p.y+" contentOrigin="
                +contentOrigin[0]+","+contentOrigin[1]+" device="+devicePoint.x+","+devicePoint.y);
        long down=SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
            MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,devicePoint.x,devicePoint.y,0);
            e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            try{assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(e,action==MotionEvent.ACTION_UP));}
            finally{e.recycle();}
            if(action==MotionEvent.ACTION_DOWN)Thread.sleep(80);
        }
    }
    private static void performSystemBack(){
        assertTrue("Android accessibility service must accept the system Back request",
                InstrumentationRegistry.getInstrumentation().getUiAutomation().performGlobalAction(
                        android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK));
    }
    private static Point centre(String name)throws Exception {
        Point[] p=new Point[1];
        TopScoresDeviceTest.render(()->{try{
            Component b=(Component)field(Game.scene(),name);
            p[0]=b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2);
        }catch(Exception e){throw new RuntimeException(e);}});return p[0];
    }
    private static boolean fullyRestored()throws Exception {
        boolean[] ready={false};
        TopScoresDeviceTest.render(()->{try{
            if(Game.scene() instanceof TitleScene){
                ready[0]=((com.watabou.noosa.Visual)field(Game.scene(),"logo")).am>=1f
                        && ((Component)field(Game.scene(),"btnFade")).active
                        && ((Component)field(Game.scene(),"btnChanges")).active;
            }
        }catch(Exception e){throw new RuntimeException(e);}});
        return ready[0];
    }
    private static com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings waitForSettingsDraw(
            com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings previous)throws Exception {
        AtomicInteger drawn=new AtomicInteger();
        AtomicReference<com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings> watching=new AtomicReference<>();
        waitFor(()->{
            TopScoresDeviceTest.render(()->{try{
                List<com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings> windows=
                        TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings.class);
                assertTrue("At most one actual settings window",windows.size()<=1);
                if(windows.size()==1 && windows.get(0)!=previous){
                    if(watching.compareAndSet(null,windows.get(0))){
                        windows.get(0).add(new com.watabou.noosa.Gizmo(){
                            @Override public void draw(){super.draw();drawn.incrementAndGet();}
                        });
                    }else assertSame("Settings window changed before its second draw",watching.get(),windows.get(0));
                }
            }catch(Exception e){throw new RuntimeException(e);}});
            return watching.get()!=null&&drawn.get()>=2;
        });
        return watching.get();
    }
    private static void waitForSettingsClosed(ActivityScenario<AndroidLauncher> scenario)throws Exception {
        waitFor(()->{
            assertEquals("Back must not leave APD while closing Settings",
                    androidx.lifecycle.Lifecycle.State.RESUMED,scenario.getState());
            AtomicInteger open=new AtomicInteger();
            TopScoresDeviceTest.render(()->{try{
                List<com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings> windows=
                        TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings.class);
                assertTrue("At most one actual settings window",windows.size()<=1);
                open.set(windows.size());
            }catch(Exception e){throw new RuntimeException(e);}});
            return open.get()==0;
        });
        Thread.sleep(300);
        assertEquals("Closing Settings must keep APD resumed",
                androidx.lifecycle.Lifecycle.State.RESUMED,scenario.getState());
    }
    @Test public void titleControlsStaySeparateAndNativeChangesAndHideUiRemainUsable()throws Exception {
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
            waitFor(()->Game.scene()!=null && PixelScene.uiCamera!=null);
            Thread.sleep(1000);
            TopScoresDeviceTest.render(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));
            waitFor(()->Game.scene() instanceof TitleScene);Thread.sleep(1000);
            TopScoresDeviceTest.render(()->{try{
                List<Component> buttons=new ArrayList<>();
                for(String name:new String[]{"btnPlay","btnStatistics","btnBadges","btnJournal","btnOptions","btnQuit","btnAPDSettings","btnCommunity","btnNews","btnChanges","btnAbout","btnFade"}){
                    Component b=(Component)field(Game.scene(),name);buttons.add(b);
                    assertTrue(name+" visible and enabled",b.visible && b.active);
                    assertTrue(name+" inside actual viewport",b.left()>=0 && b.top()>=0 && b.right()<=b.camera().width+0.01 && b.bottom()<=b.camera().height+0.01);
                    assertTrue(name+" usable target",b.width()>=16 && b.height()>=16);
                    if(b instanceof StyledButton){
                        Object frame=field(b,"bg");
                        assertEquals(name+" uses only the actual button frame, excluding the lower swatch",18f,((Number)field(frame,"nHeight")).floatValue(),0f);
                        com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock text=(com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock)field(b,"text");
                        assertTrue(name+" label fits horizontally",text.left()>=b.left()-0.5 && text.right()<=b.right()+0.5);
                        assertTrue(name+" label fits vertically",text.top()>=b.top()-0.5 && text.bottom()<=b.bottom()+0.5);
                    }
                }
                for(int i=0;i<buttons.size();i++)for(int j=i+1;j<buttons.size();j++){
                    Component a=buttons.get(i),b=buttons.get(j);
                    assertFalse("Main-menu controls overlap: "+i+"/"+j,a.left()<b.right() && b.left()<a.right() && a.top()<b.bottom() && b.top()<a.bottom());
                }
            }catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot("main-title-layout.png");
            nativeTap(centre("btnChanges"));waitFor(()->Game.scene() instanceof APDTextScene);
            assertEquals(APDTextScene.Page.CHANGES,APDTextScene.page);
            performSystemBack();
            waitFor(TitleMenuLayoutDeviceTest::fullyRestored);
            if(android.os.Build.VERSION.SDK_INT>=33){
                assertTrue("Registered Android callback owns Back",com.watabou.input.BackGate.systemBackOwned());
                // Replay the observed late libGDX copy after an actual OS Back.
                // Timing-window-only deduplication wrongly closes Title here.
                Thread.sleep(356);
                AtomicInteger frames=new AtomicInteger();
                TopScoresDeviceTest.render(()->{
                    assertTrue(com.badlogic.gdx.Gdx.input.getInputProcessor().keyDown(com.badlogic.gdx.Input.Keys.BACK));
                    assertTrue(com.badlogic.gdx.Gdx.input.getInputProcessor().keyUp(com.badlogic.gdx.Input.Keys.BACK));
                    Game.scene().add(new com.watabou.noosa.Gizmo(){
                        @Override public void draw(){super.draw();frames.incrementAndGet();}
                    });
                });
                waitFor(()->frames.get()>=2);
                assertTrue("Delayed legacy Back must preserve the returned title",fullyRestored());
                InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                    assertFalse("One Back must not finish the Activity",AndroidLauncher.instance.isFinishing());
                    assertFalse(AndroidLauncher.instance.isDestroyed());
                });
            }
            nativeTap(centre("btnFade"));Thread.sleep(900);
            TopScoresDeviceTest.render(()->{try{assertFalse(((Component)field(Game.scene(),"btnChanges")).active);}catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot("main-title-hidden.png");
            Point[] reset=new Point[1];TopScoresDeviceTest.render(()->reset[0]=com.watabou.noosa.Camera.main.cameraToScreen(com.watabou.noosa.Camera.main.width/2f,com.watabou.noosa.Camera.main.height/2f));
            nativeTap(reset[0]);waitFor(TitleMenuLayoutDeviceTest::fullyRestored);
            TopScoresDeviceTest.render(()->{try{assertTrue(((Component)field(Game.scene(),"btnChanges")).active);}catch(Exception e){throw new RuntimeException(e);}});
            TopScoresDeviceTest.screenshot("main-title-restored.png");
            // A physical finger can land beside the tiny icon. This native press must still
            // hide the menu and allow the existing restore behavior to run afterward.
            Point[] outerEdge=new Point[1];
            String[] outerGeometry=new String[1];
            AtomicInteger observedDown=new AtomicInteger(), observedUp=new AtomicInteger();
            Signal.Listener<PointerEvent> observer=event->{
                if(event!=null && event.type!=PointerEvent.Type.HOVER){
                    Log.i("APDTitleNative","expanded-target event="+event.type+" x="+event.current.x+" y="+event.current.y+" expected="+outerEdge[0]);
                    if(Math.abs(event.current.x-outerEdge[0].x)<=2 && Math.abs(event.current.y-outerEdge[0].y)<=2){
                        if(event.type==PointerEvent.Type.DOWN)observedDown.incrementAndGet();
                        if(event.type==PointerEvent.Type.UP)observedUp.incrementAndGet();
                    }
                }
                return false;
            };
            TopScoresDeviceTest.render(()->{try{
                Component icon=(Component)field(Game.scene(),"btnFade");
                com.watabou.noosa.PointerArea target=(com.watabou.noosa.PointerArea)field(icon,"hotArea");
                assertTrue("Footer hit area reaches outside the visible icon",target.x+4<icon.left());
                assertTrue("Footer hit area extends upward",target.y<icon.top());
                outerEdge[0]=target.camera().cameraToScreen(icon.left()-2,target.y+target.height/2);
                Point iconTopLeft=icon.camera().cameraToScreen(icon.left(),icon.top());
                Point iconBottomRight=icon.camera().cameraToScreen(icon.right(),icon.bottom());
                assertTrue("Diagnostic point must remain outside the visible icon",
                        outerEdge[0].x<Math.min(iconTopLeft.x,iconBottomRight.x)
                                || outerEdge[0].x>Math.max(iconTopLeft.x,iconBottomRight.x)
                                || outerEdge[0].y<Math.min(iconTopLeft.y,iconBottomRight.y)
                                || outerEdge[0].y>Math.max(iconTopLeft.y,iconBottomRight.y));
                assertTrue("Diagnostic point must be inside the expanded target",target.overlapsScreenPoint(outerEdge[0].x,outerEdge[0].y));
                outerGeometry[0]="raw="+outerEdge[0]+" icon="+icon.left()+","+icon.top()+","+icon.right()+","+icon.bottom()
                        +" target="+target.x+","+target.y+","+(target.x+target.width)+","+(target.y+target.height)
                        +" camera="+target.camera().width+"x"+target.camera().height+" zoom="+target.camera().zoom;
                Log.i("APDTitleNative","expanded-target "+outerGeometry[0]);
                PointerEvent.addPointerListener(observer);
            }catch(Exception e){throw new RuntimeException(e);}});
            boolean[] stillActive={true};
            try{
                nativeTap(outerEdge[0]);
                long diagnosticEnd=SystemClock.uptimeMillis()+10000;
                do{
                    TopScoresDeviceTest.render(()->{try{
                        stillActive[0]=((Component)field(Game.scene(),"btnChanges")).active;
                    }catch(Exception e){throw new RuntimeException(e);}});
                    if(!stillActive[0])break;
                    Thread.sleep(100);
                }while(SystemClock.uptimeMillis()<diagnosticEnd);
                assertFalse("Outside-icon touch must hide UI; observedDown="+observedDown.get()
                        +" observedUp="+observedUp.get()+" "+outerGeometry[0],stillActive[0]);
            }finally{
                TopScoresDeviceTest.render(()->PointerEvent.removePointerListener(observer));
            }
            nativeTap(reset[0]);waitFor(TitleMenuLayoutDeviceTest::fullyRestored);
            TopScoresDeviceTest.render(()->{try{
                assertTrue("UI must restore after expanded touch",((Component)field(Game.scene(),"btnChanges")).active);
            }catch(Exception e){throw new RuntimeException(e);}});
            final boolean waterWas=com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmWaterskin();
            final boolean trapWas=com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmKnownTraps();
            try{
                TopScoresDeviceTest.render(()->{com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmWaterskin(true);com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmKnownTraps(true);com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings.last_index=1;});
                nativeTap(centre("btnOptions"));
                com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings firstSettings=waitForSettingsDraw(null);
                Point[] toggles=new Point[2];
                String[] toggleGeometry=new String[2];
                TopScoresDeviceTest.render(()->{try{
                    List<com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox> boxes=TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox.class);
                    for(com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox b:boxes){
                        int index=b.text().equals("Confirm Waterskin")?0:b.text().equals("Warn about traps")?1:-1;
                        if(index>=0){
                            assertTrue(b.checked());assertTrue(b.visible && b.active);
                            toggles[index]=b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2);
                            Point screenTopLeft=b.camera().cameraToScreen(b.left(),b.top());
                            Point screenBottomRight=b.camera().cameraToScreen(b.right(),b.bottom());
                            toggleGeometry[index]="label="+b.text()+" tap="+toggles[index].x+","+toggles[index].y
                                    +" logical="+b.left()+","+b.top()+","+b.right()+","+b.bottom()
                                    +" screen="+screenTopLeft.x+","+screenTopLeft.y+","+screenBottomRight.x+","+screenBottomRight.y
                                    +" camera="+b.camera().width+"x"+b.camera().height+" zoom="+b.camera().zoom;
                            Log.i("APDTitleNative","settings-toggle "+toggleGeometry[index]);
                        }
                    }
                    assertNotNull(toggles[0]);assertNotNull(toggles[1]);
                }catch(Exception e){throw new RuntimeException(e);}});
                TopScoresDeviceTest.screenshot("options-confirmations.png");
                for(int toggleIndex=0;toggleIndex<2;toggleIndex++){
                    final int index=toggleIndex;
                    if(index>0){
                        waitForSettingsDraw(null);
                        TopScoresDeviceTest.render(()->{try{
                            for(com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox b:
                                    TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox.class))
                                if(b.text().equals("Warn about traps")){
                                    toggles[index]=b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2);
                                    toggleGeometry[index]+=" settledTap="+toggles[index].x+","+toggles[index].y;
                                }
                        }catch(Exception e){throw new RuntimeException(e);}});
                    }
                    AtomicInteger toggleDown=new AtomicInteger(),toggleUp=new AtomicInteger();
                    Signal.Listener<PointerEvent> toggleObserver=event->{
                        if(event!=null && event.type!=PointerEvent.Type.HOVER){
                            Log.i("APDTitleNative","settings-toggle event="+event.type+" x="+event.current.x+" y="+event.current.y+" expected="+toggles[index].x+","+toggles[index].y);
                            if(Math.abs(event.current.x-toggles[index].x)<=2 && Math.abs(event.current.y-toggles[index].y)<=2){
                                if(event.type==PointerEvent.Type.DOWN)toggleDown.incrementAndGet();
                                if(event.type==PointerEvent.Type.UP)toggleUp.incrementAndGet();
                            }
                        }
                        return false;
                    };
                    TopScoresDeviceTest.render(()->PointerEvent.addPointerListener(toggleObserver));
                    boolean changed=false;
                    try{
                        nativeTap(toggles[index]);
                        long diagnosticEnd=SystemClock.uptimeMillis()+10000;
                        do{
                            changed=index==0
                                    ? !com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmWaterskin()
                                    : !com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmKnownTraps();
                            if(changed)break;
                            Thread.sleep(100);
                        }while(SystemClock.uptimeMillis()<diagnosticEnd);
                        assertTrue("Native settings toggle did not change; observedDown="+toggleDown.get()
                                +" observedUp="+toggleUp.get()+" "+toggleGeometry[index],changed);
                    }finally{
                        TopScoresDeviceTest.render(()->PointerEvent.removePointerListener(toggleObserver));
                    }
                    if(index==0)assertTrue(com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmKnownTraps());
                }
                assertFalse(com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmWaterskin());
                assertFalse(com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmKnownTraps());
                performSystemBack();
                waitForSettingsClosed(scenario);
                waitFor(()->Game.scene() instanceof TitleScene);
                nativeTap(centre("btnOptions"));
                com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings reopenedSettings=waitForSettingsDraw(firstSettings);
                assertNotSame("Settings must reopen as a new window",firstSettings,reopenedSettings);
                TopScoresDeviceTest.render(()->{try{
                    for(com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox b:TopScoresDeviceTest.find(Game.scene(),com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox.class))
                        if(b.text().equals("Confirm Waterskin") || b.text().equals("Warn about traps"))assertFalse("Preference retained on reopening",b.checked());
                }catch(Exception e){throw new RuntimeException(e);}});
                performSystemBack();
                waitForSettingsClosed(scenario);
            }finally{
                if(scenario.getState()==androidx.lifecycle.Lifecycle.State.RESUMED){
                    TopScoresDeviceTest.render(()->{com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmWaterskin(waterWas);com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmKnownTraps(trapWas);});
                }else{
                    // Preference cleanup must not mask a real Activity-lifecycle failure.
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmWaterskin(waterWas);
                    com.shatteredpixel.shatteredpixeldungeon.SPDSettings.confirmKnownTraps(trapWas);
                }
            }

        }
    }
}


// Diagnostic helper dependency corrected; all native assertions unchanged.
