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

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.DerivedStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactTable;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.File;
import java.util.List;

//The one door between the game and APD run recording.
//
//RULES
// - Every hook is safe to call from gameplay code: nothing escapes. A problem inside the recorder is counted and
//   remembered (status().lastError). After MAX_ERRORS the recorder switches itself off for the rest of the app session,
//   gameplay carries on untouched.
// - The recorder only READS the game. It never changes game state and never calls SPD's random number generator.
// - Until configure() is called (the game does that at startup, tests do it explicitly) every hook does nothing, so
//   code that runs the game without the app, like the dungeon generation tests, behaves exactly as before.
// - Hooks run on the game thread and are cheap: they encode events and put them on the writer's queue. Disk work
//   happens on the writer thread, with short bounded waits only at save and run end.
public final class ApdRecorder {

	public static final int MAX_ERRORS = 5;

	private static StatsStore store;
	private static File root;
	private static boolean configured;
	private static boolean enabled;
	private static boolean sessionDisabled;
	private static int errorCount;
	private static String lastError = "";
	private static RunRecorder active;

	//test hook: makes the next recorder action fail, to prove failures are contained
	static volatile RuntimeException failureInjection;

	private ApdRecorder(){}

	//---- setup ---------------------------------------------------------------------------------------------

	//Sets where history is kept and whether recording is on. Calling it again resets the recorder (used by tests to
	//simulate an app restart).
	public static synchronized void configure( File historyRoot, boolean enable ){
		shutdownQuietly();
		//synthetic demo data is never real history: nothing is ever recorded into it
		if (com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData.isSyntheticRoot(historyRoot)){
			historyRoot = null;
			enable = false;
		}
		root = historyRoot;
		store = historyRoot == null ? null : new StatsStore(historyRoot);
		configured = historyRoot != null;
		enabled = enable;
		sessionDisabled = false;
		errorCount = 0;
		lastError = "";
		active = null;
		failureInjection = null;
		refreshItemFlag();
	}

	//the app's normal location: an "apd" folder in the game's data folder
	public static synchronized void configureDefault( boolean enable ){
		try {
			configure(FileUtils.getFileHandle("apd").file(), enable);
		} catch (Throwable t){
			configure(null, false);
			lastError = "setup: " + t;
		}
	}

	//stops recording and forgets everything, leaving any open run log resumable
	public static synchronized void reset(){
		configure(null, false);
	}

	private static void shutdownQuietly(){
		try {
			if (active != null) active.pause();
		} catch (Throwable ignored){
		}
		active = null;
		refreshItemFlag();
	}

	private static boolean usable(){
		return configured && enabled && !sessionDisabled && store != null;
	}

	public static synchronized boolean isRecording(){
		return usable();
	}

	//A floor replay generates levels and builds a hero kit through game code that is hooked for recording. While it runs, every
	//hook is off, so nothing it does can reach an open run log; resumeHooks puts the flags back exactly as they were.
	public static synchronized boolean[] suspendHooks(){
		boolean[] was = { ApdGen.active, ApdItems.active, ApdCombat.active, ApdFacts.active };
		ApdGen.active = false;
		ApdItems.active = false;
		ApdCombat.active = false;
		ApdFacts.active = false;
		return was;
	}

	public static synchronized void resumeHooks( boolean[] was ){
		if (was == null || was.length != 4) return;
		ApdGen.active = was[0];
		ApdItems.active = was[1];
		ApdCombat.active = was[2];
		ApdFacts.active = was[3];
	}

	//---- what the game save stores -----------------------------------------------------------------------------

	//The id to write into the game save as apd_run_id, null if no run is being tracked. Never throws.
	public static synchronized String currentRunId(){
		try {
			return active == null ? null : active.logId;
		} catch (Throwable t){
			return null;
		}
	}

	//---- hooks ---------------------------------------------------------------------------------------------

	//a new run was just set up (Dungeon.init)
	public static synchronized void onNewRun(){
		if (!usable()) return;
		try {
			inject();
			closeActive();
			String id = RunRecorder.newId();
			active = RunRecorder.startNew(store, GamesInProgress.curSlot, id, id, 0, LifecycleEvents.START_NEW_RUN, false);
		} catch (Throwable t){
			failed(t);
		}
		refreshItemFlag();
	}

