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

package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

//Writes the generic fact observations of a run (see FactKinds). A plain kind becomes one FACT event per observation. A SERIES kind
//(high-frequency: steps, regeneration ticks) keeps its observations in memory and writes them as one delta-encoded FACT_SERIES event:
//every observation is kept at full resolution, only the representation is compact. Pending series are written before the game is
//saved, when a floor is left and when the run ends, so a series never spans a save marker (what rollback relies on).
//
//Never throws, never touches game state or random numbers.
final class FactTracker {

	//a long stretch of observations is written in pieces so a kill loses little and no event grows without limit
	private static final int FLUSH_ROWS = 512;

	private final RunLogWriter w;
	private final Map<String, Series> series = new LinkedHashMap<>();

	FactTracker( RunLogWriter w ){
		this.w = w;
	}

	private static final class Series {
		final FactKind kind;
		final String subject, actor;
		final String[] tags;
		long[] clocks = new long[64];
		long[][] rows = new long[64][];
		int count;

		Series( FactKind kind, String subject, String actor, String[] tags ){
			this.kind = kind;
			this.subject = subject;
			this.actor = actor;
			this.tags = tags;
		}

		void add( long clock, long[] values ){
			if (count == clocks.length){
				clocks = Arrays.copyOf(clocks, count * 2);
				rows = Arrays.copyOf(rows, count * 2);
			}
			clocks[count] = clock;
			rows[count] = values;
			count++;
		}
	}

	//the number of values and tags a kind declares is what is stored: extra ones are cut, missing ones are filled, so a careless call
	//can never produce a payload the reader would take for a different shape
	private static long[] fitValues( FactKind k, long[] v ){
		int n = k.values().size();
		if (v != null && v.length == n) return v;
		long[] out = new long[n];
		if (v != null) System.arraycopy(v, 0, out, 0, Math.min(n, v.length));
		return out;
	}

	private static String[] fitTags( FactKind k, String[] t ){
		int n = k.tags().size();
		if (t != null && t.length == n) return t;
		String[] out = new String[n];
		for (int i = 0; i < n; i++) out[i] = t != null && i < t.length && t[i] != null ? t[i] : "";
		return out;
	}

	synchronized void record( FactKind k, long clock, String subject, String actor, long[] values, String[] tags ){
		if (k == null) return;
		long[] v = fitValues(k, values);
		String[] t = fitTags(k, tags);
		String subj = subject == null ? "" : subject;
		String act = actor == null ? "" : actor;
		if (!k.series){
			FactEvents.emit(w, k.eventType(), clock, new FactEvents.Observation(k.key, subj, act, clock, v, t));
			return;
		}
		StringBuilder sb = new StringBuilder(k.key).append('\u0000').append(subj).append('\u0000').append(act);
		for (String x : t) sb.append('\u0001').append(x);
		String sig = sb.toString();
		Series s = series.get(sig);
		if (s == null){
			s = new Series(k, subj, act, t);
			series.put(sig, s);
		}
		s.add(clock, v);
		if (s.count >= FLUSH_ROWS) flush(s);
	}

	private synchronized void flush( Series s ){
		if (s.count == 0) return;
		FactEvents.emitSeries(w, s.kind.eventType(), s.kind.key, s.subject, s.actor, s.tags, s.kind.values().size(), s.clocks, s.rows, s.count);
		s.count = 0;
		Arrays.fill(s.rows, null);
	}

	//write everything pending, in the order the series were started
	synchronized void flushAll(){
		try {
			for (Series s : series.values()) flush(s);
		} catch (Throwable ignored){
		}
	}
}
