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

import java.util.List;
import java.util.Random;

// The permanent creative direction, as rules the packs must obey: the developer says "I", invents no community, and the self-aware
// layers (remarks on the grudge itself, long-delayed callbacks) are possible without being part of the first release.
public class CreativeDirectionTest {

	private static final long DAY = 24L * 3600 * 1000;
	private static final long T0 = 1_000_000_000_000L;

	private static void rejects( String row, String fragment ){
		try {
			LinePack.parse( row, "t" );
			fail( "should have been refused: " + row );
		} catch (LinePack.PackException e){
			assertTrue( e.getMessage(), e.getMessage().contains( fragment ) );
		}
	}

	@Test
	public void theDeveloperSpeaksAsIAndNeverAsWe(){
		for (String bad : new String[]{ "We hate Cleric.", "Cleric is bad for us.", "Our Cleric problem continues.", "Let's talk about Cleric.",
				"We're done with Cleric.", "Cleric is ours to suffer." }){
			rejects( "any.0001 | anytime | - | - | 5 | " + bad + " |", "speaks as 'I'" );
		}
		rejects( "any.0001 | anytime | - | - | 5 | Cleric is bad. | We hate Cleric.", "speaks as 'I'" );
		LinePack.parse( "any.0001 | anytime | - | - | 5 | I hate Cleric, and my reasons are my own. |", "t" );
		//the shipped lines all comply (parsing is the lint)
		for (Line l : HumorTestSupport.shipped()){
			assertFalse( l.id, LinePack.PLURAL_VOICE.matcher( l.clean ).find() );
			if (l.adult != null) assertFalse( l.id, LinePack.PLURAL_VOICE.matcher( l.adult ).find() );
		}
	}

	@Test
	public void theDeveloperNeverCallsHimselfTheDeveloper(){
		for (String bad : new String[]{ "The developer hates Cleric.", "This developer is tired of Cleric.", "Local developer insults Cleric again.",
				"As the developer, I dislike Cleric." }){
			rejects( "any.0001 | anytime | - | - | 5 | " + bad + " |", "never as" );
		}
		LinePack.parse( "any.0001 | anytime | - | - | 5 | I dislike Cleric, and I am the one who wrote this. |", "t" );
	}

	@Test
	public void noLineInventsOrQuotesACommunity(){
		for (String bad : new String[]{ "The community agrees Cleric is bad.", "Players have told me Cleric is great.", "Someone on Reddit said Cleric is fine.",
				"A streamer won with Cleric.", "The forums are full of Cleric posts.", "Players wrote to me about Cleric." }){
			rejects( "any.0001 | anytime | - | - | 5 | " + bad + " |", "community" );
		}
	}

	@Test
	public void aLongDelayedPunchlineWaitsForTheDelay(){
		List<Line> lines = LinePack.parse( "app.0001 | appreciation | appreciation,setup | - | 5 | Cleric is my favorite. |\n"
				+ "app.0002 | appreciation | appreciation,after=app.0001,delay=30 | - | 5 | Thirty days later: Cleric is still not my favorite. |\n", "t" );
		Ledger led = new Ledger( null );
		led.markShown( "app.0001", T0 );
		assertEquals( 30, lines.get( 1 ).delayDays );
		assertEquals( "app.0001", Picker.pick( lines, Moment.MENU, Context.EMPTY, led, new Random( 1 ), null, Long.MAX_VALUE, T0 + 29 * DAY ).id );
		assertEquals( "app.0002", Picker.pick( lines, Moment.MENU, Context.EMPTY, led, new Random( 1 ), null, Long.MAX_VALUE, T0 + 30 * DAY ).id );
		LinePack.PackException e = null;
		try {
			LinePack.parse( "app.0003 | appreciation | appreciation,delay=5 | - | 5 | Cleric. |", "t" );
		} catch (LinePack.PackException x){
			e = x;
		}
		assertNotNull( "delay needs a first part", e );
	}

