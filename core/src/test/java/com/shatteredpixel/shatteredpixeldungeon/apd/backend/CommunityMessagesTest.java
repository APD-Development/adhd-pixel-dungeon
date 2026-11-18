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

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

public class CommunityMessagesTest {

	private static final Id ROUND = Id.of(Id.Kind.VOTE_ROUND, "vr_1");

	@Test
	public void aBallotRoundTripsWithoutNamingTheVoter(){
		CommunityMessages.BallotRequest b = new CommunityMessages.BallotRequest(ROUND, new HashSet<>(Arrays.asList("b", "a")));
		Envelope e = CommunityMessages.ballotToEnvelope(b);
		assertFalse("the sender is the session, never a field", e.payload.contains("player") || e.payload.contains("voter"));
		CommunityMessages.BallotRequest back = CommunityMessages.ballotFromEnvelope(e);
		assertNotNull(back);
		assertEquals(ROUND, back.round);
		assertEquals(new HashSet<>(Arrays.asList("a", "b")), back.choices);
		assertEquals("revoting in the same round is the same request key", b.idempotencyKey(), new CommunityMessages.BallotRequest(ROUND, new HashSet<>(Arrays.asList("c"))).idempotencyKey());
		assertEquals(DataPolicy.DataClass.ACCOUNT_PRIVATE, DataPolicy.classOf(e.schema));
		assertNull(CommunityMessages.ballotFromEnvelope(new Envelope(CommunityMessages.BALLOT_SCHEMA, 2, 0, e.payload)));
		assertNull(CommunityMessages.ballotFromEnvelope(new Envelope(CommunityMessages.BALLOT_SCHEMA, 1, 0, "round=bad id\nchoices=a\n")));
		try {
			CommunityMessages.ballotToEnvelope(new CommunityMessages.BallotRequest(ROUND, new HashSet<>(Arrays.asList("a,b"))));
			fail("a comma would split a choice");
		} catch (IllegalArgumentException expected){
		}
	}

	@Test
	public void aReportRoundTripsWithABoundedSingleLineNote(){
		String longNote = new String(new char[Report.MAX_NOTE + 50]).replace('\0', 'x');
		CommunityMessages.ReportRequest r = new CommunityMessages.ReportRequest(Id.of(Id.Kind.REPORT, "rp_1"), Report.TargetKind.SUBMISSION, "sub_9", Report.Reason.CHEATING, "line one\nline two " + longNote);
		assertEquals(Report.MAX_NOTE, r.note.length());
		assertFalse(r.note.contains("\n"));
		CommunityMessages.ReportRequest back = CommunityMessages.reportFromEnvelope(CommunityMessages.reportToEnvelope(r));
		assertNotNull(back);
		assertEquals(Report.TargetKind.SUBMISSION, back.targetKind);
		assertEquals("sub_9", back.target);
		assertEquals(Report.Reason.CHEATING, back.reason);
		assertEquals(r.note, back.note);
		assertEquals("rep_rp_1", back.idempotencyKey());
		assertNull(CommunityMessages.reportFromEnvelope(new Envelope(CommunityMessages.REPORT_SCHEMA, 1, 0, "id=rp_1\ntargetKind=NOPE\ntarget=x\nreason=SPAM\n")));
		assertNull("missing target", CommunityMessages.reportFromEnvelope(new Envelope(CommunityMessages.REPORT_SCHEMA, 1, 0, "id=rp_1\ntargetKind=CONTENT\nreason=SPAM\n")));
	}
}
