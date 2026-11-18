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

import java.util.ArrayList;
import java.util.List;

//Everything learned from reading one run log.
public final class ReadResult implements StringLookup {

	public ReadStatus status = ReadStatus.BAD_HEADER;
	//null if the header could not be read
	public LogHeader header;

	//events in file order. With ReadOptions.knownTypesOnly, events of unknown types are left out of this list.
	public final List<ApdEvent> events = new ArrayList<>();
	//string table, index = id. Entries are null where a definition was lost in a damaged log.
	public final List<String> strings = new ArrayList<>();

	//all event frames found, whether or not they are in 'events'
	public long eventCount;
	public long gapEvents;
	public long skippedUnknownFrames;
	public long skippedUnknownEvents;
	//clock of the last event found
	public long lastClock;

	public boolean sealed;
	//event count recorded in the seal frame, if sealed
	public long sealEventCount;

	//file offset just after the last good frame (or header). Bytes from here on are not trusted.
	public long validBytes;
	public long fileLength;
	//bytes after validBytes. For a damaged log this is the tail that repair would cut off.
	public long damagedBytes(){
		return Math.max(0, fileLength - validBytes);
	}

	//Events are stored as clock differences. After a resync the clock of the events up to the next clock frame
	//cannot be trusted, because the differences in the lost frames are missing. This counts those events.
	public long uncertainClockEvents;
	public boolean clockUncertain;

	//set when ReadOptions.resync was used
	public int resyncs;
	public long skippedBytes;

	//what went wrong first, for diagnostics. null if nothing did.
	public String problem;

	@Override
	public String get( int id ){
		return id >= 0 && id < strings.size() ? strings.get(id) : null;
	}

	//true if the log says it was recorded by a newer telemetry version than this build implements
	public boolean newerThanReader(){
		return header != null && header.telemetryVersion.isNewerThan(TelemetryVersion.CURRENT);
	}
}
