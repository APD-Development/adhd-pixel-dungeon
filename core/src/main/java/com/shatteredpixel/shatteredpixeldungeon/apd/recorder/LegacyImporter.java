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

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.DerivedStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactTable;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;
import com.watabou.utils.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

//Copies what APD's OWN rankings file (rankings.dat in APD's private app storage, written by APD's copy of SPD's Rankings class)
//holds into APD's history as raw import events (legacy/import-*.apdlog). It does NOT read official Shattered Pixel Dungeon's
//data: that is a different Android app with a different sandbox, which no app can read on an unrooted phone.
//
//It only READS: the rankings bundle is navigated as plain data (no game objects are created, Rankings itself is not
//touched, rankings.dat is not changed). Every field is imported only if the old record actually contains it; a field the
//old record lacks is written as UNKNOWN, never as zero. Importing twice imports nothing the second time (records are
//recognised by SPD's game id). Imported records are raw evidence like run logs: derived facts are rebuilt from them.
//
//The rankings file keeps only the best 11 runs, so older runs are gone for good: this can only preserve what is still there.
public final class LegacyImporter {

	private LegacyImporter(){}

	public static final String SOURCE = "rankings.dat";

	public static final class Report {
		public boolean fileFound;
		public int records;         //records found in the file (latest daily included)
		public int imported;
		public int alreadyKnown;
		public int unreadable;
		public boolean totalsImported;
		public int dailyScoresImported;
		public int fieldsKnown;
		public int fieldsUnknown;
		public String importFile = "";
		public long millis;
		public String problem = "";

		public String summary(){
			if (!problem.isEmpty()) return "Import failed: " + problem;
			if (!fileFound) return "No APD rankings file yet (it appears after you finish a run in APD). Nothing to import.";
			if (imported == 0 && !totalsImported && dailyScoresImported == 0){
				return "Nothing new: " + alreadyKnown + " of " + records + " APD ranking records were already imported"
						+ (unreadable > 0 ? ", " + unreadable + " unreadable" : "") + ".";
			}
			return "Imported " + imported + " new APD ranking record" + (imported == 1 ? "" : "s") + " (" + alreadyKnown + " already known"
					+ (unreadable > 0 ? ", " + unreadable + " unreadable" : "") + "). "
					+ fieldsUnknown + " of " + (fieldsKnown + fieldsUnknown) + " fields were missing from those ranking records and stay unknown.";
		}
	}

	//Reads rankings.dat with the game's own file API and imports it. Never throws.
	public static Report importRankingsFile( File root, long nowMillis ){
		Extract x = extractRankingsFile(nowMillis);
		if (x == null) return new Report();
		return write(root, x, nowMillis);
	}

	//what was read from a rankings bundle, before anything is written
	public static final class Extract {
		public boolean fileFound;
		public int records;
		public int unreadable;
		public final List<LegacyEvents.Record> fresh = new ArrayList<>();
		public LegacyEvents.Totals totals;
		public final List<LegacyEvents.DailyScore> days = new ArrayList<>();
	}

	//only one import writes at a time (the button and the automatic import after a run can overlap), so the second one
	//sees what the first wrote and adds nothing twice
	private static final Object WRITE_LOCK = new Object();

	//Reads a rankings bundle into plain records. Cheap (no history is read) and read-only. Never throws.
	public static Extract extract( Bundle rankings, long nowMillis ){
		Extract x = new Extract();
		try {
			if (rankings == null || rankings.isNull()) return x;
			x.fileFound = true;
			List<Bundle> sources = new ArrayList<>();
			if (rankings.contains("records")){
				Bundle[] arr = rankings.getBundleArray("records");
				if (arr != null) for (Bundle b : arr) sources.add(b);
			}
			if (rankings.contains("latest_daily")) sources.add(rankings.getBundle("latest_daily"));
			x.records = sources.size();
			for (Bundle b : sources){
				try {
					x.fresh.add(readRecord(b, nowMillis));
				} catch (Throwable bad){
					x.unreadable++;
				}
			}
			x.totals = readTotals(rankings, nowMillis);
			if (rankings.contains("daily_history_dates") && rankings.contains("daily_history_scores")){
				long[] dates = rankings.getLongArray("daily_history_dates");
				int[] scores = rankings.getIntArray("daily_history_scores");
				if (dates != null && scores != null){
					for (int i = 0; i < dates.length && i < scores.length; i++){
						LegacyEvents.DailyScore d = new LegacyEvents.DailyScore();
						d.day = dates[i];
						d.score = scores[i];
						d.importedAtMillis = nowMillis;
						x.days.add(d);
					}
				}
			}
		} catch (Throwable t){
			//whatever was read so far is kept
		}
		return x;
	}

