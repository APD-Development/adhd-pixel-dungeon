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


package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunTimeline;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.watabou.noosa.Game;
import com.watabou.utils.DeviceCompat;

import java.io.File;
import java.util.UUID;

//Records one run into its log. Package-private: the game only talks to ApdRecorder, which guarantees that nothing
//here can throw into gameplay.
//
//Terms: the "log id" names the log file and is what the game save stores as apd_run_id. The "run id" identifies the run
//itself. They are equal, except when a run continues in a new segment log (another telemetry version, say): then the
//run id stays the same and the log id changes.
final class RunRecorder {

	private static final int FLUSH_WAIT_MILLIS = 250;
	private static final int CLOSE_WAIT_MILLIS = 2000;

	private final StatsStore store;
	private final int slot;
	String logId;
	String runId;
	int segment;
	RunLogWriter writer;
	ItemTracker items;
	CombatTracker combat;
	GenerationTracker generation;
	FactTracker facts;
	PlayTracker play;
	boolean ended;

	private boolean levelOpen;
	private int curDepth, curBranch;
	private long curEnterClock;
	private final com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomScoreSnapshot scoringSnapshot= com.shatteredpixel.shatteredpixeldungeon.Dungeon.customScoring();
	private String rulesetId = "";
	private int rulesetChanges;

	private RunRecorder( StatsStore store, int slot, String logId, String runId, int segment ){
		this.store = store;
		this.slot = slot;
		this.logId = logId;
		this.runId = runId;
		this.segment = segment;
	}

	int slot(){
		return slot;
	}

	//---- starting ------------------------------------------------------------------------------------------

	static String newId(){
		return UUID.randomUUID().toString();
	}

	//A new log with a RUN_START. 'partial' means earlier history of this run is unknown.
	static RunRecorder startNew( StatsStore store, int slot, String logId, String runId, int segment, int startKind,
	                             boolean partial ){
		RunRecorder r = new RunRecorder(store, slot, logId, runId, segment);
		r.writer = store.startRun(logId, Game.version, System.currentTimeMillis());
		if (r.writer.isDisabled()) return r;

		long clock = GameSnapshot.turn();
		Ruleset rules = Ruleset.current();
		r.rulesetId = rules.id;
		r.emitSessionStart(clock);

		LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
		s.runId = runId;
		s.segment = segment;
		s.startKind = startKind;
		s.apdVersionCode = Game.versionCode;
		s.apdVersionName = Game.version == null ? "" : Game.version;
		s.baseGame = Ruleset.BASE_GAME;
		s.initialVersion = Dungeon.initialVersion;
		s.seed = Dungeon.seed;
		s.seedType = Dungeon.daily ? (Dungeon.dailyReplay ? LifecycleEvents.SEED_DAILY_REPLAY : LifecycleEvents.SEED_DAILY)
				: (Dungeon.customSeedText == null || Dungeon.customSeedText.isEmpty() ? LifecycleEvents.SEED_RANDOM : LifecycleEvents.SEED_CUSTOM);
		s.customSeedText = Dungeon.customSeedText == null ? "" : Dungeon.customSeedText;
		s.challenges = Dungeon.challenges;
		s.heroClass = Dungeon.hero == null || Dungeon.hero.heroClass == null ? "" : Dungeon.hero.heroClass.name();
		s.rulesetId = rules.id;
		s.rulesetText = rules.text;
		s.exoticChance = rules.exoticChance;
		s.introActive = com.shatteredpixel.shatteredpixeldungeon.SPDSettings.intro();
		s.externalModsPresent = false;
		s.platform = platform();
		s.realTimeMillis = System.currentTimeMillis();
		//a run from before difficulty existed (loaded from an older save) stays "not recorded"
		s.difficulty = Dungeon.difficultyKnown ? Dungeon.difficulty : 0;
		s.difficultyBalance = Dungeon.difficultyKnown ? Dungeon.difficultyBalance : 0;
		s.customRules = Dungeon.customRules() == null ? "" : Dungeon.customRules().preferenceText();
        if(r.scoringSnapshot!=null)r.scoringSnapshot.recordTo(s);
		LifecycleEvents.emitRunStart(r.writer, clock, partial, s);
		r.checkpoint(LifecycleEvents.CP_RUN_START);
		//item identity: a new run starts with its kit, a run picked up from a save is caught up with what the hero holds
		r.items = new ItemTracker(r.writer);
		r.items.poll(startKind == LifecycleEvents.START_NEW_RUN, startKind != LifecycleEvents.START_NEW_RUN);
		r.combat = new CombatTracker(r.writer, r.items);
		r.combat.baseline();
		r.generation = new GenerationTracker(r.writer, r.items);
		r.facts = new FactTracker(r.writer);
		r.play = new PlayTracker(r.facts);
		r.play.recordTime();
		return r;
	}

