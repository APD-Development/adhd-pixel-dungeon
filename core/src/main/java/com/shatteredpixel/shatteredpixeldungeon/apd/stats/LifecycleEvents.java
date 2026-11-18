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
import java.util.List;

//Encoders and decoders for the run lifecycle events of telemetry 1.1. (The per-level seed is not stored: it is a
//function of the run seed and the depth, and computing it would touch SPD's RNG, which the recorder never does.) Pure data, no game classes: the game-side
//recorder fills these in, tests and (later) the statistics system read them back.
//
//Field order inside a payload is part of the format. A telemetry 1.x version may only ADD fields at the END of a
//payload (older readers stop reading early), never reorder or change existing ones.
public final class LifecycleEvents {

	public static final int EXOTIC_NOT_RECORDED = 255;

	private LifecycleEvents(){}

	//event flag bits
	public static final int FLAG_PARTIAL = 1; //RUN_START: the log starts in the middle of a run, earlier history is unknown
	public static final int FLAG_DETAILS_UNKNOWN = 2; //RUN_END: only the outcome is known, every other field is just a placeholder

	//RUN_START.seedType
	public static final int SEED_RANDOM = 0, SEED_CUSTOM = 1, SEED_DAILY = 2, SEED_DAILY_REPLAY = 3;

	//RUN_START.startKind: why this log begins
	public static final int START_NEW_RUN = 0;
	public static final int START_ADOPTED_RUN = 1;      //the save existed before APD recorded runs (no run id in it)
	public static final int START_LOG_MISSING = 2;      //the save names a run id but its log is gone
	public static final int START_NEW_SEGMENT = 3;      //the previous log could not be continued (e.g. another telemetry version)
	public static final int START_AFTER_RUN_ENDED = 4;  //the save is of a run whose log is already sealed

	//RUN_END.outcome
	public static final int OUTCOME_DIED = 0, OUTCOME_WON = 1, OUTCOME_ABANDONED = 2;

	//LEVEL_EXIT.reason
	public static final int EXIT_LEAVING = 0, EXIT_RUN_ENDED = 1;

	//CHECKPOINT.reason
	public static final int CP_RUN_START = 1, CP_LEVEL_ENTER = 2, CP_LEVEL_EXIT = 3, CP_SAVE = 4, CP_RUN_END = 5;

	//ROLLBACK.kind
	public static final int ROLLBACK_MATCHED = 0;     //the loaded save is a known SAVE marker, later events were abandoned
	public static final int ROLLBACK_UNMATCHED = 1;   //events exist after the last marker we can use but the save matches none: uncertain

	//CHECKPOINT item slots
	public static final int SLOT_WEAPON = 0, SLOT_ARMOR = 1, SLOT_ARTIFACT = 2, SLOT_MISC = 3, SLOT_RING = 4,
			SLOT_SECOND_WEAPON = 5, SLOT_BACKPACK = 100;

	//events that describe the recording, not the game. They are never part of an abandoned branch.
	public static boolean isRecordingMetaEvent( int type ){
		return type == EventTypes.GAP || type == EventTypes.ROLLBACK || type == EventTypes.SESSION_START;
	}

	//---- RUN_START ---------------------------------------------------------------------------------------

	public static final class RunStart {
		public String runId = "";
		public int segment;
		public int startKind;
		public int apdVersionCode;
		public String apdVersionName = "";
		public String baseGame = "";
		public int initialVersion;
		public long seed;
		public int seedType;
		public String customSeedText = "";
		public int challenges;
		public String heroClass = "";
		public String rulesetId = "";
		public String rulesetText = "";
		//EXOTIC_NOT_RECORDED when the run did not record its exotic setting (a real chance is 0-80, the demo data uses up to 100)
		public int exoticChance;
		public boolean introActive;
		public boolean externalModsPresent;
		public String platform = "";
		public long realTimeMillis;
		//telemetry 1.7: the run's difficulty 1-5 (0 = not recorded: a run from before difficulty existed) and the balance revision it was played with
		public int difficulty;
		public int difficultyBalance;
		// Additive custom-rules snapshot. Empty means not recorded, never implicit defaults.
		public String customRules = "";
        public int customScoreVersion;
        public String customScoreStatus="",customScoreCalculation="",customScoreMultiplier="";
	}

