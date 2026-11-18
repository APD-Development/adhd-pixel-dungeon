package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class OffensiveBossSpellIntegrationTest {
 static class Quiet extends CharSprite{@Override public void showStatus(int c,String s,Object...a){} @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}}
 static class Target extends Char{Object source;Target(){HP=HT=1000;sprite=new Quiet();sprite.visible=false;alignment=Alignment.ALLY;}@Override public void damage(int v,Object s){source=s;super.damage(v,s);}}
 static class Bright extends YogFist.BrightFist{void fire(Char t){enemy=t;zap();}@Override public int attackSkill(Char t){return INFINITE_ACCURACY;}}
 static class Dark extends YogFist.DarkFist{void fire(Char t){enemy=t;zap();}@Override public int attackSkill(Char t){return INFINITE_ACCURACY;}}
 Hero prior;
 @Before public void setup(){HeadlessTestSupport.start();prior=Dungeon.hero;Dungeon.hero=new Hero();}
 @After public void cleanup(){Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.hero=prior;}
 void rules(int enemy,int boss){Bundle a=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_DAMAGE,enemy).with(CustomDungeonRules.Slider.BOSS_DAMAGE,boss).store(r);a.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(a);}
 @Test public void actualBrightAndDarkZapUseOnlyBossFactorAndKeepSourceClasses(){for(int percent=25;percent<=500;percent+=25){rules(500,percent);int raw;Random.pushGenerator(91428);try{raw=Random.NormalIntRange(10,20);}finally{Random.popGenerator();}Target b=new Target();Random.pushGenerator(91428);try{new Bright().fire(b);}finally{Random.popGenerator();}assertEquals(1000-Math.round(raw*percent/100f),b.HP);assertEquals(YogFist.BrightFist.LightBeam.class,b.source.getClass());Target d=new Target();Random.pushGenerator(91428);try{new Dark().fire(d);}finally{Random.popGenerator();}assertEquals(1000-Math.round(raw*percent/100f),d.HP);assertEquals(YogFist.DarkFist.DarkBolt.class,d.source.getClass());}}
 @Test public void neutralRealBossZapKeepsOriginalRollAndNextRng(){rules(100,100);int raw;long next;Random.pushGenerator(55104);try{raw=Random.NormalIntRange(10,20);next=Random.Long();}finally{Random.popGenerator();}Target t=new Target();Random.pushGenerator(55104);try{new Dark().fire(t);assertEquals(next,Random.Long());}finally{Random.popGenerator();}assertEquals(1000-raw,t.HP);}
}
