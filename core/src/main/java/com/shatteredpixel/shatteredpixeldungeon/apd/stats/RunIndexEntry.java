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

//One line of the run index: a summary of one run log. It is derived data, everything in it can be recomputed by
//reading the log, which stays the authority.
public final class RunIndexEntry {

	public enum Status {
		//ended with a seal frame
		SEALED(1),
		//readable, no seal: in progress, or recording stopped abruptly
		OPEN(2),
		//has a damaged or partly written tail that has not been repaired yet
		DAMAGED(3),
		//header unreadable or written by a newer container format
		UNREADABLE(4);

		public final int code;

		Status( int code ){
			this.code = code;
		}

		static Status fromCode( int code ){
			for (Status s : values()) if (s.code == code) return s;
			return UNREADABLE;
		}

		static Status of( ReadResult r ){
			switch (r.status){
				case SEALED:    return r.damagedBytes() > 0 ? DAMAGED : SEALED;
				case UNSEALED:  return OPEN;
				case BAD_HEADER:
				case UNSUPPORTED_CONTAINER:
					return UNREADABLE;
				default:        return DAMAGED;
			}
		}
	}

	public final String runId;
	//the telemetry version the run was recorded with, null if the header is unreadable
	public final TelemetryVersion telemetryVersion;
	//true if that version is newer than this build implements
	public final boolean newerThanReader;
	public final String logFile;
	public final long createdAtMillis;
	public final Status status;
	public final long eventCount;
	public final long lastClock;
	public final long validBytes;
	public final long fileLength;

	public RunIndexEntry( String runId, TelemetryVersion telemetryVersion, boolean newerThanReader, String logFile,
	                      long createdAtMillis, Status status, long eventCount, long lastClock, long validBytes,
	                      long fileLength ){
		this.runId = runId;
		this.telemetryVersion = telemetryVersion;
		this.newerThanReader = newerThanReader;
		this.logFile = logFile;
		this.createdAtMillis = createdAtMillis;
		this.status = status;
		this.eventCount = eventCount;
		this.lastClock = lastClock;
		this.validBytes = validBytes;
		this.fileLength = fileLength;
	}

	//Builds an entry by reading a log. runIdIfUnreadable is used when the header cannot be read.
	static RunIndexEntry describe( java.io.File logFile, String runIdIfUnreadable ) throws java.io.IOException {
		ReadResult r = RunLogReader.read(logFile);
		String id = r.header != null ? r.header.runId : runIdIfUnreadable;
		return new RunIndexEntry(id,
				r.header != null ? r.header.telemetryVersion : null,
				r.newerThanReader(),
				logFile.getName(),
				r.header != null ? r.header.createdAtMillis : 0,
				Status.of(r), r.eventCount, r.lastClock, r.validBytes, r.fileLength);
	}

	byte[] encode(){
		ByteSink b = new ByteSink();
		b.writeString(runId);
		b.writeBoolean(telemetryVersion != null);
		if (telemetryVersion != null){
			b.writeUnsigned(telemetryVersion.major);
			b.writeUnsigned(telemetryVersion.minor);
		}
		b.writeBoolean(newerThanReader);
		b.writeString(logFile);
		b.writeUnsigned(createdAtMillis);
		b.writeUnsigned(status.code);
		b.writeUnsigned(eventCount);
		b.writeSigned(lastClock);
		b.writeUnsigned(validBytes);
		b.writeUnsigned(fileLength);
		return b.toByteArray();
	}

	static RunIndexEntry decode( byte[] data ){
		ByteSource b = new ByteSource(data);
		String runId = b.readString();
		TelemetryVersion v = null;
		if (b.readBoolean()){
			v = new TelemetryVersion(b.readUnsignedInt(), b.readUnsignedInt());
		}
		boolean newer = b.readBoolean();
		String file = b.readString();
		long created = b.readUnsigned();
		Status st = Status.fromCode(b.readUnsignedInt());
		long events = b.readUnsigned();
		long clock = b.readSigned();
		long valid = b.readUnsigned();
		long length = b.readUnsigned();
		//fields added by later versions would follow here and are ignored by this reader
		return new RunIndexEntry(runId, v, newer, file, created, st, events, clock, valid, length);
	}

	@Override
	public boolean equals( Object o ){
		if (!(o instanceof RunIndexEntry)) return false;
		RunIndexEntry e = (RunIndexEntry) o;
		return runId.equals(e.runId)
				&& (telemetryVersion == null ? e.telemetryVersion == null : telemetryVersion.equals(e.telemetryVersion))
				&& newerThanReader == e.newerThanReader && logFile.equals(e.logFile)
				&& createdAtMillis == e.createdAtMillis && status == e.status && eventCount == e.eventCount
				&& lastClock == e.lastClock && validBytes == e.validBytes && fileLength == e.fileLength;
	}

	@Override
	public int hashCode(){
		return runId.hashCode();
	}

	@Override
	public String toString(){
		return runId + " telemetry=" + telemetryVersion + " " + status + " events=" + eventCount;
	}
}
