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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

//The registry of event type IDs.
//
//RULES (checked by a test against core/src/test/resources/apd/event_type_manifest.txt):
// - an ID is never reused for a different meaning, and never removed from this list
// - new event types only ever get new IDs
// - ID 0 is invalid
//
//Ranges: 1-255 storage infrastructure, 256 and up are for gameplay events, added in later phases.
//A reader that meets an ID it does not know skips the event, it is never an error.
public final class EventTypes {

	private EventTypes(){}

	public static final int INVALID = 0;

	//Recording was interrupted. payload: varint reason (GapReason), varint amount
	public static final int GAP = 1;

	//GapReason values, the first payload number of a GAP event
	public static final int GAP_DROPPED_EVENTS = 1;  //amount = events lost because the write queue was full
	public static final int GAP_RECOVERED_TAIL = 2;  //amount = bytes of damaged tail cut off when the log was reopened
	public static final int GAP_WRITER_FAILED = 3;   //amount = events lost after the writer failed
	public static final int GAP_RECORDING_BEHIND_SAVE = 4; //the game save is further on than the log, amount = game turns missing
	//A redacted export omitted suspect/unreadable source content. Amount is 0: the lost event count is unknown.
	public static final int GAP_REDACTED_INCOMPLETE = 5;

	//---- run lifecycle events (telemetry 1.1), payloads are defined and tested in LifecycleEvents ----
	public static final int RUN_START = 256;
	public static final int RUN_END = 257;
	public static final int SAVE = 258;
	public static final int LEVEL_ENTER = 259;
	public static final int LEVEL_EXIT = 260;
	public static final int CHECKPOINT = 261;
	public static final int SESSION_START = 262;
	public static final int ROLLBACK = 263;
	public static final int RULESET_CHANGE = 264;

	//---- item identity and lineage events (telemetry 1.2), payloads are defined and tested in ItemEvents ----
	//PUBLIC ones only carry what the player could see at that moment. INTERNAL ones carry hidden truth and lineage.
	public static final int ITEM_SEEN = 265;
	public static final int ITEM_ACQUIRED = 266;
	public static final int ITEM_LEFT = 267;
	public static final int ITEM_EQUIPPED = 268;
	public static final int ITEM_UNEQUIPPED = 269;
	public static final int ITEM_KNOWLEDGE = 270;
	public static final int ITEM_CLASS_KNOWN = 271;
	public static final int ITEM_REGISTERED = 272;
	public static final int ITEM_LINEAGE = 273;
	public static final int ITEM_TRUTH_CHANGED = 274;

	//---- combat and health events (telemetry 1.3), payloads are defined and tested in CombatEvents ----
	//PUBLIC ones only carry what the player can see (names the game shows, damage numbers, their own HP). The *_DETAIL events
	//are INTERNAL: true classes, ids, exact enemy HP and the numbers behind a hit.
	public static final int ATTACK = 275;
	public static final int ATTACK_DETAIL = 276;
	public static final int DAMAGE = 277;
	public static final int DAMAGE_DETAIL = 278;
	public static final int HEAL = 279;               //hero healing: inferred in 1.3 (flag INFERRED); exact since 1.6 (no INFERRED flag), see HEAL_DETAIL
	public static final int MAX_HP_CHANGE = 280;
	public static final int SHIELD_GAIN = 281;
	public static final int HUNGER_CHANGE = 282;      //hunger was changed by eating or an effect (exact)
	public static final int HUNGER_STATE = 283;       //hunger crossed between well fed / normal / hungry / starving
	public static final int XP_GAIN = 284;
	public static final int LEVEL_UP = 285;
	public static final int KILL = 286;
	public static final int KILL_DETAIL = 287;
	public static final int HERO_DEATH = 288;
	public static final int HERO_REVIVED = 289;
	public static final int HP_CHANGE_INFERRED = 290; //hero HP went down with no damage event explaining it (inferred)

