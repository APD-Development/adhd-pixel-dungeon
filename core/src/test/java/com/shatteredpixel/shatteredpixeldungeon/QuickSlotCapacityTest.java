package com.shatteredpixel.shatteredpixeldungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.watabou.utils.Bundle;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class QuickSlotCapacityTest {
 @org.junit.BeforeClass public static void setup(){com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport.start();}
 @Test public void internalCapacityCanExpandIndependentlyOfTheSixSlotMobileInterface(){
  QuickSlot model=new QuickSlot();assertEquals(6,model.capacity());assertEquals(6,QuickSlot.MOBILE_VISIBLE_SLOTS);
  Item item=new Pasty();model.setSlot(2,item);model.ensureCapacity(12);assertEquals(12,model.capacity());assertSame(item,model.getItem(2));
  model.setSlot(11,item);assertNull(model.getItem(2));assertEquals(11,model.getSlot(item));assertTrue(model.contains(item));
  model.ensureCapacity(4);assertEquals(12,model.capacity());assertSame(item,model.getItem(11));assertEquals(6,QuickSlot.MOBILE_VISIBLE_SLOTS);
 }
 @Test public void invalidAssignmentRetainsTheExistingItemAndShortcut(){
  QuickSlot model=new QuickSlot();Item item=new Pasty();model.setSlot(0,item);
  for(int index:new int[]{-1,6,Integer.MAX_VALUE}){
   try{model.setSlot(index,item);fail("invalid slot accepted");}catch(IndexOutOfBoundsException expected){}
   assertSame(item,model.getItem(0));assertEquals(0,model.getSlot(item));
  }
 }
 @Test public void resettingClearsAssignmentsButRetainsConfiguredCapacity(){
  QuickSlot model=new QuickSlot(20);model.setSlot(19,new Pasty());model.reset();assertEquals(20,model.capacity());
  for(int i=0;i<20;i++)assertNull(model.getItem(i));
 }
 @Test public void existingPlacementFormatRestoresSixAndExtendedSparsePlaceholders(){
  QuickSlot old=Dungeon.quickslot;boolean restoring=Belongings.bundleRestoring;
  try {
   Belongings.bundleRestoring=false;
   for(int capacity:new int[]{6,12}){
    QuickSlot source=new QuickSlot(capacity);source.setSlot(1,new Pasty().virtual());source.setSlot(capacity-1,new PotionOfHealing().virtual());
    Dungeon.quickslot=source;Bundle bundle=new Bundle();source.storePlaceholders(bundle);
    QuickSlot target=new QuickSlot();Dungeon.quickslot=target;target.restorePlaceholders(bundle);
    assertEquals(capacity,target.capacity());assertTrue(target.getItem(1) instanceof Pasty);assertEquals(0,target.getItem(1).quantity());
    assertTrue(target.getItem(capacity-1) instanceof PotionOfHealing);assertEquals(0,target.getItem(capacity-1).quantity());
    Item replacement=new Pasty();target.replacePlaceholder(replacement);assertSame(replacement,target.getItem(1));
   }
  }finally{Dungeon.quickslot=old;Belongings.bundleRestoring=restoring;}
 }
 @Test public void malformedPlacementMappingCannotCrashOrRemoveExistingItems(){
  QuickSlot old=Dungeon.quickslot;boolean restoring=Belongings.bundleRestoring;
  try {
   Belongings.bundleRestoring=false;QuickSlot model=new QuickSlot();Dungeon.quickslot=model;Item actual=new Pasty();model.setSlot(0,actual);
   Bundle bundle=new Bundle();bundle.put("placements",new boolean[]{false});bundle.put("placeholders",Arrays.asList(new PotionOfHealing().virtual()));
   model.restorePlaceholders(bundle);assertSame(actual,model.getItem(0));assertEquals(1,actual.quantity());
  }finally{Dungeon.quickslot=old;Belongings.bundleRestoring=restoring;}
 }
 @Test public void invalidSavedShortcutDoesNotPreventPhysicalItemRestoration(){
  QuickSlot old=Dungeon.quickslot;boolean restoring=Belongings.bundleRestoring;
  try {
   Dungeon.quickslot=new QuickSlot();Belongings.bundleRestoring=true;
   for(int index:new int[]{-1,6,Integer.MAX_VALUE}){
    Item original=new Pasty().quantity(3);Bundle saved=new Bundle();original.storeInBundle(saved);saved.put("quickslotpos",index);saved.put("apd_uid","preserved-item");
    Item restored=new Pasty();restored.restoreFromBundle(saved);
    assertEquals(3,restored.quantity());assertEquals("preserved-item",restored.apdUid);assertEquals(-1,Dungeon.quickslot.getSlot(restored));
   }
  }finally{Dungeon.quickslot=old;Belongings.bundleRestoring=restoring;}
 }
}
