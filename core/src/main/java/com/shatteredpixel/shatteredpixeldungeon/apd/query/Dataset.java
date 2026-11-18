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
import java.util.List;

//A dataset's schema together with its rows, ready to be queried. The rows are derived from raw telemetry (see DatasetStore)
//and can always be rebuilt.
public final class Dataset {

	//One run as far as this dataset is concerned. It lets a result say how many of the runs in scope the rows come from:
	//a run recorded before the dataset's telemetry version has no rows because nothing was recorded, not because nothing happened.
	public static final class RunInfo {
		public final String runId;
		public final boolean finished;
		public final String ruleset;
		public final boolean covered;
		public final boolean duplicate;

		public RunInfo( String runId, boolean finished, String ruleset, boolean covered, boolean duplicate ){
			this.runId = runId;
			this.finished = finished;
			this.ruleset = ruleset;
			this.covered = covered;
			this.duplicate = duplicate;
		}
	}

	public final DatasetDef def;
	public final List<Row> rows;
	public final List<RunInfo> runs;

	public Dataset( DatasetDef def, List<Row> rows, List<RunInfo> runs ){
		this.def = def;
		this.rows = rows;
		this.runs = runs == null ? new ArrayList<RunInfo>() : runs;
	}
}