	public static boolean emitRunStart( RunLogWriter w, long clock, boolean partial, RunStart s ){
		return w.event(EventTypes.RUN_START).clock(clock).flags(partial ? FLAG_PARTIAL : 0)
				.string(s.runId).unsigned(s.segment).unsigned(s.startKind)
				.unsigned(s.apdVersionCode).string(s.apdVersionName).string(s.baseGame).unsigned(Math.max(0, s.initialVersion))
				.signed(s.seed).unsigned(s.seedType).string(s.customSeedText).unsigned(Math.max(0, s.challenges))
				.string(s.heroClass).string(s.rulesetId).string(s.rulesetText).unsigned(Math.max(0, s.exoticChance))
				.bool(s.introActive).bool(s.externalModsPresent).string(s.platform).unsigned(Math.max(0, s.realTimeMillis))
				.unsigned(Math.max(0, s.difficulty)).unsigned(Math.max(0, s.difficultyBalance)).string(s.customRules).unsigned(s.customScoreVersion).string(s.customScoreStatus).string(s.customScoreCalculation).string(s.customScoreMultiplier)
				.emit();
	}

	public static RunStart decodeRunStart( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		RunStart s = new RunStart();
		s.runId = str(p.string());
		s.segment = p.unsignedInt();
		s.startKind = p.unsignedInt();
		s.apdVersionCode = p.unsignedInt();
		s.apdVersionName = str(p.string());
		s.baseGame = str(p.string());
		s.initialVersion = p.unsignedInt();
		s.seed = p.signed();
		s.seedType = p.unsignedInt();
		s.customSeedText = str(p.string());
		s.challenges = p.unsignedInt();
		s.heroClass = str(p.string());
		s.rulesetId = str(p.string());
		s.rulesetText = str(p.string());
		s.exoticChance = p.unsignedInt();
		s.introActive = p.bool();
		s.externalModsPresent = p.bool();
		s.platform = str(p.string());
		s.realTimeMillis = p.unsigned();
		//1.7 appended these; an older log ends here and its difficulty stays 0 (not recorded)
		if (p.hasRemaining()){
			s.difficulty = p.unsignedInt();
			s.difficultyBalance = p.hasRemaining() ? p.unsignedInt() : 0;
		}
		if (p.hasRemaining()) s.customRules = str(p.string());
        if(p.hasRemaining())s.customScoreVersion=p.unsignedInt();
        if(p.hasRemaining())s.customScoreStatus=str(p.string());
        if(p.hasRemaining())s.customScoreCalculation=str(p.string());
        if(p.hasRemaining())s.customScoreMultiplier=str(p.string());
		return s;
	}

	public static boolean isPartial( ApdEvent e ){
		return (e.flags & FLAG_PARTIAL) != 0;
	}

	//---- RUN_END -----------------------------------------------------------------------------------------

	public static final class RunEnd {
		public int outcome;
		public String cause = "";
		public int depth;
		public int deepest;
		public int heroLevel;
		public long score;
		public long turns;
		public int rulesetChanges;
        public boolean customAdjustedKnown;
        public long customAdjustedScore;
		// Optional tail: older logs leave ascension unknown, never false by default.
		public boolean ascendedKnown;
		public boolean ascended;
	}

	public static boolean emitRunEnd( RunLogWriter w, long clock, RunEnd r ){
		return emitRunEnd(w, clock, r, 0);
	}

	public static boolean emitRunEnd( RunLogWriter w, long clock, RunEnd r, int flags ){
		EventBuilder b=w.event(EventTypes.RUN_END).clock(clock).flags(flags).unsigned(r.outcome).string(r.cause).unsigned(Math.max(0, r.depth))
                .unsigned(Math.max(0, r.deepest)).unsigned(Math.max(0, r.heroLevel)).signed(r.score).unsigned(Math.max(0, r.turns))
                .unsigned(r.rulesetChanges).bool(r.customAdjustedKnown);
        if(r.customAdjustedKnown)b.signed(r.customAdjustedScore);
		b.bool(r.ascendedKnown);
		if (r.ascendedKnown) b.bool(r.ascended);
        return b.emit();
	}

	public static RunEnd decodeRunEnd( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		RunEnd r = new RunEnd();
		r.outcome = p.unsignedInt();
		r.cause = str(p.string());
		r.depth = p.unsignedInt();
		r.deepest = p.unsignedInt();
		r.heroLevel = p.unsignedInt();
		r.score = p.signed();
		r.turns = p.unsigned();
		r.rulesetChanges = p.unsignedInt();
        if(p.hasRemaining())r.customAdjustedKnown=p.bool();
        if(r.customAdjustedKnown)r.customAdjustedScore=p.signed();
		if (p.hasRemaining()) r.ascendedKnown=p.bool();
		if (r.ascendedKnown) r.ascended=p.bool();
		return r;
	}

	//---- SAVE --------------------------------------------------------------------------------------------

