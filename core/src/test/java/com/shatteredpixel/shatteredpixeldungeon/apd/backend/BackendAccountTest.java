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

public class BackendAccountTest {

	private static final Id PLAYER = Id.of(Id.Kind.PLAYER, "pl_abc");

	@Test
	public void exportAndDeleteRequestsRoundTripAndAreIdempotentPerPlayer(){
		for (AccountRequests.Kind k : AccountRequests.Kind.values()){
			AccountRequests.Request r = new AccountRequests.Request(k, PLAYER, 12345L);
			Envelope e = AccountRequests.toEnvelope(r);
			AccountRequests.Request back = AccountRequests.fromEnvelope(e);
			assertNotNull(back);
			assertEquals(k, back.kind);
			assertEquals(PLAYER, back.player);
			assertEquals(12345L, back.requestedAtMillis);
			assertEquals("asking twice is one request", r.idempotencyKey(), new AccountRequests.Request(k, PLAYER, 99L).idempotencyKey());
			assertTrue("may be queued: it is account data, not local-only", DataPolicy.mayLeaveDevice(e.schema));
		}
		assertNotEquals("export and delete are different requests", new AccountRequests.Request(AccountRequests.Kind.EXPORT, PLAYER, 1).idempotencyKey(),
				new AccountRequests.Request(AccountRequests.Kind.DELETE, PLAYER, 1).idempotencyKey());
	}

	@Test
	public void badAccountRequestsAreRefusedNotGuessed(){
		assertNull(AccountRequests.fromEnvelope(new Envelope("apd.other", 1, 0, "player=pl_abc\nrequestedAtMillis=1\n")));
		assertNull("a different major version is a different contract", AccountRequests.fromEnvelope(new Envelope(AccountRequests.DELETE_SCHEMA, 2, 0, "player=pl_abc\nrequestedAtMillis=1\n")));
		assertNull("a missing field", AccountRequests.fromEnvelope(new Envelope(AccountRequests.DELETE_SCHEMA, 1, 0, "player=pl_abc\n")));
		assertNull("a device id is not a player", AccountRequests.fromEnvelope(new Envelope(AccountRequests.DELETE_SCHEMA, 1, 0, "player=bad id\nrequestedAtMillis=1\n")));
		assertNotNull("a newer minor version only adds fields", AccountRequests.fromEnvelope(new Envelope(AccountRequests.DELETE_SCHEMA, 1, 3, "player=pl_abc\nrequestedAtMillis=1\nfuture=x\n")));
		try {
			new AccountRequests.Request(AccountRequests.Kind.DELETE, Id.of(Id.Kind.DEVICE, "dv_1"), 1);
			fail("a device id is not a player id");
		} catch (IllegalArgumentException expected){
		}
	}

	@Test
	public void evidenceRequestRoundTripsAndDataCarriesItsDigest(){
		Id sub = Id.of(Id.Kind.SUBMISSION, "sub_1");
		Evidence.Request r = new Evidence.Request(sub, Evidence.Kind.FULL_RUN_LOG, 5000L);
		Evidence.Request back = EvidenceMessages.requestFromEnvelope(EvidenceMessages.requestToEnvelope(r));
		assertNotNull(back);
		assertEquals(sub, back.submissionId);
		assertEquals(Evidence.Kind.FULL_RUN_LOG, back.kind);
		assertEquals(5000L, back.deadlineMillis);
		assertNull(EvidenceMessages.requestFromEnvelope(new Envelope(EvidenceMessages.REQUEST_SCHEMA, 1, 0, "submission=sub_1\nkind=NOPE\ndeadlineMillis=1\n")));

		byte[] data = "the run log bytes".getBytes();
		Envelope e = EvidenceMessages.dataToEnvelope(sub, Evidence.Kind.FULL_RUN_LOG, data);
		String digest = Canon.hex(Canon.sha256(data));
		assertTrue(e.payload.contains("digest=" + digest));
		assertTrue("the server can check the received bytes against the submitted digest", Evidence.matchesSubmission(data, digest));
		assertEquals(DataPolicy.DataClass.ACCOUNT_PRIVATE, DataPolicy.classOf(e.schema));
		assertEquals("raw logs themselves stay local-only", DataPolicy.DataClass.LOCAL_ONLY, DataPolicy.classOf("apd.run-log"));
	}
}
