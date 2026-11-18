package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static com.shatteredpixel.shatteredpixeldungeon.apd.recorder.RecorderTestSupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadStatus;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunIndexEntry;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.After;
import org.junit.Test;

import java.util.List;

//The recorder driven through the real game code paths.
public class RecorderLifecycleTest {

	@After
	public void stopRecorder(){
		com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport.start();
		ApdRecorder.reset();
		SPDSettings.apdExoticChance(0);
	}

	@Test
	public void newRunWritesSessionStartRunStartAndAStartingCheckpoint() throws Exception {
		Session s = new Session();
		newRun(s, 4242L);
		String id = ApdRecorder.currentRunId();
		assertNotNull(id);
		assertTrue(StatsStore.isValidRunId(id));

		ReadResult r = s.read(id);
		assertEquals(ReadStatus.UNSEALED, r.status);
		assertEquals(TelemetryVersion.CURRENT, r.header.telemetryVersion);
		assertEquals(id, r.header.runId);
		assertEquals(EventTypes.SESSION_START, r.events.get(0).type);
		assertEquals(EventTypes.RUN_START, r.events.get(1).type);
		assertEquals(EventTypes.CHECKPOINT, r.events.get(2).type);

		LifecycleEvents.RunStart rs = runStart(r);
		assertEquals(id, rs.runId);
		assertEquals(0, rs.segment);
		assertEquals(LifecycleEvents.START_NEW_RUN, rs.startKind);
		assertFalse(LifecycleEvents.isPartial(r.events.get(1)));
		assertEquals(4242L, rs.seed);
		assertEquals(LifecycleEvents.SEED_RANDOM, rs.seedType);
		assertEquals("WARRIOR", rs.heroClass);
		assertEquals(0, rs.challenges);
		assertTrue(rs.rulesetId.startsWith("r4-"));
        assertTrue(rs.rulesetText.contains("exotic-scope=healing-absolute-v2"));
		assertTrue(rs.rulesetText.contains("apd0001=on"));
		assertTrue(rs.rulesetText.contains("cleric=blocked"));
		assertFalse(rs.externalModsPresent);
		assertEquals(com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset.BASE_GAME, rs.baseGame);
		assertEquals(920, rs.apdVersionCode);
		assertEquals("4.0.8-apd-INDEV", rs.apdVersionName);

		LifecycleEvents.Checkpoint cp = LifecycleEvents.decodeCheckpoint(r.events.get(2), r);
		assertEquals(LifecycleEvents.CP_RUN_START, cp.reason);
		assertEquals("WARRIOR", cp.heroClass);
		assertEquals(Dungeon.hero.HP, cp.hp);
		assertEquals(Dungeon.hero.HT, cp.hpMax);
		assertTrue("the starting kit is in the checkpoint", cp.items.size() >= 3);
		boolean hasWeaponSlot = false;
		for (LifecycleEvents.ItemEntry i : cp.items) if (i.slot == LifecycleEvents.SLOT_WEAPON) hasWeaponSlot = true;
		assertTrue(hasWeaponSlot);
	}

