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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

// Persists the Outbox (one line per item, temp file then move). Damaged lines are skipped and counted; local-only data is re-checked on load.
public final class OutboxStore {

	public static final class Loaded {
		public int items, skipped;
	}

	private OutboxStore(){}

	public static void save( Outbox box, File file ) throws IOException {
		File tmp = new File(file.getPath() + ".tmp");
		try (BufferedWriter w = Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8)){
			for (Outbox.Item i : box.items()){
				w.write(i.id.value + "\t" + i.createdAtMillis + "\t" + i.attempts + "\t" + i.nextAttemptAtMillis + "\t" + i.envelope.schema + "\t" + i.envelope.major + "\t"
						+ i.envelope.minor + "\t" + b64(i.envelope.payload) + "\t" + b64(i.lastDetail));
				w.newLine();
			}
		}
		Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	//reads what can be read into 'box' (which is normally empty)
	public static Loaded load( Outbox box, File file ) throws IOException {
		Loaded r = new Loaded();
		if (!file.exists()) return r;
		java.util.Set<String> restoredIds = new java.util.HashSet<>();
		try (BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)){
			String line;
			while ((line = in.readLine()) != null){
				if (line.isEmpty()) continue;
				try {
					String[] p = line.split("\t", -1);
					if (p.length != 9) throw new IllegalArgumentException("fields");
					if (restoredIds.contains(p[0])) throw new IllegalArgumentException("repeated queue id");
					box.restore(Id.of(Id.Kind.OUTBOX, p[0]), new Envelope(p[4], Integer.parseInt(p[5]), Integer.parseInt(p[6]), unb64(p[7])), Long.parseLong(p[1]),
							Integer.parseInt(p[2]), Long.parseLong(p[3]), unb64(p[8]));
					restoredIds.add(p[0]);
					r.items++;
				} catch (RuntimeException e){
					r.skipped++;
				}
			}
		}
		return r;
	}

	private static String b64( String s ){
		return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
	}

	private static String unb64( String s ){
		try {
			return StandardCharsets.UTF_8.newDecoder().decode(
					java.nio.ByteBuffer.wrap(Base64.getDecoder().decode(s))).toString();
		} catch (java.nio.charset.CharacterCodingException e) {
			throw new IllegalArgumentException("invalid UTF-8 in queue field", e);
		}
	}
}
