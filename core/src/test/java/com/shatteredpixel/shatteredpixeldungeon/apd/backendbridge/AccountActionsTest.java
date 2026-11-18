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
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Envelope;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Gateway;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Outbox;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.OutboxStore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.List;

public class AccountActionsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void requestsNeedASessionNameTheSessionsPlayerAndAreQueuedOnce(){
		long now = System.currentTimeMillis();
		assertEquals("no account, no request", AccountActions.Result.NOT_SIGNED_IN, AccountActions.request(tmp.getRoot(), AccountRequests.Kind.DELETE, now));
		assertEquals(0, SubmissionQueue.pending(tmp.getRoot()));

		new SessionStore(tmp.getRoot()).signIn(Id.of(Id.Kind.PLAYER, "pl_ann"), "tok", now + 60_000);
		assertEquals(AccountActions.Result.QUEUED, AccountActions.request(tmp.getRoot(), AccountRequests.Kind.DELETE, now));
		assertEquals("asking twice is asking once", AccountActions.Result.ALREADY_QUEUED, AccountActions.request(tmp.getRoot(), AccountRequests.Kind.DELETE, now + 5));
		assertEquals("export is a different request", AccountActions.Result.QUEUED, AccountActions.request(tmp.getRoot(), AccountRequests.Kind.EXPORT, now));
		assertEquals(2, SubmissionQueue.pending(tmp.getRoot()));

		final List<AccountRequests.Request> sent = new ArrayList<>();
		Gateway ok = new Gateway(){
			public Response send( String key, Envelope e ){
				AccountRequests.Request r = AccountRequests.fromEnvelope(e);
				assertEquals("the key is the request's own idempotency key", "acct_" + r.idempotencyKey(), key);
				sent.add(r);
				return new Response(Status.OK, "");
			}
		};
		assertEquals(2, SubmissionQueue.flush(tmp.getRoot(), ok, now + 1_000_000_000L, true).sent);
		assertEquals(2, sent.size());
		for (AccountRequests.Request r : sent) assertEquals("pl_ann", r.player.value);
		assertEquals(0, SubmissionQueue.pending(tmp.getRoot()));
	}
}
