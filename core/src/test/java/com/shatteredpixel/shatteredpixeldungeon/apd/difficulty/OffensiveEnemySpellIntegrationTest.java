package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class OffensiveEnemySpellIntegrationTest {
 static class Quiet extends CharSprite{@Override public void showStatus(int c,String s,Object...a){} @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}}
 static class Target extends Char{Object source;Target(){HP=HT=1000;sprite=new Quiet();sprite.visible=false;alignment=Alignment.ALLY;pos=17;}@Override public void damage(int d,Object s){source=s;super.damage(d,s);}}
 interface Caster{void fire(Char t)throws Exception;Mob mob();Class source();}
 static class TestWarlock extends Warlock implements Caster{public void fire(Char t){enemy=t;zap();}public Mob mob(){return this;}public Class source(){return DarkBolt.class;}@Override public int attackSkill(Char c){return INFINITE_ACCURACY;}}
 static class TestDM extends DM100 implements Caster{public void fire(Char t){Actor.add(t);try{doAttack(t);}finally{Actor.remove(t);}}public Mob mob(){return this;}public Class source(){return LightningBolt.class;}@Override public int attackSkill(Char c){return INFINITE_ACCURACY;}}
 static class TestWisp extends CrystalWisp implements Caster{public void fire(Char t)throws Exception{enemy=t;java.lang.reflect.Method m=CrystalWisp.class.getDeclaredMethod("zap");m.setAccessible(true);m.invoke(this);}public Mob mob(){return this;}public Class source(){return LightBeam.class;}@Override public int attackSkill(Char c){return INFINITE_ACCURACY;}}
 Hero hero;Level level;int depth;
 @Before public void setup()throws Exception{HeadlessTestSupport.start();hero=Dungeon.hero;level=Dungeon.level;depth=Dungeon.depth;Dungeon.hero=new Hero();Dungeon.hero.pos=6;Dungeon.depth=4;Dungeon.level=new SewerLevel();java.lang.reflect.Method m=Level.class.getDeclaredMethod("setSize",int.class,int.class);m.setAccessible(true);m.invoke(Dungeon.level,5,5);java.util.Arrays.fill(Dungeon.level.passable,true);}
 @After public void cleanup(){Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.hero=hero;Dungeon.level=level;Dungeon.depth=depth;}
 void rules(int p,int e,int b){Bundle run=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);run.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(run);}
 Caster caster(int type,int role){Caster c=type==0?new TestWarlock():type==1?new TestDM():new TestWisp();c.mob().pos=7;c.mob().sprite=new Quiet();c.mob().sprite.visible=false;if(role==0)c.mob().apdInheritOffensiveOrigin(new Hero());else if(role==2)c.mob().apdInheritOffensiveOrigin(new Goo());return c;}
 @Test public void realEnemyZapsHonorActualEnemyPlayerOrBossCreatorOnce()throws Exception{for(int type=0;type<3;type++)for(int role=0;role<3;role++)for(int pct=25;pct<=500;pct+=25){rules(100,100,100);Caster baseline=caster(type,role);Target a=new Target();Random.pushGenerator(2291);try{baseline.fire(a);}finally{Random.popGenerator();}int raw=1000-a.HP;assertTrue(raw>0);rules(role==0?pct:100,role==1?pct:100,role==2?pct:100);Caster c=caster(type,role);Target t=new Target();Random.pushGenerator(2291);try{c.fire(t);}finally{Random.popGenerator();}assertEquals(1000-Math.round(raw*pct/100f),t.HP);assertEquals(c.source(),t.source.getClass());}}
 @Test public void neutralAndModifiedZapsPreserveRngAndAttackTime()throws Exception{for(int type=0;type<3;type++){rules(100,100,100);Caster original=caster(type,1);long next;Random.pushGenerator(78421);try{original.fire(new Target());next=Random.Long();}finally{Random.popGenerator();}Bundle before=new Bundle();original.mob().storeInBundle(before);rules(100,500,25);Caster changed=caster(type,1);Random.pushGenerator(78421);try{changed.fire(new Target());assertEquals(next,Random.Long());}finally{Random.popGenerator();}Bundle after=new Bundle();changed.mob().storeInBundle(after);assertTrue(before.contains("time"));assertTrue(after.contains("time"));assertEquals(before.getFloat("time"),after.getFloat("time"),0);}}
}
