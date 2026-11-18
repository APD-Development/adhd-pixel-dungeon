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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Public keys the app trusts, each for one purpose, with a validity window and a revocation list. Several keys can be active at once so keys can be rotated.
public final class TrustRoot {

	public enum Purpose { DEVELOPER_GRANT, SERVER_RECEIPT, CONTENT_APPROVAL }

	public enum Check { OK, UNKNOWN_KEY, WRONG_PURPOSE, REVOKED, NOT_YET_VALID, EXPIRED, BAD_SIGNATURE }

	private static final class Entry {
		Verifier verifier;
		Purpose purpose;
		long validFrom, validUntil;
	}

	private final Map<String, Entry> keys = new HashMap<>();
	private final Set<String> revoked = new HashSet<>();

	public TrustRoot add( String keyId, Purpose purpose, Verifier verifier, long validFromMillis, long validUntilMillis ){
		Entry e = new Entry();
		e.verifier = verifier;
		e.purpose = purpose;
		e.validFrom = validFromMillis;
		e.validUntil = validUntilMillis;
		keys.put(keyId, e);
		return this;
	}

	public TrustRoot revoke( String keyId ){
		revoked.add(keyId);
		return this;
	}

	//verifies 'signature' over 'message' as made by key 'keyId' for 'purpose' at time 'nowMillis'
	public Check check( String keyId, Purpose purpose, byte[] message, byte[] signature, long nowMillis ){
		Entry e = keys.get(keyId);
		if (e == null) return Check.UNKNOWN_KEY;
		if (revoked.contains(keyId)) return Check.REVOKED;
		if (e.purpose != purpose) return Check.WRONG_PURPOSE;
		if (nowMillis < e.validFrom) return Check.NOT_YET_VALID;
		if (nowMillis > e.validUntil) return Check.EXPIRED;
		return e.verifier.verify(message, signature) ? Check.OK : Check.BAD_SIGNATURE;
	}
}
