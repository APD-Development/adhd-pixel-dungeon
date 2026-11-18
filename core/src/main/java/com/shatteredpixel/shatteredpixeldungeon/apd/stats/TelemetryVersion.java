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


package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

//The version of APD's telemetry/statistics recording system itself, written as "Telemetry major.minor".
//
//It is completely independent of the APD game version. It describes what a recorded run contains and how to
//interpret it: which event types and fields existed when the run was recorded. A run keeps the telemetry version that
//recorded it for ever (it is stored in the log header). Readers may derive newer representations from old data, but
//they never rewrite or relabel the original version.
//
//minor: additive changes only (new event types, new optional fields). Old data keeps its meaning.
//major: anything that changes the meaning of existing data. A reader older than a log's major version can still
//       read the file frames, but must not claim to understand the events.
//
//This is separate from LogFormat.CONTAINER_FORMAT, which only describes the byte layout of the file.
public final class TelemetryVersion implements Comparable<TelemetryVersion> {

	//the version this build of APD records new runs with
	public static final TelemetryVersion CURRENT = new TelemetryVersion(1, 16);

	public final int major;
	public final int minor;

	public TelemetryVersion( int major, int minor ){
		if (major < 0 || minor < 0 || major > 0xFFFF || minor > 0xFFFF){
			throw new IllegalArgumentException("telemetry version out of range: " + major + "." + minor);
		}
		this.major = major;
		this.minor = minor;
	}

	//parses "1.0", "1.3", ...
	public static TelemetryVersion parse( String text ){
		if (text == null) throw new IllegalArgumentException("null telemetry version");
		String t = text.trim();
		if (t.startsWith("telemetry")) t = t.substring("telemetry".length()).trim();
		int dot = t.indexOf('.');
		if (dot <= 0 || dot == t.length() - 1) throw new IllegalArgumentException("bad telemetry version: " + text);
		try {
			return new TelemetryVersion(Integer.parseInt(t.substring(0, dot)), Integer.parseInt(t.substring(dot + 1)));
		} catch (NumberFormatException e){
			throw new IllegalArgumentException("bad telemetry version: " + text);
		}
	}

	//true if a reader that implements 'reader' cannot be expected to understand data recorded with this version
	public boolean isNewerThan( TelemetryVersion reader ){
		return compareTo(reader) > 0;
	}

	//true if this version's events may have a different meaning than the reader's (different major)
	public boolean isIncompatibleMajor( TelemetryVersion reader ){
		return major != reader.major;
	}

	@Override
	public int compareTo( TelemetryVersion o ){
		if (major != o.major) return major < o.major ? -1 : 1;
		if (minor != o.minor) return minor < o.minor ? -1 : 1;
		return 0;
	}

	@Override
	public boolean equals( Object o ){
		return o instanceof TelemetryVersion && compareTo((TelemetryVersion) o) == 0;
	}

	@Override
	public int hashCode(){
		return major * 65537 + minor;
	}

	@Override
	public String toString(){
		return major + "." + minor;
	}
}

