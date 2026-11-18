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

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.AccountRequests;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Canon;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Evidence;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.EvidenceMessages;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.HttpGateway;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Leaderboard;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.RunSubmission;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.RunSubmissionCodec;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.SubmissionPolicy;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.TrustLevel;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The whole client path against a small reference server: queue a finished run, send it over HTTP, see it ranked, answer an evidence request,
// and delete the account. The server here is the contract in code, not a product: it takes the player from the session token (never from the
// submission), dedupes by the idempotency key and decides trust itself.
public class EndToEndSubmissionTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private HttpServer server;
	private String url;
	private final Set<String> keysSeen = new HashSet<>();
	private final List<Leaderboard.Entry> stored = new ArrayList<>();
	private final Map<String, String> sessions = new HashMap<>();
	private int received;
	private final List<Envelope> serverRequests = new ArrayList<>();

	@Before
	public void start() throws Exception {
		sessions.put("tok-ann", "pl_ann");
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v1/envelope", this::handle);
		server.createContext("/v1/requests", ex -> {
			String auth = ex.getRequestHeaders().getFirst("Authorization");
			if (auth == null || !sessions.containsKey(auth.replace("Bearer ", ""))){
				reply(ex, 401);
				return;
			}
			byte[] body = com.shatteredpixel.shatteredpixeldungeon.apd.backend.MailboxCodec.encode(serverRequests).getBytes(StandardCharsets.UTF_8);
			ex.sendResponseHeaders(200, body.length);
			ex.getResponseBody().write(body);
			ex.close();
		});
		server.start();
		url = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@After
	public void stop(){
		server.stop(0);
	}

	private void reply( HttpExchange ex, int code ) throws java.io.IOException {
		ex.sendResponseHeaders(code, -1);
		ex.close();
	}

	private void handle( HttpExchange ex ) throws java.io.IOException {
		String auth = ex.getRequestHeaders().getFirst("Authorization");
		String player = auth == null ? null : sessions.get(auth.replace("Bearer ", ""));
		if (player == null){
			reply(ex, 401);
			return;
		}
		String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		String schema = ex.getRequestHeaders().getFirst("X-APD-Schema");
		String[] v = ex.getRequestHeaders().getFirst("X-APD-Version").split("\\.");
		Envelope e = new Envelope(schema, Integer.parseInt(v[0]), Integer.parseInt(v[1]), body);
		String key = ex.getRequestHeaders().getFirst("Idempotency-Key");
		received++;
		if (!keysSeen.add(key + "|" + schema)){
			reply(ex, 409);
			return;
		}
		if (RunSubmission.SCHEMA.equals(schema)){
			RunSubmission s = RunSubmissionCodec.fromEnvelope(e);
			if (s == null || SubmissionPolicy.check(s, true) != SubmissionPolicy.Verdict.OK){
				reply(ex, 422);
				return;
			}
			stored.add(new Leaderboard.Entry(Id.of(Id.Kind.PLAYER, player), Id.of(Id.Kind.SUBMISSION, "sub_" + s.runId.value), s, TrustLevel.SERVER_RECEIVED, 1L));
		} else if (AccountRequests.DELETE_SCHEMA.equals(schema)){
			AccountRequests.Request r = AccountRequests.fromEnvelope(e);
			if (r == null || !r.player.value.equals(player)){
				reply(ex, 403);
				return;
			}
			stored.removeIf(x -> x.player.value.equals(player));
		} else if (EvidenceMessages.DATA_SCHEMA.equals(schema)){
			Map<String, String> m = new HashMap<>();
			for (String line : body.split("\n")) if (line.indexOf('=') > 0) m.put(line.substring(0, line.indexOf('=')), line.substring(line.indexOf('=') + 1));
			byte[] data = java.util.Base64.getDecoder().decode(m.get("data"));
			Leaderboard.Entry mine = null;
			for (Leaderboard.Entry x : stored) if (("sub_" + x.run.runId.value).equals(m.get("submission"))) mine = x;
			if (mine == null || !Evidence.matchesSubmission(data, mine.run.evidenceDigestHex)){
				reply(ex, 422);
				return;
			}
			stored.remove(mine);
			stored.add(new Leaderboard.Entry(mine.player, mine.submission, mine.run, TrustLevel.SERVER_CHECKED, mine.receivedAtMillis));
		} else {
			reply(ex, 400);
			return;
		}
		reply(ex, 201);
	}

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

	@Test
	public void aQueuedRunIsSentRankedCheckedAndDeletedWithTheAccount() throws Exception {
		log("run-a");
		assertEquals(SubmissionQueue.Result.QUEUED, SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "4.0.8-apd", 1).result);

		//signed out: the server asks for a sign-in, the run stays queued and nothing is ranked
		Outbox.FlushResult waiting = SubmissionQueue.flush(tmp.getRoot(), new HttpGateway(url, null, 3000), 1_000_000_000L);
		assertEquals(1, waiting.kept);
		assertEquals(1, SubmissionQueue.pending(tmp.getRoot()));
		assertTrue(stored.isEmpty());

		//signed in: send what was waiting (after the back-off)
		HttpGateway ann = new HttpGateway(url, () -> "tok-ann", 3000);
		assertEquals(1, SubmissionQueue.flush(tmp.getRoot(), ann, 2_000_000_000L).sent);
		assertEquals(0, SubmissionQueue.pending(tmp.getRoot()));
		assertEquals(1, stored.size());
		assertEquals("the server took the player from the session, not from the submission", "pl_ann", stored.get(0).player.value);
		Leaderboard.Board board = new Leaderboard.Board("b", "exotic30", 0, 3, TrustLevel.SERVER_RECEIVED);
		assertEquals(1, Leaderboard.rank(board, stored).size());
		Leaderboard.Board strict = new Leaderboard.Board("s", "exotic30", 0, 3, TrustLevel.SERVER_CHECKED);
		assertEquals("not ranked on the strict board until evidence checks out", 0, Leaderboard.rank(strict, stored).size());

		//a lost reply: the same item sent again is not stored twice
		Envelope again = RunSubmissionCodec.toEnvelope(stored.get(0).run);
		assertEquals(Gateway.Status.OK, ann.send("ob_run-a", again).status);
		assertEquals(1, stored.size());

		//the server asks for the full log; the player agrees; the data matches the submitted digest
		File logFile = new StatsStore(tmp.getRoot()).logFile("run-a");
		byte[] bytes = Files.readAllBytes(logFile.toPath());
		Evidence.Request req = new Evidence.Request(Id.of(Id.Kind.SUBMISSION, "sub_run-a"), Evidence.Kind.FULL_RUN_LOG, 10_000L);
		String digest = Canon.hex(Canon.sha256(bytes));
		assertEquals(Evidence.Decision.NEEDS_CONSENT, Evidence.decide(req, false, 5_000L, digest, stored.get(0).run.evidenceDigestHex));
		assertEquals(Evidence.Decision.PROVIDE, Evidence.decide(req, true, 5_000L, digest, stored.get(0).run.evidenceDigestHex));
		assertEquals(Gateway.Status.OK, ann.send("ev_run-a", EvidenceMessages.dataToEnvelope(req.submissionId, req.kind, bytes)).status);
		assertEquals(TrustLevel.SERVER_CHECKED, stored.get(0).trust);
		assertEquals("now ranked on the strict board", 1, Leaderboard.rank(strict, stored).size());
		assertEquals("data that does not match the submitted digest is refused", Gateway.Status.REJECTED,
				ann.send("ev2", EvidenceMessages.dataToEnvelope(req.submissionId, req.kind, "other".getBytes())).status);

		//deleting the account removes the player's entries, and only for the signed-in player
		Id other = Id.of(Id.Kind.PLAYER, "pl_bob");
		assertEquals("someone else's deletion is refused", Gateway.Status.REJECTED,
				ann.send("del_pl_bob", AccountRequests.toEnvelope(new AccountRequests.Request(AccountRequests.Kind.DELETE, other, 1))).status);
		assertEquals(1, stored.size());
		AccountRequests.Request mine = new AccountRequests.Request(AccountRequests.Kind.DELETE, Id.of(Id.Kind.PLAYER, "pl_ann"), 1);
		assertEquals(Gateway.Status.OK, ann.send(mine.idempotencyKey(), AccountRequests.toEnvelope(mine)).status);
		assertTrue(stored.isEmpty());
		assertTrue("the player's own history is untouched on the device", logFile.exists());
	}

	@Test
	public void theServerAsksForEvidenceTheClientFetchesItThePlayerAgreesAndTheDataIsChecked() throws Exception {
		log("run-a");
		SubmissionQueue.enqueueRun(tmp.getRoot(), "run-a", "4.0.8-apd", 1);
		HttpGateway ann = new HttpGateway(url, () -> "tok-ann", 3000);
		assertEquals(1, SubmissionQueue.flush(tmp.getRoot(), ann, 2_000_000_000L).sent);

		//nothing asked yet
		assertEquals(0, BackendClient.collectRequests(tmp.getRoot(), ann, 3));
		//signed out: the client is told it could not ask, and nothing changes
		assertEquals(-1, BackendClient.collectRequests(tmp.getRoot(), new HttpGateway(url, null, 3000), 3));

		Evidence.Request req = new Evidence.Request(Id.of(Id.Kind.SUBMISSION, "sub_run-a"), Evidence.Kind.FULL_RUN_LOG, 100_000L);
		serverRequests.add(EvidenceMessages.askedToEnvelope(new EvidenceMessages.Asked(req, "run-a")));
		serverRequests.add(EvidenceMessages.requestToEnvelope(req));   //an old-style request that names no run: ignored
		assertEquals("one readable request arrived", 1, BackendClient.collectRequests(tmp.getRoot(), ann, 4));
		assertEquals("the same request is not added twice", 0, BackendClient.collectRequests(tmp.getRoot(), ann, 5));
		assertEquals(1, EvidenceInbox.pending(tmp.getRoot()));
		assertEquals("asking sent nothing and queued nothing", 0, SubmissionQueue.pending(tmp.getRoot()));

		EvidenceInbox.Entry entry = EvidenceInbox.list(tmp.getRoot()).get(0);
		assertEquals(EvidenceFlow.Result.QUEUED, EvidenceFlow.answer(tmp.getRoot(), entry.asked, true, 6));
		assertEquals(1, SubmissionQueue.flush(tmp.getRoot(), ann, 3_000_000_000L).sent);
		assertEquals("the server accepted the data because it matches what was submitted", TrustLevel.SERVER_CHECKED, stored.get(0).trust);
	}
}
