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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.ClericGrudge;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.Context;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.Counter;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.Frequency;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.GrudgePrefs;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.Ledger;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.Location;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.Moment;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.Safety;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.Shown;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ClericHistory;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.RankingsScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Scene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// The game's side of the Cleric grudge: where its files live, what the game tells it, and the one place a popup is put on screen. The humor
// code itself never sees the game. Everything here is a no-op while ClericGrudge.enabled() is off, and a failure anywhere in it is swallowed:
// the grudge may never reach play.
//
// A surprise is shown only in a place the player switched on (startup, main menu, during runs, after a run, statistics screens), once the
// scene has settled and no window is open; during a run only after a long, quiet stretch (hero idle, nothing hostile in view, not a boss
// floor). The popup always carries "Turn off these messages".
public final class GrudgeHooks {

	private static final long SETTLE_MS = 1500;
	//the very first title screen waits a little longer, so startup work and any recovery prompt are long finished
	private static final long STARTUP_SETTLE_MS = 3500;

	private static ClericGrudge instance;
	private static Ledger ledger;
	//its own random source, nothing to do with the game's
	private static final Random RNG = new Random();

	//what the history really says about Cleric, worked out once per session on a background thread
	private static volatile Context historyFacts;
	private static boolean historyRequested;

	//the first title screen of this process is "startup"; later visits are "main menu"
	private static boolean startupSeen;

	private static Scene lastScene;
	private static long sceneSince;
	private static boolean decided;
	private static Moment pendingEnd;
	//in play: how long things have been quiet, and whether a boss has fallen since the last chance
	private static long calmSince;
	//a boss counts as defeated when its score turns positive (each boss adds a large positive score only when it dies; before that only penalties)
	private static final boolean[] bossDown = new boolean[5];
	private static boolean bossBaseline;
	private static boolean bossFell;
	private static int bossIndex = -1;
	private static Context pendingContext = Context.EMPTY;

	private GrudgeHooks(){}

	private static final ClericGrudge.PackSource PACKS = new ClericGrudge.PackSource(){
		@Override
		public List<String> names() throws IOException {
			List<String> names = new ArrayList<>();
			for (String n : read( "packs.txt" ).split( "\r?\n" )) if (!n.trim().isEmpty()) names.add( n.trim() );
			return names;
		}

		@Override
		public String read( String name ) throws IOException {
			return Gdx.files.internal( "apd/humor/" + name ).readString( "UTF-8" );
		}
	};

	private static synchronized ClericGrudge grudge(){
		if (instance == null){
			//next to the history folder, not inside it, so exports and imports of history never carry it
			File history = ApdRecorder.historyRoot();
			File file = history == null || history.getParentFile() == null ? null : new File( history.getParentFile(), "apd-humor/ledger.txt" );
			ledger = new Ledger( file );
			instance = new ClericGrudge( ClericGrudge.loadPacks( PACKS ), ledger, RNG, ClericGrudge.enabled() );
		}
		return instance;
	}

	public static boolean available(){
		return ClericGrudge.enabled();
	}

	public static Counter counter(){
		return grudge().counter();
	}

	//the one-time joke for opening the counter screen, or null
	public static String discovery(){
		long now = System.currentTimeMillis();
		Shown s = grudge().counterDiscovery( SPDSettings.apdAdultHumor(), now );
		if (s == null) return null;
		//the caller puts the text on the page straight away
		grudge().confirm( s, now );
		return s.text;
	}

	//the preferences live in the app settings; the rules about them are in GrudgePrefs
	private static final GrudgePrefs PREFS = new GrudgePrefs( new GrudgePrefs.Store(){
		@Override public String get( String key ){ return SPDSettings.apdGrudgePref( key ); }
		@Override public void put( String key, String value ){ SPDSettings.apdGrudgePref( key, value ); }
	} );

	public static GrudgePrefs prefs(){
		return PREFS;
	}

	public static Frequency frequency(){
		return PREFS.frequency();
	}

	public static void setFrequency( Frequency f ){
		if (!ClericGrudge.enabled()) return;
		PREFS.setFrequency( f );
	}

	// "Turn off these messages": immediate and saved
	public static void turnOff(){
		if (!ClericGrudge.enabled()) return;
		PREFS.turnOff();
	}

