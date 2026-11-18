package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Scene;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Tests actual pylon acts and the real scheduler; it does not emulate a phone or a complete boss fight. */
public class PylonTimingRegressionTest {
    private final Map<Field,Object> actorState = new LinkedHashMap<>();
    private Hero previousHero;
    private Level previousLevel;
    private int previousChallenges;
    private Game previousGame;
    private Object previousSceneClass;
    private float previousElapsed, previousPlayback;
    private boolean previousKeepAlive;
    private Field direction;

    private static class Shell extends Game {
        Shell(){ super(Scene.class, Game.platform); requestedReset = false; }
    }
    private static class Board extends Level {
        @Override protected boolean build(){ return true; }
        @Override protected void createMobs(){}
        @Override protected void createItems(){}
        @Override public void updateFieldOfView(Char who,boolean[] visible){}
    }
    private static class SilentSprite extends CharSprite {
        @Override public void hideAlert(){}
        @Override public void hideLost(){}
        @Override public void flash(){}
    }
    private static class Rotator extends Pylon {
        int acts;
        @Override protected Char chooseEnemy(){ return null; }
        @Override protected boolean act(){ acts++; return super.act(); }
        boolean step(){ return act(); }
    }
    private static class Receiver extends Char {
        int hits;
        @Override public void damage(int amount,Object source){ hits++; }
    }
    private static class InputClock extends Actor {
        int actions;
        final int limit;
        final float cost;
        InputClock(int limit,float cost){ this.limit=limit;this.cost=cost;actPriority=HERO_PRIO; }
        @Override protected boolean act(){
            if(actions==limit){ keepActorThreadAlive=false;return true; }
            actions++;spend(cost);return true;
        }
    }
    private static Field field(Class<?> owner,String name)throws Exception{
        Field f=owner.getDeclaredField(name);f.setAccessible(true);return f;
    }
    @Before public void isolate()throws Exception{
        HeadlessTestSupport.start();
        previousHero=Dungeon.hero;previousLevel=Dungeon.level;previousChallenges=Dungeon.challenges;
        previousGame=Game.instance;previousElapsed=Game.elapsed;previousKeepAlive=Actor.keepActorThreadAlive;
        previousSceneClass=field(Game.class,"sceneClass").get(null);
        previousPlayback=field(PlaybackGroup.class,"speed").getFloat(null);
        for(String name:new String[]{"all","chars","ids","current","now","nextID"}){
            Field f=field(Actor.class,name);actorState.put(f,f.get(null));
        }
        field(Actor.class,"all").set(null,new HashSet<Actor>());
        field(Actor.class,"chars").set(null,new HashSet<Char>());
        field(Actor.class,"ids").set(null,new SparseArray<Actor>());
        field(Actor.class,"current").set(null,null);
        field(Actor.class,"now").setFloat(null,0f);
        field(Actor.class,"nextID").setInt(null,1);
        new Shell();
        Dungeon.hero=new Hero();Dungeon.hero.HP=Dungeon.hero.HT=100;
        Dungeon.hero.pos=10;Dungeon.challenges=0;
        Board board=new Board();board.setSize(9,9);board.heaps=new SparseArray<>();Dungeon.level=board;
        direction=field(Pylon.class,"targetNeighbor");
    }
    @After public void restore()throws Exception{
        for(Map.Entry<Field,Object> e:actorState.entrySet())e.getKey().set(null,e.getValue());
        Actor.keepActorThreadAlive=previousKeepAlive;
        Dungeon.hero=previousHero;Dungeon.level=previousLevel;Dungeon.challenges=previousChallenges;
        Game.instance=previousGame;Game.elapsed=previousElapsed;
        field(Game.class,"sceneClass").set(null,previousSceneClass);
        if(previousLevel!=null)PathFinder.setMapSize(previousLevel.width(),previousLevel.height());
        field(PlaybackGroup.class,"speed").setFloat(null,previousPlayback);
    }
    private Rotator pylon(boolean active)throws Exception{
        Rotator p=new Rotator();p.pos=40;p.sprite=new SilentSprite();
        p.alignment=active?Char.Alignment.ENEMY:Char.Alignment.NEUTRAL;
        direction.setInt(p,0);return p;
    }
    private void scheduler(int mode,float actionCost,int expectedActs)throws Exception{
        Actor.clear();PlaybackGroup.speed(mode);Game.elapsed=1f/144;
        Rotator p=pylon(true);InputClock input=new InputClock(16,actionCost);
        Actor.add(input);Actor.add(p);Actor.add(p); // Duplicate registration must remain idempotent.
        AtomicReference<Throwable> failed=new AtomicReference<>();
        Thread worker=new Thread(Actor::process,"pylon-timing-regression");worker.setDaemon(true);
        worker.setUncaughtExceptionHandler((thread,error)->failed.set(error));
        Actor.keepActorThreadAlive=true;worker.start();worker.join(5000);
        if(worker.isAlive()){
            Actor.keepActorThreadAlive=false;worker.interrupt();
            synchronized(worker){worker.notifyAll();}
            worker.join(1000);
        }
        assertFalse("scheduler must terminate",worker.isAlive());
        assertNull("real scheduler failed",failed.get());
        assertEquals(expectedActs,p.acts);assertEquals(expectedActs%8,direction.getInt(p));
        assertEquals(16f*actionCost,Actor.now(),0.00001f);
    }
    @Test public void realSchedulerAdvancesOncePerNormalTurnAtEveryPlaybackSpeed()throws Exception{
        for(int mode=0;mode<3;mode++)scheduler(mode,1f,16);
    }
    @Test public void twoTurnPlayerActionsAllowTwoPylonActsWithoutPlaybackDoubleScaling()throws Exception{
        for(int mode=0;mode<3;mode++)scheduler(mode,2f,32);
    }
    @Test public void shockPatternRetainsVanillaOppositeCellsAndStrongerBossesOffsets()throws Exception{
        for(boolean stronger:new boolean[]{false,true}){
            Actor.clear();Dungeon.challenges=stronger?Challenges.STRONGER_BOSSES:0;
            Rotator p=pylon(true);List<Receiver> targets=new ArrayList<>();
            for(int offset=0;offset<8;offset++){
                Receiver target=new Receiver();target.pos=p.pos+PathFinder.CIRCLE8[offset];
                target.sprite=new SilentSprite();targets.add(target);Actor.addDelayed(target,1000);
            }
            for(int turn=0;turn<16;turn++){
                int[] before=new int[8];for(int i=0;i<8;i++)before[i]=targets.get(i).hits;
                assertTrue(p.step());
                for(int i=0;i<8;i++){
                    int d=(i-turn%8+8)%8;
                    int hit=d==0||(!stronger&&d==4)||(stronger&&(d==3||d==5))?1:0;
                    assertEquals("turn "+turn+" neighbour "+i,before[i]+hit,targets.get(i).hits);
                }
                assertEquals((turn+1)%8,direction.getInt(p));
            }
        }
    }
    @Test public void inactiveAndRestoredPylonsRetainTheirDirectionAndTurnCost()throws Exception{
        Rotator inactive=pylon(false);float before=inactive.cooldown();inactive.step();
        assertEquals(0,direction.getInt(inactive));assertEquals(before+1f,inactive.cooldown(),0.00001f);
        Rotator original=pylon(true);for(int i=0;i<5;i++)original.step();
        Bundle saved=new Bundle();original.storeInBundle(saved);Rotator restored=new Rotator();
        restored.restoreFromBundle(saved);restored.sprite=new SilentSprite();
        assertEquals(5,direction.getInt(restored));before=restored.cooldown();restored.step();
        assertEquals(6,direction.getInt(restored));assertEquals(before+1f,restored.cooldown(),0.00001f);
    }
}
