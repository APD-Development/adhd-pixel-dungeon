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

// A preference-type record kept in step between a player's devices (never raw history). The server assigns the version; deletes are tombstones.
public final class SyncRecord {

	public enum Policy {
		//the newest change wins; ties are broken by device id so every device picks the same winner
		LAST_WRITE_WINS,
		//the server's copy always wins (for anything that must stay authoritative, e.g. ranking records, grants)
		SERVER_WINS
	}

	public final String collection, key, deviceId, value;
	public final long serverVersion, updatedAtMillis;
	public final boolean deleted;

	public SyncRecord( String collection, String key, long serverVersion, long updatedAtMillis, String deviceId, boolean deleted, String value ){
		this.collection = collection;
		this.key = key;
		this.serverVersion = serverVersion;
		this.updatedAtMillis = updatedAtMillis;
		this.deviceId = deviceId;
		this.deleted = deleted;
		this.value = value == null ? "" : value;
	}

	//the record both sides should keep. 'local' may be null (not seen here yet).
	public static SyncRecord merge( SyncRecord local, SyncRecord remote, Policy policy ){
		if (local == null) return remote;
		if (remote == null) return local;
		if (!local.collection.equals(remote.collection) || !local.key.equals(remote.key)) throw new IllegalArgumentException("not the same record");
		if (policy == Policy.SERVER_WINS) return remote;
		if (local.serverVersion != remote.serverVersion) return local.serverVersion > remote.serverVersion ? local : remote;
		if (local.updatedAtMillis != remote.updatedAtMillis) return local.updatedAtMillis > remote.updatedAtMillis ? local : remote;
		return local.deviceId.compareTo(remote.deviceId) >= 0 ? local : remote;
	}
}