	// Called when the player starts a Cleric run. Returns true if the one-time warning was put on screen (the run then starts when it is
	// dismissed, by `proceed`); false means nothing was shown and the caller starts the run itself. It never blocks and never fails the start.
	public static boolean firstClericRun( final Runnable proceed ){
		if (!ClericGrudge.enabled()) return false;
		try {
			if (!clericAvailable()) return false;
			ClericGrudge g = grudge();
			long now = System.currentTimeMillis();
			Shown shown = g.firstClericRun( PREFS, SPDSettings.apdAdultHumor(), now );
			if (shown == null) return false;
			ShatteredPixelDungeon.scene().addToFront( WndClericGrudge.firstRun( shown.text, proceed ) );
			//only now, with the window really up, is the warning remembered as shown
			g.confirm( shown, now );
			return true;
		} catch (Throwable t){
			return false;
		}
	}

	public static void resetHistory(){
		if (!ClericGrudge.enabled()) return;
		grudge();
		ledger.clear();
	}

	// Called when a run has permanently ended. It only notes what happened; the results screen shows it if a popup is due.
	public static void onRunEnded( boolean won, Object cause ){
		if (!ClericGrudge.enabled()) return;
		try {
			//only a run the game itself ended the normal way counts: the recorded result agrees with the call, and a win has a living hero
			if (Dungeon.hero == null || Statistics.gameWon != won || (won && !Dungeon.hero.isAlive())) return;
			Context c = Context.EMPTY.with( "runVerified", 1 ).with( "lastOutcome", won ? "WON" : "DIED" ).with( "depth", Dungeon.depth )
					.with( "lastAscended", won && Statistics.ascended ? 1 : 0 ).with( "lastAmulet", Statistics.amuletObtained ? 1 : 0 );
			if (Dungeon.hero != null){
				c = c.with( "lastHero", Dungeon.hero.heroClass.name() ).with( "hero", Dungeon.hero.heroClass.name() )
						.with( "lastHeroName", Dungeon.hero.heroClass.title() ).with( "heroName", Dungeon.hero.heroClass.title() );
				if (Dungeon.hero.subClass != null && Dungeon.hero.subClass != HeroSubClass.NONE) c = c.with( "subclass", Dungeon.hero.subClass.name() );
			}
			if (cause instanceof Char) c = c.with( "killer", ((Char)cause).name() );
			pendingContext = c;
			pendingEnd = won ? Moment.RUN_END_VICTORY : Moment.RUN_END_DEATH;
		} catch (Throwable ignored){
			//never into the game
		}
	}

