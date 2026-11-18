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

import java.util.LinkedHashMap;
import java.util.Map;

//The derived facts of one run (an APD run log) or one imported old record, keyed by stable field names (FactKeys).
//Only known values are present. Rebuildable from the raw sources at any time.
public final class RunFacts {

	public static final int SOURCE_APD = 1, SOURCE_LEGACY = 2;

	public final int source;
	//the run id for APD runs, "legacy:" + the ranking record's game id for imported records
	public final String id;
	private final Map<String, Fact> facts = new LinkedHashMap<>();

	public RunFacts( int source, String id ){
		this.source = source;
		this.id = id;
	}

	public RunFacts put( String key, Fact f ){
		facts.put(key, f);
		return this;
	}

	public RunFacts num( String key, long value, int prov ){
		return put(key, Fact.num(value, prov));
	}

	public RunFacts text( String key, String value, int prov ){
		return put(key, Fact.text(value, prov));
	}

	public boolean isKnown( String key ){
		return facts.containsKey(key);
	}

	public Fact get( String key ){
		return facts.get(key);
	}

	//the number, or 'ifUnknown' - callers that need to tell the difference must use isKnown()
	public long numOr( String key, long ifUnknown ){
		Fact f = facts.get(key);
		return f != null && f.isNum() ? f.num : ifUnknown;
	}

	public String textOr( String key, String ifUnknown ){
		Fact f = facts.get(key);
		return f != null && !f.isNum() ? f.text : ifUnknown;
	}

	public Map<String, Fact> all(){
		return facts;
	}

	@Override
	public boolean equals( Object o ){
		if (!(o instanceof RunFacts)) return false;
		RunFacts r = (RunFacts) o;
		return source == r.source && id.equals(r.id) && facts.equals(r.facts);
	}

	@Override
	public int hashCode(){
		return id.hashCode() * 31 + facts.hashCode();
	}
}
