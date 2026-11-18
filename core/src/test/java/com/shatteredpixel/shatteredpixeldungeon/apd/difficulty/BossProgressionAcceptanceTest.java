package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.*;
import java.util.*;
import java.lang.reflect.*;
import org.junit.*;
import static org.junit.Assert.*;
/** Real boss damage/phase/save logic; rendering, jump animation and pylon visual callbacks are isolated. */
public class BossProgressionAcceptanceTest {
 private int oldLogLevel;private Object oldBossBar;private boolean oldBleeding;
 private Hero oldHero;private Level oldLevel;private int oldDifficulty,oldChallenges,oldBalance;private final Bundle oldRules=new Bundle();private Set<Actor> oldActors;
 private static class Quiet extends CharSprite {Quiet(){visible=false;}@Override public void showStatus(int c,String t,Object...a){}@Override public void showStatusWithIcon(int c,String t,int i,Object...a){}}
 private static class Prison extends PrisonBossLevel {
  State stage=State.FIGHT_START;int transitions;
  @Override public State state(){return stage;}
  @Override public void progress(){transitions++;stage=stage==State.FIGHT_START?State.FIGHT_ARENA:State.WON;}
 }
 private static class TenguProbe extends Tengu {@Override public void yell(String s){}@Override public void notice(){}}
 private static class DMProbe extends DM300 {
  boolean died;
  @Override public void notice(){}
  @Override public void supercharge(){supercharged=true;pylonsActivated++;}
  @Override public void loseSupercharge(){supercharged=false;}
  @Override public void die(Object source){died=true;}
 }
 @Before public void before()throws Exception{HeadlessTestSupport.start();oldLogLevel=com.badlogic.gdx.Gdx.app.getLogLevel();com.badlogic.gdx.Gdx.app.setLogLevel(com.badlogic.gdx.Application.LOG_ERROR);Field bar=com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar.class.getDeclaredField("boss");bar.setAccessible(true);oldBossBar=bar.get(null);Field bleed=com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar.class.getDeclaredField("bleeding");bleed.setAccessible(true);oldBleeding=bleed.getBoolean(null);oldHero=Dungeon.hero;oldLevel=Dungeon.level;oldDifficulty=Dungeon.difficulty;oldBalance=Dungeon.difficultyBalance;oldChallenges=Dungeon.challenges;Dungeon.apdStoreCustomRules(oldRules);oldActors=Actor.all();}
 @After public void after()throws Exception{com.badlogic.gdx.Gdx.app.setLogLevel(oldLogLevel);Field bar=com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar.class.getDeclaredField("boss");bar.setAccessible(true);bar.set(null,oldBossBar);Field bleed=com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar.class.getDeclaredField("bleeding");bleed.setAccessible(true);bleed.setBoolean(null,oldBleeding);for(Actor a:Actor.all())if(!oldActors.contains(a))Actor.remove(a);Dungeon.hero=oldHero;Dungeon.level=oldLevel;Dungeon.difficulty=oldDifficulty;Dungeon.difficultyBalance=oldBalance;Dungeon.challenges=oldChallenges;Dungeon.apdRestoreCustomRules(oldRules);}
 private void fixture(HeroClass heroClass,int difficulty,int challenge,int hp,Level level){
  Dungeon.difficulty=difficulty;Dungeon.difficultyBalance=Difficulty.balanceRevision(difficulty);Dungeon.challenges=challenge;
  Bundle b=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.BOSS_HP,hp).with(CustomDungeonRules.Slider.PLAYER_DAMAGE,500).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,25).with(CustomDungeonRules.Slider.BOSS_DAMAGE,500).store(r);b.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(b);
  Dungeon.hero=new Hero();Dungeon.hero.heroClass=heroClass;Talent.initClassTalents(Dungeon.hero);Dungeon.hero.sprite=new Quiet();Dungeon.hero.pos=23;
  Dungeon.level=level;level.setSize(7,7);Arrays.fill(level.map,Terrain.EMPTY);Arrays.fill(level.passable,true);level.mobs=new HashSet<>();level.blobs=new HashMap<>();level.heaps=new SparseArray<>();level.plants=new SparseArray<>();
 }
 private static void discardNewActors(Set<Actor> previous){for(Actor a:Actor.all())if(!previous.contains(a))Actor.remove(a);}
 @Test public void tenguCannotSkipEitherFightAtAnyBossHpClassOrDifficulty()throws Exception {
  int cases=0;
  for(int difficulty=1;difficulty<=5;difficulty++)for(int challenge:new int[]{0,Challenges.STRONGER_BOSSES})for(int hp=25;hp<=500;hp+=25)for(HeroClass hero:HeroClass.values()){
   Prison prison=new Prison();fixture(hero,difficulty,challenge,hp,prison);TenguProbe boss=new TenguProbe();boss.sprite=new Quiet();boss.pos=24;prison.mobs.add(boss);CustomMobHooks.applyHealth(boss);int maximum=boss.HT;boolean reloaded=false;
   for(int hits=0;prison.stage==PrisonBossLevel.State.FIGHT_START&&hits<16;hits++){
    Set<Actor> before=Actor.all();CustomOffensiveHooks.raw(boss,1_000_000,Dungeon.hero,Dungeon.hero);discardNewActors(before);
    if(!reloaded&&prison.stage==PrisonBossLevel.State.FIGHT_START){Bundle saved=new Bundle();boss.storeInBundle(saved);prison.mobs.remove(boss);boss=new TenguProbe();boss.restoreFromBundle(saved);boss.sprite=new Quiet();prison.mobs.add(boss);assertEquals(maximum,boss.HT);reloaded=true;}
   }
   assertEquals(PrisonBossLevel.State.FIGHT_ARENA,prison.stage);assertEquals(maximum/2,boss.HP);assertEquals(1,prison.transitions);
   for(int hits=0;boss.HP>0&&hits<16;hits++){
    Set<Actor> before=Actor.all();CustomOffensiveHooks.raw(boss,1_000_000,Dungeon.hero,Dungeon.hero);
    if(boss.HP==0)for(Actor actor:Actor.all())if(!before.contains(actor)){Method act=actor.getClass().getDeclaredMethod("act");act.setAccessible(true);act.invoke(actor);}
    discardNewActors(before);
   }
   assertEquals(0,boss.HP);assertEquals(PrisonBossLevel.State.WON,prison.stage);assertEquals(2,prison.transitions);cases++;
  }
  assertEquals(1200,cases);System.out.println("TENGU_PHASE_MATRIX cases="+cases+" (headless; visual callbacks isolated)");
 }
 @Test public void dm300RequiresEveryPylonAndPreservesSuperchargeAcrossReload()throws Exception {
  int cases=0;
  for(int difficulty=1;difficulty<=5;difficulty++)for(int challenge:new int[]{0,Challenges.STRONGER_BOSSES})for(int hp=25;hp<=500;hp+=25)for(HeroClass hero:HeroClass.values()){
   fixture(hero,difficulty,challenge,hp,new CavesBossLevel());DMProbe boss=new DMProbe();boss.sprite=new Quiet();boss.pos=24;Dungeon.level.mobs.add(boss);CustomMobHooks.applyHealth(boss);int maximum=boss.HT,total=boss.totalPylonsToActivate();
   for(int objective=0;objective<total;objective++){
    int threshold=challenge==0?maximum/3*(2-objective):maximum/4*(3-objective);
    CustomOffensiveHooks.raw(boss,1_000_000,Dungeon.hero,Dungeon.hero);assertEquals(threshold,boss.HP);assertTrue(boss.supercharged);assertEquals(objective+1,boss.pylonsActivated);assertFalse(boss.died);
    CustomOffensiveHooks.raw(boss,1_000_000,Dungeon.hero,Dungeon.hero);assertEquals(threshold,boss.HP);assertEquals(objective+1,boss.pylonsActivated);
    Bundle saved=new Bundle();boss.storeInBundle(saved);Dungeon.level.mobs.remove(boss);boss=new DMProbe();boss.restoreFromBundle(saved);boss.sprite=new Quiet();Dungeon.level.mobs.add(boss);assertEquals(maximum,boss.HT);assertEquals(threshold,boss.HP);assertTrue(boss.supercharged);assertEquals(objective+1,boss.pylonsActivated);boss.loseSupercharge();
   }
   CustomOffensiveHooks.raw(boss,1_000_000,Dungeon.hero,Dungeon.hero);assertEquals(0,boss.HP);assertTrue(boss.died);assertEquals(total,boss.pylonsActivated);cases++;
  }
  assertEquals(1200,cases);System.out.println("DM300_PHASE_MATRIX cases="+cases+" (headless; visual callbacks isolated)");
 }
}
