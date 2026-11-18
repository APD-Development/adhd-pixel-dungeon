/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.Scene;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;
import org.junit.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Real scheduler and sprite animation cancellation, without Android/rendering or player saves. */
public class RemovedActorStallTest {
    private final Map<Field,Object> previous = new LinkedHashMap<>();
    private Hero previousHero;
    private Game previousGame;
    private Object previousSceneClass;
    private boolean previousKeepAlive;
    private Thread worker;
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private static class Shell extends Game {
        Shell(){super(Scene.class, Game.platform);requestedReset=false;}
    }
    private static Field field(Class<?> owner,String name)throws Exception{
        Field f=owner.getDeclaredField(name);f.setAccessible(true);return f;
    }
    private static class AnimationSprite extends CharSprite {
        AnimationSprite(Char owner){
            ch=owner;owner.sprite=this;listener=this;
            attack=new MovieClip.Animation(10,false).frames(new RectF(0,0,1,1));
            die=new MovieClip.Animation(10,false).frames(new RectF(0,0,1,1));
            idle=new MovieClip.Animation(10,true).frames(new RectF(0,0,1,1));
        }
        @Override public void frame(RectF frame){}
        @Override public void turnTo(int from,int to){}
        void finishAnimation(){float old=Game.elapsed;try{Game.elapsed=1;updateAnimation();}finally{Game.elapsed=old;}}
    }
    private static class WaitingGhoul extends Ghoul {
        final AtomicInteger attacks=new AtomicInteger();
        @Override protected boolean act(){sprite.attack(0);return false;}
        @Override public void onAttackComplete(){attacks.incrementAndGet();next();}
    }
    private static class InputBoundary extends Actor {
        final AtomicInteger turns=new AtomicInteger();
        InputBoundary(){actPriority=HERO_PRIO;}
        @Override protected boolean act(){turns.incrementAndGet();return false;}
    }
    @Before public void isolate()throws Exception{
        HeadlessTestSupport.start();previousHero=Dungeon.hero;previousGame=Game.instance;
        previousSceneClass=field(Game.class,"sceneClass").get(null);previousKeepAlive=Actor.keepActorThreadAlive;
        for(String name:new String[]{"all","chars","ids","current","now","nextID"}){
            Field f=field(Actor.class,name);previous.put(f,f.get(null));
        }
        field(Actor.class,"all").set(null,new HashSet<Actor>());
        field(Actor.class,"chars").set(null,new HashSet<Char>());
        field(Actor.class,"ids").set(null,new SparseArray<Actor>());
        field(Actor.class,"current").set(null,null);
        field(Actor.class,"now").setFloat(null,0);field(Actor.class,"nextID").setInt(null,1);
        new Shell();Dungeon.hero=new Hero();Dungeon.hero.HP=Dungeon.hero.HT=100;
    }
    @After public void restore()throws Exception{
        Actor.keepActorThreadAlive=false;
        if(worker!=null){worker.interrupt();synchronized(worker){worker.notifyAll();}worker.join(2000);assertFalse("scheduler must stop",worker.isAlive());}
        for(Map.Entry<Field,Object> e:previous.entrySet())e.getKey().set(null,e.getValue());
        Actor.keepActorThreadAlive=previousKeepAlive;Dungeon.hero=previousHero;Game.instance=previousGame;
        field(Game.class,"sceneClass").set(null,previousSceneClass);
        if(failure.get()!=null)throw new AssertionError(failure.get());
    }
    private void start() {
        Actor.keepActorThreadAlive=true;worker=new Thread(Actor::process,"removed-actor-regression");worker.setDaemon(true);
        worker.setUncaughtExceptionHandler((t,e)->failure.set(e));worker.start();
    }
    private void awaitWaitingOn(Actor actor)throws Exception{
        long deadline=System.nanoTime()+2_000_000_000L;
        while(field(Actor.class,"current").get(null)!=actor || worker.getState()!=Thread.State.WAITING){
            if(failure.get()!=null)throw new AssertionError(failure.get());
            if(System.nanoTime()>deadline)fail("scheduler did not reach expected animation/input wait");
            Thread.sleep(1);
        }
    }
    private void wake(){synchronized(worker){worker.notifyAll();}}
    @Test public void removingGhoulDuringAttackDoesNotWaitForCancelledAnimation()throws Exception{
        WaitingGhoul ghoul=new WaitingGhoul();AnimationSprite sprite=new AnimationSprite(ghoul);
        InputBoundary input=new InputBoundary();Actor.add(ghoul);Actor.addDelayed(input,1);start();awaitWaitingOn(ghoul);
        Actor.remove(ghoul);sprite.die();sprite.finishAnimation();
        assertEquals("death animation must not execute cancelled attack",0,ghoul.attacks.get());
        assertFalse("removed ghoul must not keep controls locked waiting for its cancelled attack",Actor.processing());
        wake();awaitWaitingOn(input);assertEquals(1,input.turns.get());assertEquals(1,Actor.now(),0);
        ghoul.next();assertSame("late cancelled callback cannot release the next actor",input,field(Actor.class,"current").get(null));
    }
    @Test public void unrelatedRemovalDoesNotReleaseLiveAttackOrSpendItsTurn()throws Exception{
        WaitingGhoul ghoul=new WaitingGhoul();AnimationSprite sprite=new AnimationSprite(ghoul);
        InputBoundary input=new InputBoundary();InputBoundary unrelated=new InputBoundary();
        Actor.add(ghoul);Actor.addDelayed(input,1);Actor.addDelayed(unrelated,2);start();awaitWaitingOn(ghoul);
        Actor.remove(null);Actor.remove(unrelated);assertTrue(Actor.processing());assertSame(ghoul,field(Actor.class,"current").get(null));
        assertEquals(0,input.turns.get());assertEquals(0,Actor.now(),0);
        sprite.finishAnimation();assertEquals(1,ghoul.attacks.get());
        Actor.remove(ghoul);wake();awaitWaitingOn(input);assertEquals(1,input.turns.get());
    }
    @Test public void removedActorCanBeRegisteredAgainWithoutBeingDuplicated()throws Exception{
        WaitingGhoul ghoul=new WaitingGhoul();new AnimationSprite(ghoul);Actor.add(ghoul);start();awaitWaitingOn(ghoul);
        Actor.remove(ghoul);assertFalse(Actor.processing());assertFalse(Actor.chars().contains(ghoul));assertNull(Actor.findById(ghoul.id()));
        Actor.addDelayed(ghoul,5);Actor.add(ghoul);assertEquals(1,Actor.all().size());assertSame(ghoul,Actor.findById(ghoul.id()));
        wake();awaitWaitingOn(ghoul);assertEquals(5,Actor.now(),0);
    }
    @Test public void pushingKeepsSchedulerWaitingUntilItsLandingCallback()throws Exception{
        com.shatteredpixel.shatteredpixeldungeon.levels.Level priorLevel=Dungeon.level;
        float priorElapsed=Game.elapsed;
        try {
            Dungeon.level=new com.shatteredpixel.shatteredpixeldungeon.apd.qol.GrassDewPickupTest.TestLevel();
            Dungeon.level.setSize(5,5);
            Char victim=new Char(){};
            victim.sprite=new CharSprite(){
                @Override public com.watabou.utils.PointF worldToCamera(int cell){return new com.watabou.utils.PointF(cell,0);}
            };
            com.watabou.noosa.Group group=new com.watabou.noosa.Group();group.add(victim.sprite);
            AtomicInteger landed=new AtomicInteger();
            com.shatteredpixel.shatteredpixeldungeon.effects.Pushing pushing=new com.shatteredpixel.shatteredpixeldungeon.effects.Pushing(victim,6,7,()->landed.incrementAndGet());
            InputBoundary input=new InputBoundary();Actor.add(pushing);Actor.addDelayed(input,1);
            start();awaitWaitingOn(pushing);
            assertFalse("push leaves actor queue while its visual is running",Actor.all().contains(pushing));
            assertTrue("temporary removal must not let guard move before victim lands",Actor.processing());
            assertEquals(0,landed.get());assertEquals(0,input.turns.get());
            com.shatteredpixel.shatteredpixeldungeon.effects.Pushing.Effect effect=null;
            for(Object child:(java.util.List<?>)field(com.watabou.noosa.Group.class,"members").get(group))if(child instanceof com.shatteredpixel.shatteredpixeldungeon.effects.Pushing.Effect)effect=(com.shatteredpixel.shatteredpixeldungeon.effects.Pushing.Effect)child;
            assertNotNull(effect);Game.elapsed=.2f;effect.update();
            assertEquals(1,landed.get());assertFalse(Actor.processing());
            wake();awaitWaitingOn(input);assertEquals(1,input.turns.get());assertEquals(1,Actor.now(),0);
        } finally {Dungeon.level=priorLevel;Game.elapsed=priorElapsed;}
    }

}