	//The same, reading rankings.dat with the game's own file API. Null when there is no file. Never throws.
	public static Extract extractRankingsFile( long nowMillis ){
		try {
			Bundle b;
			try {
				b = com.watabou.utils.FileUtils.bundleFromFile(com.shatteredpixel.shatteredpixeldungeon.Rankings.RANKINGS_FILE);
			} catch (java.io.IOException missing){
				return null;
			}
			return extract(b, nowMillis);
		} catch (Throwable t){
			return null;
		}
	}

	//Imports a rankings bundle into the history at 'root'. Never throws.
	public static Report importFrom( Bundle rankings, File root, long nowMillis ){
		return write(root, extract(rankings, nowMillis), nowMillis);
	}

	//Writes what is not in the history yet. Never throws.
	public static Report write( File root, Extract x, long nowMillis ){
		Report report = new Report();
		//synthetic demo data is never a place for real records
		if (com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData.isSyntheticRoot(root)) return report;
		long t0 = System.nanoTime();
		synchronized (WRITE_LOCK){
			try {
				report.fileFound = x.fileFound;
				report.records = x.records;
				report.unreadable = x.unreadable;
				if (!x.fileFound) return report;
				DerivedStore derived = new DerivedStore(root);
				FactTable existing = derived.buildLegacyOnly();
				Set<String> knownIds = new HashSet<>();
				for (RunFacts f : existing.legacyRows) knownIds.add(f.id.substring("legacy:".length()));
				Set<Long> knownDays = new HashSet<>();
				for (long[] d : existing.dailyScores) knownDays.add(d[0]);

				List<LegacyEvents.Record> fresh = new ArrayList<>();
				Set<String> batchIds = new HashSet<>();
				for (LegacyEvents.Record rec : x.fresh){
					if (knownIds.contains(rec.gameId) || !batchIds.add(rec.gameId)){
						report.alreadyKnown++;
						continue;
					}
					for (LegacyEvents.Value v : rec.fields.values()){
						if (v.isKnown()) report.fieldsKnown++; else report.fieldsUnknown++;
					}
					fresh.add(rec);
				}
				boolean writeTotals = x.totals != null && !sameTotals(existing.legacyTotals, x.totals);
				List<LegacyEvents.DailyScore> days = new ArrayList<>();
				for (LegacyEvents.DailyScore d : x.days) if (knownDays.add(d.day)) days.add(d);

				if (fresh.isEmpty() && !writeTotals && days.isEmpty()) return report;

				File dir = derived.legacyDir();
				File file = null;
				String id = null;
				for (int n = 0; n < 1000 && file == null; n++){
					String candidate = "legacy-import-" + nowMillis + (n == 0 ? "" : "-" + n);
					File f = new File(dir, candidate + ".apdlog");
					if (!f.exists()){
						file = f;
						id = candidate;
					}
				}
				if (file == null){
					report.problem = "could not choose an import file name";
					return report;
				}
				RunLogWriter w = RunLogWriter.create(file, TelemetryVersion.CURRENT, id, "legacy-import", nowMillis);
				if (w.isDisabled()){
					report.problem = "could not open " + file.getName() + ": " + w.failure();
					return report;
				}
				long clock = 0;
				boolean ok = true;
				for (LegacyEvents.Record rec : fresh) ok &= LegacyEvents.emitRecord(w, clock++, rec);
				if (writeTotals) ok &= LegacyEvents.emitTotals(w, clock++, x.totals);
				for (LegacyEvents.DailyScore d : days) ok &= LegacyEvents.emitDailyScore(w, clock++, d);
				ok &= w.close(true, 10000);
				if (!ok || w.isDisabled()){
					report.problem = "writing the import file failed";
					return report;
				}
				report.imported = fresh.size();
				report.totalsImported = writeTotals;
				report.dailyScoresImported = days.size();
				report.importFile = file.getName();
			} catch (Throwable t){
				report.problem = String.valueOf(t);
			} finally {
				report.millis = (System.nanoTime() - t0) / 1000000L;
			}
		}
		return report;
	}

