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
import java.util.Map;

// Account-level requests a player can make: export what the server holds about them, or delete the account. Both are queued like any other
// item, so they survive being offline, and both are idempotent per player (asking twice is the same as asking once).
public final class AccountRequests {

	public static final String EXPORT_SCHEMA = "apd.account-export";
	public static final String DELETE_SCHEMA = "apd.account-delete";
	public static final int MAJOR = 1, MINOR = 0;

	public enum Kind { EXPORT, DELETE }

	public static final class Request {
		public final Kind kind;
		public final Id player;
		public final long requestedAtMillis;

		public Request( Kind kind, Id player, long requestedAtMillis ){
			if (player.kind != Id.Kind.PLAYER) throw new IllegalArgumentException("not a player id");
			this.kind = kind;
			this.player = player;
			this.requestedAtMillis = requestedAtMillis;
		}

		//same key for the same request, so a retry after a lost reply is not a second request
		public String idempotencyKey(){
			return (kind == Kind.EXPORT ? "exp_" : "del_") + player.value;
		}
	}

	private AccountRequests(){}

	public static String schemaOf( Kind k ){
		return k == Kind.EXPORT ? EXPORT_SCHEMA : DELETE_SCHEMA;
	}

	public static Envelope toEnvelope( Request r ){
		return new Envelope(schemaOf(r.kind), MAJOR, MINOR, "player=" + r.player.value + "\nrequestedAtMillis=" + r.requestedAtMillis + "\n");
	}

	//null if the envelope is not a readable account request
	public static Request fromEnvelope( Envelope e ){
		Kind kind;
		if (EXPORT_SCHEMA.equals(e.schema)) kind = Kind.EXPORT;
		else if (DELETE_SCHEMA.equals(e.schema)) kind = Kind.DELETE;
		else return null;
		if (e.compatibleWith(MAJOR, MINOR) == Envelope.Compat.UNSUPPORTED_MAJOR) return null;
		Map<String, String> m = new HashMap<>();
		for (String line : e.payload.split("\n")){
			int i = line.indexOf('=');
			if (i > 0) m.put(line.substring(0, i), line.substring(i + 1));
		}
		try {
			return new Request(kind, Id.of(Id.Kind.PLAYER, m.get("player")), Long.parseLong(m.get("requestedAtMillis")));
		} catch (RuntimeException ex){
			return null;
		}
	}
}
