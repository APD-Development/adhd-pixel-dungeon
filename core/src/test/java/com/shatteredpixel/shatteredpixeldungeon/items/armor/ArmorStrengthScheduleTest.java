package com.shatteredpixel.shatteredpixeldungeon.items.armor;

import static org.junit.Assert.*;

import org.junit.Test;

/** Armor strength requirement: 8 + 2 x tier, minus one at +1, +3, +6, +10; negative levels count as zero. */
public class ArmorStrengthScheduleTest {
	@Test public void baseRequirementPerTier(){
		assertEquals(10, Armor.STRReq(1, 0));
		assertEquals(18, Armor.STRReq(5, 0));
	}

	@Test public void dropsAtTriangularLevels(){
		int[] expectedDrop = {0, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4};
		for (int lvl = 0; lvl < expectedDrop.length; lvl++){
			assertEquals("level " + lvl, 18 - expectedDrop[lvl], Armor.STRReq(5, lvl));
		}
	}

	@Test public void negativeLevelsDoNotRaiseTheRequirement(){
		assertEquals(Armor.STRReq(3, 0), Armor.STRReq(3, -3));
	}

	@Test public void masteryPotionTakesTwoMore(){
		PlateArmor plate = new PlateArmor();
		int before = plate.STRReq(0);
		plate.masteryPotionBonus = true;
		assertEquals(before - 2, plate.STRReq(0));
	}
}