	private static boolean sameTotals( RunFacts old, LegacyEvents.Totals t ){
		boolean any = false;
		for (java.util.Map.Entry<String, LegacyEvents.Value> e : t.fields.entrySet()){
			if (!e.getValue().isKnown()) continue;
			any = true;
			if (old == null) return false;
			Fact f = old.get(e.getKey());
			if (f == null || !f.isNum() || f.num != e.getValue().num) return false;
		}
		return !any || old != null;
	}

	private static LegacyEvents.Totals readTotals( Bundle rankings, long now ){
		LegacyEvents.Totals t = new LegacyEvents.Totals();
		t.source = SOURCE;
		t.importedAtMillis = now;
		int total = rankings.contains("total") ? rankings.getInt("total") : 0;
		int won = rankings.contains("won") ? rankings.getInt("won") : 0;
		//SPD itself cannot tell 0 from "never stored" here (it recounts from the table when total is 0), so neither can we
		if (total > 0) t.fields.put("legacy.totalNumber", LegacyEvents.Value.num(total));
		else t.fields.put("legacy.totalNumber", LegacyEvents.Value.UNKNOWN);
		if (won > 0 || (total > 0 && rankings.contains("won"))) t.fields.put("legacy.wonNumber", LegacyEvents.Value.num(won));
		else t.fields.put("legacy.wonNumber", LegacyEvents.Value.UNKNOWN);
		return (total > 0 || won > 0) ? t : null;
	}

	//---- one record -------------------------------------------------------------------------------------------

	private static final String CLASS_NAME = "__className";

