package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.navigation.StairNavigation;

import org.junit.Test;

public class HeroActionMoveTelemetryTest {

	@Test
	public void movementModesAreStableAndRecordOnlyOnce(){
		HeroAction.Move step = new HeroAction.Move(1);
		HeroAction.Move travel = new HeroAction.Move(2, HeroAction.Move.Mode.TRAVEL);
		HeroAction.Move stairs = new StairNavigation.Walk(3, null);

		assertEquals("step", step.apdModeTag());
		assertEquals("travel", travel.apdModeTag());
		assertEquals("stairs", stairs.apdModeTag());
		assertTrue(step.apdStartRecording());
		assertFalse(step.apdStartRecording());
	}
}
