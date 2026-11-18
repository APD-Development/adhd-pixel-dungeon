package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;
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
public class AttributedCorrosiveGasIntegrationTest {
 static class Quiet extends CharSprite{@Override public void showStatus(int c,String s,Object...a){} @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}}
 static class Target extends Char{boolean immune;Object source;Target(){HP=HT=1000;sprite=new Quiet();alignment=Alignment.ENEMY;pos=12;}@Override public void damage(int d,Object s){source=s;super.damage(d,s);}@Override public boolean isImmune(Class effect){return immune || super.isImmune(effect);}}
 Hero hero;Level level;int depth;Target t;
 @Before public void setup()throws Exception{HeadlessTestSupport.start();hero=Dungeon.hero;level=Dungeon.level;depth=Dungeon.depth;Dungeon.hero=new Hero();Dungeon.hero.pos=6;Dungeon.depth=4;Dungeon.level=new SewerLevel();java.lang.reflect.Method m=Level.class.getDeclaredMethod("setSize",int.class,int.class);m.setAccessible(true);m.invoke(Dungeon.level,5,5);t=new Target();Actor.add(t);}
 @After public void cleanup(){if(t.buff(Corrosion.class)!=null)t.buff(Corrosion.class).detach();Actor.remove(t);Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.hero=hero;Dungeon.level=level;Dungeon.depth=depth;}
 void rules(int p,int e,int b){Bundle run=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);run.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(run);}
 CorrosiveGas gas(Char source,int strength){CorrosiveGas g=new CorrosiveGas();g.seed(Dungeon.level,12,80);g.setStrengthAttributed(strength,WandOfCorrosion.class,source);return g;}
 @Test public void realGasAppliesPlayerEnemyBossCorrosionOnceAcrossAllSettings(){for(int pct=25;pct<=500;pct+=25)for(int role=0;role<3;role++){rules(role==0?pct:100,role==1?pct:100,role==2?pct:100);t.HP=1000;CorrosiveGas g=gas(role==0?new Hero():role==1?new Rat():new Goo(),7);g.act();Corrosion c=t.buff(Corrosion.class);assertNotNull(c);c.act();assertEquals(1000-Math.round(7*pct/100f),t.HP);assertSame(c,t.source);c.detach();}}
 @Test public void strongestCloudPreservesExistingSourcePolicyAndEqualStrengthDoesNotSteal(){rules(200,300,500);CorrosiveGas g=gas(new Goo(),7);g.setStrengthAttributed(7,null,new Hero());g.setStrengthAttributed(2,null,new Rat());g.act();t.buff(Corrosion.class).act();assertEquals(965,t.HP);Bundle b=new Bundle();g.storeInBundle(b);assertEquals(7,b.getInt("strength"));assertEquals(WandOfCorrosion.class,b.getClass("source"));assertEquals("BOSS",b.getString("apd_corrosive_gas_origin"));}
 @Test public void newAndLegacyGasReloadKeepsGeometryAndUnknownOwnership(){rules(200,300,500);CorrosiveGas g=gas(new Goo(),7);Bundle s=new Bundle();g.storeInBundle(s);CorrosiveGas restored=new CorrosiveGas();restored.restoreFromBundle(s);assertArrayEquals(g.cur,restored.cur);assertEquals(g.volume,restored.volume);restored.act();t.buff(Corrosion.class).act();assertEquals(965,t.HP);t.buff(Corrosion.class).detach();s.remove("apd_corrosive_gas_origin");CorrosiveGas old=new CorrosiveGas();old.restoreFromBundle(s);old.act();t.buff(Corrosion.class).act();assertEquals(958,t.HP);}
 @Test public void neutralDiffusionAndRngMatchUnknownCloud(){rules(100,100,100);CorrosiveGas owned=gas(new Hero(),7),unknown=new CorrosiveGas();unknown.seed(Dungeon.level,12,80);unknown.setStrength(7,WandOfCorrosion.class);long expected;Random.pushGenerator(447);try{expected=Random.Long();}finally{Random.popGenerator();}Random.pushGenerator(447);try{owned.act();unknown.act();assertEquals(expected,Random.Long());}finally{Random.popGenerator();}assertArrayEquals(unknown.cur,owned.cur);assertEquals(unknown.volume,owned.volume);}
 @Test public void gasImmunityStillBlocksAllCorrosion(){rules(200,300,500);t.immune=true;gas(new Goo(),7).act();assertNull(t.buff(Corrosion.class));assertEquals(1000,t.HP);}
 @Test public void unknownRoleRejectsWithoutModifyingSave(){Bundle s=new Bundle();gas(new Goo(),7).storeInBundle(s);s.put("apd_corrosive_gas_origin","FUTURE");try{new CorrosiveGas().restoreFromBundle(s);fail();}catch(IllegalArgumentException expected){}assertEquals("FUTURE",s.getString("apd_corrosive_gas_origin"));assertEquals(7,s.getInt("strength"));}
}
