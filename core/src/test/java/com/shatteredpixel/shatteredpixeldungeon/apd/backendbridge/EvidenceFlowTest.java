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

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Evidence;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.EvidenceMessages;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.OutboxStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;

public class EvidenceFlowTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Gateway OK = new Gateway(){
		public Response send( String key, Envelope e ){
			return new Response(Status.OK, "");
		}
	};

	private void log( String runId ){
		File f = new StatsStore(tmp.getRoot()).logFile(runId);
		f.getParentFile().mkdirs();
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, runId, "test", 1);
		LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
		s.runId = runId; s.segment = 0; s.heroClass = "MAGE"; s.rulesetId = "exotic30"; s.exoticChance = 30; s.apdVersionName = "4.0.8-apd";
		s.difficulty = 3; s.difficultyBalance = 0;
		LifecycleEvents.emitRunStart(w, 1, false, s);
		LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
		e.outcome = LifecycleEvents.OUTCOME_DIED; e.cause = "Rat"; e.depth = 4; e.deepest = 4; e.heroLevel = 6; e.score = 900; e.turns = 1500;
		LifecycleEvents.emitRunEnd(w, 2, e);
		w.close(true);
	}

	private void submit( String runId ){
		log(runId);
		assertEquals(SubmissionQueue.Result.QUEUED, SubmissionQueue.enqueueRun(tmp.getRoot(), runId, "b", 1).result);
		assertEquals(1, SubmissionQueue.flush(tmp.getRoot(), OK, 2_000_000_000L).sent);
	}

	private static EvidenceMessages.Asked ask( String runId, Evidence.Kind kind, long deadline ){
		return new EvidenceMessages.Asked(new Evidence.Request(Id.of(Id.Kind.SUBMISSION, "sub_" + runId), kind, deadline), runId);
	}

	@Test
	public void whatWasSentIsRememberedOnceAndAskedRequestsAreStoredWithoutDuplicates(){
		assertNull(SubmittedLedger.get(tmp.getRoot(), "run-a"));
		submit("run-a");
		SubmittedLedger.Entry e = SubmittedLedger.get(tmp.getRoot(), "run-a");
		assertNotNull(e);
		assertEquals(64, e.digestHex.length());
		assertFalse("the first record stands", SubmittedLedger.record(tmp.getRoot(), "run-a", "other", 5));
		assertEquals(e.digestHex, SubmittedLedger.get(tmp.getRoot(), "run-a").digestHex);

		EvidenceMessages.Asked a = ask("run-a", Evidence.Kind.FULL_RUN_LOG, 9_000L);
		assertTrue(EvidenceInbox.add(tmp.getRoot(), a, 1));
		assertFalse("the same request is not stored twice", EvidenceInbox.add(tmp.getRoot(), a, 2));
		assertEquals(1, EvidenceInbox.pending(tmp.getRoot()));
		EvidenceInbox.Entry stored = EvidenceInbox.list(tmp.getRoot()).get(0);
		assertEquals("run-a", stored.asked.runId);
		assertEquals(9_000L, stored.asked.request.deadlineMillis);
		EvidenceMessages.Asked back = EvidenceMessages.askedFromEnvelope(EvidenceMessages.askedToEnvelope(a));
		assertNotNull(back);
		assertEquals("run-a", back.runId);
		assertNull("a 1.0 request names no run, so it cannot be answered", EvidenceMessages.askedFromEnvelope(EvidenceMessages.requestToEnvelope(a.request)));
	}

	@Test
	public void agreeingQueuesTheLogForSendingAndDecliningQueuesNothing() throws Exception {
		submit("run-a");
		EvidenceMessages.Asked a = ask("run-a", Evidence.Kind.FULL_RUN_LOG, 9_000L);
		EvidenceInbox.add(tmp.getRoot(), a, 1);
		assertEquals(EvidenceFlow.Result.DECLINED, EvidenceFlow.answer(tmp.getRoot(), a, false, 100));
		assertEquals("answering closes the request", 0, EvidenceInbox.pending(tmp.getRoot()));
		assertEquals("declining sends nothing", 0, SubmissionQueue.pending(tmp.getRoot()));

		EvidenceInbox.add(tmp.getRoot(), a, 1);
		assertEquals(EvidenceFlow.Result.QUEUED, EvidenceFlow.answer(tmp.getRoot(), a, true, 100));
		assertEquals(0, EvidenceInbox.pending(tmp.getRoot()));
		Outbox box = new Outbox();
		OutboxStore.load(box, SubmissionQueue.fileIn(tmp.getRoot()));
		assertEquals(1, box.size());
		Envelope data = box.items().get(0).envelope;
		assertEquals(EvidenceMessages.DATA_SCHEMA, data.schema);
		assertTrue(data.payload.contains("digest=" + SubmittedLedger.get(tmp.getRoot(), "run-a").digestHex));
	}

	@Test
	public void lateChangedUnsubmittedAndUnsupportedRequestsAreAnsweredWithNothingSent() throws Exception {
		submit("run-a");
		assertEquals("past its deadline", EvidenceFlow.Result.EXPIRED, EvidenceFlow.answer(tmp.getRoot(), ask("run-a", Evidence.Kind.FULL_RUN_LOG, 50), true, 100));
		assertEquals("a run never sent from this device", EvidenceFlow.Result.NOT_SUBMITTED, EvidenceFlow.answer(tmp.getRoot(), ask("run-zzz", Evidence.Kind.FULL_RUN_LOG, 9_000), true, 100));
		assertEquals(EvidenceFlow.Result.UNSUPPORTED, EvidenceFlow.answer(tmp.getRoot(), ask("run-a", Evidence.Kind.FLOOR_REPLAY_DATA, 9_000), true, 100));
		try (FileOutputStream out = new FileOutputStream(new StatsStore(tmp.getRoot()).logFile("run-a"), true)){
			out.write(1);
		}
		assertEquals("the log changed after it was submitted", EvidenceFlow.Result.LOG_CHANGED, EvidenceFlow.answer(tmp.getRoot(), ask("run-a", Evidence.Kind.FULL_RUN_LOG, 9_000), true, 100));
		assertEquals(0, SubmissionQueue.pending(tmp.getRoot()));
	}

	@Test
	public void requestsPastTheirDeadlineAreDropped(){
		EvidenceInbox.add(tmp.getRoot(), ask("run-a", Evidence.Kind.FULL_RUN_LOG, 1_000L), 1);
		EvidenceInbox.add(tmp.getRoot(), ask("run-b", Evidence.Kind.FULL_RUN_LOG, 9_000L), 1);
		assertEquals(0, EvidenceInbox.prune(tmp.getRoot(), 500));
		assertEquals(1, EvidenceInbox.prune(tmp.getRoot(), 2_000));
		assertEquals(1, EvidenceInbox.pending(tmp.getRoot()));
		assertEquals("run-b", EvidenceInbox.list(tmp.getRoot()).get(0).asked.runId);
	}
}
