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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;

// The player's granted achievements. A grant is accepted only for this player, signed by a trusted unrevoked key; accepting it twice changes nothing.
public final class GrantLedger {

	public enum Result { ACCEPTED, ALREADY_HAVE, WRONG_PLAYER, REJECTED_SIGNATURE }

	private final Id player;
	private final TrustRoot trust;
	private final Map<Id, AchievementGrant> byGrant = new HashMap<>();
	private final Set<Id> achievements = new TreeSet<>();

	public GrantLedger( Id player, TrustRoot trust ){
		this.player = player;
		this.trust = trust;
	}

	public Result accept( AchievementGrant g, long nowMillis ){
		if (!g.playerId.equals(player)) return Result.WRONG_PLAYER;
		if (trust.check(g.keyId, TrustRoot.Purpose.DEVELOPER_GRANT, g.signedBytes(), g.signature, nowMillis) != TrustRoot.Check.OK) return Result.REJECTED_SIGNATURE;
		if (byGrant.containsKey(g.grantId)) return Result.ALREADY_HAVE;
		byGrant.put(g.grantId, g);
		achievements.add(g.achievementId);
		return Result.ACCEPTED;
	}

	public boolean has( Id achievement ){
		return achievements.contains(achievement);
	}

	public Set<Id> all(){
		return achievements;
	}
}
