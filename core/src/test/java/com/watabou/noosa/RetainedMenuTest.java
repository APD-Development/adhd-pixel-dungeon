package com.watabou.noosa;

import com.badlogic.gdx.Input;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.input.PointerEvent;
import com.watabou.input.GameAction;
import com.watabou.utils.Signal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercise the real scene lifecycle, camera ownership and input dispatch without GL. */
public class RetainedMenuTest {
    private final Game previous = Game.instance;
    private final Class<? extends Scene> previousClass = Game.sceneClass;
    private final int width = Game.width, height = Game.height;
    private Signal<PointerEvent> previousPointers;
    private Camera.State previousCameras;
    private Driver game;

    public static class Menu extends Scene {
        int creates, destroys, restores, pointers, backs;
        Camera primary, extra;
        @Override public void create(){
            super.create(); creates++;
            primary = Camera.main;
            extra = Camera.add(Camera.createFullscreen(2));
            PointerEvent.addPointerListener(event -> { pointers++; return false; });
        }
        @Override public void destroy(){ destroys++; super.destroy(); PointerEvent.clearListeners(); }
        @Override public void onRestore(){ restores++; }
        @Override protected void onBackPressed(){ backs++; }
    }
    public static class Title extends Menu {}
    public static class Child extends Menu {}
    public static class Other extends Menu {}
    public static class Gameplay extends Menu {}
    private static class Driver extends Game {
        Driver(){ super(Title.class, null); }
        @Override protected boolean isReturnMenu(Class<? extends Scene> target){ return target != Gameplay.class; }
        @Override protected boolean retainForReturn(Scene current, Class<? extends Scene> target){
            return current instanceof Title && target != Title.class && isReturnMenu(target);
        }
        void go(Class<? extends Scene> target){ Game.switchScene(target); applyRequestedScene(); }
        void flush(){ applyRequestedScene(); }
    }
    @Before public void setUp(){
        Game.width=320; Game.height=640;
        previousPointers=PointerEvent.detachListeners(); previousCameras=Camera.detach();
        game=new Driver(); game.go(Title.class);
    }
    @After public void tearDown(){
        game.destroy(); Camera.detach().destroy(); Camera.restore(previousCameras);
        PointerEvent.swapListeners(previousPointers);
        Game.instance=previous; Game.sceneClass=previousClass; Game.width=width; Game.height=height;
    }
    private void pointer(){
        PointerEvent.addPointerEvent(new PointerEvent(2,3,0,PointerEvent.Type.HOVER));
        PointerEvent.processPointerEvents();
    }
    @Test public void backReusesTheTitleAndBothCamerasWithoutCreatingItAgain(){
        Title title=(Title)Game.scene();
        game.go(Child.class); assertFalse(title.isActive()); assertEquals(0,title.destroys);
        game.go(Title.class);
        assertSame(title,Game.scene()); assertEquals(1,title.creates); assertEquals(1,title.restores);
        assertSame(title.primary,Camera.main); assertTrue(title.isActive());
        Camera.remove(title.extra); // extra camera survives along with main
        assertNotNull(title.extra.matrix);
        assertEquals(0,title.destroys);
    }
    @Test public void onlyTheVisibleSceneReceivesPointersAndChildCleanupKeepsTitleInput(){
        Title title=(Title)Game.scene();
        game.go(Child.class); Child child=(Child)Game.scene(); pointer();
        assertEquals(0,title.pointers); assertTrue(child.pointers>0);
        game.go(Title.class); pointer();
        assertTrue(title.pointers>0); assertEquals(1,child.destroys);
    }
    @Test public void movingBetweenChildrenKeepsOnlyTheOneTitleReturn(){
        Title title=(Title)Game.scene(); game.go(Child.class); Child first=(Child)Game.scene();
        game.go(Other.class); assertEquals(1,first.destroys); assertEquals(0,title.destroys);
        game.go(Title.class); assertSame(title,Game.scene());
    }
    @Test public void gameplayReleasesTheReturnAndRecreatesTitleLater(){
        Title title=(Title)Game.scene(); game.go(Child.class); game.go(Gameplay.class);
        assertEquals(1,title.destroys); game.go(Title.class); assertNotSame(title,Game.scene());
    }
    @Test public void invalidationDoesNotEraseTheActiveMenusPointerListeners(){
        Title title=(Title)Game.scene(); game.go(Child.class); Child child=(Child)Game.scene();
        Game.invalidateRetainedReturn(); game.flush(); pointer();
        assertEquals(1,title.destroys); assertTrue(child.pointers>0);
        game.go(Title.class); assertNotSame(title,Game.scene());
    }
    @Test public void resetAndResumeDiscardRetainedGraphics(){
        Title title=(Title)Game.scene(); game.go(Child.class);
        Game.resetScene(); game.flush(); assertEquals(1,title.destroys);
        game.go(Title.class); Title second=(Title)Game.scene(); game.go(Child.class);
        game.resume(); game.flush(); assertEquals(1,second.destroys);
    }
    @Test public void callbacksAlwaysUseFreshCreation(){
        Title title=(Title)Game.scene(); game.go(Child.class); final int[] calls={0};
        Game.switchScene(Title.class,new Game.SceneChangeCallback(){
            @Override public void beforeCreate(){ calls[0]++; }
            @Override public void afterCreate(){ calls[0]++; }
        });
        game.flush(); assertNotSame(title,Game.scene()); assertEquals(1,title.destroys); assertEquals(2,calls[0]);
    }
    @Test public void destroyingGameReleasesActiveAndRetainedScenesOnce(){
        Title title=(Title)Game.scene(); game.go(Child.class); Child child=(Child)Game.scene();
        game.destroy(); assertEquals(1,title.destroys); assertEquals(1,child.destroys);
    }

