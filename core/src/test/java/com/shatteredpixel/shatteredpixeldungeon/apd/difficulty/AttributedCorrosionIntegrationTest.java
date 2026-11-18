package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class AttributedCorrosionIntegrationTest {
 static class Quiet extends CharSprite{@Override public void showStatus(int c,String s,Object...a){} @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}}
 static class Target extends Char{Object source;Target(){HP=HT=1000;sprite=new Quiet();alignment=Alignment.ENEMY;}@Override public void damage(int d,Object s){source=s;super.damage(d,s);}}
 static class TestCorrosion extends Corrosion{void bind(Char c){target=c;}}
 Hero hero;Level level;int depth;
 @Before public void setup()throws Exception{HeadlessTestSupport.start();hero=Dungeon.hero;level=Dungeon.level;depth=Dungeon.depth;Dungeon.hero=new Hero();Dungeon.depth=4;Dungeon.level=new SewerLevel();java.lang.reflect.Method m=Level.class.getDeclaredMethod("setSize",int.class,int.class);m.setAccessible(true);m.invoke(Dungeon.level,5,5);}
 @After public void cleanup(){Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.hero=hero;Dungeon.level=level;Dungeon.depth=depth;}
 void rules(int p,int e,int b){Bundle run=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);run.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(run);}
 @Test public void everyPlayerEnemyBossPercentageScalesRealTickOnce(){for(int pct=25;pct<=500;pct+=25)for(int role=0;role<3;role++){rules(role==0?pct:100,role==1?pct:100,role==2?pct:100);Target t=new Target();TestCorrosion c=new TestCorrosion();c.bind(t);c.setAttributed(4,7,null,role==0?new Hero():role==1?new Rat():new Goo());c.act();assertEquals(1000-Math.round(7*pct/100f),t.HP);assertSame(c,t.source);Bundle s=new Bundle();c.storeInBundle(s);assertEquals(7.5f,s.getFloat("damage"),0);assertEquals(3f,s.getFloat("left"),0);}}
 @Test public void neutralTickProgressionForecastAndRngMatchOriginal(){rules(100,100,100);Target t=new Target();TestCorrosion c=new TestCorrosion();c.bind(t);c.setAttributed(3,2,WandOfCorrosion.class,new Hero());long expected;Random.pushGenerator(7721);try{expected=Random.Long();}finally{Random.popGenerator();}Random.pushGenerator(7721);try{assertEquals(9,c.totalIncomingDMG());c.act();c.act();assertEquals(expected,Random.Long());}finally{Random.popGenerator();}assertEquals(995,t.HP);Bundle s=new Bundle();c.storeInBundle(s);assertEquals(4f,s.getFloat("damage"),0);assertEquals(1f,s.getFloat("left"),0);assertEquals(WandOfCorrosion.class,s.getClass("source"));}
 @Test public void newAndLegacySaveReloadPreservesSourceAndNeverInventsOwnership(){rules(200,300,500);Target t=new Target();TestCorrosion c=new TestCorrosion();c.bind(t);c.setAttributed(4,8,WandOfCorrosion.class,new Goo());Bundle s=new Bundle();c.storeInBundle(s);TestCorrosion restored=new TestCorrosion();restored.restoreFromBundle(s);restored.bind(t);assertEquals(170,restored.totalIncomingDMG());restored.act();assertEquals(960,t.HP);s.remove("apd_corrosion_origin");TestCorrosion old=new TestCorrosion();old.restoreFromBundle(s);Target u=new Target();old.bind(u);old.act();assertEquals(992,u.HP);assertEquals(WandOfCorrosion.class,s.getClass("source"));}
 @Test public void overlapsKeepOriginalMaxStrengthMaxDurationAndLatestSourcePolicy(){rules(200,300,500);TestCorrosion c=new TestCorrosion();Target t=new Target();c.bind(t);c.setAttributed(5,8,WandOfCorrosion.class,new Goo());c.setAttributed(2,2,null,new Hero());Bundle s=new Bundle();c.storeInBundle(s);assertEquals(8f,s.getFloat("damage"),0);assertEquals(5f,s.getFloat("left"),0);assertEquals("PLAYER",s.getString("apd_corrosion_origin"));c.act();assertEquals(984,t.HP);c.set(1,1);c.act();assertEquals(976,t.HP);}
 @Test public void forecastAndExtensionsDoNotScaleStoredRawDamageOrConsumeRng(){rules(200,300,500);TestCorrosion c=new TestCorrosion();c.bind(new Target());c.setAttributed(2,4,null,new Rat());c.extend(1);Bundle before=new Bundle();c.storeInBundle(before);assertEquals(39,c.totalIncomingDMG());Bundle after=new Bundle();c.storeInBundle(after);assertEquals(before.getFloat("damage"),after.getFloat("damage"),0);assertEquals(before.getFloat("left"),after.getFloat("left"),0);assertEquals(before.getString("apd_corrosion_origin"),after.getString("apd_corrosion_origin"));}
 @Test public void unknownOriginRejectsWithoutRewritingExistingSave(){Bundle s=new Bundle();s.put("damage",4f);s.put("left",2f);s.put("apd_corrosion_origin","FUTURE_UNKNOWN");try{new TestCorrosion().restoreFromBundle(s);fail();}catch(IllegalArgumentException expected){}assertEquals("FUTURE_UNKNOWN",s.getString("apd_corrosion_origin"));assertEquals(4f,s.getFloat("damage"),0);}
}
