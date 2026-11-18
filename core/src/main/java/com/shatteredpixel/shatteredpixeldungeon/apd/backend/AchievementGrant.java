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

// A developer-granted achievement: a signed statement made server-side. The app only verifies and records grants; it has no signing key.
public final class AchievementGrant {

	public static final int SCHEMA_VERSION = 1;

	public final Id grantId, playerId, achievementId;
	public final long issuedAtMillis;
	public final String keyId;
	public final byte[] signature;

	public AchievementGrant( Id grantId, Id playerId, Id achievementId, long issuedAtMillis, String keyId, byte[] signature ){
		if (grantId.kind != Id.Kind.GRANT || playerId.kind != Id.Kind.PLAYER || achievementId.kind != Id.Kind.ACHIEVEMENT) throw new IllegalArgumentException("wrong id kinds");
		this.grantId = grantId;
		this.playerId = playerId;
		this.achievementId = achievementId;
		this.issuedAtMillis = issuedAtMillis;
		this.keyId = keyId;
		this.signature = signature;
	}

	//exactly what is signed: every field except the signature itself (and the key id is included, so a grant cannot be re-labelled to another key)
	public static byte[] signedBytes( Id grantId, Id playerId, Id achievementId, long issuedAtMillis, String keyId ){
		return new Canon("apd.achievement-grant", SCHEMA_VERSION).id(grantId).id(playerId).id(achievementId).num(issuedAtMillis).str(keyId).toBytes();
	}

	public byte[] signedBytes(){
		return signedBytes(grantId, playerId, achievementId, issuedAtMillis, keyId);
	}
}
