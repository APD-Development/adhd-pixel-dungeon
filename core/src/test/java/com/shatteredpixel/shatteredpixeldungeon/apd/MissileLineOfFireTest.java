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
package com.shatteredpixel.shatteredpixeldungeon.apd;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.*;
import org.junit.*;
import static org.junit.Assert.*;
public class MissileLineOfFireTest {
    private Hero previousHero,hero;private Level previousLevel,arena;private Rat enemy,blocker;
    private int at(int x,int y){return y*arena.width()+x;}
    private void sight(){ShadowCaster.castShadow(hero.pos%arena.width(),hero.pos/arena.width(),arena.width(),hero.fieldOfView,arena.losBlocking,20);}
    @Before public void setup(){
        HeadlessTestSupport.start();previousHero=Dungeon.hero;previousLevel=Dungeon.level;
        arena=new SewerLevel();arena.blobs=new java.util.HashMap<>();arena.traps=new com.watabou.utils.SparseArray<>();arena.setSize(32,32);java.util.Arrays.fill(arena.map,Terrain.EMPTY);
        for(int y=0;y<32;y++)for(int x=0;x<32;x++)if(x==0||y==0||x==31||y==31)arena.map[at(x,y)]=Terrain.WALL;
        arena.buildFlagMaps();Dungeon.level=arena;hero=new Hero();hero.pos=at(2,2);hero.fieldOfView=new boolean[arena.length()];Dungeon.hero=hero;
        enemy=new Rat();enemy.pos=at(6,5);Actor.add(enemy);
    }
    @After public void cleanup(){Actor.remove(enemy);if(blocker!=null)Actor.remove(blocker);Dungeon.hero=previousHero;Dungeon.level=previousLevel;}
    @Test public void visibleEnemyAtTieSlopeHasAContinuousClearStraightRay(){
        Level.set(at(4,4),Terrain.WALL);sight();assertTrue("enemy is legitimately visible",hero.fieldOfView[enemy.pos]);
        assertNotEquals("forward rounding hits the neighboring wall",enemy.pos,new Ballistica(hero.pos,enemy.pos,Ballistica.PROJECTILE).collisionPos.intValue());
        assertEquals("same straight line in reverse rounding is clear",hero.pos,new Ballistica(enemy.pos,hero.pos,Ballistica.PROJECTILE).collisionPos.intValue());
        assertEquals("visible enemy with clear ray remains throwable",enemy.pos,new ThrowingKnife().throwPos(hero,enemy.pos));
    }
    @Test public void clearVisibleShotsHaveNoArtificialMeleeOrShortRangeLimit(){
        enemy.pos=at(20,2);sight();assertTrue(hero.fieldOfView[enemy.pos]);assertEquals(enemy.pos,new ThrowingKnife().throwPos(hero,enemy.pos));
        enemy.pos=at(14,14);sight();assertTrue(hero.fieldOfView[enemy.pos]);assertEquals(enemy.pos,new ThrowingKnife().throwPos(hero,enemy.pos));
    }
    @Test public void seeingAnEnemyDoesNotAllowBendingAroundAClosedCorner(){
        Level.set(at(4,3),Terrain.WALL);Level.set(at(4,4),Terrain.WALL);hero.fieldOfView[enemy.pos]=true;
        assertNotEquals(enemy.pos,new ThrowingKnife().throwPos(hero,enemy.pos));
    }
    @Test public void interveningCreaturesStillBlockTheShot(){
        Level.set(at(4,4),Terrain.WALL);sight();blocker=new Rat();blocker.pos=at(3,3);Actor.add(blocker);
        assertNotEquals(enemy.pos,new ThrowingKnife().throwPos(hero,enemy.pos));
    }
    @Test public void unseenEnemiesKeepNormalCollisionRules(){
        Level.set(at(4,4),Terrain.WALL);assertFalse(hero.fieldOfView[enemy.pos]);
        assertEquals(new Ballistica(hero.pos,enemy.pos,Ballistica.PROJECTILE).collisionPos.intValue(),new ThrowingKnife().throwPos(hero,enemy.pos));
    }
    private void largeArena(){
        arena.setSize(64,64);java.util.Arrays.fill(arena.map,Terrain.EMPTY);
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)if(x==0||y==0||x==63||y==63)arena.map[at(x,y)]=Terrain.WALL;
        arena.buildFlagMaps();hero.pos=at(32,32);hero.fieldOfView=new boolean[arena.length()];
    }
    @Test public void aimingDirectlyFindsTheAlreadyLegalBeyondTargetRayInEveryOctant(){
        int[][] directions={{1,0,0,1},{1,0,0,-1},{-1,0,0,1},{-1,0,0,-1},{0,1,1,0},{0,1,-1,0},{0,-1,1,0},{0,-1,-1,0}};
        for(int[] d:directions){
            largeArena();enemy.pos=at(32+d[0]*22+d[1]*9,32+d[2]*22+d[3]*9);
            int wall=at(32+d[0]*21+d[1]*9,32+d[2]*21+d[3]*9),beyond=at(32+d[0]*23+d[1]*9,32+d[2]*23+d[3]*9);
            Level.set(wall,Terrain.WALL);hero.fieldOfView[enemy.pos]=true;
            assertNotEquals(enemy.pos,new Ballistica(hero.pos,enemy.pos,Ballistica.PROJECTILE).collisionPos.intValue());
            assertNotEquals(hero.pos,new Ballistica(enemy.pos,hero.pos,Ballistica.PROJECTILE).collisionPos.intValue());
            assertEquals("aiming beyond already hits legally",enemy.pos,new Ballistica(hero.pos,beyond,Ballistica.PROJECTILE).collisionPos.intValue());
            ThrowingKnife knife=new ThrowingKnife();assertEquals("direct targeting must use the existing legal ray",enemy.pos,knife.throwPos(hero,enemy.pos));assertEquals(enemy.pos,knife.targetingPos(hero,enemy.pos));assertEquals(enemy.pos,com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.autoAim(enemy,knife));
        }
    }
    @Test public void alternativeThrowAimingDoesNotChangeOrdinaryItemsWandsOrBowArrows(){
        largeArena();hero.pos=at(3,3);enemy.pos=at(25,12);Level.set(at(24,12),Terrain.WALL);hero.fieldOfView[enemy.pos]=true;
        int original=new Ballistica(hero.pos,enemy.pos,Ballistica.PROJECTILE).collisionPos;
        assertEquals(original,new com.shatteredpixel.shatteredpixeldungeon.items.Item().throwPos(hero,enemy.pos));
        assertEquals(original,new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile().throwPos(hero,enemy.pos));
        assertEquals(original,new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow().knockArrow().throwPos(hero,enemy.pos));
        assertEquals(enemy.pos,new ThrowingKnife().throwPos(hero,at(26,12)));
        hero.fieldOfView[enemy.pos]=false;assertEquals(original,new ThrowingKnife().throwPos(hero,enemy.pos));
    }
    @Test public void candidateFilterMatchesEveryLegalAimAcrossDifferentAnglesAndObstacles(){
        java.util.Random layouts=new java.util.Random(8112026);
        for(int sample=0;sample<64;sample++){
            arena.setSize(16,16);java.util.Arrays.fill(arena.map,Terrain.EMPTY);hero.pos=at(7,7);
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)if(x==0||y==0||x==15||y==15||layouts.nextInt(5)==0)arena.map[at(x,y)]=Terrain.WALL;
            do{enemy.pos=at(1+layouts.nextInt(14),1+layouts.nextInt(14));}while(enemy.pos==hero.pos);
            arena.map[hero.pos]=arena.map[enemy.pos]=Terrain.EMPTY;arena.buildFlagMaps();hero.fieldOfView=new boolean[arena.length()];hero.fieldOfView[enemy.pos]=true;
            boolean bruteForce=false;
            for(int aim=0;aim<arena.length();aim++)if(aim!=hero.pos&&aim!=enemy.pos&&new Ballistica(hero.pos,aim,Ballistica.PROJECTILE).collisionPos==enemy.pos){bruteForce=true;break;}
            assertEquals("layout "+sample,bruteForce,com.shatteredpixel.shatteredpixeldungeon.apd.qol.ThrowableTargeting.hasExistingTrajectory(hero.pos,enemy.pos));
            boolean direct=new Ballistica(hero.pos,enemy.pos,Ballistica.PROJECTILE).collisionPos==enemy.pos;
            boolean reverse=new Ballistica(enemy.pos,hero.pos,Ballistica.PROJECTILE).collisionPos==hero.pos;
            assertEquals(direct||reverse||bruteForce,new ThrowingKnife().throwPos(hero,enemy.pos)==enemy.pos);
        }
    }
    @Test public void longDistanceFallbackStillStopsForSolidWallsAndInterveningCreatures(){
        largeArena();enemy.pos=at(54,41);hero.fieldOfView[enemy.pos]=true;Level.set(at(53,41),Terrain.WALL);
        blocker=new Rat();blocker.pos=at(33,32);Actor.add(blocker);
        assertNotEquals(enemy.pos,new ThrowingKnife().throwPos(hero,enemy.pos));Actor.remove(blocker);blocker=null;
        for(int y=1;y<63;y++)Level.set(at(40,y),Terrain.WALL);
        assertNotEquals(enemy.pos,new ThrowingKnife().throwPos(hero,enemy.pos));
    }
    @Test public void searchingForAnExistingAimDoesNotSpendTurnsOrRandomness(){
        largeArena();enemy.pos=at(54,41);Level.set(at(53,41),Terrain.WALL);float time=hero.cooldown();int hp=hero.HP;
        com.watabou.utils.Random.pushGenerator(81126);int next=com.watabou.utils.Random.Int();com.watabou.utils.Random.popGenerator();
        com.watabou.utils.Random.pushGenerator(81126);try{assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.qol.ThrowableTargeting.hasExistingTrajectory(hero.pos,enemy.pos));assertEquals(next,com.watabou.utils.Random.Int());}finally{com.watabou.utils.Random.popGenerator();}
        assertEquals(time,hero.cooldown(),0);assertEquals(hp,hero.HP);
    }
    @Test public void intentionalProjectingEnchantmentRetainsItsExistingWallBypass(){
        for(int y=1;y<31;y++)Level.set(at(4,y),Terrain.WALL);
        ThrowingKnife weapon=new ThrowingKnife();weapon.enchantment=new Projecting();
        assertEquals(enemy.pos,weapon.throwPos(hero,enemy.pos));
    }
}
