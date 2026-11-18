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

//Builds one event and hands it to the writer. Obtained from RunLogWriter.event(type).
//
//Nothing here ever throws: if the writer is disabled or something goes wrong, the builder quietly does nothing and
//emit() returns false. A statistics problem must never reach gameplay code.
//
//Fields are written in call order and must be read back in the same order with PayloadReader.
//Strings go through the log's string table (each distinct string is stored once per log).
public final class EventBuilder {

	private final RunLogWriter writer;
	private final int type;
	private final ByteSink payload;
	//Strings are not given table IDs while the event is being built. Where each one goes in the payload is remembered
	//and the writer fills in the IDs when (and only if) the event is accepted, so a dropped event leaves no trace
	//in the string table.
	private int[] stringCuts = new int[4];
	private String[] strings = new String[4];
	private int stringCount;
	private int flags;
	private long clock;
	private boolean hasClock;
	private boolean inert;

	EventBuilder( RunLogWriter writer, int type, boolean inert ){
		this.writer = writer;
		this.type = type;
		this.inert = inert;
		this.payload = inert ? null : new ByteSink();
	}

	public EventBuilder flags( int flags ){
		this.flags = flags;
		return this;
	}

	//the absolute clock of the event. If never called, the event gets the previous event's clock.
	public EventBuilder clock( long clock ){
		this.clock = clock;
		this.hasClock = true;
		return this;
	}

	public EventBuilder unsigned( long v ){
		if (!inert){
			if (v < 0) problem("negative value in unsigned()", null);
			else payload.writeUnsigned(v);
		}
		return this;
	}

	public EventBuilder signed( long v ){
		if (!inert) payload.writeSigned(v);
		return this;
	}

	public EventBuilder bool( boolean v ){
		if (!inert) payload.writeBoolean(v);
		return this;
	}

	public EventBuilder f32( float v ){
		if (!inert) payload.writeFloat(v);
		return this;
	}

	public EventBuilder f64( double v ){
		if (!inert) payload.writeDouble(v);
		return this;
	}

	public EventBuilder bytes( byte[] v ){
		if (!inert){
			if (v == null) v = new byte[0];
			payload.writeUnsigned(v.length);
			payload.writeBytes(v);
		}
		return this;
	}

	//null is stored as the empty string
	public EventBuilder string( String v ){
		if (!inert){
			if (stringCount == strings.length){
				stringCuts = java.util.Arrays.copyOf(stringCuts, stringCount * 2);
				strings = java.util.Arrays.copyOf(strings, stringCount * 2);
			}
			stringCuts[stringCount] = payload.size();
			strings[stringCount] = v == null ? "" : v;
			stringCount++;
		}
		return this;
	}

	//Sends the event to the background writer. Returns true if it was accepted, false if it was dropped
	//(writer disabled, closed, queue full). A dropped event is counted and a GAP event is written later.
	public boolean emit(){
		if (inert) return false;
		inert = true; //a builder is single use
		try {
			return writer.accept(type, flags, hasClock, clock, payload.toByteArray(), stringCuts, strings, stringCount);
		} catch (Throwable t){
			writer.reportFailure("emit()", t);
			return false;
		}
	}

	//a mistake in how the event was built: this one event is abandoned, recording carries on
	private void problem( String what, Throwable t ){
		inert = true;
		writer.reportEventProblem(what, t);
	}
}
