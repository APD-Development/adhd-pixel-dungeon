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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

// The Developer's Personal Vendetta Against Cleric: offline flavor text, nothing else. It decides what to say and when it may be said; the
// caller shows it. It reads no game state itself (the caller passes the recorded facts and whether the moment is quiet), uses its own random
// source, and writes only its own ledger file. Cleric is its only subject.
//
// Two kinds of appearance, both separate from play:
//   popup()           a surprise, at a quiet moment in a place the player switched on; at most one per session, rare, off if the player says so
//   firstClericRun()  the one-time warning when the player picks Cleric to start a run
// Choosing a line never uses it up; the game confirms() once the player was really shown it.
public final class ClericGrudge {

	// The owner's switch: off in the normal build; only the private test build turns it on (ApdFeatures).
	public static boolean enabled(){
		return com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.grudge();
	}

	// nothing in the first twenty minutes after the feature first sees the player
	static final long GRACE_MS = 20L * 60 * 1000;

	static final String ST_INSTALL = "installAt", ST_LAST_POPUP = "lastPopupAt",
			ST_LAST_CATEGORY = "lastCategory", ST_FIRST_RUN = "firstRunShown", ST_COUNTER = "counterShown";

	public interface PackSource {
		List<String> names() throws IOException;
		String read( String name ) throws IOException;
	}

	private final List<Line> lines;
	private final Ledger ledger;
	private final Random rng;
	private final boolean enabled;
	private final long sessionStart;
	private boolean popupThisSession;
	//this session's one roll: null until the grace period and gap allow it, then whether this session gets a popup at all
	private Boolean armed;

	public ClericGrudge( List<Line> lines, Ledger ledger, Random rng, boolean enabled ){
		this( lines, ledger, rng, enabled, System.currentTimeMillis() );
	}

	public ClericGrudge( List<Line> lines, Ledger ledger, Random rng, boolean enabled, long sessionStart ){
		this.lines = lines;
		this.ledger = ledger;
		this.rng = rng;
		this.enabled = enabled;
		this.sessionStart = sessionStart;
	}

	// Loads every pack the source lists. A pack that fails its lint is refused whole and the others still load.
	public static List<Line> loadPacks( PackSource source ){
		List<Line> all = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		try {
			for (String name : source.names()){
				try {
					List<Line> pack = LinePack.parse( source.read( name ), name );
					boolean clash = false;
					for (Line l : pack) if (ids.contains( l.id )) clash = true;
					if (clash) continue;
					for (Line l : pack) ids.add( l.id );
					all.addAll( pack );
				} catch (LinePack.PackException | IOException skip){
					//a broken pack is left out; the game goes on without it
				}
			}
		} catch (IOException e){
			//no packs: no humor
		}
		//a line whose first part is missing could never be shown, so it is not counted or kept either
		Set<String> never = LinePack.unreachable( all, new ArrayList<String>() );
		if (!never.isEmpty()){
			List<Line> kept = new ArrayList<>();
			for (Line l : all) if (!never.contains( l.id )) kept.add( l );
			all = kept;
		}
		return all;
	}

	public Counter counter(){
		Set<String> hatred = new HashSet<>(), appreciation = new HashSet<>();
		for (Line l : lines){
			if (!l.counted) continue;
			(l.pool == Pool.APPRECIATION ? appreciation : hatred).add( l.id );
		}
		return new Counter( hatred.size(), ledger.distinctSeenAmong( hatred ), appreciation.size(), ledger.distinctSeenAmong( appreciation ) );
	}

	// how many counted lines each content version introduced, per pool (for truthful patch notes)
	public java.util.Map<Integer, int[]> addedByVersion(){
		java.util.Map<Integer, int[]> m = new java.util.TreeMap<>();
		for (Line l : lines){
			if (!l.counted) continue;
			int[] c = m.get( l.since );
			if (c == null) m.put( l.since, c = new int[2] );
			c[l.pool == Pool.APPRECIATION ? 1 : 0]++;
		}
		return m;
	}

