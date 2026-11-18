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

//Encoders and decoders for the generic fact events of telemetry 1.8 (FACT, FACT_INTERNAL, FACT_SERIES, FACT_SERIES_INTERNAL). Pure data,
//no game classes. What a kind means is declared in FactKinds; these events only carry the key and the values.
//
//FACT:    key, subject, actor, number of values + signed values, number of tags + tag strings
//SERIES:  key, subject, actor, tags (shared by every row), number of values, number of rows, then one block of bytes holding the columns
//         (the clock first, then each value). A column is written as steps between rows (the first step is from zero), and a run of
//         equal steps is cheap: each column picks the smaller of two codings, steps (mode 0) or steps of steps (mode 1, a steady walk costs
//         almost nothing), and zero steps are written as run lengths. A series is exactly the observations it expands to: nothing is
//         summarised, only written compactly.
//
//Field order is part of the format. Later versions may only ADD fields at the END of a payload.
public final class FactEvents {

	private FactEvents(){}

	//one observation, as recorded or as read back
	public static final class Observation {
		public String key = "";
		public String subject = "";
		public String actor = "";
		public long clock;
		public long[] values = new long[0];
		public String[] tags = new String[0];

		public Observation(){}

		public Observation( String key, String subject, String actor, long clock, long[] values, String[] tags ){
			this.key = key == null ? "" : key;
			this.subject = subject == null ? "" : subject;
			this.actor = actor == null ? "" : actor;
			this.clock = clock;
			this.values = values == null ? new long[0] : values;
			this.tags = tags == null ? new String[0] : tags;
		}
	}

	public static boolean isFactEvent( int type ){
		return type == EventTypes.FACT || type == EventTypes.FACT_INTERNAL
				|| type == EventTypes.FACT_SERIES || type == EventTypes.FACT_SERIES_INTERNAL;
	}

	public static boolean isSeries( int type ){
		return type == EventTypes.FACT_SERIES || type == EventTypes.FACT_SERIES_INTERNAL;
	}

	//---- FACT ----

	public static boolean emit( RunLogWriter w, int eventType, long clock, Observation o ){
		EventBuilder b = w.event(eventType).clock(clock);
		b.string(o.key).string(o.subject).string(o.actor);
		b.unsigned(o.values.length);
		for (long v : o.values) b.signed(v);
		b.unsigned(o.tags.length);
		for (String t : o.tags) b.string(t);
		return b.emit();
	}

	//---- SERIES ----

	//rows[i] are the values of observation i, clocks[i] its clock (not decreasing). All rows have 'valueCount' values.
	public static boolean emitSeries( RunLogWriter w, int eventType, String key, String subject, String actor, String[] tags,
	                                  int valueCount, long[] clocks, long[][] rows, int rowCount ){
		if (rowCount <= 0) return true;
		EventBuilder b = w.event(eventType).clock(clocks[0]);
		b.string(key).string(subject).string(actor);
		b.unsigned(tags.length);
		for (String t : tags) b.string(t);
		b.unsigned(valueCount).unsigned(rowCount);
		ByteSink blob = new ByteSink(rowCount * 2 + 16);
		long[] col = new long[rowCount];
		for (int i = 0; i < rowCount; i++) col[i] = clocks[i] - clocks[0];
		writeColumn(blob, col, rowCount);
		for (int j = 0; j < valueCount; j++){
			for (int i = 0; i < rowCount; i++) col[i] = rows[i][j];
			writeColumn(blob, col, rowCount);
		}
		b.bytes(blob.toByteArray());
		return b.emit();
	}

	//---- reading ----