	//a saved game was loaded (Dungeon.loadGame). savedRunId is the apd_run_id stored in that save, null for older saves.
	public static synchronized void onGameLoaded( int slot, String savedRunId, long lastPlayed ){
		if (!usable()) return;
		try {
			inject();
			closeActive();
			if (savedRunId == null || !StatsStore.isValidRunId(savedRunId)){
				//a run that began before APD recorded runs: record from here on, marked partial
				String id = RunRecorder.newId();
				active = RunRecorder.startNew(store, slot, id, id, 0, LifecycleEvents.START_ADOPTED_RUN, true);
			} else {
				active = RunRecorder.resume(store, slot, savedRunId, lastPlayed);
			}
		} catch (Throwable t){
			failed(t);
		}
		refreshItemFlag();
	}

	//the game is about to be written to a save slot: items get their identity first so it is part of the save
	public static synchronized void onBeforeGameSave(){
		if (!usable() || active == null) return;
		try {
			inject();
			ItemTracker t = items();
			if (t != null) t.poll(false, false);
		} catch (Throwable t){
			failed(t);
		}
	}

	//the game was just written to a save slot (Dungeon.saveGame)
	public static synchronized void onGameSaved( long lastPlayed ){
		if (!usable() || active == null) return;
		try {
			inject();
			active.saved(lastPlayed);
		} catch (Throwable t){
			failed(t);
		}
	}

	//the hero arrived on a level (Dungeon.switchLevel)
	public static synchronized void onLevelSwitched(){
		if (!usable() || active == null) return;
		try {
			inject();
			active.levelSwitched();
		} catch (Throwable t){
			failed(t);
		}
	}

	//the run ended in death or victory (Dungeon.fail / Dungeon.win)
	public static synchronized void onRunEnded( boolean won, Object cause ){
		if (!usable()) return;
		if (active == null){
			autoImportRankings();   //a run APD was not following still produced a ranking record
			return;
		}
		try {
			inject();
			active.end(won ? LifecycleEvents.OUTCOME_WON : LifecycleEvents.OUTCOME_DIED, cause);
			active = null;
		} catch (Throwable t){
			failed(t);
			active = null;
		}
		refreshItemFlag();
		autoImportRankings();
	}

	//Rankings.submit is about to add this run's record and trim SPD's table to 11 records: whatever the file holds now is read
	//first, so a record that is about to be dropped has already been copied (Dungeon.fail / Dungeon.win)
	public static void onBeforeRankingSubmit(){
		autoImportRankings();
	}

	private static Thread importThread;

	//Copies SPD's ranking records into the history without ever getting in the way of the game. The file is read here (a few
	//small reads, read-only) and the history is written on a background thread. Duplicates are impossible (records are
	//recognised by their id) and any problem is dropped silently: this is not allowed to count against the recorder or
	//touch the run being finished.
	private static void autoImportRankings(){
		try {
			final File base;
			synchronized (ApdRecorder.class){
				if (!usable()) return;
				base = root;
			}
			if (base == null) return;
			final long now = System.currentTimeMillis();
			final LegacyImporter.Extract x = LegacyImporter.extractRankingsFile(now);
			if (x == null || !x.fileFound) return;
			Thread t = new Thread(new Runnable(){
				@Override
				public void run(){
					try {
						LegacyImporter.write(base, x, now);
					} catch (Throwable ignored){
						//never reported against the recorder
					}
				}
			}, "apd-legacy-import");
			t.setDaemon(true);
			synchronized (ApdRecorder.class){
				importThread = t;
			}
			t.start();
		} catch (Throwable ignored){
			//importing is optional, finishing the run is not
		}
	}

	//tests: wait for the automatic import to finish writing
	static void awaitAutoImportForTests(){
		Thread t;
		synchronized (ApdRecorder.class){
			t = importThread;
		}
		try {
			if (t != null) t.join(15000);
		} catch (InterruptedException ignored){
		}
	}

	//a save slot is about to be deleted (Dungeon.deleteGame): its run, if still open, ends as abandoned
	public static synchronized void onSlotDeleted( int slot ){
		if (!usable()) return;
		try {
			inject();
			if (active != null && !active.ended && active.slot() == slot){
				active.end(LifecycleEvents.OUTCOME_ABANDONED, null);
				active = null;
				refreshItemFlag();
				return;
			}
			String id = readSavedRunId(slot);
			if (id != null) abandonStoredRun(id);
		} catch (Throwable t){
			failed(t);
		}
	}

