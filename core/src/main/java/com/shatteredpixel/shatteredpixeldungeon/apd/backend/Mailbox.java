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

import java.util.List;

// The other direction of the Gateway: things the server has for this player (for now, evidence requests). Asking is a read; it changes nothing
// on the server and never sends the player's data.
public interface Mailbox {

	final class Fetched {
		public final boolean ok;
		public final List<Envelope> items;
		public final String detail;

		public Fetched( boolean ok, List<Envelope> items, String detail ){
			this.ok = ok;
			this.items = items;
			this.detail = detail == null ? "" : detail;
		}
	}

	Fetched fetch();
}
