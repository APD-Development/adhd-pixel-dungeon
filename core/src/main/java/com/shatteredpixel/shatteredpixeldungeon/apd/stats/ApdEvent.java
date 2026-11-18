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

//One decoded event. The payload is opaque to the storage library, its meaning belongs to the event type.
public final class ApdEvent {

	//position among all event frames of the log, starting at 0
	public final long index;
	public final int type;
	public final int flags;
	//absolute clock value (what the clock measures is decided by the telemetry version, later phases use game time)
	public final long clock;
	public final byte[] payload;

	public ApdEvent( long index, int type, int flags, long clock, byte[] payload ){
		this.index = index;
		this.type = type;
		this.flags = flags;
		this.clock = clock;
		this.payload = payload;
	}

	public PayloadReader reader( StringLookup strings ){
		return new PayloadReader(new ByteSource(payload), strings);
	}

	@Override
	public String toString(){
		return "#" + index + " " + EventTypes.name(type) + " t=" + clock + " flags=" + flags + " payload=" + payload.length + "B";
	}
}
