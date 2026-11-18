package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.replay.FloorReplay;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

//A run that is saved and left open (title screen after leaving a run) must not receive anything from a replay.
public class ReplayRecorderIsolationTest {

	@After
	public void cleanUp(){
		ApdRecorder.reset();
		FloorReplay.setLiveGameCheck(null);
	}

	private static long totalBytes( File dir ){
		long n = 0;
		File[] kids = dir.listFiles();
		if (kids == null) return 0;
		for (File k : kids) n += k.isDirectory() ? totalBytes(k) : k.length();
		return n;
	}

	@Test
	public void replayWritesNothingToAnOpenRunAndRestoresTheHooks() throws Exception {
		RecorderTestSupport.Session s = new RecorderTestSupport.Session();
		ApdRecorder.configure(s.root, true);
		RecorderTestSupport.newRun(s, 4711L);
		RecorderTestSupport.enterLevel(1);
		assertNull("a run left open must not stop a replay", FloorReplay.refusal());
		assertTrue(ApdGen.active);
		long before = totalBytes(s.root);
		final AtomicBoolean offDuring = new AtomicBoolean(false);

		FloorReplay.replay(99L, 0, HeroClass.WARRIOR, FloorReplay.LAST_FLOOR, new FloorReplay.Observer(){
			@Override
			public void level( int depth, Level level ){
				if (!ApdGen.active && !ApdItems.active && !ApdCombat.active) offDuring.set(true);
			}
		});

		assertTrue("hooks must be off while replaying", offDuring.get());
		assertTrue("hooks must be back on afterwards", ApdGen.active);
		ApdRecorder.reset();
		//closing flushes anything buffered: the run log did not grow from the replay's 26 floors
		long after = totalBytes(s.root);
		assertFalse("replay wrote " + (after - before) + " bytes into the run log", after - before > 4096);
	}
}