	// Starts, once, reading the Cleric counts from the real history. Until it finishes the popup simply waits; if it fails there are no
	// history facts and the lines that need them stay silent.
	private static void requestHistory(){
		if (historyRequested) return;
		historyRequested = true;
		final File root = ApdRecorder.historyRoot();
		Thread t = new Thread( new Runnable(){
			@Override
			public void run(){
				Context c = Context.EMPTY;
				try {
					if (root != null){
						//APD: never build the full UIMSD catalog here; during play it could exhaust memory.
						//The run-facts summary is one small row per run.
						ClericHistory h = ClericHistory.fromRunFacts(
								new com.shatteredpixel.shatteredpixeldungeon.apd.stats.DerivedStore( root ).load().rows() );
						c = c.with( "clericRuns", h.runs ).with( "clericWins", h.wins ).with( "clericDeaths", h.deaths );
					}
				} catch (Throwable ignored){
					//no history facts
				}
				historyFacts = c;
			}
		}, "apd-grudge-history" );
		t.setDaemon( true );
		//APD: stay out of the way of the title screen while it loads the history
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	// Can Cleric be chosen for a run right now? The same switch the hero select reads, so the two can never disagree.
	static boolean clericAvailable(){
		try {
			return com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.clericPlayable();
		} catch (Throwable t){
			return false;
		}
	}

	// Is the hero idle in a quiet place? Reads game state only; it changes none of it, spends no turn and draws no random number.
	private static boolean quiet(){
		if (Dungeon.hero == null || Dungeon.level == null || !Dungeon.hero.isAlive()) return false;
		if (!Dungeon.hero.ready || Dungeon.hero.resting || Dungeon.hero.curAction != null) return false;
		if (Dungeon.level.locked || Dungeon.bossLevel() || GameScene.showingWindow()) return false;
		for (Mob m : Dungeon.level.mobs){
			if (m.isAlive() && m.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[m.pos]) return false;
		}
		return true;
	}

	// LEVEL_CALM after a continuous quiet stretch, BOSS_CLEARED the first time it is quiet after a boss fell; otherwise null.
	private static Moment quietMoment( long now ){
		int fallen = fallenBoss( Statistics.bossScores, bossDown, bossBaseline );
		bossBaseline = true;
		if (fallen >= 0){
			bossFell = true;
			bossIndex = fallen;
		}
		if (!quiet()){
			calmSince = 0;
			return null;
		}
		if (calmSince == 0) calmSince = now;
		if (now - calmSince < 2500) return null;
		if (bossFell){
			bossFell = false;
			return Moment.BOSS_CLEARED;
		}
		return Moment.LEVEL_CALM;
	}

	// Which boss has just fallen, or -1. `down` remembers which bosses were already down at the last look (it is updated here); the first
	// look only records the state, so loading a run that already beat a boss says nothing. A boss is down when its score is positive.
	public static int fallenBoss( int[] scores, boolean[] down, boolean baselineKnown ){
		int found = -1;
		for (int i = 0; i < down.length && i < scores.length; i++){
			boolean now = scores[i] > 0;
			if (baselineKnown && now && !down[i]) found = i;
			down[i] = now;
		}
		return found;
	}

	//the facts the game side passes in (everything else a line may use stays absent, so such lines never show)
	public static final java.util.Set<String> SUPPLIED = java.util.Collections.unmodifiableSet( new java.util.HashSet<>( java.util.Arrays.asList(
			"runVerified", "lastOutcome", "depth", "lastAscended", "lastAmulet", "lastHero", "hero", "lastHeroName", "heroName", "subclass", "killer",
			"boss", "clericAvailable", "clericRuns", "clericWins", "clericDeaths" ) ) );

	private static final String[] BOSSES = { "Goo", "Tengu", "DM-300", "the Dwarf King", "Yog-Dzewa" };

	private static Context inPlayContext( Moment m ){
		Context c = Context.EMPTY;
		try {
			c = c.with( "depth", Dungeon.depth ).with( "hero", Dungeon.hero.heroClass.name() ).with( "heroName", Dungeon.hero.heroClass.title() );
			if (Dungeon.hero.subClass != null && Dungeon.hero.subClass != HeroSubClass.NONE) c = c.with( "subclass", Dungeon.hero.subClass.name() );
			if (m == Moment.BOSS_CLEARED && bossIndex >= 0 && bossIndex < BOSSES.length) c = c.with( "boss", BOSSES[bossIndex] );
		} catch (Throwable ignored){
		}
		return c;
	}

	// Called every frame by the game; does nothing unless the feature is on and a quiet scene has settled.
	public static void tick(){
		if (!ClericGrudge.enabled()) return;
		try {
			Scene s = Game.scene();
			long now = System.currentTimeMillis();
			if (s != lastScene){
				lastScene = s;
				sceneSince = now;
				decided = false;
			}
			if (s == null || decided || now - sceneSince < SETTLE_MS) return;
			if (s instanceof TitleScene && !startupSeen && now - sceneSince < STARTUP_SETTLE_MS) return;

			Moment m;
			Context ctx = Context.EMPTY;
			if (s instanceof TitleScene){
				//the first title screen of the process is startup; it is settled and has no window (loading and recovery are over)
				m = startupSeen ? Moment.MENU : Moment.STARTUP;
			} else if (s instanceof RankingsScene && pendingEnd != null){
				m = pendingEnd;
				ctx = pendingContext;
			} else if (s instanceof RankingsScene || s instanceof UimsdScene){
				m = Moment.STATS;
			} else if (s instanceof GameScene){
				//in play only a long, quiet stretch counts, and it is looked at without ever touching the game
				m = quietMoment( now );
				if (m == null) return;
				ctx = inPlayContext( m );
			} else {
				decided = true;
				return;
			}
			//a window already open: wait, and ask again once it is gone
			if (PixelScene.hasWindow( s )) return;
			requestHistory();
			if (historyFacts == null) return;
			ctx = ctx.withAll( historyFacts ).with( "clericAvailable", clericAvailable() ? 1 : 0 );
			decided = true;
			if (m == Moment.STARTUP) startupSeen = true;
			if (m == Moment.RUN_END_DEATH || m == Moment.RUN_END_VICTORY){
				pendingEnd = null;
				pendingContext = Context.EMPTY;
			}
			ClericGrudge g = grudge();
			Shown shown = g.popup( m, ctx, Safety.CALM, PREFS, SPDSettings.apdAdultHumor(), now );
			if (shown != null){
				if (s instanceof GameScene) GameScene.show( WndClericGrudge.popup( shown.text ) );
				else s.addToFront( WndClericGrudge.popup( shown.text ) );
				//only now, with the window really up, does the line count as heard and a story move on
				g.confirm( shown, now );
			}
		} catch (Throwable ignored){
			//the grudge may never reach play
		}
	}
}