	//A new log for a run that continues where another log cannot be continued.
	static RunRecorder startSegment( StatsStore store, int slot, String previousLogId, int startKind ){
		String logical = previousLogId;
		int seg = 0;
		try {
			File prev = store.logFile(previousLogId);
			ReadResult r = RunLogReader.read(prev);
			for (ApdEvent e : r.events){
				if (e.type == EventTypes.RUN_START){
					try {
						LifecycleEvents.RunStart rs = LifecycleEvents.decodeRunStart(e, r);
						if (rs.runId != null && !rs.runId.isEmpty()) logical = rs.runId;
						seg = Math.max(seg, rs.segment);
						break;
					} catch (RuntimeException ignored){
						//A damaged segment header does not make a later readable one disappear.
					}
				}
			}
		} catch (Throwable ignored){
			//unreadable previous log: the new segment still starts, linked by the id
		}
		int n = seg + 1;
		String id = logical + "_" + n;
		while (store.logFile(id).exists()){
			n++;
			id = logical + "_" + n;
		}
		return startNew(store, slot, id, logical, n, startKind, false);
	}

	//---- resuming ------------------------------------------------------------------------------------------

	//Continues the log named by the loaded save. 'lastPlayed' is the save's own time stamp, used to find which SAVE
	//marker in the log the loaded game corresponds to.
	static RunRecorder resume( StatsStore store, int slot, String savedLogId, long lastPlayed ){
		File f = store.logFile(savedLogId);
		if (!f.isFile()){
			//the save names a run whose log is gone (cleared app data, restored backup): start a partial log with that id
			return startNew(store, slot, savedLogId, savedLogId, 0, LifecycleEvents.START_LOG_MISSING, true);
		}
		RunLogWriter w = RunLogWriter.openForAppend(f, TelemetryVersion.CURRENT);
		switch (w.openStatus()){
			case OK:
			case RECOVERED_TAIL:
				return resumeExisting(store, slot, savedLogId, w, lastPlayed);
			case FAILED_SEALED:
				return startSegment(store, slot, savedLogId, LifecycleEvents.START_AFTER_RUN_ENDED);
			case FAILED_VERSION_MISMATCH:
				return startSegment(store, slot, savedLogId, LifecycleEvents.START_NEW_SEGMENT);
			default: {
				//cannot use this log. Keep the id so the save stays linked, but record nothing.
				RunRecorder r = new RunRecorder(store, slot, savedLogId, savedLogId, 0);
				r.writer = w;
				return r;
			}
		}
	}

