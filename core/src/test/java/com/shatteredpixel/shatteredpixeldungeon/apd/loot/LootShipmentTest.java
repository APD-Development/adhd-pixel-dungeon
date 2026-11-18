package com.shatteredpixel.shatteredpixeldungeon.apd.loot;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.watabou.utils.Bundle;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class LootShipmentTest {
 private LootShipment parcel()throws Exception {
  HeadlessTestSupport.start();Dagger weapon=new Dagger();weapon.upgrade(3);weapon.enchant(new Projecting());
  weapon.cursed=true;weapon.cursedKnown=false;weapon.levelKnown=true;weapon.apdUid="existing-weapon";
  Pasty food=new Pasty();food.quantity(7);food.apdUid="existing-stack";
  return new LootShipment("transfer-1",8,0,11,Arrays.asList(weapon,food));
 }
 @Test public void fullItemBundlesSurviveRepeatedShipmentRestoration()throws Exception {
  LootShipment p=parcel();
  for(int i=0;i<3;i++){
   Bundle b=new Bundle();p.storeInBundle(b);LootShipment restored=new LootShipment();restored.restoreFromBundle(b);p=restored;
   Dagger d=(Dagger)p.items().get(0);assertEquals(3,d.level());assertTrue(d.enchantment instanceof Projecting);
   assertTrue(d.cursed);assertFalse(d.cursedKnown);assertTrue(d.levelKnown);assertEquals("existing-weapon",d.apdUid);
   assertEquals(7,p.items().get(1).quantity());assertEquals("existing-stack",p.items().get(1).apdUid);
   assertEquals("transfer-1",p.id());assertEquals(8,p.sourceDepth());assertEquals(11,p.destinationDepth());
  }
 }
 @Test public void missingOrFuturePayloadFailsClosed()throws Exception {
  Bundle b=new Bundle();parcel().storeInBundle(b);b.remove("quantity");
  try{new LootShipment().restoreFromBundle(b);fail("Missing integrity field");}catch(IllegalArgumentException expected){}
  b=new Bundle();parcel().storeInBundle(b);b.put("version",2);
  try{new LootShipment().restoreFromBundle(b);fail("Future payload");}catch(IllegalArgumentException expected){}
 }
 @Test public void truncatedPayloadIsNotAcceptedAsASmallerDelivery()throws Exception {
  Bundle b=new Bundle();parcel().storeInBundle(b);b.put("items",Collections.singletonList(new Pasty()));
  try{new LootShipment().restoreFromBundle(b);fail("Lost items must not be silently dropped");}catch(IllegalArgumentException expected){}
 }
 @Test public void demonHallsAndTheShopFloorCanRouteToTheImp()throws Exception {
  HeadlessTestSupport.start();Item item=new Pasty();
  for(int depth=20;depth<=26;depth++){
   LootShipment p=new LootShipment("return-"+depth,depth,0,20,Collections.singletonList(item));
   Bundle b=new Bundle();p.storeInBundle(b);LootShipment restored=new LootShipment();restored.restoreFromBundle(b);
   assertEquals(depth,restored.sourceDepth());assertEquals(20,restored.destinationDepth());
  }
 }
 @Test public void aliasedStacksAndBackwardsDeliveryAreRejected()throws Exception {
  HeadlessTestSupport.start();Item item=new Pasty();
  try{new LootShipment("t",8,0,11,Arrays.asList(item,item));fail("Aliased stack");}catch(IllegalArgumentException expected){}
  try{new LootShipment("t",11,0,6,Collections.singletonList(item));fail("Wrong destination");}catch(IllegalArgumentException expected){}
 }
}