	static LegacyEvents.Record readRecord( Bundle b, long now ){
		if (b == null || b.isNull()) throw new IllegalArgumentException("no record");
		LegacyEvents.Record rec = new LegacyEvents.Record();
		rec.source = SOURCE;
		rec.importedAtMillis = now;
		rec.gameId = b.contains("gameID") ? b.getString("gameID") : contentId(b);

		boolean hasWin = b.contains("win");
		boolean win = hasWin && b.getBoolean("win");
		if (hasWin) rec.putNum(FactKeys.OUTCOME, win ? LifecycleEvents.OUTCOME_WON : LifecycleEvents.OUTCOME_DIED);
		else rec.putUnknown(FactKeys.OUTCOME);
		text(rec, FactKeys.CAUSE, b, "cause", true);
		num(rec, FactKeys.SCORE, b, "score");
		num(rec, FactKeys.HERO_LEVEL, b, "level");
		text(rec, FactKeys.HERO_CLASS, b, "class", false);
		num(rec, "legacy.armorTier", b, "tier");
		bool(rec, FactKeys.ASCENDING, b, "ascending");
		bool(rec, FactKeys.DAILY, b, "daily");
		text(rec, FactKeys.CUSTOM_SEED, b, "custom_seed", false);
		text(rec, FactKeys.DATE, b, "date", false);
		text(rec, FactKeys.GAME_VERSION, b, "version", false);
		//the rankings depth is the deepest floor, or the highest ascent for a run that went back up
		num(rec, "legacy.rankDepth", b, "depth");

		Bundle data = b.contains("gameData") ? b.getBundle("gameData") : null;
		boolean hasData = data != null && !data.isNull();
		num(rec, FactKeys.INITIAL_VERSION, hasData ? data : null, "game_version");
		num(rec, FactKeys.SEED, hasData ? data : null, "seed");
		num(rec, FactKeys.CHALLENGES, hasData ? data : null, "challenges");

		Bundle stats = hasData && data.contains("stats") ? data.getBundle("stats") : null;
		boolean hasStats = stats != null && !stats.isNull();
		Bundle st = hasStats ? stats : null;
		num(rec, "legacy.goldCollected", st, "score");
		num(rec, "legacy.deepestFloor", st, "maxDepth");
		num(rec, "legacy.highestAscent", st, "maxAscent");
		num(rec, "legacy.enemiesSlain", st, "enemiesSlain");
		num(rec, "legacy.foodEaten", st, "foodEaten");
		num(rec, "legacy.itemsCrafted", st, "potionsCooked");
		num(rec, "legacy.piranhasKilled", st, "priranhas");
		num(rec, "legacy.hazardAssistedKills", st, "hazard_assists");
		num(rec, "legacy.ankhsUsed", st, "ankhsUsed");
		num(rec, "legacy.upgradesUsed", st, "upgradesUsed");
		num(rec, "legacy.sneakAttacks", st, "sneakAttacks");
		num(rec, "legacy.thrownAssists", st, "thrownAssists");
		num(rec, "legacy.progressScore", st, "prog_score");
		num(rec, "legacy.heldItemValue", st, "item_val");
		num(rec, "legacy.treasureScore", st, "tres_score");
		num(rec, "legacy.exploreScore", st, "expl_score");
		num(rec, "legacy.bossScore", st, "tot_boss");
		num(rec, "legacy.questScore", st, "tot_quest");
		num(rec, "legacy.totalScore", st, "total_score");
		scaled(rec, "legacy.winMultiplierPct", st, "win_mult", 100);
		scaled(rec, "legacy.chalMultiplierPct", st, "chal_mult", 100);
		scaled(rec, "legacy.durationMilli", st, "duration", 1000);
		bool(rec, "legacy.amuletObtained", st, "amuletObtained");
		bool(rec, "legacy.gameWon", st, "won");
		bool(rec, "legacy.ascended", st, "ascended");
		bool(rec, "run.ascended", st, "ascended");
		if (st != null && st.contains("item_types_discovered")){
			String[] names = st.getStringArray("item_types_discovered");
			if (names != null) rec.putNum("legacy.itemTypesDiscovered", names.length);
			else rec.putUnknown("legacy.itemTypesDiscovered");
		} else {
			rec.putUnknown("legacy.itemTypesDiscovered");
		}
		//deepest floor: the statistics when present, else the rankings depth if the run never went back up
		if (st != null && st.contains("maxDepth")) rec.putNum(FactKeys.DEEPEST, st.getInt("maxDepth"));
		else if (b.contains("depth") && b.contains("ascending") && !b.getBoolean("ascending")) rec.putNum(FactKeys.DEEPEST, b.getInt("depth"));
		else rec.putUnknown(FactKeys.DEEPEST);

		Bundle hero = hasData && data.contains("hero") ? data.getBundle("hero") : null;
		boolean hasHero = hero != null && !hero.isNull();
		Bundle h = hasHero ? hero : null;
		text(rec, FactKeys.FINAL_SUBCLASS, h, "subClass", false);
		num(rec, FactKeys.FINAL_STRENGTH, h, "STR");
		num(rec, FactKeys.FINAL_HP_MAX, h, "HT");
		num(rec, FactKeys.FINAL_HERO_LEVEL, h, "lvl");
		num(rec, "legacy.experience", h, "exp");
		String[] slots = { "weapon", "armor", "artifact", "misc", "ring", "second_wep" };
		for (String slot : slots) item(rec, FactKeys.LOADOUT_PREFIX + slot, h, slot);
		//what the old record kept in the pack: trinkets, an escape crystal and quickslotted items
		if (h != null && h.contains("inventory")){
			Bundle[] pack = h.getBundleArray("inventory");
			if (pack != null){
				rec.putNum("loadout.packItems", pack.length);
				for (int i = 0; i < pack.length && i < 20; i++){
					item(rec, FactKeys.LOADOUT_PREFIX + "pack." + i, pack[i]);
				}
			}
		} else {
			rec.putUnknown("loadout.packItems");
		}
		return rec;
	}

