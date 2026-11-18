package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import org.junit.Test;
import static org.junit.Assert.*;

public class UpgradeScrollPlanTest {
    @Test public void everyPermittedCountHasExactBalancedAllocation() {
        for (int count = 1; count <= 60; count++) {
            UpgradeScrollPlan plan = new UpgradeScrollPlan(count);
            int sum = 0, min = Integer.MAX_VALUE, max = 0;
            for (int region = 0; region < 5; region++) {
                int n = plan.regionCount(region);
                sum += n; min = Math.min(min, n); max = Math.max(max, n);
            }
            assertEquals(count, sum);
            assertTrue(max - min <= 1);
            assertEquals(count * 100.0 / 15, plan.percentage(), 0);
            assertEquals(count == 15, plan.usesVanillaGeneration());
            assertEquals((count + 1) / 2, plan.countAfterForbiddenRunes());
        }
    }
    @Test public void neutralRequiresOriginalGenerationAndThreePerRegion() {
        UpgradeScrollPlan plan = new UpgradeScrollPlan(15);
        assertEquals(100.0, plan.percentage(), 0);
        assertTrue(plan.usesVanillaGeneration());
        for (int r = 0; r < 5; r++) assertEquals(3, plan.regionCount(r));
    }
    @Test public void rejectsInvalidCountsAndRegions() {
        for (int count : new int[]{Integer.MIN_VALUE, 0, 61, Integer.MAX_VALUE}) {
            try { new UpgradeScrollPlan(count); fail("Invalid count accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        for (int region : new int[]{-1, 5}) {
            try { new UpgradeScrollPlan(1).regionCount(region); fail("Invalid region accepted"); }
            catch (IllegalArgumentException expected) { }
        }
    }
}
