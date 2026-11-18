package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static com.shatteredpixel.shatteredpixeldungeon.apd.recorder.RecorderTestSupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LogHeader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.PayloadReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadStatus;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunTimeline;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

//Loading an older save than the end of the log: the play after that save becomes an abandoned branch.
public class RollbackTest {

	@After
	public void stopRecorder(){
		ApdRecorder.reset();
	}

	//plays that exist only in the log: hero arrives on levels (no save)
	private static void unsavedPlay( int times ){
		for (int i = 0; i < times; i++) ApdRecorder.onLevelSwitched();
	}

	private static long countGameEvents( List<ApdEvent> events ){
		long n = 0;
		for (ApdEvent e : events) if (!LifecycleEvents.isRecordingMetaEvent(e.type)) n++;
		return n;
	}

	@Test
	public void killedAfterASaveAbandonsTheLaterPlayButKeepsItInTheLog() throws Exception {
		Session s = new Session();
		newRun(s, 100L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		resetGameClock(50);
		enterLevel(2);               //saves here: this is the state the game will come back to
		resetGameClock(80);
		unsavedPlay(2);              //play that never reaches a save, then the app is killed
		s.restartApp();
		Dungeon.loadGame(s.slot);    //continue

		ReadResult r = s.read(id);
		List<ApdEvent> rollbacks = ofType(r, EventTypes.ROLLBACK);
		assertEquals(1, rollbacks.size());
		LifecycleEvents.Rollback rb = LifecycleEvents.decodeRollback(rollbacks.get(0));
		assertEquals(LifecycleEvents.ROLLBACK_MATCHED, rb.kind);
		ApdEvent matchedSave = r.events.get((int) rb.matchedSaveIndex);
		assertEquals(EventTypes.SAVE, matchedSave.type);
		assertEquals(2, LifecycleEvents.decodeSave(matchedSave).depth);
		//2 unsaved level switches = FACT (time played), EXIT, CP, ENTER, CP each = 10 events
		assertEquals(10, rb.abandonedEvents);
		assertEquals(80, rb.lastAbandonedClock);

		RunTimeline timeline = RunTimeline.of(r.events);
		assertEquals(10, timeline.abandonedCount());
		for (ApdEvent e : timeline.abandoned()){
			assertTrue(e.index > matchedSave.index && e.index < rollbacks.get(0).index);
		}
		//nothing was deleted: the abandoned events are still in the file and still decode
		assertEquals(r.events.size(), timeline.canonical().size() + timeline.abandoned().size());
		assertFalse(timeline.isAbandoned(rollbacks.get(0)));
		//the resumed session's own events are canonical
		ApdEvent session = ofType(r, EventTypes.SESSION_START).get(1);
		assertFalse(timeline.isAbandoned(session));
		assertTrue(session.index > rollbacks.get(0).index);
	}

	@Test
	public void canonicalTimelineIsExactlyThePlayThatSurvived() throws Exception {
		Session s = new Session();
		newRun(s, 101L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		enterLevel(2);
		unsavedPlay(1);
		s.restartApp();
		Dungeon.loadGame(s.slot);
		enterLevel(3); //play continues after the rollback and is saved by the level switch
		ReadResult r = s.read(id);
		RunTimeline t = RunTimeline.of(r.events);

		//level enters in the canonical timeline follow the real history: 1, 2, then 3 (the abandoned extra enter is gone)
		StringBuilder canonicalEnters = new StringBuilder(), allEnters = new StringBuilder();
		for (ApdEvent e : t.canonical()) if (e.type == EventTypes.LEVEL_ENTER) canonicalEnters.append(LifecycleEvents.decodeLevelEnter(e, r).depth);
		for (ApdEvent e : r.events) if (e.type == EventTypes.LEVEL_ENTER) allEnters.append(LifecycleEvents.decodeLevelEnter(e, r).depth);
		assertEquals("123", canonicalEnters.toString());
		assertEquals("the abandoned extra arrival on depth 2 is still in the log", "1223", allEnters.toString());
		assertTrue(t.abandonedCount() > 0);
	}

	@Test
	public void repeatedKillsKeepEveryAbandonedBranch() throws Exception {
		Session s = new Session();
		newRun(s, 102L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);                //save S
		unsavedPlay(1);
		s.restartApp();
		Dungeon.loadGame(s.slot);     //rollback 1 (abandons branch A)
		unsavedPlay(2);
		s.restartApp();
		Dungeon.loadGame(s.slot);     //rollback 2, same save (abandons branch B)

		ReadResult r = s.read(id);
		List<ApdEvent> rollbacks = ofType(r, EventTypes.ROLLBACK);
		assertEquals(2, rollbacks.size());
		LifecycleEvents.Rollback r1 = LifecycleEvents.decodeRollback(rollbacks.get(0));
		LifecycleEvents.Rollback r2 = LifecycleEvents.decodeRollback(rollbacks.get(1));
		assertEquals(r1.matchedSaveIndex, r2.matchedSaveIndex);
		assertEquals(5, r1.abandonedEvents);          //1 unsaved switch: FACT (time played), EXIT, CP, ENTER, CP
		assertEquals(9, r2.abandonedEvents);          //the resumed session: FACT (time played, written when it resumed), then ENTER, CP, EXIT, CP, ENTER, CP and one FACT per switch
		//(the resumed session has no open level: ENTER, CP then EXIT, CP, ENTER, CP, and one time-played FACT per switch)

		RunTimeline t = RunTimeline.of(r.events);
		assertEquals(5 + 9, t.abandonedCount());
		assertEquals(2, t.rollbackCount());
		//no abandoned event is a recording-meta event
		for (ApdEvent e : t.abandoned()) assertFalse(LifecycleEvents.isRecordingMetaEvent(e.type));
		//canonical timeline has no unsaved play at all
		long gameEvents = countGameEvents(t.canonical());
		long upToSave = countGameEvents(r.events.subList(0, (int) r1.matchedSaveIndex + 1));
		//plus the one "time played" FACT the surviving session wrote when it resumed (it starts the played-time total over): not unsaved play
		assertEquals(upToSave + 1, gameEvents);
	}

	@Test
	public void loadingTheNewestSaveRollsBackNothing() throws Exception {
		Session s = new Session();
		newRun(s, 103L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		s.restartApp();
		Dungeon.loadGame(s.slot);
		ReadResult r = s.read(id);
		assertEquals(0, ofType(r, EventTypes.ROLLBACK).size());
		assertEquals(2, ofType(r, EventTypes.SESSION_START).size());
		assertEquals(0, RunTimeline.of(r.events).abandonedCount());
		assertEquals(ReadStatus.UNSEALED, r.status);
	}

	@Test
	public void aSaveFurtherOnThanTheLogMeansRecordingWasMissingAndSaysSo() throws Exception {
		Session s = new Session();
		newRun(s, 104L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);                         //log has events up to turn 0
		ApdRecorder.configure(s.root, false);  //recording off while the player keeps playing
		resetGameClock(500);
		Dungeon.saveAll();                     //the save is now at turn 500 and has no run id (the recorder was off)
		ApdRecorder.configure(s.root, true);
		//point the save at the log, as it would have been if only recording had failed
		forceRunId(s.slot, id);
		Dungeon.loadGame(s.slot);

		ReadResult r = s.read(id);
		List<ApdEvent> gaps = ofType(r, EventTypes.GAP);
		ApdEvent behind = null;
		for (ApdEvent g : gaps){
			PayloadReader p = g.reader(r);
			if (p.unsigned() == EventTypes.GAP_RECORDING_BEHIND_SAVE) behind = g;
		}
		assertNotNull("a GAP must say the recording is behind the save", behind);
		PayloadReader p = behind.reader(r);
		p.unsigned();
		assertEquals(500, p.unsigned());
	}

	@Test
	public void aSaveThatMatchesNoMarkerIsReportedAsUncertainAndNothingIsAbandoned() throws Exception {
		Session s = new Session();
		newRun(s, 105L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		unsavedPlay(1);
		//make the save's time stamp match no SAVE marker
		changeLastPlayed(s.slot, 123456789L);
		s.restartApp();
		Dungeon.loadGame(s.slot);

		ReadResult r = s.read(id);
		List<ApdEvent> rollbacks = ofType(r, EventTypes.ROLLBACK);
		assertEquals(1, rollbacks.size());
		assertEquals(LifecycleEvents.ROLLBACK_UNMATCHED, LifecycleEvents.decodeRollback(rollbacks.get(0)).kind);
		RunTimeline t = RunTimeline.of(r.events);
		assertEquals("uncertain means nothing is abandoned automatically", 0, t.abandonedCount());
		assertEquals(rollbacks.get(0).index, t.uncertainFromIndex());
	}

	@Test
	public void aSaveFromBeforeRecordingExistedIsAdoptedAsAPartialRun() throws Exception {
		Session s = new Session();
		ApdRecorder.configure(s.root, false); //an older build: no recording, no run id in the save
		newRun(s, 106L);
		enterLevel(1);
		assertFalse(saveBundle(s.slot).contains("apd_run_id"));

		s.restartApp();
		Dungeon.loadGame(s.slot);
		String id = ApdRecorder.currentRunId();
		assertNotNull(id);
		ReadResult r = s.read(id);
		ApdEvent start = ofType(r, EventTypes.RUN_START).get(0);
		assertTrue("the log must say it starts mid-run", LifecycleEvents.isPartial(start));
		assertEquals(LifecycleEvents.START_ADOPTED_RUN, LifecycleEvents.decodeRunStart(start, r).startKind);
		//it records the state as found, not an invented history
		assertEquals(LifecycleEvents.CP_RUN_START, LifecycleEvents.decodeCheckpoint(ofType(r, EventTypes.CHECKPOINT).get(0), r).reason);
		assertEquals(0, ofType(r, EventTypes.LEVEL_ENTER).size());

		//and from now on saves are linked
		s.restartApp();
		Dungeon.loadGame(s.slot);
		enterLevel(2);
		assertNotNull(saveBundle(s.slot).getString("apd_run_id"));
	}

	@Test
	public void aMissingLogStartsAPartialLogWithTheSameId() throws Exception {
		Session s = new Session();
		newRun(s, 107L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		s.restartApp();
		assertTrue(s.log(id).delete());
		Dungeon.loadGame(s.slot);

		assertEquals(id, ApdRecorder.currentRunId());
		ReadResult r = s.read(id);
		ApdEvent start = ofType(r, EventTypes.RUN_START).get(0);
		assertTrue(LifecycleEvents.isPartial(start));
		assertEquals(LifecycleEvents.START_LOG_MISSING, LifecycleEvents.decodeRunStart(start, r).startKind);
	}

	@Test
	public void aLogOfAnotherTelemetryVersionIsNeverAppendedToANewSegmentIsStarted() throws Exception {
		Session s = new Session();
		newRun(s, 108L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		s.restartApp();
		//replace the log with one recorded by another telemetry version
		File log = s.log(id);
		assertTrue(log.delete());
		TelemetryVersion old = new TelemetryVersion(1, 0);
		RunLogWriter w = RunLogWriter.create(log, old, id, "older build", 1);
		LifecycleEvents.RunStart rs = new LifecycleEvents.RunStart();
		rs.runId = id;
		LifecycleEvents.emitRunStart(w, 0, false, rs);
		w.close(false);
		byte[] before = readAll(log);

		Dungeon.loadGame(s.slot);
		String segmentId = ApdRecorder.currentRunId();
		assertEquals(id + "_1", segmentId);
		assertTrue("the old log is untouched", java.util.Arrays.equals(before, readAll(log)));
		//the next save points at the new segment
		enterLevel(2);
		assertEquals(segmentId, saveBundle(s.slot).getString("apd_run_id"));

		ReadResult r = s.read(segmentId);
		assertEquals(TelemetryVersion.CURRENT, r.header.telemetryVersion);
		LifecycleEvents.RunStart seg = runStart(r);
		assertEquals(id, seg.runId);
		assertEquals(1, seg.segment);
		assertEquals(LifecycleEvents.START_NEW_SEGMENT, seg.startKind);
		assertEquals(old, com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader.read(log).header.telemetryVersion);

	}

	@Test
	public void aSaveOfAnAlreadyEndedRunStartsANewSegmentInsteadOfTouchingTheSealedLog() throws Exception {
		Session s = new Session();
		newRun(s, 109L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		byte[] saveCopy = readAll(saveFile(s.slot));
		Dungeon.fail(Hunger.class);   //seals the log (and the game would delete the save)
		byte[] sealed = readAll(s.log(id));
		writeAll(saveFile(s.slot), saveCopy); //... but here the old save comes back (a restored backup)

		s.restartApp();
		Dungeon.loadGame(s.slot);
		assertEquals(id + "_1", ApdRecorder.currentRunId());
		assertTrue(java.util.Arrays.equals(sealed, readAll(s.log(id))));
		ReadResult r = s.read(id + "_1");
		assertEquals(LifecycleEvents.START_AFTER_RUN_ENDED, runStart(r).startKind);
	}

	//---- helpers -------------------------------------------------------------------------------------

	private static byte[] readAll( File f ) throws Exception {
		FileInputStream in = new FileInputStream(f);
		try {
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
			return out.toByteArray();
		} finally {
			in.close();
		}
	}

	private static void writeAll( File f, byte[] data ) throws Exception {
		FileOutputStream out = new FileOutputStream(f);
		try {
			out.write(data);
		} finally {
			out.close();
		}
	}

	//rewrites the save with a different "last played" stamp
	private static void changeLastPlayed( int slot, long value ) throws Exception {
		Bundle b = saveBundle(slot);
		b.put("last_played", value);
		FileUtils.bundleToFile(GamesInProgress.gameFile(slot), b);
	}

	private static void forceRunId( int slot, String id ) throws Exception {
		Bundle b = saveBundle(slot);
		b.put("apd_run_id", id);
		FileUtils.bundleToFile(GamesInProgress.gameFile(slot), b);
	}
}
