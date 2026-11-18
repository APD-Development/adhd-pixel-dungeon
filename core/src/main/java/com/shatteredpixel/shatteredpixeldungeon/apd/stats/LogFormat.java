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

import java.util.zip.CRC32;

//Byte layout of a run log ("container format 1"). Documented in docs/APD-STATS-STORAGE.md.
//
//  file   = header frame*
//  header = "APDL" | containerFormat u8 | bodyLength u16 | body | crc32 u32
//  body   = telemetryMajor | telemetryMinor | createdAtMillis | runId | writerBuild | (extra bytes, ignored)
//  frame  = 0xA7 | frameType u8 | payloadLength (varint) | payload | crc32 u32
//
// The frame CRC covers frameType, payloadLength and payload. Every frame is self-contained and length-prefixed,
// so a reader can skip frame types it does not know and can stop (or resynchronise) at damage.
// Integers u16/u32 are big endian, "varint" is LEB128.
public final class LogFormat {

	private LogFormat(){}

	public static final byte[] MAGIC = { 'A', 'P', 'D', 'L' };

	//describes only the byte layout, not what events mean (that is TelemetryVersion)
	public static final int CONTAINER_FORMAT = 1;

	public static final int SYNC = 0xA7;

	//frame types. Types this build does not know are skipped by readers, so new ones can be added later.
	public static final int FRAME_STRING = 1; //defines one string table entry
	public static final int FRAME_EVENT  = 2; //one event
	public static final int FRAME_SEAL   = 3; //the run ended cleanly, the log is complete
	public static final int FRAME_CLOCK  = 4; //absolute clock value, so a reader that lost frames can re-anchor

	public static final int MAX_FRAME_PAYLOAD = 16 * 1024 * 1024;
	public static final int MAX_HEADER_BODY = 4096;
	public static final int MAX_RUN_ID_LENGTH = 64;

	static long crc( byte[] data, int offset, int length ){
		CRC32 crc = new CRC32();
		crc.update(data, offset, length);
		return crc.getValue();
	}

	//builds a complete frame
	public static byte[] frame( int frameType, byte[] payload ){
		ByteSink out = new ByteSink(payload.length + 16);
		out.writeByte(SYNC);
		out.writeByte(frameType);
		out.writeUnsigned(payload.length);
		out.writeBytes(payload);
		//CRC over everything after the sync byte
		long crc = crc(out.rawBuffer(), 1, out.size() - 1);
		out.writeInt32((int) crc);
		return out.toByteArray();
	}

	public static byte[] eventFrame( int eventType, int flags, long clockDelta, byte[] payload ){
		ByteSink body = new ByteSink(payload.length + 16);
		body.writeUnsigned(eventType);
		body.writeUnsigned(flags);
		body.writeSigned(clockDelta);
		body.writeBytes(payload);
		return frame(FRAME_EVENT, body.toByteArray());
	}

	public static byte[] stringFrame( int id, String value ){
		ByteSink body = new ByteSink();
		body.writeUnsigned(id);
		body.writeBytes(value.getBytes(Utf8.CHARSET));
		return frame(FRAME_STRING, body.toByteArray());
	}

	public static byte[] clockFrame( long clock ){
		ByteSink body = new ByteSink();
		body.writeSigned(clock);
		return frame(FRAME_CLOCK, body.toByteArray());
	}

	public static byte[] sealFrame( long eventCount, long lastClock ){
		ByteSink body = new ByteSink();
		body.writeUnsigned(eventCount);
		body.writeSigned(lastClock);
		return frame(FRAME_SEAL, body.toByteArray());
	}
}
