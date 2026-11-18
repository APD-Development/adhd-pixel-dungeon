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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ClericGrudgeTest {

	//java.util.Random seeded 0,1,2... gives nearly the same first number each time, so spread the seeds first
	static Random scrambled( long seed ){
		return new Random( new Random( seed ).nextLong() );
	}

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private static final long DAY = 24L * 3600 * 1000;
	private static final long T0 = 1_000_000_000_000L;

	private ClericGrudge grudge( List<Line> lines, Ledger led, long seed, boolean enabled ){
		return new ClericGrudge( lines, led, scrambled( seed ), enabled );
	}

	// a freshly installed feature: the first call only starts the grace period
	private ClericGrudge started( List<Line> lines, Ledger led, GrudgePrefs prefs ){
		ClericGrudge g = grudge( lines, led, 1, true );
		assertNull( g.popup( Moment.MENU, Context.EMPTY, Safety.CALM, prefs, false, T0 ) );
		return g;
	}

	@Test
	public void disabledByDefaultAndWhenOffNothingEverAppears(){
		assertFalse( "the shipped switch is off until the owner has reviewed the content", ClericGrudge.enabled() );
		List<Line> lines = HumorTestSupport.shipped();
		GrudgePrefs prefs = HumorTestSupport.prefs( Frequency.RARE );
		ClericGrudge off = grudge( lines, new Ledger( null ), 1, false );
		for (int i = 0; i < 500; i++) assertNull( off.popup( Moment.MENU, Context.EMPTY, Safety.CALM, prefs, false, T0 + i * DAY ) );
		assertNull( off.firstClericRun( HumorTestSupport.prefs( Frequency.RARER ), false, T0 ) );
		GrudgePrefs none = HumorTestSupport.prefs( Frequency.OFF );
		ClericGrudge on = started( lines, new Ledger( null ), none );
		for (int i = 0; i < 500; i++) assertNull( "Off means no surprise popups", on.popup( Moment.MENU, Context.EMPTY, Safety.CALM, none, false, T0 + i * DAY ) );
	}

	@Test
	public void noPopupInTheFirstTwentyMinutesAndAtMostOnePerSession(){
		List<Line> lines = HumorTestSupport.shipped();
		GrudgePrefs prefs = HumorTestSupport.prefs( Frequency.RARE );
		//a session that is going to get a popup: find a seed whose one roll succeeds
		for (int seed = 0; seed < 100; seed++){
			Ledger led = new Ledger( null );
			ClericGrudge g = new ClericGrudge( lines, led, scrambled( seed ), true, T0 );
			assertNull( g.popup( Moment.MENU, Context.EMPTY, Safety.CALM, prefs, false, T0 ) );
			for (int i = 0; i < 200; i++){
				assertNull( "grace period", g.popup( Moment.MENU, Context.EMPTY, Safety.CALM, prefs, false, T0 + 60_000 + i * 1000L ) );
			}
			long later = T0 + 30 * DAY;
			Shown first = HumorTestSupport.popupAndShow( g, Moment.MENU, Context.EMPTY, Safety.CALM, prefs, false, later );
			if (first == null) continue;
			//a long time later, still the same session: never a second one
			for (int i = 0; i < 1000; i++) assertNull( g.popup( Moment.RUN_END_DEATH, Context.EMPTY, Safety.CALM, prefs, false, later + (100 + i) * DAY ) );
			return;
		}
		fail( "no session ever got a popup" );
	}

	@Test
	public void theGapBetweenPopupsFollowsTheChosenFrequency(){
		List<Line> lines = HumorTestSupport.shipped();
		GrudgePrefs prefs = HumorTestSupport.prefs( Frequency.RARER );
		Ledger led = new Ledger( null );
		started( lines, led, prefs );
		long shown = -1;
		for (int seed = 0; seed < 400 && shown < 0; seed++){
			ClericGrudge g = grudge( lines, led, seed, true );
			Shown s = HumorTestSupport.popupAndShow( g, Moment.MENU, Context.EMPTY, Safety.CALM, prefs, false, T0 + 40 * DAY );
			if (s != null) shown = led.stateLong( "lastPopupAt", -1 );
		}
		assertTrue( shown > 0 );
		//a new session (a new object, same ledger): too soon after, still nothing
		for (int i = 0; i < 1000; i++){
			assertNull( grudge( lines, led, i, true ).popup( Moment.MENU, Context.EMPTY, Safety.CALM, prefs, false, shown + 9 * DAY ) );
		}
		//and after the gap it can happen again
		boolean again = false;
		for (int i = 0; i < 1000 && !again; i++) again = grudge( lines, led, i, true ).popup( Moment.MENU, Context.EMPTY, Safety.CALM, prefs, false, shown + 11 * DAY ) != null;
		assertTrue( again );
	}

	@Test
	public void rarerReallyIsRarerThanRare(){
		List<Line> lines = HumorTestSupport.shipped();
		int rare = 0, rarer = 0;
		GrudgePrefs pa = HumorTestSupport.prefs( Frequency.RARE ), pb = HumorTestSupport.prefs( Frequency.RARER );
		for (int s = 0; s < 400; s++){
			Ledger a = new Ledger( null ), b = new Ledger( null );
			ClericGrudge ga = new ClericGrudge( lines, a, scrambled( s ), true, T0 ), gb = new ClericGrudge( lines, b, scrambled( s ), true, T0 );
			ga.popup( Moment.MENU, Context.EMPTY, Safety.CALM, pa, false, T0 );
			gb.popup( Moment.MENU, Context.EMPTY, Safety.CALM, pb, false, T0 );
			if (ga.popup( Moment.MENU, Context.EMPTY, Safety.CALM, pa, false, T0 + 5 * DAY ) != null) rare++;
			if (gb.popup( Moment.MENU, Context.EMPTY, Safety.CALM, pb, false, T0 + 5 * DAY ) != null) rarer++;
		}
		assertTrue( "rare " + rare + " rarer " + rarer, rare > rarer );
		assertTrue( "the default is the most conservative setting that is on", Frequency.DEFAULT == Frequency.RARER );
	}

	@Test
	public void anyBusyStateBlocksAPopup(){
		List<Line> lines = HumorTestSupport.shipped();
		GrudgePrefs prefs = HumorTestSupport.prefs( Frequency.RARE );
		Safety[] busy = {
				new Safety( true, false, false, false, false, false, false ),
				new Safety( false, true, false, false, false, false, false ),
				new Safety( false, false, true, false, false, false, false ),
				new Safety( false, false, false, true, false, false, false ),
				new Safety( false, false, false, false, true, false, false ),
				new Safety( false, false, false, false, false, true, false ),
				new Safety( false, false, false, false, false, false, true ) };
		for (Safety s : busy){
			Ledger led = new Ledger( null );
			ClericGrudge g = started( lines, led, prefs );
			for (int i = 0; i < 2000; i++) assertNull( g.popup( Moment.MENU, Context.EMPTY, s, prefs, false, T0 + 30 * DAY + i * 1000L ) );
			assertEquals( "nothing was recorded as shown", 0, led.distinctSeenAmong( idsOf( lines ) ) );
		}
		//the one-off moments are not surprises
		ClericGrudge g = started( lines, new Ledger( null ), prefs );
		for (int i = 0; i < 500; i++) assertNull( g.popup( Moment.FIRST_CLERIC_RUN, Context.EMPTY, Safety.CALM, prefs, false, T0 + 30 * DAY + i ) );
	}

	@Test
	public void adultWordingNeverReachesAPlayerWhoHasNotChosenIt(){
		List<Line> lines = HumorTestSupport.shipped();
		int withAdult = 0;
		for (Line l : lines) if (l.adult != null) withAdult++;
		assertTrue( withAdult > 20 );
		Set<String> seenAdultForms = new HashSet<>();
		for (Line l : lines){
			if (l.adult == null || l.category == Category.META) continue;
			Context ctx = fullContext();
			String clean = l.render( ctx, false );
			String adult = l.render( ctx, true );
			if (clean == null) continue;
			assertFalse( l.id + " clean render leaked profanity", LinePack.PROFANITY.matcher( clean ).find() );
			if (!clean.equals( adult )) seenAdultForms.add( l.id );
		}
		assertTrue( seenAdultForms.size() > 20 );
	}

	@Test
	public void theFirstClericRunWarningAppearsExactlyOnce() throws Exception {
		File f = new File( tmp.getRoot(), "l.txt" );
		List<Line> lines = HumorTestSupport.shipped();
		ClericGrudge g = grudge( lines, new Ledger( f ), 1, true );
		Shown s = g.firstClericRun( HumorTestSupport.prefs( Frequency.RARER ), false, T0 );
		assertNotNull( s );
		assertTrue( s.text.contains( "225" ) );
		assertFalse( "clean wording by default", s.text.contains( "fucking" ) );
		g.confirm( s, T0 );
		assertNull( g.firstClericRun( HumorTestSupport.prefs( Frequency.RARER ), false, T0 + 1 ) );
		//also after a restart
		assertNull( grudge( lines, new Ledger( f ), 9, true ).firstClericRun( HumorTestSupport.prefs( Frequency.RARER ), true, T0 + 2 ) );
		//an Adult player on a fresh install gets the Adult wording
		Shown adult = grudge( lines, new Ledger( null ), 1, true ).firstClericRun( HumorTestSupport.prefs( Frequency.RARER ), true, T0 );
		assertTrue( adult.text.contains( "fucking" ) );
	}

	@Test
	public void theCounterCountsDistinctComplaintsAndSurvivesUpdates() throws Exception {
		File f = new File( tmp.getRoot(), "l.txt" );
		List<Line> v1 = HumorTestSupport.make( "a", "anytime", 10, "-", "1.0" );
		Ledger led = new Ledger( f );
		ClericGrudge g = grudge( v1, led, 1, true );
		assertEquals( 10, g.counter().total );
		assertEquals( 0, g.counter().heard );
		assertEquals( "I'VE COMPLAINED ABOUT CLERIC 10 TIMES.", g.counter().totalText() );
		assertEquals( "YOU'VE HEARD ME COMPLAIN 0 TIMES.", g.counter().heardText() );
		for (int i = 0; i < 4; i++) assertNotNull( g.inlineAlways( Moment.MENU, Context.EMPTY, T0 + i * DAY ) );
		assertEquals( 4, g.counter().heard );
		//showing the same lines again never raises the player's count
		for (int i = 0; i < 6; i++) g.inlineAlways( Moment.MENU, Context.EMPTY, T0 + (10 + i) * DAY );
		int heardAfterAll = g.counter().heard;
		assertEquals( 10, heardAfterAll );
		for (int i = 0; i < 30; i++) g.inlineAlways( Moment.MENU, Context.EMPTY, T0 + (20 + i) * DAY );
		assertEquals( "repeats do not count twice", 10, g.counter().heard );

		//an update adds 7 complaints: the total grows, the history stays
		List<Line> v2 = new ArrayList<>( v1 );
		v2.addAll( HumorTestSupport.make( "b", "anytime", 7, "-", "1.1" ) );
		ClericGrudge updated = grudge( v2, new Ledger( f ), 2, true );
		assertEquals( 17, updated.counter().total );
		assertEquals( 10, updated.counter().heard );
		//a line that was later removed from the packs no longer counts toward either number
		ClericGrudge shrunk = grudge( HumorTestSupport.make( "a", "anytime", 8, "-", "1.0" ), new Ledger( f ), 3, true );
		assertEquals( 8, shrunk.counter().total );
		assertEquals( 8, shrunk.counter().heard );
	}

	@Test
	public void metaLinesDoNotCountAndAppearOnlyOnTheCounterScreenOnce() throws Exception {
		List<Line> lines = HumorTestSupport.shipped();
		int meta = 0;
		for (Line l : lines) if (l.category == Category.META) meta++;
		assertTrue( meta >= 1 );
		ClericGrudge g = grudge( lines, new Ledger( null ), 1, true );
		assertEquals( lines.size() - meta, g.counter().total + g.counter().appreciationTotal );
		Shown joke = g.counterDiscovery( false, T0 );
		assertNotNull( joke );
		g.confirm( joke, T0 );
		assertNull( g.counterDiscovery( false, T0 + 1 ) );
		assertEquals( "the discovery joke is not a counted complaint", 0, g.counter().heard );
		//and the normal draw never produces one
		for (int i = 0; i < 300; i++){
			Shown s = g.inlineAlways( Moment.MENU, Context.EMPTY, T0 + i * DAY );
			if (s != null) assertTrue( s.category != Category.META );
		}
	}

	@Test
	public void aBrokenPackIsLeftOutAndTheRestStillLoads(){
		ClericGrudge.PackSource src = new ClericGrudge.PackSource(){
			@Override public List<String> names(){ List<String> n = new ArrayList<>(); n.add( "good" ); n.add( "bad" ); n.add( "clash" ); return n; }
			@Override public String read( String name ){
				if (name.equals( "good" )) return "g.0001 | anytime | - | - | 5 | Cleric one. |\ng.0002 | anytime | - | - | 5 | Cleric two. |";
				if (name.equals( "bad" )) return "b.0001 | anytime | - | - | 5 | Cleric is a fucking class. |";
				return "g.0001 | anytime | - | - | 5 | Cleric again. |";
			}
		};
		List<Line> l = ClericGrudge.loadPacks( src );
		assertEquals( 2, l.size() );
	}

	@Test
	public void everyLineInTheLibraryIsAboutCleric(){
		for (Line l : HumorTestSupport.shipped()){
			if (l.category == Category.META) continue;
			assertTrue( l.id, LinePack.ABOUT_CLERIC.matcher( l.clean ).find() );
		}
	}

	private static Set<String> idsOf( List<Line> lines ){
		Set<String> s = new HashSet<>();
		for (Line l : lines) s.add( l.id );
		return s;
	}

	private static Context fullContext(){
		return Context.EMPTY.with( "hero", "Warrior" ).with( "lastHero", "Cleric" ).with( "lastOutcome", "DIED" ).with( "depth", 9 )
				.with( "killer", "gnoll" ).with( "boss", "Goo" ).with( "subclass", "PRIEST" ).with( "spell", "Guiding Light" )
				.with( "weapon", "mace" ).with( "armorAbility", "Ascended Form" ).with( "clericRuns", 12 ).with( "clericWins", 3 )
				.with( "clericDeaths", 9 ).with( "clericStreak", 4 ).with( "guidingLightCasts", 300 );
	}
}
