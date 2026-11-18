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

//The fixed start of every run log. It records which telemetry version recorded the run, and never changes.
public final class LogHeader {

	//a log whose writer-build text starts with this is a copy made for an archive of a run that was not finished, with the hidden
	//information (and the run start's seed) taken out
	public static final String REDACTED_PREFIX = "REDACTED:";

	public final int containerFormat;
	public final TelemetryVersion telemetryVersion;
	public final long createdAtMillis;
	public final String runId;
	//free text provenance, e.g. the APD version string that wrote the file
	public final String writerBuild;
	//number of bytes the header occupies at the start of the file
	public final int length;

	LogHeader( int containerFormat, TelemetryVersion telemetryVersion, long createdAtMillis, String runId,
	           String writerBuild, int length ){
		this.containerFormat = containerFormat;
		this.telemetryVersion = telemetryVersion;
		this.createdAtMillis = createdAtMillis;
		this.runId = runId;
		this.writerBuild = writerBuild;
		this.length = length;
	}

	public static byte[] encode( TelemetryVersion version, long createdAtMillis, String runId, String writerBuild ){
		if (createdAtMillis < 0) createdAtMillis = 0;
		ByteSink body = new ByteSink();
		body.writeUnsigned(version.major);
		body.writeUnsigned(version.minor);
		body.writeUnsigned(createdAtMillis);
		body.writeString(runId);
		body.writeString(writerBuild == null ? "" : writerBuild);
		if (body.size() > LogFormat.MAX_HEADER_BODY) throw new FormatException("header too large");

		ByteSink out = new ByteSink(body.size() + 16);
		out.writeBytes(LogFormat.MAGIC);
		out.writeByte(LogFormat.CONTAINER_FORMAT);
		out.writeByte(body.size() >>> 8);
		out.writeByte(body.size());
		out.writeBytes(body.toByteArray());
		//CRC covers everything after the magic
		out.writeInt32((int) LogFormat.crc(out.rawBuffer(), LogFormat.MAGIC.length, out.size() - LogFormat.MAGIC.length));
		return out.toByteArray();
	}

	//returns the container format byte if the data starts with the magic, otherwise -1
	public static int peekContainerFormat( byte[] data ){
		if (data.length < LogFormat.MAGIC.length + 1) return -1;
		for (int i = 0; i < LogFormat.MAGIC.length; i++){
			if (data[i] != LogFormat.MAGIC[i]) return -1;
		}
		return data[LogFormat.MAGIC.length] & 0xFF;
	}

	//Parses a header whose container format this build supports. Throws FormatException if it is damaged.
	public static LogHeader parse( byte[] data ){
		int fmt = peekContainerFormat(data);
		if (fmt < 0) throw new FormatException("not an APD log");
		ByteSource src = new ByteSource(data, LogFormat.MAGIC.length + 1, data.length - LogFormat.MAGIC.length - 1);
		int bodyLength = (src.readByte() << 8) | src.readByte();
		if (bodyLength > LogFormat.MAX_HEADER_BODY) throw new FormatException("header body too large");
		int bodyStart = src.position();
		byte[] body = src.readBytes(bodyLength);
		long stored = src.readInt32() & 0xFFFFFFFFL;
		int crcStart = LogFormat.MAGIC.length;
		int crcLength = bodyStart + bodyLength - crcStart;
		if (LogFormat.crc(data, crcStart, crcLength) != stored) throw new FormatException("header checksum mismatch");

		ByteSource b = new ByteSource(body);
		int major = b.readUnsignedInt();
		int minor = b.readUnsignedInt();
		long created = b.readUnsigned();
		String runId = b.readString();
		String build = b.readString();
		if (major > 0xFFFF || minor > 0xFFFF) throw new FormatException("bad telemetry version");
		return new LogHeader(fmt, new TelemetryVersion(major, minor), created, runId, build, src.position());
	}
}
