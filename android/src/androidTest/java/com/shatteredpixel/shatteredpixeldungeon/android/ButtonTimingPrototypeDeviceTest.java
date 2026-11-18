package com.shatteredpixel.shatteredpixeldungeon.android;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.*;
import com.watabou.utils.Signal;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Frozen340 diagnostic: no backend replacement, no direct clicks or synthetic edges. */
@RunWith(AndroidJUnit4.class)
public class ButtonTimingPrototypeDeviceTest {
 private static final Field PRESSED,TIME;
 static{try{PRESSED=Button.class.getDeclaredField("pressedButton");TIME=Button.class.getDeclaredField("pressTime");PRESSED.setAccessible(true);TIME.setAccessible(true);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 @Test public void discardingPreDownFrameTimePassesTheUnchangedNativeMenuRegression()throws Exception{
  AtomicBoolean alive=new AtomicBoolean(true),posting=new AtomicBoolean(false);
  AtomicReference<Scene> hooked=new AtomicReference<>();
  AtomicInteger corrected=new AtomicInteger();
  Signal.Listener<PointerEvent> observer=event->{
   if(event!=null&&event.type==PointerEvent.Type.DOWN){
    float credit=Game.elapsed;
    long id=event.diagnosticId;
    Gdx.app.postRunnable(()->{try{
     Object button=PRESSED.get(null);
     if(button!=null&&credit>0){
      float before=TIME.getFloat(button);
      if(before>=credit){
       TIME.setFloat(button,before-credit);corrected.incrementAndGet();
       Log.w("APDTAPPROTO","discard_pre_down_credit_ms="+credit*1000+" before_ms="+before*1000+" input="+id+" button="+button.getClass().getName());
      }
     }
    }catch(Exception e){throw new RuntimeException(e);}});
   }
   return false;
  };
  Thread watcher=new Thread(()->{
   while(alive.get()){
    Scene scene=Game.instance==null?null:Game.scene();
    if(scene instanceof TitleScene&&scene!=hooked.get()&&posting.compareAndSet(false,true)){
     Game.runOnRenderThread(()->{
      Scene current=Game.scene();
      if(alive.get()&&current instanceof TitleScene&&current!=hooked.get()){
       org.junit.Assert.assertTrue("First frame cannot itself reach the hold threshold",Button.longClick>.2f);
       current.add(new Gizmo(){
        @Override public void update(){PointerEvent.removePointerListener(observer);PointerEvent.addPointerListener(observer);}
        @Override public void destroy(){PointerEvent.removePointerListener(observer);super.destroy();}
       });
       PointerEvent.removePointerListener(observer);PointerEvent.addPointerListener(observer);hooked.set(current);
       Log.w("APDTAPPROTO","passive timing observer attached; original InputHandler and Android Back ownership unchanged");
      }
      posting.set(false);
     });
    }
    if(!(scene instanceof TitleScene))hooked.set(null);
    try{Thread.sleep(20);}catch(InterruptedException done){return;}
   }
  },"APD-passive-timing-prototype");
  watcher.start();
  try{
   new TitleMenuLayoutDeviceTest().titleControlsStaySeparateAndNativeChangesAndHideUiRemainUsable();
   org.junit.Assert.assertTrue("Real native presses exercised pre-DOWN credit correction",corrected.get()>0);
  }finally{
   alive.set(false);watcher.interrupt();watcher.join(3000);
   Log.w("APDTAPPROTO","prototype_only corrected="+corrected.get()+"; frozen APK unchanged; not shipped340 acceptance");
  }
 }
}
