/* ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Guard;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import org.junit.*;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

public class GuardPullDestinationTest {
    private Level previous;
    private Guard guard;
    private Char victim;
    private Board board;
    private Method arrival;
    static class Board extends Level {
        int entries;
        protected boolean build(){return true;}
        protected void createMobs(){}
        protected void createItems(){}
        public void occupyCell(Char ch){entries++;}
    }
    static class Sprite extends CharSprite {
        int placed=-1;
        public void place(int cell){placed=cell;}
    }
    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();previous=Dungeon.level;
        board=new Board();board.setSize(9,9);board.mobs=new java.util.HashSet<>();Dungeon.level=board;
        guard=new Guard();guard.pos=40;board.mobs.add(guard);
        victim=new Char(){};victim.HP=victim.HT=20;victim.pos=43;victim.sprite=new Sprite();
        arrival=Guard.class.getDeclaredMethod("pullEnemy",Char.class,int.class);arrival.setAccessible(true);
    }
    @After public void restore(){
        if(victim!=null)for(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff b:victim.buffs())b.detach();
        Actor.remove(guard);Actor.remove(victim);Dungeon.level=previous;
    }
    @Test public void guardEnteringDestinationBeforeAnimationEndsCannotOverlapVictim() throws Exception {
        // The destination was open when the chain began; the guard now occupies it.
        arrival.invoke(guard,victim,guard.pos);
        assertEquals(43,victim.pos);assertEquals(43,((Sprite)victim.sprite).placed);
        assertEquals(0,board.entries);assertNull(victim.buff(Cripple.class));
    }
    @Test public void anotherMobEnteringDestinationCannotBeOverlapped() throws Exception {
        Guard blocker=new Guard();blocker.pos=41;board.mobs.add(blocker);
        arrival.invoke(guard,victim,41);
        assertEquals(43,victim.pos);assertEquals(0,board.entries);assertNull(victim.buff(Cripple.class));
    }
    @Test public void legalPullStillMovesOccupiesAndCripples() throws Exception {
        arrival.invoke(guard,victim,41);
        assertEquals(41,victim.pos);assertEquals(41,((Sprite)victim.sprite).placed);
        assertEquals(1,board.entries);assertNotNull(victim.buff(Cripple.class));
    }
    @Test public void destinationThatBecameSolidIsRejected() throws Exception {
        board.solid[41]=true;arrival.invoke(guard,victim,41);
        assertEquals(43,victim.pos);assertEquals(0,board.entries);
    }
}
