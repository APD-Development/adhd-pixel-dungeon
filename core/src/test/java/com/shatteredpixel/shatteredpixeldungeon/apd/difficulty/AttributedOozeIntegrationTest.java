package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class AttributedOozeIntegrationTest {
 static class Quiet extends CharSprite{@Override public void showStatus(int c,String s,Object...a){} @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}}
 static class Target extends Char{Object source;Target(){HP=HT=1000;sprite=new Quiet();alignment=Alignment.ENEMY;pos=12;}@Override public void damage(int d,Object s){source=s;super.damage(d,s);}}
 static class TestOoze extends Ooze{void bind(Char c){target=c;}}
 Hero hero;Level level;int depth;
 @Before public void setup()throws Exception{HeadlessTestSupport.start();hero=Dungeon.hero;level=Dungeon.level;depth=Dungeon.depth;Dungeon.hero=new Hero();Dungeon.depth=4;Dungeon.level=new SewerLevel();java.lang.reflect.Method m=Level.class.getDeclaredMethod("setSize",int.class,int.class);m.setAccessible(true);m.invoke(Dungeon.level,5,5);}
 @After public void cleanup(){Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.hero=hero;Dungeon.level=level;Dungeon.depth=depth;}
 void rules(int p,int e,int b){Bundle run=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);run.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(run);}
 @Test public void allDamagePercentagesAcrossSewerGooAndDeepFloorTicks(){for(int d:new int[]{4,5,14})for(int pct=25;pct<=500;pct+=25)for(int role=0;role<3;role++){Dungeon.depth=d;rules(role==0?pct:100,role==1?pct:100,role==2?pct:100);Target t=new Target();TestOoze o=new TestOoze();o.bind(t);o.setAttributed(4,role==0?new Hero():role==1?new Rat():new Goo());o.act();int raw=d>5?1+d/5:1;assertEquals(1000-Math.round(raw*pct/100f),t.HP);assertSame(o,t.source);Bundle s=new Bundle();o.storeInBundle(s);assertEquals(3f,s.getFloat("left"),0);}}
 @Test public void neutralPreservesSewerAlternatingTicksAndRng(){rules(100,100,100);Target t=new Target();TestOoze o=new TestOoze();o.bind(t);o.setAttributed(4,new Hero());long expected;Random.pushGenerator(665);try{expected=Random.Long();}finally{Random.popGenerator();}Random.pushGenerator(665);try{assertEquals(2,o.totalIncomingDMG());o.act();assertEquals(999,t.HP);o.act();assertEquals(999,t.HP);o.act();assertEquals(998,t.HP);assertEquals(expected,Random.Long());}finally{Random.popGenerator();}}
 @Test public void savesRetainBossOwnershipAndLegacyHasNoInventedAttacker(){Dungeon.depth=14;rules(200,300,500);TestOoze o=new TestOoze();Target t=new Target();o.bind(t);o.setAttributed(4,new Goo());Bundle s=new Bundle();o.storeInBundle(s);TestOoze restored=new TestOoze();restored.restoreFromBundle(s);restored.bind(t);restored.act();assertEquals(985,t.HP);s.remove("apd_ooze_origin");TestOoze old=new TestOoze();old.restoreFromBundle(s);Target u=new Target();old.bind(u);old.act();assertEquals(997,u.HP);}
 @Test public void replacementsExtensionsAndForecastDoNotMultiplyTwice(){Dungeon.depth=14;rules(200,300,500);TestOoze o=new TestOoze();o.bind(new Target());o.setAttributed(4,new Goo());o.setAttributed(2,new Hero());o.extend(1);Bundle before=new Bundle();o.storeInBundle(before);assertEquals(18,o.totalIncomingDMG());Bundle after=new Bundle();o.storeInBundle(after);assertEquals(3f,after.getFloat("left"),0);assertEquals(before.getString("apd_ooze_origin"),after.getString("apd_ooze_origin"));o.set(2);assertEquals(6,o.totalIncomingDMG());}
 @Test public void waterWashRetainsOriginalFirstTickAndNoLaterDamage(){Dungeon.depth=14;rules(200,300,500);Target t=new Target();TestOoze o=new TestOoze();o.bind(t);o.setAttributed(4,new Goo());Dungeon.level.water[t.pos]=true;o.act();assertEquals(985,t.HP);o.act();assertEquals(985,t.HP);}
 @Test public void malformedOriginDoesNotRewriteSavedData(){Bundle s=new Bundle();s.put("left",4f);s.put("apd_ooze_origin","FUTURE");try{new TestOoze().restoreFromBundle(s);fail();}catch(IllegalArgumentException expected){}assertEquals("FUTURE",s.getString("apd_ooze_origin"));assertEquals(4f,s.getFloat("left"),0);}
}
