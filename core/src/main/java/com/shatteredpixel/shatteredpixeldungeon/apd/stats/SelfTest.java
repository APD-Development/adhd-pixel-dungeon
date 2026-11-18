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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//A check of the storage library that can run on any device, from a unit test or from inside the game.
//
//It exists so the library is validated on the real runtime and file system it will use (Android, ART, the phone's
//storage) and not only on a desktop JVM. It writes only to its own temporary folder and deletes it afterwards.
//It uses no randomness and no game classes. It returns a report, it never throws.
public final class SelfTest {

	private SelfTest(){}

	public static final class Report {
		public boolean passed = true;
		public final List<String> lines = new ArrayList<>();

		public void pass( String what ){
			lines.add("PASS  " + what);
		}

		public void fail( String what ){
			passed = false;
			lines.add("FAIL  " + what);
		}

		public void info( String what ){
			lines.add("      " + what);
		}

		public String text(){
			StringBuilder sb = new StringBuilder();
			sb.append(passed ? "SELF-TEST PASSED" : "SELF-TEST FAILED").append('\n');
			for (String l : lines) sb.append(l).append('\n');
			return sb.toString();
		}
	}

	//'workDir' is an existing, writable folder. The test makes (and removes) a sub-folder in it.
	public static Report run( File workDir ){
		return run(workDir, 5000);
	}

	public static Report run( File workDir, int eventCount ){
		Report r = new Report();
		File dir = new File(workDir, "selftest-" + System.nanoTime());
		try {
			environment(r, workDir);
			if (!dir.mkdirs()){
				r.fail("cannot create " + dir);
				return r;
			}
			writeAndReadBack(r, dir, eventCount);
			lifecycleRoundTrip(r, dir);
			itemEventsRoundTrip(r, dir);
			combatEventsRoundTrip(r, dir);
			generationEventsRoundTrip(r, dir);
			legacyImportRoundTrip(r, dir);
			damage(r, dir, eventCount);
			concurrency(r, dir);
			store(r, dir);
		} catch (Throwable t){
			r.fail("unexpected " + t);
		} finally {
			deleteTree(dir);
		}
		return r;
	}

