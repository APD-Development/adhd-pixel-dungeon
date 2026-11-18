package com.shatteredpixel.shatteredpixeldungeon.actors.hero;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BattleAxe;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import org.junit.Test;
import static org.junit.Assert.*;
public class InventoryPickupMessageTest {
 private interface Check {void run(Hero hero);}
 private void withHero(Check check){HeadlessTestSupport.start();Hero previous=Dungeon.hero;try{Hero hero=new Hero();Talent.initClassTalents(hero);hero.HP=0;Dungeon.hero=hero;check.run(hero);}finally{Dungeon.hero=previous;}}
 @Test public void normalCapacityFailureKeepsOriginalMessage(){withHero(h->assertTrue(h.pickupFailureMessage(new Pasty()).startsWith("You can't carry:")));}
 @Test public void lostBackpackExplainsAllReportedPickupFailuresWithFreeSlots(){withHero(h->{
  LostInventory lost=new LostInventory();assertTrue(lost.attachTo(h));
  try{for(Item item:new Item[]{new BattleAxe(),new Pasty(),new Sungrass.Seed()}){
   assertFalse(item.collect(h.belongings.backpack));assertTrue(h.belongings.backpack.items.isEmpty());
   int quantity=item.quantity();float cooldown=h.cooldown();
   assertTrue(h.pickupFailureMessage(item).startsWith("Recover your lost backpack"));
   assertEquals(quantity,item.quantity());assertEquals(cooldown,h.cooldown(),0);
  }}finally{lost.detach();}
 });}
 @Test public void retainedItemsDoNotReceiveTheWrongRestrictionMessage(){withHero(h->{
  LostInventory lost=new LostInventory();assertTrue(lost.attachTo(h));
  try{Pasty item=new Pasty();item.keptThoughLostInvent=true;assertTrue(h.pickupFailureMessage(item).startsWith("You can't carry:"));}finally{lost.detach();}
 });}
 @Test public void recoveryRemovesLostBackpackExplanation(){withHero(h->{
  LostInventory lost=new LostInventory();assertTrue(lost.attachTo(h));lost.detach();assertTrue(h.pickupFailureMessage(new Pasty()).startsWith("You can't carry:"));
 });}
}
