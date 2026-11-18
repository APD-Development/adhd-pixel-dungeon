package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

//Every lifecycle event must read back exactly as it was written.
public class LifecycleEventsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private RunLogWriter writer;
	private File file;

	private void open() throws Exception {
		file = new File(tmp.getRoot(), "lc" + System.nanoTime() + ".apdlog");
		writer = RunLogWriter.create(file, TelemetryVersion.CURRENT, "lc", "t", 0);
	}

	private ReadResult finish() throws Exception {
		assertTrue(writer.close());
		return RunLogReader.read(file);
	}

	@Test
	public void runStartRoundTripsAndKeepsThePartialFlag() throws Exception {
		open();
		LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
		s.runId = "run-abc"; s.segment = 3; s.startKind = LifecycleEvents.START_LOG_MISSING; s.apdVersionCode = 920;
		s.apdVersionName = "4.0.8-apd"; s.baseGame = "SPD 4.0.0"; s.initialVersion = 909; s.seed = -123456789012L;
		s.seedType = LifecycleEvents.SEED_DAILY; s.customSeedText = "2026-10-06"; s.challenges = 511; s.heroClass = "MAGE";
		s.rulesetId = "r1-abcdef012345"; s.rulesetText = "base=spd-4.0.0"; s.exoticChance = 80; s.introActive = true;
		s.externalModsPresent = false; s.platform = "android"; s.realTimeMillis = 1_700_000_000_123L;
		assertTrue(LifecycleEvents.emitRunStart(writer, 5, true, s));
		ReadResult r = finish();

		ApdEvent e = r.events.get(0);
		assertEquals(EventTypes.RUN_START, e.type);
		assertTrue(LifecycleEvents.isPartial(e));
		LifecycleEvents.RunStart d = LifecycleEvents.decodeRunStart(e, r);
		assertEquals("run-abc", d.runId);
		assertEquals(3, d.segment);
		assertEquals(LifecycleEvents.START_LOG_MISSING, d.startKind);
		assertEquals(920, d.apdVersionCode);
		assertEquals("4.0.8-apd", d.apdVersionName);
		assertEquals("SPD 4.0.0", d.baseGame);
		assertEquals(909, d.initialVersion);
		assertEquals(-123456789012L, d.seed);
		assertEquals(LifecycleEvents.SEED_DAILY, d.seedType);
		assertEquals("2026-10-06", d.customSeedText);
		assertEquals(511, d.challenges);
		assertEquals("MAGE", d.heroClass);
		assertEquals("r1-abcdef012345", d.rulesetId);
		assertEquals(80, d.exoticChance);
		assertTrue(d.introActive);
		assertFalse(d.externalModsPresent);
		assertEquals("android", d.platform);
		assertEquals(1_700_000_000_123L, d.realTimeMillis);
	}

	@Test
	public void runEndSaveLevelAndSessionEventsRoundTrip() throws Exception {
		open();
		LifecycleEvents.RunEnd end = new LifecycleEvents.RunEnd();
		end.outcome = LifecycleEvents.OUTCOME_WON; end.cause = "actors.mobs.Yog"; end.depth = 26; end.deepest = 26;
		end.heroLevel = 30; end.score = -5; end.turns = 99999; end.rulesetChanges = 2;
		LifecycleEvents.emitRunEnd(writer, 9, end, LifecycleEvents.FLAG_DETAILS_UNKNOWN);

		LifecycleEvents.Save save = new LifecycleEvents.Save();
		save.slot = 2; save.depth = 7; save.branch = 1; save.lastPlayedMillis = 1_700_000_000_999L; save.turn = 4242;
		LifecycleEvents.emitSave(writer, 10, save);

		LifecycleEvents.LevelEnter enter = new LifecycleEvents.LevelEnter();
		enter.depth = 7; enter.branch = 1; enter.entryMode = "FALL"; enter.levelClass = "PrisonLevel"; enter.feeling = "CHASM";
		LifecycleEvents.emitLevelEnter(writer, 11, enter);

		LifecycleEvents.LevelExit exit = new LifecycleEvents.LevelExit();
		exit.depth = 7; exit.branch = 1; exit.turnsSpent = -1; exit.reason = LifecycleEvents.EXIT_RUN_ENDED;
		LifecycleEvents.emitLevelExit(writer, 12, exit);

		LifecycleEvents.SessionStart session = new LifecycleEvents.SessionStart();
		session.apdVersionCode = 920; session.apdVersionName = "4.0.8-apd"; session.platform = "desktop"; session.realTimeMillis = 77;
		LifecycleEvents.emitSessionStart(writer, 13, session);

		LifecycleEvents.RulesetChange change = new LifecycleEvents.RulesetChange();
		change.oldId = "r1-a"; change.newId = "r1-b"; change.newText = "exotic=10"; change.depth = 4;
		LifecycleEvents.emitRulesetChange(writer, 14, change);

		ReadResult r = finish();
		assertEquals(6, r.events.size());

		assertTrue((r.events.get(0).flags & LifecycleEvents.FLAG_DETAILS_UNKNOWN) != 0);
		LifecycleEvents.RunEnd e = LifecycleEvents.decodeRunEnd(r.events.get(0), r);
		assertEquals(LifecycleEvents.OUTCOME_WON, e.outcome);
		assertEquals("actors.mobs.Yog", e.cause);
		assertEquals(26, e.depth);
		assertEquals(30, e.heroLevel);
		assertEquals(-5, e.score);
		assertEquals(99999, e.turns);
		assertEquals(2, e.rulesetChanges);

		LifecycleEvents.Save sv = LifecycleEvents.decodeSave(r.events.get(1));
		assertEquals(2, sv.slot); assertEquals(7, sv.depth); assertEquals(1, sv.branch);
		assertEquals(1_700_000_000_999L, sv.lastPlayedMillis); assertEquals(4242, sv.turn);

		LifecycleEvents.LevelEnter le = LifecycleEvents.decodeLevelEnter(r.events.get(2), r);
		assertEquals(7, le.depth); assertEquals(1, le.branch); assertEquals("FALL", le.entryMode);
		assertEquals("PrisonLevel", le.levelClass); assertEquals("CHASM", le.feeling);

		LifecycleEvents.LevelExit lx = LifecycleEvents.decodeLevelExit(r.events.get(3));
		assertEquals(-1, lx.turnsSpent);
		assertEquals(LifecycleEvents.EXIT_RUN_ENDED, lx.reason);

		LifecycleEvents.SessionStart ss = LifecycleEvents.decodeSessionStart(r.events.get(4), r);
		assertEquals(920, ss.apdVersionCode); assertEquals("desktop", ss.platform); assertEquals(77, ss.realTimeMillis);

		LifecycleEvents.RulesetChange rc = LifecycleEvents.decodeRulesetChange(r.events.get(5), r);
		assertEquals("r1-a", rc.oldId); assertEquals("r1-b", rc.newId); assertEquals("exotic=10", rc.newText); assertEquals(4, rc.depth);
	}

	@Test
	public void checkpointKeepsItemsWithWhatThePlayerKnew() throws Exception {
		open();
		LifecycleEvents.Checkpoint c = new LifecycleEvents.Checkpoint();
		c.reason = LifecycleEvents.CP_SAVE; c.depth = 4; c.branch = 0; c.turn = 1234; c.heroLevel = 6; c.exp = 77;
		c.hp = 31; c.hpMax = 40; c.strength = 12; c.hunger = 301; c.shielding = 4; c.gold = 250; c.energy = 3;
		c.heroClass = "ROGUE"; c.subClass = "ASSASSIN";
		LifecycleEvents.ItemEntry ring = new LifecycleEvents.ItemEntry();
		ring.slot = LifecycleEvents.SLOT_RING; ring.className = "items.rings.RingOfWealth"; ring.quantity = 1;
		ring.level = 3; ring.levelKnown = false; ring.cursedKnown = false; //a +3 ring the player does not know about yet
		LifecycleEvents.ItemEntry sword = new LifecycleEvents.ItemEntry();
		sword.slot = LifecycleEvents.SLOT_BACKPACK; sword.className = "items.weapon.melee.Sword"; sword.quantity = 1;
		sword.level = -2; sword.levelKnown = true; sword.cursedKnown = true;
		c.items.add(ring);
		c.items.add(sword);
		LifecycleEvents.emitCheckpoint(writer, 1234, c);
		ReadResult r = finish();

		LifecycleEvents.Checkpoint d = LifecycleEvents.decodeCheckpoint(r.events.get(0), r);
		assertEquals(LifecycleEvents.CP_SAVE, d.reason);
		assertEquals(1234, d.turn); assertEquals(6, d.heroLevel); assertEquals(31, d.hp); assertEquals(40, d.hpMax);
		assertEquals(12, d.strength); assertEquals(301, d.hunger); assertEquals(4, d.shielding); assertEquals(250, d.gold);
		assertEquals("ROGUE", d.heroClass); assertEquals("ASSASSIN", d.subClass);
		assertEquals(2, d.items.size());
		LifecycleEvents.ItemEntry r0 = d.items.get(0), r1 = d.items.get(1);
		assertEquals("items.rings.RingOfWealth", r0.className);
		assertEquals(3, r0.level);
		assertFalse("the player did not know this ring's level", r0.levelKnown);
		assertEquals(-2, r1.level);
		assertTrue(r1.levelKnown && r1.cursedKnown);
	}

	@Test
	public void noHunterOrStatusFieldsAreLostWhenEmpty() throws Exception {
		open();
		LifecycleEvents.emitCheckpoint(writer, 0, new LifecycleEvents.Checkpoint());
		ReadResult r = finish();
		LifecycleEvents.Checkpoint d = LifecycleEvents.decodeCheckpoint(r.events.get(0), r);
		assertEquals(-1, d.hunger);
		assertEquals(0, d.items.size());
		assertEquals("", d.heroClass);
	}

	@Test
	public void recordingMetaEventsAreOnlyGapRollbackAndSession(){
		assertTrue(LifecycleEvents.isRecordingMetaEvent(EventTypes.GAP));
		assertTrue(LifecycleEvents.isRecordingMetaEvent(EventTypes.ROLLBACK));
		assertTrue(LifecycleEvents.isRecordingMetaEvent(EventTypes.SESSION_START));
		for (int t : new int[]{EventTypes.RUN_START, EventTypes.RUN_END, EventTypes.SAVE, EventTypes.LEVEL_ENTER,
				EventTypes.LEVEL_EXIT, EventTypes.CHECKPOINT, EventTypes.RULESET_CHANGE}){
			assertFalse(EventTypes.name(t), LifecycleEvents.isRecordingMetaEvent(t));
		}
	}

	@Test
	public void decodingAShortPayloadFailsCleanlyNotWithAnArrayError() throws Exception {
		open();
		writer.event(EventTypes.SAVE).unsigned(1).emit(); //far too few fields
		ReadResult r = finish();
		try {
			LifecycleEvents.decodeSave(r.events.get(0));
			org.junit.Assert.fail("expected FormatException");
		} catch (FormatException expected){
			//good
		}
	}
}
