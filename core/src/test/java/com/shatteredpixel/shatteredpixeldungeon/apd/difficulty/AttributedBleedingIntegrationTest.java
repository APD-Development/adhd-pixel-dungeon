package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class AttributedBleedingIntegrationTest {
 static class Quiet extends CharSprite{@Override public void showStatus(int c,String s,Object...a){} @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}}
 static class Target extends Char{Object source;Target(){HP=HT=1000;sprite=new Quiet();sprite.visible=false;alignment=Alignment.ENEMY;}@Override public void damage(int v,Object s){source=s;super.damage(v,s);}}
 static class TestBleed extends Bleeding{void bind(Char c){target=c;}}
 Hero prior;
 @Before public void setup(){HeadlessTestSupport.start();prior=Dungeon.hero;Dungeon.hero=new Hero();}
 @After public void cleanup(){Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.hero=prior;}
 void rules(int p,int e,int b){Bundle a=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);a.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(a);}
 @Test public void realTickUsesSourceAndPreservesRawDecay(){rules(200,300,500);TestBleed b=new TestBleed();Target t=new Target();b.bind(t);b.setAttributed(20,new Goo());int raw=Math.round(b.level());b.act();assertEquals(1000-raw*5,t.HP);assertSame(b,t.source);assertTrue(b.level()<=20);}
 @Test public void weakerReapplicationDoesNotStealStrongerSource(){rules(200,300,500);TestBleed b=new TestBleed();Target t=new Target();b.bind(t);b.setAttributed(40,new Rat());int raw=Math.round(b.level());b.setAttributed(1,new Goo());b.act();assertEquals(1000-raw*3,t.HP);}
 @Test public void reloadPreservesOriginAndUnknownLegacyStaysUnscaled(){rules(200,300,500);TestBleed b=new TestBleed();b.setAttributed(20,new Hero());Bundle saved=new Bundle();b.storeInBundle(saved);TestBleed copy=new TestBleed();copy.restoreFromBundle(saved);Target t=new Target();copy.bind(t);int raw=Math.round(copy.level());copy.act();assertEquals(1000-raw*2,t.HP);Bundle legacy=new Bundle();legacy.put("level",20f);TestBleed old=new TestBleed();old.restoreFromBundle(legacy);Target u=new Target();old.bind(u);old.act();assertEquals(980,u.HP);}
 @Test public void extensionsScaleOnlyTheirOwnAmplitudeAndRoundOnce(){rules(200,300,500);AmplitudeDamageOrigins o=AmplitudeDamageOrigins.single(OffensiveDamageOrigin.ENEMY);o.add(10,10,OffensiveDamageOrigin.PLAYER);assertEquals(50,o.scale(20));Bundle save=new Bundle();o.store(save,"x");assertEquals(50,AmplitudeDamageOrigins.restore(save,"x").scale(20));o.add(20,20,OffensiveDamageOrigin.UNATTRIBUTED);assertEquals(70,o.scale(40));assertEquals(Integer.MIN_VALUE,o.scale(Integer.MIN_VALUE));}
 @Test public void neutralSetAndActualTickKeepExactRng(){rules(100,100,100);Random.pushGenerator(91245);float level;float nextLevel;long next;try{level=Random.NormalFloat(10,20);nextLevel=Random.NormalFloat(level/2,level);next=Random.Long();}finally{Random.popGenerator();}Random.pushGenerator(91245);try{TestBleed b=new TestBleed();Target t=new Target();b.bind(t);b.setAttributed(20,new Hero());assertEquals(level,b.level(),0);b.act();assertEquals(nextLevel,b.level(),0);assertEquals(1000-Math.round(level),t.HP);assertEquals(next,Random.Long());}finally{Random.popGenerator();}}
 @Test public void invalidOptionalMetadataIsRejectedWithoutChangingBundle(){Bundle b=new Bundle();b.put("x_version",1);b.put("x_shares",new String[]{"NaN","0","0","1"});try{AmplitudeDamageOrigins.restore(b,"x");fail();}catch(IllegalArgumentException expected){}assertEquals("NaN",b.getStringArray("x_shares")[0]);assertNull(AmplitudeDamageOrigins.restore(new Bundle(),"x"));}
 @Test public void alreadyScaledConvertedDamageIsNotMultipliedAgain(){rules(200,300,500);TestBleed b=new TestBleed();Target t=new Target();b.bind(t);b.set(40,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sickle.HarvestBleedTracker.class);int raw=Math.round(b.level());b.act();assertEquals(1000-raw,t.HP);assertSame(b,t.source);}
 @Test public void realMixedExtensionAndSavedDecayKeepCauseShares(){rules(200,300,500);TestBleed b=new TestBleed();Target t=new Target();b.bind(t);b.setAttributed(20,new Rat());float old=b.level();b.extendAttributed(old,new Hero());int raw=Math.round(b.level());Bundle saved=new Bundle();b.storeInBundle(saved);TestBleed copy=new TestBleed();copy.restoreFromBundle(saved);copy.bind(t);copy.act();assertEquals(1000-Math.round(raw*2.5),t.HP);}
}
