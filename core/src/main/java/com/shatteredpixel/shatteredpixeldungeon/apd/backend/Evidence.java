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

// Evidence a server may ask for to verify a run. Only provided with the player's consent, before the deadline, and only if the local log still matches the submitted digest.
public final class Evidence {

	public enum Kind { FULL_RUN_LOG, FLOOR_REPLAY_DATA }

	public static final class Request {
		public final Id submissionId;
		public final Kind kind;
		public final long deadlineMillis;

		public Request( Id submissionId, Kind kind, long deadlineMillis ){
			if (submissionId.kind != Id.Kind.SUBMISSION) throw new IllegalArgumentException("not a submission id");
			this.submissionId = submissionId;
			this.kind = kind;
			this.deadlineMillis = deadlineMillis;
		}
	}

	public enum Decision { PROVIDE, NEEDS_CONSENT, EXPIRED, LOG_CHANGED }

	private Evidence(){}

	//what the app does with a request: it provides the data only after the player agreed, before the deadline, and only if the local log still
	//hashes to the digest that was submitted
	public static Decision decide( Request r, boolean playerAgreed, long nowMillis, String localLogDigestHex, String submittedDigestHex ){
		if (nowMillis > r.deadlineMillis) return Decision.EXPIRED;
		if (localLogDigestHex == null || !localLogDigestHex.equals(submittedDigestHex)) return Decision.LOG_CHANGED;
		return playerAgreed ? Decision.PROVIDE : Decision.NEEDS_CONSENT;
	}

	//server side: do the received bytes match the digest that was submitted?
	public static boolean matchesSubmission( byte[] received, String submittedDigestHex ){
		return Canon.hex(Canon.sha256(received)).equals(submittedDigestHex);
	}
}
