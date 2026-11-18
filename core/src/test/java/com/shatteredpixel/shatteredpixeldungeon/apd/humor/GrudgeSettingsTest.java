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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

// The Grudge page's rules: where messages may appear, how often, the instant opt-out, and that none of it can be bypassed or double-counted.
public class GrudgeSettingsTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private static final long DAY = 24L * 3600 * 1000;
	private static final long T0 = 1_000_000_000_000L;
	private static final long LATER = T0 + 10 * DAY;

	private static Random rng( long seed ){
		return new Random( new Random( seed ).nextLong() );
	}

	// a ledger whose first-time grace period is long over
	private static Ledger seasoned(){
		Ledger l = new Ledger( null );
		l.setState( "installAt", Long.toString( T0 ) );
		return l;
	}

	private static ClericGrudge fresh( List<Line> lines, Ledger led, long seed ){
		return new ClericGrudge( lines, led, rng( seed ), true, LATER );
	}

	// the surprise moments, in the order a busy session visits them
	private static final Moment[] SESSION = { Moment.STARTUP, Moment.MENU, Moment.LEVEL_CALM, Moment.BOSS_CLEARED, Moment.RUN_END_DEATH, Moment.STATS };

	/** a file-backed store: a stand-in for the app settings that survives a restart */
	private static final class FileStore implements GrudgePrefs.Store {
		private final File file;
		private final Properties p = new Properties();

		FileStore( File file ) throws Exception {
			this.file = file;
			if (file.isFile()) try (FileInputStream in = new FileInputStream( file )){ p.load( in ); }
		}

		@Override public String get( String key ){ return p.getProperty( key, "" ); }

		@Override public void put( String key, String value ){
			p.setProperty( key, value );
			try (FileOutputStream out = new FileOutputStream( file )){ p.store( out, "" ); } catch (Exception e){ throw new RuntimeException( e ); }
		}
	}

	@Test
	public void defaultsAreConservative(){
		GrudgePrefs p = new GrudgePrefs( new HumorTestSupport.MapStore() );
		assertEquals( Frequency.RARER, p.frequency() );
		assertTrue( p.enabled( Location.STARTUP ) );
		assertTrue( p.enabled( Location.MENU ) );
		assertTrue( p.enabled( Location.RUN ) );
		assertTrue( p.enabled( Location.AFTER_RUN ) );
		assertFalse( "statistics screens are off until the player turns them on", p.enabled( Location.STATS ) );
		assertEquals( "the switches the page shows", 5, Location.values().length );
	}

	@Test
	public void preferencesSurviveARestart() throws Exception {
		File f = new File( tmp.getRoot(), "prefs.properties" );
		GrudgePrefs a = new GrudgePrefs( new FileStore( f ) );
		a.setFrequency( Frequency.RARE );
		a.setEnabled( Location.MENU, false );
		a.setEnabled( Location.STATS, true );
		GrudgePrefs b = new GrudgePrefs( new FileStore( f ) );
		assertEquals( Frequency.RARE, b.frequency() );
		assertFalse( b.enabled( Location.MENU ) );
		assertTrue( b.enabled( Location.STATS ) );
		assertTrue( b.enabled( Location.STARTUP ) );
		//the opt-out is saved too
		b.turnOff();
		assertEquals( Frequency.OFF, new GrudgePrefs( new FileStore( f ) ).frequency() );
	}

	@Test
	public void existingSettingsAndSeenHistoryMigrateWithoutLoss() throws Exception {
		//before the location switches existed: a frequency under the old key, a leftover contextual-jokes switch, and a seen history
		File prefs = new File( tmp.getRoot(), "old.properties" );
		FileStore old = new FileStore( prefs );
		old.put( "apd_grudge_popups", "RARE" );
		old.put( "apd_grudge_inline", "0" );
		File ledgerFile = new File( tmp.getRoot(), "apd-humor/ledger.txt" );
		List<Line> lines = HumorTestSupport.shipped();
		Ledger led = new Ledger( ledgerFile );
		for (int i = 0; i < 12; i++) led.markShown( lines.get( i ).id, T0 + i );
		led.save();
		int heardBefore = new ClericGrudge( lines, new Ledger( ledgerFile ), rng( 1 ), true, LATER ).counter().heard
				+ new ClericGrudge( lines, new Ledger( ledgerFile ), rng( 1 ), true, LATER ).counter().appreciationHeard;
		//the new code reads the same stores
		GrudgePrefs p = new GrudgePrefs( new FileStore( prefs ) );
		assertEquals( "the player's frequency is kept", Frequency.RARE, p.frequency() );
		assertTrue( p.enabled( Location.MENU ) );
		assertFalse( p.enabled( Location.STATS ) );
		ClericGrudge g = new ClericGrudge( lines, new Ledger( ledgerFile ), rng( 1 ), true, LATER );
		assertEquals( "no history was lost", 12, heardBefore );
		assertEquals( heardBefore, g.counter().heard + g.counter().appreciationHeard );
	}

	@Test
	public void eachLocationSwitchControlsOnlyItsOwnMoments(){
		List<Line> lines = HumorTestSupport.shipped();
		for (Location only : Location.values()){
			GrudgePrefs prefs = HumorTestSupport.prefsOnly( Frequency.RARE, only );
			for (Moment m : SESSION){
				int shown = 0;
				for (int seed = 0; seed < 150; seed++){
					ClericGrudge g = fresh( lines, seasoned(), seed );
					if (g.popup( m, Context.EMPTY, Safety.CALM, prefs, false, LATER ) != null) shown++;
				}
				if (m.location() == only) assertTrue( only + " should allow " + m + " (" + shown + ")", shown > 0 );
				else assertEquals( only + " must not allow " + m, 0, shown );
			}
		}
	}

	@Test
	public void frequencyOffAndAllLocationsOffEachMeanNothingWhateverTheOther(){
		List<Line> lines = HumorTestSupport.shipped();
		GrudgePrefs off = HumorTestSupport.prefsOnly( Frequency.OFF, Location.values() );
		GrudgePrefs noPlaces = HumorTestSupport.prefsOnly( Frequency.RARE );
		GrudgePrefs noPlacesFrequent = HumorTestSupport.prefsOnly( Frequency.RARER );
		for (int seed = 0; seed < 100; seed++){
			for (Moment m : SESSION){
				assertNull( "frequency Off beats every location", fresh( lines, seasoned(), seed ).popup( m, Context.EMPTY, Safety.CALM, off, false, LATER ) );
				assertNull( "no location beats any frequency", fresh( lines, seasoned(), seed ).popup( m, Context.EMPTY, Safety.CALM, noPlaces, false, LATER ) );
				assertNull( fresh( lines, seasoned(), seed ).popup( m, Context.EMPTY, Safety.CALM, noPlacesFrequent, false, LATER ) );
			}
		}
	}

	@Test
	public void enablingMoreLocationsNeverMultipliesHowOftenASessionGetsAMessage(){
		List<Line> lines = HumorTestSupport.shipped();
		int sessions = 1200;
		int one = 0, all = 0, mostPerSession = 0;
		GrudgePrefs onlyMenu = HumorTestSupport.prefsOnly( Frequency.RARE, Location.MENU );
		GrudgePrefs everywhere = HumorTestSupport.prefsOnly( Frequency.RARE, Location.values() );
		for (int seed = 0; seed < sessions; seed++){
			ClericGrudge a = fresh( lines, seasoned(), seed ), b = fresh( lines, seasoned(), seed );
			boolean gotOne = false;
			int perSession = 0;
			for (Moment m : SESSION){
				if (HumorTestSupport.popupAndShow( a, m, Context.EMPTY, Safety.CALM, onlyMenu, false, LATER ) != null) gotOne = true;
				if (HumorTestSupport.popupAndShow( b, m, Context.EMPTY, Safety.CALM, everywhere, false, LATER ) != null) perSession++;
			}
			if (gotOne) one++;
			if (perSession > 0) all++;
			mostPerSession = Math.max( mostPerSession, perSession );
		}
		double chance = Frequency.RARE.chance;
		assertEquals( "one place", chance, one / (double)sessions, 0.06 );
		assertEquals( "every place: the same share of sessions", chance, all / (double)sessions, 0.06 );
		assertEquals( "never more than one in a session", 1, mostPerSession );
	}

	@Test
	public void turningTheMessagesOffWorksAtOnceAndIsSaved() throws Exception {
		List<Line> lines = HumorTestSupport.shipped();
		File f = new File( tmp.getRoot(), "p.properties" );
		GrudgePrefs prefs = new GrudgePrefs( new FileStore( f ) );
		prefs.setFrequency( Frequency.RARE );
		//find a session that does get a message
		Shown shown = null;
		ClericGrudge g = null;
		for (int seed = 0; seed < 100 && shown == null; seed++){
			g = fresh( lines, seasoned(), seed );
			shown = g.popup( Moment.MENU, Context.EMPTY, Safety.CALM, prefs, false, LATER );
		}
		assertNotNull( shown );
		//the player taps "Turn off these messages" before it is even dismissed
		prefs.turnOff();
		for (Moment m : SESSION) assertNull( g.popup( m, Context.EMPTY, Safety.CALM, prefs, false, LATER + DAY ) );
		//and a later session, after a restart, still gets none
		GrudgePrefs after = new GrudgePrefs( new FileStore( f ) );
		for (int seed = 0; seed < 100; seed++){
			for (Moment m : SESSION) assertNull( fresh( lines, seasoned(), seed ).popup( m, Context.EMPTY, Safety.CALM, after, false, LATER + 40 * DAY ) );
		}
	}

	private static List<Line> story(){
		return LinePack.parse( "app.0001 | appreciation | appreciation,setup | - | 5 | Cleric is my favorite character. | Cleric is my favorite character, I said calmly.\n"
				+ "app.0002 | appreciation | appreciation,after=app.0001,nextsession | - | 5 | That statement about Cleric was a lapse in judgment. |\n", "t" );
	}

	@Test
	public void aStoryContinuesAcrossLocationsAndSessionsAndNothingIsConsumedUnlessShown(){
		List<Line> lines = story();
		Ledger led = seasoned();
		GrudgePrefs startupOnly = HumorTestSupport.prefsOnly( Frequency.RARE, Location.STARTUP );
		//session 1: the story begins at startup. Chosen but not confirmed (the window never came up): nothing is consumed
		Shown start = null;
		ClericGrudge s1 = null;
		for (int seed = 0; seed < 100 && start == null; seed++){
			s1 = new ClericGrudge( lines, led, rng( seed ), true, LATER );
			start = s1.popup( Moment.STARTUP, Context.EMPTY, Safety.CALM, startupOnly, false, LATER );
		}
		assertNotNull( start );
		assertEquals( "app.0001", start.id );
		assertNull( "chosen is not shown: the ledger is untouched", led.get( "app.0001" ) );
		assertEquals( 0, s1.counter().appreciationHeard );
		s1.confirm( start, LATER );
		assertEquals( 1, s1.counter().appreciationHeard );

		//session 2 (later): the follow-up is not available at a location that is switched off, and nothing is consumed there
		GrudgePrefs noAfterRun = HumorTestSupport.prefsOnly( Frequency.RARE, Location.STARTUP, Location.MENU );
		for (int seed = 0; seed < 150; seed++){
			ClericGrudge g = new ClericGrudge( lines, led, rng( seed ), true, LATER + 3 * DAY );
			assertNull( g.popup( Moment.RUN_END_DEATH, Context.EMPTY, Safety.CALM, noAfterRun, false, LATER + 3 * DAY ) );
		}
		assertNull( led.get( "app.0002" ) );
		//with humor Off it is never forced
		GrudgePrefs off = HumorTestSupport.prefsOnly( Frequency.OFF, Location.values() );
		for (int seed = 0; seed < 150; seed++) assertNull( new ClericGrudge( lines, led, rng( seed ), true, LATER + 3 * DAY ).popup( Moment.RUN_END_DEATH, Context.EMPTY, Safety.CALM, off, false, LATER + 3 * DAY ) );
		assertNull( led.get( "app.0002" ) );
		//in the same session as the first part the follow-up waits
		for (int seed = 0; seed < 100; seed++){
			Shown s = new ClericGrudge( lines, led, rng( seed ), true, LATER - DAY ).popup( Moment.RUN_END_DEATH, Context.EMPTY, Safety.CALM,
					HumorTestSupport.prefsOnly( Frequency.RARE, Location.AFTER_RUN ), false, LATER + 3 * DAY );
			if (s != null) assertEquals( "first part already seen this session", "app.0001", s.id );
		}
		//after a later run, where the player allowed it, the punchline arrives and then counts
		GrudgePrefs afterRun = HumorTestSupport.prefsOnly( Frequency.RARE, Location.AFTER_RUN );
		Shown payoff = null;
		ClericGrudge s3 = null;
		for (int seed = 0; seed < 200 && payoff == null; seed++){
			s3 = new ClericGrudge( lines, led, rng( seed ), true, LATER + 3 * DAY );
			payoff = s3.popup( Moment.RUN_END_DEATH, Context.EMPTY, Safety.CALM, afterRun, false, LATER + 3 * DAY );
		}
		assertNotNull( payoff );
		assertEquals( "app.0002", payoff.id );
		assertNull( led.get( "app.0002" ) );
		s3.confirm( payoff, LATER + 3 * DAY );
		assertNotNull( led.get( "app.0002" ) );
	}

	@Test
	public void cleanIsTheDefaultAndAdultWordingOnlyAppearsWhenAsked(){
		List<Line> lines = HumorTestSupport.shipped();
		GrudgePrefs prefs = HumorTestSupport.prefs( Frequency.RARE );
		int clean = 0, adultSeen = 0;
		for (int seed = 0; seed < 600; seed++){
			for (boolean adult : new boolean[]{ false, true }){
				ClericGrudge g = fresh( lines, seasoned(), seed );
				Shown s = g.popup( Moment.MENU, Context.EMPTY, Safety.CALM, prefs, adult, LATER );
				if (s == null) continue;
				if (!adult){
					clean++;
					assertFalse( s.id + " leaked profanity to a Clean player", LinePack.PROFANITY.matcher( s.text ).find() );
				} else if (LinePack.PROFANITY.matcher( s.text ).find()){
					adultSeen++;
				}
			}
		}
		assertTrue( clean > 20 );
		assertTrue( "Adult players do get the stronger wording", adultSeen > 0 );
	}

	@Test
	public void unsafeMomentsAreRefusedInEveryLocation(){
		List<Line> lines = HumorTestSupport.shipped();
		GrudgePrefs everywhere = HumorTestSupport.prefsOnly( Frequency.RARE, Location.values() );
		Safety busy = new Safety( true, true, true, true, true, true, true );
		for (int seed = 0; seed < 100; seed++){
			for (Moment m : SESSION) assertNull( fresh( lines, seasoned(), seed ).popup( m, Context.EMPTY, busy, everywhere, false, LATER ) );
		}
	}

	@Test
	public void theCountersMatchTheContentAndTheHistoryExactly(){
		List<Line> lines = HumorTestSupport.shipped();
		Ledger led = seasoned();
		ClericGrudge g = new ClericGrudge( lines, led, rng( 1 ), true, LATER );
		int hatred = 0, appreciation = 0;
		for (Line l : lines){
			if (!l.counted) continue;
			if (l.pool == Pool.APPRECIATION) appreciation++; else hatred++;
		}
		assertEquals( hatred, g.counter().total );
		assertEquals( appreciation, g.counter().appreciationTotal );
		Set<String> seen = new java.util.HashSet<>();
		int h = 0, a = 0;
		for (Line l : lines){
			if (!l.counted || seen.size() >= 30) continue;
			led.markShown( l.id, T0 );
			led.markShown( l.id, T0 + 1 );
			seen.add( l.id );
			if (l.pool == Pool.APPRECIATION) a++; else h++;
		}
		assertEquals( h, g.counter().heard );
		assertEquals( a, g.counter().appreciationHeard );
	}

	@Test
	public void aStoryDelayIsCountedFromTheFirstTimeThePlayerWasReallyShownTheFirstPart(){
		//every shipped delayed follow-up: not before its delay has passed since the first sighting, and not earlier because of install or eligibility
		int checked = 0;
		List<Line> lines = HumorTestSupport.shipped();
		java.util.Map<String, Line> byId = new java.util.HashMap<>();
		for (Line l : lines) byId.put( l.id, l );
		for (Line l : lines){
			if (l.delayDays <= 0) continue;
			checked++;
			Ledger led = new Ledger( null );
			//the first part is shown at T0, and again much later (a repeat): the clock still runs from T0
			led.markShown( l.after, T0 );
			led.markShown( l.after, T0 + (l.delayDays - 1) * DAY );
			assertFalse( l.id + " one day early", l.eligible( Moment.MENU, Context.EMPTY, led, Long.MAX_VALUE, T0 + (l.delayDays - 1) * DAY ) );
			assertTrue( l.id + " on time, counted from the first sighting", l.eligible( Moment.MENU, Context.EMPTY, led, Long.MAX_VALUE, T0 + l.delayDays * DAY ) );
			//a first part that was never shown starts no clock at all
			assertFalse( l.id + " unseen first part", l.eligible( Moment.MENU, Context.EMPTY, new Ledger( null ), Long.MAX_VALUE, T0 + 1000 * DAY ) );
		}
		assertTrue( "the 14, 30 and 45 day stories exist", checked >= 3 );
		java.util.Set<Integer> days = new java.util.TreeSet<>();
		for (Line l : lines) if (l.delayDays > 0) days.add( l.delayDays );
		assertTrue( days.toString(), days.contains( 14 ) && days.contains( 30 ) && days.contains( 45 ) );
	}

	@Test
	public void theFirstSightingSurvivesARestartAndOlderHistoryFilesStillLoad() throws Exception {
		File f = new File( tmp.getRoot(), "apd-humor/ledger.txt" );
		Ledger a = new Ledger( f );
		a.markShown( "app.0142", T0 );
		a.markShown( "app.0142", T0 + 20 * DAY );
		a.save();
		assertEquals( T0, new Ledger( f ).get( "app.0142" ).firstAt );
		assertEquals( T0 + 20 * DAY, new Ledger( f ).get( "app.0142" ).lastAt );
		//a history file written before the first-sighting column existed
		File old = new File( tmp.getRoot(), "old/ledger.txt" );
		old.getParentFile().mkdirs();
		java.nio.file.Files.write( old.toPath(), "#apd-humor-ledger v1\n@seq=3\napp.0142\t2\t3\t5000\n".getBytes( java.nio.charset.StandardCharsets.UTF_8 ) );
		Ledger migrated = new Ledger( old );
		assertEquals( 2, migrated.get( "app.0142" ).count );
		assertEquals( 5000, migrated.get( "app.0142" ).firstAt );
	}

	@Test
	public void theFirstClericRunWarningIsIndependentOfTheRollAndRespectsTheOptOutAndIsRememberedOnlyWhenShown() throws Exception {
		List<Line> lines = HumorTestSupport.shipped();
		GrudgePrefs prefs = HumorTestSupport.prefs( Frequency.RARER );
		File f = new File( tmp.getRoot(), "apd-humor/ledger.txt" );
		//a session whose surprise roll failed and that already used its one popup: the warning still comes (it is the player's own choice)
		ClericGrudge g = new ClericGrudge( lines, new Ledger( f ), rng( 1 ), true, LATER );
		Shown shown = g.firstClericRun( prefs, false, LATER );
		assertNotNull( shown );
		assertEquals( Category.FIRST_RUN, shown.category );
		assertTrue( shown.text.contains( "legitimate class" ) );
		//chosen but never displayed: not remembered, so it can still come later
		assertNotNull( new ClericGrudge( lines, new Ledger( f ), rng( 2 ), true, LATER ).firstClericRun( prefs, false, LATER + 1 ) );
		//displayed: remembered across a restart, and never again
		g.confirm( shown, LATER );
		assertNull( new ClericGrudge( lines, new Ledger( f ), rng( 3 ), true, LATER + DAY ).firstClericRun( prefs, true, LATER + DAY ) );
		//the master opt-out wins, and the warning is then still available if the player turns messages back on
		GrudgePrefs off = HumorTestSupport.prefs( Frequency.OFF );
		File g2 = new File( tmp.getRoot(), "other/ledger.txt" );
		assertNull( new ClericGrudge( lines, new Ledger( g2 ), rng( 4 ), true, LATER ).firstClericRun( off, false, LATER ) );
		assertNotNull( new ClericGrudge( lines, new Ledger( g2 ), rng( 4 ), true, LATER ).firstClericRun( prefs, false, LATER ) );
		//the switch for the whole feature wins too
		assertNull( new ClericGrudge( lines, new Ledger( null ), rng( 5 ), false, LATER ).firstClericRun( prefs, false, LATER ) );
	}

	@Test
	public void clericRunStartNeverUsesTheRemovedBlackScreenAndLoreNeverBlocksStart() throws Exception {
		String hero = new String( java.nio.file.Files.readAllBytes( new File( System.getProperty( "apd.game.src" ), "scenes/HeroSelectScene.java" ).toPath() ), java.nio.charset.StandardCharsets.UTF_8 );
		int hook = hero.indexOf( "GrudgeHooks.firstClericRun" );
		assertFalse( "approved Cleric starts must never route to the undismissable black screen", hero.contains( "WndClericRemoved.trigger();" ) );
		assertFalse( "normal APKs must not be blocked by their missing private feature asset", hero.contains( "&& !com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.clericPlayable()" ) );
		assertTrue( "the separate optional Cleric lore warning is preserved", hook > 0 );
		assertTrue( "only Cleric runs invoke the optional first-run lore hook",
				hero.substring(Math.max(0, hook - 130), hook).contains( "selectedClass == HeroClass.CLERIC" ) );
		assertTrue( "all classes still use the same guarded new-slot start method", hero.contains( "private void startRun()" ) );
		assertTrue( "both first-run lore completion and the normal path start the run", hero.indexOf( "startRun();", hook ) > hook );
		String wnd = new String( java.nio.file.Files.readAllBytes( new File( System.getProperty( "apd.game.src" ), "windows/WndClericGrudge.java" ).toPath() ), java.nio.charset.StandardCharsets.UTF_8 );
		assertTrue( "closing the optional lore warning continues the same start", wnd.contains( "onBackPressed" ) && wnd.contains( "proceed.run()" ) );
	}

	@Test
	public void fictionIsLabelledAsFictionAndTheGrudgeNeverTouchesAchievementsOrStatistics() throws Exception {
		for (Line l : HumorTestSupport.shipped()){
			boolean statsy = LinePack.STATISTICSY.matcher( l.clean ).find();
			if (statsy) assertTrue( l.id + " sounds like a statistic and must be a flagged joke", l.joke );
			if (LinePack.ACHIEVEMENT.matcher( l.clean ).find()) assertTrue( l.id, l.clean.toLowerCase().contains( "fake" ) && l.clean.toLowerCase().contains( "not a real" ) );
		}
		//the humor code cannot reach badges, rankings or the recorder: it is plain Java and writes only its own ledger
		for (File src : new File( System.getProperty( "apd.game.src" ), "apd/humor" ).listFiles()){
			String text = new String( java.nio.file.Files.readAllBytes( src.toPath() ), java.nio.charset.StandardCharsets.UTF_8 );
			assertFalse( src.getName(), text.contains( "Badges" ) || text.contains( "Rankings" ) || text.contains( "ApdRecorder" ) );
		}
	}
}