	@Test
	public void theGameCanRemarkOnItsOwnComplainingUsingOnlyRealTotals(){
		//a future self-aware line: it knows how many complaints this player has really heard, and only speaks once that is true
		List<Line> lines = LinePack.parse(
				"any.0001 | anytime | - | - | 5 | Cleric plain complaint one. |\n"
				+ "any.0002 | anytime | - | - | 5 | Cleric plain complaint two. |\n"
				+ "any.0003 | anytime | - | - | 5 | Cleric plain complaint three. |\n"
				+ "meta2.0001 | anytime | - | complaintsHeard>=3 | 1 | You have now heard {complaintsHeard} of my {complaintsTotal} Cleric complaints. I should stop. I will not stop. |\n", "t" );
		ClericGrudge g = new ClericGrudge( lines, new Ledger( null ), new Random( 3 ), true, T0 );
		//until three complaints were really heard, the remark can never appear
		for (int i = 0; i < 3; i++){
			Shown s = g.inlineAlways( Moment.MENU, Context.EMPTY, T0 + i );
			assertFalse( s.text, s.text.contains( "I should stop" ) );
		}
		boolean said = false;
		for (int i = 0; i < 3 && !said; i++){
			Shown s = g.inlineAlways( Moment.MENU, Context.EMPTY, T0 + 100 + i );
			if (s.text.contains( "I should stop" )){
				said = true;
				assertEquals( "You have now heard 3 of my 4 Cleric complaints. I should stop. I will not stop.", s.text );
			}
		}
		assertTrue( said );
	}

	@Test
	public void theSelfAwareLayersAreNotPartOfTheFirstContentVersion(){
		//only the first content version exists; later layers arrive as new `since` versions, never dumped in at once
		java.util.Set<Integer> versions = new java.util.TreeSet<>();
		for (Line l : HumorTestSupport.shipped()) versions.add( l.since );
		assertEquals( "one content version so far", 1, versions.size() );
		for (Line l : HumorTestSupport.shipped()){
			assertNull( l.id + " uses the grudge's own totals before the layers were introduced", usesGrudgeTotals( l ) );
		}
	}

	private static String usesGrudgeTotals( Line l ){
		return (l.clean.contains( "{complaints" ) || l.clean.contains( "{appreciations" )) ? l.id : null;
	}

	@Test
	public void factsAboutAFinishedRunNeedAVerifiedRun(){
		rejects( "any.0001 | death | - | lastHero=CLERIC & lastOutcome=DIED | 5 | Cleric died. |", "runVerified" );
		rejects( "any.0001 | victory | - | lastOutcome=WON | 5 | Cleric won. |", "runVerified" );
		rejects( "any.0001 | death | - | depth>=1 | 5 | {lastHeroName} died. Cleric would not have. |", "runVerified" );
		LinePack.parse( "any.0001 | death | - | lastHero=CLERIC & lastOutcome=DIED & runVerified=1 | 5 | Cleric died. |", "t" );
		//the boss fact belongs to lines about a defeated boss
		rejects( "any.0001 | anytime | - | boss!=none | 5 | Cleric versus {boss}. |", "boss category" );
		LinePack.parse( "any.0001 | boss | - | boss!=none | 5 | You defeated {boss}. Cleric would not have. |", "t" );
	}

	@Test
	public void noLineExposesPersonalInformation(){
		for (String bad : new String[]{ "I wrote this with my full legal name on it, about Cleric.", "My home address is on the Cleric complaint form.",
				"Email me at my email about Cleric.", "My real name is on the Cleric poster." }){
			rejects( "any.0001 | anytime | - | - | 5 | " + bad + " |", "personal information" );
		}
	}

	@Test
	public void adultVariantsAreMoreThanCleanTextWithProfanityInserted(){
		//where the swearing IS the punchline of a different sentence, the line is listed here on purpose
		java.util.Set<String> swearIsTheJoke = new java.util.HashSet<>( java.util.Arrays.asList( "any.0043", "fmt.0028" ) );
		java.util.regex.Pattern swear = java.util.regex.Pattern.compile( LinePack.PROFANITY.pattern() + "|\\b(damn\\w*|hell|ass|crap\\w*|dumbass)\\b", java.util.regex.Pattern.CASE_INSENSITIVE );
		for (Line l : HumorTestSupport.shipped()){
			if (l.adult == null || swearIsTheJoke.contains( l.id )) continue;
			String a = norm( swear.matcher( l.adult ).replaceAll( "" ) ), c = norm( swear.matcher( l.clean ).replaceAll( "" ) );
			boolean justInserted = a.equals( c ) || (a.startsWith( c ) && a.length() - c.length() < 8);
			assertFalse( l.id + " Adult wording is just the Clean text with profanity added; give it its own punchline", justInserted );
		}
	}

