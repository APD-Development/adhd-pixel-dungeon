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

package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

//The Outbox against a reference in-memory server: the executable description of what a real server must do with the contracts.
public class BackendOutboxTest {

	private static RunSubmission run( String id, long score ){
		return new RunSubmission(Id.of(Id.Kind.RUN, id), "rs", 1, 3, 0, "MAGE", RunSubmission.Outcome.WON, 26, 5000, 600_000, score, "4.0.8-apd", 1, 8, Canon.hex(Canon.sha256(id.getBytes())));
	}

	//accepts submissions: validates with the shared policy, de-duplicates by idempotency key, and stamps trust itself (the client's say-so is ignored)
	static final class ReferenceServer implements Gateway {
		final Id sessionPlayer;
		final Map<String, Leaderboard.Entry> stored = new LinkedHashMap<>();
		final Set<String> seenKeys = new HashSet<>();
		boolean reachable = true, failBeforeProcessing, loseReply;
		long clock = 1000;

		ReferenceServer( Id sessionPlayer ){
			this.sessionPlayer = sessionPlayer;
		}

		@Override
		public Response send( String key, Envelope e ){
			if (!reachable) return new Response(Status.OFFLINE, "no connection");
			if (failBeforeProcessing){
				failBeforeProcessing = false;
				return new Response(Status.RETRY_LATER, "busy");
			}
			boolean repeat = !seenKeys.add(key);
			if (!repeat){
				RunSubmission s = RunSubmissionCodec.fromEnvelope(e);
				if (s == null) return reject(key, "unreadable");
				SubmissionPolicy.Verdict v = SubmissionPolicy.check(s, true);
				if (v != SubmissionPolicy.Verdict.OK) return reject(key, v.name());
				stored.put(key, new Leaderboard.Entry(sessionPlayer, Id.of(Id.Kind.SUBMISSION, "sub_" + key), s, TrustLevel.SERVER_RECEIVED, clock++));
			}
			if (loseReply){
				loseReply = false;
				return new Response(Status.OFFLINE, "reply lost");   //the server did the work; the client never heard
			}
			return new Response(Status.OK, repeat ? "already had it" : "stored");
		}

		private Response reject( String key, String why ){
			seenKeys.remove(key);
			return new Response(Status.REJECTED, why);
		}
	}

	private static Id box( int n ){
		return Id.of(Id.Kind.OUTBOX, "ob_" + n);
	}

	@Test
	public void offlinePlayKeepsEverythingQueuedAndNothingBlocks(){
		ReferenceServer server = new ReferenceServer(Id.of(Id.Kind.PLAYER, "pl_a"));
		server.reachable = false;
		Outbox outbox = new Outbox();
		outbox.enqueue(box(1), RunSubmissionCodec.toEnvelope(run("r1", 10)), 0);
		outbox.enqueue(box(2), RunSubmissionCodec.toEnvelope(run("r2", 20)), 0);
		Outbox.FlushResult r = outbox.flush(server, 0);
		assertEquals(0, r.sent);
		assertEquals(2, r.kept);
		assertEquals("an outage is not a failure of the items", 2, outbox.size());
		assertEquals("backed off, not hammering", 0, outbox.flush(server, 1000).sent);
		server.reachable = true;
		Outbox.FlushResult later = outbox.flush(server, 10 * Backoff.FIRST_MILLIS);
		assertEquals(2, later.sent);
		assertEquals(0, outbox.size());
		assertEquals(2, server.stored.size());
	}

	@Test
	public void aLostReplyDoesNotDoubleSubmit(){
		ReferenceServer server = new ReferenceServer(Id.of(Id.Kind.PLAYER, "pl_a"));
		server.loseReply = true;
		Outbox outbox = new Outbox();
		outbox.enqueue(box(1), RunSubmissionCodec.toEnvelope(run("r1", 10)), 0);
		outbox.flush(server, 0);
		assertEquals("the server stored it; the client does not know", 1, server.stored.size());
		assertEquals(1, outbox.size());
		Outbox.FlushResult r = outbox.flush(server, Backoff.MAX_MILLIS);
		assertEquals(1, r.sent);
		assertEquals("the retry used the same key and was recognised", 1, server.stored.size());
		outbox.enqueue(box(1), RunSubmissionCodec.toEnvelope(run("r1", 10)), 0);
		assertEquals("queuing the same item again is a no-op", 1, outbox.size());
	}

