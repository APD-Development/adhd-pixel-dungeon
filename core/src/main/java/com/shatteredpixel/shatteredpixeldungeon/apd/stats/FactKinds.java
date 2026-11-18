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

package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//The registry of every kind of fact APD records. Anything that wants its history kept declares a FactKind here once, in a static block or
//at startup, and then records observations of it. Nothing else has to change: the datasets, columns, filters, aggregates, topic pages
//and guide text are generated from these declarations.
//
//Rules (checked by a test against core/src/test/resources/apd/fact_kind_manifest.txt): a key is never reused for something else and never
//removed; fields of a kind are only ever added at the end.
public final class FactKinds {

	private FactKinds(){}

	private static final Map<String, FactKind> KINDS = new LinkedHashMap<>();
	// Guarded by the same monitor as registration; declarations are immutable.
	private static String cachedFingerprint;

	//Registers a kind. Registering the same key twice is a mistake (IllegalStateException), so two mechanics can never share a key by accident.
	public static synchronized FactKind register( FactKind k ){
		FactKind old = KINDS.get(k.key);
		if (old != null){
			if (old == k) return k;
			throw new IllegalStateException("fact kind registered twice: " + k.key);
		}
		KINDS.put(k.key, k);
		cachedFingerprint = null;
		return k;
	}

	public static FactKind get( String key ){
		loadBuiltins();
		synchronized (FactKinds.class){
			return KINDS.get(key);
		}
	}

	//all kinds in registration order
	public static List<FactKind> all(){
		loadBuiltins();
		synchronized (FactKinds.class){
			return Collections.unmodifiableList(new ArrayList<>(KINDS.values()));
		}
	}

	//The kinds the game itself declares. Called lazily so a test that only wants the registry sees them too.
	private static void loadBuiltins(){
		// GameFacts initialization registers into this map. Never hold its monitor while
		// waiting for that class initialization; the JVM already makes initialization once-only.
		BuiltinFactKinds.register();
	}

	//Changes whenever the set of kinds or their fields change. Row caches built from the registry are valid only for the same fingerprint.
	public static String fingerprint(){
		loadBuiltins();
		synchronized (FactKinds.class){
			if (cachedFingerprint != null) return cachedFingerprint;
			long h = 1125899906842597L;
			for (FactKind k : KINDS.values()){
				h = mix(h, k.key);
				h = mix(h, k.isInternal() ? "I" : "P");
				for (FactKind.Field f : k.values()) h = mix(h, "v" + f.key + f.scale);
				for (FactKind.Field f : k.tags()) h = mix(h, "t" + f.key);
			}
			return cachedFingerprint = Long.toHexString(h);
		}
	}

	private static long mix( long h, String s ){
		for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);
		return h * 31 + 7;
	}
}
