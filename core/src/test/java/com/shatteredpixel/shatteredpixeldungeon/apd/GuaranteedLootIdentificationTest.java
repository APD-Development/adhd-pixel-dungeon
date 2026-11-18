package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class GuaranteedLootIdentificationTest {
    @Before public void startUnknownPotionRun() throws Exception {
        // Mage does not start with Healing identified. Use real potion appearance initialization.
        LevelGenerationHarness.generate(73491, 0, HeroClass.MAGE, LevelGenerationHarness.Context.VETERAN, 1);
        assertFalse(new PotionOfHealing().isKnown());
    }

    @Test public void normalHealingDropIdentifiesEveryMatchingAppearanceImmediately() throws Exception {
        for (Mob source : new Mob[]{new Swarm(), new Bat(), new Necromancer()}) {
            startUnknownPotionRun();
            PotionOfHealing held = new PotionOfHealing();
            PotionOfHealing dropped = new PotionOfHealing();
            int quantity = dropped.quantity();
            GuaranteedLootIdentification.identifyNormalDrop(source, dropped);
            assertTrue(dropped.isIdentified());
            assertTrue(held.isKnown());
            assertEquals(quantity, dropped.quantity());
        }
        assertFalse("unrelated potion appearances stay unknown", new PotionOfExperience().isKnown());
    }

    @Test public void actualExoticReplacementUsesTheExistingSharedAppearanceMapping() throws Exception {
        for (Mob source : new Mob[]{new Swarm(), new Bat(), new Necromancer()}) {
            startUnknownPotionRun();
            PotionOfShielding dropped = new PotionOfShielding();
            PotionOfShielding heldShielding = new PotionOfShielding();
            PotionOfHealing heldHealing = new PotionOfHealing();
            GuaranteedLootIdentification.identifyNormalDrop(source, dropped);
            assertTrue(dropped.isIdentified());
            assertTrue(heldShielding.isKnown());
            assertTrue("vanilla exotic/regular appearance mapping is preserved", heldHealing.isKnown());
            assertFalse(new PotionOfExperience().isKnown());
        }
    }

    @Test public void ambiguousSourcesAndUnrelatedPotionsRemainUnidentified() {
        PotionOfHealing healing = new PotionOfHealing();
        GuaranteedLootIdentification.identifyNormalDrop(new Scorpio(), healing);
        GuaranteedLootIdentification.identifyNormalDrop(new Rat(), healing);
        assertFalse(healing.isKnown());
        PotionOfExperience experience = new PotionOfExperience();
        GuaranteedLootIdentification.identifyNormalDrop(new Swarm(), experience);
        assertFalse(experience.isKnown());
    }

    @Test public void actualNormalLootRollPlacesAnIdentifiedPotionBeforePickup() throws Exception {
        Camera previous = Camera.main;
        Game previousGame = Game.instance;
        try {
            new LootTestGame();
            Camera.main = new Camera(0, 0, 320, 240, 1);
            for (Mob source : new Mob[]{new Swarm(), new Bat(), new Necromancer()}) {
                startUnknownPotionRun();
                Dungeon.LimitedDrops counter = source instanceof Swarm ? Dungeon.LimitedDrops.SWARM_HP
                        : source instanceof Bat ? Dungeon.LimitedDrops.BAT_HP : Dungeon.LimitedDrops.NECRO_HP;
                int previousDropCount = counter.count;
                source.pos = Dungeon.level.entrance();
                Heap heap = new Heap();
                heap.pos = source.pos;
                heap.sprite = new ItemSprite(heap);
                heap.sprite.link(heap);
                Dungeon.level.heaps.put(heap.pos, heap);
                // Find a successful normal roll without replacing the real loot generator.
                long seed = 0;
                for (; seed < 10000; seed++) {
                    Random.pushGenerator(seed);
                    boolean succeeds;
                    try { succeeds = Random.Float() < source.lootChance(); }
                    finally { Random.popGenerator(); }
                    if (succeeds) break;
                }
                assertTrue("bounded successful fixture seed", seed < 10000);
                Random.pushGenerator(seed);
                try { source.rollToDropLoot(); }
                finally { Random.popGenerator(); }
                assertEquals("only the existing normal drop", 1, heap.size());
                assertEquals("normal drop limit increments exactly once", previousDropCount + 1, counter.count);
                Item drop = heap.peek();
                assertTrue(drop instanceof PotionOfHealing || drop instanceof PotionOfShielding);
                assertTrue("known on floor before pickup", drop.isIdentified());
                assertTrue(new PotionOfHealing().isKnown());
                assertFalse(new PotionOfExperience().isKnown());
            }
        } finally { Camera.main = previous; Game.instance = previousGame; }
    }

    private static class LootTestGame extends Game {
        LootTestGame() { super(sceneClass, platform); }
    }

    @Test public void actualLuckyBonusHealingDropFromSwarmRemainsUnknown() {
        Camera previous = Camera.main;
        Game previousGame = Game.instance;
        try {
            new LootTestGame();
            Camera.main = new Camera(0, 0, 320, 240, 1);
            Swarm source = new Swarm();
            source.pos = Dungeon.level.entrance();
            Heap heap = new Heap();
            heap.pos = source.pos;
            heap.sprite = new ItemSprite(heap);
            heap.sprite.link(heap);
            Dungeon.level.heaps.put(heap.pos, heap);
            Buff.affect(source, MasterThievesArmband.StolenTracker.class).setItemStolen(true);
            Buff.affect(source, Lucky.LuckProc.class);
            int previousDropCount = Dungeon.LimitedDrops.SWARM_HP.count;
            long seed = 0;
            for (; seed < 10000; seed++) {
                Random.pushGenerator(seed);
                Item candidate;
                try { candidate = Lucky.genLoot(); }
                finally { Random.popGenerator(); }
                if (candidate.getClass() == PotionOfHealing.class) break;
            }
            assertTrue("bounded bonus-healing fixture seed", seed < 10000);
            Random.pushGenerator(seed);
            try { source.rollToDropLoot(); }
            finally { Random.popGenerator(); }
            assertEquals(1, heap.size());
            assertEquals(PotionOfHealing.class, heap.peek().getClass());
            assertEquals("bonus loot never changes the normal Healing limit", previousDropCount,
                    Dungeon.LimitedDrops.SWARM_HP.count);
            assertFalse("random bonus source does not guarantee Healing", heap.peek().isIdentified());
            assertFalse(new PotionOfHealing().isKnown());
        } finally { Camera.main = previous; Game.instance = previousGame; }
    }

    @Test public void identificationConsumesNoRandomDrawsAndDoesNotChangeLootChanceOrLimitedDrops() {
        Mob source = new Swarm();
        PotionOfHealing drop = new PotionOfHealing();
        float chance = source.lootChance();
        int count = Dungeon.LimitedDrops.SWARM_HP.count;
        Random.pushGenerator(82716);
        float expected;
        try { expected = Random.Float(); } finally { Random.popGenerator(); }
        Random.pushGenerator(82716);
        try {
            GuaranteedLootIdentification.identifyNormalDrop(source, drop);
            assertEquals(expected, Random.Float(), 0f);
        } finally { Random.popGenerator(); }
        assertEquals(chance, source.lootChance(), 0f);
        assertEquals(count, Dungeon.LimitedDrops.SWARM_HP.count);
    }

    @Test public void dropHookPrecedesPlacementAndIsAbsentFromBonusLootBranch() throws Exception {
        String mob = Files.readString(Paths.get(System.getProperty("apd.game.src"), "actors/mobs/Mob.java"));
        int normal = mob.indexOf("Item loot = createLoot();", mob.indexOf("public void rollToDropLoot()"));
        int identify = mob.indexOf("GuaranteedLootIdentification.identifyNormalDrop(this, loot)", normal);
        int placement = mob.indexOf("Dungeon.level.drop(loot, pos)", normal);
        assertTrue("identify at creation, before pickup", normal >= 0 && identify > normal && placement > identify);
        assertEquals("never reused for wealth/lucky bonus loot", identify, mob.lastIndexOf("GuaranteedLootIdentification.identifyNormalDrop(this, loot)"));
    }
}
