package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.*;
import java.nio.file.*;
import org.junit.*;
import static org.junit.Assert.*;
public class FireDamageProvenanceTest {
 static final int CELL=12, NEXT=13;
 static class Quiet extends CharSprite{@Override public void showStatus(int c,String s,Object...a){} @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}}
 static class Target extends Char{Object source;int calls;Target(int pos){HP=HT=1000;this.pos=pos;sprite=new Quiet();}@Override public void damage(int d,Object s){source=s;calls++;super.damage(d,s);}}
 Hero hero;Level level;int depth;Bundle run;java.util.Set<Actor> actors;
 @Before public void setup()throws Exception{HeadlessTestSupport.start();hero=Dungeon.hero;level=Dungeon.level;depth=Dungeon.depth;run=new Bundle();Dungeon.apdStoreCustomRules(run);actors=Actor.all();Dungeon.hero=new Hero();Dungeon.depth=14;Dungeon.level=new SewerLevel();java.lang.reflect.Method size=Level.class.getDeclaredMethod("setSize",int.class,int.class);size.setAccessible(true);size.invoke(Dungeon.level,5,5);java.util.Arrays.fill(Dungeon.level.map,Terrain.EMPTY);java.util.Arrays.fill(Dungeon.level.passable,true);Dungeon.level.blobs=new java.util.HashMap<>();Dungeon.level.heaps=new SparseArray<>();Dungeon.level.plants=new SparseArray<>();}
 @After public void cleanup(){for(Actor a:Actor.all())if(!actors.contains(a))Actor.remove(a);Dungeon.hero=hero;Dungeon.level=level;Dungeon.depth=depth;Dungeon.apdRestoreCustomRules(run);assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,OffensiveEffectContext.current());}
 void rules(int p,int e,int b){Bundle saved=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);saved.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(saved);}
 Fire seed(int cell,int amount,OffensiveDamageOrigin role){try(OffensiveEffectContext ignored=OffensiveEffectContext.enter(role)){Fire f=new Fire();f.seed(Dungeon.level,cell,amount);return f;}}
 void tickBurn(Target t,int raw){Burning b=t.buff(Burning.class);assertNotNull(b);Bundle state=new Bundle();b.storeInBundle(state);state.put("next_dmg",raw);b.restoreFromBundle(state);b.act();assertSame(b,t.source);}
 @Test public void fieldContactUsesEachDamageSliderExactlyOnce(){for(OffensiveDamageOrigin role:new OffensiveDamageOrigin[]{OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.BOSS})for(int pct=25;pct<=500;pct+=25){rules(role==OffensiveDamageOrigin.PLAYER?pct:100,role==OffensiveDamageOrigin.ENEMY?pct:100,role==OffensiveDamageOrigin.BOSS?pct:100);Fire f=seed(CELL,4,role);Target t=new Target(CELL);Actor.add(t);f.act();tickBurn(t,4);assertEquals(1000-Math.round(4*pct/100f),t.HP);assertEquals(1,t.calls);Actor.remove(t);}}
 @Test public void mergedFieldsBlendPerTickAndKeepOneDamageEvent(){rules(200,300,500);Fire f=seed(CELL,4,OffensiveDamageOrigin.PLAYER);try(OffensiveEffectContext ignored=OffensiveEffectContext.enter(OffensiveDamageOrigin.BOSS)){f.seed(Dungeon.level,CELL,4);}Target t=new Target(CELL);Actor.add(t);f.act();tickBurn(t,4);assertEquals(986,t.HP);assertEquals(1,t.calls);Actor.remove(t);}
 @Test public void spreadAndSaveReloadRetainCauseWhileLegacyRemainsUnknown(){rules(200,300,500);Dungeon.level.flamable[NEXT]=true;Fire original=seed(CELL,4,OffensiveDamageOrigin.ENEMY);Bundle saved=new Bundle();original.storeInBundle(saved);Fire restored=new Fire();restored.restoreFromBundle(saved);Target t=new Target(NEXT);Actor.add(t);restored.act();tickBurn(t,4);assertEquals(988,t.HP);Actor.remove(t);CellDamageOrigins.clearMetadata(saved,"apd_fire_origins");Fire legacy=new Fire();legacy.restoreFromBundle(saved);Target u=new Target(CELL);Actor.add(u);legacy.act();tickBurn(u,4);assertEquals(996,u.HP);Actor.remove(u);}
 @Test public void clearReuseAndNeutralTrackingDoNotKeepOwnershipOrChangeFire(){rules(200,300,500);Fire tagged=seed(CELL,4,OffensiveDamageOrigin.BOSS);tagged.clear(CELL);tagged.seed(Dungeon.level,CELL,4);Fire plain=seed(CELL,4,OffensiveDamageOrigin.UNATTRIBUTED);tagged.act();plain.act();assertArrayEquals(plain.cur,tagged.cur);assertEquals(plain.volume,tagged.volume);Bundle b=new Bundle();tagged.storeInBundle(b);assertFalse(b.contains("apd_fire_origins_version"));}
 @Test public void knownPlayerFireFieldsUseAttributedEntryPoint()throws Exception{Path root=Paths.get(System.getProperty("apd.game.src"));for(String path:new String[]{"items/potions/PotionOfLiquidFlame.java","items/wands/WandOfFireblast.java"}){String text=Files.readString(root.resolve(path));assertTrue(path,text.contains("Fire.seedAttributed("));}}
}