	private static String norm( String t ){
		return t.toLowerCase().replaceAll( "[^a-z0-9 ]", "" ).replaceAll( "\\s+", " " ).trim();
	}

	@Test
	public void subclassAscensionAndPickingLinesNeedTheirVerifiedFacts(){
		rejects( "any.0001 | death | - | lastHero=CLERIC & lastOutcome=DIED & runVerified=1 | 5 | A Priest died. Cleric would have. |", "subclass=PRIEST" );
		rejects( "any.0001 | death | - | subclass=PRIEST | 5 | A Paladin died. Cleric would have. |", "subclass=PALADIN" );
		LinePack.parse( "any.0001 | death | - | lastHero=CLERIC & lastOutcome=DIED & subclass=PRIEST & runVerified=1 | 5 | A Priest died. Cleric would have. |", "t" );
		//a win is not an ascension
		rejects( "any.0001 | victory | - | lastHero=CLERIC & lastOutcome=WON & runVerified=1 | 5 | You ascended with Cleric. |", "lastAscended=1" );
		LinePack.parse( "any.0001 | victory | - | lastHero=CLERIC & lastOutcome=WON & lastAscended=1 & runVerified=1 | 5 | You ascended with Cleric. |", "t" );
		//the ability named Ascended Form is not a claim about ascending
		LinePack.parse( "any.0001 | victory | - | lastHero=CLERIC & lastOutcome=WON & runVerified=1 | 5 | Ascended Form is a Cleric ability. |", "t" );
		//advice about picking Cleric only while Cleric can be picked
		rejects( "any.0001 | anytime | - | - | 5 | Please do not pick Cleric. |", "clericAvailable=1" );
		LinePack.parse( "any.0001 | anytime | - | clericAvailable=1 | 5 | Please do not pick Cleric. |", "t" );
	}

	@Test
	public void theShippedLinesBehaveAsTheAuditRequires(){
		java.util.Map<String, Line> byId = new java.util.HashMap<>();
		for (Line l : HumorTestSupport.shipped()) byId.put( l.id, l );
		Ledger led = new Ledger( null );
		Context run = Context.EMPTY.with( "runVerified", 1 ).with( "lastHero", "CLERIC" ).with( "hero", "CLERIC" ).with( "lastOutcome", "WON" )
				.with( "depth", 26 ).with( "clericRuns", 3 ).with( "clericWins", 1 ).with( "clericDeaths", 2 );
		//a win by leaving is not an ascension; a completed ascension is
		assertFalse( byId.get( "clr.win.0012" ).eligible( Moment.RUN_END_VICTORY, run.with( "lastAscended", 0 ), led, Long.MAX_VALUE, 0 ) );
		assertTrue( byId.get( "clr.win.0012" ).eligible( Moment.RUN_END_VICTORY, run.with( "lastAscended", 1 ), led, Long.MAX_VALUE, 0 ) );
		//an unverified result unlocks nothing about a finished run
		Context unverified = Context.EMPTY.with( "lastHero", "CLERIC" ).with( "lastOutcome", "WON" ).with( "depth", 26 );
		for (Line l : byId.values()) assertFalse( l.id, l.eligible( Moment.RUN_END_VICTORY, unverified, led, Long.MAX_VALUE, 0 ) && l.category != Category.POSTRUN && l.category != Category.ANYTIME && l.category != Category.APPRECIATION && l.category != Category.FOURTH_WALL );
		//while Cleric is blocked, the lines that tell players not to pick it stay silent
		assertFalse( byId.get( "menu.0007" ).eligible( Moment.MENU, Context.EMPTY.with( "clericAvailable", 0 ), led, Long.MAX_VALUE, 0 ) );
		assertFalse( byId.get( "menu.0007" ).eligible( Moment.MENU, Context.EMPTY, led, Long.MAX_VALUE, 0 ) );
		assertTrue( byId.get( "menu.0007" ).eligible( Moment.MENU, Context.EMPTY.with( "clericAvailable", 1 ), led, Long.MAX_VALUE, 0 ) );
		//subclass dialogue needs the matching subclass
		Context died = run.with( "lastOutcome", "DIED" );
		assertFalse( byId.get( "clr.death.0014" ).eligible( Moment.RUN_END_DEATH, died, led, Long.MAX_VALUE, 0 ) );
		assertFalse( byId.get( "clr.death.0014" ).eligible( Moment.RUN_END_DEATH, died.with( "subclass", "PALADIN" ), led, Long.MAX_VALUE, 0 ) );
		assertTrue( byId.get( "clr.death.0014" ).eligible( Moment.RUN_END_DEATH, died.with( "subclass", "PRIEST" ), led, Long.MAX_VALUE, 0 ) );
		assertTrue( byId.get( "clr.death.0015" ).eligible( Moment.RUN_END_DEATH, died.with( "subclass", "PALADIN" ), led, Long.MAX_VALUE, 0 ) );
		assertFalse( byId.get( "clr.death.0015" ).eligible( Moment.RUN_END_DEATH, died.with( "subclass", "PRIEST" ), led, Long.MAX_VALUE, 0 ) );
		//"not a record" is gone: no line claims a personal best that the game has not verified
		for (Line l : byId.values()) assertFalse( l.id, l.clean.toLowerCase().contains( "not a record" ) );
	}

