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

package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ClericHistoryTest {

	private final DatasetDef def = Datasets.runs();
	private final List<Row> rows = new ArrayList<>();

	private Row run( String cls, int outcome, boolean finished ){
		Row r = new Row( "r" + rows.size(), null, "r" + rows.size(), def.columns().size() );
		r.finished = finished;
		r.setText( def.indexOf( FactKeys.HERO_CLASS ), cls, Fact.PROV_RECORDED );
		r.setNum( def.indexOf( FactKeys.OUTCOME ), outcome, Fact.PROV_RECORDED );
		rows.add( r );
		return r;
	}

	@Test
	public void onlyFinishedNonDuplicateClericRunsAreCounted(){
		run( "CLERIC", LifecycleEvents.OUTCOME_DIED, true );
		run( "CLERIC", LifecycleEvents.OUTCOME_DIED, true );
		run( "CLERIC", LifecycleEvents.OUTCOME_WON, true );
		run( "CLERIC", LifecycleEvents.OUTCOME_WON, false );       //still going
		run( "MAGE", LifecycleEvents.OUTCOME_WON, true );          //another class
		run( "CLERIC", LifecycleEvents.OUTCOME_DIED, true ).duplicate = true;
		ClericHistory h = ClericHistory.compute( new Dataset( def, rows, new ArrayList<Dataset.RunInfo>() ) );
		assertEquals( 3, h.runs );
		assertEquals( 1, h.wins );
		assertEquals( 2, h.deaths );
	}

	@Test
	public void finishedRowsWithoutARecordedOutcomeAreNotInventedAsRuns(){
		Row incomplete = new Row( "incomplete", null, "incomplete", def.columns().size() );
		incomplete.finished = true;
		incomplete.setText( def.indexOf( FactKeys.HERO_CLASS ), "CLERIC", Fact.PROV_RECORDED );
		rows.add( incomplete );
		ClericHistory h = ClericHistory.compute( new Dataset( def, rows, new ArrayList<Dataset.RunInfo>() ) );
		assertEquals( 0, h.runs );
		assertEquals( 0, h.wins );
		assertEquals( 0, h.deaths );
	}

	@Test
	public void noHistoryMeansZeroAndNothingIsInvented(){
		ClericHistory h = ClericHistory.compute( null );
		assertEquals( 0, h.runs );
		assertEquals( 0, h.wins );
		assertEquals( 0, h.deaths );
		assertEquals( 0, ClericHistory.compute( new Dataset( def, new ArrayList<Row>(), new ArrayList<Dataset.RunInfo>() ) ).runs );
	}

	@Test public void runFactsCountOnlyFinishedClericRunsWithoutBuildingTheCatalog(){
		java.util.List<com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts> rows = new java.util.ArrayList<>();
		long[][] specs = {{1, LifecycleEvents.OUTCOME_WON},{1, LifecycleEvents.OUTCOME_DIED},{1, LifecycleEvents.OUTCOME_DIED},{0, LifecycleEvents.OUTCOME_WON},{1,-99}};
		int i = 0;
		for (long[] spec : specs){
			com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts r = new com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts(
					com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts.SOURCE_APD, "run" + (i++));
			r.text( FactKeys.HERO_CLASS, spec[0] == 1 ? "CLERIC" : "WARRIOR", Fact.PROV_RECORDED );
			if (spec[1] != -99) r.num( FactKeys.OUTCOME, spec[1], Fact.PROV_RECORDED );
			rows.add( r );
		}
		rows.add( null );
		ClericHistory h = ClericHistory.fromRunFacts( rows );
		assertEquals( 3, h.runs );
		assertEquals( 1, h.wins );
		assertEquals( 2, h.deaths );
		assertEquals( 0, ClericHistory.fromRunFacts( null ).runs );
	}
}
