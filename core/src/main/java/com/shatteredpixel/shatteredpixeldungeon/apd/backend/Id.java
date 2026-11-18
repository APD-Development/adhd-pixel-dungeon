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

import java.security.SecureRandom;
import java.util.Objects;
import java.util.regex.Pattern;

// Typed opaque id. The kind is part of the identity, so a player id can't be passed where a run id is expected.
public final class Id implements Comparable<Id> {

	public enum Kind {
		PLAYER("pl"), GROUP("gp"), DEVICE("dv"), RUN("run"), SUBMISSION("sub"), CONTENT("ct"), VOTE_ROUND("vr"), REPORT("rp"), GRANT("gr"), ACHIEVEMENT("ac"), AUDIT("au"), OUTBOX("ob");

		public final String prefix;

		Kind( String prefix ){
			this.prefix = prefix;
		}
	}

	private static final Pattern VALUE = Pattern.compile("[A-Za-z0-9._:-]{1,80}");

	public final Kind kind;
	public final String value;

	private Id( Kind kind, String value ){
		this.kind = kind;
		this.value = value;
	}

	public static Id of( Kind kind, String value ){
		if (kind == null || value == null || !VALUE.matcher(value).matches()) throw new IllegalArgumentException("not a valid " + kind + " id: " + value);
		return new Id(kind, value);
	}

	//a new random id (128 bits); the caller supplies the randomness so tests can be deterministic
	public static Id random( Kind kind, SecureRandom rnd ){
		byte[] b = new byte[16];
		rnd.nextBytes(b);
		StringBuilder sb = new StringBuilder(kind.prefix).append('_');
		for (byte x : b) sb.append(Character.forDigit((x >> 4) & 15, 16)).append(Character.forDigit(x & 15, 16));
		return new Id(kind, sb.toString());
	}

	//the text to store or send; the kind travels with the field it is in, not with the text
	@Override
	public String toString(){
		return value;
	}

	@Override
	public boolean equals( Object o ){
		return o instanceof Id && ((Id) o).kind == kind && ((Id) o).value.equals(value);
	}

	@Override
	public int hashCode(){
		return Objects.hash(kind, value);
	}

	@Override
	public int compareTo( Id o ){
		int c = kind.compareTo(o.kind);
		return c != 0 ? c : value.compareTo(o.value);
	}
}
