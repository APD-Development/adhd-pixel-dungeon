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

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.AccountRequests;
import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;

import java.io.File;

// The player asking the server for a copy of their account data, or to delete the account. Both need a signed-in session (the request names
// the player the session belongs to, never anyone else), are queued like any other item and sent only by "Send queued runs now". Asking twice
// is the same as asking once. Neither touches the history, saves or queue on this device.
public final class AccountActions {

	public enum Result { QUEUED, ALREADY_QUEUED, NOT_SIGNED_IN, FAILED }

	private AccountActions(){}

	private static Id queueId( AccountRequests.Request r ){
		return Id.of(Id.Kind.OUTBOX, "acct_" + r.idempotencyKey());
	}

	public static Result request( File historyRoot, AccountRequests.Kind kind, long nowMillis ){
		SessionStore.Session session = new SessionStore(historyRoot).current(nowMillis);
		if (session == null) return Result.NOT_SIGNED_IN;
		AccountRequests.Request r = new AccountRequests.Request(kind, session.player, nowMillis);
		Id id;
		try {
			id = queueId(r);
		} catch (IllegalArgumentException e){
			return Result.FAILED;
		}
		if (SubmissionQueue.hasItem(historyRoot, id)) return Result.ALREADY_QUEUED;
		return SubmissionQueue.enqueueEnvelope(historyRoot, id, AccountRequests.toEnvelope(r), nowMillis) ? Result.QUEUED : Result.FAILED;
	}
}
