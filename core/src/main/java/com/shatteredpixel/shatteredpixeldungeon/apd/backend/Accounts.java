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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Account model independent of any sign-in provider: a player id of ours, with credentials as ways in. Credentials can be added or removed (never the last one) and are never moved between players silently.
public final class Accounts {

	public static final class Credential {
		public final String provider, subject;

		public Credential( String provider, String subject ){
			this.provider = provider;
			this.subject = subject;
		}

		String key(){
			return provider + "\u0000" + subject;
		}
	}

	public enum Link { OK, ALREADY_LINKED_TO_THIS_PLAYER, BELONGS_TO_ANOTHER_PLAYER, UNKNOWN_PLAYER, LAST_CREDENTIAL }

	private final Map<String, Id> byCredential = new HashMap<>();
	private final Map<Id, List<Credential>> byPlayer = new HashMap<>();
	private final java.security.SecureRandom rnd;

	public Accounts( java.security.SecureRandom rnd ){
		this.rnd = rnd;
	}

	//the player for this credential; first sight creates the player
	public Id signIn( Credential c ){
		Id p = byCredential.get(c.key());
		if (p != null) return p;
		p = Id.random(Id.Kind.PLAYER, rnd);
		byCredential.put(c.key(), p);
		List<Credential> l = new ArrayList<>();
		l.add(c);
		byPlayer.put(p, l);
		return p;
	}

	//adds another way in to an existing player; a credential that already belongs to someone else is never moved silently
	//(merging two players is a product decision with consequences for rankings and content: see docs/APD-BACKEND.md)
	public Link link( Id player, Credential c ){
		List<Credential> mine = byPlayer.get(player);
		if (mine == null) return Link.UNKNOWN_PLAYER;
		Id owner = byCredential.get(c.key());
		if (owner != null) return owner.equals(player) ? Link.ALREADY_LINKED_TO_THIS_PLAYER : Link.BELONGS_TO_ANOTHER_PLAYER;
		byCredential.put(c.key(), player);
		mine.add(c);
		return Link.OK;
	}

	public Link unlink( Id player, Credential c ){
		List<Credential> mine = byPlayer.get(player);
		if (mine == null) return Link.UNKNOWN_PLAYER;
		if (!player.equals(byCredential.get(c.key()))) return Link.UNKNOWN_PLAYER;
		if (mine.size() <= 1) return Link.LAST_CREDENTIAL;
		byCredential.remove(c.key());
		for (int i = 0; i < mine.size(); i++) if (mine.get(i).key().equals(c.key())) { mine.remove(i); break; }
		return Link.OK;
	}

	public int credentials( Id player ){
		List<Credential> l = byPlayer.get(player);
		return l == null ? 0 : l.size();
	}
}
