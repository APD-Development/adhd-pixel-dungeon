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

package com.shatteredpixel.shatteredpixeldungeon.apd.age;

// The 16+ gate for Community only (Chris, Nov 18 2026). The game itself stays open to everyone.
//
// 1. Google Play's Age Signals come first. A range starting at 16 or higher unlocks; a range that ends below 16 locks.
// 2. If Play gives no answer, the player is asked for birth month and year. Only pass or fail is kept on the device,
//    never the date, and a failed answer can't be retried with a different year on this device.
// 3. A later Play answer still wins over the stored birth-date answer (Claude's call, Nov 18: Play's range is the better
//    evidence in both directions).
//
// Birth month and year can't tell whether this month's birthday has passed yet, so a player born this month counts as
// not having had it (the gate errs toward locking by at most a few weeks).
public final class CommunityAgeGate {

	public static final int MIN_AGE = 16;

	public enum Result { UNLOCKED, LOCKED, ASK_BIRTH_DATE }

	// What Play said. ageLower/ageUpper null = unknown (Play's open-ended top band has ageUpper null).
	public static final class PlaySignal {
		public final Integer ageLower, ageUpper;
		public PlaySignal(Integer ageLower, Integer ageUpper){ this.ageLower = ageLower; this.ageUpper = ageUpper; }
		public static PlaySignal none(){ return new PlaySignal(null, null); }
	}

	// Where the single pass/fail answer lives (a preference on the device).
	public interface Store {
		String get();          // "pass", "fail" or null
		void put(String value);
	}

	public static final String PASS = "pass", FAIL = "fail";

	// The platform's way to ask Google Play (set by the Android launcher; desktop and tests leave it null = no answer).
	// Must call done exactly once, on any thread, with PlaySignal.none() when Play can't or won't say.
	public interface PlaySource {
		void request(java.util.function.Consumer<PlaySignal> done);
	}

	public static volatile PlaySource play = null;

	public static void askPlay(java.util.function.Consumer<PlaySignal> done){
		PlaySource p = play;
		if (p == null) { done.accept(PlaySignal.none()); return; }
		try { p.request(done); } catch (RuntimeException e){ done.accept(PlaySignal.none()); }
	}

	private CommunityAgeGate(){}

	public static Result check(PlaySignal play, Store store){
		if (play != null){
			if (play.ageLower != null && play.ageLower >= MIN_AGE) return Result.UNLOCKED;
			if (play.ageUpper != null && play.ageUpper < MIN_AGE) return Result.LOCKED;
		}
		String stored = store.get();
		if (PASS.equals(stored)) return Result.UNLOCKED;
		if (FAIL.equals(stored)) return Result.LOCKED;
		return Result.ASK_BIRTH_DATE;
	}

	// Age in whole years from birth month/year, counting a birthday in the current month as not yet reached.
	static int ageFrom(int birthMonth, int birthYear, int nowMonth, int nowYear){
		int age = nowYear - birthYear;
		if (birthMonth >= nowMonth) age--;
		return age;
	}

	// Called once with the player's answer. Refuses a second answer: the first stored answer stands.
	// Returns the gate result after the answer. month is 1-12.
	public static Result answer(int birthMonth, int birthYear, int nowMonth, int nowYear, Store store){
		if (store.get() != null) return PASS.equals(store.get()) ? Result.UNLOCKED : Result.LOCKED;
		if (birthMonth < 1 || birthMonth > 12 || birthYear > nowYear || birthYear < nowYear - 120)
			throw new IllegalArgumentException("not a real birth month/year");
		boolean ok = ageFrom(birthMonth, birthYear, nowMonth, nowYear) >= MIN_AGE;
		store.put(ok ? PASS : FAIL);
		return ok ? Result.UNLOCKED : Result.LOCKED;
	}
}
