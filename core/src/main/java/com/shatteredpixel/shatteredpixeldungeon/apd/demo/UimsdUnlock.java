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

import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;

import java.io.File;
import java.io.IOException;

// Whether UIMSD is open to the player. It opens for good in either of two ways: after TEN_RUNS genuine runs of their own, or once, by
// generating a small demo history ("I LIKE CONFUSION"). Both are remembered as flags that do not depend on any data still being there:
// deleting runs or the demo data never closes UIMSD again, and once the demo route has been used its button never comes back.
//
// A genuine run is a finished run APD recorded itself in the real history folder. Demo data lives in its own marked folder and is never
// counted; imported old records are not counted either.
public final class UimsdUnlock {

	public static final int TEN_RUNS = UnlockProgression.TEN_RUNS;
	public static final int DEMO_RUNS = 10;

	// Chris, Nov 18 2026: the gate is on in every build (Developer, Alpha and the Play release), not only the Developer build.
	// Before 10 genuine runs the title arrow says the button is useless until then; after, the dopamine/drugs arrow by humor setting.
	public static boolean enforced(){
		return true;
	}

	// Where the two permanent flags live (settings in the game, memory in tests).
	public interface Flags extends UnlockProgression.Flags {}

	public enum Generated { DONE, NOT_AVAILABLE, FAILED }

	private UimsdUnlock(){}

	// finished runs of the player's own, counted up to 'cap' (no need to read further)
	public static int genuineRuns( File realRoot, int cap ){
		return UnlockProgression.genuineRuns(realRoot, cap);
	}

	// Is UIMSD open? Reaching TEN_RUNS opens it for good the first time it is seen (and opens the guided tour with it).
	public static boolean isUnlocked( Flags flags, File realRoot, boolean enforced ){
		return UnlockProgression.isUnlocked(flags, realRoot, enforced);
	}

	// Is the guided tour open? Only ten genuine runs open it. Opening UIMSD with demo runs does not, demo runs never count, and once it is
	// open it stays open (deleting runs or demo data does not close it), just as it stays closed for a demo-unlocked player until then.
	public static boolean isTourUnlocked( Flags flags, File realRoot, boolean enforced ){
		return UnlockProgression.isTourUnlocked(flags, realRoot, enforced);
	}

	// Should the demo button be offered? Only while UIMSD is closed and the demo route has never been used.
	public static boolean demoButtonAvailable( Flags flags, File realRoot, boolean enforced ){
		return UnlockProgression.demoButtonAvailable(flags, realRoot, enforced);
	}

	// Called once a run has ended and its log is sealed (never before: death, ascension, ranking and save work are all done by then). If this
	// was the player's TENTH genuine run, UIMSD and the guided tour open and the arrow prompt is queued for the title screen. It looks at
	// the count, so it fires for the tenth run exactly: never for demo runs (they are not counted), never for the 11th or later, never
	// again once the arrow has been retired. Returns true when the prompt was just queued.
	public static boolean onRunEnded( Flags flags, File realRoot, boolean enforced ){
		return UnlockProgression.onRunEnded(flags, realRoot, enforced);
	}

	// Is the tenth-run arrow due? From the tenth genuine run until the player opens UIMSD for the first time after it.
	public static boolean promptActive( Flags flags ){
		return UnlockProgression.promptActive(flags);
	}

	// The player opened UIMSD: if the tenth-run arrow was due it is retired for good. Returns true when it was.
	public static boolean retirePrompt( Flags flags ){
		return UnlockProgression.retirePrompt(flags);
	}

	// "I LIKE CONFUSION": writes the DEMO_RUNS demo runs, and only if that worked opens UIMSD and uses up the button. A failure leaves both
	// flags alone and removes what was half written.
	public static Generated generateDemo( Flags flags, File realRoot, Ruleset current, long now, boolean enforced ){
		if (!demoButtonAvailable(flags, realRoot, enforced)) return Generated.NOT_AVAILABLE;
		File demoRoot = DemoData.demoRootFor(realRoot);
		try {
			int written = DemoGenerator.generateUnlockSet(demoRoot, current, now);
			if (written != DEMO_RUNS) throw new IOException("wrote " + written + " runs");
		} catch (IOException | RuntimeException e){
			DemoData.delete(demoRoot);
			return Generated.FAILED;
		}
		flags.setUnlocked();
		flags.setDemoUsed();
		return Generated.DONE;
	}

	// "Delete all demo data": the demo folder only (the demo route and the unlock stay as they are). False if it could not be removed.
	public static boolean deleteDemoData( File realRoot ){
		return DemoData.delete(DemoData.demoRootFor(realRoot));
	}
}
