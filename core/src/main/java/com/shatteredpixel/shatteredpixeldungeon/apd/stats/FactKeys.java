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

//The stable names of derived facts. A name is never reused for something else. Which source can know a fact:
//APD run logs fill run.*, result.*, journey.*, final.*, combat.*, items.*, gen.* and link.*; imported old records fill
//run.*, result.*, legacy.* and loadout.*. The same name means the same thing in both.
public final class FactKeys {

	private FactKeys(){}

	//---- identity and setup ----
	public static final String RUN_ID = "run.id";                 //text: the run's id (shared by every segment of one run)
	public static final String LOG_ID = "run.log";                //text: the log (or import record) this row was derived from
	public static final String SEGMENT = "run.segment";           //num: which stretch of the run this log holds (0 first)
	public static final String PARTIAL = "run.partial";           //num 1: the log starts in the middle of a run, earlier history is unknown
	public static final String TELEMETRY = "run.telemetry";       //text: "1.4"
	public static final String STATUS = "run.status";             //text: sealed / open / damaged / unreadable / legacy
	public static final String APD_VERSION = "run.apdVersion";
	public static final String INITIAL_VERSION = "run.initialVersion"; //num: the game's version code when the run began
	public static final String SEED = "run.seed";
	public static final String SEED_TYPE = "run.seedType";        //num: LifecycleEvents.SEED_*
	public static final String CUSTOM_SEED = "run.customSeed";
	public static final String DAILY = "run.daily";               //num 0/1
	public static final String EXTERNAL_MODS = "run.externalMods"; //num 0/1: an unofficial mod was present (always 0 until a mod system exists)
	public static final String CHALLENGES = "run.challenges";     //num: the challenge bit mask
	public static final String HERO_CLASS = "run.heroClass";
	public static final String RULESET = "run.ruleset";
	public static final String EXOTIC_CHANCE = "run.exoticChance";
	public static final String DIFFICULTY = "run.difficulty";                  //num 1-5, absent = not recorded (telemetry 1.7+)
	public static final String DIFFICULTY_BALANCE = "run.difficultyBalance";    //num: 0 = placeholder balance, 1+ = an approved balance revision
	public static final String START_MILLIS = "run.startMillis";
	public static final String DATE = "run.date";                 //text yyyy-MM-dd, legacy records only
	public static final String GAME_VERSION = "run.gameVersion";  //text "v3.1.1", legacy records only
	public static final String ABANDONED_EVENTS = "run.abandonedEvents"; //num: events kept but excluded (rolled back)
	public static final String ROLLBACKS = "run.rollbacks";

	//---- how it ended ----
	public static final String OUTCOME = "result.outcome";        //num: LifecycleEvents.OUTCOME_*
	public static final String CAUSE = "result.cause";
	public static final String DEPTH = "result.depth";            //depth at the end (APD runs only)
	public static final String DEEPEST = "result.deepest";
	public static final String HERO_LEVEL = "result.heroLevel";
	public static final String SCORE = "result.score";
	public static final String TURNS = "result.turns";            //APD runs only
	public static final String ASCENDING = "result.ascending";    //legacy records only

	//---- journey, from the checkpoints and level events ----
	public static final String LEVELS_ENTERED = "journey.levelsEntered";
	public static final String DEEPEST_ENTERED = "journey.deepestEntered";
	public static final String FINAL_HERO_LEVEL = "final.heroLevel";
	public static final String FINAL_HP_MAX = "final.hpMax";
	public static final String FINAL_STRENGTH = "final.strength";
	public static final String FINAL_GOLD = "final.gold";
	public static final String FINAL_SUBCLASS = "final.subClass";

	//---- combat and health (telemetry 1.3 and later) ----
	public static final String HERO_ATTACKS = "combat.heroAttacks";
	public static final String HERO_HITS = "combat.heroHits";
	public static final String ATTACKS_ON_HERO = "combat.attacksOnHero";
	public static final String HITS_ON_HERO = "combat.hitsOnHero";
	public static final String DAMAGE_DEALT = "combat.damageDealt";   //damage shown on enemies
	public static final String DAMAGE_TAKEN = "combat.damageTaken";   //damage shown on the hero (shielding included)
	public static final String HERO_KILLS = "combat.heroKills";
	public static final String ENEMY_DEATHS = "combat.enemyDeaths";
	public static final String XP_GAINED = "combat.xpGained";
	public static final String LEVEL_UPS = "combat.levelUps";
	public static final String HEALED = "combat.healed";              //all HP healed: exact since telemetry 1.6 (except the part in HEALED_INFERRED)
	public static final String HEALED_INFERRED = "combat.healedInferred"; //the part of HEALED only inferred from the hero's HP between turns
	//HP healed by one kind of source (telemetry 1.6, from the INTERNAL HEAL_DETAIL events): one key per CombatEvents.HEAL_CATEGORIES entry
	public static String healedFrom( String category ){
		return "combat.healedFrom." + category;
	}
	public static final String HERO_DEATHS = "combat.heroDeaths";
	public static final String REVIVALS = "combat.revivals";

	//---- items (telemetry 1.2 and later) ----
	public static final String ITEMS_REGISTERED = "items.registered";
	public static final String ITEMS_ACQUIRED = "items.acquired";
	public static final String ITEMS_EQUIPPED = "items.equipped";     //distinct items ever equipped
	public static final String EQUIP_EVENTS = "items.equipEvents";

	//---- generation (telemetry 1.4 and later) ----
	public static final String GEN_LEVELS = "gen.levels";
	public static final String GEN_ITEMS = "gen.items";
	public static final String GEN_MOBS = "gen.mobs";

	//---- links ----
	public static final String RANKING_ID = "link.rankingId";         //text: the ranking record id this run produced
	public static final String LINKED_RUN = "link.apdRun";            //legacy rows: the APD run this record is the same run as
	public static final String LINK_KIND = "link.kind";               //num: LINK_EXACT / LINK_PROBABLE
	public static final String LINKED_LEGACY = "link.legacyId";       //APD rows: the imported record that is the same run
	public static final int LINK_EXACT = 1, LINK_PROBABLE = 2;

	//---- imported statistics, named after what SPD called them ----
	public static final String LEGACY_PREFIX = "legacy.";
	//final equipment of an imported record: loadout.<slot> (class), loadout.<slot>.level (num)
	public static final String LOADOUT_PREFIX = "loadout.";
}
