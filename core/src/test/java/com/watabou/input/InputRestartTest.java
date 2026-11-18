package com.watabou.input;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.Signal;
import org.junit.Test;
import static org.junit.Assert.*;

public class InputRestartTest {
    @Test public void aNewInputHandlerMustNotReplayATapFromThePreviousHandler() {
        HeadlessTestSupport.start();
        new InputHandler(Gdx.input);
        PointerEvent.addPointerEvent(new PointerEvent(20,20,991,PointerEvent.Type.DOWN,PointerEvent.LEFT));
        PointerEvent.addPointerEvent(new PointerEvent(20,20,991,PointerEvent.Type.UP,PointerEvent.LEFT));
        new InputHandler(Gdx.input); // The real new-engine input initialization boundary.
        int[] dispatched={0};
        Signal.Listener<PointerEvent> listener=e->{dispatched[0]++;PointerEvent.clearKeyboardThisPress=false;return false;};
        PointerEvent.addPointerListener(listener);
        try { PointerEvent.processPointerEvents(); assertEquals("Old queued taps must never reach the reopened screen",0,dispatched[0]); }
        finally { PointerEvent.removePointerListener(listener); }
    }

    @Test public void freshTapsStillDispatchNormallyAfterReset() {
        HeadlessTestSupport.start();InputHandler.discardPendingEvents();int[] events={0};
        Signal.Listener<PointerEvent> listener=e->{events[0]++;PointerEvent.clearKeyboardThisPress=false;return false;};
        PointerEvent.addPointerListener(listener);
        try{
            PointerEvent.addPointerEvent(new PointerEvent(1,1,992,PointerEvent.Type.DOWN));
            PointerEvent.addPointerEvent(new PointerEvent(1,1,992,PointerEvent.Type.UP));
            PointerEvent.processPointerEvents();assertEquals(2,events[0]);
        }finally{PointerEvent.removePointerListener(listener);InputHandler.discardPendingEvents();}
    }

    @Test public void lifecycleResetCancelsHeldPointerWithoutSynthesizingUp() {
        HeadlessTestSupport.start();InputHandler.discardPendingEvents();int[] cancels={0},ups={0};
        Signal.Listener<PointerEvent> listener=e->{
            if(e!=null&&e.type==PointerEvent.Type.CANCEL)cancels[0]++;
            if(e!=null&&e.type==PointerEvent.Type.UP)ups[0]++;
            PointerEvent.clearKeyboardThisPress=false;return false;
        };
        PointerEvent.addPointerListener(listener);
        try{
            PointerEvent.addPointerEvent(new PointerEvent(1,1,993,PointerEvent.Type.DOWN));PointerEvent.processPointerEvents();
            InputHandler.discardPendingEvents();assertEquals(1,cancels[0]);assertEquals(0,ups[0]);
            InputHandler.discardPendingEvents();assertEquals(1,cancels[0]);
        }finally{PointerEvent.removePointerListener(listener);InputHandler.discardPendingEvents();}
    }

    @Test public void staleKeysAndWheelEventsAreDiscardedWithTouchEvents() {
        HeadlessTestSupport.start();InputHandler.discardPendingEvents();int[] keys={0},wheel={0};
        Signal.Listener<KeyEvent> k=e->{keys[0]++;return false;};Signal.Listener<ScrollEvent> s=e->{wheel[0]++;return false;};
        KeyEvent.addKeyListener(k);ScrollEvent.addScrollListener(s);
        try{
            KeyEvent.addKeyEvent(new KeyEvent(com.badlogic.gdx.Input.Keys.BACK,true));
            ScrollEvent.addScrollEvent(new ScrollEvent(new com.watabou.utils.PointF(1,1),1));
            InputHandler.discardPendingEvents();KeyEvent.processKeyEvents();ScrollEvent.processScrollEvents();
            assertEquals(0,keys[0]);assertEquals(0,wheel[0]);
        }finally{KeyEvent.removeKeyListener(k);ScrollEvent.removeScrollListener(s);InputHandler.discardPendingEvents();}
    }