	//Written AFTER the game save was written and after its CHECKPOINT. It is the last event of a save, so every event
	//after it belongs to play that exists only if that save is never loaded.
	public static final class Save {
		public int slot;
		public int depth;
		public int branch;
		//the save's own "last played" time stamp, the key used to recognise this save when it is loaded again
		public long lastPlayedMillis;
		public long turn;
	}

	public static boolean emitSave( RunLogWriter w, long clock, Save s ){
		return w.event(EventTypes.SAVE).clock(clock).unsigned(s.slot).unsigned(Math.max(0, s.depth)).unsigned(s.branch)
				.unsigned(Math.max(0, s.lastPlayedMillis)).unsigned(Math.max(0, s.turn)).emit();
	}

	public static Save decodeSave( ApdEvent e ){
		PayloadReader p = e.reader(null);
		Save s = new Save();
		s.slot = p.unsignedInt();
		s.depth = p.unsignedInt();
		s.branch = p.unsignedInt();
		s.lastPlayedMillis = p.unsigned();
		s.turn = p.unsigned();
		return s;
	}

	//---- LEVEL_ENTER / LEVEL_EXIT ------------------------------------------------------------------------

	public static final class LevelEnter {
		public int depth;
		public int branch;
		//how the hero arrived: DESCEND, ASCEND, CONTINUE, RESURRECT, RETURN, FALL, RESET, NONE
		public String entryMode = "";
		public String levelClass = "";
		public String feeling = "";
	}

	public static boolean emitLevelEnter( RunLogWriter w, long clock, LevelEnter l ){
		return w.event(EventTypes.LEVEL_ENTER).clock(clock).unsigned(Math.max(0, l.depth)).unsigned(l.branch)
				.string(l.entryMode).string(l.levelClass).string(l.feeling).emit();
	}

	public static LevelEnter decodeLevelEnter( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		LevelEnter l = new LevelEnter();
		l.depth = p.unsignedInt();
		l.branch = p.unsignedInt();
		l.entryMode = str(p.string());
		l.levelClass = str(p.string());
		l.feeling = str(p.string());
		return l;
	}

	public static final class LevelExit {
		public int depth;
		public int branch;
		//game turns since the matching LEVEL_ENTER in this log, -1 if that enter is not in this log
		public long turnsSpent = -1;
		public int reason;
	}

	public static boolean emitLevelExit( RunLogWriter w, long clock, LevelExit l ){
		return w.event(EventTypes.LEVEL_EXIT).clock(clock).unsigned(Math.max(0, l.depth)).unsigned(l.branch)
				.signed(l.turnsSpent).unsigned(l.reason).emit();
	}

	public static LevelExit decodeLevelExit( ApdEvent e ){
		PayloadReader p = e.reader(null);
		LevelExit l = new LevelExit();
		l.depth = p.unsignedInt();
		l.branch = p.unsignedInt();
		l.turnsSpent = p.signed();
		l.reason = p.unsignedInt();
		return l;
	}

	//---- CHECKPOINT --------------------------------------------------------------------------------------

	public static final class ItemEntry {
		public int slot;
		public String className = "";
		public int quantity;
		//the item's real level. levelKnown says whether the PLAYER knew it at the time.
		public int level;
		public boolean levelKnown;
		public boolean cursedKnown;
	}

	//A compact summary of the hero, not a save state. Item levels are stored as they truly were together with what the
	//player knew, so later analysis can honour "do not treat later identification as earlier knowledge".
	public static final class Checkpoint {
		public int reason;
		public int depth;
		public int branch;
		public long turn;
		public int heroLevel;
		public int exp;
		public int hp;
		public int hpMax;
		public int strength;
		public long hunger = -1;
		public int shielding;
		public int gold;
		public int energy;
		public String heroClass = "";
		public String subClass = "";
		public final List<ItemEntry> items = new ArrayList<>();
	}

	public static boolean emitCheckpoint( RunLogWriter w, long clock, Checkpoint c ){
		EventBuilder b = w.event(EventTypes.CHECKPOINT).clock(clock).unsigned(c.reason).unsigned(Math.max(0, c.depth))
				.unsigned(c.branch).unsigned(Math.max(0, c.turn)).unsigned(Math.max(0, c.heroLevel)).unsigned(Math.max(0, c.exp))
				.signed(c.hp).signed(c.hpMax).signed(c.strength).signed(c.hunger).unsigned(Math.max(0, c.shielding))
				.signed(c.gold).signed(c.energy).string(c.heroClass).string(c.subClass).unsigned(c.items.size());
		for (ItemEntry i : c.items){
			b.unsigned(i.slot).string(i.className).unsigned(Math.max(0, i.quantity)).signed(i.level)
					.unsigned((i.levelKnown ? 1 : 0) | (i.cursedKnown ? 2 : 0));
		}
		return b.emit();
	}