	private static void environment( Report r, File workDir ){
		r.info("telemetry " + TelemetryVersion.CURRENT + ", container format " + LogFormat.CONTAINER_FORMAT);
		r.info("vm " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
		r.info("os " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
		r.info("cpus " + Runtime.getRuntime().availableProcessors() + ", free space " + (workDir.getUsableSpace() / (1024 * 1024)) + " MB");
	}

	private static void writeAndReadBack( Report r, File dir, int n ){
		File f = new File(dir, "rw.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "selftest", "selftest", 1,
				new RunLogWriter.Options().queueCapacity(1 << 20));
		if (w.isDisabled()){
			r.fail("could not create a log: " + w.openStatus() + " " + w.failure());
			return;
		}
		long t0 = System.nanoTime();
		int accepted = 0;
		for (int i = 0; i < n; i++){
			if (w.event(5000).clock(i).unsigned(i % 40).string("name-" + (i % 30)).signed(i % 17 - 8).emit()) accepted++;
		}
		long t1 = System.nanoTime();
		boolean flushed = w.flush(20_000, true);
		long t2 = System.nanoTime();
		boolean closed = w.close();
		long t3 = System.nanoTime();
		if (accepted != n) r.fail("only " + accepted + " of " + n + " events accepted"); else r.pass(n + " events accepted");
		if (!flushed || !closed) r.fail("flush/close failed: " + w.failure());
		r.info("caller cost " + ((t1 - t0) / Math.max(1, n)) + " ns/event, flush+sync " + ((t2 - t1) / 1_000_000) + " ms, close " + ((t3 - t2) / 1_000_000) + " ms");

		try {
			long t4 = System.nanoTime();
			ReadResult rd = RunLogReader.read(f);
			long t5 = System.nanoTime();
			boolean ok = rd.status == ReadStatus.SEALED && rd.events.size() == n && rd.strings.size() == 30;
			for (int i = 0; ok && i < n; i++){
				ApdEvent e = rd.events.get(i);
				PayloadReader p = e.reader(rd);
				ok = e.clock == i && p.unsigned() == i % 40 && ("name-" + (i % 30)).equals(p.string()) && p.signed() == i % 17 - 8;
			}
			if (ok) r.pass("log read back exactly (" + f.length() + " bytes, " + (f.length() / Math.max(1, n)) + " B/event)");
			else r.fail("log did not read back correctly: " + rd.status + " " + rd.problem);
			r.info("read " + ((t5 - t4) / 1_000_000) + " ms");
		} catch (Throwable t){
			r.fail("read back threw " + t);
		}
	}

	private static void lifecycleRoundTrip( Report r, File dir ){
		File f = new File(dir, "lc.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "selftestlc", "selftest", 1);
		LifecycleEvents.RunStart rs = new LifecycleEvents.RunStart();
		rs.runId = "selftestlc"; rs.seed = -42; rs.heroClass = "WARRIOR"; rs.rulesetId = "r1-test"; rs.platform = "selftest";
		LifecycleEvents.emitRunStart(w, 0, true, rs);
		LifecycleEvents.Save sv = new LifecycleEvents.Save();
		sv.slot = 1; sv.depth = 3; sv.lastPlayedMillis = 99; sv.turn = 7;
		LifecycleEvents.emitSave(w, 7, sv);
		LifecycleEvents.Rollback rb = new LifecycleEvents.Rollback();
		rb.kind = LifecycleEvents.ROLLBACK_MATCHED; rb.matchedSaveIndex = 1; rb.abandonedEvents = 0;
		LifecycleEvents.emitRollback(w, 8, rb);
		w.close();
		try {
			ReadResult rd = RunLogReader.read(f);
			boolean ok = rd.status == ReadStatus.SEALED && rd.events.size() == 3
					&& LifecycleEvents.decodeRunStart(rd.events.get(0), rd).seed == -42
					&& LifecycleEvents.isPartial(rd.events.get(0))
					&& LifecycleEvents.decodeSave(rd.events.get(1)).lastPlayedMillis == 99
					&& LifecycleEvents.decodeRollback(rd.events.get(2)).matchedSaveIndex == 1;
			if (ok) r.pass("lifecycle events round trip"); else r.fail("lifecycle events did not round trip");
		} catch (Throwable t){
			r.fail("lifecycle round trip threw " + t);
		}
	}

	//item identity events (telemetry 1.2): what is written for the player stays free of hidden facts, history is derived
	private static void itemEventsRoundTrip( Report r, File dir ){
		File f = new File(dir, "items.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "selftestitems", "selftest", 1);
		ItemEvents.Where where = new ItemEvents.Where();
		where.depth = 4;
		where.pos = 77;
		ItemEvents.Registered reg = new ItemEvents.Registered();
		reg.uid = "aaaa"; reg.where = where; reg.trueClass = "items.rings.RingOfWealth"; reg.trueLevel = 3; reg.quantity = 1;
		ItemEvents.emitRegistered(w, 1, 0, reg);
		ItemEvents.Seen seen = new ItemEvents.Seen();
		seen.uid = "aaaa"; seen.where = where;
		seen.state.category = ItemEvents.CAT_RING; seen.state.name = "ruby ring";
		seen.state.levelKnown = false; seen.state.visibleLevel = 3; //must not be stored
		ItemEvents.emitSeen(w, 2, 0, seen);
		ItemEvents.Knowledge k = new ItemEvents.Knowledge();
		k.uid = "aaaa"; k.where = where;
		k.state.category = ItemEvents.CAT_RING; k.state.name = "Ring of Wealth"; k.state.levelKnown = true; k.state.cursedKnown = true;
		k.state.visibleLevel = 3; k.state.identified = true;
		ItemEvents.emitKnowledge(w, 9, 0, k);
		w.close();
		try {
			ReadResult rd = RunLogReader.read(f);
			ItemEvents.Seen s = ItemEvents.decodeSeen(rd.events.get(1), rd);
			ItemEvents.VisibleState then = ItemHistory.visibleStateAt(rd.events, rd, "aaaa", rd.events.get(1).index);
			ItemEvents.VisibleState now = ItemHistory.visibleStateAt(rd.events, rd, "aaaa", Long.MAX_VALUE);
			boolean ok = rd.status == ReadStatus.SEALED && rd.events.size() == 3 && s.state.visibleLevel == 0
					&& "ruby ring".equals(then.name) && "Ring of Wealth".equals(now.name) && now.visibleLevel == 3
					&& EventTypes.visibility(EventTypes.ITEM_REGISTERED) == EventTypes.Visibility.INTERNAL
					&& EventTypes.visibility(EventTypes.ITEM_SEEN) == EventTypes.Visibility.PUBLIC;
			if (ok) r.pass("item events round trip, seen state keeps what the player knew then"); else r.fail("item events did not round trip");
		} catch (Throwable t){
			r.fail("item events round trip threw " + t);
		}
	}

	//combat and health events (telemetry 1.3): a burst of fight events, timed on this device, read back exactly
	private static void combatEventsRoundTrip( Report r, File dir ){
		File f = new File(dir, "combat.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "selftestcombat", "selftest", 1,
				new RunLogWriter.Options().queueCapacity(1 << 20));
		int rounds = 1500;
		CombatEvents.Where where = new CombatEvents.Where();
		where.depth = 3;
		long t0 = System.nanoTime();
		for (int i = 0; i < rounds; i++){
			CombatEvents.Attack a = new CombatEvents.Attack();
			a.where = where; a.attacker.name = "you"; a.defender.name = "rat"; a.defender.kind = CombatEvents.KIND_ENEMY; a.hit = true; a.weaponName = "sword";
			CombatEvents.emitAttack(w, i, a);
			CombatEvents.AttackDetail ad = new CombatEvents.AttackDetail();
			ad.where = where; ad.attackerClass = "Hero"; ad.defenderClass = "actors.mobs.Rat"; ad.hit = true; ad.rolled = 6; ad.effective = 5; ad.publicEmitted = true;
			CombatEvents.emitAttackDetail(w, i, ad);
			CombatEvents.Damage d = new CombatEvents.Damage();
			d.where = where; d.target.name = "rat"; d.shown = 5;
			CombatEvents.emitDamage(w, i, d);
			CombatEvents.DamageDetail dd = new CombatEvents.DamageDetail();
			dd.where = where; dd.targetClass = "actors.mobs.Rat"; dd.hpBefore = 8; dd.hpAfter = 3; dd.hpMax = 8; dd.requested = 5; dd.hpLost = 5; dd.publicEmitted = true;
			CombatEvents.emitDamageDetail(w, i, dd);
		}
		long t1 = System.nanoTime();
		w.close();
		try {
			ReadResult rd = RunLogReader.read(f);
			boolean ok = rd.status == ReadStatus.SEALED && rd.events.size() == rounds * 4
					&& CombatEvents.decodeDamageDetail(rd.events.get(3), rd).hpBefore == 8
					&& CombatEvents.decodeAttack(rd.events.get(0), rd).defender.name.equals("rat")
					&& EventTypes.visibility(EventTypes.DAMAGE_DETAIL) == EventTypes.Visibility.INTERNAL;
			if (ok) r.pass("combat events round trip (" + (f.length() / rounds) + " bytes per attack with its damage)"); else r.fail("combat events did not round trip");
			r.info("combat events: " + ((t1 - t0) / (rounds * 4L)) + " ns/event caller cost");
		} catch (Throwable t){
			r.fail("combat events round trip threw " + t);
		}
	}

	//generation provenance events (telemetry 1.4): one level's worth of generated items, timed on this device
	private static void generationEventsRoundTrip( Report r, File dir ){
		File f = new File(dir, "generation.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "selftestgen", "selftest", 1,
				new RunLogWriter.Options().queueCapacity(1 << 20));
		int items = 600;
		GenerationEvents.Where where = new GenerationEvents.Where();
		where.depth = 4;
		long t0 = System.nanoTime();
		for (int i = 0; i < items; i++){
			GenerationEvents.ItemGenerated g = new GenerationEvents.ItemGenerated();
			g.uid = "uid" + i; g.where = where; g.container = GenerationEvents.C_HEAP; g.pos = i; g.stage = GenerationEvents.STAGE_ITEMS;
			g.drawKind = GenerationEvents.DRAW_CATEGORY; g.requestedCategory = "POTION"; g.actualCategory = "POTION";
			g.trueClass = "items.potions.PotionOfHealing"; g.quantity = 1;
			GenerationEvents.emitItemGenerated(w, i, g);
		}
		long t1 = System.nanoTime();
		w.close();
		try {
			ReadResult rd = RunLogReader.read(f);
			boolean ok = rd.status == ReadStatus.SEALED && rd.events.size() == items
					&& GenerationEvents.decodeItemGenerated(rd.events.get(items - 1), rd).uid.equals("uid" + (items - 1))
					&& EventTypes.visibility(EventTypes.ITEM_GENERATED) == EventTypes.Visibility.INTERNAL;
			if (ok) r.pass("generation events round trip (" + (f.length() / items) + " bytes per generated item)"); else r.fail("generation events did not round trip");
			r.info("generation events: " + ((t1 - t0) / items) + " ns/event caller cost");
		} catch (Throwable t){
			r.fail("generation events round trip threw " + t);
		}
	}

	//legacy import events and the derived fact table (telemetry 1.5): import records, rebuild the table from the raw files,
	//delete the cache and rebuild again, and check both give the same answer and that unknown stays unknown
	private static void legacyImportRoundTrip( Report r, File dir ){
		try {
			File root = new File(dir, "derive-root");
			File legacy = new File(root, DerivedStore.LEGACY_DIR);
			File f = new File(legacy, "legacy-import-1.apdlog");
			RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "legacy-import-1", "selftest", 1);
			int records = 40;
			for (int i = 0; i < records; i++){
				LegacyEvents.Record rec = new LegacyEvents.Record();
				rec.gameId = "g" + i; rec.source = "rankings.dat";
				rec.putNum(FactKeys.SCORE, 1000 + i);
				rec.putNum(FactKeys.OUTCOME, i % 2);
				rec.putText(FactKeys.HERO_CLASS, "MAGE");
				rec.putUnknown(FactKeys.DATE);
				LegacyEvents.emitRecord(w, i, rec);
			}
			w.close(true);
			DerivedStore store = new DerivedStore(root);
			long t0 = System.nanoTime();
			FactTable built = store.rebuild();
			long t1 = System.nanoTime();
			FactTable cached = store.load();
			boolean fromCache = store.lastSource() == DerivedStore.Source.CACHE;
			store.deleteCache();
			FactTable again = store.load();
			boolean ok = built.legacyRows.size() == records && cached.legacyRows.equals(built.legacyRows)
					&& again.legacyRows.equals(built.legacyRows) && fromCache
					&& !built.legacyRows.get(0).isKnown(FactKeys.DATE)
					&& built.legacyRows.get(3).numOr(FactKeys.SCORE, -1) == 1003
					&& !built.legacyRows.get(0).isKnown(FactKeys.HERO_ATTACKS);
			if (ok) r.pass("legacy-record import and fact derivation round trip (synthetic records, not your rankings)"); else r.fail("legacy import or fact derivation did not round trip");
			r.info("fact derivation: " + ((t1 - t0) / 1000L) + " us to rebuild " + records + " synthetic test records (made and deleted by the self-test)");
			deleteTree(root);
		} catch (Throwable t){
			r.fail("legacy import round trip threw " + t);
		}
	}

	private static void damage( Report r, File dir, int n ){
		File f = new File(dir, "dmg.apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "selftestdmg", "selftest", 1);
		for (int i = 0; i < 300; i++) w.event(5001).clock(i).unsigned(i).string("s" + (i % 5)).emit();
		w.close(false);
		try {
			byte[] full = RunLogReader.readFully(f);
			ReadResult whole = RunLogReader.parse(full);
			boolean ok = true;
			//cut the file at many places: always a clean prefix, never an exception
			for (int cut = whole.header.length; cut <= full.length && ok; cut += Math.max(1, full.length / 97)){
				ReadResult c = RunLogReader.parse(Arrays.copyOf(full, cut));
				ok = (c.status == ReadStatus.UNSEALED || c.status == ReadStatus.TRUNCATED) && c.events.size() <= whole.events.size();
				for (int i = 0; ok && i < c.events.size(); i++) ok = c.events.get(i).clock == whole.events.get(i).clock;
			}
			if (ok) r.pass("a log cut at any point reads back as a clean prefix"); else r.fail("truncation handling is wrong");

			byte[] bad = full.clone();
			bad[full.length / 2] ^= 0x55;
			ReadResult cr = RunLogReader.parse(bad);
			if (cr.status == ReadStatus.CORRUPT && cr.events.size() > 0 && cr.events.size() < whole.events.size())
				r.pass("a damaged byte is detected, earlier events are kept"); else r.fail("corruption not handled: " + cr.status);

			//repair a cut copy and carry on writing
			byte[] cutBytes = Arrays.copyOf(full, full.length - 3);
			File g = new File(dir, "fix.apdlog");
			write(g, cutBytes);
			LogRecovery.RepairReport rep = LogRecovery.repair(g);
			RunLogWriter again = RunLogWriter.openForAppend(g, TelemetryVersion.CURRENT);
			again.event(5001).clock(1000).unsigned(1).string("s0").emit();
			boolean sealed = again.close();
			ReadResult after = RunLogReader.read(g);
			boolean fixed = rep.error == null && rep.truncated && rep.savedTail != null && sealed && after.status == ReadStatus.SEALED;
			if (fixed) r.pass("a cut log is repaired, the cut bytes are kept, and writing continues"); else r.fail("repair failed: " + rep.error + " " + after.status);
		} catch (Throwable t){
			r.fail("damage tests threw " + t);
		}
	}

	private static void concurrency( Report r, File dir ){
		final RunLogWriter w = RunLogWriter.create(new File(dir, "conc.apdlog"), TelemetryVersion.CURRENT, "selftestc", "selftest", 1,
				new RunLogWriter.Options().queueCapacity(1 << 20));
		final int threads = 4, per = 2000;
		List<Thread> ts = new ArrayList<>();
		for (int t = 0; t < threads; t++){
			final int id = t;
			Thread th = new Thread(new Runnable(){
				@Override
				public void run(){
					for (int i = 0; i < per; i++) w.event(5002).unsigned(id).string("t" + id).signed(i).emit();
				}
			});
			ts.add(th);
			th.start();
		}
		try {
			for (Thread th : ts) th.join();
			w.close();
			ReadResult rd = RunLogReader.read(w.file());
			int[] next = new int[threads];
			boolean ok = rd.status == ReadStatus.SEALED && rd.events.size() == threads * per;
			for (int i = 0; ok && i < rd.events.size(); i++){
				PayloadReader p = rd.events.get(i).reader(rd);
				int id = (int) p.unsigned();
				ok = ("t" + id).equals(p.string()) && p.signed() == next[id]++;
			}
			if (ok) r.pass(threads + " threads wrote " + (threads * per) + " events in order"); else r.fail("concurrent writing lost or reordered events");
		} catch (Throwable t){
			r.fail("concurrency test threw " + t);
		}
	}

	private static void store( Report r, File dir ){
		try {
			StatsStore store = new StatsStore(new File(dir, "store"));
			for (int i = 0; i < 3; i++){
				RunLogWriter w = store.startRun("selftest-run-" + i, "selftest", 100 + i);
				w.event(5000).unsigned(i).emit();
				if (i < 2) w.close(); else w.close(false);
				store.refreshIndex("selftest-run-" + i);
			}
			List<RunIndexEntry> before = store.index().all();
			File index = new File(store.root(), "index.apdi");
			boolean deleted = index.delete();
			RunIndex rebuilt = RunIndex.load(index, store.runsDir());
			boolean ok = deleted && rebuilt.wasRebuilt() && rebuilt.all().equals(before) && before.size() == 3
					&& before.get(0).status == RunIndexEntry.Status.SEALED && before.get(2).status == RunIndexEntry.Status.OPEN;
			if (ok) r.pass("run index rebuilds from the logs"); else r.fail("run index rebuild mismatch");
		} catch (Throwable t){
			r.fail("store test threw " + t);
		}
	}

	private static void write( File f, byte[] data ) throws java.io.IOException {
		java.io.FileOutputStream out = new java.io.FileOutputStream(f);
		try {
			out.write(data);
		} finally {
			out.close();
		}
	}

	private static void deleteTree( File f ){
		try {
			File[] kids = f.listFiles();
			if (kids != null) for (File k : kids) deleteTree(k);
			f.delete();
		} catch (Throwable ignored){
		}
	}
}
