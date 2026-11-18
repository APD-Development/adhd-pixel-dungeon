package com.watabou.noosa;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/** Scene scheduling is testable without creating an OpenGL context. */
public class SceneRequestTest {
    private final Game previous=Game.instance;
    private final Class<? extends Scene> previousSceneClass=Game.sceneClass;
    @After public void restore(){Game.instance=previous;Game.sceneClass=previousSceneClass;}
    public static class First extends Scene {}
    public static class Second extends Scene {}
    private static class Driver extends Game {
        int switches,updates;boolean chain;
        Driver(){super(First.class,null);}
        @Override protected void switchScene(){
            switches++;scene=requestedScene;
            if(chain){chain=false;Game.switchScene(Second.class);}
        }
        @Override protected void update(){updates++;}
        void applyBeforeDraw(){applyRequestedScene();}
        void nextStep(){step();}
    }
    @Test public void earlyBackTransitionIsNotAppliedAgainAtTheEndOfTheFrame(){
        Driver game=new Driver();Game.switchScene(Second.class);
        game.applyBeforeDraw();assertTrue(Game.scene() instanceof Second);
        assertFalse(Game.switchingScene());game.nextStep();
        assertEquals(1,game.switches);assertEquals(1,game.updates);
    }
    @Test public void requestDuringCreationIsPreservedAsASeparateTransition(){
        Driver game=new Driver();game.chain=true;Game.switchScene(First.class);
        game.applyBeforeDraw();assertTrue(Game.scene() instanceof First);assertTrue(Game.switchingScene());
        game.nextStep();assertTrue(Game.scene() instanceof Second);assertFalse(Game.switchingScene());
        assertEquals(2,game.switches);assertEquals(1,game.updates);
    }
}
