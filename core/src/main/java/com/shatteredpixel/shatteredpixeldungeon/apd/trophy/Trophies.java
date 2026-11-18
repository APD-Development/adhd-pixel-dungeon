package com.shatteredpixel.shatteredpixeldungeon.apd.trophy;

import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;

/**
 * APD trophy outlines: lifetime defeats of an enemy type (the Bestiary's all-runs count) earn a
 * bronze, silver or gold outline. Bosses and rare enemies use lower thresholds.
 */
public final class Trophies {
	private Trophies(){}

	public static final int NONE = 0, BRONZE = 1, SILVER = 2, GOLD = 3;
	private static final int[] NORMAL = {100, 500, 2000};
	private static final int[] SPECIAL = {5, 25, 100};

	public static final int BRONZE_COLOR = 0xCD7F32, SILVER_COLOR = 0xD8D8E0, GOLD_COLOR = 0xFFD700;

	public static boolean special(Class<?> cls){
		return cls != null && (Bestiary.BOSSES.entities().contains(cls) || Bestiary.RARE.entities().contains(cls));
	}

	public static int[] thresholds(Class<?> cls){
		return (special(cls) ? SPECIAL : NORMAL).clone();
	}

	public static int tierFor(int defeats, boolean special){
		int[] t = special ? SPECIAL : NORMAL;
		if (defeats >= t[2]) return GOLD;
		if (defeats >= t[1]) return SILVER;
		if (defeats >= t[0]) return BRONZE;
		return NONE;
	}

	public static int tier(Class<?> cls){
		if (cls == null) return NONE;
		try { return tierFor(Bestiary.encounterCount(cls), special(cls)); }
		catch (RuntimeException e){ return NONE; }
	}

	public static int color(int tier){
		switch (tier){
			case GOLD: return GOLD_COLOR;
			case SILVER: return SILVER_COLOR;
			case BRONZE: return BRONZE_COLOR;
			default: return 0;
		}
	}

	public static String name(int tier){
		switch (tier){
			case GOLD: return "Gold";
			case SILVER: return "Silver";
			case BRONZE: return "Bronze";
			default: return "";
		}
	}

	/** One line for the Bestiary entry, e.g. "Trophy: Silver. Gold at 2,000 defeats." */
	public static String describe(Class<?> cls){
		int defeats;
		try { defeats = Bestiary.encounterCount(cls); } catch (RuntimeException e){ return ""; }
		boolean sp = special(cls);
		int[] t = sp ? SPECIAL : NORMAL;
		int tier = tierFor(defeats, sp);
		String next = tier >= GOLD ? "" : " " + name(tier + 1) + " at _" + String.format("%,d", t[tier]) + "_ defeats.";
		return (tier == NONE ? "Trophy: none yet." : "Trophy: _" + name(tier) + "_.") + next;
	}
}
