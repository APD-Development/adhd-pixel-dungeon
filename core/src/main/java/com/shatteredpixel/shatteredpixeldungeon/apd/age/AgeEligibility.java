package com.shatteredpixel.shatteredpixeldungeon.apd.age;

/** Verified age signals and the independent player humor preference.
 * These are APD policy states, not claimed Google Play API status values.
 */
public final class AgeEligibility {
	public enum State { ELIGIBLE, INELIGIBLE, UNKNOWN, UNAVAILABLE }

	private AgeEligibility(){}

	//No age provider is integrated. Missing signals must not be treated as verified adulthood.
	public static State current(){
		return State.UNAVAILABLE;
	}

	public static boolean permitsAdult(State state){
		return state != State.INELIGIBLE;
	}

	//Everyone defaults to Clean. Only a player not known to be underage may choose Adult.
	//Chris, Nov 18 2026: Adult humor is OFF by default for everyone (Clean mode), even a verified adult.
	//A verified adult can still turn it on in settings; nothing else about the choice changed.
	public static boolean defaultAdultHumor(State state){
		return false;
	}

	public static boolean adultHumor(State state, boolean preferred){
		return preferred && permitsAdult(state);
	}
}
