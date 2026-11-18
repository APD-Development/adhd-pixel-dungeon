package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class BombDamageProvenanceTest{
 static class Quiet extends CharSprite{@Override public void showStatus(int c,String s,Object...a){} @Override public void showStatusWithIcon(int c,String s,int i,Object...a){}}
 static class Target extends Char{Object source;Target(){HP=HT=1000;pos=12;sprite=new Quiet();} @Override public int drRoll(){return 3;} @Override public void damage(int d,Object s){source=s;super.damage(d,s);}}
 static class DropLevel extends SewerLevel{@Override public com.shatteredpixel.shatteredpixeldungeon.items.Heap drop(com.shatteredpixel.shatteredpixeldungeon.items.Item item,int cell){com.shatteredpixel.shatteredpixeldungeon.items.Heap heap=new com.shatteredpixel.shatteredpixeldungeon.items.Heap();heap.pos=cell;heap.items.add(item);heap.sprite=new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(){@Override public void drop(int from){}};heaps.put(cell,heap);return heap;}}
 static class CaptureBomb extends Bomb{Bundle captured;boolean fail;@Override public void explode(int cell){captured=new Bundle();storeInBundle(captured);if(fail)throw new IllegalStateException("test");}}
 Hero hero;Level level;int depth;Bundle run;
 @Before public void setup()throws Exception{HeadlessTestSupport.start();hero=Dungeon.hero;level=Dungeon.level;depth=Dungeon.depth;run=new Bundle();Dungeon.apdStoreCustomRules(run);Dungeon.hero=new Hero();Dungeon.depth=4;Dungeon.level=new SewerLevel();java.lang.reflect.Method m=Level.class.getDeclaredMethod("setSize",int.class,int.class);m.setAccessible(true);m.invoke(Dungeon.level,5,5);java.util.Arrays.fill(Dungeon.level.map,Terrain.EMPTY);Dungeon.level.heaps=new SparseArray<>();}
 @After public void cleanup(){Dungeon.hero=hero;Dungeon.level=level;Dungeon.depth=depth;Dungeon.apdRestoreCustomRules(run);}
 void rules(int p,int e,int b){Bundle saved=new Bundle(),r=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,p).with(CustomDungeonRules.Slider.ENEMY_DAMAGE,e).with(CustomDungeonRules.Slider.BOSS_DAMAGE,b).store(r);saved.put("apd_custom_rules",r);Dungeon.apdRestoreCustomRules(saved);}
 @Test public void explosionsScaleExactlyOnceBeforeArmorForAllSourceRoles(){for(OffensiveDamageOrigin role:new OffensiveDamageOrigin[]{OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.BOSS})for(int pct=25;pct<=500;pct+=25){rules(role==OffensiveDamageOrigin.PLAYER?pct:100,role==OffensiveDamageOrigin.ENEMY?pct:100,role==OffensiveDamageOrigin.BOSS?pct:100);int raw;Random.pushGenerator(551);try{raw=Random.NormalIntRange(8,24);}finally{Random.popGenerator();}Target t=new Target();Actor.add(t);Bomb bomb=new Bomb();Random.pushGenerator(551);try{bomb.apdExplodeAttributed(12,role);assertEquals(1000-Math.max(0,Math.round(raw*pct/100f)-3),t.HP);if(Math.round(raw*pct/100f)>3)assertSame(bomb,t.source);else assertNull(t.source);}finally{Random.popGenerator();Actor.remove(t);}}}
 @Test public void unownedExplosionsRemainEnvironmentalAndNeutralPreservesRng(){rules(500,500,500);Target t=new Target();Actor.add(t);Bomb bomb=new Bomb();int raw;long next;Random.pushGenerator(43);try{raw=Random.NormalIntRange(8,24);next=Random.Long();}finally{Random.popGenerator();}Random.pushGenerator(43);try{bomb.explode(12);assertEquals(1000-(raw-3),t.HP);assertEquals(next,Random.Long());}finally{Random.popGenerator();Actor.remove(t);}rules(100,100,100);Target neutral=new Target();Actor.add(neutral);Random.pushGenerator(43);try{bomb.apdExplodeAttributed(12,OffensiveDamageOrigin.PLAYER);assertEquals(1000-(raw-3),neutral.HP);assertEquals(next,Random.Long());}finally{Random.popGenerator();Actor.remove(neutral);}}
 @Test public void attributedSnapshotSurvivesRestoreAndLegacyStaysUnowned(){CaptureBomb b=new CaptureBomb();b.apdExplodeAttributed(12,OffensiveDamageOrigin.BOSS);assertEquals("BOSS",b.captured.getString("apd_bomb_origin"));b.captured.remove("fuse");Bomb restored=new Bomb();restored.restoreFromBundle(b.captured);Bundle saved=new Bundle();restored.storeInBundle(saved);assertEquals("BOSS",saved.getString("apd_bomb_origin"));saved.remove("apd_bomb_origin");saved.remove("fuse");restored.restoreFromBundle(saved);Bundle legacy=new Bundle();restored.storeInBundle(legacy);assertFalse(legacy.contains("apd_bomb_origin"));}
 @Test public void scopedExplosionRestoresOriginAfterFailureAndRejectsMalformedSaves(){CaptureBomb b=new CaptureBomb();b.fail=true;try{b.apdExplodeAttributed(12,OffensiveDamageOrigin.PLAYER);fail();}catch(IllegalStateException expected){}Bundle after=new Bundle();b.storeInBundle(after);assertFalse(after.contains("apd_bomb_origin"));Bundle invalid=new Bundle();invalid.put("apd_bomb_origin","FUTURE");try{b.restoreFromBundle(invalid);fail();}catch(IllegalArgumentException expected){}assertEquals("FUTURE",invalid.getString("apd_bomb_origin"));}
 @Test public void realBlastStoneUsesTheCapturedThrower(){
  rules(200,300,500);Target t=new Target();Actor.add(t);
  com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast stone=new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast(){@Override protected void onThrow(int cell){activate(cell);}};
  int raw;Random.pushGenerator(79);try{raw=Random.NormalIntRange(8,24);}finally{Random.popGenerator();}
  Random.pushGenerator(79);try{stone.apdThrowAttributed(12,OffensiveDamageOrigin.PLAYER);assertEquals(1000-(raw*2-3),t.HP);assertTrue(t.source instanceof Bomb.ConjuredBomb);}finally{Random.popGenerator();Actor.remove(t);}
 }
 @Test public void realCursedWandExplosionUsesBossRoleInsteadOfEnemyRole(){
  rules(200,300,500);Target t=new Target();Actor.add(t);
  int raw;Random.pushGenerator(89);try{raw=Random.NormalIntRange(8,24);}finally{Random.popGenerator();}
  com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica bolt=new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(6,12,com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE);
  Random.pushGenerator(89);try{assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand.Explosion().effect(new Bomb(),new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo(),bolt,false));assertEquals(1000-(raw*5-3),t.HP);assertTrue(t.source instanceof Bomb.ConjuredBomb);}finally{Random.popGenerator();Actor.remove(t);}
 }
 @Test public void actualLitThrowAndRestoredFuseKeepTheCreatorAndOriginalDelay()throws Exception{
  rules(200,300,500);Bundle catalog=new Bundle();com.shatteredpixel.shatteredpixeldungeon.journal.Catalog.store(catalog);
  java.lang.reflect.Field lighting=Bomb.class.getDeclaredField("lightingFuse");lighting.setAccessible(true);boolean before=lighting.getBoolean(null);
  DropLevel drop=new DropLevel();java.lang.reflect.Method size=Level.class.getDeclaredMethod("setSize",int.class,int.class);size.setAccessible(true);size.invoke(drop,5,5);java.util.Arrays.fill(drop.map,Terrain.EMPTY);drop.heaps=new SparseArray<>();Dungeon.level=drop;
  Bomb thrown=new Bomb(),restored=new Bomb();Target t=new Target();Actor.add(t);
  try{
   lighting.setBoolean(null,true);thrown.apdThrowAttributed(12,OffensiveDamageOrigin.BOSS);assertNotNull(thrown.fuse);assertEquals(2f,thrown.fuse.cooldown(),0);
   Bundle saved=new Bundle();thrown.storeInBundle(saved);assertEquals("BOSS",saved.getString("apd_bomb_origin"));thrown.fuse.snuff();restored.restoreFromBundle(saved);assertNotNull(restored.fuse);assertEquals(2f,restored.fuse.cooldown(),0);
   com.shatteredpixel.shatteredpixeldungeon.items.Heap heap=drop.heaps.get(12);heap.items.clear();heap.items.add(restored);
   int raw;Random.pushGenerator(19);try{raw=Random.NormalIntRange(8,24);}finally{Random.popGenerator();}
   java.lang.reflect.Method trigger=Bomb.Fuse.class.getDeclaredMethod("trigger",com.shatteredpixel.shatteredpixeldungeon.items.Heap.class);trigger.setAccessible(true);
   Random.pushGenerator(19);try{trigger.invoke(restored.fuse,heap);assertEquals(1000-(raw*5-3),t.HP);assertSame(restored,t.source);assertNull(restored.fuse);}finally{Random.popGenerator();}
  }finally{if(thrown.fuse!=null)thrown.fuse.snuff();if(restored.fuse!=null)restored.fuse.snuff();Actor.remove(t);lighting.setBoolean(null,before);com.shatteredpixel.shatteredpixeldungeon.journal.Catalog.restore(catalog);}
 }
}
