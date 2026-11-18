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

import java.util.ArrayList;
import java.util.List;

// Wire form of a list of envelopes in one response: for each one a header line "schema major.minor", then its payload lines, then a line "--".
public final class MailboxCodec {

	private MailboxCodec(){}

	public static String encode( List<Envelope> items ){
		StringBuilder sb = new StringBuilder();
		for (Envelope e : items){
			for (String line : e.payload.split("\n")) if (line.equals("--")) throw new IllegalArgumentException("payload line clashes with the separator");
			sb.append(e.schema).append(' ').append(e.major).append('.').append(e.minor).append('\n');
			sb.append(e.payload);
			if (!e.payload.endsWith("\n")) sb.append('\n');
			sb.append("--\n");
		}
		return sb.toString();
	}

	//Reads what it can: a damaged item is skipped, the rest are returned.
	public static List<Envelope> decode( String body ){
		List<Envelope> out = new ArrayList<>();
		if (body == null) return out;
		String[] lines = body.split("\n", -1);
		int i = 0;
		while (i < lines.length){
			String header = lines[i++];
			if (header.isEmpty()) continue;
			StringBuilder payload = new StringBuilder();
			boolean closed = false;
			while (i < lines.length){
				String l = lines[i++];
				if (l.equals("--")){
					closed = true;
					break;
				}
				payload.append(l).append('\n');
			}
			if (!closed) break;
			try {
				String[] h = header.split(" ");
				String[] v = h[1].split("\\.");
				out.add(new Envelope(h[0], Integer.parseInt(v[0]), Integer.parseInt(v[1]), payload.toString()));
			} catch (RuntimeException ex){
				//skip a damaged item
			}
		}
		return out;
	}
}
