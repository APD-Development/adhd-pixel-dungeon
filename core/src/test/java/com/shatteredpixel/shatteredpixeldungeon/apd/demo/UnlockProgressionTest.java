package com.shatteredpixel.shatteredpixeldungeon.apd.demo;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;
import java.io.File;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class UnlockProgressionTest {
	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private static final class Flags implements UnlockProgression.Flags {
		boolean unlocked, demo, tour, pending, shown;
		public boolean unlocked(){ return unlocked; }
		public void setUnlocked(){ unlocked = true; }
		public boolean demoUsed(){ return demo; }
		public void setDemoUsed(){ demo = true; }
		public boolean tourUnlocked(){ return tour; }
		public void setTourUnlocked(){ tour = true; }
		public boolean promptPending(){ return pending; }
		public void setPromptPending(){ pending = true; }
		public boolean promptShown(){ return shown; }
		public void setPromptShown(){ shown = true; }
	}

	private void log(File root, String id, boolean ended){
		log(root, id, ended, LifecycleEvents.OUTCOME_DIED);
	}

	private void log(File root, String id, boolean ended, int outcome){
		RunLogWriter writer = RunLogWriter.create(new StatsStore(root).logFile(id), TelemetryVersion.CURRENT, id, "test", 1);
		LifecycleEvents.RunStart start = new LifecycleEvents.RunStart();
		start.runId = id;
		start.heroClass = "WARRIOR";
		start.rulesetId = "test-rules";
		LifecycleEvents.emitRunStart(writer, 1, false, start);
		if (ended){
			LifecycleEvents.RunEnd end = new LifecycleEvents.RunEnd();
			end.outcome = outcome;
			end.cause = "Rat";
			LifecycleEvents.emitRunEnd(writer, 2, end);
		}
		writer.close(ended);
	}

	@Test public void theTenthCompletedRealRunUnlocksBothAndQueuesThePrompt(){
		Flags flags = new Flags();
		for (int i = 1; i <= 9; i++) log(tmp.getRoot(), "run-" + i, true);
		log(tmp.getRoot(), "unfinished", false);
		assertEquals(9, UnlockProgression.genuineRuns(tmp.getRoot(), 11));
		assertFalse(UnlockProgression.isUnlocked(flags, tmp.getRoot(), true));
		assertFalse(UnlockProgression.isTourUnlocked(flags, tmp.getRoot(), true));
		assertFalse(UnlockProgression.onRunEnded(flags, tmp.getRoot(), true));
		log(tmp.getRoot(), "run-10", true);
		assertTrue(UnlockProgression.onRunEnded(flags, tmp.getRoot(), true));
		assertTrue(flags.unlocked);
		assertTrue(flags.tour);
		assertTrue(UnlockProgression.promptActive(flags));
		assertFalse("duplicate notification does not queue another prompt", UnlockProgression.onRunEnded(flags, tmp.getRoot(), true));
		assertTrue(UnlockProgression.retirePrompt(flags));
		assertFalse(UnlockProgression.promptActive(flags));
		log(tmp.getRoot(), "run-11", true);
		assertFalse(UnlockProgression.onRunEnded(flags, tmp.getRoot(), true));
	}

	@Test public void aMarkedDemoRootNeverCountsEvenIfItsRunsAreFinished() throws Exception {
		Files.write(new File(tmp.getRoot(), DatasetStore.DEMO_MARKER_FILE).toPath(), new byte[]{1});
		for (int i = 0; i < 10; i++) log(tmp.getRoot(), "demo-" + i, true);
		assertTrue("the demo fixture really contains rows", new DatasetStore(tmp.getRoot()).load().dataset("runs").rows.size() > 0);
		assertEquals(0, UnlockProgression.genuineRuns(tmp.getRoot(), 10));
		Flags flags = new Flags();
		assertFalse(UnlockProgression.isUnlocked(flags, tmp.getRoot(), true));
		assertFalse(UnlockProgression.isTourUnlocked(flags, tmp.getRoot(), true));
	}

	@Test public void demoUnlockNeverUnlocksTheTourOrQueuesARealRunPrompt(){
		Flags flags = new Flags();
		flags.setUnlocked();
		flags.setDemoUsed();
		assertTrue(UnlockProgression.isUnlocked(flags, tmp.getRoot(), true));
		assertFalse(UnlockProgression.isTourUnlocked(flags, tmp.getRoot(), true));
		assertFalse(UnlockProgression.onRunEnded(flags, tmp.getRoot(), true));
		assertFalse(flags.pending);
		assertFalse(UnlockProgression.demoButtonAvailable(flags, tmp.getRoot(), true));
	}

	@Test public void rememberedUnlockFlagsDoNotDependOnDeletedHistory(){
		Flags flags = new Flags();
		flags.setUnlocked();
		flags.setTourUnlocked();
		assertTrue(UnlockProgression.isUnlocked(flags, tmp.getRoot(), true));
		assertTrue(UnlockProgression.isTourUnlocked(flags, tmp.getRoot(), true));
	}

	@Test public void normalBuildPolicyDoesNotEnforceThePrivateGate(){
		Flags flags = new Flags();
		assertTrue(UnlockProgression.isUnlocked(flags, tmp.getRoot(), false));
		assertTrue(UnlockProgression.isTourUnlocked(flags, tmp.getRoot(), false));
		assertFalse(UnlockProgression.demoButtonAvailable(flags, tmp.getRoot(), false));
		assertFalse(UnlockProgression.onRunEnded(flags, tmp.getRoot(), false));
		assertFalse(flags.unlocked);
		assertFalse(flags.tour);
	}

	@Test public void missingHistoryAndNonpositiveCapsCountNothing(){
		assertEquals(0, UnlockProgression.genuineRuns(null, 10));
		log(tmp.getRoot(), "run-1", true);
		assertEquals(0, UnlockProgression.genuineRuns(tmp.getRoot(), 0));
		assertEquals(0, UnlockProgression.genuineRuns(tmp.getRoot(), -1));
	}

	@Test public void importedRecordsAndStrayDemoLogsNeverCountAsRealRuns(){
		File root = tmp.getRoot();
		log(root, "real-run", true);
		log(root, "demo-stray", true);
		File file = new File(new File(root, "legacy"), "imported.apdlog");
		RunLogWriter writer = RunLogWriter.create(file, TelemetryVersion.CURRENT, "imported", "test", 1);
		for (int i = 0; i < 20; i++){
			com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents.Record record = new com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents.Record();
			record.gameId = "old-" + i;
			record.source = "rankings.dat";
			com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents.emitRecord(writer, i + 1, record);
		}
		writer.close(true);
		assertTrue(new DatasetStore(root).load().dataset("runs").rows.size() > 1);
		assertEquals(1, UnlockProgression.genuineRuns(root, 100));
		assertFalse(UnlockProgression.isUnlocked(new Flags(), root, true));
	}
	@Test public void recordedAbandonmentCurrentlyCountsAsAnEndedRun(){
		//Characterization: older docs disagree about "completed". Do not change
		//players' progression until the intended abandonment policy is settled.
		log(tmp.getRoot(), "abandoned-real-run", true, LifecycleEvents.OUTCOME_ABANDONED);
		assertEquals(1, UnlockProgression.genuineRuns(tmp.getRoot(), 10));
	}

}
