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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

// Holds the newest catalogue the device has accepted, on disk, so the content list works offline. It accepts a downloaded catalogue only if
// it is correctly signed, not expired and NEWER than the one already held (a stale or replayed file can never undo a revocation). A damaged or
// unreadable cache file is treated as "nothing held yet", never as a crash. Reads no game state, sends nothing.
public final class CatalogueKeeper {

	public enum Result { ACCEPTED, SAME_OR_OLDER, MALFORMED, BAD_SIGNATURE, UNKNOWN_KEY, KEY_NOT_ALLOWED, KEY_NOT_VALID, EXPIRED }

	//Separate keepers for the same on-disk catalogue do not share their instance monitor.
	//Serialize the compare-and-replace section so a stale instance cannot overwrite a newer revocation catalogue.
	private static final Object STORE_LOCK = new Object();

	interface OfferHook {
		void insideStoreLock( File temp ) throws InterruptedException;
	}

	private final File file;
	private final TrustRoot trust;
	private ContentCatalogue held;

	public CatalogueKeeper( File file, TrustRoot trust ){
		this.file = file;
		this.trust = trust;
	}

	//re-checks the cached file against the CURRENT trust root: a key revoked since it was saved drops the cache
	public synchronized ContentCatalogue load( long nowMillis ){
		held = readPersisted(nowMillis);
		return held;
	}

	private ContentCatalogue readPersisted( long nowMillis ){
		try {
			if (!file.exists()) return null;
			ContentCatalogue c = ContentCatalogue.parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
			//expiry is judged when the list is used, not when it is read: an expired cache still guards revocations, so only signature checks apply
			ContentCatalogue.Verdict v = c.verify(trust, nowMillis);
			if (v == ContentCatalogue.Verdict.OK || v == ContentCatalogue.Verdict.EXPIRED) return c;
		} catch (IOException | RuntimeException ignored){
		}
		return null;
	}

	public synchronized ContentCatalogue held(){
		return held;
	}

	//is the held catalogue still fresh enough to OFFER content from? (revocations are always honoured, even when stale)
	public synchronized boolean fresh( long nowMillis ){
		return held != null && nowMillis <= held.expiresAtMillis;
	}

	public synchronized Result offer( String downloadedText, long nowMillis ){
		return offer(downloadedText, nowMillis, null);
	}

	//Package-private hook makes the cross-instance replacement regression deterministic.
	synchronized Result offer( String downloadedText, long nowMillis, OfferHook hook ){
		ContentCatalogue c;
		try {
			c = ContentCatalogue.parse(downloadedText);
		} catch (RuntimeException e){
			return Result.MALFORMED;
		}
		switch (c.verify(trust, nowMillis)){
			case BAD_SIGNATURE: return Result.BAD_SIGNATURE;
			case UNKNOWN_KEY: return Result.UNKNOWN_KEY;
			case KEY_NOT_ALLOWED: return Result.KEY_NOT_ALLOWED;
			case KEY_NOT_VALID: return Result.KEY_NOT_VALID;
			case EXPIRED: return Result.EXPIRED;
			default: break;
		}
		synchronized (STORE_LOCK){
			ContentCatalogue persisted = readPersisted(nowMillis);
			if (persisted != null && (held == null || persisted.sequence > held.sequence)) held = persisted;
			if (held != null && c.sequence <= held.sequence) return Result.SAME_OR_OLDER;
			try {
				File tmp = new File(file.getPath() + ".tmp");
				if (hook != null){
					try {
						hook.insideStoreLock(tmp);
					} catch (InterruptedException interrupted){
						Thread.currentThread().interrupt();
						throw new IOException("interrupted while saving the catalogue", interrupted);
					}
				}
				Files.write(tmp.toPath(), c.toText().getBytes(StandardCharsets.UTF_8));
				Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e){
				//could not save: keep what we had rather than hold something the disk does not have
				return Result.MALFORMED;
			}
			held = c;
			return Result.ACCEPTED;
		}
	}

	//the content the player could be offered right now (nothing if there is no fresh, verified catalogue)
	public synchronized java.util.List<ContentCatalogue.Entry> offered( String rulesetId, long nowMillis ){
		if (!fresh(nowMillis)) return java.util.Collections.emptyList();
		return held.available(rulesetId);
	}

	//may these downloaded bytes be loaded? needs a held catalogue that lists the version, has not revoked it, and the approval signature must verify
	public synchronized boolean mayLoad( ContentVersion v, byte[] downloaded, long nowMillis ){
		if (held == null || held.isRevoked(v)) return false;
		for (ContentCatalogue.Entry e : held.entries()){
			if (e.version.key().equals(v.key())) return e.mayLoad(downloaded, trust, nowMillis);
		}
		return false;
	}
}