    @Test public void backAfterAMenuTapMustWaitForTheRequestedSceneInsteadOfClosingTheOldOne(){
        HeadlessTestSupport.start();com.watabou.noosa.Game previous=com.watabou.noosa.Game.instance;
        ReadyGame game=new ReadyGame();InputHandler handler=new InputHandler(Gdx.input);int[] backs={0};
        Signal.Listener<PointerEvent> pointer=e->{
            PointerEvent.clearKeyboardThisPress=false;
            if(e!=null&&e.type==PointerEvent.Type.DOWN)com.watabou.noosa.Game.switchScene(com.watabou.noosa.Scene.class);
            return false;
        };
        Signal.Listener<KeyEvent> key=e->{backs[0]++;return false;};PointerEvent.addPointerListener(pointer);KeyEvent.addKeyListener(key);
        try{
            PointerEvent.addPointerEvent(new PointerEvent(1,1,997,PointerEvent.Type.DOWN));
            KeyEvent.addKeyEvent(new KeyEvent(com.badlogic.gdx.Input.Keys.BACK,true));
            handler.processAllEvents();assertEquals("Back cannot go to the old title after a menu transition was requested",0,backs[0]);
            game.transitionCompleted();handler.processAllEvents();assertEquals("The original Back press is preserved",1,backs[0]);
        }finally{PointerEvent.removePointerListener(pointer);KeyEvent.removeKeyListener(key);InputHandler.discardPendingEvents();com.watabou.noosa.Game.instance=previous;}
    }
    private static class ReadyGame extends com.watabou.noosa.Game {
        ReadyGame(){super(com.watabou.noosa.Scene.class,null);requestedReset=false;}
        void transitionCompleted(){requestedReset=false;}
    }

