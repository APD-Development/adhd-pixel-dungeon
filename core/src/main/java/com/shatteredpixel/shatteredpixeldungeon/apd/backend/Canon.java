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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.ByteArrayOutputStream;

// Canonical bytes for hashing and signing. Fields are tagged and length-prefixed, so different records never produce the same bytes.
public final class Canon {

	private final ByteArrayOutputStream out = new ByteArrayOutputStream();

	//'type' names the record kind and 'version' its schema version, so a signature on one kind of record is never valid for another
	public Canon( String type, int version ){
		str(type);
		num(version);
	}

	public Canon str( String s ){
		byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
		out.write('S');
		len(b.length);
		out.write(b, 0, b.length);
		return this;
	}

	public Canon num( long v ){
		out.write('N');
		for (int i = 56; i >= 0; i -= 8) out.write((int) (v >>> i) & 0xFF);
		return this;
	}

	public Canon bool( boolean v ){
		out.write('B');
		out.write(v ? 1 : 0);
		return this;
	}

	public Canon bytes( byte[] b ){
		out.write('Y');
		len(b.length);
		out.write(b, 0, b.length);
		return this;
	}

	public Canon id( Id id ){
		return str(id.kind.name()).str(id.value);
	}

	private void len( int n ){
		for (int i = 24; i >= 0; i -= 8) out.write((n >>> i) & 0xFF);
	}

	public byte[] toBytes(){
		return out.toByteArray();
	}

	public String sha256Hex(){
		return hex(sha256(toBytes()));
	}

	public static byte[] sha256( byte[] data ){
		try {
			return MessageDigest.getInstance("SHA-256").digest(data);
		} catch (NoSuchAlgorithmException e){
			throw new IllegalStateException(e);
		}
	}

	public static String hex( byte[] b ){
		StringBuilder sb = new StringBuilder();
		for (byte x : b) sb.append(Character.forDigit((x >> 4) & 15, 16)).append(Character.forDigit(x & 15, 16));
		return sb.toString();
	}
}
