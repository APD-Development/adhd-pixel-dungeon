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

//A snapshot of what the recorder is doing, for diagnostics screens and tests.
public final class RecorderStatus {

	public boolean configured;
	public boolean enabled;
	//recording was switched off for this app session after repeated internal errors
	public boolean sessionDisabled;
	public int errorCount;
	public String lastError = "";
	public String telemetryVersion = "";

	//the run being recorded, null if none
	public String logId;
	public String logPath;
	public boolean writerDisabled;
	public String writerProblem = "";
	public long eventsAccepted;
	public long eventsDropped;

	//size of everything under the history folder
	public long storageBytes;
	public int runLogs;

	public String summary(){
		StringBuilder sb = new StringBuilder();
		sb.append("Recording: ");
		if (!configured) sb.append("not set up");
		else if (!enabled) sb.append("off");
		else if (sessionDisabled) sb.append("STOPPED after errors");
		else sb.append("on");
		sb.append(" (Telemetry ").append(telemetryVersion).append(")\n");
		if (logId != null){
			sb.append("Run log: ").append(logId.length() > 8 ? logId.substring(0, 8) + "..." : logId).append("\n");
			sb.append("Events: ").append(eventsAccepted);
			if (eventsDropped > 0) sb.append(" (").append(eventsDropped).append(" dropped)");
			sb.append(writerDisabled ? "  WRITER OFF" : "").append("\n");
		} else {
			sb.append("No run being recorded\n");
		}
		sb.append("History: ").append(runLogs).append(" runs, ").append(storageBytes / 1024).append(" KB\n");
		if (errorCount > 0 || !lastError.isEmpty()){
			sb.append("Errors: ").append(errorCount).append("  last: ").append(lastError).append("\n");
		}
		if (!writerProblem.isEmpty()) sb.append("Writer: ").append(writerProblem).append("\n");
		return sb.toString();
	}
}
