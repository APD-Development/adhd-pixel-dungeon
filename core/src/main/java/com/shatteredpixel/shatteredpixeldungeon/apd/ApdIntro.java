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

package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.watabou.utils.FileUtils;

import java.io.File;

// APD keeps the guidebook and introductory hints, but never blocks controls or the entrance exit.
// Returning-install detection remains for compatibility. The intro also ends when its guide is read
// or the player leaves the first entrance room, so the tutorial hunger exemption cannot follow a run.
public final class ApdIntro {

	private ApdIntro(){}

	//the rule, separate from the game so it can be tested: any finished run, or evidence of several earlier runs
	public static boolean returningPlayer( boolean rankingsFileExists, int savedGames, int recordedRuns ){
		return rankingsFileExists || savedGames >= 2 || recordedRuns >= 2;
	}

	//ends the tutorial for an install that has clearly been used before; never starts it and never throws
	public static void endIfNotNew(){
		try {
			if (!SPDSettings.intro()) return;
			int saves = 0;
			for (int slot = 0; slot < GamesInProgress.MAX_SLOTS; slot++){
				if (GamesInProgress.gameExists(slot)) saves++;
			}
			if (returningPlayer(FileUtils.fileExists(Rankings.RANKINGS_FILE), saves, recordedRuns())){
				SPDSettings.intro(false);
			}
		} catch (Throwable t){
			//an unreadable folder must never stop the app from starting; the tutorial simply stays as it was
		}
	}

	public static boolean shouldEndIntro(int depth, int branch, boolean inEntrance, boolean guideRead){
		return guideRead || depth != 1 || branch != 0 || !inEntrance;
	}

	public static void finishIfPastEntrance(com.shatteredpixel.shatteredpixeldungeon.levels.Level level, int depth, int branch, int pos){
		if (level == null || !SPDSettings.intro()) return;
		com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room room = level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel
				? ((com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel) level).room(pos) : null;
		if (shouldEndIntro(depth, branch, room != null && room.isEntrance(),
				com.shatteredpixel.shatteredpixeldungeon.journal.Document.ADVENTURERS_GUIDE.isPageRead(
						com.shatteredpixel.shatteredpixeldungeon.journal.Document.GUIDE_INTRO))) SPDSettings.intro(false);
	}

	private static int recordedRuns(){
		File root = ApdRecorder.historyRoot();
		if (root == null) return 0;
		File[] runs = new File(root, "runs").listFiles();
		if (runs == null) return 0;
		int n = 0;
		for (File f : runs) if (f.getName().endsWith(".apdlog")) n++;
		return n;
	}
}
