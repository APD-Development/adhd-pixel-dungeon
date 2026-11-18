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

package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

//The first entry of an APD history archive: what the archive holds and a checksum of every file, so an import can check
//everything before it keeps anything. A plain text format (tab separated lines, UTF-8), versioned, that ignores lines it does not know:
//
//  APD-ARCHIVE <format>
//  created <millis>             app <version name>        telemetry <the exporting build's current telemetry version>
//  container <log container format>
//  file <path> <size> <sha256> <kind> <runId> <telemetry> <sealed 0/1> <redacted 0/1> <events> <status>
//  note <text>
//  count <files> <total bytes>
//  check <sha256 of every line above, each with its newline>
//
//Only raw evidence travels: run logs (runs/) and imported record files (legacy/). The index and the derived caches are rebuilt by the
//importing app, so they are never trusted from an archive.
public final class ArchiveManifest {

	public static final String ENTRY_NAME = "MANIFEST.txt";
	public static final int FORMAT = 1;
	public static final int MAX_BYTES = 16 * 1024 * 1024;
	public static final int MAX_FILES = 500_000;
	// A malformed signed manifest can contain a multi-megabyte line. Keep the
	// refusal readable without retaining the entire attacker-controlled line.
	private static final int MAX_DAMAGED_LINE_DETAIL_CHARS = 160;
	public static final String KIND_RUN = "run";
	public static final String KIND_IMPORTED = "legacy";

	public static final class Entry {
		public String path = "";
		public long size;
		public String sha256 = "";
		public String kind = KIND_RUN;
		public String runId = "";
		public String telemetry = "";
		public boolean sealed;
		public boolean redacted;
		public long events;
		public String status = "";

		//the id of the log (the file name without its folder and suffix)
		public String logId(){
			int slash = path.lastIndexOf('/');
			String n = path.substring(slash + 1);
			return n.endsWith(SUFFIX) ? n.substring(0, n.length() - SUFFIX.length()) : n;
		}
	}

	public static final String SUFFIX = ".apdlog";

	public int format = FORMAT;
	public long createdMillis;
	public String app = "";
	public String telemetry = "";
	public int container;
	public final List<Entry> files = new ArrayList<>();
	public final List<String> notes = new ArrayList<>();

	//---- paths ----

	//Only "runs/<id>.apdlog" and "legacy/<id>.apdlog" with a plain id are ever read from an archive: no other name can reach the disk.
	public static boolean isValidPath( String path ){
		if (path == null) return false;
		String folder, name;
		if (path.startsWith("runs/")){
			folder = "runs/";
		} else if (path.startsWith("legacy/")){
			folder = "legacy/";
		} else {
			return false;
		}
		name = path.substring(folder.length());
		if (!name.endsWith(SUFFIX)) return false;
		String id = name.substring(0, name.length() - SUFFIX.length());
		return com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore.isValidRunId(id);
	}

	//---- text ----

	private static String clean( String s ){
		if (s == null) return "";
		return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
	}

	public String encode(){
		StringBuilder b = new StringBuilder();
		b.append("APD-ARCHIVE\t").append(format).append('\n');
		b.append("created\t").append(createdMillis).append('\n');
		b.append("app\t").append(clean(app)).append('\n');
		b.append("telemetry\t").append(clean(telemetry)).append('\n');
		b.append("container\t").append(container).append('\n');
		long total = 0;
		for (Entry e : files){
			b.append("file\t").append(e.path).append('\t').append(e.size).append('\t').append(e.sha256).append('\t').append(e.kind).append('\t')
					.append(clean(e.runId)).append('\t').append(clean(e.telemetry)).append('\t').append(e.sealed ? 1 : 0).append('\t')
					.append(e.redacted ? 1 : 0).append('\t').append(e.events).append('\t').append(clean(e.status)).append('\n');
			total += e.size;
		}
		for (String n : notes) b.append("note\t").append(clean(n)).append('\n');
		b.append("count\t").append(files.size()).append('\t').append(total).append('\n');
		String check = sha256Hex(b.toString().getBytes(utf8()));
		b.append("check\t").append(check).append('\n');
		return b.toString();
	}

