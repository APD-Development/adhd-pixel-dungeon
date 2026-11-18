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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Everything UIMSD can show right now: the datasets with their rows, and the views onto them. Built by DatasetStore from the raw
//logs (and from nothing else), and rebuilt whenever they change.
public final class Catalog {

	public static final class RulesetInfo {
		public final String id;
		public final String label;
		public final int runs;

		RulesetInfo( String id, String label, int runs ){
			this.id = id;
			this.label = label;
			this.runs = runs;
		}
	}

	private final Map<String, Dataset> datasets = new LinkedHashMap<>();
	private final Map<String, ViewDef> views = new LinkedHashMap<>();
	//Seed floors are added to the live catalog from a worker thread while the screen reads it, so every access to the two maps is synchronized
	//(shown to be needed by UimsdModelTest.theCatalogCanBeReadWhileSeedFloorsAreInstalled). The rest is built once and only read.
	public final List<RulesetInfo> rulesets = new ArrayList<>();
	//how it was built
	public int logsCached;
	public int logsRebuilt;
	public long millis;
	//built from synthetic demo/sample data (never real history)
	public boolean synthetic;

	public synchronized Catalog add( Dataset d ){
		datasets.put(d.def.id, d);
		return this;
	}

	public synchronized Catalog add( ViewDef v ){
		views.put(v.id, v);
		return this;
	}

	public synchronized Dataset dataset( String id ){
		return datasets.get(id);
	}

	public synchronized ViewDef view( String id ){
		return views.get(id);
	}

	public synchronized List<ViewDef> views(){
		return new ArrayList<>(views.values());
	}

	public synchronized List<Dataset> datasets(){
		return new ArrayList<>(datasets.values());
	}

	//the dataset a view reads
	public synchronized Dataset datasetOf( ViewDef v ){
		return datasets.get(v.dataset());
	}
}
