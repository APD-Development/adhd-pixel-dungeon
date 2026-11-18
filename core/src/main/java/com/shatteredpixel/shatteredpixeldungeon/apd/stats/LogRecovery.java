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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

//Cuts a damaged tail off a run log so it ends on its last valid frame again.
//
//The cut-off bytes are never silently deleted: they are first copied to "<log>.tail-<offset>.damaged" next to the log.
//Logs that are healthy, sealed, or whose header is unreadable are left untouched.
public final class LogRecovery {

	private LogRecovery(){}

	public static final class RepairReport {
		public final File file;
		public final ReadStatus statusBefore;
		public final long validBytes;
		public final long discardedBytes;
		//where the discarded bytes were saved, null if nothing was cut
		public final File savedTail;
		public final boolean truncated;
		//set if repair itself failed (the log is then left as it was)
		public final String error;

		RepairReport( File file, ReadStatus statusBefore, long validBytes, long discardedBytes, File savedTail,
		              boolean truncated, String error ){
			this.file = file;
			this.statusBefore = statusBefore;
			this.validBytes = validBytes;
			this.discardedBytes = discardedBytes;
			this.savedTail = savedTail;
			this.truncated = truncated;
			this.error = error;
		}
	}

	//Never throws. Callers without an on-disk identity use the original repair behavior.
	public static RepairReport repair( File file ){
		return repair(file, null);
	}

	//Destructive store recovery passes its validated filename ID. The parsed header must
	//identify that same log before any damaged bytes are copied or truncated.
	static RepairReport repair( File file, String expectedRunId ){
		byte[] data;
		ReadResult r;
		try {
			data = RunLogReader.readFully(file);
			r = RunLogReader.parse(data);
		} catch (Throwable t){
			return new RepairReport(file, ReadStatus.BAD_HEADER, 0, 0, null, false, "could not read: " + t);
		}

		if (expectedRunId != null && r.header != null && !expectedRunId.equals(r.header.runId)){
			return new RepairReport(file, r.status, r.validBytes, 0, null, false,
					"run log header ID differs from on-disk filename");
		}

		boolean damaged = r.status == ReadStatus.TRUNCATED || r.status == ReadStatus.CORRUPT
				|| (r.sealed && r.status == ReadStatus.SEALED && r.damagedBytes() > 0);
		if (!damaged || r.damagedBytes() == 0){
			return new RepairReport(file, r.status, r.validBytes, 0, null, false, null);
		}

		long valid = r.validBytes;
		int discarded = (int) (data.length - valid);
		File tail = uniqueTailFile(file, valid);
		try {
			FileOutputStream out = new FileOutputStream(tail);
			try {
				out.write(data, (int) valid, discarded);
				out.getFD().sync();
			} finally {
				out.close();
			}
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			try {
				raf.setLength(valid);
				raf.getFD().sync();
			} finally {
				raf.close();
			}
		} catch (Throwable t){
			return new RepairReport(file, r.status, valid, 0, null, false, "repair failed: " + t);
		}
		return new RepairReport(file, r.status, valid, discarded, tail, true, null);
	}

	private static File uniqueTailFile( File file, long offset ){
		File f = new File(file.getPath() + ".tail-" + offset + ".damaged");
		int n = 1;
		while (f.exists()){
			f = new File(file.getPath() + ".tail-" + offset + "-" + (n++) + ".damaged");
		}
		return f;
	}
}