	//Parses a manifest. Throws IllegalArgumentException with a reason when it is damaged, from a newer format, or fails its checksum.
	public static ArchiveManifest parse( byte[] data ){
		// Invalid byte sequences must not turn into replacement characters before
		// checksum validation: the archive manifest is a byte-level integrity record.
		String text;
		try {
			text = utf8().newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(data)).toString();
		} catch (CharacterCodingException damaged){
			throw new IllegalArgumentException("the manifest has invalid UTF-8", damaged);
		}
		int checkAt = text.lastIndexOf("check\t");
		if (checkAt < 0 || (checkAt > 0 && text.charAt(checkAt - 1) != '\n')) throw new IllegalArgumentException("the manifest has no checksum");
		String body = text.substring(0, checkAt);
		// The terminal checksum authenticates only the body before this line.
		// Do not silently trim extra unchecksummed lines or whitespace after it.
		// Keep the writer's single LF, plus previously accepted missing-newline/CRLF endings.
		String claimed = text.substring(checkAt + "check\t".length());
		if (claimed.endsWith("\r\n")) claimed = claimed.substring(0, claimed.length() - 2);
		else if (claimed.endsWith("\n")) claimed = claimed.substring(0, claimed.length() - 1);
		if (!claimed.matches("[0-9a-fA-F]{64}")) throw new IllegalArgumentException("the manifest has a damaged checksum footer");
		if (!sha256Hex(body.getBytes(utf8())).equalsIgnoreCase(claimed)) throw new IllegalArgumentException("the manifest does not match its checksum");
		ArchiveManifest m = new ArchiveManifest();
		boolean header = false, counted = false, containerSeen = false;
		boolean createdSeen = false, appSeen = false, telemetrySeen = false;
		long countFiles = -1, countBytes = -1;
		for (String line : body.split("\n")){
			if (line.isEmpty()) continue;
			String[] p = line.split("\t", -1);
			try {
				switch (p[0]){
					case "APD-ARCHIVE":
						// Format 1 has one authoritative header; later headers must not replace it.
						if (header) throw new IllegalArgumentException("the manifest has repeated headers");
						m.format = Integer.parseInt(p[1]);
						if (m.format < 1) throw new IllegalArgumentException("the archive has an invalid format version (" + m.format + ")");
						if (m.format > FORMAT) throw new IllegalArgumentException("this archive is from a newer version of APD (format " + m.format + ")");
						header = true;
						break;
					case "created":
						if (createdSeen) throw new IllegalArgumentException("the manifest has repeated created metadata");
						m.createdMillis = Long.parseLong(p[1]);
						createdSeen = true;
						break;
					case "app":
						if (appSeen) throw new IllegalArgumentException("the manifest has repeated app metadata");
						m.app = p[1];
						appSeen = true;
						break;
					case "telemetry":
						if (telemetrySeen) throw new IllegalArgumentException("the manifest has repeated telemetry metadata");
						m.telemetry = p[1];
						telemetrySeen = true;
						break;
					case "container":
						// One authoritative log-container version per format-1 archive.
						if (containerSeen) throw new IllegalArgumentException("the manifest has repeated container metadata");
						m.container = Integer.parseInt(p[1]);
						// A log-container version is an unsigned on-disk value. Preserve
						// legacy checksummed zero, but a negative one cannot exist.
						if (m.container < 0) throw new IllegalArgumentException("the manifest has an invalid container version (" + m.container + ")");
						containerSeen = true;
						break;
					case "file": {
						Entry e = new Entry();
						e.path = p[1];
						e.size = Long.parseLong(p[2]);
						e.sha256 = p[3];
						e.kind = p[4];
						e.runId = p[5];
						e.telemetry = p[6];
						// Format 1 defines these metadata flags as exactly 0 or 1.
						// Never silently change damaged/foreign values to false.
						if (!p[7].equals("0") && !p[7].equals("1")) throw new IllegalArgumentException("a file has an invalid sealed flag");
						if (!p[8].equals("0") && !p[8].equals("1")) throw new IllegalArgumentException("a file has an invalid redacted flag");
						e.sealed = p[7].equals("1");
						e.redacted = p[8].equals("1");
						e.events = Long.parseLong(p[9]);
						// Event counts represent observed records and can never be negative.
						if (e.events < 0) throw new IllegalArgumentException("a file has a negative event count");
						e.status = p.length > 10 ? p[10] : "";
						if (e.size < 0) throw new IllegalArgumentException("a file has a negative size");
						m.files.add(e);
						if (m.files.size() > MAX_FILES) throw new IllegalArgumentException("the manifest lists too many files");
						break;
					}
					case "check":
						// The sole checksum belongs after the body, never inside it.
						// A second valid final checksum must not disguise an earlier check record.
						throw new IllegalArgumentException("the manifest has repeated checksums");
					case "note": m.notes.add(p.length > 1 ? p[1] : ""); break;
					case "count":
						// The format-1 writer emits exactly one totals record. A second
						// record cannot silently override contradictory archive totals.
						if (counted) throw new IllegalArgumentException("the manifest has repeated totals");
						countFiles = Long.parseLong(p[1]);
						countBytes = Long.parseLong(p[2]);
						counted = true;
						break;
					default: //a line from a newer writer
				}
			} catch (IndexOutOfBoundsException | NumberFormatException bad){
				String excerpt = line.length() <= MAX_DAMAGED_LINE_DETAIL_CHARS ? line
						: line.substring(0, MAX_DAMAGED_LINE_DETAIL_CHARS) + "... (truncated)";
				throw new IllegalArgumentException("the manifest has a damaged line: " + excerpt);
			}
		}
		if (!header) throw new IllegalArgumentException("this is not an APD history archive");
		// The format-1 container version is essential to identifying the raw log format.
		// Unlike optional source descriptors, the writer always supplies this record.
		if (!containerSeen) throw new IllegalArgumentException("the manifest has no container metadata");
		long total = 0;
		for (Entry e : m.files){
			//A valid checksum must not let an impossible file-size sum wrap into a plausible total.
			if (e.size > Long.MAX_VALUE - total) throw new IllegalArgumentException("the manifest's file sizes overflow");
			total += e.size;
		}
		if (!counted || countFiles != m.files.size() || countBytes != total) throw new IllegalArgumentException("the manifest's totals do not match its file list");
		return m;
	}

	public long totalBytes(){
		long t = 0;
		for (Entry e : files) t += e.size;
		return t;
	}

	public Entry find( String path ){
		for (Entry e : files) if (e.path.equals(path)) return e;
		return null;
	}

	static java.nio.charset.Charset utf8(){
		return java.nio.charset.Charset.forName("UTF-8");
	}

	public static String sha256Hex( byte[] data ){
		try {
			MessageDigest d = MessageDigest.getInstance("SHA-256");
			return hex(d.digest(data));
		} catch (java.security.NoSuchAlgorithmException e){
			throw new IllegalStateException(e);
		}
	}

	public static String hex( byte[] bytes ){
		StringBuilder b = new StringBuilder();
		for (byte x : bytes) b.append(Character.forDigit((x >> 4) & 0xF, 16)).append(Character.forDigit(x & 0xF, 16));
		return b.toString();
	}
}