	//---- item identity (see ApdItems) ----------------------------------------------------------------------------

	private static ItemTracker items(){
		return usable() && active != null && !active.ended ? active.items : null;
	}

	private static void refreshItemFlag(){
		ApdItems.active = usable() && active != null && !active.ended && active.items != null;
		ApdCombat.active = usable() && active != null && !active.ended && active.combat != null;
		ApdGen.active = usable() && active != null && !active.ended && active.generation != null;
		ApdFacts.active = usable() && active != null && !active.ended && active.facts != null;
		ApdFacts.play = ApdFacts.active ? active.play : null;
	}

	private static GenerationTracker gen(){
		return usable() && active != null && !active.ended ? active.generation : null;
	}

	static synchronized int genBegin(){
		try {
			GenerationTracker g = gen();
			return g == null ? -1 : g.begin();
		} catch (Throwable t){
			failed(t);
			return -1;
		}
	}

	static synchronized void genEnd( int depth, int kind, com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category requested, Item result ){
		try {
			GenerationTracker g = gen();
			if (g != null) g.end(depth, kind, requested, result);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void genLevelStart(){
		try {
			inject();
			GenerationTracker g = gen();
			if (g != null) g.levelStart();
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void genBuildAttempt(){
		try {
			GenerationTracker g = gen();
			if (g != null) g.buildAttempt();
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void genStage( int stage ){
		try {
			GenerationTracker g = gen();
			if (g != null) g.setStage(stage);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void genLevelGenerated( com.shatteredpixel.shatteredpixeldungeon.levels.Level level ){
		try {
			inject();
			GenerationTracker g = gen();
			if (g != null) g.levelGenerated(level);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void genLoot( com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob, Item item ){
		try {
			inject();
			GenerationTracker g = gen();
			if (g != null) g.loot(mob, item);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void genBonus( Item item, int pos ){
		try {
			inject();
			GenerationTracker g = gen();
			if (g != null) g.bonus(item, pos);
		} catch (Throwable t){
			failed(t);
		}
	}

	private static CombatTracker combat(){
		return usable() && active != null && !active.ended ? active.combat : null;
	}

	static synchronized void combatAttackRolled( com.shatteredpixel.shatteredpixeldungeon.actors.Char a, com.shatteredpixel.shatteredpixeldungeon.actors.Char d, boolean hit ){
		try {
			inject();
			CombatTracker t = combat();
			if (t != null) t.onAttackRolled(a, d, hit);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void combatAttackDetail( com.shatteredpixel.shatteredpixeldungeon.actors.Char a, com.shatteredpixel.shatteredpixeldungeon.actors.Char d, int rolled, int dr, int effective ){
		try {
			inject();
			CombatTracker t = combat();
			if (t != null) t.onAttackDetail(a, d, rolled, dr, effective);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void combatMagicRoll( com.shatteredpixel.shatteredpixeldungeon.actors.Char a, com.shatteredpixel.shatteredpixeldungeon.actors.Char d, boolean hit ){
		try {
			inject();
			CombatTracker t = combat();
			if (t != null) t.onMagicRoll(a, d, hit);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void combatDamage( com.shatteredpixel.shatteredpixeldungeon.actors.Char target, Object src, int requested, int hpBefore, int shieldBefore, int absorbed, int hpLost ){
		try {
			inject();
			CombatTracker t = combat();
			if (t != null) t.onDamage(target, src, requested, hpBefore, shieldBefore, absorbed, hpLost);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void combatHeal( com.shatteredpixel.shatteredpixeldungeon.actors.Char target, int hpBefore, String source, int requested ){
		try {
			inject();
			CombatTracker t = combat();
			if (t != null) t.onHeal(target, hpBefore, source, requested);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void combatCharDied( com.shatteredpixel.shatteredpixeldungeon.actors.Char ch, Object src ){
		try {
			inject();
			CombatTracker t = combat();
			if (t != null) t.onCharDied(ch, src);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void combatHeroDying( Object cause ){
		try {
			inject();
			CombatTracker t = combat();
			if (t != null) t.onHeroDying(cause);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void combatHeroRevived( int kind ){
		try {
			inject();
			CombatTracker t = combat();
			if (t != null) t.onHeroRevived(kind);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void combatXp( com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero, int amount, Class<?> source ){
		try {
			inject();
			CombatTracker t = combat();
			if (t != null) t.onXp(hero, amount, source);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void combatHunger( float energy, int before, int after ){
		try {
			inject();
			CombatTracker t = combat();
			if (t != null) t.onHungerChanged(energy, before, after);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void factRecord( com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind kind, String subject, String actor, long[] values, String[] tags ){
		try {
			RunRecorder r = active;
			if (r == null || r.ended || r.facts == null || !usable()) return;
			r.facts.record(kind, GameSnapshot.turn(), subject, actor, values, tags);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void itemNoteAction( Item item, String action ){
		try {
			ItemTracker t = items();
			if (t != null) t.noteAction(item, action);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void itemNoteAcquire( int context ){
		try {
			ItemTracker t = items();
			if (t != null) t.noteAcquireContext(context);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void itemSplit( Item parent, Item child, int amount ){
		try {
			inject();
			ItemTracker t = items();
			if (t != null) t.onSplit(parent, child, amount);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void itemMerge( Item survivor, Item absorbed, int amount ){
		try {
			inject();
			ItemTracker t = items();
			if (t != null) t.onMerge(survivor, absorbed, amount);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void itemDetached( Item original, Item piece ){
		try {
			inject();
			ItemTracker t = items();
			if (t != null) t.onDetached(original, piece);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void itemTransformed( Item oldItem, Item newItem, String cause ){
		try {
			inject();
			ItemTracker t = items();
			if (t != null) t.onTransformed(oldItem, newItem, cause);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void itemCrafted( List<Item> ingredients, Item result ){
		try {
			inject();
			ItemTracker t = items();
			if (t != null) t.onCrafted(ingredients, result);
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void itemClassKnown( Class<?> cls, String labelBefore, Item sample ){
		try {
			inject();
			ItemTracker t = items();
			if (t != null) t.onClassKnown(cls, labelBefore, sample == null ? ItemEvents.CAT_OTHER : ItemFacts.category(sample));
		} catch (Throwable t){
			failed(t);
		}
	}

	static synchronized void itemHeroTurn(){
		try {
			inject();
			CombatTracker c = combat();
			if (c != null) c.poll();
			ItemTracker t = items();
			if (t != null) t.poll(false, false);
		} catch (Throwable t){
			failed(t);
		}
	}

	//---- internals -----------------------------------------------------------------------------------------

	private static void closeActive(){
		if (active != null){
			active.pause();
			active = null;
		}
		refreshItemFlag();
	}

	private static String readSavedRunId( int slot ){
		try {
			Bundle b = FileUtils.bundleFromFile(GamesInProgress.gameFile(slot));
			String id = b.contains("apd_run_id") ? b.getString("apd_run_id") : null;
			return id != null && StatsStore.isValidRunId(id) ? id : null;
		} catch (Throwable t){
			return null;
		}
	}

	//ends a run that is not the one being played, from its log alone
	private static void abandonStoredRun( String id ){
		File f = store.logFile(id);
		if (!f.isFile()) return;
		RunLogWriter w = RunLogWriter.openForAppend(f, TelemetryVersion.CURRENT);
		if (w.isDisabled()) return; //sealed, another telemetry version or unreadable: leave it alone
		try {
			ReadResult r = RunLogReader.read(f);
			LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
			e.outcome = LifecycleEvents.OUTCOME_ABANDONED;
			//nothing else is known here, the flag says so instead of pretending
			LifecycleEvents.emitRunEnd(w, r.lastClock, e, LifecycleEvents.FLAG_DETAILS_UNKNOWN);
		} catch (Throwable t){
			failed(t);
		}
		w.close(true, 2000);
		store.refreshIndex(id);
	}

	private static void inject(){
		RuntimeException r = failureInjection;
		if (r != null) throw r;
	}

	private static void failed( Throwable t ){
		errorCount++;
		lastError = String.valueOf(t);
		if (errorCount >= MAX_ERRORS){
			sessionDisabled = true;
			shutdownKeepId();
		}
		refreshItemFlag();
	}

	//stop writing but remember the run id, so game saves stay linked to the log
	private static void shutdownKeepId(){
		try {
			if (active != null && active.writer != null) active.writer.close(false, 500);
		} catch (Throwable ignored){
		}
	}

	//tests: write out what is queued so the log file can be read while the run goes on
	static synchronized void flushForTests(){
		if (active != null && active.writer != null) active.writer.flush(5000, false);
	}

	//---- diagnostics ---------------------------------------------------------------------------------------

	public static synchronized RecorderStatus status(){
		RecorderStatus s = new RecorderStatus();
		try {
			s.configured = configured;
			s.enabled = enabled;
			s.sessionDisabled = sessionDisabled;
			s.errorCount = errorCount;
			s.lastError = lastError;
			s.telemetryVersion = TelemetryVersion.CURRENT.toString();
			if (active != null){
				s.logId = active.logId;
				if (active.writer != null){
					s.logPath = active.writer.file().getPath();
					s.writerDisabled = active.writer.isDisabled();
					s.eventsAccepted = active.writer.eventsAccepted();
					s.eventsDropped = active.writer.eventsDropped();
					if (active.writer.failure() != null) s.writerProblem = String.valueOf(active.writer.failure());
				}
			}
			if (store != null){
				File[] logs = store.runsDir().listFiles();
				if (logs != null){
					for (File f : logs){
						if (f.isFile()){
							s.storageBytes += f.length();
							if (f.getName().endsWith(".apdlog")) s.runLogs++;
						}
					}
				}
			}
		} catch (Throwable t){
			s.lastError = String.valueOf(t);
		}
		return s;
	}

	//UIMSD is about to read the logs: write out what is queued so the run in progress shows up (a short wait at most). Never throws.
	public static synchronized void flushForReading(){
		try {
			if (active != null && active.facts != null) active.facts.flushAll();
			if (active != null && active.writer != null) active.writer.flush(300, false);
		} catch (Throwable ignored){
		}
	}

	//Copies the records of APD's OWN rankings.dat (the file APD's Rankings class writes in APD's private storage) into the history as
	//raw import events (see LegacyImporter) and says what happened. It cannot see another app's files, official SPD's included.
	//Not synchronized with the recorder: it never blocks recording. Never throws.
	public static String importApdRankings(){
		try {
			File base = historyRoot();
			if (base == null) return "History is not set up, nothing imported.";
			return LegacyImporter.importRankingsFile(base, System.currentTimeMillis()).summary();
		} catch (Throwable t){
			return "Import failed: " + t;
		}
	}

	//A few lines about what the history holds, from the derived fact table (rebuilt from the raw files when needed).
	public static String historyOverview(){
		try {
			File base = historyRoot();
			if (base == null) return "History is not set up.";
			DerivedStore derived = new DerivedStore(base);
			FactTable t = derived.load();
			java.util.List<RunFacts> rows = t.rows();
			int linked = 0;
			for (RunFacts leg : t.legacyRows) if (leg.isKnown(FactKeys.LINKED_RUN)) linked++;
			StringBuilder b = new StringBuilder();
			b.append(t.apdRows.size()).append(" recorded runs, ").append(t.legacyRows.size()).append(" imported APD ranking records");
			if (linked > 0) b.append(" (").append(linked).append(" are also recorded runs)");
			b.append(". ").append(rows.size()).append(" distinct runs in total.\n");
			if (!rows.isEmpty()){
				b.append(FactTable.coverage(rows, FactKeys.OUTCOME)).append(".\n");
				b.append(FactTable.coverage(rows, FactKeys.HERO_ATTACKS)).append(".\n");
				b.append(FactTable.coverage(rows, FactKeys.GEN_ITEMS)).append(".\n");
			}
			if (t.legacyTotals != null && t.legacyTotals.isKnown("legacy.totalNumber")){
				b.append("APD's rankings counted ").append(t.legacyTotals.numOr("legacy.totalNumber", 0)).append(" games");
				if (t.legacyTotals.isKnown("legacy.wonNumber")) b.append(", ").append(t.legacyTotals.numOr("legacy.wonNumber", 0)).append(" won");
				b.append(" (the rankings file only keeps the best 11 records).\n");
			}
			b.append("Facts ready in ").append(derived.lastMillis()).append(" ms (").append(derived.lastSource()).append(").");
			return b.toString();
		} catch (Throwable t){
			return "Could not read the history: " + t;
		}
	}

	//where history is kept, null if not set up
	public static synchronized File historyRoot(){
		return root;
	}
}
