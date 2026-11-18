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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Collections that may sync between a player's devices. Only small account-private preference data; raw history never.
public final class SyncCollections {

	public static final class Spec {
		public final String name;
		public final SyncRecord.Policy policy;
		public final String schema;

		Spec( String name, SyncRecord.Policy policy, String schema ){
			this.name = name;
			this.policy = policy;
			this.schema = schema;
		}
	}

	private static final Map<String, Spec> ALL = new HashMap<>();
	static {
		add(new Spec("settings", SyncRecord.Policy.LAST_WRITE_WINS, "apd.settings-sync"));
		add(new Spec("saved-views", SyncRecord.Policy.LAST_WRITE_WINS, "apd.settings-sync"));
	}

	private SyncCollections(){}

	private static void add( Spec s ){
		ALL.put(s.name, s);
	}

	public static Spec get( String name ){
		return ALL.get(name);
	}

	public static Set<String> names(){
		return Collections.unmodifiableSet(ALL.keySet());
	}
}
