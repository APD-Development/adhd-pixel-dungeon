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

// The player's Cleric-grudge preferences: how often (Frequency) and where (Location). They are independent: frequency Off means nothing
// anywhere, and every location off means nothing at any frequency. The values live in whatever store the game gives (the app settings;
// a plain map in tests), under the same keys as before, so a player who already had a frequency keeps it and the new location switches
// start at their defaults.
public final class GrudgePrefs {

	public interface Store {
		// "" when unset
		String get( String key );
		void put( String key, String value );
	}

	public static final String KEY_FREQUENCY = "apd_grudge_popups";
	static final String KEY_LOCATION = "apd_grudge_loc_";

	private final Store store;

	public GrudgePrefs( Store store ){
		this.store = store;
	}

	public Frequency frequency(){
		String v = store.get( KEY_FREQUENCY );
		return v == null || v.isEmpty() ? Frequency.DEFAULT : Frequency.of( v );
	}

	public void setFrequency( Frequency f ){
		store.put( KEY_FREQUENCY, f.name() );
	}

	public boolean enabled( Location loc ){
		String v = store.get( KEY_LOCATION + loc.name().toLowerCase() );
		return v == null || v.isEmpty() ? loc.defaultOn : v.equals( "1" );
	}

	public void setEnabled( Location loc, boolean on ){
		store.put( KEY_LOCATION + loc.name().toLowerCase(), on ? "1" : "0" );
	}

	public boolean anyLocation(){
		for (Location l : Location.values()) if (enabled( l )) return true;
		return false;
	}

	// "Turn off these messages": takes effect at once and is saved
	public void turnOff(){
		setFrequency( Frequency.OFF );
	}
}