	private static RunRecorder resumeExisting( StatsStore store, int slot, String logId, RunLogWriter w, long lastPlayed ){
		RunRecorder r = new RunRecorder(store, slot, logId, logId, 0);
		r.writer = w;

		ReadResult log;
		try {
			log = RunLogReader.read(w.file());
		} catch (Throwable t){
			//cannot read it back, carry on appending without the analysis
			r.emitSessionStart(GameSnapshot.turn());
			r.items = new ItemTracker(r.writer);
			r.items.poll(false, true);
			r.combat = new CombatTracker(r.writer, r.items);
			r.combat.baseline();
			r.generation = new GenerationTracker(r.writer, r.items);
			r.facts = new FactTracker(r.writer);
			r.play = new PlayTracker(r.facts);
			return r;
		}
		RunTimeline timeline = RunTimeline.of(log.events);
		r.restoreTrackedState(log, timeline);

		//which SAVE marker does the loaded save correspond to? The save's own time stamp is the key, and the game turn
		//and depth must agree too, so two saves that happen to share a time stamp are not confused.
		ApdEvent matched = null;
		long loadedTurn = GameSnapshot.turn();
		for (ApdEvent e : log.events){
			if (e.type != EventTypes.SAVE) continue;
			try {
				LifecycleEvents.Save sv = LifecycleEvents.decodeSave(e);
				if (sv.lastPlayedMillis == lastPlayed && sv.turn == loadedTurn && sv.depth == Dungeon.depth) matched = e;
			} catch (Throwable ignored){
			}
		}

		long lastGameClock = 0;
		long lastGameIndex = -1;
		for (ApdEvent e : log.events){
			if (!LifecycleEvents.isRecordingMetaEvent(e.type) && !timeline.isAbandoned(e)){
				lastGameClock = e.clock;
				lastGameIndex = e.index;
			}
		}

		if (matched != null){
			//everything after that marker that still counts as real play is now abandoned
			long count = 0, lastAbandonedClock = 0;
			for (ApdEvent e : log.events){
				if (e.index <= matched.index) continue;
				if (LifecycleEvents.isRecordingMetaEvent(e.type) || timeline.isAbandoned(e)) continue;
				count++;
				lastAbandonedClock = e.clock;
			}
			if (count > 0){
				LifecycleEvents.Rollback rb = new LifecycleEvents.Rollback();
				rb.kind = LifecycleEvents.ROLLBACK_MATCHED;
				rb.matchedSaveIndex = matched.index;
				rb.abandonedEvents = count;
				rb.lastAbandonedClock = lastAbandonedClock;
				LifecycleEvents.emitRollback(w, GameSnapshot.turn(), rb);
			}
		} else {
			long turn = GameSnapshot.turn();
			if (turn > lastGameClock){
				//the save is further on than the log: events are missing (recorder was off or failed)
				w.event(EventTypes.GAP).clock(turn).unsigned(EventTypes.GAP_RECORDING_BEHIND_SAVE)
						.unsigned(turn - lastGameClock).emit();
			} else if (lastGameIndex >= 0){
				//the save matches no marker and the log is not behind it: we cannot tell what survived
				LifecycleEvents.Rollback rb = new LifecycleEvents.Rollback();
				rb.kind = LifecycleEvents.ROLLBACK_UNMATCHED;
				rb.matchedSaveIndex = -1;
				rb.abandonedEvents = 0;
				rb.lastAbandonedClock = lastGameClock;
				LifecycleEvents.emitRollback(w, turn, rb);
			}
		}
		r.emitSessionStart(GameSnapshot.turn());
		//what the tracker knows is what the log says up to the loaded save (a rollback leaves later play out), then the
		//game is looked at and any difference is written, marked as reconciled
		r.items = new ItemTracker(w);
		r.items.replay(log, timeline, matched != null ? matched.index : -1);
		r.items.poll(false, true);
		//combat state needs no replay: what changed while away is unknown, so "unchanged" starts from the loaded hero
		r.combat = new CombatTracker(w, r.items);
		r.combat.baseline();
		r.generation = new GenerationTracker(w, r.items);
		r.facts = new FactTracker(w);
		r.play = new PlayTracker(r.facts);
		r.play.recordTime();
		store.refreshIndex(logId);
		return r;
	}

	//logical run id, segment and ruleset bookkeeping as they were at the end of the canonical timeline
	private void restoreTrackedState( ReadResult log, RunTimeline timeline ){
		for (ApdEvent e : log.events){
			if (timeline.isAbandoned(e)) continue;
			try {
				if (e.type == EventTypes.RUN_START){
					LifecycleEvents.RunStart rs = LifecycleEvents.decodeRunStart(e, log);
					if (!rs.runId.isEmpty()) runId = rs.runId;
					segment = rs.segment;
					rulesetId = rs.rulesetId;
				} else if (e.type == EventTypes.RULESET_CHANGE){
					rulesetId = LifecycleEvents.decodeRulesetChange(e, log).newId;
					rulesetChanges++;
				}
			} catch (RuntimeException ignored){
				//One unreadable lifecycle payload must not hide later readable state.
			}
		}
	}

