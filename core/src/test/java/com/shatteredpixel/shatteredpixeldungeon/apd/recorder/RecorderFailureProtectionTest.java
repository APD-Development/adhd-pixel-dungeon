package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static com.shatteredpixel.shatteredpixeldungeon.apd.recorder.RecorderTestSupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

//A recorder problem must never reach gameplay: no exceptions, no changed game state, no lost saves.
public class RecorderFailureProtectionTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@After
	public void stop(){
		ApdRecorder.reset();
	}

	//a whole run's worth of game calls, which must work whatever the recorder does
	private static void playARun( Session s, boolean ends ) throws Exception {
		newRun(s, 55L);
		enterLevel(1);
		enterLevel(2);
		Dungeon.saveAll();
		Dungeon.loadGame(s.slot);
		if (ends) Dungeon.fail(Hunger.class);
	}

	@Test
	public void anUnusableHistoryFolderNeverBreaksTheGame() throws Exception {
		Session s = new Session();
		File inTheWay = tmp.newFile("not-a-folder");
		ApdRecorder.configure(new File(inTheWay, "apd"), true); //cannot be created
		int hpBefore;
		newRun(s, 56L);
		hpBefore = Dungeon.hero.HP;
		enterLevel(1);
		Dungeon.saveAll();
		Dungeon.loadGame(s.slot);
		Dungeon.fail(Hunger.class);
		assertEquals("gameplay state is untouched", hpBefore, Dungeon.hero.HP);
		RecorderStatus st = ApdRecorder.status();
		assertTrue(st.configured);
		assertEquals(0, st.eventsAccepted); //nothing could be written, and nothing complained to the game
	}

	@Test
	public void injectedRecorderFailuresAreContainedAndSwitchTheRecorderOff() throws Exception {
		Session s = new Session();
		newRun(s, 57L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		long eventsBefore = ApdRecorder.status().eventsAccepted;
		int hp = Dungeon.hero.HP;

		ApdRecorder.failureInjection = new RuntimeException("boom");
		for (int i = 0; i < ApdRecorder.MAX_ERRORS + 3; i++){
			ApdRecorder.onLevelSwitched();       //none of these may throw
			ApdRecorder.onGameSaved(i);
		}
		RecorderStatus st = ApdRecorder.status();
		assertTrue(st.sessionDisabled);
		assertEquals(ApdRecorder.MAX_ERRORS, st.errorCount);
		assertTrue(st.lastError.contains("boom"));
		assertFalse(ApdRecorder.isRecording());

		//even through real game calls, with the failure still armed
		Dungeon.saveAll();
		enterLevel(2);
		Dungeon.loadGame(s.slot);
		Dungeon.fail(Hunger.class);
		assertEquals(hp, Dungeon.hero.HP);

		//the save stays linked to the run
		assertNotNull(id);
		ApdRecorder.failureInjection = null;
		assertEquals("no more events once switched off", eventsBefore, ApdRecorder.status().eventsAccepted);
	}

	@Test
	public void aFailingStartOfARunLeavesTheGameAndItsSavesAlone() throws Exception {
		Session s = new Session();
		ApdRecorder.failureInjection = new IllegalStateException("cannot start");
		newRun(s, 58L);                        //onNewRun throws inside the recorder, must not escape
		assertNull(ApdRecorder.currentRunId());
		enterLevel(1);
		assertFalse("no run id, so the save has none", saveBundle(s.slot).contains("apd_run_id"));
		assertNotNull(Dungeon.hero);
		assertEquals(1, Dungeon.depth);
	}

	@Test
	public void recordingSwitchedOffWritesNothingAtAll() throws Exception {
		HeadlessTestSupport.start();
		File root = new File(HeadlessTestSupport.tempDir(), "off-" + System.nanoTime());
		ApdRecorder.configure(root, false);
		Session offSession = new Session();
		ApdRecorder.configure(root, false);
		playARun(offSession, true);
		assertFalse("no history folder may even be created", root.exists());
		assertFalse(saveBundle(offSession.slot).contains("apd_run_id"));
	}

	@Test
	public void anUnconfiguredRecorderDoesNothing() throws Exception {
		Session s = new Session();
		ApdRecorder.reset();
		playARun(s, true);
		assertNull(ApdRecorder.currentRunId());
		assertFalse(ApdRecorder.isRecording());
		assertFalse(ApdRecorder.status().configured);
	}

	@Test
	public void aMissingHeroDoesNotStopACheckpoint() throws Exception {
		Session s = new Session();
		newRun(s, 59L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		Hero hero = Dungeon.hero;
		Dungeon.hero = null; //something odd is going on in the game
		ApdRecorder.onGameSaved(5);
		ApdRecorder.onLevelSwitched();
		Dungeon.hero = hero;
		assertEquals(0, ApdRecorder.status().errorCount);

		ReadResult r = s.read(id);
		boolean found = false;
		for (ApdEvent e : r.events) if (e.type == EventTypes.SAVE) found = true;
		assertTrue(found);
	}

	@Test
	public void theRecorderCanBeSwitchedOffMidRunAndBackOnWithoutCorruptingTheLog() throws Exception {
		Session s = new Session();
		newRun(s, 60L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		ApdRecorder.configure(s.root, false);   //player turns recording off
		enterLevel(2);                          //play continues unrecorded
		ApdRecorder.configure(s.root, true);    //and back on
		Dungeon.loadGame(s.slot);               //continue the game: the save has no run id, since it was written while off
		String after = ApdRecorder.currentRunId();
		assertNotNull(after);

		ReadResult first = s.read(id);
		assertTrue(first.status.isClean());     //the first log is intact, just unfinished
		ReadResult second = s.read(after);
		assertTrue(second.status.isClean());
		assertTrue("a new partial log was started for the unrecorded stretch", !id.equals(after));
	}
}
