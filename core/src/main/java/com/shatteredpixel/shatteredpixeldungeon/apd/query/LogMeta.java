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

//What the assembly of datasets needs to know about one log (or import file) besides its rows. It is stored with the cached rows.
public final class LogMeta {

	public String logId = "";
	//the run the log belongs to (shared by the segments of one run), "" for imported records
	public String runId = "";
	public String telemetry = "";
	public long createdAtMillis;
	public int segment;
	//the log ends with RUN_END: the run is finished as far as this log goes
	public boolean hasRunEnd;
	public boolean imported;
	public boolean unreadable;
	//the ruleset the run started with, null when unknown, Row.RULESET_MIXED when it changed during the run
	public String ruleset;
	//the id SPD gave the ranking record this run produced ("" when none was written)
	public String rankingId = "";
}
