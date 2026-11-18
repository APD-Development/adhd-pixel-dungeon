/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings.Backpack;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.*;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.*;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.*;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.*;
import com.shatteredpixel.shatteredpixeldungeon.plants.*;
import com.watabou.utils.Bundle;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;

public class AutomaticInventoryOrderTest {
    private Hero oldHero;
    private Object oldPotions,oldRings,oldScrolls;
    @Before public void setup(){
        HeadlessTestSupport.start();oldHero=Dungeon.hero;oldPotions=Potion.apdHandler();oldRings=Ring.apdHandler();oldScrolls=Scroll.apdHandler();
        com.watabou.utils.Random.pushGenerator(378L);Potion.initColors();Ring.initGems();Scroll.initLabels();
        Dungeon.hero=new Hero();Dungeon.hero.HP=0;
    }
    @After public void cleanup(){Dungeon.hero=oldHero;Potion.apdHandler(oldPotions);Ring.apdHandler(oldRings);Scroll.apdHandler(oldScrolls);com.watabou.utils.Random.popGenerator();}
    private <T extends Item> T known(T item,int level){item.level(level);item.identify();return item;}
    private void expect(Bag bag,Item... expected){assertEquals(Arrays.asList(expected),bag.itemsForDisplay());}
    private void populate(Bag bag,Item... items){bag.items.addAll(Arrays.asList(items));}
    @Test public void meleeTierComesBeforeUpgradeAndUnknownGearComesLast(){
        Backpack bag=new Backpack();Item low=known(new WornShortsword(),8),middle=known(new Shortsword(),0),middlePlus=known(new Spear(),3),high=known(new Sword(),0),unknown=new WornShortsword().level(-4);
        populate(bag,high,unknown,middlePlus,middle,low);expect(bag,low,middle,middlePlus,high,unknown);
    }
    @Test public void armorUsesTierThenUpgradeRatherThanStrengthRequirement(){
        Backpack bag=new Backpack();Item cloth=known(new ClothArmor(),9),leather=known(new LeatherArmor(),0),leatherPlus=known(new LeatherArmor(),3),mail=known(new MailArmor(),0),unknown=new ClothArmor().level(14);
        populate(bag,unknown,mail,leatherPlus,cloth,leather);expect(bag,cloth,leather,leatherPlus,mail,unknown);
    }
    @Test public void ringsAndOtherUpgradeableCategoriesUseVisibleLevels(){
        for(Item[] pair:new Item[][]{{known(new RingOfForce(),0),known(new RingOfMight(),4)},{known(new WandOfFrost(),0),known(new WandOfMagicMissile(),3)},{known(new CloakOfShadows(),1),known(new DriedRose(),7)},{known(new SaltCube(),0),known(new MossyClump(),3)}}){
            Backpack bag=new Backpack();populate(bag,pair[1],pair[0]);expect(bag,pair[0],pair[1]);
        }
    }
    @Test public void consumableBagsKeepItemFamiliesTogetherThenSortStacks(){
        Bag pouch=new VelvetPouch();Item seed=new Sungrass.Seed().quantity(5),stone=new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink().quantity(1);populate(pouch,stone,seed);expect(pouch,seed,stone);
        Bag scrolls=new ScrollHolder();Item identify=new ScrollOfIdentify().identify().quantity(7),upgrade=new ScrollOfUpgrade().identify().quantity(2);populate(scrolls,identify,upgrade);expect(scrolls,upgrade,identify);
        Bag potions=new PotionBandolier();Item healing=new PotionOfHealing().identify().quantity(8),shield=new PotionOfShielding().identify().quantity(1);populate(potions,healing,shield);expect(potions,shield,healing);
    }
    @Test public void pouchGroupsSeedsBeforeStonesEvenWhenStonesHaveSmallerStacks(){
        Bag pouch=new VelvetPouch();
        Item seedBig=new Sungrass.Seed().quantity(6);
        Item seedSmall=new Firebloom.Seed().quantity(1);
        Item stoneSmall=new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink().quantity(1);
        Item stoneBig=new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition().quantity(8);
        populate(pouch,stoneSmall,seedBig,stoneBig,seedSmall);
        expect(pouch,seedSmall,seedBig,stoneSmall,stoneBig);
    }
    @Test public void visibleUpgradeRemainsSortableBeforeFullCurseIdentification(){
        Bag bag=new Backpack();
        Item knownLow=new LeatherArmor().level(1);
        knownLow.levelKnown=true;
        knownLow.cursedKnown=false;
        Item knownHigh=known(new LeatherArmor(),3);
        Item unknown=new LeatherArmor().level(9);
        populate(bag,unknown,knownHigh,knownLow);
        expect(bag,knownLow,knownHigh,unknown);
        assertFalse(knownLow.isIdentified());
        assertEquals(1,knownLow.visiblyUpgraded());
        // Sorting is a presentation snapshot, not an identification action.
        assertFalse(knownLow.cursedKnown);
        assertFalse(unknown.levelKnown);
    }
    @Test public void backpackKeepsSeedsAndStonesGroupedRegardlessOfStackSizes(){
        Backpack bag=new Backpack();
        Item seed=new Sungrass.Seed().quantity(9);
        Item stone=new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink().quantity(1);
        populate(bag,stone,seed);
        expect(bag,seed,stone);
    }
    @Test public void holsterKeepsThrowablesTierAndUpgradeBeforeWandUpgrades(){
        Bag bag=new MagicalHolster();Item knife=known(new ThrowingKnife(),8),club=known(new ThrowingClub(),0),clubPlus=known(new ThrowingClub(),3),wand=known(new WandOfFrost(),-1),wandPlus=known(new WandOfMagicMissile(),4),unknown=new WandOfFrost().level(12);
        populate(bag,unknown,wandPlus,clubPlus,wand,club,knife);expect(bag,knife,club,clubPlus,wand,wandPlus,unknown);
    }
    @Test public void unidentifiedItemsNeverSortOnHiddenLevelsAndMoveAfterIdentification(){
        Bag bag=new Backpack();Item known=known(new RingOfMight(),8),first=new RingOfForce().level(-4),second=new RingOfForce().level(14);populate(bag,first,known,second);expect(bag,known,first,second);
        first.level(25);second.level(-9);expect(bag,known,first,second);
        second.identify();expect(bag,second,known,first);
        first.identify();expect(bag,second,known,first);
    }
    @Test public void unknownConsumablesRemainLastButUseVisibleStackCounts(){
        // Reset these two type flags explicitly so the test does not depend on earlier global IDs.
        com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.initColors();
        Bag bag=new PotionBandolier();Item known=new PotionOfHealing().identify().quantity(8),unknownSmall=new PotionOfFrost().quantity(1),unknownLarge=new PotionOfLiquidFlame().quantity(4);
        assertFalse(unknownSmall.isIdentified());populate(bag,unknownLarge,known,unknownSmall);expect(bag,known,unknownSmall,unknownLarge);
        unknownSmall.identify();expect(bag,unknownSmall,known,unknownLarge);
    }
    @Test public void snapshotRefreshesAfterUpgradeAndQuantityChangeWithoutMutatingStoredItems(){
        Bag bag=new Backpack();Item first=known(new Sword(),0),second=known(new Sword(),2);populate(bag,second,first);List<Item> original=new ArrayList<>(bag.items);
        expect(bag,first,second);first.upgrade(3);expect(bag,second,first);assertEquals(original,bag.items);
        Bag pouch=new VelvetPouch();Item a=new Sungrass.Seed().quantity(2),b=new Firebloom.Seed().quantity(5);populate(pouch,b,a);expect(pouch,a,b);a.quantity(7);expect(pouch,b,a);assertEquals(7,a.quantity());assertEquals(5,b.quantity());
    }
    @Test public void restoredOldInventoryUsesNewOrderAndKeepsItemState(){
        Bag bag=new Backpack();Item high=known(new ScaleArmor(),4),low=known(new LeatherArmor(),1);populate(bag,high,low);Bundle saved=new Bundle();saved.put("bag",bag);Bag restored=(Bag)saved.get("bag");
        List<Item> view=restored.itemsForDisplay();assertTrue(view.get(0) instanceof LeatherArmor);assertTrue(view.get(1) instanceof ScaleArmor);assertEquals(1,view.get(0).level());assertEquals(4,view.get(1).level());assertTrue(view.get(0).isIdentified());assertTrue(view.get(1).isIdentified());
    }
    @Test public void sortingKeepsEquippedGearQuickslotsAndStackIdentity(){
        com.shatteredpixel.shatteredpixeldungeon.QuickSlot previous=Dungeon.quickslot;
        try{
            Dungeon.quickslot=new com.shatteredpixel.shatteredpixeldungeon.QuickSlot();
            Item weapon=known(new Sword(),4),ring=known(new RingOfMight(),2);Dungeon.hero.belongings.weapon=(MeleeWeapon)weapon;Dungeon.hero.belongings.ring=(Ring)ring;
            Bag bag=Dungeon.hero.belongings.backpack;Item first=known(new LeatherArmor(),0),second=known(new ScaleArmor(),3);populate(bag,second,first);Dungeon.quickslot.setSlot(0,second);
            expect(bag,first,second);assertSame(second,Dungeon.quickslot.getItem(0));assertSame(weapon,Dungeon.hero.belongings.weapon);assertSame(ring,Dungeon.hero.belongings.ring);assertEquals(Arrays.asList(second,first),bag.items);
        }finally{Dungeon.quickslot=previous;}
    }
    @Test public void sortingDoesNotChangeKnowledgeSerializedItemStateOrRandomness(){
        Bag bag=new MagicalHolster();Item hidden=new WandOfFrost().level(12),known=known(new WandOfMagicMissile(),3);populate(bag,hidden,known);
        Bundle before=new Bundle();hidden.storeInBundle(before);
        com.watabou.utils.Random.pushGenerator(834);long expected=com.watabou.utils.Random.Long();com.watabou.utils.Random.popGenerator();
        com.watabou.utils.Random.pushGenerator(834);try{expect(bag,known,hidden);assertEquals(expected,com.watabou.utils.Random.Long());}finally{com.watabou.utils.Random.popGenerator();}
        Bundle after=new Bundle();hidden.storeInBundle(after);assertEquals(before.toString(),after.toString());assertFalse(hidden.isIdentified());assertEquals(12,hidden.level());
    }
    @Test public void everyPickupRoutesToItsBagAndRefreshesTheSortedViewImmediately(){
        com.shatteredpixel.shatteredpixeldungeon.QuickSlot previous=Dungeon.quickslot;
        try{
            Dungeon.quickslot=new com.shatteredpixel.shatteredpixeldungeon.QuickSlot();
            Bag pack=Dungeon.hero.belongings.backpack;pack.items.clear();
            Bag pouch=new VelvetPouch(),scrolls=new ScrollHolder(),potions=new PotionBandolier(),holster=new MagicalHolster();
            for(Bag b:new Bag[]{pouch,scrolls,potions,holster}){assertTrue(b.collect(pack));}
            Object[][] cases={
                {pack,new Item[]{known(new Sword(),0),known(new Shortsword(),2),known(new WornShortsword(),5)}},
                {pack,new Item[]{known(new ScaleArmor(),0),known(new LeatherArmor(),3),known(new ClothArmor(),1)}},
                {pack,new Item[]{known(new RingOfMight(),3),known(new RingOfForce(),0)}},
                {holster,new Item[]{known(new WandOfMagicMissile(),3),known(new ThrowingKnife(),0),known(new WandOfFrost(),0)}},
                {pouch,new Item[]{new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink(),new Sungrass.Seed()}},
                {scrolls,new Item[]{new ScrollOfIdentify().identify().quantity(3),new ScrollOfUpgrade().identify()}},
                {potions,new Item[]{new PotionOfHealing().identify().quantity(2),new PotionOfShielding().identify()}}};
            for(Object[] c:cases){
                Bag target=(Bag)c[0];
                for(Item item:(Item[])c[1]){
                    com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.updateItemDisplays=false;
                    assertTrue(item.getClass().getSimpleName(),item.collect(pack));
                    assertTrue("display refresh requested after picking up "+item.getClass().getSimpleName(),
                            com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.updateItemDisplays);
                    assertTrue(item.getClass().getSimpleName()+" in its bag",target.items.contains(item));
                    List<Item> view=target.itemsForDisplay();
                    List<Item> sorted=new ArrayList<>(view);
                    sorted.sort((a,b)->com.shatteredpixel.shatteredpixeldungeon.apd.qol.InventoryOrder.compareInBag(target,a,b));
                    assertEquals(sorted,view);
                }
            }
            //spot-check the visible order the player gets without re-sorting
            List<Item> holsterView=holster.itemsForDisplay();
            assertTrue(holsterView.get(0) instanceof ThrowingKnife);
            assertTrue(holsterView.get(1) instanceof WandOfFrost);
            assertTrue(holsterView.get(2) instanceof WandOfMagicMissile);
        }finally{Dungeon.quickslot=previous;}
    }
}
