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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

// Small local record files: one line per record, fields base64 and tab separated, written to a temp file and renamed over the old one.
// A damaged line is skipped, never fatal.
final class TabFile {

	private TabFile(){}

	static void write( File file, List<String[]> rows ) throws IOException {
		// A record destination must never be replaced if it is a directory.
		if (file.isDirectory()) throw new IOException("not a record file: " + file);
		File tmp = new File(file.getPath() + ".tmp");
		try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8))){
			for (String[] row : rows){
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < row.length; i++){
					if (i > 0) sb.append('\t');
					sb.append(Base64.getEncoder().encodeToString(row[i].getBytes(StandardCharsets.UTF_8)));
				}
				w.write(sb.toString());
				w.newLine();
			}
		}
		// A failed atomic replacement must leave the previous records intact.
		// Never delete the old file just to retry a rename.
		if (!tmp.renameTo(file)) throw new IOException("cannot replace " + file);
	}

	//rows with exactly 'fields' fields; anything else is skipped
	static List<String[]> read( File file, int fields ){
		List<String[]> out = new ArrayList<>();
		if (file == null || !file.exists()) return out;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))){
			String line;
			while ((line = in.readLine()) != null){
				if (line.isEmpty()) continue;
				try {
					String[] p = line.split("\t", -1);
					if (p.length != fields) throw new IllegalArgumentException("fields");
					String[] row = new String[fields];
					for (int i = 0; i < fields; i++){
						// Base64 can contain malformed UTF-8. Do not fabricate record text with U+FFFD.
						byte[] bytes = Base64.getDecoder().decode(p[i]);
						row[i] = StandardCharsets.UTF_8.newDecoder()
								.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
					}
					out.add(row);
				} catch (RuntimeException | java.nio.charset.CharacterCodingException e){
					//skip the damaged line
				}
			}
		} catch (IOException e){
			//an unreadable file is an empty one
		}
		return out;
	}
}