	@Test
	public void aBossIsDefeatedExactlyWhenItsOwnScoreTurnsPositive(){
		boolean[] down = new boolean[5];
		//the first look only records the state: a loaded run that already beat the first boss says nothing
		assertEquals( -1, com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.fallenBoss( new int[]{ 900, 0, 0, 0, 0 }, down, false ) );
		assertEquals( -1, com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.fallenBoss( new int[]{ 900, 0, 0, 0, 0 }, down, true ) );
		//penalties alone (a negative score) never count as a defeat
		assertEquals( -1, com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.fallenBoss( new int[]{ 900, -100, 0, 0, 0 }, down, true ) );
		//the second boss dies: its score jumps positive
		assertEquals( 1, com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.fallenBoss( new int[]{ 900, 1900, 0, 0, 0 }, down, true ) );
		assertEquals( "reported once", -1, com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.fallenBoss( new int[]{ 900, 1900, 0, 0, 0 }, down, true ) );
		//a new run resets the scores and the next boss is seen again
		com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.fallenBoss( new int[]{ 0, 0, 0, 0, 0 }, down, true );
		assertEquals( 0, com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.fallenBoss( new int[]{ 1000, 0, 0, 0, 0 }, down, true ) );
	}

	@Test
	public void everyFactTheGameSuppliesIsReallyReadFromTheGameAndNothingElseIsInvented() throws Exception {
		String hooks = new String( java.nio.file.Files.readAllBytes( new java.io.File( System.getProperty( "apd.game.src" ), "windows/GrudgeHooks.java" ).toPath() ),
				java.nio.charset.StandardCharsets.UTF_8 );
		java.util.Set<String> passed = new java.util.HashSet<>();
		java.util.regex.Matcher m = java.util.regex.Pattern.compile( "\\.with\\( \"([A-Za-z]+)\"" ).matcher( hooks );
		while (m.find()) passed.add( m.group( 1 ) );
		assertEquals( "the list of supplied facts matches what the hooks really pass", passed, com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.SUPPLIED );
		//a line that uses any fact the game does not supply can never be shown, whatever the other facts say
		java.util.Set<String> supplied = new java.util.HashSet<>( com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.SUPPLIED );
		Context everything = Context.EMPTY;
		for (String k : supplied) everything = everything.with( k, k.equals( "lastOutcome" ) ? "DIED" : (k.endsWith( "Name" ) ? "Name" : "1") );
		int silent = 0;
		for (Line l : HumorTestSupport.shipped()){
			java.util.Set<String> used = new java.util.HashSet<>();
			java.util.regex.Matcher pm = Line.PLACEHOLDER.matcher( l.clean );
			while (pm.find()) used.add( pm.group( 1 ) );
			for (Term t : l.requires ) used.add( t.key );
			used.removeAll( java.util.Arrays.asList( "complaintsHeard", "complaintsTotal", "appreciationsHeard", "appreciationsTotal" ) );
			if (supplied.containsAll( used )) continue;
			silent++;
			for (Moment mo : Moment.values()) assertFalse( l.id + " uses facts the game does not supply", l.eligible( mo, everything, new Ledger( null ), Long.MAX_VALUE, Long.MAX_VALUE ) );
		}
		assertEquals( "all curated entries use facts actually supplied by the game", 0, silent );
	}
}
