package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.watabou.utils.*;
import org.junit.*;
import static org.junit.Assert.*;
public class ChainedBombDamageProvenanceTest {
 SpecialBombDamageProvenanceTest fixture=new SpecialBombDamageProvenanceTest();
 static class ProbeBomb extends Bomb {String captured;boolean fail;@Override public void explode(int cell){Bundle b=new Bundle();storeInBundle(b);captured=b.contains("apd_bomb_origin")?b.getString("apd_bomb_origin"):"UNATTRIBUTED";if(fail)throw new IllegalStateException("probe failure");}}
 @Before public void setup()throws Exception{fixture.setup();}
 @After public void cleanup()throws Exception{fixture.cleanup();}
 Heap heap(int cell,Bomb bomb){Heap h=new Heap();h.pos=cell;h.items.add(bomb);Dungeon.level.heaps.put(cell,h);return h;}
 void storedOrigin(Bomb bomb,OffensiveDamageOrigin role){Bundle b=new Bundle();bomb.storeInBundle(b);b.put("apd_bomb_origin",role.name());bomb.restoreFromBundle(b);}
 @Test public void unownedSecondaryBombsInheritActualExplosionOrBurnTrigger(){for(OffensiveDamageOrigin role:new OffensiveDamageOrigin[]{OffensiveDamageOrigin.PLAYER,OffensiveDamageOrigin.ENEMY,OffensiveDamageOrigin.BOSS})for(boolean burn:new boolean[]{false,true}){ProbeBomb b=new ProbeBomb();Heap h=heap(312,b);try(OffensiveEffectContext scope=OffensiveEffectContext.enter(role)){if(burn)h.burn();else h.explode();assertEquals(role.name(),b.captured);assertEquals(role,OffensiveEffectContext.current());}Bundle after=new Bundle();b.storeInBundle(after);assertFalse(after.contains("apd_bomb_origin"));assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,OffensiveEffectContext.current());}}
 @Test public void previouslySavedCreatorIsNotReplacedByAnotherTrigger(){ProbeBomb b=new ProbeBomb();storedOrigin(b,OffensiveDamageOrigin.BOSS);Heap h=heap(312,b);try(OffensiveEffectContext scope=OffensiveEffectContext.enter(OffensiveDamageOrigin.PLAYER)){h.explode();assertEquals("BOSS",b.captured);}Bundle after=new Bundle();b.storeInBundle(after);assertEquals("BOSS",after.getString("apd_bomb_origin"));}
 @Test public void actualParentAndSecondaryBlastsScaleEachHitOnce(){
  fixture.rules(100,100,100);Bomb first=new Bomb(),second=new Bomb();heap(312,second);SpecialBombDamageProvenanceTest.Target ref=new SpecialBombDamageProvenanceTest.Target(false);Actor.add(ref);long next;
  Random.pushGenerator(491);try{first.explode(312);next=Random.Long();}finally{Random.popGenerator();Actor.remove(ref);}assertEquals(2,ref.hits.size());
  fixture.rules(200,300,500);Bomb parent=new Bomb(),child=new Bomb();heap(312,child);SpecialBombDamageProvenanceTest.Target target=new SpecialBombDamageProvenanceTest.Target(false);Actor.add(target);
  Random.pushGenerator(491);try{parent.apdExplodeAttributed(312,OffensiveDamageOrigin.PLAYER);assertEquals(2,target.hits.size());for(int i=0;i<2;i++)assertEquals((ref.hits.get(i)+3)*2-3,(int)target.hits.get(i));assertSame(child,target.sources.get(0));assertSame(parent,target.sources.get(1));assertEquals(next,Random.Long());}finally{Random.popGenerator();Actor.remove(target);}
 }
 @Test public void environmentalParentOverridesUnrelatedOuterContext(){ProbeBomb child=new ProbeBomb();heap(312,child);try(OffensiveEffectContext scope=OffensiveEffectContext.enter(OffensiveDamageOrigin.PLAYER)){new Bomb().explode(312);assertEquals("UNATTRIBUTED",child.captured);assertEquals(OffensiveDamageOrigin.PLAYER,OffensiveEffectContext.current());}assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,OffensiveEffectContext.current());}
 @Test public void exceptionalSecondaryBlastRestoresBothCreatorAndContext(){ProbeBomb child=new ProbeBomb();child.fail=true;heap(312,child);Bomb parent=new Bomb();try(OffensiveEffectContext scope=OffensiveEffectContext.enter(OffensiveDamageOrigin.ENEMY)){try{parent.apdExplodeAttributed(312,OffensiveDamageOrigin.BOSS);fail();}catch(IllegalStateException expected){}assertEquals("BOSS",child.captured);assertEquals(OffensiveDamageOrigin.ENEMY,OffensiveEffectContext.current());}Bundle saved=new Bundle();parent.storeInBundle(saved);assertFalse(saved.contains("apd_bomb_origin"));child.storeInBundle(saved);assertFalse(saved.contains("apd_bomb_origin"));}
 @Test public void multipleSecondaryBombsRetainOneCausalOwnerAndNeutralRng(){
  fixture.rules(100,100,100);Bomb root=new Bomb(),middle=new Bomb(),last=new Bomb();heap(313,middle);heap(314,last);SpecialBombDamageProvenanceTest.Target ref=new SpecialBombDamageProvenanceTest.Target(false);ref.pos=313;Actor.add(ref);long next;
  Random.pushGenerator(571);try{root.explode(312);next=Random.Long();}finally{Random.popGenerator();Actor.remove(ref);}assertEquals(3,ref.hits.size());
  fixture.rules(200,300,500);Bomb parent=new Bomb(),child=new Bomb(),grandchild=new Bomb();heap(313,child);heap(314,grandchild);SpecialBombDamageProvenanceTest.Target t=new SpecialBombDamageProvenanceTest.Target(false);t.pos=313;Actor.add(t);
  Random.pushGenerator(571);try{parent.apdExplodeAttributed(312,OffensiveDamageOrigin.BOSS);assertEquals(3,t.hits.size());for(int i=0;i<3;i++)assertEquals((ref.hits.get(i)+3)*5-3,(int)t.hits.get(i));assertSame(grandchild,t.sources.get(0));assertSame(child,t.sources.get(1));assertSame(parent,t.sources.get(2));assertEquals(next,Random.Long());}finally{Random.popGenerator();Actor.remove(t);}
 }
}