	//---- generation provenance events (telemetry 1.4), payloads are defined and tested in GenerationEvents ----
	//All INTERNAL: they describe what the game placed in a level, including everything the player has not found.
	public static final int LEVEL_GENERATED = 291;
	public static final int ITEM_GENERATED = 292;
	public static final int MOB_GENERATED = 293;
	public static final int MOB_ID_ASSIGNED = 294;   //links a generated creature to the id the game gives it when the level is entered

	//---- legacy import and ranking link (telemetry 1.5), payloads are defined and tested in LegacyEvents ----
	//LEGACY_* events live in their own import logs (legacy/), never in a run log. They carry what the records in APD's own rankings file
	//held, field by field, with fields the old record did not contain marked UNKNOWN (never zero).
	public static final int LEGACY_RECORD = 295;
	public static final int LEGACY_TOTALS = 296;       //SPD's own played / won counters
	public static final int LEGACY_DAILY_SCORE = 297;
	public static final int RANKING_LINK = 298;        //in a run log: the id SPD gave the ranking record this run produced

	//---- exact healing (telemetry 1.6), payload is defined and tested in CombatEvents ----
	//INTERNAL: the true source of an exact heal (which item, effect or ability, and what it asked for). The PUBLIC HEAL beside it only says
	//how much the hero's HP went up, which the HP bar shows anyway.
	public static final int HEAL_DETAIL = 299;

	//---- generic facts (telemetry 1.8), payloads are defined and tested in FactEvents ----
	//One observation of any registered kind of fact (see FactKinds): what happened is named by the kind's key inside the payload, so a new
	//mechanic never needs a new event type. A kind is PUBLIC or INTERNAL; the event type carries that, so hidden truth is filtered by
	//the type like every other event. A SERIES is many observations of one kind packed into one event (delta encoded, lossless): it is
	//how high-frequency facts such as the hero's steps are kept at full resolution for a few bytes each.
	public static final int FACT = 300;
	public static final int FACT_INTERNAL = 301;
	public static final int FACT_SERIES = 302;
	public static final int FACT_SERIES_INTERNAL = 303;

	//Who may be shown an event's contents. Anything that is not PUBLIC must never reach a player-facing view, an export
	//meant for players, or a run-replication package without being filtered first.
	public enum Visibility {
		PUBLIC,   //only things the player could legitimately see or do at that moment
		INTERNAL  //may contain hidden information (true item class, true level, generation facts)
	}

