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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

// Hatred and Appreciation are separate pools with the same machinery; stories link lines across sessions.
public class PoolsAndStoriesTest {

	private static final long DAY = 24L * 3600 * 1000;
	private static final long T0 = 1_000_000_000_000L;

	private static List<Line> parse( String text ){
		return LinePack.parse( text, "t" );
	}

	private static void rejects( String row, String fragment ){
		try {
			LinePack.parse( row, "t" );
			fail( "should have been refused: " + row );
		} catch (LinePack.PackException e){
			assertTrue( e.getMessage(), e.getMessage().contains( fragment ) );
		}
	}

	@Test
	public void appreciationLinesAreAPoolOfTheirOwnWithTheirOwnIdsAndCounts(){
		rejects( "app.0001 | anytime | - | - | 5 | Cleric is great. |", "appreciation" );
		rejects( "any.0001 | anytime | appreciation | - | 5 | Cleric is great. |", "appreciation" );
		List<Line> lines = parse( "any.0001 | anytime | - | - | 5 | Cleric is terrible. |\n"
				+ "any.0002 | anytime | - | - | 5 | Cleric is awful. |\n"
				+ "app.0001 | anytime | appreciation | - | 5 | Cleric is wonderful. |\n" );
		ClericGrudge g = new ClericGrudge( lines, new Ledger( null ), new Random( 1 ), true, T0 );
		Counter c = g.counter();
		assertEquals( 2, c.total );
		assertEquals( 1, c.appreciationTotal );
		assertEquals( 0, c.heard );
		assertEquals( 0, c.appreciationHeard );
		assertEquals( "I'VE COMPLAINED ABOUT CLERIC 2 TIMES.", c.totalText() );
		assertEquals( "YOU'VE HEARD ME COMPLAIN 0 TIMES.", c.heardText() );
		assertEquals( "I'VE APPRECIATED CLERIC 1 TIMES.", c.appreciationTotalText() );
		assertEquals( "YOU'VE WITNESSED 0 QUESTIONABLE COMPLIMENTS.", c.appreciationHeardText() );
		//showing every line moves only the matching counter
		for (int i = 0; i < 3; i++) g.inlineAlways( Moment.MENU, Context.EMPTY, T0 + i );
		assertEquals( 2, g.counter().heard );
		assertEquals( 1, g.counter().appreciationHeard );
		assertEquals( 2, g.counter().total );
	}

	@Test
	public void linePacksCannotClaimBalanceOrPatchChanges(){
		for (String bad : new String[]{ "Cleric was nerfed.", "Cleric got buffed in the next update.", "I patched Cleric.", "A hotfix for Cleric.", "Cleric was rebalanced." }){
			rejects( "any.0001 | anytime | - | - | 5 | " + bad + " |", "balance or patch" );
		}
		//saying it is NOT a balance statement is fine
		parse( "any.0001 | anytime | - | - | 5 | That is not a balance statement about Cleric. |" );
	}

	@Test
	public void aFollowUpAppearsOnlyAfterItsFirstPartWasReallySeen(){
		List<Line> lines = parse( "app.0001 | anytime | appreciation,setup | - | 5 | Cleric is my favorite character. |\n"
				+ "app.0002 | anytime | appreciation,after=app.0001 | - | 5 | That statement was made during a lapse in judgment about Cleric. |\n" );
		Ledger led = new Ledger( null );
		//the follow-up is never offered while the first part is unseen
		for (int i = 0; i < 200; i++){
			Line l = Picker.pick( lines, Moment.MENU, Context.EMPTY, led, new Random( i * 31L + 1 ), null );
			assertEquals( "app.0001", l.id );
		}
		led.markShown( "app.0001", T0 );
		boolean follow = false;
		for (int i = 0; i < 50; i++){
			Line l = Picker.pick( lines, Moment.MENU, Context.EMPTY, led, new Random( i * 31L + 1 ), null );
			if (l.id.equals( "app.0002" )) follow = true;
		}
		assertTrue( "once the first part was seen, the payoff is possible", follow );
		//the first part is never repeated while an unseen payoff is waiting
		for (int i = 0; i < 200; i++) assertEquals( "app.0002", Picker.pick( lines, Moment.MENU, Context.EMPTY, led, new Random( i * 17L + 3 ), null ).id );
	}

	@Test
	public void aNextSessionFollowUpWaitsForALaterSession(){
		List<Line> lines = parse( "app.0001 | anytime | appreciation,setup | - | 5 | Cleric is my favorite character. |\n"
				+ "app.0002 | anytime | appreciation,after=app.0001,nextsession | - | 5 | Cleric is not my favorite character. |\n" );
		Ledger led = new Ledger( null );
		long sessionStart = T0;
		led.markShown( "app.0001", T0 + 1000 );
		//same session: shown after the session began, so the follow-up must wait
		for (int i = 0; i < 100; i++) assertNull( "not in the same session", onlyFollowUp( lines, led, sessionStart ) );
		//next session: the session starts after the first part was seen
		assertNotNull( onlyFollowUp( lines, led, T0 + DAY ) );
	}

	private static Line onlyFollowUp( List<Line> lines, Ledger led, long sessionStart ){
		Line l = Picker.pick( lines, Moment.MENU, Context.EMPTY, led, new Random( 5 ), null, sessionStart );
		return l != null && l.id.equals( "app.0002" ) ? l : null;
	}

