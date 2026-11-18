package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.watabou.utils.Bundle;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class EnemyHealingPotionRegionsTest {
    @Before public void reset() { Statistics.reset(); }

    @Test public void exactFiveDungeonRegionsAndInvalidDepth() {
        for (int i = 0; i < 5; i++) {
            assertEquals(i, Statistics.enemyHealingRegionForDepth(1 + 5*i));
            assertEquals(i, Statistics.enemyHealingRegionForDepth(5 + 5*i));
        }
        assertEquals(-1, Statistics.enemyHealingRegionForDepth(0));
        assertEquals(-1, Statistics.enemyHealingRegionForDepth(26));
    }

    @Test public void basePotionConstructorCanResetBeforeSubclassFieldsInitialize() {
        PotionOfHealing potion = new PotionOfHealing();
        potion.apdMarkEnemyDrop(1);
        potion.reset();
        potion.apdClaimCollectedEnemyDrops();
        assertArrayEquals("reset clears pending drop origin", new int[5], Statistics.enemyHealingPotionsByRegion);
    }

    @Test public void enemyDropCountedOnlyAfterCollectionAndOnlyOnce() {
        PotionOfHealing enemy = new PotionOfHealing();
        enemy.apdMarkEnemyDrop(7);
        assertEquals(0, Statistics.enemyHealingPotionsByRegion[1]);
        enemy.apdClaimCollectedEnemyDrops();
        enemy.apdClaimCollectedEnemyDrops();
        assertArrayEquals(new int[]{0,1,0,0,0}, Statistics.enemyHealingPotionsByRegion);
    }

    @Test public void floorPileMergeAndSplitPreserveOriginsWithoutDoubleCounting() {
        PotionOfHealing sewers = new PotionOfHealing();
        PotionOfHealing caves = new PotionOfHealing();
        sewers.apdMarkEnemyDrop(2);
        caves.apdMarkEnemyDrop(14);
        sewers.merge(caves);
        assertEquals(2, sewers.quantity());
        assertEquals(0, caves.quantity());
        PotionOfHealing separated = (PotionOfHealing)sewers.split(1);
        separated.apdClaimCollectedEnemyDrops();
        sewers.apdClaimCollectedEnemyDrops();
        assertArrayEquals(new int[]{1,0,1,0,0}, Statistics.enemyHealingPotionsByRegion);
    }

    @Test public void saveRestoresUncollectedProvenanceAndCountsWithoutRepricingOldRuns() {
        PotionOfHealing original = new PotionOfHealing();
        original.apdMarkEnemyDrop(18);
        Bundle itemBundle = new Bundle();
        original.storeInBundle(itemBundle);
        PotionOfHealing restored = new PotionOfHealing();
        restored.restoreFromBundle(itemBundle);
        restored.apdClaimCollectedEnemyDrops();
        Bundle statistics = new Bundle();
        Statistics.storeInBundle(statistics);
        Statistics.reset();
        Statistics.restoreFromBundle(statistics);
        assertTrue(Statistics.enemyHealingPotionsRecorded);
        assertArrayEquals(new int[]{0,0,0,1,0}, Statistics.enemyHealingPotionsByRegion);

        Statistics.restoreFromBundle(new Bundle());
        assertFalse("legacy saves must not be assigned invented earlier drops", Statistics.enemyHealingPotionsRecorded);
        assertArrayEquals(new int[]{0,0,0,0,0}, Statistics.enemyHealingPotionsByRegion);
        Statistics.recordEnemyHealingPickup(new int[]{0,0,0,0,2});
        Bundle upgradedLegacy = new Bundle();
        Statistics.storeInBundle(upgradedLegacy);
        Statistics.reset();
        Statistics.restoreFromBundle(upgradedLegacy);
        assertFalse("partial history remains marked incomplete across upgraded saves", Statistics.enemyHealingPotionsRecorded);
        assertArrayEquals(new int[]{0,0,0,0,2}, Statistics.enemyHealingPotionsByRegion);
    }

    @Test public void noEnemyMarkForFloorShopsOrInvalidDepthAndRegionIsOriginal() {
        PotionOfHealing ordinary = new PotionOfHealing();
        ordinary.apdMarkEnemyDrop(26);
        ordinary.apdClaimCollectedEnemyDrops();
        assertArrayEquals(new int[5], Statistics.enemyHealingPotionsByRegion);
        PotionOfHealing enemy = new PotionOfHealing();
        enemy.apdMarkEnemyDrop(4);
        enemy.apdClaimCollectedEnemyDrops();
        assertArrayEquals(new int[]{1,0,0,0,0}, Statistics.enemyHealingPotionsByRegion);
    }
}
