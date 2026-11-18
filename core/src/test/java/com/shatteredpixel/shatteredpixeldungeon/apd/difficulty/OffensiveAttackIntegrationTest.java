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
package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class OffensiveAttackIntegrationTest {
 static class Quiet extends CharSprite {
  @Override public void showStatus(int c,String s,Object...a){}
  @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}
 }
 static class Board extends Level {protected boolean build(){return true;}protected void createMobs(){}protected void createItems(){}}
 static class Enemy extends Char {
  Enemy(boolean boss){HP=HT=1000;alignment=Alignment.ENEMY;pos=23;sprite=new Quiet();sprite.visible=false;if(boss)properties.add(Property.BOSS);}
  @Override public int attackSkill(Char enemy){return 10000;}
  @Override public int damageRoll(){return 20;}
 }
 static class Player extends Hero {
  Player(){HP=HT=1000;pos=23;sprite=new Quiet();sprite.visible=false;fieldOfView=new boolean[49];}
  @Override public int attackSkill(Char enemy){return 10000;}
  @Override public int damageRoll(){return 20;}
 }
 static class Receiver extends Char {
  Object source;int incoming;
  @Override public int defenseProc(Char enemy,int damage){incoming=damage;return super.defenseProc(enemy,damage);}
  Receiver(){HP=HT=1000;pos=24;alignment=Alignment.ENEMY;sprite=new Quiet();sprite.visible=false;}
  @Override public int defenseSkill(Char enemy){return 0;}
  @Override public int drRoll(){return 5;}
  @Override public void damage(int amount,Object from){source=from;super.damage(amount,from);}
 }
 private Hero priorHero;private Level priorLevel;private boolean recording;
 @Before public void setup(){HeadlessTestSupport.start();priorHero=Dungeon.hero;priorLevel=Dungeon.level;recording=SPDSettings.apdRecording();SPDSettings.apdRecording(false);
  Dungeon.hero=new Player();Board board=new Board();board.setSize(7,7);Dungeon.level=board;Dungeon.difficulty=3;Dungeon.difficultyBalance=0;Dungeon.challenges=0;}
 @After public void clear(){Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.hero=priorHero;Dungeon.level=priorLevel;SPDSettings.apdRecording(recording);}
 private void rules(int p,int e,int b){Bundle all=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);all.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(all);}
 private Receiver hit(Char actor){Receiver target=new Receiver();Random.pushGenerator(89237);try{assertTrue(actor.attack(target));}finally{Random.popGenerator();}return target;}
 @Test public void actualAttackScalesBeforeArmorAndBossReplacesEnemy(){
  rules(300,200,500);Char player=new Player();Char enemy=new Enemy(false);Char boss=new Enemy(true);
  Receiver a=hit(player),b=hit(enemy),c=hit(boss);assertEquals(945,a.HP);assertEquals(965,b.HP);assertEquals(905,c.HP);
  assertSame(player,a.source);assertSame(enemy,b.source);assertSame(boss,c.source);
  Char summon=new Enemy(false);summon.apdInheritOffensiveOrigin(boss);assertEquals(905,hit(summon).HP);
 }
 @Test public void neutralAttackKeepsRealEngineDamageAndNextRng(){
  rules(100,100,100);Char enemy=new Enemy(false);Random.pushGenerator(89237);long next;
  try{Receiver r=new Receiver();assertTrue(enemy.attack(r));assertEquals(985,r.HP);next=Random.Long();}finally{Random.popGenerator();}
  rules(100,100,500);Random.pushGenerator(89237);try{Receiver r=new Receiver();assertTrue(enemy.attack(r));assertEquals(985,r.HP);assertEquals(next,Random.Long());}finally{Random.popGenerator();}
 }
 @Test public void rawSecondaryOffensePreservesItsSourceAndLeavesUnattributedCostsUnscaled(){
  rules(300,200,500);Object source=new Object();Receiver a=new Receiver();CustomOffensiveHooks.raw(a,20,source,new Player());assertEquals(940,a.HP);assertSame(source,a.source);
  Receiver unowned=new Receiver();CustomOffensiveHooks.raw(unowned,20,source,null);assertEquals(980,unowned.HP);
  assertEquals(-1,CustomOffensiveHooks.attackDamage(new Player(),-1),0);
  assertEquals(Float.MAX_VALUE,CustomOffensiveHooks.attackDamage(null,Float.MAX_VALUE),0);
 }

 @Test public void realStartingWeaponsForAllSixClassesUseTheSameSinglePreArmorFactor(){
  CustomDungeonRules prefs=SPDSettings.apdCustomRules();com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass chosen=GamesInProgress.selectedClass;
  int difficulty=SPDSettings.apdDifficulty(),challenges=SPDSettings.challenges();boolean intro=SPDSettings.intro();
  try{
   SPDSettings.apdDifficulty(3);SPDSettings.challenges(0);SPDSettings.intro(false);
   for(var type:com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.values()){
    int neutralIncoming=0;
    for(int percent:new int[]{100,25,200,500}){
     SPDSettings.apdCustomRules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,percent));GamesInProgress.selectedClass=type;
     Dungeon.seed=7831;Dungeon.customSeedText="";Dungeon.daily=false;Dungeon.dailyReplay=false;Dungeon.init();
     Board board=new Board();board.setSize(7,7);Dungeon.level=board;Hero hero=Dungeon.hero;hero.pos=23;hero.fieldOfView=new boolean[49];hero.sprite=new Quiet();hero.sprite.visible=false;
     Receiver target=hit(hero);
     if(percent==100)neutralIncoming=target.incoming;
     assertEquals(type+" incoming offense",Math.round(neutralIncoming*percent/100f),target.incoming);
     assertEquals(type+" armor applied afterward",1000-Math.max(0,target.incoming-5),target.HP);
    }
   }
  }finally{SPDSettings.apdCustomRules(prefs);GamesInProgress.selectedClass=chosen;SPDSettings.apdDifficulty(difficulty);SPDSettings.challenges(challenges);SPDSettings.intro(intro);}
 }
 @Test public void weaknessAndVulnerabilityDescriptionsAgreeWithActualArmorOrderAndRounding(){
  rules(100,100,100);Enemy attacker=new Enemy(false);Receiver target=new Receiver();
  com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness weak=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(attacker,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness.class);
  com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable vulnerable=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(target,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable.class);
  Random.pushGenerator(89237);try{assertTrue(attacker.attack(target));}finally{Random.popGenerator();}
  assertEquals(Math.round(20*.67f),target.incoming);assertEquals(1000-(int)((target.incoming-5)*1.33f),target.HP);
  assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.MechanicsDetails.buff(weak,weak.desc()).contains("33% less damage before armor and rounding"));
  assertTrue(com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.MechanicsDetails.buff(vulnerable,vulnerable.desc()).contains("33% more of the damage left after armor, rounded down"));
 }
}
