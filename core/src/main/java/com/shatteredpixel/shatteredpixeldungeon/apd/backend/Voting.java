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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// One ballot per account per round, replaceable until the round closes, so retries are harmless. No weights anywhere (non-P2W).
public final class Voting {

	public static final class Round {
		public final Id id;
		public final Set<String> options;
		public final int maxChoices;
		public final long opensAtMillis, closesAtMillis;

		public Round( Id id, Set<String> options, int maxChoices, long opensAtMillis, long closesAtMillis ){
			if (id.kind != Id.Kind.VOTE_ROUND || options.isEmpty() || maxChoices < 1 || closesAtMillis <= opensAtMillis) throw new IllegalArgumentException("bad round");
			this.id = id;
			this.options = options;
			this.maxChoices = maxChoices;
			this.opensAtMillis = opensAtMillis;
			this.closesAtMillis = closesAtMillis;
		}
	}

	public enum Cast { OK, NOT_OPEN, CLOSED, UNKNOWN_OPTION, TOO_MANY_CHOICES, EMPTY }

	private final Round round;
	private final Map<Id, Set<String>> ballots = new HashMap<>();

	public Voting( Round round ){
		this.round = round;
	}

	public Cast cast( Id voter, Set<String> choices, long nowMillis ){
		if (voter.kind != Id.Kind.PLAYER) throw new IllegalArgumentException("only a player can vote");
		if (nowMillis < round.opensAtMillis) return Cast.NOT_OPEN;
		if (nowMillis >= round.closesAtMillis) return Cast.CLOSED;
		if (choices.isEmpty()) return Cast.EMPTY;
		if (choices.size() > round.maxChoices) return Cast.TOO_MANY_CHOICES;
		for (String c : choices) if (!round.options.contains(c)) return Cast.UNKNOWN_OPTION;
		ballots.put(voter, new HashSet<>(choices));
		return Cast.OK;
	}

	//counts per option (every option listed, zero included), computed from the ballots - never stored separately, so it cannot drift
	public Map<String, Integer> tally(){
		Map<String, Integer> t = new TreeMap<>();
		for (String o : round.options) t.put(o, 0);
		for (Set<String> b : ballots.values()) for (String c : b) t.put(c, t.get(c) + 1);
		return t;
	}

	public int voters(){
		return ballots.size();
	}
}
