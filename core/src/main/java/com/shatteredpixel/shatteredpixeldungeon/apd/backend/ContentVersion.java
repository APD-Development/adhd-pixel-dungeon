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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

// One immutable version of community content, identified by its package hash. A fix is a new version; REVOKED is terminal.
public final class ContentVersion {

	public enum Kind { CHARACTER, MOD, CHALLENGE, OTHER }

	public enum State { DRAFT, SUBMITTED, APPROVED, REJECTED, WITHDRAWN, REVOKED }

	public final Id contentId, author;
	public final Kind kind;
	public final int major, minor, patch;
	//SHA-256 (hex) of the package bytes
	public final String packageHashHex;
	//the ruleset era this version was made for; the client refuses content for an era it does not run
	public final String rulesetId;
	public final long createdAtMillis;

	public ContentVersion( Id contentId, Id author, Kind kind, int major, int minor, int patch, String packageHashHex, String rulesetId, long createdAtMillis ){
		if (contentId.kind != Id.Kind.CONTENT || author.kind != Id.Kind.PLAYER) throw new IllegalArgumentException("wrong id kinds");
		if (major < 0 || minor < 0 || patch < 0 || major + minor + patch == 0) throw new IllegalArgumentException("bad version");
		if (packageHashHex == null || !packageHashHex.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("bad hash");
		this.contentId = contentId;
		this.author = author;
		this.kind = kind;
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.packageHashHex = packageHashHex;
		this.rulesetId = rulesetId;
		this.createdAtMillis = createdAtMillis;
	}

	public String versionText(){
		return major + "." + minor + "." + patch;
	}

	public String key(){
		return contentId.value + "@" + versionText();
	}

	private static final Map<State, Set<State>> NEXT = new EnumMap<>(State.class);
	static {
		NEXT.put(State.DRAFT, EnumSet.of(State.SUBMITTED, State.WITHDRAWN));
		NEXT.put(State.SUBMITTED, EnumSet.of(State.APPROVED, State.REJECTED, State.WITHDRAWN));
		NEXT.put(State.APPROVED, EnumSet.of(State.WITHDRAWN, State.REVOKED));
		NEXT.put(State.REJECTED, EnumSet.noneOf(State.class));
		NEXT.put(State.WITHDRAWN, EnumSet.noneOf(State.class));
		NEXT.put(State.REVOKED, EnumSet.noneOf(State.class));
	}

	public static boolean canMove( State from, State to ){
		return NEXT.get(from).contains(to);
	}

	//what the approving authority signs: this exact version, with this exact hash, was approved at this time
	public static byte[] approvalBytes( ContentVersion v, long approvedAtMillis, String keyId ){
		return new Canon("apd.content-approval", 1).id(v.contentId).str(v.versionText()).str(v.packageHashHex).str(v.rulesetId).num(approvedAtMillis).str(keyId).toBytes();
	}

	//Would a client accept these downloaded bytes as this approved version? The bytes must hash to the approved hash AND the approval must carry a
	//valid CONTENT_APPROVAL signature: a forged or tampered package, or a made-up approval, fails here before anything is loaded.
	public static boolean clientMayLoad( ContentVersion v, byte[] downloaded, long approvedAtMillis, String keyId, byte[] approvalSignature, TrustRoot trust, long nowMillis ){
		if (!Canon.hex(Canon.sha256(downloaded)).equals(v.packageHashHex)) return false;
		return trust.check(keyId, TrustRoot.Purpose.CONTENT_APPROVAL, approvalBytes(v, approvedAtMillis, keyId), approvalSignature, nowMillis) == TrustRoot.Check.OK;
	}
}