	private static final Map<Integer, String> NAMES;
	static {
		Map<Integer, String> m = new LinkedHashMap<>();
		m.put(GAP, "GAP");
		m.put(RUN_START, "RUN_START");
		m.put(RUN_END, "RUN_END");
		m.put(SAVE, "SAVE");
		m.put(LEVEL_ENTER, "LEVEL_ENTER");
		m.put(LEVEL_EXIT, "LEVEL_EXIT");
		m.put(CHECKPOINT, "CHECKPOINT");
		m.put(SESSION_START, "SESSION_START");
		m.put(ROLLBACK, "ROLLBACK");
		m.put(RULESET_CHANGE, "RULESET_CHANGE");
		m.put(ITEM_SEEN, "ITEM_SEEN");
		m.put(ITEM_ACQUIRED, "ITEM_ACQUIRED");
		m.put(ITEM_LEFT, "ITEM_LEFT");
		m.put(ITEM_EQUIPPED, "ITEM_EQUIPPED");
		m.put(ITEM_UNEQUIPPED, "ITEM_UNEQUIPPED");
		m.put(ITEM_KNOWLEDGE, "ITEM_KNOWLEDGE");
		m.put(ITEM_CLASS_KNOWN, "ITEM_CLASS_KNOWN");
		m.put(ITEM_REGISTERED, "ITEM_REGISTERED");
		m.put(ITEM_LINEAGE, "ITEM_LINEAGE");
		m.put(ITEM_TRUTH_CHANGED, "ITEM_TRUTH_CHANGED");
		m.put(ATTACK, "ATTACK");
		m.put(ATTACK_DETAIL, "ATTACK_DETAIL");
		m.put(DAMAGE, "DAMAGE");
		m.put(DAMAGE_DETAIL, "DAMAGE_DETAIL");
		m.put(HEAL, "HEAL");
		m.put(MAX_HP_CHANGE, "MAX_HP_CHANGE");
		m.put(SHIELD_GAIN, "SHIELD_GAIN");
		m.put(HUNGER_CHANGE, "HUNGER_CHANGE");
		m.put(HUNGER_STATE, "HUNGER_STATE");
		m.put(XP_GAIN, "XP_GAIN");
		m.put(LEVEL_UP, "LEVEL_UP");
		m.put(KILL, "KILL");
		m.put(KILL_DETAIL, "KILL_DETAIL");
		m.put(HERO_DEATH, "HERO_DEATH");
		m.put(HERO_REVIVED, "HERO_REVIVED");
		m.put(HP_CHANGE_INFERRED, "HP_CHANGE_INFERRED");
		m.put(LEVEL_GENERATED, "LEVEL_GENERATED");
		m.put(ITEM_GENERATED, "ITEM_GENERATED");
		m.put(MOB_GENERATED, "MOB_GENERATED");
		m.put(MOB_ID_ASSIGNED, "MOB_ID_ASSIGNED");
		m.put(LEGACY_RECORD, "LEGACY_RECORD");
		m.put(LEGACY_TOTALS, "LEGACY_TOTALS");
		m.put(LEGACY_DAILY_SCORE, "LEGACY_DAILY_SCORE");
		m.put(RANKING_LINK, "RANKING_LINK");
		m.put(HEAL_DETAIL, "HEAL_DETAIL");
		m.put(FACT, "FACT");
		m.put(FACT_INTERNAL, "FACT_INTERNAL");
		m.put(FACT_SERIES, "FACT_SERIES");
		m.put(FACT_SERIES_INTERNAL, "FACT_SERIES_INTERNAL");
		NAMES = Collections.unmodifiableMap(m);
	}

	public static boolean isKnown( int type ){
		return NAMES.containsKey(type);
	}

	public static String name( int type ){
		String n = NAMES.get(type);
		return n != null ? n : "UNKNOWN_" + type;
	}

	//Every known type has exactly one visibility (a test enforces it). Unknown types are INTERNAL: when in doubt, hide.
	public static Visibility visibility( int type ){
		switch (type){
			case CHECKPOINT:        //stores true item levels
			case ITEM_REGISTERED:
			case ITEM_LINEAGE:
			case ITEM_TRUTH_CHANGED:
			case ATTACK_DETAIL:
			case DAMAGE_DETAIL:
			case KILL_DETAIL:
			case LEVEL_GENERATED:
			case ITEM_GENERATED:
			case MOB_GENERATED:
			case MOB_ID_ASSIGNED:
			case RANKING_LINK:
			case FACT_INTERNAL:
			case FACT_SERIES_INTERNAL:
				return Visibility.INTERNAL;
			case GAP: case RUN_START: case RUN_END: case SAVE: case LEVEL_ENTER: case LEVEL_EXIT:
			case SESSION_START: case ROLLBACK: case RULESET_CHANGE:
			case ITEM_SEEN: case ITEM_ACQUIRED: case ITEM_LEFT: case ITEM_EQUIPPED: case ITEM_UNEQUIPPED:
			case ITEM_KNOWLEDGE: case ITEM_CLASS_KNOWN:
			case ATTACK: case DAMAGE: case HEAL: case MAX_HP_CHANGE: case SHIELD_GAIN: case HUNGER_CHANGE:
			case HUNGER_STATE: case XP_GAIN: case LEVEL_UP: case KILL: case HERO_DEATH: case HERO_REVIVED:
			case HP_CHANGE_INFERRED:
			case FACT: case FACT_SERIES:
			case LEGACY_RECORD: case LEGACY_TOTALS: case LEGACY_DAILY_SCORE: //everything the old Rankings screens showed
				return Visibility.PUBLIC;
			default:
				return Visibility.INTERNAL;
		}
	}

	//id -> name, in registration order
	public static Map<Integer, String> all(){
		return NAMES;
	}
}
