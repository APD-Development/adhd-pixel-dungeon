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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class PickerTest {

	//java.util.Random seeded 0,1,2... gives nearly the same first number each time, so spread the seeds first
	static Random scrambled( long seed ){
		return new Random( new Random( seed ).nextLong() );
	}

	private static Line show( List<Line> pool, Moment m, Context c, Ledger led, Random rng, long t ){
		Line l = Picker.pick( pool, m, c, led, rng, null );
		if (l != null) led.markShown( l.id, t );
		return l;
	}

	@Test
	public void everyLineIsShownOnceBeforeAnyIsRepeated(){
		List<Line> pool = HumorTestSupport.make( "a", "anytime", 120, "-", "1.0" );
		Ledger led = new Ledger( null );
		Random rng = new Random( 1 );
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 120; i++){
			Line l = show( pool, Moment.MENU, Context.EMPTY, led, rng, i );
			assertTrue( "repeat before the pool was used up at step " + i, seen.add( l.id ) );
		}
		assertEquals( 120, seen.size() );
	}

	@Test
	public void afterExhaustionItNeverDriesUpAndAvoidsTheMostRecentLines(){
		List<Line> pool = HumorTestSupport.make( "a", "anytime", 60, "-", "1.0" );
		Ledger led = new Ledger( null );
		Random rng = new Random( 2 );
		for (int i = 0; i < 60; i++) show( pool, Moment.MENU, Context.EMPTY, led, rng, i );
		//keep going for a long time: never null, and a repeat is never one of the last window shown
		List<String> history = new ArrayList<>();
		for (int i = 0; i < 600; i++){
			Line l = show( pool, Moment.MENU, Context.EMPTY, led, rng, 100 + i );
			assertNotNull( l );
			int window = Math.min( Picker.RECENT_WINDOW, pool.size() / 2 );
			int from = Math.max( 0, history.size() - window + 1 );
			assertTrue( "repeated a very recent line: " + l.id, !history.subList( from, history.size() ).contains( l.id ) );
			history.add( l.id );
		}
		//and over time all of them keep coming back (no line is lost)
		assertEquals( 60, new HashSet<>( history ).size() );
	}

	@Test
	public void newLinesAreServedFirstToAPlayerWhoHasSeenTheOldOnes(){
		List<Line> old = HumorTestSupport.make( "old", "anytime", 40, "-", "1.0" );
		Ledger led = new Ledger( null );
		Random rng = new Random( 3 );
		for (int i = 0; i < 80; i++) show( old, Moment.MENU, Context.EMPTY, led, rng, i );
		//an update adds lines; the history is untouched
		List<Line> pool = new ArrayList<>( old );
		List<Line> added = HumorTestSupport.make( "new", "anytime", 5, "-", "1.1" );
		pool.addAll( added );
		Set<String> addedIds = new HashSet<>();
		for (Line l : added) addedIds.add( l.id );
		for (int i = 0; i < 5; i++){
			Line l = show( pool, Moment.MENU, Context.EMPTY, led, rng, 1000 + i );
			assertTrue( "the next five lines are the new ones: " + l.id, addedIds.contains( l.id ) );
		}
	}

	@Test
	public void theNewestUnseenLinesAreFavoured(){
		List<Line> pool = new ArrayList<>( HumorTestSupport.make( "old", "anytime", 20, "-", "1.0" ) );
		pool.addAll( HumorTestSupport.make( "new", "anytime", 20, "-", "1.1" ) );
		int fromNew = 0;
		for (int seed = 0; seed < 2000; seed++){
			Line l = Picker.pick( pool, Moment.MENU, Context.EMPTY, new Ledger( null ), scrambled( seed ), null );
			if (l.id.startsWith( "new" )) fromNew++;
		}
		assertTrue( "newer lines roughly twice as likely, got " + fromNew + "/2000", fromNew > 1200 && fromNew < 1500 );
	}

	@Test
	public void aLineIsOnlyEligibleWhereItsCategoryAndConditionsAllow(){
		List<Line> pool = new ArrayList<>();
		pool.addAll( LinePack.parse(
				"d.0001 | death | - | lastHero=CLERIC & lastOutcome=DIED & runVerified=1 | 5 | Cleric died on depth {depth}. |\n"
				+ "m.0001 | menu | - | - | 5 | Welcome. Cleric exists. |\n"
				+ "h.0001 | history | - | clericRuns>=100 | 5 | {clericRuns} Cleric runs. |\n", "t" ) );
		Random rng = new Random( 4 );
		Ledger led = new Ledger( null );
		Context died = Context.EMPTY.with( "runVerified", 1 ).with( "lastHero", "CLERIC" ).with( "lastOutcome", "DIED" ).with( "depth", 7 );
		//the death line only at a death, never at the menu
		assertEquals( "d.0001", Picker.pick( pool, Moment.RUN_END_DEATH, died, led, rng, null ).id );
		assertEquals( "m.0001", Picker.pick( pool, Moment.MENU, died, led, rng, null ).id );
		//wrong hero, wrong outcome, missing fact: not eligible
		assertNull( Picker.pick( pool, Moment.RUN_END_DEATH, died.with( "lastHero", "WARRIOR" ), led, rng, null ) );
		assertNull( Picker.pick( pool, Moment.RUN_END_DEATH, died.with( "lastOutcome", "WON" ), led, rng, null ) );
		assertNull( Picker.pick( pool, Moment.RUN_END_DEATH, Context.EMPTY.with( "lastHero", "CLERIC" ).with( "lastOutcome", "DIED" ), led, rng, null ) );
		//a history line needs the recorded count to reach its threshold
		assertNull( Picker.pick( pool, Moment.RUN_END_VICTORY, Context.EMPTY.with( "clericRuns", 99 ), led, rng, null ) );
		assertEquals( "h.0001", Picker.pick( pool, Moment.RUN_END_VICTORY, Context.EMPTY.with( "clericRuns", 100 ), led, rng, null ).id );
		//placeholders come from the recorded value
		assertEquals( "Cleric died on depth 7.", pool.get( 0 ).render( died, false ) );
	}

	@Test
	public void theSameKindOfJokeIsNotRepeatedWhenThereIsAChoice(){
		List<Line> pool = new ArrayList<>( HumorTestSupport.make( "a", "anytime", 30, "-", "1.0" ) );
		pool.addAll( HumorTestSupport.make( "f", "fourth_wall", 30, "-", "1.0" ) );
		for (int seed = 0; seed < 300; seed++){
			Line l = Picker.pick( pool, Moment.MENU, Context.EMPTY, new Ledger( null ), scrambled( seed ), Category.ANYTIME );
			assertEquals( Category.FOURTH_WALL, l.category );
		}
		//but if that is all there is, it is still used
		assertNotNull( Picker.pick( HumorTestSupport.make( "a", "anytime", 3, "-", "1.0" ), Moment.MENU, Context.EMPTY, new Ledger( null ), new Random( 1 ), Category.ANYTIME ) );
	}

	@Test
	public void selectionIsWeightedAndNothingIsLeftOutOfTheDraw(){
		List<Line> pool = LinePack.parse(
				"a.0001 | anytime | - | - | 90 | Cleric one. |\n"
				+ "a.0002 | anytime | - | - | 10 | Cleric two. |\n", "t" );
		Map<String, Integer> n = new HashMap<>();
		for (int seed = 0; seed < 4000; seed++){
			Line l = Picker.pick( pool, Moment.MENU, Context.EMPTY, new Ledger( null ), scrambled( seed ), null );
			Integer c = n.get( l.id );
			n.put( l.id, c == null ? 1 : c + 1 );
		}
		assertTrue( n.get( "a.0001" ) > 3300 && n.get( "a.0002" ) > 200 );
	}
}
