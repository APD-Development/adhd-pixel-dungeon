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

package com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class SubmissionQueueTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private void log( String runId, boolean finished, int difficulty, int segment ){
		log(runId, finished, difficulty, segment, "MAGE");
	}

	private void log( String runId, boolean finished, int difficulty, int segment, String heroClass ){
		File f = new StatsStore(tmp.getRoot()).logFile(runId);
		f.getParentFile().mkdirs();
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, runId, "test", 1);
		LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
		s.runId = runId; s.segment = segment; s.heroClass = heroClass; s.rulesetId = "exotic30"; s.exoticChance = 30; s.apdVersionName = "4.0.8-apd";
		s.difficulty = difficulty; s.difficultyBalance = 0;
		LifecycleEvents.emitRunStart(w, 1, false, s);
		if (finished){
			LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
			e.outcome = LifecycleEvents.OUTCOME_DIED; e.cause = "Rat"; e.depth = 4; e.deepest = 4; e.heroLevel = 6; e.score = 900; e.turns = 1500;
			LifecycleEvents.emitRunEnd(w, 2, e);
		}
		w.close(finished);
	}

	@Test
	public void aFinishedRunIsQueuedOnceAndSurvivesARestart(){
		log("run-a", true, 3, 0);
		SubmissionQueue.Queued q = SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "4.0.8-apd", 100);
		assertEquals(q.detail + q.refusal, SubmissionQueue.Result.QUEUED, q.result);
		assertEquals(1, SubmissionQueue.pending(tmp.getRoot()));
		assertEquals("queuing it again does not duplicate it", SubmissionQueue.Result.ALREADY_QUEUED, SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "4.0.8-apd", 200).result);
		assertEquals(1, SubmissionQueue.pending(tmp.getRoot()));
		assertTrue(SubmissionQueue.fileIn(tmp.getRoot()).exists());
	}

	@Test
	public void addInspectRemovePersistsAndNeverChangesHistory() throws Exception {
		log("warrior-run", true, 3, 0, "WARRIOR");
		File root = tmp.getRoot();
		File run = new StatsStore(root).logFile("warrior-run");
		byte[] original = java.nio.file.Files.readAllBytes(run.toPath());
		File statistics = new File(root, "player-statistics-sentinel");
		java.nio.file.Files.write(statistics.toPath(), new byte[]{1, 2, 3});
		assertEquals(SubmissionQueue.Result.QUEUED, SubmissionQueue.enqueueRun(root, "warrior-run", "test", 10).result);
		byte[] queued = java.nio.file.Files.readAllBytes(SubmissionQueue.fileIn(root).toPath());
		assertEquals(SubmissionQueue.Result.ALREADY_QUEUED, SubmissionQueue.enqueueRun(root, "warrior-run", "new-build", 20).result);
		assertArrayEquals(queued, java.nio.file.Files.readAllBytes(SubmissionQueue.fileIn(root).toPath()));
		//Every query reopens the on-disk outbox: no in-memory queue carries this state.
		File reopenedRoot = new File(root.getCanonicalPath());
		assertEquals(1, SubmissionQueue.pending(reopenedRoot));
		assertEquals(1, SubmissionQueue.entries(reopenedRoot).size());
		assertTrue(SubmissionQueue.preview(reopenedRoot, "warrior-run").contains("runId=warrior-run"));
		assertTrue(SubmissionQueue.preview(reopenedRoot, "warrior-run").contains("WARRIOR"));
		assertTrue(SubmissionQueue.withdraw(reopenedRoot, "warrior-run"));
		assertFalse(SubmissionQueue.withdraw(reopenedRoot, "warrior-run"));
		assertEquals(0, SubmissionQueue.pending(new File(root.getCanonicalPath())));
		assertNull(SubmissionQueue.preview(root, "warrior-run"));
		assertTrue(SubmissionQueue.entries(root).isEmpty());
		assertArrayEquals(original, java.nio.file.Files.readAllBytes(run.toPath()));
		assertArrayEquals(new byte[]{1, 2, 3}, java.nio.file.Files.readAllBytes(statistics.toPath()));
		assertFalse("local queue operations do not record a transmission", SubmittedLedger.fileIn(root).exists());
	}

	@Test
	public void missingRootAndInvalidIdsNeverReferToARelativeQueue(){
		assertEquals(0, SubmissionQueue.pending(null));
		assertFalse(SubmissionQueue.isQueued(tmp.getRoot(), null));
		assertNull(SubmissionQueue.preview(tmp.getRoot(), "../bad"));
		assertFalse(SubmissionQueue.withdraw(tmp.getRoot(), null));
	}

	@Test
	public void queueCanBeInspectedAndRemovedByFreshJavaProcesses() throws Exception {
		log("warrior-restart", true, 3, 0, "WARRIOR");
		File root = tmp.getRoot();
		File log = new StatsStore(root).logFile("warrior-restart");
		byte[] original = java.nio.file.Files.readAllBytes(log.toPath());
		assertEquals(SubmissionQueue.Result.QUEUED, SubmissionQueue.enqueueRun(root, "warrior-restart", "test", 100).result);
		probe(root, "inspect", 1);
		probe(root, "remove", 1);
		probe(root, "inspect", 0);
		assertEquals(0, SubmissionQueue.pending(root));
		assertArrayEquals(original, java.nio.file.Files.readAllBytes(log.toPath()));
		assertFalse(SubmittedLedger.fileIn(root).exists());
	}

	private void probe(File root, String operation, int expected) throws Exception {
		String separator = File.pathSeparator;
		String classpath = System.getProperty("java.class.path") + separator
				+ new File(SubmissionQueueTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath() + separator
				+ new File(SubmissionQueue.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
		String javaBinary = new File(System.getProperty("java.home"), "bin/java").getPath();
		Process process = new ProcessBuilder(javaBinary, "-cp", classpath, QueueRestartProbe.class.getName(),
				root.getCanonicalPath(), operation, Integer.toString(expected)).redirectErrorStream(true).start();
		if (!process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
			process.destroyForcibly();
			fail("fresh-process queue check timed out");
		}
		String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		assertEquals(output, 0, process.exitValue());
	}

	private byte[] damagedQueue() throws Exception {
		log("run-a", true, 3, 0);
		assertEquals(SubmissionQueue.Result.QUEUED, SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "test", 1).result);
		File file = SubmissionQueue.fileIn(tmp.getRoot());
		java.nio.file.Files.writeString(file.toPath(), "damaged queue entry\n", java.nio.file.StandardOpenOption.APPEND);
		return java.nio.file.Files.readAllBytes(file.toPath());
	}

	@Test
	public void damagedQueueCannotBeSilentlyRewrittenWhenAddingARun() throws Exception {
		byte[] before = damagedQueue();
		log("run-b", true, 3, 0);
		assertEquals(SubmissionQueue.Result.FAILED, SubmissionQueue.enqueueRun(tmp.getRoot(), "run-b", "test", 2).result);
		assertArrayEquals(before, java.nio.file.Files.readAllBytes(SubmissionQueue.fileIn(tmp.getRoot()).toPath()));
	}

	@Test
	public void damagedQueueCannotBeSilentlyRewrittenWhenRemovingARun() throws Exception {
		byte[] before = damagedQueue();
		assertFalse(SubmissionQueue.withdraw(tmp.getRoot(), "run-a"));
		assertArrayEquals(before, java.nio.file.Files.readAllBytes(SubmissionQueue.fileIn(tmp.getRoot()).toPath()));
	}

	@Test
	public void damagedQueueDoesNotContactAGatewayOrLoseItsOriginalBytes() throws Exception {
		byte[] before = damagedQueue();
		final int[] contacted = {0};
		com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway gateway = (key, envelope) -> {
			contacted[0]++;
			return new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway.Response(
					com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway.Status.OK, "");
		};
		assertEquals(0, SubmissionQueue.flush(tmp.getRoot(), gateway, 100, true).sent);
		assertEquals(0, contacted[0]);
		assertArrayEquals(before, java.nio.file.Files.readAllBytes(SubmissionQueue.fileIn(tmp.getRoot()).toPath()));
	}

	@Test
	public void damageDuringAcknowledgedSendPreservesQueueAndReportsActualTransmission() throws Exception {
		log("run-a", true, 3, 0);
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "test", 1);
		final byte[][] damaged = {null};
		com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway gateway = (key, envelope) -> {
			try {
				File file = SubmissionQueue.fileIn(tmp.getRoot());
				java.nio.file.Files.writeString(file.toPath(), "damaged entry\n", java.nio.file.StandardOpenOption.APPEND);
				damaged[0] = java.nio.file.Files.readAllBytes(file.toPath());
			} catch (java.io.IOException e) { throw new AssertionError(e); }
			return new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway.Response(
					com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway.Status.OK, "");
		};
		assertEquals("a successful gateway acknowledgement is not an unsent result", 1,
				SubmissionQueue.flush(tmp.getRoot(), gateway, 100, true).sent);
		assertNotNull(SubmittedLedger.get(tmp.getRoot(), "run-a"));
		assertArrayEquals(damaged[0], java.nio.file.Files.readAllBytes(SubmissionQueue.fileIn(tmp.getRoot()).toPath()));
	}

	@Test
	public void privacyReportsUnreadableQueueInsteadOfClaimingItIsEmpty() throws Exception {
		File queue = SubmissionQueue.fileIn(tmp.getRoot());
		java.nio.file.Files.writeString(queue.toPath(), "broken entry\n");
		String text = String.join(" ", PrivacySummary.of(tmp.getRoot()).lines());
		assertTrue(text, text.contains("unreadable entries"));
		assertFalse(text, text.contains("No runs are queued") || text.contains("No items are queued"));
		assertEquals("broken entry\n", java.nio.file.Files.readString(queue.toPath()));
	}

	@Test
	public void privacyReportsUnknownCountWhenQueueIsNotReadable() throws Exception {
		assertTrue(SubmissionQueue.fileIn(tmp.getRoot()).mkdir());
		String text = String.join(" ", PrivacySummary.of(tmp.getRoot()).lines());
		assertTrue(text, text.contains("count is unknown"));
	}

	@Test
	public void manualSendReportsDamagedQueueBeforeStartingNetworkWork() throws Exception {
		damagedQueue();
		final String[] result = {null};
		BackendClient.sendNow(tmp.getRoot(), true, "http://127.0.0.1:1", message -> result[0] = message);
		assertNotNull("the refusal happens before any background sender is started", result[0]);
		assertTrue(result[0], result[0].contains("unreadable entries"));
		assertTrue(result[0], result[0].contains("Nothing was sent"));
	}

	@Test
	public void repeatedPersistedQueueIdsAreDiagnosedWithoutRewritingTheFile() throws Exception {
		log("run-a", true, 3, 0);
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "test", 1);
		File file = SubmissionQueue.fileIn(tmp.getRoot());
		String line = java.nio.file.Files.readString(file.toPath());
		java.nio.file.Files.writeString(file.toPath(), line, java.nio.file.StandardOpenOption.APPEND);
		byte[] before = java.nio.file.Files.readAllBytes(file.toPath());
		assertNotNull(SubmissionQueue.readProblem(tmp.getRoot()));
		assertEquals(1, SubmissionQueue.pending(tmp.getRoot()));
		assertFalse(SubmissionQueue.withdraw(tmp.getRoot(), "run-a"));
		assertArrayEquals(before, java.nio.file.Files.readAllBytes(file.toPath()));
	}

	@Test
	public void privacyDoesNotCallSettingsOrEvidenceQueueEntriesFinishedRuns() {
		assertTrue(SubmissionQueue.enqueueEnvelope(tmp.getRoot(),
				com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id.of(
						com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id.Kind.OUTBOX, "settings"),
				new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope("apd.settings-sync", 1, 0, ""), 1));
		String text = String.join(" ", PrivacySummary.of(tmp.getRoot()).lines());
		assertTrue(text, text.contains("1 item is queued"));
		assertFalse(text, text.contains("finished run is queued") || text.contains("never the full history"));
	}

	//No Android claim: this checks the actual queue after JVM static state has been discarded.
	public static final class QueueRestartProbe {
		public static void main(String[] args) {
			File root = new File(args[0]);
			int expected = Integer.parseInt(args[2]);
			if (SubmissionQueue.pending(root) != expected || SubmissionQueue.entries(root).size() != expected)
				throw new AssertionError("queue count after restart");
			String preview = SubmissionQueue.preview(root, "warrior-restart");
			if (expected == 1 && (preview == null || !preview.contains("runId=warrior-restart") || !preview.contains("WARRIOR")))
				throw new AssertionError("queue preview after restart");
			if (expected == 0 && preview != null) throw new AssertionError("removed queue entry returned");
			if (args[1].equals("remove") && !SubmissionQueue.withdraw(root, "warrior-restart"))
				throw new AssertionError("queue removal after restart");
		}
	}

	@Test
	public void unfinishedAndIncompleteRunsAreRefusedAndNothingIsWritten(){
		log("run-live", false, 3, 0);
		assertEquals(SubmissionQueue.Result.REFUSED, SubmissionQueue.enqueueRun(tmp.getRoot(), "run-live", "b", 1).result);
		log("run-old", true, 0, 0);
		SubmissionQueue.Queued old = SubmissionQueue.enqueueRun(tmp.getRoot(), "run-old", "b", 1);
		assertEquals("a run from before difficulty existed", RunSubmissionFactory.Refusal.VALUE_NOT_RECORDED, old.refusal);
		log("run-seg", true, 3, 1);
		assertEquals(RunSubmissionFactory.Refusal.HISTORY_STARTS_MID_RUN, SubmissionQueue.enqueueRun(tmp.getRoot(), "run-seg", "b", 1).refusal);
		assertEquals("no such run", SubmissionQueue.Result.REFUSED, SubmissionQueue.enqueueRun(tmp.getRoot(), "run-missing", "b", 1).result);
		assertEquals("bad run id", SubmissionQueue.Result.REFUSED, SubmissionQueue.enqueueRun(tmp.getRoot(), "../x", "b", 1).result);
		assertEquals(0, SubmissionQueue.pending(tmp.getRoot()));
		assertFalse(SubmissionQueue.fileIn(tmp.getRoot()).exists());
	}

	@Test
	public void flushingThroughTheOfflineGatewayKeepsTheQueueAndAWorkingGatewayEmptiesIt(){
		log("run-a", true, 3, 0);
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "b", 1);
		com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox.FlushResult off = SubmissionQueue.flush(tmp.getRoot(), new com.shatteredpixel.shatteredpixeldungeon.apd.backend.OfflineGateway(), 10_000_000);
		assertEquals(0, off.sent);
		assertEquals(1, SubmissionQueue.pending(tmp.getRoot()));
		final int[] got = new int[1];
		com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway ok = new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway(){
			public Response send( String key, com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope e ){
				got[0]++;
				return new Response(Status.OK, "");
			}
		};
		assertEquals(1, SubmissionQueue.flush(tmp.getRoot(), ok, 1_000_000_000L).sent);
		assertEquals(1, got[0]);
		assertEquals(0, SubmissionQueue.pending(tmp.getRoot()));
		assertEquals("an empty or missing queue is a no-op", 0, SubmissionQueue.flush(tmp.getRoot(), ok, 2_000_000_000L).sent);
		assertEquals(1, got[0]);
	}

	@Test
	public void aQueuedRunCanBeReadBeforeSendingAndTakenBack(){
		log("run-a", true, 3, 0);
		log("run-b", true, 2, 0);
		assertFalse(SubmissionQueue.isQueued(tmp.getRoot(), "run-a"));
		assertNull(SubmissionQueue.preview(tmp.getRoot(), "run-a"));
		assertFalse("nothing to take back", SubmissionQueue.withdraw(tmp.getRoot(), "run-a"));
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "b", 1);
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-b", "b", 2);
		assertTrue(SubmissionQueue.isQueued(tmp.getRoot(), "run-a"));
		String text = SubmissionQueue.preview(tmp.getRoot(), "run-a");
		assertNotNull(text);
		assertTrue(text.startsWith("apd.run-submission "));
		assertTrue(text.contains("runId=run-a"));
		assertTrue("a short summary, never the raw log", text.length() < 1000);
		assertTrue(SubmissionQueue.withdraw(tmp.getRoot(), "run-a"));
		assertFalse(SubmissionQueue.isQueued(tmp.getRoot(), "run-a"));
		assertTrue("the other run stays queued", SubmissionQueue.isQueued(tmp.getRoot(), "run-b"));
		assertEquals(1, SubmissionQueue.pending(tmp.getRoot()));
		assertFalse("a bad id is simply not queued", SubmissionQueue.withdraw(tmp.getRoot(), "../x"));
	}

	@Test
	public void thePrivacySummaryCountsLogsAndQueueAndSaysNothingWasSent(){
		PrivacySummary none = PrivacySummary.of(tmp.getRoot());
		assertEquals(0, none.runLogs);
		assertEquals(0, none.queued);
		log("run-a", true, 3, 0);
		log("run-b", false, 3, 0);
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "b", 1);
		PrivacySummary s = PrivacySummary.of(tmp.getRoot());
		assertEquals(2, s.runLogs);
		assertTrue(s.runLogBytes > 0);
		assertEquals(1, s.queued);
		String all = String.join(" ", s.lines());
		assertTrue(all.contains("Nothing is sent anywhere"));
		assertTrue(all.contains("1 item is queued"));
		assertTrue("when sending is on it says it is still manual", String.join(" ", s.lines(true)).contains("only when you press"));
		assertEquals("a missing folder is simply empty", 0, PrivacySummary.of(null).runLogs);
	}

	@Test
	public void sendNowDoesNothingUnlessSendingIsOnWithASecureAddress() throws Exception {
		log("run-a", true, 3, 0);
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "b", 1);
		final String[] said = new String[1];
		BackendClient.Done done = new BackendClient.Done(){
			public void finished( String message ){
				said[0] = message;
			}
		};
		BackendClient.sendNow(tmp.getRoot(), false, "https://example.org", done);
		assertTrue(said[0].startsWith("Off"));
		said[0] = null;
		BackendClient.sendNow(tmp.getRoot(), true, "", done);
		assertTrue(said[0].contains("no usable server address"));
		assertEquals("the queue is untouched", 1, SubmissionQueue.pending(tmp.getRoot()));
		assertEquals("Nothing was queued.", BackendClient.describe(new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox.FlushResult(), 0));
		com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox.FlushResult r = new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox.FlushResult();
		r.sent = 2;
		r.kept = 1;
		assertEquals("2 sent. 1 still queued, to try again later.", BackendClient.describe(r, 1));
	}

	@Test
	public void sendNowIgnoresTheRetryScheduleBecauseThePlayerAsked(){
		log("run-a", true, 3, 0);
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "b", 1);
		com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway retry = new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway(){
			public Response send( String key, com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope e ){
				return new Response(Status.RETRY_LATER, "sign-in needed");
			}
		};
		assertEquals(1, SubmissionQueue.flush(tmp.getRoot(), retry, 100).kept);
		final int[] tried = new int[1];
		com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway ok = new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway(){
			public Response send( String key, com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope e ){
				tried[0]++;
				return new Response(Status.OK, "");
			}
		};
		assertEquals("not due yet on the normal schedule", 0, SubmissionQueue.flush(tmp.getRoot(), ok, 101).sent);
		assertEquals("but a manual send tries it now", 1, SubmissionQueue.flush(tmp.getRoot(), ok, 102, true).sent);
		assertEquals(1, tried[0]);
	}

	@Test
	public void aRunQueuedOrTakenBackWhileASendIsInFlightIsNotLost(){
		log("run-a", true, 3, 0);
		log("run-b", true, 3, 0);
		log("run-c", true, 3, 0);
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "b", 1);
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-c", "b", 1);
		final java.io.File root = tmp.getRoot();
		com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway slow = new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway(){
			private boolean done;
			public Response send( String key, com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope e ){
				if (!done){
					done = true;
					//while the server is being asked, the player queues another run and takes one back
					assertEquals(SubmissionQueue.Result.QUEUED, SubmissionQueue.enqueueRun(root, "run-b", "b", 5).result);
					assertTrue(SubmissionQueue.withdraw(root, "run-c"));
				}
				return key.equals("ob_run-a") ? new Response(Status.OK, "") : new Response(Status.RETRY_LATER, "busy");
			}
		};
		SubmissionQueue.flush(root, slow, 100, true);
		assertFalse("the sent run is gone", SubmissionQueue.isQueued(root, "run-a"));
		assertTrue("the run queued during the send is still queued", SubmissionQueue.isQueued(root, "run-b"));
		assertFalse("the run taken back during the send stays out", SubmissionQueue.isQueued(root, "run-c"));
		assertEquals(1, SubmissionQueue.pending(root));
	}

	@Test
	public void theReportOfWhatIsHeldShowsQueuedItemsExactlyAndSentOnes(){
		log("run-a", true, 3, 0);
		log("run-b", true, 2, 0);
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "b", 1);
		com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway ok = new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway(){
			public Response send( String key, com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope e ){
				return new Response(Status.OK, "");
			}
		};
		SubmissionQueue.flush(tmp.getRoot(), ok, 1_000_000_000L, true);
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-b", "b", 2);
		com.shatteredpixel.shatteredpixeldungeon.apd.report.Report r = PrivacyReport.build(tmp.getRoot(), false, 5, "test");
		String text = com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportRenderer.text(r);
		assertTrue(text.contains("Queued to send"));
		assertTrue("the queued run is shown as it would be sent", text.contains("ob_run-b") && text.contains("runId=run-b"));
		assertTrue("the sent run is listed with its digest", text.contains("run-a") && text.contains(SubmittedLedger.get(tmp.getRoot(), "run-a").digestHex));
		assertFalse("a run that was only sent is not shown as queued", text.contains("ob_run-a"));
		assertTrue(text.contains("Nothing is sent anywhere"));
	}
	@Test public void aReplacementQueuedDuringAFlushKeepsItsOwnPayloadAndRetryState() throws Exception {
		com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope[] replacements = {
				new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope("apd.settings-sync", 1, 0, "new"),
				new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope("apd.settings-sync", 1, 0, "old"),
				new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope("apd.settings-sync", 1, 1, "old")};
		long[] created = {100, 200, 100};
		for (int variant = 0; variant < replacements.length; variant++) {
			final com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope replacement = replacements[variant];
			final long replacementTime = created[variant];
			for (com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway.Status status : new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway.Status[]{
					com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway.Status.OK,
					com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway.Status.REJECTED,
					com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway.Status.RETRY_LATER}) {
				File root = tmp.newFolder(status.name() + variant);
				com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id id = com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id.of(
						com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id.Kind.OUTBOX, "ob_changed");
				assertTrue(SubmissionQueue.enqueueEnvelope(root, id,
						new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope("apd.settings-sync", 1, 0, "old"), 100));
				//An in-memory gateway only: no server, connection or network traffic.
				com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway gateway = (key, envelope) -> {
					assertEquals("old", envelope.payload);
					assertTrue(SubmissionQueue.replaceEnvelope(root, id, replacement, replacementTime));
					return new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway.Response(status, "old outcome");
				};
				SubmissionQueue.flush(root, gateway, 1000, true);
				assertEquals(status.name() + variant, 1, SubmissionQueue.pending(root));
				com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox stored = new com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox();
				com.shatteredpixel.shatteredpixeldungeon.apd.backend.OutboxStore.load(stored, SubmissionQueue.fileIn(root));
				com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox.Item item = stored.items().get(0);
				assertEquals(replacement.payload, item.envelope.payload);
				assertEquals(replacement.minor, item.envelope.minor);
				assertEquals("replacement starts with its own retry state", 0, item.attempts);
				assertEquals(replacementTime, item.nextAttemptAtMillis);
				assertEquals("", item.lastDetail);
			}
		}
	}

}