	private static void num( LegacyEvents.Record rec, String key, Bundle b, String bundleKey ){
		if (b != null && b.contains(bundleKey)) rec.putNum(key, b.getLong(bundleKey));
		else rec.putUnknown(key);
	}

	private static void scaled( LegacyEvents.Record rec, String key, Bundle b, String bundleKey, int factor ){
		if (b != null && b.contains(bundleKey)) rec.putNum(key, Math.round(b.getFloat(bundleKey) * factor));
		else rec.putUnknown(key);
	}

	private static void bool( LegacyEvents.Record rec, String key, Bundle b, String bundleKey ){
		if (b != null && b.contains(bundleKey)) rec.putNum(key, b.getBoolean(bundleKey) ? 1 : 0);
		else rec.putUnknown(key);
	}

	//a class name is stored as its short name: "Rat" for ...actors.mobs.Rat
	private static void text( LegacyEvents.Record rec, String key, Bundle b, String bundleKey, boolean className ){
		if (b != null && b.contains(bundleKey)){
			String s = b.getString(bundleKey);
			rec.putText(key, className ? shortName(s) : s);
		} else {
			rec.putUnknown(key);
		}
	}

	static String shortName( String s ){
		if (s == null) return "";
		String t = s.replace("class ", "");
		int dot = Math.max(t.lastIndexOf('.'), t.lastIndexOf('$'));
		return dot >= 0 ? t.substring(dot + 1) : t;
	}

	private static void item( LegacyEvents.Record rec, String prefix, Bundle parent, String slot ){
		Bundle it = parent != null && parent.contains(slot) ? parent.getBundle(slot) : null;
		item(rec, prefix, it);
	}

	//An equipped (or kept) item as the old record stored it. The stored level and curse state are the item's true state;
	//the old game showed them identified on its rankings screen, so they are what the player saw there.
	private static void item( LegacyEvents.Record rec, String prefix, Bundle it ){
		if (it == null || it.isNull() || !it.contains(CLASS_NAME)){
			rec.putUnknown(prefix);
			return;
		}
		rec.putText(prefix, shortName(it.getString(CLASS_NAME)));
		num(rec, prefix + ".level", it, "level");
		bool(rec, prefix + ".cursed", it, "cursed");
		if (it.contains("quantity")) rec.putNum(prefix + ".quantity", it.getInt("quantity"));
		Bundle ench = it.contains("enchantment") ? it.getBundle("enchantment") : it.contains("glyph") ? it.getBundle("glyph") : null;
		if (ench != null && !ench.isNull() && ench.contains(CLASS_NAME)) rec.putText(prefix + ".enchant", shortName(ench.getString(CLASS_NAME)));
	}

	//records saved before SPD gave them an id get a stable id from their content instead (SPD makes a random one on every load)
	private static String contentId( Bundle b ){
		StringBuilder s = new StringBuilder();
		String[] keys = { "class", "date", "version", "score", "depth", "level", "win", "tier", "custom_seed", "cause" };
		for (String k : keys) s.append(b.contains(k) ? b.getString(k) : "-").append('|');
		CRC32 crc = new CRC32();
		byte[] bytes;
		try {
			bytes = s.toString().getBytes("UTF-8");
		} catch (java.io.UnsupportedEncodingException e){
			bytes = s.toString().getBytes();
		}
		crc.update(bytes, 0, bytes.length);
		return "content-" + Long.toHexString(crc.getValue());
	}
}

