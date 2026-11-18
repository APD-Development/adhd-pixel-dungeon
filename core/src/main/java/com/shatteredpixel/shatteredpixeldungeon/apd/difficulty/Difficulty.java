package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.replay.ReplayContext;

//APD's global difficulty system: levels 1-5, level 3 is the game as it is (the baseline). This class is the ONE place that knows what a
//level changes: gameplay code asks for a Modifier and never looks at the level itself, so a later balance pass edits the table below and
//nothing else (the few seams in the game call DifficultyHooks, which call here).
//
//STATE (balance revision 1, the first real pass): only ENEMY_HP, ENEMY_DAMAGE and RESOURCES are active; every other modifier is neutral (1.0).
//Level 3 is neutral by definition, whatever the table says, and its balance revision is always 0: the baseline never changes.
//
//WHAT THE ACTIVE MODIFIERS MEAN (exactly; see docs/APD-DIFFICULTY.md):
// ENEMY_HP      the health of ordinary creatures when Level.createMob makes them (the level's population and later respawns). Not bosses,
//               not creatures made any other way (mimics, statues, quest givers, summoned minions), not champions' own bonuses.
// ENEMY_DAMAGE  the damage of attacks resolved by Char.attack that a hostile non-boss creature makes. Not magic, gas, fire, traps or other
//               damage that bypasses Char.attack, and not the hero's or allies' attacks.
// RESOURCES     the NUMBER of ordinary random items RegularLevel.createItems places (its 3-5 Generator draws, +2 on a Large level).
//               Not the guaranteed per-floor items (food, strength potions, scrolls of upgrade, stylus, enchantment/intuition stones, trinket
//               catalyst), not special rooms, shops, quest rewards, bones, journal pages, monster loot, gold or any unique item.
//ROUNDING: counts and health are scaled as round-half-up(value * modifier) with a 1e-9 allowance so exact halves (5 x 0.7 = 3.5) always round up,
//never to chance; damage is a float and is not rounded here. Nothing here draws a random number.
//
//IDENTITY: a run's difficulty AND balance revision are fixed when it starts (Dungeon.difficulty / Dungeon.difficultyBalance, saved with the run) and are
//part of its ruleset identity, so UIMSD always knows them. Changing the setting, or a later build with a newer balance pass, only affects runs started
//afterwards: a run keeps playing with the revision it began with (revision 0 = no modifiers, the early placeholder; revision 1 = the first pass).
//Old revisions are never deleted from this class: they are what an old run, and a replay of it, is played with.
//
//NON-P2W (permanent): difficulty is a plain gameplay setting, free for everyone, and nothing may ever be sold that changes it or its results.
public final class Difficulty {

	private Difficulty(){}

	public static final int MIN = 1, DEFAULT = 3, MAX = 5;

	//what a level can change. Add a constant here when a new difficulty-sensitive system is brought under control; nothing else moves.
	public enum Modifier {
		ENEMY_HP,        //enemies' health when they are created
		ENEMY_DAMAGE,    //damage enemies deal
		RESOURCES,       //how much loot/consumables the dungeon generates
		HEALING,         //healing the hero receives
		FOOD,            //hunger and food
		ENEMY_SPAWN,     //how many enemies are generated and how fast new ones arrive
		BOSS,            //boss strength
		CHAMPION,        //chance of champion enemies
		TRAPS            //trap frequency and strength
	}

	//modifier by level (row = level 1..5, column = Modifier ordinal). Approved values, balance revision 1.
	private static final double[][] TABLE = new double[MAX][Modifier.values().length];

	//The newest balance revision by level (index = level - 1): 0 = no modifiers applied, 1+ = an approved balance pass. Level 3 is always 0.
	//TABLE holds revision 1. Revision 0 is neutral for every level. A future revision gets its own table and the older ones stay.
	private static final int[] REVISION = { 1, 1, 0, 1, 1 };

	static {
		for (double[] row : TABLE) java.util.Arrays.fill(row, 1.0);
		set(1, 0.70, 0.70, 1.40);
		set(2, 0.85, 0.85, 1.20);
		set(4, 1.20, 1.20, 0.85);
		set(5, 1.50, 1.50, 0.70);
	}

	private static void set( int level, double enemyHp, double enemyDamage, double resources ){
		TABLE[level - 1][Modifier.ENEMY_HP.ordinal()] = enemyHp;
		TABLE[level - 1][Modifier.ENEMY_DAMAGE.ordinal()] = enemyDamage;
		TABLE[level - 1][Modifier.RESOURCES.ordinal()] = resources;
	}

	public static int normalize( int level ){
		return level < MIN ? DEFAULT : level > MAX ? DEFAULT : level;
	}

	public static boolean isValid( int level ){
		return level >= MIN && level <= MAX;
	}

	//the difficulty the game is being played at now: the run's (Dungeon.difficulty), or the run being replayed by a floor replay
	public static int level(){
		if (ReplayContext.active()) return ReplayContext.difficulty();
		return Dungeon.difficulty;
	}

	//the balance revision the game is being played with now: the run's own, or the replayed run's
	public static int revision(){
		if (ReplayContext.active()) return ReplayContext.balance();
		return Dungeon.difficultyBalance;
	}

	//the newest balance revision of a level (what a NEW run at that level is played with)
	public static int balanceRevision( int level ){
		return level == DEFAULT || !isValid(level) ? 0 : REVISION[level - 1];
	}

	//true if this build knows how to play (or replay) a run with this difficulty and revision
	public static boolean supportsRevision( int level, int revision ){
		if (level == DEFAULT || !isValid(level)) return revision == 0;
		return revision == 0 || revision == 1;
	}

	//1.0 for the baseline level and for revision 0, whatever the table says
	public static double modifier( int level, int revision, Modifier m ){
		if (level == DEFAULT || !isValid(level) || revision <= 0) return 1.0;
		return TABLE[level - 1][m.ordinal()];     //revision 1 (the only one with values so far)
	}

	//a new run's modifier: the newest revision of the level
	public static double modifier( int level, Modifier m ){
		return modifier(level, balanceRevision(level), m);
	}

	public static double modifier( Modifier m ){
		return modifier(level(), revision(), m);
	}

	//v scaled by the current modifier; exactly v (no float rounding) when the modifier is neutral
	public static float scale( Modifier m, float v ){
		double k = modifier(m);
		return k == 1.0 ? v : (float) (v * k);
	}

	public static int scale( Modifier m, int v ){
		return scale(modifier(m), v);
	}

	//round-half-up(v * k); the allowance makes exact halves (5 x 0.7) round up whatever the binary error of the product
	static int scale( double k, int v ){
		return k == 1.0 ? v : (int) Math.floor(v * k + 0.5 + 1e-9);
	}

	//true when this level, at its newest revision, changes nothing (level 3)
	public static boolean isNeutral( int level ){
		for (Modifier m : Modifier.values()) if (modifier(level, m) != 1.0) return false;
		return true;
	}

	//placeholder wording (final names come with the language pass)
	public static String label( int level ){
		switch (level){
			case 1: return "1 - Easiest";
			case 2: return "2 - Easier";
			case 3: return "3 - Standard";
			case 4: return "4 - Harder";
			case 5: return "5 - Hardest";
			default: return "unknown";
		}
	}
}
