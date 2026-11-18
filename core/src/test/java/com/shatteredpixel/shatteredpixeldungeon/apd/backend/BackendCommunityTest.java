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

public class BackendCommunityTest {

	private static Id player( String s ){
		return Id.of(Id.Kind.PLAYER, s);
	}

	private static RunSubmission run( String id, String ruleset, int rev, int diff, long score ){
		return new RunSubmission(Id.of(Id.Kind.RUN, id), ruleset, rev, diff, 0, "WARRIOR", RunSubmission.Outcome.DIED, 7, 1000, 60_000, score, "4.0.8-apd", 1, 8, Canon.hex(Canon.sha256(id.getBytes())));
	}

	@Test
	public void theSubmissionContractIsExactlyTheseFields() throws Exception {
		Set<String> fields = new TreeSet<>();
		for (java.lang.reflect.Field f : RunSubmission.class.getDeclaredFields()){
			if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) fields.add(f.getName());
		}
		assertEquals("changing the submission contract must be a deliberate, reviewed change (privacy table and docs/APD-BACKEND.md too)",
				new TreeSet<>(Arrays.asList("runId", "rulesetId", "balanceRevision", "difficulty", "challenges", "heroClass", "outcome", "depth", "turns", "durationMillis",
						"scoreClaim", "appBuild", "telemetryMajor", "telemetryMinor", "evidenceDigestHex")), fields);
		assertFalse("the submitter's identity is attached by the server, never claimed", fields.contains("playerId") || fields.contains("player"));
	}

	@Test
	public void policyRefusesWhatCannotBeRight(){
		RunSubmission ok = run("r1", "rs", 1, 3, 100);
		assertEquals(SubmissionPolicy.Verdict.OK, SubmissionPolicy.check(ok, true));
		assertEquals("an unfinished run is never submitted (hidden information)", SubmissionPolicy.Verdict.NOT_FINISHED, SubmissionPolicy.check(ok, false));
		assertEquals(SubmissionPolicy.Verdict.DIFFICULTY_OUT_OF_RANGE, SubmissionPolicy.check(run("r2", "rs", 1, 6, 1), true));
		assertEquals(SubmissionPolicy.Verdict.IMPOSSIBLE_VALUES, SubmissionPolicy.check(run("r3", "rs", 1, 3, -1), true));
		assertEquals(SubmissionPolicy.Verdict.UNKNOWN_RULESET, SubmissionPolicy.check(run("r4", "unknown", 1, 3, 1), true));
		RunSubmission badDigest = new RunSubmission(Id.of(Id.Kind.RUN, "r5"), "rs", 1, 3, 0, "WARRIOR", RunSubmission.Outcome.WON, 26, 1, 1, 1, "b", 1, 8, "nothex");
		assertEquals(SubmissionPolicy.Verdict.BAD_EVIDENCE_DIGEST, SubmissionPolicy.check(badDigest, true));
	}

	@Test
	public void theWireFormIsLosslessAndForwardCompatible(){
		RunSubmission s = run("r1", "rs", 4, 2, 321);
		Envelope e = RunSubmissionCodec.toEnvelope(s);
		RunSubmission back = RunSubmissionCodec.fromEnvelope(e);
		assertArrayEquals(s.canonical(), back.canonical());
		Envelope newer = new Envelope(e.schema, e.major, e.minor + 1, e.payload + "futureField=1\n");
		assertNotNull("a newer minor adds fields: unknown ones are skipped", RunSubmissionCodec.fromEnvelope(newer));
		assertNull("a different major is refused", RunSubmissionCodec.fromEnvelope(new Envelope(e.schema, e.major + 1, 0, e.payload)));
		assertNull("a missing field is refused", RunSubmissionCodec.fromEnvelope(new Envelope(e.schema, e.major, e.minor, e.payload.replace("difficulty=2\n", ""))));
		assertNull("another schema is not a submission", RunSubmissionCodec.fromEnvelope(new Envelope("apd.report", 1, 0, e.payload)));
	}

	@Test
	public void rankingIsBestPerPlayerAndHonoursTrustAndBoardRules(){
		Leaderboard.Board board = new Leaderboard.Board("normal", "rs", 1, 3, TrustLevel.SERVER_CHECKED);
		List<Leaderboard.Entry> all = new ArrayList<>();
		all.add(new Leaderboard.Entry(player("a"), Id.of(Id.Kind.SUBMISSION, "s1"), run("r1", "rs", 1, 3, 100), TrustLevel.SERVER_CHECKED, 10));
		all.add(new Leaderboard.Entry(player("a"), Id.of(Id.Kind.SUBMISSION, "s2"), run("r2", "rs", 1, 3, 300), TrustLevel.SERVER_CHECKED, 20));
		all.add(new Leaderboard.Entry(player("b"), Id.of(Id.Kind.SUBMISSION, "s3"), run("r3", "rs", 1, 3, 300), TrustLevel.REPLAY_VERIFIED, 15));
		all.add(new Leaderboard.Entry(player("c"), Id.of(Id.Kind.SUBMISSION, "s4"), run("r4", "rs", 1, 3, 9999), TrustLevel.SERVER_RECEIVED, 5));   //not checked
		all.add(new Leaderboard.Entry(player("d"), Id.of(Id.Kind.SUBMISSION, "s5"), run("r5", "rs", 2, 3, 9999), TrustLevel.REPLAY_VERIFIED, 5));    //other balance revision
		all.add(new Leaderboard.Entry(player("e"), Id.of(Id.Kind.SUBMISSION, "s6"), run("r6", "rs", 1, 4, 9999), TrustLevel.REPLAY_VERIFIED, 5));    //other difficulty
		all.add(new Leaderboard.Entry(player("f"), Id.of(Id.Kind.SUBMISSION, "s7"), run("r7", "other", 1, 3, 9999), TrustLevel.REPLAY_VERIFIED, 5)); //other ruleset
		List<Leaderboard.Entry> ranked = Leaderboard.rank(board, all);
		assertEquals(2, ranked.size());
		assertEquals("b ties a on score but submitted earlier", "b", ranked.get(0).player.value);
		assertEquals("a", ranked.get(1).player.value);
		assertEquals("a's best run counts, not the first", 300, ranked.get(1).run.scoreClaim);
		List<Leaderboard.Entry> strict = Leaderboard.rank(new Leaderboard.Board("strict", "rs", 1, 0, TrustLevel.REPLAY_VERIFIED), all);
		assertEquals("a stricter board counts only replay-verified runs, across difficulties", 2, strict.size());
		assertEquals("e", strict.get(0).player.value);
	}

	@Test
	public void oneAccountOneVoteAndRetriesAreHarmless(){
		Voting.Round round = new Voting.Round(Id.of(Id.Kind.VOTE_ROUND, "vr_1"), new TreeSet<>(Arrays.asList("x", "y", "z")), 1, 100, 200);
		Voting v = new Voting(round);
		assertEquals(Voting.Cast.NOT_OPEN, v.cast(player("a"), Collections.singleton("x"), 50));
		assertEquals(Voting.Cast.OK, v.cast(player("a"), Collections.singleton("x"), 150));
		assertEquals(Voting.Cast.OK, v.cast(player("a"), Collections.singleton("x"), 151));    //a retry
		assertEquals(Voting.Cast.OK, v.cast(player("b"), Collections.singleton("y"), 152));
		assertEquals(Voting.Cast.OK, v.cast(player("a"), Collections.singleton("z"), 160));    //a changes their mind: replaces, never adds
		assertEquals(Voting.Cast.UNKNOWN_OPTION, v.cast(player("c"), Collections.singleton("nope"), 160));
		assertEquals(Voting.Cast.TOO_MANY_CHOICES, v.cast(player("c"), new TreeSet<>(Arrays.asList("x", "y")), 160));
		assertEquals(Voting.Cast.EMPTY, v.cast(player("c"), new HashSet<String>(), 160));
		assertEquals(Voting.Cast.CLOSED, v.cast(player("c"), Collections.singleton("x"), 200));
		Map<String, Integer> t = v.tally();
		assertEquals(Integer.valueOf(0), t.get("x"));
		assertEquals(Integer.valueOf(1), t.get("y"));
		assertEquals(Integer.valueOf(1), t.get("z"));
		assertEquals(2, v.voters());
	}

	@Test
	public void auditLogDetectsEditsAndRemovals(){
		AuditLog log = new AuditLog();
		log.append(1, "dev", "approve", "ct_a@1.0.0", "ok");
		log.append(2, "dev", "grant", "gr_1", "ok");
		log.append(3, "mod", "revoke", "ct_a@1.0.0", "unsafe");
		assertTrue(AuditLog.verify(log.entries(), log.headHashHex()));
		String head = log.headHashHex();
		List<AuditLog.Entry> cut = new ArrayList<>(log.entries());
		cut.remove(1);
		assertFalse("a removed entry breaks the chain", AuditLog.verify(cut, null));
		assertFalse("truncating the end is caught by the known head", AuditLog.verify(log.entries().subList(0, 2), head));
		assertTrue(AuditLog.verify(new ArrayList<AuditLog.Entry>(), null));
	}

	@Test
	public void reportsAreBoundedAndRepeatsAreNoOps(){
		Report.Inbox inbox = new Report.Inbox();
		StringBuilder big = new StringBuilder();
		for (int i = 0; i < 5000; i++) big.append('x');
		Report r1 = new Report(Id.of(Id.Kind.REPORT, "rp_1"), player("a"), Report.TargetKind.CONTENT, "ct_a", Report.Reason.OFFENSIVE, big.toString(), 1);
		assertEquals(Report.MAX_NOTE, r1.note.length());
		assertTrue(inbox.add(r1));
		assertFalse(inbox.add(new Report(Id.of(Id.Kind.REPORT, "rp_2"), player("a"), Report.TargetKind.CONTENT, "ct_a", Report.Reason.SPAM, "", 2)));
		assertTrue("another reporter counts", inbox.add(new Report(Id.of(Id.Kind.REPORT, "rp_3"), player("b"), Report.TargetKind.CONTENT, "ct_a", Report.Reason.SPAM, "", 3)));
		assertEquals(2, inbox.count());
	}

	@Test
	public void syncMergeIsDeterministicAndDeletesStick(){
		SyncRecord a = new SyncRecord("settings", "k", 5, 100, "dev-a", false, "A");
		SyncRecord b = new SyncRecord("settings", "k", 5, 100, "dev-b", false, "B");
		assertSame("same version and time: the device id breaks the tie the same way on both sides", SyncRecord.merge(a, b, SyncRecord.Policy.LAST_WRITE_WINS), SyncRecord.merge(b, a, SyncRecord.Policy.LAST_WRITE_WINS));
		SyncRecord newer = new SyncRecord("settings", "k", 6, 1, "dev-a", false, "N");
		assertSame(newer, SyncRecord.merge(a, newer, SyncRecord.Policy.LAST_WRITE_WINS));
		SyncRecord tomb = new SyncRecord("settings", "k", 7, 1, "dev-a", true, "");
		assertTrue("a delete wins over an older copy, so a deleted record cannot come back", SyncRecord.merge(a, tomb, SyncRecord.Policy.LAST_WRITE_WINS).deleted);
		assertSame("server-authoritative records ignore the device's copy", a, SyncRecord.merge(newer, a, SyncRecord.Policy.SERVER_WINS));
		assertSame(a, SyncRecord.merge(null, a, SyncRecord.Policy.LAST_WRITE_WINS));
	}

	@Test
	public void accountsKeepOnePlayerAcrossCredentials(){
		Accounts acc = new Accounts(new java.security.SecureRandom());
		Accounts.Credential g = new Accounts.Credential("provider-a", "u1"), m = new Accounts.Credential("provider-b", "u9");
		Id p = acc.signIn(g);
		assertEquals("signing in again is the same player", p, acc.signIn(g));
		assertEquals(Accounts.Link.LAST_CREDENTIAL, acc.unlink(p, g));
		assertEquals(Accounts.Link.OK, acc.link(p, m));
		assertEquals(Accounts.Link.ALREADY_LINKED_TO_THIS_PLAYER, acc.link(p, m));
		assertEquals("the new provider leads to the same player id", p, acc.signIn(m));
		Id other = acc.signIn(new Accounts.Credential("provider-a", "u2"));
		assertEquals("a credential is never moved to another player silently", Accounts.Link.BELONGS_TO_ANOTHER_PLAYER, acc.link(other, m));
		assertEquals(Accounts.Link.OK, acc.unlink(p, g));
		assertEquals(1, acc.credentials(p));
		LocalProfile lp = new LocalProfile(Id.of(Id.Kind.DEVICE, "dv_1"));
		assertFalse("no account is needed to have a profile", lp.signedIn());
		lp.signIn(p);
		assertEquals(p, lp.playerId());
		lp.signOut();
		assertFalse(lp.signedIn());
	}
}