	//The observations an event holds: one for a FACT, one per row for a SERIES. Throws FormatException on a payload that cannot be read
	//(the reader skips such an event, it contributes nothing).
	public static List<Observation> decode( ApdEvent e, StringLookup strings ){
		List<Observation> out = new ArrayList<>();
		PayloadReader p = e.reader(strings);
		if (!isSeries(e.type)){
			Observation o = new Observation();
			o.key = str(p.string());
			o.subject = str(p.string());
			o.actor = str(p.string());
			o.clock = e.clock;
			int nv = p.unsignedInt();
			limit(nv, MAX_FIELDS);
			o.values = new long[nv];
			for (int i = 0; i < nv; i++) o.values[i] = p.signed();
			int nt = p.unsignedInt();
			limit(nt, MAX_FIELDS);
			o.tags = new String[nt];
			for (int i = 0; i < nt; i++) o.tags[i] = str(p.string());
			out.add(o);
			return out;
		}
		String key = str(p.string());
		String subject = str(p.string());
		String actor = str(p.string());
		int nt = p.unsignedInt();
		limit(nt, MAX_FIELDS);
		String[] tags = new String[nt];
		for (int i = 0; i < nt; i++) tags[i] = str(p.string());
		int nv = p.unsignedInt();
		int rows = p.unsignedInt();
		limit(nv, MAX_FIELDS);
		limit(rows, MAX_ROWS);
		ByteSource blob = new ByteSource(p.bytes());
		long[] clocks = readColumn(blob, rows);
		long[][] cols = new long[nv][];
		for (int j = 0; j < nv; j++) cols[j] = readColumn(blob, rows);
		for (int i = 0; i < rows; i++){
			long[] v = new long[nv];
			for (int j = 0; j < nv; j++) v[j] = cols[j][i];
			out.add(new Observation(key, subject, actor, e.clock + clocks[i], v, tags));
		}
		return out;
	}

	// ---- columns ----

	//mode 0: steps between rows. mode 1: steps of the steps. Zero steps are written as run lengths, so a column that rarely changes costs a few bytes.
	private static void writeColumn( ByteSink out, long[] v, int n ){
		long[] steps = new long[n], second = new long[n];
		long prev = 0, prevStep = 0;
		for (int i = 0; i < n; i++){
			steps[i] = v[i] - prev;
			second[i] = steps[i] - prevStep;
			prev = v[i];
			prevStep = steps[i];
		}
		ByteSink a = new ByteSink(n + 8), b = new ByteSink(n + 8);
		writeZeroRuns(a, steps, n);
		writeZeroRuns(b, second, n);
		if (b.size() < a.size()){
			out.writeByte(1);
			out.writeBytes(b.toByteArray());
		} else {
			out.writeByte(0);
			out.writeBytes(a.toByteArray());
		}
	}

	private static void writeZeroRuns( ByteSink out, long[] s, int n ){
		int i = 0;
		while (i < n){
			int run = 0;
			while (i + run < n && s[i + run] == 0) run++;
			out.writeUnsigned(run);
			i += run;
			if (i < n){
				out.writeSigned(s[i]);
				i++;
			}
		}
	}

	private static long[] readColumn( ByteSource in, int n ){
		int mode = in.readByte();
		if (mode != 0 && mode != 1) throw new FormatException("unknown fact column coding " + mode);
		long[] s = new long[n];
		int i = 0;
		while (i < n){
			long run = in.readUnsigned();
			if (run < 0 || run > n - i) throw new FormatException("fact column run out of range");
			i += (int) run;
			if (i < n) s[i++] = in.readSigned();
		}
		// Reconstruct in the existing buffer; each later encoded step is still untouched.
		long prev = 0, step = 0;
		for (int k = 0; k < n; k++){
			step = mode == 1 ? step + s[k] : s[k];
			prev += step;
			s[k] = prev;
		}
		return s;
	}

	//a damaged payload must not make the reader allocate without limit
	private static final int MAX_FIELDS = 256, MAX_ROWS = 1 << 20;

	private static void limit( int n, int max ){
		if (n < 0 || n > max) throw new FormatException("fact payload count out of range: " + n);
	}

	private static String str( String s ){
		return s == null ? "" : s;
	}
}
