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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

// Reads the real shipped packs (the tests run in the assets folder) and builds small packs of our own.
final class HumorTestSupport {

	private HumorTestSupport(){}

	static final ClericGrudge.PackSource SHIPPED = new ClericGrudge.PackSource(){
		@Override
		public List<String> names() throws IOException {
			List<String> names = new ArrayList<>();
			for (String n : read( "packs.txt" ).split( "\r?\n" )) if (!n.trim().isEmpty()) names.add( n.trim() );
			return names;
		}

		@Override
		public String read( String name ) throws IOException {
			return new String( Files.readAllBytes( new File( "apd/humor/" + name ).toPath() ), StandardCharsets.UTF_8 );
		}
	};

	// preferences kept in a plain map (a stand-in for the app settings)
	static final class MapStore implements GrudgePrefs.Store {
		final java.util.Map<String, String> map = new java.util.HashMap<>();
		@Override public String get( String key ){ String v = map.get( key ); return v == null ? "" : v; }
		@Override public void put( String key, String value ){ map.put( key, value ); }
	}

	static GrudgePrefs prefs( Frequency f ){
		GrudgePrefs p = new GrudgePrefs( new MapStore() );
		p.setFrequency( f );
		return p;
	}

	static GrudgePrefs prefsOnly( Frequency f, Location... on ){
		GrudgePrefs p = prefs( f );
		for (Location l : Location.values()) p.setEnabled( l, false );
		for (Location l : on) p.setEnabled( l, true );
		return p;
	}

	// ask for a surprise and, if one is offered, show it (confirm), as the game does
	static Shown popupAndShow( ClericGrudge g, Moment m, Context c, Safety s, GrudgePrefs p, boolean adult, long now ){
		Shown shown = g.popup( m, c, s, p, adult, now );
		if (shown != null) g.confirm( shown, now );
		return shown;
	}

	static List<Line> shipped(){
		return ClericGrudge.loadPacks( SHIPPED );
	}

	// n generated lines in one category, ids <prefix>.0001...
	static List<Line> make( String prefix, String category, int n, String requires, String since ){
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= n; i++){
			sb.append( String.format( "%s.%04d | %s | since=%s | %s | 10 | Cleric complaint number %d. | Cleric adult complaint number %d.\n", prefix, i, category, since, requires, i, i ) );
		}
		return LinePack.parse( sb.toString(), "test" );
	}
}