	public static Checkpoint decodeCheckpoint( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		Checkpoint c = new Checkpoint();
		c.reason = p.unsignedInt();
		c.depth = p.unsignedInt();
		c.branch = p.unsignedInt();
		c.turn = p.unsigned();
		c.heroLevel = p.unsignedInt();
		c.exp = p.unsignedInt();
		c.hp = (int) p.signed();
		c.hpMax = (int) p.signed();
		c.strength = (int) p.signed();
		c.hunger = p.signed();
		c.shielding = p.unsignedInt();
		c.gold = (int) p.signed();
		c.energy = (int) p.signed();
		c.heroClass = str(p.string());
		c.subClass = str(p.string());
		int n = p.unsignedInt();
		for (int k = 0; k < n; k++){
			ItemEntry i = new ItemEntry();
			i.slot = p.unsignedInt();
			i.className = str(p.string());
			i.quantity = p.unsignedInt();
			i.level = (int) p.signed();
			int flags = p.unsignedInt();
			i.levelKnown = (flags & 1) != 0;
			i.cursedKnown = (flags & 2) != 0;
			c.items.add(i);
		}
		return c;
	}

	//---- SESSION_START -----------------------------------------------------------------------------------

	//The app opened (or resumed) this run's log. Marks a boundary between app sessions.
	public static final class SessionStart {
		public int apdVersionCode;
		public String apdVersionName = "";
		public String platform = "";
		public long realTimeMillis;
	}

	public static boolean emitSessionStart( RunLogWriter w, long clock, SessionStart s ){
		return w.event(EventTypes.SESSION_START).clock(clock).unsigned(s.apdVersionCode).string(s.apdVersionName)
				.string(s.platform).unsigned(Math.max(0, s.realTimeMillis)).emit();
	}

	public static SessionStart decodeSessionStart( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		SessionStart s = new SessionStart();
		s.apdVersionCode = p.unsignedInt();
		s.apdVersionName = str(p.string());
		s.platform = str(p.string());
		s.realTimeMillis = p.unsigned();
		return s;
	}

	//---- ROLLBACK ----------------------------------------------------------------------------------------

	//The game was loaded from a save that is older than the end of the log. Events after the matched SAVE marker
	//describe play that no longer exists in the game: they are the "abandoned branch".
	public static final class Rollback {
		public int kind;
		//event index of the SAVE marker the loaded save corresponds to (kind MATCHED)
		public long matchedSaveIndex = -1;
		//how many events lay between that marker and this event
		public long abandonedEvents;
		//clock of the last abandoned event
		public long lastAbandonedClock;
	}

	public static boolean emitRollback( RunLogWriter w, long clock, Rollback r ){
		return w.event(EventTypes.ROLLBACK).clock(clock).unsigned(r.kind).signed(r.matchedSaveIndex)
				.unsigned(Math.max(0, r.abandonedEvents)).signed(r.lastAbandonedClock).emit();
	}

	public static Rollback decodeRollback( ApdEvent e ){
		PayloadReader p = e.reader(null);
		Rollback r = new Rollback();
		r.kind = p.unsignedInt();
		r.matchedSaveIndex = p.signed();
		r.abandonedEvents = p.unsigned();
		r.lastAbandonedClock = p.signed();
		return r;
	}

	//---- RULESET_CHANGE ----------------------------------------------------------------------------------

	//A gameplay-affecting setting changed during the run (e.g. the exotic chance), so the ruleset id is now different.
	public static final class RulesetChange {
		public String oldId = "";
		public String newId = "";
		public String newText = "";
		public int depth;
	}

	public static boolean emitRulesetChange( RunLogWriter w, long clock, RulesetChange r ){
		return w.event(EventTypes.RULESET_CHANGE).clock(clock).string(r.oldId).string(r.newId).string(r.newText)
				.unsigned(Math.max(0, r.depth)).emit();
	}

	public static RulesetChange decodeRulesetChange( ApdEvent e, StringLookup strings ){
		PayloadReader p = e.reader(strings);
		RulesetChange r = new RulesetChange();
		r.oldId = str(p.string());
		r.newId = str(p.string());
		r.newText = str(p.string());
		r.depth = p.unsignedInt();
		return r;
	}

	private static String str( String s ){
		return s == null ? "" : s;
	}
}


