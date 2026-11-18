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
import java.util.Set;
import java.util.TreeSet;

// What a server sends to open a vote: the round (options, how many may be picked, when it opens and closes) and a one-line title.
public final class VoteMessages {

	public static final String ROUND_SCHEMA = "apd.vote-round";
	public static final int MAJOR = 1, MINOR = 0;
	public static final int MAX_TITLE = 80, MAX_OPTIONS = 50;

	public static final class Offer {
		public final Voting.Round round;
		public final String title;

		public Offer( Voting.Round round, String title ){
			String t = title == null ? "" : title.replace('\n', ' ').replace('\r', ' ').trim();
			this.round = round;
			this.title = t.length() > MAX_TITLE ? t.substring(0, MAX_TITLE) : t;
		}
	}

	private VoteMessages(){}

	public static Envelope offerToEnvelope( Offer o ){
		for (String opt : o.round.options){
			if (opt.isEmpty() || opt.indexOf('\n') >= 0 || opt.indexOf(',') >= 0) throw new IllegalArgumentException("bad option");
		}
		return new Envelope(ROUND_SCHEMA, MAJOR, MINOR, "id=" + o.round.id.value + "\ntitle=" + o.title + "\noptions=" + String.join(",", new TreeSet<>(o.round.options))
				+ "\nmaxChoices=" + o.round.maxChoices + "\nopensAtMillis=" + o.round.opensAtMillis + "\nclosesAtMillis=" + o.round.closesAtMillis + "\n");
	}

	//null if the envelope is not a readable, sane round
	public static Offer offerFromEnvelope( Envelope e ){
		if (!ROUND_SCHEMA.equals(e.schema) || e.compatibleWith(MAJOR, MINOR) == Envelope.Compat.UNSUPPORTED_MAJOR) return null;
		Map<String, String> m = new HashMap<>();
		for (String line : e.payload.split("\n")){
			int i = line.indexOf('=');
			if (i > 0) m.put(line.substring(0, i), line.substring(i + 1));
		}
		try {
			Set<String> options = new TreeSet<>();
			for (String o : m.get("options").split(",")) if (!o.isEmpty()) options.add(o);
			if (options.size() > MAX_OPTIONS) return null;
			Voting.Round r = new Voting.Round(Id.of(Id.Kind.VOTE_ROUND, m.get("id")), options, Integer.parseInt(m.get("maxChoices")), Long.parseLong(m.get("opensAtMillis")),
					Long.parseLong(m.get("closesAtMillis")));
			return new Offer(r, m.containsKey("title") ? m.get("title") : "");
		} catch (RuntimeException ex){
			return null;
		}
	}
}
