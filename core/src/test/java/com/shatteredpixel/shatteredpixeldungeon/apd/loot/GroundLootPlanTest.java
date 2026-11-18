package com.shatteredpixel.shatteredpixeldungeon.apd.loot;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.watabou.utils.SparseArray;
import org.junit.Test;
import static org.junit.Assert.*;
public class GroundLootPlanTest {
 private SewerLevel level() throws Exception {
  HeadlessTestSupport.start();SewerLevel l=new SewerLevel();l.setSize(9,9);l.heaps=new SparseArray<>();return l;
 }
 private Heap heap(SewerLevel l,int cell,Heap.Type type,Item...items){
  Heap h=new Heap();h.pos=cell;h.type=type;
  java.util.Collections.addAll(h.items,items);l.heaps.put(cell,h);return h;
 }
 @Test public void looseLootIsCapturedWithoutOpeningContainersOrTakingStock() throws Exception {
  SewerLevel l=level();Pasty food=new Pasty();heap(l,10,Heap.Type.HEAP,food);
  for(Heap.Type type:Heap.Type.values())if(type!=Heap.Type.HEAP)heap(l,11+type.ordinal(),type,new Dagger());
  GroundLootPlan p=GroundLootPlan.capture(l,i->true);
  assertEquals(1,p.stacks());assertSame(food,p.sources.get(0).items.get(0));assertTrue(p.stillValid());
  assertEquals(1,l.heaps.get(10).items.size());
 }
 @Test public void noArbitraryStackLimitOrItemModification() throws Exception {
  SewerLevel l=level();Heap h=heap(l,10,Heap.Type.HEAP);
  for(int i=0;i<100;i++){Dagger d=new Dagger();d.upgrade(i%3);d.apdUid="loot-"+i;d.cursed=i%2==0;h.items.add(d);}
  GroundLootPlan p=GroundLootPlan.capture(l,i->true);assertEquals(100,p.stacks());
  for(int i=0;i<100;i++){assertSame(h.items.get(i),p.sources.get(0).items.get(i));assertEquals("loot-"+i,h.items.get(i).apdUid);assertEquals(i%2==0,h.items.get(i).cursed);}
 }
 @Test public void policyCanLeaveItemsBehindWithoutMutatingAnything() throws Exception {
  SewerLevel l=level();Dagger weapon=new Dagger();Pasty food=new Pasty();Heap h=heap(l,10,Heap.Type.HEAP,weapon,food);
  GroundLootPlan p=GroundLootPlan.capture(l,i->i==food);assertEquals(1,p.stacks());assertEquals(2,h.items.size());assertSame(food,p.sources.get(0).items.get(0));
 }
 @Test public void changedQuantityHeapOrContainerInvalidatesBeforeCommit() throws Exception {
  SewerLevel l=level();Pasty food=new Pasty();food.quantity(3);Heap h=heap(l,10,Heap.Type.HEAP,food);
  GroundLootPlan p=GroundLootPlan.capture(l,i->true);food.quantity(2);assertFalse(p.stillValid());
  food.quantity(3);assertTrue(p.stillValid());h.type=Heap.Type.CHEST;assertFalse(p.stillValid());
  h.type=Heap.Type.HEAP;l.heaps.remove(10);assertFalse(p.stillValid());
 }
 @Test public void aliasedItemsFailClosedWithoutRemovingLoot() throws Exception {
  SewerLevel l=level();Pasty food=new Pasty();Heap h=heap(l,10,Heap.Type.HEAP,food,food);
  try{GroundLootPlan.capture(l,i->true);fail("Aliased item must not be copied twice");}catch(IllegalStateException expected){}
  assertEquals(2,h.items.size());
 }
}
