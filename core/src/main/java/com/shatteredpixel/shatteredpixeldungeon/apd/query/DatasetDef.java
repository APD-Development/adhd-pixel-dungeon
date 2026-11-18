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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//The schema of a dataset. A dataset is a set of rows that all have these columns.
public final class DatasetDef {

	public final String id;
	public final String title;
	public final String description;
	private final List<ColumnDef> columns = new ArrayList<>();
	private final Map<String, ColumnDef> byId = new HashMap<>();
	public final List<LinkDef> links = new ArrayList<>();
	//what a row is called when a group of them is counted ("Instances", "Encounters"), shown as the count column of grouped tables
	public String countLabel = "Rows";

	//what one and several of them are called in a sentence ("run", "runs"): the count label, lower case
	public String unitPlural(){
		return countLabel.toLowerCase();
	}

	public String unitSingular(){
		String p = unitPlural();
		if (p.endsWith("ies")) return p.substring(0, p.length() - 3) + "y";
		return p.endsWith("s") ? p.substring(0, p.length() - 1) : p;
	}
	//the statistics sheet shown for a group of rows (empty: the group's table aggregates only)
	public final List<StatLine> sheet = new ArrayList<>();
	//the telemetry version whose logs can supply rows at all (older logs have no rows: unknown, not "none")
	public com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion minTelemetry;

	public DatasetDef( String id, String title, String description ){
		this.id = id;
		this.title = title;
		this.description = description;
	}

	public DatasetDef add( ColumnDef c ){
		if (byId.containsKey(c.id)) throw new IllegalArgumentException("duplicate column " + c.id);
		c.index = columns.size();
		columns.add(c);
		byId.put(c.id, c);
		return this;
	}

	public DatasetDef stat( StatLine l ){
		sheet.add(l);
		return this;
	}

	public DatasetDef link( LinkDef l ){
		links.add(l);
		return this;
	}

	public List<ColumnDef> columns(){
		return Collections.unmodifiableList(columns);
	}

	public int size(){
		return columns.size();
	}

	public ColumnDef column( String id ){
		return byId.get(id);
	}

	public ColumnDef column( int index ){
		return columns.get(index);
	}

	//-1 when there is no such column
	public int indexOf( String id ){
		ColumnDef c = byId.get(id);
		return c == null ? -1 : c.index;
	}

	public List<String> defaultColumnIds(){
		List<String> out = new ArrayList<>();
		for (ColumnDef c : columns) if (c.defaultVisible) out.add(c.id);
		if (out.isEmpty() && !columns.isEmpty()) out.add(columns.get(0).id);
		return out;
	}
}