    @Test public void dormantSceneDoesNotHandleBackWhileItsChildIsOpen(){
        java.util.LinkedHashMap<Integer,GameAction> bindings=KeyBindings.getAllBindings();
        try {
            java.util.LinkedHashMap<Integer,GameAction> testBindings=new java.util.LinkedHashMap<>(bindings);
            testBindings.put(Input.Keys.BACK,GameAction.BACK); KeyBindings.setAllBindings(testBindings);
            Title title=(Title)Game.scene(); game.go(Child.class); Child child=(Child)Game.scene();
            KeyEvent.addKeyEvent(new KeyEvent(Input.Keys.BACK,true)); KeyEvent.processKeyEvents();
            assertEquals(0,title.backs); assertEquals(1,child.backs);
            game.go(Title.class);
            KeyEvent.addKeyEvent(new KeyEvent(Input.Keys.BACK,true)); KeyEvent.processKeyEvents();
            assertEquals(1,title.backs);
        } finally { KeyBindings.setAllBindings(bindings); }
    }

    @Test public void aPressHeldAcrossNavigationCannotClickTheReturnedTitle(){
        Title title=(Title)Game.scene(); final int[] clicks={0},ups={0};
        PointerArea area=new PointerArea(0,0,20,20){
            @Override protected void onClick(PointerEvent event){clicks[0]++;}
            @Override protected void onPointerUp(PointerEvent event){ups[0]++;}
        };
        Group nested=new Group(); nested.add(area); title.add(nested);
        PointerEvent press=new PointerEvent(2,3,7,PointerEvent.Type.DOWN);
        area.onSignal(press); game.go(Child.class); assertEquals(1,ups[0]);
        game.go(Title.class); area.onSignal(press.up()); assertEquals(0,clicks[0]);
    }

    @Test public void firstDrawHasRealCameraTransformsWithoutAdvancingSimulation(){
        final int[] updates={0};
        Camera camera=new Camera(0,0,160,320,2){
            @Override public synchronized void update(){updates[0]++;super.update();}
        };
        Camera.add(camera);
        assertEquals(1f,camera.matrix[0],0f);
        float total=Game.timeTotal,elapsed=Game.elapsed;
        Camera.refreshMatrices();
        assertEquals(4f/320,camera.matrix[0],0f);
        assertEquals(-4f/640,camera.matrix[5],0f);
        assertEquals(-1f,camera.matrix[12],0f);
        assertEquals(1f,camera.matrix[13],0f);
        assertEquals(0,updates[0]);
        assertEquals(total,Game.timeTotal,0f);assertEquals(elapsed,Game.elapsed,0f);
    }
}
