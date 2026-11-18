/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import static org.junit.Assert.*;

import org.junit.Test;

public class ApdIntroTest {

	@Test
	public void aTrueFirstTimePlayerStillGetsTheTutorial(){
		assertFalse(ApdIntro.returningPlayer(false, 0, 0));
		//the first run's own save and its own log are not evidence of an earlier player
		assertFalse(ApdIntro.returningPlayer(false, 1, 1));
	}

	@Test
	public void aUsedInstallDoesNot(){
		assertTrue("a finished run", ApdIntro.returningPlayer(true, 0, 0));
		assertTrue("several saved games (the tester's case: many abandoned runs)", ApdIntro.returningPlayer(false, 2, 0));
		assertTrue("several recorded runs", ApdIntro.returningPlayer(false, 0, 2));
	}
	@Test
	public void optionalIntroductionCannotSuppressHungerAfterLeavingTheEntrance(){
		assertFalse(ApdIntro.shouldEndIntro(1, 0, true, false));
		assertTrue(ApdIntro.shouldEndIntro(1, 0, false, false));
		assertTrue(ApdIntro.shouldEndIntro(2, 0, true, false));
		assertTrue(ApdIntro.shouldEndIntro(1, 1, true, false));
		assertTrue(ApdIntro.shouldEndIntro(1, 0, true, true));
	}

}
