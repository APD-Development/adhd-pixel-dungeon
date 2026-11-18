/*
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
package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GhoulSprite;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.PathFinder;
import com.watabou.noosa.Game;
import com.watabou.utils.SparseArray;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Disposable emulator: real scheduler and GhoulSprite attack/death animations, no player saves. */
@RunWith(AndroidJUnit4.class)
public class RemovedActorStallDeviceTest {
    private static Field field(String name)throws Exception{Field f=Actor.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private static class Board extends Level {
        @Override protected boolean build(){return true;}
        @Override protected void createMobs(){}
        @Override protected void createItems(){}
    }
    private static class GhoulAttack extends Ghoul {
        final AtomicInteger attacks=new AtomicInteger();
        @Override protected boolean act(){sprite.attack(0);return false;}
        @Override public void onAttackComplete(){attacks.incrementAndGet();next();}
    }
    private static class ActualGhoulSprite extends GhoulSprite {
        ActualGhoulSprite(Ghoul ghoul){ch=ghoul;ghoul.sprite=this;}
        void advance(){float old=Game.elapsed;try{Game.elapsed=1;updateAnimation();}finally{Game.elapsed=old;}}
    }
    private static class InputBoundary extends Actor {
        final AtomicInteger turns=new AtomicInteger();
        InputBoundary(){actPriority=HERO_PRIO;}
        @Override protected boolean act(){turns.incrementAndGet();return false;}
    }
    private static void waiting(Thread worker,Actor expected,AtomicReference<Throwable> failure)throws Exception{
        long end=android.os.SystemClock.uptimeMillis()+10000;
        while(field("current").get(null)!=expected||worker.getState()!=Thread.State.WAITING){
            if(failure.get()!=null)throw new AssertionError(failure.get());
            if(android.os.SystemClock.uptimeMillis()>end)fail("Scheduler did not reach expected animation/input boundary");Thread.sleep(5);
        }
    }
    @Test public void cancelledGhoulAttackReleasesQueueButLiveAttackStaysPending()throws Exception{
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)){
            long deadline=android.os.SystemClock.uptimeMillis()+120000;
            while(Game.scene()==null||PixelScene.uiCamera==null){assertTrue(android.os.SystemClock.uptimeMillis()<deadline);Thread.sleep(100);}
            TopScoresDeviceTest.render(()->com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon.switchNoFade(TitleScene.class));
            while(!(Game.scene() instanceof TitleScene)){assertTrue(android.os.SystemClock.uptimeMillis()<deadline);Thread.sleep(100);}
            for(boolean cancel:new boolean[]{true,false}){
                Map<Field,Object> previous=new LinkedHashMap<>();Hero oldHero=Dungeon.hero;Level oldLevel=Dungeon.level;boolean oldKeepAlive=Actor.keepActorThreadAlive;
                AtomicReference<GhoulAttack> ghoul=new AtomicReference<>();AtomicReference<ActualGhoulSprite> sprite=new AtomicReference<>();InputBoundary input=new InputBoundary();
                AtomicReference<Throwable> failure=new AtomicReference<>();Thread worker=null;
                try{
                    TopScoresDeviceTest.render(()->{try{
                        for(String name:new String[]{"all","chars","ids","current","now","nextID"}){Field f=field(name);previous.put(f,f.get(null));}
                        field("all").set(null,new HashSet<Actor>());field("chars").set(null,new HashSet<Char>());field("ids").set(null,new SparseArray<Actor>());
                        field("current").set(null,null);field("now").setFloat(null,0);field("nextID").setInt(null,1);
                        Board board=new Board();board.setSize(9,9);Dungeon.level=board;
                        Dungeon.hero=new Hero();Dungeon.hero.HP=Dungeon.hero.HT=100;
                        ghoul.set(new GhoulAttack());sprite.set(new ActualGhoulSprite(ghoul.get()));Actor.add(ghoul.get());Actor.addDelayed(input,1);
                    }catch(Exception e){throw new AssertionError(e);}});
                    Actor.keepActorThreadAlive=true;worker=new Thread(Actor::process,"APD-native-removed-actor-test");worker.setDaemon(true);worker.setUncaughtExceptionHandler((t,e)->failure.set(e));worker.start();waiting(worker,ghoul.get(),failure);
                    TopScoresDeviceTest.render(()->{
                        if(cancel){Actor.remove(ghoul.get());sprite.get().die();sprite.get().advance();assertEquals(0,ghoul.get().attacks.get());assertFalse(Actor.processing());}
                        else{Actor.remove(null);assertTrue(Actor.processing());assertEquals(0,input.turns.get());sprite.get().advance();assertEquals(1,ghoul.get().attacks.get());Actor.remove(ghoul.get());}
                    });
                    synchronized(worker){worker.notifyAll();}waiting(worker,input,failure);assertEquals(1,input.turns.get());assertEquals(1,Actor.now(),0);
                    ghoul.get().next();assertSame(input,field("current").get(null));
                    System.out.println("APD_NATIVE_REMOVED_ACTOR cancel="+cancel+" nextInputTurns="+input.turns.get()+" clock="+Actor.now());
                }finally{
                    Actor.keepActorThreadAlive=false;if(worker!=null){worker.interrupt();synchronized(worker){worker.notifyAll();}worker.join(3000);assertFalse(worker.isAlive());}
                    TopScoresDeviceTest.render(()->{try{for(Map.Entry<Field,Object> e:previous.entrySet())e.getKey().set(null,e.getValue());Dungeon.hero=oldHero;Dungeon.level=oldLevel;if(oldLevel!=null)PathFinder.setMapSize(oldLevel.width(),oldLevel.height());Actor.keepActorThreadAlive=oldKeepAlive;if(sprite.get()!=null)sprite.get().destroy();}catch(Exception e){throw new AssertionError(e);}});
                    if(failure.get()!=null)throw new AssertionError(failure.get());
                }
            }
        }
    }
}
