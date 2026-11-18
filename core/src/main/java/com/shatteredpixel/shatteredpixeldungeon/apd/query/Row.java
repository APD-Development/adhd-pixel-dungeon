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


package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

//One row of a dataset. Values are stored compactly (a few bytes per cell) so a large history stays cheap: a number is a
//long, a text is a string, and a byte per cell says which of them is set and where the value came from. A cell that is
//not set is UNKNOWN. It is never zero.
public final class Row {

	public static final int K_UNKNOWN = 0, K_NUM = 1, K_TEXT = 2;

	//the ruleset of a run whose rules changed while it was being played: it belongs to no single ruleset
	public static final String RULESET_MIXED = "mixed";

	public final String id;
	//rows with the same merge key were recorded in several log segments of one run and are combined, null: never merged
	public final String mergeKey;
	//the run this row belongs to, empty if none (imported ranking records)
	public final String runId;
	//the run is permanently finished (its newest log segment has ended). Hidden information of unfinished runs is never shown.
	public boolean finished;
	//the player legitimately knew about this row (an item they saw, an enemy they met). Rows that exist only in hidden
	//information are not public rows.
	public boolean publicRow = true;
	//null: unknown, RULESET_MIXED: changed during the run
	public String ruleset;
	//an imported ranking record that is the same run as a recorded run: counted once, via the recorded run
	public boolean duplicate;
	//imported ranking record (not a recorded run)
	public boolean imported;
	//the telemetry version of the log this row came from, "" if none
	public String telemetry = "";

	private final long[] nums;
	private final String[] texts;
	private final byte[] meta;

	public Row( String id, String mergeKey, String runId, int columns ){
		this.id = id;
		this.mergeKey = mergeKey;
		this.runId = runId == null ? "" : runId;
		this.nums = new long[columns];
		this.texts = new String[columns];
		this.meta = new byte[columns];
	}

	public int size(){
		return meta.length;
	}

	public Row setNum( int col, long value, int prov ){
		nums[col] = value;
		texts[col] = null;
		meta[col] = (byte) (K_NUM | (prov << 2));
		return this;
	}

	public Row setText( int col, String value, int prov ){
		nums[col] = 0;
		texts[col] = value == null ? "" : value;
		meta[col] = (byte) (K_TEXT | (prov << 2));
		return this;
	}

	public Row setFact( int col, Fact f ){
		if (f == null) return this;
		return f.isNum() ? setNum(col, f.num, f.prov) : setText(col, f.text, f.prov);
	}

	public Row clear( int col ){
		nums[col] = 0;
		texts[col] = null;
		meta[col] = 0;
		return this;
	}

	public int kind( int col ){
		return meta[col] & 3;
	}

	public boolean isKnown( int col ){
		return (meta[col] & 3) != K_UNKNOWN;
	}

	public long num( int col ){
		return nums[col];
	}

	public String text( int col ){
		return texts[col] == null ? "" : texts[col];
	}

	public int prov( int col ){
		return (meta[col] >> 2) & 63;
	}

	//the text of a TEXT cell, or the number as text, "" when unknown
	public String asText( int col ){
		int k = kind(col);
		return k == K_NUM ? Long.toString(nums[col]) : k == K_TEXT ? text(col) : "";
	}

	public Row copy(){
		Row r = new Row(id, mergeKey, runId, meta.length);
		System.arraycopy(nums, 0, r.nums, 0, nums.length);
		System.arraycopy(texts, 0, r.texts, 0, texts.length);
		System.arraycopy(meta, 0, r.meta, 0, meta.length);
		r.finished = finished;
		r.publicRow = publicRow;
		r.ruleset = ruleset;
		r.duplicate = duplicate;
		r.imported = imported;
		r.telemetry = telemetry;
		return r;
	}
}
