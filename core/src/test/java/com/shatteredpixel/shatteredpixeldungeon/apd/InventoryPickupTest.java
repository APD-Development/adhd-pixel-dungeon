package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BattleAxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vorpal;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventoryPickupTest {
    private interface Check { void run(Hero hero); }
    private void withHero(Check check) {
        HeadlessTestSupport.start();
        Hero previous = Dungeon.hero;
        try {
            Hero hero = new Hero(); com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.initClassTalents(hero); hero.HP = 0; Dungeon.hero = hero;
            check.run(hero);
        } finally { Dungeon.hero = previous; }
    }
    private BattleAxe axe() { BattleAxe axe = new BattleAxe(); axe.enchant(new Vorpal()); return axe; }
    private void fill(Hero hero, int slots) { for(int i=0;i<slots;i++) hero.belongings.backpack.items.add(new Sword()); }
    @Test public void vorpalAxeCollectsIntoLastAvailableBackpackSlot() { withHero(h -> {
        fill(h,19); BattleAxe axe=axe();
        assertEquals(20,h.belongings.backpack.capacity());
        assertTrue(axe.collect(h.belongings.backpack));
        assertSame(axe,h.belongings.backpack.items.get(h.belongings.backpack.items.indexOf(axe)));
        assertEquals(20,h.belongings.backpack.items.size());
    }); }
    @Test public void fullBackpackRejectsWithoutConsumingOrChangingAxe() { withHero(h -> {
        fill(h,20); BattleAxe axe=axe(); int quantity=axe.quantity();
        assertFalse(axe.collect(h.belongings.backpack));
        assertEquals(20,h.belongings.backpack.items.size());assertEquals(quantity,axe.quantity());
        assertTrue(axe.enchantment instanceof Vorpal);assertFalse(h.belongings.backpack.contains(axe));
    }); }
    @Test public void emptySpecializedContainersCannotAcceptMeleeWeaponOrExpandBackpack() { withHero(h -> {
        fill(h,20);
        for(Bag bag:new Bag[]{new VelvetPouch(),new ScrollHolder(),new PotionBandolier(),new MagicalHolster()}) {
            bag.owner=h;h.belongings.backpack.items.add(bag);
            assertFalse(bag.canHold(axe()));assertTrue(bag.items.isEmpty());
        }
        assertEquals(24,h.belongings.backpack.capacity());
        assertFalse(axe().collect(h.belongings.backpack));
        assertFalse(new Pasty().collect(h.belongings.backpack));
        h.belongings.backpack.items.remove(0);
        assertTrue(axe().collect(h.belongings.backpack));
    }); }
    @Test public void secondaryWeaponStillConsumesOneBackpackSlot() { withHero(h -> {
        h.belongings.secondWep=new Sword();fill(h,19);
        assertEquals(19,h.belongings.backpack.capacity());assertFalse(axe().collect(h.belongings.backpack));
        assertFalse(new Pasty().collect(h.belongings.backpack));
        h.belongings.backpack.items.remove(0);assertTrue(axe().collect(h.belongings.backpack));
    }); }
    @Test public void pastyCollectsIntoAvailableSlotAndStacksWhenFull() { withHero(h -> {
        fill(h,19); Pasty first=new Pasty();assertTrue(first.collect(h.belongings.backpack));
        int before=first.quantity();assertTrue(new Pasty().collect(h.belongings.backpack));
        assertEquals(before+1,first.quantity());assertEquals(20,h.belongings.backpack.items.size());
    }); }
    @Test public void lostInventoryBlocksNewAxeEvenWithEmptyBackpack() { withHero(h -> {
        LostInventory lost=new LostInventory();assertTrue(lost.attachTo(h));
        assertTrue(h.belongings.backpack.items.isEmpty());assertFalse(axe().collect(h.belongings.backpack));
        assertFalse(new Pasty().collect(h.belongings.backpack));
        lost.detach();assertTrue(axe().collect(h.belongings.backpack));
    }); }
    @Test public void lostInventoryIsPersistentNotATimedDebuffAndDetachRestoresExistingItems() { withHero(h -> {
        BattleAxe existing=axe(); h.belongings.backpack.items.add(existing);
        LostInventory lost=new LostInventory();assertTrue(lost.attachTo(h));
        assertTrue(h.belongings.lostInventory());assertSame(existing,h.belongings.backpack.items.get(0));
        assertTrue(lost.act());assertSame(lost,h.buff(LostInventory.class));
        assertTrue(h.belongings.lostInventory());
        lost.detach();assertNull(h.buff(LostInventory.class));assertFalse(h.belongings.lostInventory());
        assertSame(existing,h.belongings.backpack.items.get(0));assertTrue(new Pasty().collect(h.belongings.backpack));
    }); }
    @Test public void saveRoundtripKeepsLostInventoryAndItemsThenAllowsRecovery() { withHero(h -> {
        BattleAxe existing=axe();h.belongings.backpack.items.add(existing);
        LostInventory lost=new LostInventory();assertTrue(lost.attachTo(h));
        com.watabou.utils.Bundle bundle=new com.watabou.utils.Bundle();h.storeInBundle(bundle);
        Hero restored=new Hero();Dungeon.hero=restored;restored.restoreFromBundle(bundle);
        assertNotNull(restored.buff(LostInventory.class));assertTrue(restored.belongings.lostInventory());
        assertEquals(1,restored.belongings.backpack.items.size());
        assertFalse(new Pasty().collect(restored.belongings.backpack));
        restored.buff(LostInventory.class).detach();assertFalse(restored.belongings.lostInventory());
        assertTrue(new Pasty().collect(restored.belongings.backpack));lost.detach();
    }); }

    @Test public void emptyBackpackAndPouchRejectAllThreeUnderLostInventoryThenRecover() { withHero(h -> {
        VelvetPouch pouch=new VelvetPouch();pouch.owner=h;h.belongings.backpack.items.add(pouch);
        BattleAxe weapon=axe();Pasty food=new Pasty();
        com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed seed=new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed();
        LostInventory lost=new LostInventory();assertTrue(lost.attachTo(h));
        assertTrue(h.belongings.backpack.items.size()<h.belongings.backpack.capacity());
        assertTrue(pouch.items.size()<pouch.capacity());
        for(Item item:new Item[]{weapon,food,seed})assertFalse(item.collect(h.belongings.backpack));
        assertTrue(pouch.items.isEmpty());assertEquals(1,h.belongings.backpack.items.size());
        lost.detach();
        for(Item item:new Item[]{weapon,food,seed})assertTrue(item.collect(h.belongings.backpack));
        assertTrue(pouch.items.contains(seed));assertFalse(h.belongings.backpack.items.contains(seed));
        assertTrue(h.belongings.backpack.items.contains(weapon));assertTrue(h.belongings.backpack.items.contains(food));
    }); }
    @Test public void pouchAcceptsSungrassEvenWhenRootBackpackIsFull() { withHero(h -> {
        fill(h,20);VelvetPouch pouch=new VelvetPouch();pouch.owner=h;h.belongings.backpack.items.add(pouch);
        com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed seed=new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed();
        assertTrue(seed.collect(h.belongings.backpack));assertTrue(pouch.items.contains(seed));
        assertEquals(21,h.belongings.backpack.items.size());
    }); }
    @Test public void challengeFlagsDoNotRejectAvailableWeaponFoodOrSeedSlots() { withHero(h -> {
        int before=Dungeon.challenges;
        try {
            for(int flags:new int[]{0,com.shatteredpixel.shatteredpixeldungeon.Challenges.NO_FOOD,com.shatteredpixel.shatteredpixeldungeon.Challenges.NO_HERBALISM,511}){
                Dungeon.challenges=flags;h.belongings.backpack.items.clear();
                VelvetPouch pouch=new VelvetPouch();pouch.owner=h;h.belongings.backpack.items.add(pouch);
                assertTrue(axe().collect(h.belongings.backpack));assertTrue(new Pasty().collect(h.belongings.backpack));
                assertTrue(new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed().collect(h.belongings.backpack));
                assertEquals(1,pouch.items.size());
            }
        } finally {Dungeon.challenges=before;}
    }); }

    @Test public void serializedSaveKeepsPouchOwnershipAndClearedEffectDoesNotReturn() { withHero(h -> {
        VelvetPouch pouch=new VelvetPouch();pouch.owner=h;h.belongings.backpack.items.add(pouch);
        pouch.items.add(new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed());
        h.belongings.backpack.items.add(axe());
        LostInventory lost=new LostInventory();assertTrue(lost.attachTo(h));
        try {
            Hero restored=serializedHero(h);
            assertTrue(restored.belongings.lostInventory());assertNotNull(restored.buff(LostInventory.class));
            assertEquals(2,restored.belongings.backpack.items.size());
            VelvetPouch restoredPouch=restored.belongings.getBags().stream().filter(b->b instanceof VelvetPouch).map(b->(VelvetPouch)b).findFirst().get();
            assertSame(restored,restoredPouch.owner);assertEquals(1,restoredPouch.items.size());
            assertFalse(new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed().collect(restored.belongings.backpack));
            restored.buff(LostInventory.class).detach();
            Hero recovered=serializedHero(restored);
            assertNull(recovered.buff(LostInventory.class));assertFalse(recovered.belongings.lostInventory());
            assertTrue(axe().collect(recovered.belongings.backpack));assertTrue(new Pasty().collect(recovered.belongings.backpack));
            assertTrue(new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed().collect(recovered.belongings.backpack));
        } catch(java.io.IOException e){throw new AssertionError(e);} finally {lost.detach();}
    }); }
    private Hero serializedHero(Hero source) throws java.io.IOException {
        com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();source.storeInBundle(saved);
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        assertTrue(com.watabou.utils.Bundle.write(saved,bytes));
        com.watabou.utils.Bundle decoded=com.watabou.utils.Bundle.read(new java.io.ByteArrayInputStream(bytes.toByteArray()));
        Hero restored=new Hero();Dungeon.hero=restored;restored.restoreFromBundle(decoded);return restored;
    }

}