	// A surprise at a quiet moment, or null. All of these must hold, and they are independent of each other:
	//   - the feature is on, the frequency is not Off, and at least one location is switched on;
	//   - this moment's location is switched on, and the moment is quiet (Safety);
	//   - nothing has been shown yet this session, the first-time grace period is over, and the minimum gap since the last one has passed.
	// Whether this session gets a popup at all is decided by ONE random roll per session (after the grace period and the gap), not by one per
	// moment, so switching on more locations changes where a popup can appear, never how many sessions get one.
	// Choosing does not use the line up: the caller shows it and then calls confirm().
	public synchronized Shown popup( Moment moment, Context ctx, Safety safety, GrudgePrefs prefs, boolean adult, long now ){
		Frequency freq = prefs.frequency();
		if (!enabled || !moment.surprise || freq == Frequency.OFF || !prefs.anyLocation() || popupThisSession) return null;
		long install = ledger.stateLong( ST_INSTALL, -1 );
		if (install < 0){
			ledger.setState( ST_INSTALL, Long.toString( now ) );
			ledger.save();
			return null;
		}
		if (now - install < GRACE_MS) return null;
		long last = ledger.stateLong( ST_LAST_POPUP, -1 );
		if (last >= 0 && now - last < freq.minGapMs) return null;
		if (armed == null) armed = rng.nextDouble() < freq.chance;
		if (!armed) return null;
		Location where = moment.location();
		if (where == null || !prefs.enabled( where ) || !safety.ok()) return null;
		return choose( moment, ctx, adult, now );
	}

	// The game calls this once the player has really been shown the line (the window is up). Only now does a line count as heard, a story
	// move on, and the session's popup be used up; a line that was chosen but never shown changes nothing.
	public synchronized void confirm( Shown shown, long now ){
		ledger.markShown( shown.id, now );
		ledger.setState( ST_LAST_CATEGORY, shown.category.name() );
		if (shown.moment.surprise){
			popupThisSession = true;
			ledger.setState( ST_LAST_POPUP, Long.toString( now ) );
		} else if (shown.moment == Moment.FIRST_CLERIC_RUN){
			ledger.setState( ST_FIRST_RUN, "1" );
		} else if (shown.moment == Moment.COUNTER_SCREEN){
			ledger.setState( ST_COUNTER, "1" );
		}
		ledger.save();
	}

	// A line on request, shown and counted at once, ignoring the chance and the gap (for tests and for a page the player opens on purpose).
	synchronized Shown inlineAlways( Moment moment, Context ctx, long now ){
		if (!enabled) return null;
		Shown s = choose( moment, ctx, false, now );
		if (s != null) confirm( s, now );
		return s;
	}

	// The one-time warning when the player first starts a Cleric run. It does not depend on the surprise roll or the session limit (it is the
	// player's own choice that triggers it), but it respects the master opt-out (frequency Off) and shows only once, remembered when the
	// game confirms it was really displayed.
	public synchronized Shown firstClericRun( GrudgePrefs prefs, boolean adult, long now ){
		if (!enabled || prefs.frequency() == Frequency.OFF || ledger.state( ST_FIRST_RUN ) != null) return null;
		return choose( Moment.FIRST_CLERIC_RUN, Context.EMPTY, adult, now );
	}

	// The one-time joke the first time the counter screen is opened.
	public synchronized Shown counterDiscovery( boolean adult, long now ){
		if (!enabled || ledger.state( ST_COUNTER ) != null) return null;
		return choose( Moment.COUNTER_SCREEN, Context.EMPTY, adult, now );
	}

	private Shown choose( Moment moment, Context ctx, boolean adult, long now ){
		Category avoid = null;
		String last = ledger.state( ST_LAST_CATEGORY );
		if (last != null){
			try {
				avoid = Category.valueOf( last );
			} catch (IllegalArgumentException ignored){
			}
		}
		Counter counts = counter();
		//the grudge's own real totals are available to lines that remark on the grudge itself
		ctx = ctx.with( "complaintsHeard", counts.heard ).with( "complaintsTotal", counts.total )
				.with( "appreciationsHeard", counts.appreciationHeard ).with( "appreciationsTotal", counts.appreciationTotal );
		Line l = Picker.pick( lines, moment, ctx, ledger, rng, avoid, sessionStart, now );
		if (l == null) return null;
		String text = l.render( ctx, adult );
		if (text == null) return null;
		return new Shown( l.id, l.category, moment, text );
	}
}