	@Test
	public void permanentRefusalsAreDroppedAndExplained(){
		ReferenceServer server = new ReferenceServer(Id.of(Id.Kind.PLAYER, "pl_a"));
		Outbox outbox = new Outbox();
		RunSubmission impossible = new RunSubmission(Id.of(Id.Kind.RUN, "r9"), "rs", 1, 9, 0, "MAGE", RunSubmission.Outcome.DIED, 3, 1, 1, 1, "b", 1, 8, Canon.hex(Canon.sha256("x".getBytes())));
		outbox.enqueue(box(1), RunSubmissionCodec.toEnvelope(impossible), 0);
		Outbox.FlushResult r = outbox.flush(server, 0);
		assertEquals(1, r.rejected);
		assertEquals(0, outbox.size());
		assertEquals("DIFFICULTY_OUT_OF_RANGE", r.rejectedDetails.get(0));
		assertTrue(server.stored.isEmpty());
	}

	@Test
	public void temporaryTroubleRetriesWithGrowingDelaysAndAGatewayThatThrowsIsSurvived(){
		Outbox outbox = new Outbox();
		outbox.enqueue(box(1), RunSubmissionCodec.toEnvelope(run("r1", 10)), 0);
		Gateway boom = new Gateway(){
			public Response send( String k, Envelope e ){
				throw new IllegalStateException("bug in transport");
			}
		};
		Outbox.FlushResult r = outbox.flush(boom, 0);
		assertEquals(1, r.kept);
		assertEquals(1, outbox.size());
		assertEquals(Backoff.delayMillis(1), outbox.items().get(0).nextAttemptAtMillis);
		assertTrue(Backoff.delayMillis(2) == 2 * Backoff.delayMillis(1));
		assertEquals("capped", Backoff.MAX_MILLIS, Backoff.delayMillis(60));
		assertEquals(0, Backoff.delayMillis(0));
	}

	@Test
	public void theServerAttachesTheIdentityAndTrustItselfAndRankingUsesOnlyWhatItChecked(){
		Id session = Id.of(Id.Kind.PLAYER, "pl_session");
		ReferenceServer server = new ReferenceServer(session);
		Outbox outbox = new Outbox();
		outbox.enqueue(box(1), RunSubmissionCodec.toEnvelope(run("r1", 500)), 0);
		outbox.flush(server, 0);
		Leaderboard.Entry e = server.stored.values().iterator().next();
		assertEquals("the player is the authenticated one", session, e.player);
		assertEquals("a fresh submission is only RECEIVED", TrustLevel.SERVER_RECEIVED, e.trust);
		Leaderboard.Board strict = new Leaderboard.Board("n", "rs", 1, 3, TrustLevel.SERVER_CHECKED);
		assertTrue("not on a board that wants a checked run until the server checks it", Leaderboard.rank(strict, new ArrayList<>(server.stored.values())).isEmpty());
		Leaderboard.Entry checked = new Leaderboard.Entry(e.player, e.submission, e.run, TrustLevel.SERVER_CHECKED, e.receivedAtMillis);
		assertEquals(1, Leaderboard.rank(strict, Collections.singletonList(checked)).size());
	}

    @Test
    public void malformedGatewayRepliesKeepQueuedItemsAndBackOff(){
        for (Gateway gateway : Arrays.asList(
                (Gateway)(key, envelope) -> null,
                (Gateway)(key, envelope) -> new Gateway.Response(null, "bad transport"))) {
            Outbox outbox = new Outbox();
            outbox.enqueue(box(1), RunSubmissionCodec.toEnvelope(run("r1",10)), 0);
            Outbox.FlushResult result = outbox.flush(gateway,0);
            assertEquals(0,result.sent);
            assertEquals(0,result.rejected);
            assertEquals(1,result.kept);
            assertEquals(1,outbox.size());
            assertEquals(1,outbox.items().get(0).attempts);
            assertEquals(Backoff.delayMillis(1),outbox.items().get(0).nextAttemptAtMillis);
        }
    }

    @Test
    public void malformedReplySurvivesPersistenceAndThenSubmitsOnce() throws Exception {
        Outbox outbox = new Outbox();
        outbox.enqueue(box(1),RunSubmissionCodec.toEnvelope(run("r1",10)),0);
        outbox.enqueue(box(2),RunSubmissionCodec.toEnvelope(run("r2",20)),0);
        assertEquals(2,outbox.flush((key,envelope)->null,0).kept);
        java.io.File file=java.io.File.createTempFile("apd-outbox-response-", ".txt");
        try {
            OutboxStore.save(outbox,file);
            Outbox restored=new Outbox();
            assertEquals(2,OutboxStore.load(restored,file).items);
            ReferenceServer server=new ReferenceServer(Id.of(Id.Kind.PLAYER,"pl_a"));
            assertEquals(0,restored.flush(server,0).sent);
            assertEquals(2,restored.flush(server,Backoff.MAX_MILLIS).sent);
            assertEquals(0,restored.size());
            assertEquals(2,server.stored.size());
            assertEquals(0,restored.flush(server,Backoff.MAX_MILLIS).sent);
        } finally {
            java.nio.file.Files.deleteIfExists(file.toPath());
            java.nio.file.Files.deleteIfExists(new java.io.File(file.getPath()+".tmp").toPath());
        }
    }
}
