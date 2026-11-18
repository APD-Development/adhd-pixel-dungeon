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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// This device's copy of the preference records that sync between a player's devices (SyncCollections only, never history). Provider-neutral:
// it never talks to anything. The caller feeds it what the server sent (applyRemote), uploads what it reports (pending) and tells it what the
// server accepted (acknowledge).
//
// Rules: a local edit stays "dirty" until acknowledged, a delete is a tombstone (so it can reach the other devices), a newer remote change
// replaces a clean record, and when both sides changed the same record the later edit wins (ties go to the larger device id, so every device
// picks the same winner). SERVER_WINS collections always take the server's copy. Values are small; anything else is refused.
public final class SyncReplica {

	public static final int MAX_VALUE_CHARS = 8192;

	public static final class Change {
		public final String collection, key, value;
		public final boolean deleted;

		Change( String collection, String key, boolean deleted, String value ){
			this.collection = collection;
			this.key = key;
			this.deleted = deleted;
			this.value = value;
		}
	}

	private static final class Slot {
		SyncRecord record;
		boolean dirty;
	}

	private final String deviceId;
	private final Map<String, Slot> slots = new TreeMap<>();

	public SyncReplica( String deviceId ){
		if (deviceId == null || deviceId.isEmpty()) throw new IllegalArgumentException("device id");
		this.deviceId = deviceId;
	}

	private static String id( String collection, String key ){
		return collection + "\u0000" + key;
	}

	private static void check( String collection, String key, String value ){
		if (SyncCollections.get(collection) == null) throw new IllegalArgumentException("not a syncable collection: " + collection);
		if (key == null || key.isEmpty() || key.length() > 120) throw new IllegalArgumentException("bad key");
		if (value != null && value.length() > MAX_VALUE_CHARS) throw new IllegalArgumentException("value too large");
	}

	public synchronized void put( String collection, String key, String value, long nowMillis ){
		check(collection, key, value);
		Slot s = slots.get(id(collection, key));
		long base = s == null ? 0 : s.record.serverVersion;
		if (s == null) slots.put(id(collection, key), s = new Slot());
		s.record = new SyncRecord(collection, key, base, nowMillis, deviceId, false, value);
		s.dirty = true;
	}

	//a tombstone: kept so the delete reaches the other devices
	public synchronized void delete( String collection, String key, long nowMillis ){
		check(collection, key, null);
		Slot s = slots.get(id(collection, key));
		if (s == null || s.record.deleted) return;
		s.record = new SyncRecord(collection, key, s.record.serverVersion, nowMillis, deviceId, true, "");
		s.dirty = true;
	}

	//the current value, or null if there is none (never set, or deleted)
	public synchronized String get( String collection, String key ){
		Slot s = slots.get(id(collection, key));
		return s == null || s.record.deleted ? null : s.record.value;
	}

	public synchronized List<SyncRecord> pending(){
		List<SyncRecord> out = new ArrayList<>();
		for (Slot s : slots.values()) if (s.dirty) out.add(s.record);
		return out;
	}

	//the server accepted our edit of this record and gave it this version
	public synchronized void acknowledge( SyncRecord sent, long serverVersion ){
		Slot s = slots.get(id(sent.collection, sent.key));
		if (s == null || !s.dirty || s.record.updatedAtMillis != sent.updatedAtMillis || serverVersion < s.record.serverVersion
                || s.record.deleted != sent.deleted || !s.record.value.equals(sent.value)
                || !s.record.deviceId.equals(sent.deviceId)) return;
		s.record = new SyncRecord(s.record.collection, s.record.key, serverVersion, s.record.updatedAtMillis, s.record.deviceId, s.record.deleted, s.record.value);
		s.dirty = false;
	}

	//merges what the server sent; returns what the app must now apply locally (only real changes, so applying twice does nothing)
	public synchronized List<Change> applyRemote( List<SyncRecord> remote ){
		List<Change> changes = new ArrayList<>();
		for (SyncRecord r : remote){
			SyncCollections.Spec spec = SyncCollections.get(r.collection);
			if (spec == null || r.key.isEmpty() || r.value.length() > MAX_VALUE_CHARS) continue;
			Slot s = slots.get(id(r.collection, r.key));
			boolean take;
			if (s == null){
				take = true;
			} else if (r.serverVersion < s.record.serverVersion || (r.serverVersion == s.record.serverVersion && !s.dirty)){
				take = false;
			} else if (!s.dirty || spec.policy == SyncRecord.Policy.SERVER_WINS){
				take = true;
			} else {
				//both sides changed it: the later edit wins, ties by device id
				take = r.updatedAtMillis > s.record.updatedAtMillis || (r.updatedAtMillis == s.record.updatedAtMillis && r.deviceId.compareTo(s.record.deviceId) > 0);
				if (!take){
					//our edit wins but is now based on the newer version, so it uploads as the newest
					s.record = new SyncRecord(s.record.collection, s.record.key, r.serverVersion, s.record.updatedAtMillis, s.record.deviceId, s.record.deleted, s.record.value);
					continue;
				}
			}
			if (!take) continue;
			boolean same = s != null && s.record.deleted == r.deleted && s.record.value.equals(r.value);
			if (s == null) slots.put(id(r.collection, r.key), s = new Slot());
			s.record = r;
			s.dirty = false;
			if (!same) changes.add(new Change(r.collection, r.key, r.deleted, r.value));
		}
		return changes;
	}

	//forget clean tombstones older than the retention window (the server has them; every device has had time to see them)
	public synchronized int purgeTombstones( long nowMillis, long retentionMillis ){
		int n = 0;
		for (Iterator<Slot> it = slots.values().iterator(); it.hasNext();){
			Slot s = it.next();
			if (!s.dirty && s.record.deleted && nowMillis - s.record.updatedAtMillis > retentionMillis){
				it.remove();
				n++;
			}
		}
		return n;
	}

	public synchronized void save( File file ) throws IOException {
		File tmp = new File(file.getPath() + ".tmp");
		try (BufferedWriter w = Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8)){
			for (Slot s : slots.values()){
				SyncRecord r = s.record;
				w.write(b64(r.collection) + "\t" + b64(r.key) + "\t" + r.serverVersion + "\t" + r.updatedAtMillis + "\t" + b64(r.deviceId) + "\t" + (r.deleted ? 1 : 0) + "\t"
						+ (s.dirty ? 1 : 0) + "\t" + b64(r.value));
				w.newLine();
			}
		}
		Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	//returns how many damaged lines were skipped
	public synchronized int load( File file ) throws IOException {
		int skipped = 0;
		if (!file.exists()) return 0;
		try (BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)){
			String line;
			while ((line = in.readLine()) != null){
				if (line.isEmpty()) continue;
				try {
					String[] p = line.split("\t", -1);
					if (p.length != 8) throw new IllegalArgumentException("fields");
					String coll = unb64(p[0]), key = unb64(p[1]);
					check(coll, key, null);
					Slot s = new Slot();
					s.record = new SyncRecord(coll, key, Long.parseLong(p[2]), Long.parseLong(p[3]), unb64(p[4]), p[5].equals("1"), unb64(p[7]));
					s.dirty = p[6].equals("1");
					slots.put(id(coll, key), s);
				} catch (RuntimeException e){
					skipped++;
				}
			}
		}
		return skipped;
	}

	private static String b64( String s ){
		return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
	}

	private static String unb64( String s ){
		return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
	}
}
