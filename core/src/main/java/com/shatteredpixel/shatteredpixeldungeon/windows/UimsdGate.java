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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.UimsdUnlock;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.watabou.noosa.Game;

import java.io.File;

// The door to UIMSD. While the unlock gate is off (UimsdUnlock.enforced()) it simply opens UIMSD. When it is on and UIMSD is still closed, it
// says how many finished runs are needed and, once only, offers the demo route ("I LIKE CONFUSION") behind the warning below.
public final class UimsdGate {

	// the warning and the two choices, word for word as the owner wrote them
	static final String WARNING_TITLE = "ARE YOU SURE ABOUT THIS?";
	static final String WARNING_TEXT = "UIMSD is an absurdly detailed statistics database. You can unlock it immediately with 10 demo runs, but there's a very good chance you'll have absolutely no idea what you're looking at.\n\nI strongly recommend playing 10 actual runs first.";
	static final String CHOICE_CONFUSION = "I LIKE CONFUSION";
	static final String CHOICE_PLAY = "I'LL PLAY 10 RUNS";
	// the arrow's message on the title screen while UIMSD is locked, exactly as the owner wrote it
	// the arrow after the tenth genuine run, by the player's humor setting (Clean unless they chose Adult)
	static final String TENTH_RUN_ARROW_ADULT = "HEY, GO HERE FOR DRUGS.";
	static final String TENTH_RUN_ARROW_CLEAN = "HEY, GO HERE FOR UNLIMITED DOPAMINE.";
	static final String LOCKED_ARROW_TEXT = "THIS BUTTON IS EFFECTIVELY USELESS TILL 10 RUNS HAVE BEEN COMPLETED.";

	// the two permanent flags, kept in the game's settings
	static final UimsdUnlock.Flags FLAGS = new UimsdUnlock.Flags(){
		@Override public boolean promptPending(){ return SPDSettings.apdTenthPending(); }
		@Override public void setPromptPending(){ SPDSettings.apdTenthPending(true); }
		@Override public boolean promptShown(){ return SPDSettings.apdTenthShown(); }
		@Override public void setPromptShown(){ SPDSettings.apdTenthShown(true); }
		@Override public boolean tourUnlocked(){ return SPDSettings.apdUimsdTourUnlocked(); }
		@Override public void setTourUnlocked(){ SPDSettings.apdUimsdTourUnlocked(true); }
		@Override public boolean unlocked(){ return SPDSettings.apdUimsdUnlocked(); }
		@Override public void setUnlocked(){ SPDSettings.apdUimsdUnlocked(true); }
		@Override public boolean demoUsed(){ return SPDSettings.apdDemoUnlockUsed(); }
		@Override public void setDemoUsed(){ SPDSettings.apdDemoUnlockUsed(true); }
	};

	//Lets a device test switch the gate on without changing the shipped default. Nothing in the game calls it.
	private static volatile boolean forcedOn;

	public static void forceGateOnForTest( boolean on ){
		forcedOn = on;
	}

	private static boolean enforced(){
		return UimsdUnlock.enforced() || forcedOn;
	}

	private UimsdGate(){}

    private static final com.shatteredpixel.shatteredpixeldungeon.apd.demo.TitleUnlockProbe TITLE_UNLOCK =
        new com.shatteredpixel.shatteredpixeldungeon.apd.demo.TitleUnlockProbe(task->{
            Thread worker=new Thread(task,"apd-title-unlock");worker.setDaemon(true);worker.start();
        },root->com.shatteredpixel.shatteredpixeldungeon.apd.demo.UnlockProgression.genuineRuns(root,10));

	// Do not scan a locked player's history just to warm an unavailable screen.
	// Once permanently unlocked, the title may prepare the real database early.
	public static boolean canPrewarm(){ return !enforced() || FLAGS.unlocked(); }