    @Test public void inputTraceRetainsTheBackendTimestampInsteadOfCallingAQueuedTapFresh(){
        HeadlessTestSupport.start();long nativeTime=System.nanoTime()-500000000L;
        com.badlogic.gdx.Input input=(com.badlogic.gdx.Input)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{com.badlogic.gdx.Input.class},(p,m,a)->{
            if(m.getName().equals("getCurrentEventTime"))return nativeTime;
            Class<?> t=m.getReturnType();if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;if(t==float.class)return 0f;return null;
        });
        InputHandler handler=new InputHandler(input);long[] captured={0};int oldW=com.watabou.noosa.Game.width,oldH=com.watabou.noosa.Game.height;
        Signal.Listener<PointerEvent> listener=e->{if(e!=null)captured[0]=e.sourceEventNanos;PointerEvent.clearKeyboardThisPress=false;return false;};
        PointerEvent.addPointerListener(listener);
        try{
            com.watabou.noosa.Game.width=100;com.watabou.noosa.Game.height=100;
            handler.touchDown(1,1,998,PointerEvent.LEFT);PointerEvent.processPointerEvents();assertEquals(nativeTime,captured[0]);
            String report=com.watabou.utils.SupportDiagnostics.report("");assertTrue(report.contains("source_nanos="+nativeTime));assertTrue(report.contains("app_queue_ms="));
        }finally{PointerEvent.removePointerListener(listener);InputHandler.discardPendingEvents();com.watabou.noosa.Game.width=oldW;com.watabou.noosa.Game.height=oldH;}
    }
    @Test public void backendTapsFromBlockedSceneCreationCannotActivateNewScene(){
        HeadlessTestSupport.start();long[] time={System.nanoTime()-4950000000L};
        com.badlogic.gdx.Input input=(com.badlogic.gdx.Input)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{com.badlogic.gdx.Input.class},(p,m,a)->{
            if(m.getName().equals("getCurrentEventTime"))return time[0];
            Class<?> t=m.getReturnType();if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;if(t==float.class)return 0f;return null;
        });
        InputHandler handler=new InputHandler(input);int[] downs={0},ups={0};
        int w=com.watabou.noosa.Game.width,h=com.watabou.noosa.Game.height;
        Signal.Listener<PointerEvent> listener=e->{if(e!=null){if(e.type==PointerEvent.Type.DOWN)downs[0]++;if(e.type==PointerEvent.Type.UP)ups[0]++;}PointerEvent.clearKeyboardThisPress=false;return false;};
        PointerEvent.addPointerListener(listener);
        try{
            com.watabou.noosa.Game.width=100;com.watabou.noosa.Game.height=100;
            handler.scenePresented(System.nanoTime());
            handler.touchDown(20,20,998,0);handler.touchUp(20,20,998,0);PointerEvent.processPointerEvents();
            assertEquals(0,downs[0]);assertEquals(0,ups[0]);
            time[0]=System.nanoTime();handler.touchUp(20,20,998,0);PointerEvent.processPointerEvents();assertEquals(0,ups[0]);
            handler.touchDown(20,20,998,0);handler.touchUp(20,20,998,0);PointerEvent.processPointerEvents();assertEquals(1,downs[0]);assertEquals(1,ups[0]);
            handler.scenePresented(System.nanoTime());
            // Controller-emulated touches have no backend timestamp; the last old native event must not suppress them.
            time[0]=System.nanoTime()-4950000000L;handler.emulateTouch(998,0,true);handler.emulateTouch(998,0,false);
            PointerEvent.processPointerEvents();assertEquals(2,downs[0]);assertEquals(2,ups[0]);
        }finally{PointerEvent.removePointerListener(listener);InputHandler.discardPendingEvents();com.watabou.noosa.Game.width=w;com.watabou.noosa.Game.height=h;}
    }


    @Test public void deliveredSystemBackSuppressesBothLegacyEdgesButRegistrationKeepsFallback(){
        HeadlessTestSupport.start();
        KeyBindings.addHardBinding(com.badlogic.gdx.Input.Keys.BACK,GameAction.BACK);
        InputHandler handler=new InputHandler(Gdx.input);
        Object activity=new Object();
        int[] backs={0};
        Signal.Listener<KeyEvent> listener=e->{backs[0]++;return false;};
        BackGate.setSystemBackOwner(activity);
        KeyEvent.addKeyListener(listener);
        try{
            assertTrue("System callback delivered the first Back",BackGate.acceptSystem());
            assertTrue(handler.keyDown(com.badlogic.gdx.Input.Keys.BACK));
            assertTrue(handler.keyUp(com.badlogic.gdx.Input.Keys.BACK));
            KeyEvent.processKeyEvents();
            assertEquals("No delayed second Back or orphan key-up may reach the returned scene",0,backs[0]);

            InputHandler.discardPendingEvents();
            BackGate.reset();
            assertTrue("Input reset does not unregister Android callback ownership",BackGate.systemBackOwned());
            assertTrue("Registration without delivery keeps the fallback key-down",handler.keyDown(com.badlogic.gdx.Input.Keys.BACK));
            assertTrue(handler.keyUp(com.badlogic.gdx.Input.Keys.BACK));
            KeyEvent.processKeyEvents();
            assertEquals("Accepted fallback preserves its complete down/up pair",2,backs[0]);
        }finally{
            KeyEvent.removeKeyListener(listener);
            BackGate.clearSystemBackOwner(activity);
            BackGate.reset();
            InputHandler.discardPendingEvents();
        }
    }

    @Test public void aReplacementInputHandlerKeepsFallbackWithoutReleasingNativeOwnership(){
        HeadlessTestSupport.start();
        int[] backRegistrations={0},menuRegistrations={0};
        com.badlogic.gdx.Input input=(com.badlogic.gdx.Input)java.lang.reflect.Proxy.newProxyInstance(
            getClass().getClassLoader(),new Class<?>[]{com.badlogic.gdx.Input.class},(proxy,method,args)->{
                if(method.getName().equals("setCatchKey")&&Boolean.TRUE.equals(args[1])){
                    if(((Integer)args[0])==com.badlogic.gdx.Input.Keys.BACK)backRegistrations[0]++;
                    if(((Integer)args[0])==com.badlogic.gdx.Input.Keys.MENU)menuRegistrations[0]++;
                }
                return null;
            });
        Object activity=new Object();
        try{
            BackGate.setSystemBackOwner(activity);
            new InputHandler(input);
            assertEquals("Registered Android callback retains one GDX fallback",1,backRegistrations[0]);
            assertTrue("Fallback registration must not release native ownership",BackGate.systemBackOwned());
            assertEquals("Other input routing remains registered",1,menuRegistrations[0]);
            BackGate.clearSystemBackOwner(activity);
            new InputHandler(input);
            assertEquals("Older Android/desktop retain the same legacy registration",2,backRegistrations[0]);
            assertEquals(2,menuRegistrations[0]);
        }finally{
            BackGate.clearSystemBackOwner(activity);
            BackGate.reset();
            InputHandler.discardPendingEvents();
        }
    }
}
