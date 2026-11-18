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

package com.shatteredpixel.shatteredpixeldungeon.apd.demo;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Dataset;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import java.io.File;

//Progression policy with real on-disk history, independent of rendering and build flags.
//UimsdUnlock supplies the build switch and the existing settings-backed Flags adapter.
public final class UnlockProgression {
    public static final int TEN_RUNS = 10;
    private UnlockProgression(){}

	public interface Flags {
		boolean unlocked();
		void setUnlocked();
		boolean demoUsed();
		void setDemoUsed();
		// the guided tour: opened only by ten genuine runs, never by the demo route
		boolean tourUnlocked();
		void setTourUnlocked();
		// the tenth-run arrow on the title screen: pending from the tenth genuine run, retired when UIMSD is opened
		boolean promptPending();
		void setPromptPending();
		boolean promptShown();
		void setPromptShown();
	}

	public static int genuineRuns( File realRoot, int cap ){
		if (realRoot == null || cap <= 0) return 0;
		Catalog cat = new DatasetStore(realRoot).load();
		if (cat.synthetic) return 0;
		Dataset runs = cat.dataset(Datasets.RUNS);
		if (runs == null) return 0;
		int n = 0;
		for (Row r : runs.rows){
			if (r.finished && !r.imported && !r.duplicate){
				n++;
				if (n >= cap) break;
			}
		}
		return n;
	}

	public static boolean isUnlocked( Flags flags, File realRoot, boolean enforced ){
		if (!enforced || flags.unlocked()) return true;
		if (genuineRuns(realRoot, TEN_RUNS) >= TEN_RUNS){
			flags.setUnlocked();
			flags.setTourUnlocked();
			return true;
		}
		return false;
	}

	public static boolean isTourUnlocked( Flags flags, File realRoot, boolean enforced ){
		if (!enforced || flags.tourUnlocked()) return true;
		if (genuineRuns(realRoot, TEN_RUNS) >= TEN_RUNS){
			flags.setTourUnlocked();
			flags.setUnlocked();
			return true;
		}
		return false;
	}

	public static boolean demoButtonAvailable( Flags flags, File realRoot, boolean enforced ){
		return enforced && !flags.demoUsed() && !isUnlocked(flags, realRoot, true);
	}

	public static boolean onRunEnded( Flags flags, File realRoot, boolean enforced ){
		if (!enforced || flags.promptShown() || flags.promptPending()) return false;
		if (genuineRuns(realRoot, TEN_RUNS + 1) != TEN_RUNS) return false;
		flags.setUnlocked();
		flags.setTourUnlocked();
		flags.setPromptPending();
		return true;
	}

	public static boolean promptActive( Flags flags ){
		return flags.promptPending() && !flags.promptShown();
	}

	public static boolean retirePrompt( Flags flags ){
		if (!promptActive(flags)) return false;
		flags.setPromptShown();
		return true;
	}
}