	@Test
	public void levelsAndSavesAreRecordedInOrder() throws Exception {
		Session s = new Session();
		newRun(s, 9L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		resetGameClock(120);
		enterLevel(2);
		ReadResult r = s.read(id);

		List<ApdEvent> enters = ofType(r, EventTypes.LEVEL_ENTER);
		List<ApdEvent> exits = ofType(r, EventTypes.LEVEL_EXIT);
		assertEquals(2, enters.size());
		assertEquals(1, exits.size());
		LifecycleEvents.LevelEnter e1 = LifecycleEvents.decodeLevelEnter(enters.get(0), r);
		LifecycleEvents.LevelEnter e2 = LifecycleEvents.decodeLevelEnter(enters.get(1), r);
		assertEquals(1, e1.depth);
		assertEquals(2, e2.depth);
		assertEquals("SewerLevel", e1.levelClass);
		LifecycleEvents.LevelExit x = LifecycleEvents.decodeLevelExit(exits.get(0));
		assertEquals(1, x.depth);
		assertEquals(120, x.turnsSpent);
		assertEquals(LifecycleEvents.EXIT_LEAVING, x.reason);
		assertEquals(120, enters.get(1).clock);

		//every SAVE is the last event of its group: a CHECKPOINT(save) right before it, and LEVEL_ENTER before that
		List<ApdEvent> saves = ofType(r, EventTypes.SAVE);
		assertEquals(2, saves.size());
		for (ApdEvent save : saves){
			ApdEvent before = r.events.get((int) save.index - 1);
			assertEquals(EventTypes.CHECKPOINT, before.type);
			assertEquals(LifecycleEvents.CP_SAVE, LifecycleEvents.decodeCheckpoint(before, r).reason);
		}
		assertTrue(enters.get(0).index < saves.get(0).index);
		assertTrue(enters.get(1).index < saves.get(1).index);
		assertEquals(120, LifecycleEvents.decodeSave(saves.get(1)).turn);
		assertEquals(2, LifecycleEvents.decodeSave(saves.get(1)).depth);
		assertEquals(s.slot, LifecycleEvents.decodeSave(saves.get(1)).slot);
	}

	@Test
	public void theGameSaveCarriesTheRunIdAndMatchesTheSaveMarker() throws Exception {
		Session s = new Session();
		newRun(s, 21L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		assertEquals(id, saveBundle(s.slot).getString("apd_run_id"));

		ReadResult r = s.read(id);
		List<ApdEvent> saves = ofType(r, EventTypes.SAVE);
		long lastPlayed = saveBundle(s.slot).getLong("last_played");
		assertEquals(lastPlayed, LifecycleEvents.decodeSave(saves.get(saves.size() - 1)).lastPlayedMillis);
	}

	@Test
	public void deathSealsTheLogAndUpdatesTheIndex() throws Exception {
		Session s = new Session();
		newRun(s, 5L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		resetGameClock(300);
		Dungeon.fail(Hunger.class);
		assertNull("a finished run is not tracked any more", ApdRecorder.currentRunId());

		ReadResult r = s.read(id);
		assertEquals(ReadStatus.SEALED, r.status);
		ApdEvent end = ofType(r, EventTypes.RUN_END).get(0);
		LifecycleEvents.RunEnd e = LifecycleEvents.decodeRunEnd(end, r);
		assertEquals(LifecycleEvents.OUTCOME_DIED, e.outcome);
		assertEquals("actors.buffs.Hunger", e.cause);
		assertEquals(1, e.depth);
		assertEquals(300, e.turns);
		assertEquals(0, e.rulesetChanges);
		//the open level was closed first, and the last checkpoint is the run-end one
		ApdEvent exit = ofType(r, EventTypes.LEVEL_EXIT).get(0);
		assertEquals(LifecycleEvents.EXIT_RUN_ENDED, LifecycleEvents.decodeLevelExit(exit).reason);
		assertTrue(exit.index < end.index);
		assertEquals(LifecycleEvents.CP_RUN_END, LifecycleEvents.decodeCheckpoint(r.events.get((int) end.index - 1), r).reason);

		RunIndexEntry entry = new StatsStore(s.root).index().get(id);
		assertEquals(RunIndexEntry.Status.SEALED, entry.status);
		assertEquals(TelemetryVersion.CURRENT, entry.telemetryVersion);
	}

	@Test
	public void erasingTheSaveOfTheOpenRunAbandonsIt() throws Exception {
		Session s = new Session();
		newRun(s, 6L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		Dungeon.deleteGame(s.slot, true);

		ReadResult r = s.read(id);
		assertEquals(ReadStatus.SEALED, r.status);
		LifecycleEvents.RunEnd e = LifecycleEvents.decodeRunEnd(ofType(r, EventTypes.RUN_END).get(0), r);
		assertEquals(LifecycleEvents.OUTCOME_ABANDONED, e.outcome);
	}

	@Test
	public void erasingAnotherSessionsSaveEndsItFromItsLogAndSaysDetailsAreUnknown() throws Exception {
		Session s = new Session();
		newRun(s, 7L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		s.restartApp(); //the app was closed, nothing is being recorded now
		assertNull(ApdRecorder.currentRunId());

		Dungeon.deleteGame(s.slot, true); //erased from the main menu

		ReadResult r = s.read(id);
		assertEquals(ReadStatus.SEALED, r.status);
		ApdEvent end = ofType(r, EventTypes.RUN_END).get(0);
		assertEquals(LifecycleEvents.OUTCOME_ABANDONED, LifecycleEvents.decodeRunEnd(end, r).outcome);
		assertTrue("placeholder fields must be flagged", (end.flags & LifecycleEvents.FLAG_DETAILS_UNKNOWN) != 0);
	}

	@Test
	public void deletingASlotWithNoRecordedRunDoesNothing() throws Exception {
		Session s = new Session();
		ApdRecorder.configure(s.root, false); //recording off: the save has no run id
		newRun(s, 8L);
		enterLevel(1);
		ApdRecorder.configure(s.root, true);
		Dungeon.deleteGame(s.slot, true);
		assertFalse(s.runsDir().exists() && s.runsDir().list().length > 0);
	}

	@Test
	public void aChangedGameplaySettingIsRecordedAsARulesetChange() throws Exception {
		Session s = new Session();
		newRun(s, 11L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		Dungeon.apdExotic = 30;      //the rules in force change (the setting alone no longer can: see exoticSettingBelongsToTheRun)
		enterLevel(2);
		enterLevel(3); //no further change
		Dungeon.fail(Hunger.class);

		ReadResult r = s.read(id);
		List<ApdEvent> changes = ofType(r, EventTypes.RULESET_CHANGE);
		assertEquals(1, changes.size());
		LifecycleEvents.RulesetChange c = LifecycleEvents.decodeRulesetChange(changes.get(0), r);
		assertEquals(runStart(r).rulesetId, c.oldId);
		assertTrue(!c.newId.equals(c.oldId));
		assertTrue(c.newText.contains("exotic=30"));
		assertEquals(2, c.depth);
		assertEquals(1, LifecycleEvents.decodeRunEnd(ofType(r, EventTypes.RUN_END).get(0), r).rulesetChanges);
		//the change came right before the level it applies to
		assertTrue(changes.get(0).index < ofType(r, EventTypes.LEVEL_ENTER).get(1).index);
	}

	@Test
	public void exoticSettingBelongsToTheRunSoMovingTheSliderRecordsNoRulesetChange() throws Exception {
		Session s = new Session();
		newRun(s, 13L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		SPDSettings.apdExoticChance(30);     //the player moves the slider: it is for the next run
		enterLevel(2);
		Dungeon.fail(Hunger.class);
		ReadResult r = s.read(id);
		assertEquals(0, ofType(r, EventTypes.RULESET_CHANGE).size());
		assertEquals("the run is recorded with the exotic setting it started with", 0, runStart(r).exoticChance);
	}

	@Test
	public void aSettingChangedInTheLastLevelIsStillRecordedWhenTheRunEnds() throws Exception {
		Session s = new Session();
		newRun(s, 12L);
		String id = ApdRecorder.currentRunId();
		enterLevel(1);
		Dungeon.apdExotic = 30;     //changed while playing, and the run ends before another level is entered
		Dungeon.fail(Hunger.class);

		ReadResult r = s.read(id);
		List<ApdEvent> changes = ofType(r, EventTypes.RULESET_CHANGE);
		assertEquals("the run is not left looking as if it had one ruleset", 1, changes.size());
		assertTrue(changes.get(0).index < ofType(r, EventTypes.RUN_END).get(0).index);
		assertEquals(1, LifecycleEvents.decodeRunEnd(ofType(r, EventTypes.RUN_END).get(0), r).rulesetChanges);
	}

	@Test
	public void rulesetIdsDependOnlyOnTheRules(){
		Ruleset a = Ruleset.of(0, 0), b = Ruleset.of(0, 0), exotic = Ruleset.of(30, 0), chal = Ruleset.of(0, 5);
		assertEquals(a.id, b.id);
		assertFalse(a.id.equals(exotic.id));
		assertFalse(a.id.equals(chal.id));
		assertTrue(a.id.matches("r1-[0-9a-f]{12}"));
	}

	@Test
	public void aNewRunWhileAnotherIsOpenLeavesTheFirstResumable() throws Exception {
		Session s = new Session();
		newRun(s, 31L);
		String first = ApdRecorder.currentRunId();
		enterLevel(1);
		newRun(s, 32L);
		String second = ApdRecorder.currentRunId();
		assertFalse(first.equals(second));
		ReadResult r = RunLogReaderHelper.read(s.log(first));
		assertEquals(ReadStatus.UNSEALED, r.status);
		assertEquals(0, ofType(r, EventTypes.RUN_END).size());
	}

	//small indirection so the test above reads the file without resetting the recorder
	static final class RunLogReaderHelper {
		static ReadResult read( java.io.File f ) throws Exception {
			return com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader.read(f);
		}
	}
}