	//---- events during play --------------------------------------------------------------------------------

	void saved( long lastPlayed ){
		if (writer == null || writer.isDisabled() || ended) return;
		long clock = GameSnapshot.turn();
		if (combat != null) combat.poll();
		if (items != null) items.poll(false, false);
		if (play != null){ play.pollGold("other"); play.recordTime(); }
		if (facts != null) facts.flushAll();
		//the checkpoint first, the SAVE marker last: the marker is the final event of a save
		checkRuleset(clock);
		checkpoint(LifecycleEvents.CP_SAVE);
		LifecycleEvents.Save s = new LifecycleEvents.Save();
		s.slot = Math.max(0, slot);
		s.depth = Dungeon.depth;
		s.branch = Dungeon.branch;
		s.lastPlayedMillis = lastPlayed;
		s.turn = clock;
		LifecycleEvents.emitSave(writer, clock, s);
		writer.flush(FLUSH_WAIT_MILLIS, false);
	}

	//the rules in force now against the rules the run has been recorded under: a difference is written down, never ignored. Called when a level
	//is entered, when the game is saved and when the run ends, so a setting changed in the middle of a level is seen before the run is sealed.
	private void checkRuleset( long clock ){
		Ruleset rules = Ruleset.current();
		if (!rules.id.equals(rulesetId)){
			LifecycleEvents.RulesetChange c = new LifecycleEvents.RulesetChange();
			c.oldId = rulesetId;
			c.newId = rules.id;
			c.newText = rules.text;
			c.depth = Dungeon.depth;
			LifecycleEvents.emitRulesetChange(writer, clock, c);
			rulesetId = rules.id;
			rulesetChanges++;
		}
	}

	void levelSwitched(){
		if (writer == null || writer.isDisabled() || ended) return;
		long clock = GameSnapshot.turn();
		if (combat != null) combat.poll();
		if (items != null) items.poll(false, false);
		if (play != null){ play.pollGold("other"); play.recordTime(); }
		if (facts != null) facts.flushAll();

		if (levelOpen){
			LifecycleEvents.LevelExit x = new LifecycleEvents.LevelExit();
			x.depth = curDepth;
			x.branch = curBranch;
			x.turnsSpent = clock - curEnterClock;
			x.reason = LifecycleEvents.EXIT_LEAVING;
			LifecycleEvents.emitLevelExit(writer, clock, x);
			//taken when the next level is entered, so it already includes anything that happened on the way
			checkpoint(LifecycleEvents.CP_LEVEL_EXIT);
		}

		checkRuleset(clock);

		if (generation != null) generation.levelEntered(Dungeon.level);

		LifecycleEvents.LevelEnter e = new LifecycleEvents.LevelEnter();
		e.depth = Dungeon.depth;
		e.branch = Dungeon.branch;
		e.entryMode = InterlevelScene.mode == null ? "NONE" : InterlevelScene.mode.name();
		e.levelClass = Dungeon.level == null ? "" : Dungeon.level.getClass().getSimpleName();
		e.feeling = Dungeon.level == null || Dungeon.level.feeling == null ? "" : Dungeon.level.feeling.name();
		LifecycleEvents.emitLevelEnter(writer, clock, e);
		checkpoint(LifecycleEvents.CP_LEVEL_ENTER);

		levelOpen = true;
		curDepth = Dungeon.depth;
		curBranch = Dungeon.branch;
		curEnterClock = clock;
	}

