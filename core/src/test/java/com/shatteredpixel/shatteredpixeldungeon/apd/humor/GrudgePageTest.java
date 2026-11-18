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

package com.shatteredpixel.shatteredpixeldungeon.apd.humor;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

// The owner's phone-test findings about the Grudge page and popup rate.
public class GrudgePageTest {

	private static String page() throws Exception {
		return new String( Files.readAllBytes( new File( System.getProperty( "apd.game.src" ), "windows/WndClericGrudge.java" ).toPath() ), StandardCharsets.UTF_8 );
	}

	@Test
	public void theCounterPageShowsOnlyWhatReallyHappenedNotHowManyLinesExist() throws Exception {
		String src = page();
		assertTrue( src.contains( "c.heardText(), c.appreciationHeardText()" ) );
		assertFalse( "no authored-line totals presented as history", src.contains( "c.totalText()" ) || src.contains( "c.appreciationTotalText()" ) );
	}

	@Test
	public void thereIsExactlyOneAdultHumorControlAndItIsInApdSettings() throws Exception {
		assertFalse( "the Grudge page must not carry its own copy of the preference", page().contains( "apdAdultHumor( checked() )" ) );
		String pane = new String( Files.readAllBytes( new File( System.getProperty( "apd.game.src" ), "windows/APDSettingsPane.java" ).toPath() ), StandardCharsets.UTF_8 );
		assertTrue( pane.contains( "SPDSettings.apdAdultHumor( checked() )" ) );
	}

	@Test
	public void everyOnFrequencyKeepsAtLeastADayBetweenPopupsAcrossRestarts(){
		for (Frequency f : Frequency.values()){
			if (f == Frequency.OFF) continue;
			assertTrue( f + " gap", f.minGapMs >= 24L * 3600 * 1000 );
		}
		assertEquals( Frequency.RARER, Frequency.DEFAULT );
		assertTrue( Frequency.DEFAULT.minGapMs >= 24L * 3600 * 1000 );
	}
}
