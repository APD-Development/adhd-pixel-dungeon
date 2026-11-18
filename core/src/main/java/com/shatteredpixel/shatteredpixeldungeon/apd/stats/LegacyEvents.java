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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Encoders and decoders for the legacy import events of telemetry 1.5, and RANKING_LINK. Pure data, no game classes.
//
//A legacy record is what APD's own rankings file kept about a finished run ("legacy" is only the name of the import path): the rankings entry and the statistics saved with it.
//Fields are written one by one as (key, kind, value). A field the old record did not contain is written with kind
//UNKNOWN, so a reader can tell "the old game never recorded this" from "the value is zero". Keys are free text so a newer
//importer can add fields without changing the format.
public final class LegacyEvents {

	private LegacyEvents(){}

	public static final int KIND_UNKNOWN = 0, KIND_NUM = 1, KIND_TEXT = 2;

	//one imported field: a null value means unknown
	public static final class Value {
		public final int kind;
		public final long num;
		public final String text;

		private Value( int kind, long num, String text ){
			this.kind = kind;
			this.num = num;
			this.text = text;
		}

		public static final Value UNKNOWN = new Value(KIND_UNKNOWN, 0, "");

		public static Value num( long v ){
			return new Value(KIND_NUM, v, "");
		}

		public static Value text( String v ){
			return new Value(KIND_TEXT, 0, v == null ? "" : v);
		}

		public boolean isKnown(){
			return kind != KIND_UNKNOWN;
		}

		@Override
		public boolean equals( Object o ){
			if (!(o instanceof Value)) return false;
			Value v = (Value) o;
			return kind == v.kind && num == v.num && text.equals(v.text);
		}

		@Override
		public int hashCode(){
			return kind * 31 + (int) num + text.hashCode();
		}
	}

	//---- LEGACY_RECORD ---------------------------------------------------------------------------------------

	public static final class Record {
		public String gameId = "";        //the id SPD gave the ranking record
		public String source = "";        //where it was read from ("rankings.dat")
		public long importedAtMillis;
		public final Map<String, Value> fields = new LinkedHashMap<>();

		public void put( String key, Value v ){
			fields.put(key, v);
		}

		public void putNum( String key, long v ){
			fields.put(key, Value.num(v));
		}

		public void putText( String key, String v ){
			fields.put(key, Value.text(v));
		}

		public void putUnknown( String key ){
			fields.put(key, Value.UNKNOWN);
		}

		public Value get( String key ){
			Value v = fields.get(key);
			return v == null ? Value.UNKNOWN : v;
		}
	}

	private static void putFields( EventBuilder b, Map<String, Value> fields ){
		b.unsigned(fields.size());
		for (Map.Entry<String, Value> e : fields.entrySet()){
			Value v = e.getValue();
			b.string(e.getKey()).unsigned(v.kind);
			if (v.kind == KIND_NUM) b.signed(v.num);
			else if (v.kind == KIND_TEXT) b.string(v.text);
		}
	}

	private static void readFields( PayloadReader p, Map<String, Value> out ){
		int n = p.unsignedInt();
		for (int i = 0; i < n; i++){
			String key = str(p.string());
			int kind = p.unsignedInt();
			if (kind == KIND_NUM) out.put(key, Value.num(p.signed()));
			else if (kind == KIND_TEXT) out.put(key, Value.text(str(p.string())));
			else out.put(key, Value.UNKNOWN);
		}
	}

	private static String str( String s ){
		return s == null ? "" : s;
	}

	public static boolean emitRecord( RunLogWriter w, long clock, Record r ){
		EventBuilder b = w.event(EventTypes.LEGACY_RECORD).clock(clock).string(r.gameId).string(r.source)
				.unsigned(Math.max(0, r.importedAtMillis));
		putFields(b, r.fields);
		return b.emit();
	}

	public static Record decodeRecord( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Record r = new Record();
		r.gameId = str(p.string());
		r.source = str(p.string());
		r.importedAtMillis = p.unsigned();
		readFields(p, r.fields);
		return r;
	}

	//---- LEGACY_TOTALS ---------------------------------------------------------------------------------------

	//SPD's own counters of games played and won (they keep counting runs whose records were dropped from the table of 11)
	public static final class Totals {
		public String source = "";
		public long importedAtMillis;
		public final Map<String, Value> fields = new LinkedHashMap<>();
	}

	public static boolean emitTotals( RunLogWriter w, long clock, Totals t ){
		EventBuilder b = w.event(EventTypes.LEGACY_TOTALS).clock(clock).string(t.source).unsigned(Math.max(0, t.importedAtMillis));
		putFields(b, t.fields);
		return b.emit();
	}

	public static Totals decodeTotals( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Totals t = new Totals();
		t.source = str(p.string());
		t.importedAtMillis = p.unsigned();
		readFields(p, t.fields);
		return t;
	}

	//---- LEGACY_DAILY_SCORE ----------------------------------------------------------------------------------

	public static final class DailyScore {
		public long day;     //the daily's date key as SPD stores it
		public long score;
		public long importedAtMillis;
	}

	public static boolean emitDailyScore( RunLogWriter w, long clock, DailyScore d ){
		return w.event(EventTypes.LEGACY_DAILY_SCORE).clock(clock).signed(d.day).signed(d.score)
				.unsigned(Math.max(0, d.importedAtMillis)).emit();
	}

	public static DailyScore decodeDailyScore( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		DailyScore d = new DailyScore();
		d.day = p.signed();
		d.score = p.signed();
		d.importedAtMillis = p.unsigned();
		return d;
	}

	//---- RANKING_LINK (in a run log) ---------------------------------------------------------------------------

	//The ranking record SPD wrote for this run, so the run and its old-style record can be recognised as the same run.
	public static final class RankingLink {
		public String gameId = "";
		public boolean daily;
		public boolean dailyReplay;   //replays are not kept in rankings.dat
	}

	public static boolean emitRankingLink( RunLogWriter w, long clock, RankingLink l ){
		return w.event(EventTypes.RANKING_LINK).clock(clock).string(l.gameId).bool(l.daily).bool(l.dailyReplay).emit();
	}

	public static RankingLink decodeRankingLink( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		RankingLink l = new RankingLink();
		l.gameId = str(p.string());
		l.daily = p.bool();
		l.dailyReplay = p.bool();
		return l;
	}

	//handy for tests and tools
	public static List<String> keys( Map<String, Value> fields ){
		return new ArrayList<>(fields.keySet());
	}
}
