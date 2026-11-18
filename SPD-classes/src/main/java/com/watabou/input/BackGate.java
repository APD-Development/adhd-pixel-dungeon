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

package com.watabou.input;

// Android may deliver one Back through system and backend routes. Preserve
// complete fallback presses; reject old backend copies using their provenance.
public final class BackGate {

	private BackGate(){}

	//a real second press takes a person far longer than this
	public static final long SAME_PRESS_NANOS = 120_000_000L;

	private static long last;
	private static boolean any;
	private static Object systemBackOwner;
	private static long deliveredSystemNanos;
	private static long completedLegacyNanos;
	private static long acceptedLegacyNanos;

	public static synchronized void setSystemBackOwner(Object owner){
		if (systemBackOwner != owner) clearProvenance();
		systemBackOwner = owner;
	}

	public static synchronized void clearSystemBackOwner(Object owner){
		if (systemBackOwner == owner) {
			systemBackOwner = null;
			clearProvenance();
		}
	}

	public static synchronized boolean systemBackOwned(){
		return systemBackOwner != null;
	}

	public static synchronized boolean acceptLegacy(){
		return acceptLegacy(System.nanoTime());
	}

	public static synchronized boolean acceptLegacy(long nowNanos){
		return acceptLegacy(nowNanos, 0);
	}

	public static synchronized boolean acceptLegacy(long nowNanos, long sourceEventNanos){
		// Only a backend timestamp can identify an old copy. A future genuine
		// fallback must never pay for a duplicate that failed to arrive.
		if (systemBackOwner != null && sourceEventNanos > 0
				&& sourceEventNanos <= Math.max(deliveredSystemNanos, Math.max(completedLegacyNanos, acceptedLegacyNanos))) return false;
		if (!accept(nowNanos)) return false;
		if (systemBackOwner != null && sourceEventNanos > 0) acceptedLegacyNanos = sourceEventNanos;
		return true;
	}

	public static synchronized void recordLegacyRelease(long sourceEventNanos){
		if (systemBackOwner != null && sourceEventNanos > 0)
			completedLegacyNanos = Math.max(completedLegacyNanos, sourceEventNanos);
	}

	private static void clearProvenance(){
		deliveredSystemNanos = 0;
		completedLegacyNanos = 0;
		acceptedLegacyNanos = 0;
	}

	public static synchronized boolean acceptSystem(){
		return acceptSystem(System.nanoTime());
	}

	public static synchronized boolean acceptSystem(long nowNanos){
		if (!accept(nowNanos)) return false;
		if (systemBackOwner != null) deliveredSystemNanos = nowNanos;
		return true;
	}

	//true if this Back should be acted on
	public static synchronized boolean accept(){
		return accept(System.nanoTime());
	}

	public static synchronized boolean accept( long nowNanos ){
		if (any && nowNanos - last >= 0 && nowNanos - last < SAME_PRESS_NANOS) return false;
		last = nowNanos;
		any = true;
		return true;
	}

	public static synchronized void reset(){
		any = false;
		last = 0;
		clearProvenance();
	}
}
