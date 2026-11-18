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

import java.util.Map;

//Turns an imported old record into facts, using the same names as run facts where the meaning is the same.
//Fields the old record did not contain stay absent (unknown). Nothing is filled in.
public final class LegacyFacts {

	private LegacyFacts(){}

	public static final String ID_PREFIX = "legacy:";

	public static RunFacts fromRecord( LegacyEvents.Record rec, String logId ){
		RunFacts f = new RunFacts(RunFacts.SOURCE_LEGACY, ID_PREFIX + rec.gameId);
		f.text(FactKeys.RUN_ID, ID_PREFIX + rec.gameId, Fact.PROV_LEGACY);
		f.text(FactKeys.LOG_ID, logId, Fact.PROV_LEGACY);
		f.text(FactKeys.STATUS, "legacy", Fact.PROV_LEGACY);
		for (Map.Entry<String, LegacyEvents.Value> e : rec.fields.entrySet()){
			LegacyEvents.Value v = e.getValue();
			if (!v.isKnown()) continue;
			if (v.kind == LegacyEvents.KIND_NUM){ f.num(e.getKey(), v.num, Fact.PROV_LEGACY); if(FactKeys.SCORE.equals(e.getKey())) f.num(CustomRuleFacts.ORIGINAL_SCORE,v.num,Fact.PROV_LEGACY); }
			else f.text(e.getKey(), v.text, Fact.PROV_LEGACY);
		}
		return f;
	}

	public static RunFacts fromTotals( LegacyEvents.Totals t ){
		RunFacts f = new RunFacts(RunFacts.SOURCE_LEGACY, "legacy-totals");
		for (Map.Entry<String, LegacyEvents.Value> e : t.fields.entrySet()){
			LegacyEvents.Value v = e.getValue();
			if (!v.isKnown()) continue;
			if (v.kind == LegacyEvents.KIND_NUM) f.num(e.getKey(), v.num, Fact.PROV_LEGACY);
			else f.text(e.getKey(), v.text, Fact.PROV_LEGACY);
		}
		return f;
	}
}