	//the run is over: seals the log
	void end( int outcome, Object cause ){
		if (ended) return;
		ended = true;
		if (writer == null || writer.isDisabled()){
			return;
		}
		long clock = GameSnapshot.turn();
		if (combat != null) combat.poll();
		if (items != null) items.poll(false, false);
		if (play != null){ play.pollGold("other"); play.recordTime(); }
		if (facts != null) facts.flushAll();
		if (levelOpen){
			LifecycleEvents.LevelExit x = new LifecycleEvents.LevelExit();
			x.depth = curDepth;
			x.branch = curBranch;
			x.turnsSpent = clock - curEnterClock;
			x.reason = LifecycleEvents.EXIT_RUN_ENDED;
			LifecycleEvents.emitLevelExit(writer, clock, x);
			levelOpen = false;
		}
		checkRuleset(clock);
		checkpoint(LifecycleEvents.CP_RUN_END);

		LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
		e.outcome = outcome;
		e.ascendedKnown = true;
		e.ascended = outcome == LifecycleEvents.OUTCOME_WON && Statistics.ascended;
		if (cause != null){
			e.cause = GameSnapshot.shortName(cause instanceof Class ? (Class<?>) cause : cause.getClass());
		}
		e.depth = Dungeon.depth;
		e.deepest = Statistics.deepestFloor;
		e.heroLevel = Dungeon.hero == null ? 0 : Dungeon.hero.lvl;
		e.score = Statistics.totalScore;
        Long adjusted=scoringSnapshot==null?null:scoringSnapshot.adjustedIdentityScore(e.score);
        e.customAdjustedKnown=adjusted!=null;if(adjusted!=null)e.customAdjustedScore=adjusted;
		e.turns = clock;
		e.rulesetChanges = rulesetChanges;
		LifecycleEvents.emitRunEnd(writer, clock, e);
		emitRankingLink(clock, outcome);
		writer.close(true, CLOSE_WAIT_MILLIS);
		store.refreshIndex(logId);
	}

	//SPD has just written this run's rankings record (Dungeon.fail / Dungeon.win call Rankings.submit first): remember its id
	//so the run and the old-style record are known to be the same run. Reads only; a record that does not match this run
	//(score or outcome) is not linked.
	private void emitRankingLink( long clock, int outcome ){
		try {
			Rankings rankings = Rankings.INSTANCE;
			Rankings.Record rec = null;
			if (Dungeon.daily){
				rec = Dungeon.dailyReplay ? rankings.latestDailyReplay : rankings.latestDaily;
			} else if (rankings.records != null && rankings.lastRecord >= 0 && rankings.lastRecord < rankings.records.size()){
				rec = rankings.records.get(rankings.lastRecord);
			}
			if (rec == null || rec.gameID == null || rec.gameID.isEmpty()) return;
			if (rec.score != Statistics.totalScore || rec.win != (outcome == LifecycleEvents.OUTCOME_WON)) return;
			LegacyEvents.RankingLink link = new LegacyEvents.RankingLink();
			link.gameId = rec.gameID;
			link.daily = Dungeon.daily;
			link.dailyReplay = Dungeon.dailyReplay;
			LegacyEvents.emitRankingLink(writer, clock, link);
		} catch (Throwable ignored){
			//the link is a convenience, a run is still linked later by its score and class
		}
	}

	//the app is moving on to another run or shutting down: leave the log resumable
	void pause(){
		if (writer == null || ended) return;
		writer.close(false, CLOSE_WAIT_MILLIS);
	}

	//---- helpers -------------------------------------------------------------------------------------------

	private void checkpoint( int reason ){
		LifecycleEvents.emitCheckpoint(writer, GameSnapshot.turn(), GameSnapshot.checkpoint(reason));
	}

	private void emitSessionStart( long clock ){
		LifecycleEvents.SessionStart s = new LifecycleEvents.SessionStart();
		s.apdVersionCode = Game.versionCode;
		s.apdVersionName = Game.version == null ? "" : Game.version;
		s.platform = platform();
		s.realTimeMillis = System.currentTimeMillis();
		LifecycleEvents.emitSessionStart(writer, clock, s);
	}

	static String platform(){
		if (DeviceCompat.isAndroid()) return "android";
		if (DeviceCompat.isiOS()) return "ios";
		if (DeviceCompat.isDesktop()) return "desktop";
		return "unknown";
	}
}