	// The text under the title screen's arrow: after the tenth genuine run the drugs/dopamine message (by humor setting) until UIMSD has been
	// opened once; before ten runs the owner's locked message; otherwise the usual text.
	public static String titleAnnotation( String usual ){
		if(!enforced())return usual;
        File root=ApdRecorder.historyRoot();
        if(!FLAGS.tourUnlocked()){
            // Rendering uses permanent flags/a completed probe only. The worker
            // retains the original validated count; no guessed or synthetic runs.
            if(TITLE_UNLOCK.earned(root)){FLAGS.setTourUnlocked();FLAGS.setUnlocked();}
            else TITLE_UNLOCK.request(root);
        }
        return FLAGS.tourUnlocked() ? (SPDSettings.apdAdultHumor()?TENTH_RUN_ARROW_ADULT:TENTH_RUN_ARROW_CLEAN) : LOCKED_ARROW_TEXT;
	}

	static String annotationFor( UimsdUnlock.Flags flags, File root, boolean enforced, boolean adultHumor, String usual ){
		if (!enforced) return usual;
		// Persist an earned genuine-run unlock through the existing policy. Merely
		// checking the count here used to rebuild the database on every menu return
		// for players who had not yet opened the tour (including demo-unlocked ones).
		if (UimsdUnlock.isTourUnlocked(flags, root, true))
			return adultHumor ? TENTH_RUN_ARROW_ADULT : TENTH_RUN_ARROW_CLEAN;
		// Demo access opens the database, but does not replace ten genuine runs.
		return LOCKED_ARROW_TEXT;
	}

	// Called from the end of a finished run, after everything the run needs is done. It only records the tenth-run state, on its own thread:
	// it never changes scene, never touches the save, and the title screen shows the prompt the next time it is built.
	public static void onRunEnded(){
        TITLE_UNLOCK.invalidate();
		final File root = ApdRecorder.historyRoot();
		final boolean on = enforced();
		if (!on || root == null) return;
		Thread t = new Thread(new Runnable(){
			@Override
			public void run(){
				try {
					UimsdUnlock.onRunEnded(FLAGS, root, on);
				} catch (Throwable ignored){
					//a failure here must never reach the game
				}
			}
		}, "apd-tenth-run");
		t.setDaemon(true);
		t.start();
	}

	// May the guided tour start? Only after ten genuine runs; the demo route does not open it.
	public static boolean tourAvailable(){
		return UimsdUnlock.isTourUnlocked(FLAGS, ApdRecorder.historyRoot(), enforced());
	}

	// Every way into UIMSD comes here. Open: straight in. Locked: the warning pop-up, and only because the player tapped the locked button.
	public static void open( final Class<? extends PixelScene> from ){
		final File root = ApdRecorder.historyRoot();
		if (UimsdUnlock.isUnlocked(FLAGS, root, enforced())){
			//opening UIMSD after the tenth genuine run is what retires the title arrow
			UimsdUnlock.retirePrompt(FLAGS);
			UimsdScene.openNow(from);
			return;
		}
		if (!UimsdUnlock.demoButtonAvailable(FLAGS, root, enforced())) return;   //cannot happen: using the demo route opens UIMSD
		ShatteredPixelDungeon.scene().addToFront(new WndDemoUnlock(new WndDemoUnlock.Choice(){
			@Override
			public void confusion(){
				generate(from, root);
			}

			@Override
			public void playRuns(){
				//back to normal gameplay: nothing was made and nothing is used up
			}
		}));
	}

	//ten small made-up runs take a moment, so this is done off the game's thread
	private static void generate( final Class<? extends PixelScene> from, final File root ){
		Thread t = new Thread(new Runnable(){
			@Override
			public void run(){
				final UimsdUnlock.Generated result = UimsdUnlock.generateDemo(FLAGS, root, Ruleset.forNewRun(), System.currentTimeMillis(), enforced());
				Game.runOnRenderThread(new com.watabou.utils.Callback(){
					@Override
					public void call(){
						if (result == UimsdUnlock.Generated.DONE){
							DemoData.viewDemo(true);
							UimsdScene.openNow(from);
						} else {
							ShatteredPixelDungeon.scene().addToFront(new WndMessage("The demo runs could not be made, so UIMSD is still locked. Nothing was changed."));
						}
					}
				});
			}
		}, "apd-demo-unlock");
		t.setDaemon(true);
		t.start();
	}
}


