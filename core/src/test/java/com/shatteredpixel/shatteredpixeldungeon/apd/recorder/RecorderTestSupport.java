package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//Drives the real game classes headless (Dungeon.init, switchLevel, saveAll, loadGame, fail) with the recorder on.
final class RecorderTestSupport {

	private RecorderTestSupport(){}

	private static int nextSlot = 1;

	//a fresh history folder, recorder on, and a slot nobody else in this JVM uses
	static final class Session {
		final File root;
		final int slot;

		Session(){
			HeadlessTestSupport.start();
			root = new File(HeadlessTestSupport.tempDir(), "hist-" + System.nanoTime());
			synchronized (RecorderTestSupport.class){
				slot = nextSlot++;
			}
			ApdRecorder.configure(root, true);
		}

		//simulates the app being killed and started again: recorder state is gone, files stay
		void restartApp(){
			ApdRecorder.configure(root, true);
		}

		File runsDir(){
			return new File(root, "runs");
		}

		File log( String id ){
			return new File(runsDir(), id + ".apdlog");
		}

		ReadResult read( String id ) throws Exception {
			ApdRecorder.configure(root, true); //closes the open writer (leaving the log resumable) so the file is complete
			return RunLogReader.read(log(id));
		}
	}

	static void newRun( Session s, long seed ){
		HeadlessTestSupport.start();
		SPDSettings.challenges(0);
		SPDSettings.intro(false);
		Dungeon.daily = false;
		Dungeon.dailyReplay = false;
		Dungeon.customSeedText = "";
		Dungeon.seed = seed;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		GamesInProgress.randomizedClass = false;
		GamesInProgress.curSlot = s.slot;
		Dungeon.init();
	}

	//what the game does when the hero arrives on a level: generate it, switch to it (which saves)
	//the real game updates Game.realTime every frame, headless it would stay 0 and every save would look the same
	private static long fakeRealTime = 1_700_000_000_000L;

	static void enterLevel( int depth ){
		com.watabou.noosa.Game.realTime = ++fakeRealTime;
		Dungeon.depth = depth;
		Dungeon.branch = 0;
		Dungeon.level = Dungeon.newLevel();
		Dungeon.switchLevel(Dungeon.level, -1);
	}

	static List<ApdEvent> ofType( ReadResult r, int type ){
		List<ApdEvent> out = new ArrayList<>();
		for (ApdEvent e : r.events) if (e.type == type) out.add(e);
		return out;
	}

	static LifecycleEvents.RunStart runStart( ReadResult r ){
		return LifecycleEvents.decodeRunStart(ofType(r, com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes.RUN_START).get(0), r);
	}

	//the game save of a slot, as a bundle
	static Bundle saveBundle( int slot ) throws Exception {
		return FileUtils.bundleFromFile(GamesInProgress.gameFile(slot));
	}

	static File saveFile( int slot ){
		return FileUtils.getFileHandle(GamesInProgress.gameFile(slot)).file();
	}

	static void resetGameClock( long turns ){
		Statistics.duration = turns;
	}

	static void useLevelContext(){
		//keeps the harness class loaded so its static journal handling is consistent with the golden tests
		LevelGenerationHarness.Context.values();
	}
}
