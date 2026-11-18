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

package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.SelfTest;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//The device self-test for export and import: a small made-up history (one run not finished, with a hidden secret) is exported to a
//real file, imported into a second empty history, imported again, and a damaged copy of the archive is refused. It works in a folder of
//its own, removes it afterwards, and never touches the real history.
public final class ArchiveSelfTest {

	private ArchiveSelfTest(){}

	private static final String SECRET = "items.SecretClassOfThisSelfTest";

	private static void delete( File f ){
		File[] kids = f.listFiles();
		if (kids != null) for (File k : kids) delete(k);
		f.delete();
	}

	private static byte[] read( File f ) throws IOException {
		return ArchiveExporter.readAll(f);
	}

	public static void run( File workDir, SelfTest.Report r ){
		File base = new File(workDir, "archive-selftest-" + System.nanoTime());
		try {
			File a = new File(base, "a"), b = new File(base, "b");
			int runs = 12;
			for (int i = 0; i < runs; i++){
				boolean open = i == runs - 1;
				File f = new File(new File(a, "runs"), "run" + i + ".apdlog");
				RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, "run" + i, "selftest", 1000 + i);
				LifecycleEvents.RunStart s = new LifecycleEvents.RunStart();
				s.runId = "run-" + i; s.heroClass = "WARRIOR"; s.rulesetId = "r1-selftest"; s.seed = 4242 + i; s.customSeedText = open ? SECRET : "";
				LifecycleEvents.emitRunStart(w, 0, false, s);
				LifecycleEvents.LevelEnter l = new LifecycleEvents.LevelEnter();
				l.depth = 1;
				LifecycleEvents.emitLevelEnter(w, 1, l);
				for (int k = 0; k < 10; k++){
					ItemEvents.Registered g = new ItemEvents.Registered();
					g.uid = "u" + k; g.trueClass = open ? SECRET : "items.Plain" + k; g.trueLevel = k % 5;
					ItemEvents.emitRegistered(w, 1, 0, g);
					ItemEvents.Seen seen = new ItemEvents.Seen();
					seen.uid = "u" + k; seen.state.name = "thing " + k; seen.state.category = ItemEvents.CAT_POTION;
					ItemEvents.emitSeen(w, 2 + k, 0, seen);
				}
				if (!open){
					LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
					e.outcome = LifecycleEvents.OUTCOME_DIED; e.deepest = 1; e.heroLevel = 2; e.score = 10; e.turns = 50;
					LifecycleEvents.emitRunEnd(w, 50, e);
				}
				w.close(!open);
			}

			File zip = new File(base, "history.zip");
			long t0 = System.nanoTime();
			OutputStream out = new FileOutputStream(zip);
			ArchiveExporter.Report exported;
			try {
				exported = ArchiveExporter.export(a, out, "selftest", 1);
			} finally {
				out.close();
			}
			long t1 = System.nanoTime();
			InputStream in = new FileInputStream(zip);
			ArchiveImporter.Report imported;
			try {
				imported = ArchiveImporter.importArchive(b, in);
			} finally {
				in.close();
			}
			long t2 = System.nanoTime();
			in = new FileInputStream(zip);
			ArchiveImporter.Report again;
			try {
				again = ArchiveImporter.importArchive(b, in);
			} finally {
				in.close();
			}

			boolean finishedSame = true;
			for (int i = 0; i < runs - 1; i++){
				byte[] x = read(new File(a, "runs/run" + i + ".apdlog")), y = read(new File(b, "runs/run" + i + ".apdlog"));
				finishedSame &= java.util.Arrays.equals(x, y);
			}
			byte[] openCopy = read(new File(b, "runs/run" + (runs - 1) + ".apdlog"));
			boolean secretGone = indexOf(openCopy, SECRET) < 0 && indexOf(read(zip), SECRET) < 0;
			boolean secretWasThere = indexOf(read(new File(a, "runs/run" + (runs - 1) + ".apdlog")), SECRET) >= 0;

			//a damaged copy (one byte of the last entry changed) must not be taken
			File damaged = new File(base, "damaged.zip");
			byte[] z = read(zip);
			z[z.length / 2] ^= 0x5A;
			OutputStream dout = new FileOutputStream(damaged);
			try { dout.write(z); } finally { dout.close(); }
			File c = new File(base, "c");
			in = new FileInputStream(damaged);
			ArchiveImporter.Report bad;
			try {
				bad = ArchiveImporter.importArchive(c, in);
			} finally {
				in.close();
			}
			boolean damagedRefused = bad.imported < runs || !bad.ok;
			File[] landed = new File(c, "runs").listFiles();
			boolean nothingHalfWritten = landed == null || landed.length == bad.imported;

			Catalog ca = new DatasetStore(a).load(), cb = new DatasetStore(b).load();
			boolean same = ca.dataset("runs").rows.size() == cb.dataset("runs").rows.size() && ca.dataset("items").rows.size() >= cb.dataset("items").rows.size();

			boolean ok = exported.ok && imported.ok && imported.imported == runs && imported.rejected == 0 && imported.redactedImported == 1
					&& again.imported == 0 && again.skippedAlreadyHere == runs && finishedSame && secretWasThere && secretGone
					&& damagedRefused && nothingHalfWritten && same && !new File(b, "import-tmp").exists();
			if (ok) r.pass("history archive: export, import, import again, damaged archive refused, hidden information of an open run left out");
			else r.fail("history archive gave the wrong answer: " + exported.summary() + " | " + imported.summary() + " | again " + again.imported
					+ "/" + again.skippedAlreadyHere + " | same " + finishedSame + " secret " + secretWasThere + "/" + secretGone + " damaged "
					+ damagedRefused + " clean " + nothingHalfWritten + " datasets " + same);
			r.info("archive: " + Size.text(zip.length()) + ", export " + (t1 - t0) / 1000000 + " ms, import " + (t2 - t1) / 1000000 + " ms");
		} catch (Throwable t){
			r.fail("archive self-test threw " + t);
		} finally {
			delete(base);
		}
	}

	private static int indexOf( byte[] data, String text ){
		byte[] t;
		try {
			t = text.getBytes("UTF-8");
		} catch (java.io.UnsupportedEncodingException e){
			return -1;
		}
		outer:
		for (int i = 0; i + t.length <= data.length; i++){
			for (int j = 0; j < t.length; j++) if (data[i + j] != t[j]) continue outer;
			return i;
		}
		return -1;
	}
}