	@Test
	public void storiesMustBeCompleteAndWellFormed(){
		//a prerequisite that does not exist
		List<String> problems = new ArrayList<>();
		Set<String> never = LinePack.unreachable( parse( "app.0002 | anytime | appreciation,after=app.0099 | - | 5 | Cleric. |" ), problems );
		assertTrue( never.contains( "app.0002" ) );
		assertFalse( problems.isEmpty() );
		//a story set up with no payoff
		problems.clear();
		LinePack.unreachable( parse( "app.0001 | anytime | appreciation,setup | - | 5 | Cleric is my favorite. |" ), problems );
		assertTrue( problems.get( 0 ), problems.get( 0 ).contains( "nothing follows" ) );
		//a line cannot follow itself, and loops never start
		rejects( "app.0001 | anytime | appreciation,after=app.0001 | - | 5 | Cleric. |", "follow itself" );
		problems.clear();
		never = LinePack.unreachable( parse( "app.0001 | anytime | appreciation,after=app.0002 | - | 5 | Cleric one. |\napp.0002 | anytime | appreciation,after=app.0001 | - | 5 | Cleric two. |" ), problems );
		assertEquals( 2, never.size() );
		//following a line of the other pool is refused
		problems.clear();
		LinePack.unreachable( parse( "any.0001 | anytime | - | - | 5 | Cleric one. |\napp.0001 | anytime | appreciation,after=any.0001 | - | 5 | Cleric two. |" ), problems );
		assertTrue( problems.get( 0 ), problems.get( 0 ).contains( "other pool" ) );
		//loading drops lines that could never be shown, so the counter never promises them
		ClericGrudge.PackSource src = new ClericGrudge.PackSource(){
			@Override public List<String> names(){ List<String> n = new ArrayList<>(); n.add( "p" ); return n; }
			@Override public String read( String name ){
				return "any.0001 | anytime | - | - | 5 | Cleric one. |\napp.0001 | anytime | appreciation,after=app.0050 | - | 5 | Cleric two. |";
			}
		};
		List<Line> loaded = ClericGrudge.loadPacks( src );
		assertEquals( 1, loaded.size() );
	}

	@Test
	public void theShippedStoriesAreAllComplete(){
		List<Line> lines = HumorTestSupport.shipped();
		List<String> problems = new ArrayList<>();
		assertTrue( "shipped lines with broken links: " + problems, LinePack.unreachable( lines, problems ).isEmpty() );
		assertTrue( problems.toString(), problems.isEmpty() );
		int setups = 0, followUps = 0;
		for (Line l : lines){
			if (l.setup) setups++;
			if (l.after != null) followUps++;
		}
		assertTrue( "the Appreciation pool has linked stories", setups >= 8 && followUps >= setups );
	}

	@Test
	public void theContentVersionThatIntroducedEachLineIsTracked(){
		List<Line> v1 = parse( "any.0001 | anytime | since=4.8 | - | 5 | Cleric one. |\napp.0001 | anytime | appreciation,since=4.8 | - | 5 | Cleric two. |" );
		List<Line> all = new ArrayList<>( v1 );
		all.addAll( parse( "any.0002 | anytime | since=4.9 | - | 5 | Cleric three. |\nany.0003 | anytime | since=4.9 | - | 5 | Cleric four. |\n"
				+ "app.0002 | anytime | appreciation,since=4.9 | - | 5 | Cleric five. |\nmeta.0001 | meta | meta,since=4.9 | - | 5 | A counter joke. |" ) );
		Map<Integer, int[]> m = new ClericGrudge( all, new Ledger( null ), new Random( 1 ), true, T0 ).addedByVersion();
		assertEquals( 2, m.size() );
		assertEquals( 1, m.get( 4008 )[0] );
		assertEquals( 1, m.get( 4008 )[1] );
		assertEquals( "two new complaints in 4.9", 2, m.get( 4009 )[0] );
		assertEquals( 1, m.get( 4009 )[1] );
	}

	@Test
	public void bothPoolsAreServedUnseenFirstAndTheCountersNeverDoubleCount(){
		List<Line> lines = new ArrayList<>( HumorTestSupport.make( "any", "anytime", 6, "-", "4.8" ) );
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= 4; i++) sb.append( String.format( "app.%04d | anytime | appreciation,since=4.8 | - | 10 | Cleric compliment %d. |\n", i, i ) );
		lines.addAll( parse( sb.toString() ) );
		Ledger led = new Ledger( null );
		ClericGrudge g = new ClericGrudge( lines, led, new Random( 7 ), true, T0 );
		Set<String> ids = new HashSet<>();
		for (int i = 0; i < 10; i++) ids.add( g.inlineAlways( Moment.MENU, Context.EMPTY, T0 + i ).text );
		assertEquals( "ten different lines before any repeat", 10, ids.size() );
		for (int i = 0; i < 40; i++) g.inlineAlways( Moment.MENU, Context.EMPTY, T0 + 100 + i );
		assertEquals( 6, g.counter().heard );
		assertEquals( 4, g.counter().appreciationHeard );
	}

	@Test
	public void theHatredAndAppreciationPoolsHaveTheirOwnShippedCounts(){
		ClericGrudge g = new ClericGrudge( HumorTestSupport.shipped(), new Ledger( null ), new Random( 1 ), true, T0 );
		Counter c = g.counter();
		assertTrue( "curated Hatred count, got " + c.total, c.total == 33 );
		assertTrue( "curated Appreciation count, got " + c.appreciationTotal, c.appreciationTotal == 16 );
		for (Line l : HumorTestSupport.shipped()){
			assertEquals( l.id, l.id.startsWith( "app." ), l.pool == Pool.APPRECIATION );
		}
	}
}
