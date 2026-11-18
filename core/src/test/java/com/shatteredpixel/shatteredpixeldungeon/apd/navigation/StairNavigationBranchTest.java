package com.shatteredpixel.shatteredpixeldungeon.apd.navigation;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.*;

public class StairNavigationBranchTest {

    private static class Empty extends Level {
        int activations;
        LevelTransition activatedTransition;
        @Override protected boolean build(){return true;}
        @Override protected void createMobs(){}
        @Override protected void createItems(){}
        @Override public boolean activateTransition(Hero hero, LevelTransition transition){
            activations++;
            activatedTransition=transition;
            return true;
        }
    }

    private static class InvisibleSprite extends com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite {
        void attach(Hero hero){ch=hero;hero.sprite=this;visible=false;}
        @Override public void place(int cell){}
        @Override public void move(int from,int to){}
        @Override public void idle(){}
        @Override public void showStatus(int color,String text,Object...args){}
        @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}
        @Override public void showAlert(){}
        @Override public void hideAlert(){}
    }

    private Level previousLevel;
    private Hero previousHero;
    private Object previousSelector;
    private boolean previousAutoEnter;
    private Empty level;
    private Hero hero;

    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        Field selector=GameScene.class.getDeclaredField("cellSelector");
        selector.setAccessible(true);
        previousSelector=selector.get(null);
        Field unsafeField=Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        selector.set(null,((Unsafe)unsafeField.get(null)).allocateInstance(CellSelector.class));

        previousAutoEnter=SPDSettings.autoEnterStairs();
        SPDSettings.autoEnterStairs(false);
        previousLevel=Dungeon.level;
        previousHero=Dungeon.hero;

        level=new Empty();
        level.setSize(5,5);
        level.transitions=new ArrayList<>();
        level.mobs=new HashSet<>();
        level.traps=new SparseArray<>();
        level.blobs=new HashMap<>();
        level.heaps=new SparseArray<>();
        level.plants=new SparseArray<>();
        Arrays.fill(level.map, Terrain.EMPTY);
        Arrays.fill(level.visited,true);
        Arrays.fill(level.passable,true);

        hero=new Hero();
        hero.heroClass=HeroClass.WARRIOR;
        Talent.initClassTalents(hero);
        hero.pos=12;
        hero.ready=true;
        hero.fieldOfView=new boolean[25];
        Dungeon.level=level;
        Dungeon.hero=hero;
    }

    @After public void cleanup() throws Exception {
        SPDSettings.autoEnterStairs(previousAutoEnter);
        Dungeon.level=previousLevel;
        Dungeon.hero=previousHero;
        Field selector=GameScene.class.getDeclaredField("cellSelector");
        selector.setAccessible(true);
        selector.set(null,previousSelector);
    }

    private LevelTransition add(int cell, LevelTransition.Type type) {
        LevelTransition transition=new LevelTransition(level,cell,type,10,0,LevelTransition.Type.BRANCH_EXIT);
        level.transitions.add(transition);
        return transition;
    }

    @Test public void branchAreaOffersOnlyReturnToMainDungeon() {
        add(13,LevelTransition.Type.BRANCH_ENTRANCE);
        assertArrayEquals(new StairNavigation.Route[]{StairNavigation.Route.RETURN_TO_MAIN_DUNGEON},
                StairNavigation.availableRoutes(level));
        assertEquals(13,StairNavigation.discoveredTarget(level,StairNavigation.Route.RETURN_TO_MAIN_DUNGEON));
        assertEquals(-1,StairNavigation.discoveredStair(level,true));
        assertEquals(-1,StairNavigation.discoveredStair(level,false));
    }

    @Test public void returnToMainDungeonUsesTheSafeStairWalk() {
        add(13,LevelTransition.Type.BRANCH_ENTRANCE);
        assertNull(StairNavigation.begin(hero,StairNavigation.Route.RETURN_TO_MAIN_DUNGEON));
        assertTrue(hero.curAction instanceof StairNavigation.Walk);
        StairNavigation.Walk walk=(StairNavigation.Walk)hero.curAction;
        assertEquals(13,walk.dst);
        assertEquals(StairNavigation.Route.RETURN_TO_MAIN_DUNGEON,walk.route);
        assertFalse(walk.enterOnArrival);
    }

    @Test public void autoEnterReturnsThroughBranchEntranceExactlyOnce() throws Exception {
        add(13,LevelTransition.Type.BRANCH_ENTRANCE);
        new InvisibleSprite().attach(hero);
        SPDSettings.autoEnterStairs(true);
        assertNull(StairNavigation.begin(hero,StairNavigation.Route.RETURN_TO_MAIN_DUNGEON));
        StairNavigation.Walk walk=(StairNavigation.Walk)hero.curAction;
        Method move=Hero.class.getDeclaredMethod("actMove",HeroAction.Move.class);
        move.setAccessible(true);
        assertTrue((Boolean)move.invoke(hero,walk));
        assertEquals(13,hero.pos);
        assertEquals(0,level.activations);
        assertFalse((Boolean)move.invoke(hero,walk));
        assertEquals(1,level.activations);
        assertEquals(LevelTransition.Type.BRANCH_ENTRANCE,level.activatedTransition.type);
        assertNull(hero.curAction);
    }

    @Test public void mainDungeonRestoresOrdinaryUpAndDownRoutes() {
        add(17,LevelTransition.Type.BRANCH_EXIT);
        level.transitions.add(new LevelTransition(level,11,LevelTransition.Type.REGULAR_ENTRANCE));
        level.transitions.add(new LevelTransition(level,13,LevelTransition.Type.REGULAR_EXIT));
        assertArrayEquals(new StairNavigation.Route[]{StairNavigation.Route.UP,StairNavigation.Route.DOWN},
                StairNavigation.availableRoutes(level));
        assertEquals(-1,StairNavigation.discoveredTarget(level,StairNavigation.Route.RETURN_TO_MAIN_DUNGEON));
        assertEquals(11,StairNavigation.discoveredStair(level,true));
        assertEquals(13,StairNavigation.discoveredStair(level,false));
    }

    @Test public void undiscoveredQuestExitDoesNotExposeItsCell() {
        add(13,LevelTransition.Type.BRANCH_ENTRANCE);
        level.visited[13]=false;
        assertArrayEquals(new StairNavigation.Route[]{StairNavigation.Route.RETURN_TO_MAIN_DUNGEON},
                StairNavigation.availableRoutes(level));
        assertEquals(-1,StairNavigation.discoveredTarget(level,StairNavigation.Route.RETURN_TO_MAIN_DUNGEON));
        assertEquals("The return to the main dungeon has not been discovered.",
                StairNavigation.begin(hero,StairNavigation.Route.RETURN_TO_MAIN_DUNGEON));
        assertNull(hero.curAction);
    }

    @Test public void changedTransitionIdentityCannotAutoEnter() {
        add(13,LevelTransition.Type.BRANCH_ENTRANCE);
        SPDSettings.autoEnterStairs(true);
        assertNull(StairNavigation.begin(hero,StairNavigation.Route.RETURN_TO_MAIN_DUNGEON));
        StairNavigation.Walk walk=(StairNavigation.Walk)hero.curAction;
        hero.pos=13;
        level.transitions.clear();
        level.transitions.add(new LevelTransition(level,13,LevelTransition.Type.REGULAR_EXIT));
        assertFalse(StairNavigation.canEnterOnArrival(hero,walk));
        assertEquals(0,level.activations);
    }
}
