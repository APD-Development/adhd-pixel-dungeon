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

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import java.util.LinkedHashMap;
import java.util.Map;

//One column of a dataset: what it is called, what kind of value it holds, who may see it and how values of several log
//segments of one run combine. Columns are plain data: the UI, the filters and the aggregates are all driven by these
//definitions, so a new dataset or column needs no UI change.
//
//The id is stable and never reused for something else (it is what saved queries and cross-dataset links refer to).
public final class ColumnDef {

	public enum Type { NUM, TEXT, ENUM }

	//how a number is shown (CLASS_NAME is for a TEXT column holding a stable item class key: shown as a readable name; TITLE_CASE is for a TEXT column of CAPITAL_WORDS keys such as a hero class: shown as "Capital words")
	public enum Format { PLAIN, DATE_MILLIS, HUNDREDTHS, THOUSANDTHS, CLASS_NAME, TITLE_CASE }

	//how the values of one logical row that was recorded in several log segments combine (see RowMerger). LOWER is for the
	//number of DISTINCT things: with more than one segment it is the largest part and only ever a lower bound.
	public enum Merge { SAME, LAST, SUM, MAX, ANY, LOWER }

	public final String id;
	public final String label;
	public final Type type;
	public Format format = Format.PLAIN;
	//PUBLIC: only built from what the player could see. INTERNAL: built from hidden truth, never shown for an unfinished run.
	public EventTypes.Visibility visibility = EventTypes.Visibility.PUBLIC;
	public Merge merge = Merge.SAME;
	//the first telemetry version that records what this column needs, null when every version does
	public TelemetryVersion since;
	public boolean defaultVisible;
	public String group = "";
	public String description = "";
	//ENUM: the words for the numbers
	public final Map<Long, String> labels = new LinkedHashMap<>();
	//set by DatasetDef
	int index = -1;

	private ColumnDef( String id, String label, Type type ){
		this.id = id;
		this.label = label;
		this.type = type;
	}

	public static ColumnDef num( String id, String label ){
		return new ColumnDef(id, label, Type.NUM);
	}

	public static ColumnDef text( String id, String label ){
		return new ColumnDef(id, label, Type.TEXT);
	}

	public static ColumnDef enumeration( String id, String label ){
		return new ColumnDef(id, label, Type.ENUM);
	}

	public ColumnDef format( Format f ){
		this.format = f;
		return this;
	}

	public ColumnDef internal(){
		this.visibility = EventTypes.Visibility.INTERNAL;
		return this;
	}

	public ColumnDef merge( Merge m ){
		this.merge = m;
		return this;
	}

	public ColumnDef since( int major, int minor ){
		this.since = new TelemetryVersion(major, minor);
		return this;
	}

	public ColumnDef shown(){
		this.defaultVisible = true;
		return this;
	}

	public ColumnDef group( String g ){
		this.group = g;
		return this;
	}

	public ColumnDef describe( String d ){
		this.description = d;
		return this;
	}

	public ColumnDef label( long value, String word ){
		labels.put(value, word);
		return this;
	}

	public int index(){
		return index;
	}

	public boolean isInternal(){
		return visibility != EventTypes.Visibility.PUBLIC;
	}

	//the word for an ENUM value, or the number itself when there is none
	public String labelFor( long value ){
		String w = labels.get(value);
		return w != null ? w : Long.toString(value);
	}
}
