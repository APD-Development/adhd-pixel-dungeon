package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveExporter;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveImporter;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveManifest;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveSelfTest;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellFormatter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Dataset;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Query;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Scope;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdSelfTest;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.HistoryTransfer;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.SelfTest;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

//Phase 8 end to end, on a real Android runtime and file system: a realistic history is exported through the real export path
//(HistoryTransfer, then the same stream-opening code the system file picker's answer goes through, AndroidFileProvider.deliverExport/
//deliverImport, with the file chosen by the test instead of by a person), checked, imported into a clean install, rebuilt, compared
//with the source, imported again, attacked with damaged archives, cancelled, and followed by the APD self-test.
//The only thing it cannot do is show and tap the system file picker's own screens.
@RunWith(AndroidJUnit4.class)
public class ArchiveMigrationDeviceTest {

	private static final String SECRET_CLASS = "items.rings.RingOfSecretHaste";
	private static final String SECRET_ROOM = "Treasury of Secrets";
	private static final String SECRET_SEED = "TOP-SECRET-SEED";
	//what healed an unfinished run: hidden information (an unidentified enchantment or potion would give itself away)
	private static final String SECRET_HEAL = "gear.vampiric";
	private static final String RS = "r1-aaaaaaaaaaaa";
	private static final TelemetryVersion V = TelemetryVersion.CURRENT;
	//finished runs in the source history
	private static final int FINISHED = 25;

	//a test that stops answering must fail with a message, never hold the whole job
	@org.junit.Rule
	public org.junit.rules.Timeout timeout = new org.junit.rules.Timeout(240, TimeUnit.SECONDS);

	private Context ctx;
	private File base, source, clean, other, exports;
	private ContentResolver resolver;

	//what the "picker" answers with next
	private File chosen;
	private boolean cancel;


	//---- diagnostics: every step is written to logcat; a watchdog dumps every thread if a test overruns ----
	private static final String TAG = "APDMIG";
	private static final long T0 = System.currentTimeMillis();
	private volatile boolean testDone;

	private static void step( String what ){
		Log.w(TAG, "[" + (System.currentTimeMillis() - T0) + " ms] " + what);
	}

	private void startWatchdog(){
		testDone = false;
		Thread w = new Thread(new Runnable() {
			@Override
			public void run(){
				try { Thread.sleep(150000); } catch (InterruptedException e){ return; }
				if (testDone) return;
				StringBuilder b = new StringBuilder("WATCHDOG: the test has run for 150 s. All threads:");
				for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()){
					b.append("\n== ").append(e.getKey().getName()).append(" (").append(e.getKey().getState()).append(")");
					StackTraceElement[] st = e.getValue();
					for (int i = 0; i < st.length && i < 14; i++) b.append("\n     at ").append(st[i]);
				}
				Log.e(TAG, b.toString());
			}
		}, "apdmig-watchdog");
		w.setDaemon(true);
		w.start();
	}

	@Before
	public void setUp() throws Exception {
		if (com.watabou.noosa.Game.version == null) com.watabou.noosa.Game.version = "4.0.8-apd-test";
		ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
		resolver = ctx.getContentResolver();
		base = new File(ctx.getFilesDir(), "apd-migration-" + System.nanoTime());
		source = new File(base, "source/apd");
		clean = new File(base, "clean/apd");
		other = new File(base, "other/apd");
		exports = new File(base, "exports");
		assertTrue(exports.mkdirs());
		startWatchdog();
		step("setUp: building the source history");
		buildHistory(source);
		step("setUp: history built");
		ApdFiles.provider = new ApdFiles.Provider() {
			@Override
			public void chooseExport( String suggestedName, ApdFiles.ExportTarget target ){
				step("provider.chooseExport cancel=" + cancel + " chosen=" + chosen);
				Intent answer = new Intent();
				if (chosen != null) answer.setData(Uri.fromFile(chosen));
				AndroidFileProvider.deliverExport(resolver, target, cancel ? Activity.RESULT_CANCELED : Activity.RESULT_OK, answer);
			}

			@Override
			public void chooseImport( ApdFiles.ImportSource src ){
				step("provider.chooseImport cancel=" + cancel + " chosen=" + chosen);
				Intent answer = new Intent();
				if (chosen != null) answer.setData(Uri.fromFile(chosen));
				AndroidFileProvider.deliverImport(resolver, src, cancel ? Activity.RESULT_CANCELED : Activity.RESULT_OK, answer);
			}

			@Override
			public String where(){
				return "test";
			}
		};
	}

	@After
	public void tearDown(){
		testDone = true;
		step("tearDown");
		ApdFiles.provider = null;
		ApdRecorder.reset();
		delete(base);
	}

	private static void delete( File f ){
		File[] kids = f.listFiles();
		if (kids != null) for (File k : kids) delete(k);
		f.delete();
	}

	// ---- a realistic history ----

	private static void buildHistory( File root ) throws Exception {
		//finished runs: items with hidden truth, fights, ends
		for (int i = 0; i < FINISHED; i++){
			DeviceLogs l = new DeviceLogs(root, "fin" + i, V, 1000L + i).seeds(1000 + i, "seed-" + i)
					.start("run-fin-" + i, 0, i % 2 == 0 ? "WARRIOR" : "MAGE", RS, i % 3, 30, false).enter(1);
			for (int k = 0; k < 6; k++){
				String uid = "f" + i + "-" + k;
				l.registered(uid, k % 2 == 0 ? "items.weapon.melee.Sword" : "items.rings.RingOfHaste", k, k == 3, ItemEvents.ORIGIN_GENERATED)
						.generated(uid, GenerationEvents.C_HEAP, "Room " + k)
						.seen(uid, k % 2 == 0 ? "sword" : "ruby ring", k % 2 == 0 ? ItemEvents.CAT_WEAPON : ItemEvents.CAT_RING)
						.acquired(uid, k % 2 == 0 ? "sword" : "ruby ring", k % 2 == 0 ? ItemEvents.CAT_WEAPON : ItemEvents.CAT_RING, false, 0);
				if (k == 0) l.equip(uid, "sword", ItemEvents.CAT_WEAPON).tick(20);
			}
			l.heal(3, true, "regen").heal(5, i % 2 == 0, i % 2 == 0 ? "food.pasty" : "").heal(2, true, SECRET_HEAL);
			l.mobGenerated("actors.mobs.Gnoll").heroAttacks("rat", "actors.mobs.Rat", true).damageFromHero("rat", "actors.mobs.Rat", 3)
					.heroKills("rat", "actors.mobs.Rat", 1).tick(30).enter(2);
			l.end(i % 4 == 0 ? LifecycleEvents.OUTCOME_WON : LifecycleEvents.OUTCOME_DIED, "rat", 1 + i % 9, 2 + i % 7, 100 + i).close(true);
		}
		//runs that are not finished, with hidden information that must never leave
		for (int i = 0; i < 3; i++){
			DeviceLogs o = new DeviceLogs(root, "open" + i, V, 5000L + i).seeds(88800 + i, SECRET_SEED + i)
					.start("run-open-" + i, 0, "ROGUE", RS, 0, 30, false).enter(1);
			o.registered("o-ring", SECRET_CLASS, 3, true, ItemEvents.ORIGIN_GENERATED).generated("o-ring", GenerationEvents.C_CHEST, SECRET_ROOM)
					.seen("o-ring", "ruby ring", ItemEvents.CAT_RING).acquired("o-ring", "ruby ring", ItemEvents.CAT_RING, false, 0)
					.registered("o-secret", "items.armor.PlateArmor", 9, false, ItemEvents.ORIGIN_GENERATED).mobGenerated("actors.mobs.Goo")
					.heal(4, true, SECRET_HEAL).heal(2, true, "potion.healing")
					.heroAttacks("rat", "actors.mobs.Rat", true).tick(20).enter(2).close(false);
		}
		//a run recorded in two segments that ended in the second
		DeviceLogs a = new DeviceLogs(root, "two-a", V, 7000).start("run-two", 0, "HUNTRESS", RS, 0, 30, false).enter(1);
		a.registered("t-dagger", "items.weapon.melee.Dagger", 0, false, ItemEvents.ORIGIN_GENERATED).seen("t-dagger", "dagger", ItemEvents.CAT_WEAPON).close(false);
		DeviceLogs b = new DeviceLogs(root, "two-b", V, 7100).start("run-two", 1, "HUNTRESS", RS, 0, 30, false).enter(2);
		b.heroAttacks("snake", "actors.mobs.Snake", true).end(LifecycleEvents.OUTCOME_WON, "", 26, 25, 9000).close(true);
		//older telemetry
		new DeviceLogs(root, "old", new TelemetryVersion(1, 1), 500).start("run-old", 0, "DUELIST", RS, 0, 0, false).enter(1)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 5).close(true);
		//imported rankings records
		RunLogWriter w = RunLogWriter.create(new File(new File(root, "legacy"), "import-1.apdlog"), V, "import-1", "t", 9000);
		LegacyEvents.Totals t = new LegacyEvents.Totals();
		t.source = "rankings.dat";
		t.importedAtMillis = 9000;
		t.fields.put("legacy.totalNumber", LegacyEvents.Value.num(41));
		LegacyEvents.emitTotals(w, 1, t);
		w.close(true);
	}

	// ---- tools ----

	private static byte[] read( File f ) throws Exception {
		return ArchiveExporterRead.read(f);
	}

	//the tests' own small copy of "read a whole file" so they do not depend on library internals
	private static final class ArchiveExporterRead {
		static byte[] read( File f ) throws Exception {
			InputStream in = new FileInputStream(f);
			try {
				ByteArrayOutputStream o = new ByteArrayOutputStream();
				byte[] buf = new byte[16384];
				int n;
				while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
				return o.toByteArray();
			} finally {
				in.close();
			}
		}
	}

	private static Map<String, byte[]> files( File root ) throws Exception {
		Map<String, byte[]> out = new TreeMap<>();
		walk(root, root, out);
		return out;
	}

	private static void walk( File base, File f, Map<String, byte[]> out ) throws Exception {
		File[] kids = f.listFiles();
		if (kids == null) return;
		for (File k : kids){
			if (k.isDirectory()) walk(base, k, out);
			else out.put(base.toURI().relativize(k.toURI()).getPath(), read(k));
		}
	}

	private static Map<String, byte[]> logsOnly( Map<String, byte[]> all ){
		Map<String, byte[]> out = new TreeMap<>();
		for (Map.Entry<String, byte[]> e : all.entrySet()) if (e.getKey().startsWith("runs/") || e.getKey().startsWith("legacy/")) out.put(e.getKey(), e.getValue());
		return out;
	}

	private static boolean contains( byte[] data, String text ) throws Exception {
		byte[] t = text.getBytes("UTF-8");
		outer:
		for (int i = 0; i + t.length <= data.length; i++){
			for (int j = 0; j < t.length; j++) if (data[i + j] != t[j]) continue outer;
			return true;
		}
		return false;
	}

	private static Map<String, byte[]> entries( byte[] zip ) throws Exception {
		Map<String, byte[]> out = new TreeMap<>();
		ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip));
		ZipEntry e;
		while ((e = z.getNextEntry()) != null){
			ByteArrayOutputStream o = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int n;
			while ((n = z.read(buf)) > 0) o.write(buf, 0, n);
			out.put(e.getName(), o.toByteArray());
		}
		return out;
	}

	private interface Action {
		void run( HistoryTransfer.Listener l );
	}

	private static String threads(){
		StringBuilder b = new StringBuilder();
		for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()){
			String n = e.getKey().getName();
			if (!n.contains("apd") && !n.equals("TestRunner") && !n.startsWith("Instr")) continue;
			b.append("\n").append(n).append(':');
			StackTraceElement[] st = e.getValue();
			for (int i = 0; i < st.length && i < 8; i++) b.append("\n   ").append(st[i]);
		}
		return b.toString();
	}

	private static String await( Action a ) throws Exception {
		step("await: start");
		final String[] out = new String[1];
		final CountDownLatch done = new CountDownLatch(1);
		a.run(new HistoryTransfer.Listener() {
			@Override
			public void finished( String message ){
				step("listener.finished: " + String.valueOf(message).replace("\n", " / "));
				out[0] = message;
				done.countDown();
			}
		});
		if (!done.await(60, TimeUnit.SECONDS)) throw new AssertionError("no answer from the transfer; threads: " + threads());
		return out[0];
	}

	private String exportNow( File target ) throws Exception {
		chosen = target;
		cancel = false;
		return await(new Action() {
			public void run( HistoryTransfer.Listener l ){
				HistoryTransfer.export(l);
			}
		});
	}

	private String importNow( File archive ) throws Exception {
		chosen = archive;
		cancel = false;
		return await(new Action() {
			public void run( HistoryTransfer.Listener l ){
				HistoryTransfer.importHistory(l);
			}
		});
	}

	//everything a player can read in UIMSD: every public row of every dataset, every public column
	private static String publicDump( File root ){
		Catalog c = new DatasetStore(root).load();
		StringBuilder b = new StringBuilder();
		for (Dataset ds : c.datasets()){
			Query q = Query.of(ds.def.id).withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
			List<String> lines = new ArrayList<>();
			for (Row r : ds.rows){
				if (!r.publicRow) continue;
				StringBuilder l = new StringBuilder(r.id).append(r.finished ? " F " : " U ");
				for (ColumnDef col : ds.def.columns()){
					if (col.isInternal()) continue;
					l.append(CellFormatter.cell(q, r, col).text).append('|');
				}
				lines.add(l.toString());
			}
			Collections.sort(lines);
			b.append(ds.def.id).append(':').append(lines.size()).append('\n');
			for (String l : lines) b.append(l).append('\n');
		}
		return b.toString();
	}

	private static String sha( byte[] d ) throws Exception {
		return ArchiveManifest.hex(MessageDigest.getInstance("SHA-256").digest(d));
	}

	private static File put( File folder, String name, byte[] bytes ) throws Exception {
		folder.mkdirs();
		File f = new File(folder, name);
		FileOutputStream o = new FileOutputStream(f);
		try { o.write(bytes); } finally { o.close(); }
		return f;
	}

	// ---- the migration ----

	@Test
	public void aRealisticHistoryMigratesToACleanInstallLosslesslyAndSafely() throws Exception {
		//a demo history exists next to the real one, and a demo-named log has even strayed into the real runs folder
		File demoRoot = DemoData.demoRootFor(source);
		step("migration: generating demo data");
		DemoGenerator.generate(demoRoot, Ruleset.of(30, 0), System.currentTimeMillis());
		step("migration: demo data generated");
		put(new File(source, "runs"), "demo-stray.apdlog", read(new File(source, "runs/fin0.apdlog")));
		Map<String, byte[]> sourceBefore = files(source);

		step("migration: export");
		// 2. export through the real path
		ApdRecorder.configure(source, false);
		File zip = new File(exports, "history.zip");
		String exported = exportNow(zip);
		assertTrue(exported, exported.startsWith("Exported 31 run logs and 1 imported-record file"));
		assertTrue(exported, exported.contains("3 logs belong to runs that are not finished"));

		step("migration: zip checks");
		// 3. the produced ZIP and manifest
		byte[] archive = read(zip);
		Map<String, byte[]> parts = entries(archive);
		ZipInputStream first = new ZipInputStream(new ByteArrayInputStream(archive));
		assertEquals(ArchiveManifest.ENTRY_NAME, first.getNextEntry().getName());
		ArchiveManifest manifest = ArchiveManifest.parse(parts.get(ArchiveManifest.ENTRY_NAME));
		assertEquals(32, manifest.files.size());
		int redacted = 0;
		for (ArchiveManifest.Entry e : manifest.files){
			assertTrue(e.path, ArchiveManifest.isValidPath(e.path));
			assertNotNull(e.path, parts.get(e.path));
			assertEquals(e.path, e.size, parts.get(e.path).length);
			assertEquals(e.path, e.sha256, sha(parts.get(e.path)));
			if (e.redacted) redacted++;
			assertFalse("no demo data in the archive: " + e.path, e.path.contains("demo-"));
		}
		assertEquals(3, redacted);
		for (String name : parts.keySet()) assertTrue(name, name.equals(ArchiveManifest.ENTRY_NAME) || name.startsWith("runs/") || name.startsWith("legacy/"));
		//the source history was not touched by exporting
		Map<String, byte[]> sourceAfter = files(source);
		for (String k : sourceBefore.keySet()) if (!k.startsWith("derived/") && !k.equals("index.apdi")) assertTrue(k, Arrays.equals(sourceBefore.get(k), sourceAfter.get(k)));

		step("migration: redaction checks");
		// 8. unfinished runs: redacted in the archive, hidden information nowhere
		for (int i = 0; i < 3; i++){
			byte[] open = parts.get("runs/open" + i + ".apdlog");
			assertNotNull(open);
			for (String hidden : new String[]{ SECRET_CLASS, SECRET_ROOM, SECRET_SEED + i, "actors.mobs.Goo", "items.armor.PlateArmor", SECRET_HEAL, "potion.healing" }){
				assertFalse(hidden, contains(open, hidden));
			}
			assertTrue("the raw log does hold it", contains(read(new File(source, "runs/open" + i + ".apdlog")), SECRET_CLASS));
			assertTrue("and the source of its healing", contains(read(new File(source, "runs/open" + i + ".apdlog")), SECRET_HEAL));
		}

		step("migration: import");
		// 4. import into a clean install
		ApdRecorder.configure(clean, false);
		String imported = importNow(zip);
		assertTrue(imported, imported.startsWith("Imported 32 logs"));
		assertTrue(imported, imported.contains("3 belong to runs that were not finished"));

		step("migration: lossless checks");
		// 9. finished runs, the two-segment run, the old run and the imported records are byte for byte the same files
		Map<String, byte[]> cleanFiles = logsOnly(files(clean));
		assertEquals(32, cleanFiles.size());
		for (Map.Entry<String, byte[]> e : cleanFiles.entrySet()){
			if (e.getKey().startsWith("runs/open")){
				assertFalse(e.getKey() + " is a redacted copy", Arrays.equals(e.getValue(), sourceBefore.get(e.getKey())));
				ReadResult r = RunLogReader.parse(e.getValue());
				assertTrue(r.header.writerBuild.startsWith("REDACTED:"));
				assertFalse(contains(e.getValue(), SECRET_CLASS));
				assertFalse(contains(e.getValue(), SECRET_HEAL));
			} else {
				assertTrue(e.getKey() + " is lossless", Arrays.equals(e.getValue(), sourceBefore.get(e.getKey())));
			}
		}

		step("migration: demo checks");
		// 10. no demo data reached the clean install
		assertFalse(cleanFiles.containsKey("runs/demo-stray.apdlog"));
		for (String k : files(new File(base, "clean")).keySet()) assertFalse(k, k.contains("demo-"));
		assertFalse(new File(new File(base, "clean"), DemoData.DIR_NAME).exists());

		step("migration: rebuild");
		// 5. rebuild every derived index and cache from the imported raw history
		delete(new File(clean, "derived"));
		new File(clean, "index.apdi").delete();
		StatsStore store = new StatsStore(clean);
		store.index().rebuild();
		assertTrue(store.index().size() > 0);
		Catalog rebuilt = new DatasetStore(clean).load();
		assertTrue(rebuilt.logsRebuilt > 0);

		step("migration: compare UIMSD");
		// 6. what UIMSD shows is the same as at the source
		ApdRecorder.reset();
		assertEquals(publicDump(source), publicDump(clean));
		assertFalse("the demo rows are not in the real history", publicDump(clean).contains("demo-"));

		step("migration: import again");
		// 7. importing the same archive again changes nothing
		Map<String, byte[]> beforeAgain = logsOnly(files(clean));
		ApdRecorder.configure(clean, false);
		String again = importNow(zip);
		assertTrue(again, again.startsWith("Imported 0 logs"));
		assertTrue(again, again.contains("32 already here"));
		Map<String, byte[]> afterAgain = logsOnly(files(clean));
		assertEquals(beforeAgain.keySet(), afterAgain.keySet());
		for (String k : beforeAgain.keySet()) assertTrue(k, Arrays.equals(beforeAgain.get(k), afterAgain.get(k)));
	}

	@Test
	public void demoDataCanNeitherBeExportedNorImported() throws Exception {
		File demoRoot = DemoData.demoRootFor(source);
		DemoGenerator.generate(demoRoot, Ruleset.of(30, 0), System.currentTimeMillis());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertFalse(ArchiveExporter.export(demoRoot, out, "t", 1).ok);
		//the recorder will not even use the demo folder as history
		ApdRecorder.configure(demoRoot, true);
		assertTrue(ApdRecorder.historyRoot() == null);
		ApdRecorder.reset();
		//an archive that contains a demo log is refused whole, and nothing is written
		byte[] demoLog = read(new File(demoRoot, "runs/demo-000.apdlog"));
		ArchiveManifest m = new ArchiveManifest();
		ArchiveManifest.Entry e = new ArchiveManifest.Entry();
		e.path = "runs/demo-000.apdlog";
		e.size = demoLog.length;
		e.sha256 = sha(demoLog);
		e.runId = "demo-000";
		m.files.add(e);
		ByteArrayOutputStream zip = new ByteArrayOutputStream();
		ZipOutputStream z = new ZipOutputStream(zip);
		z.putNextEntry(new ZipEntry(ArchiveManifest.ENTRY_NAME));
		z.write(m.encode().getBytes("UTF-8"));
		z.closeEntry();
		z.putNextEntry(new ZipEntry(e.path));
		z.write(demoLog);
		z.closeEntry();
		z.finish();
		ArchiveImporter.Report r = ArchiveImporter.importArchive(other, new ByteArrayInputStream(zip.toByteArray()));
		assertFalse(r.ok);
		assertTrue(logsOnly(files(other)).isEmpty());
		//and importing a real archive into the demo folder is refused
		ApdRecorder.configure(source, false);
		File archive = new File(exports, "a.zip");
		exportNow(archive);
		ApdRecorder.reset();
		assertFalse(ArchiveImporter.importArchive(demoRoot, new FileInputStream(archive)).ok);
	}

	@Test
	public void damagedArchivesLeaveTheHistoryExactlyAsItWas() throws Exception {
		ApdRecorder.configure(source, false);
		File archiveFile = new File(exports, "good.zip");
		exportNow(archiveFile);
		ApdRecorder.reset();
		byte[] good = read(archiveFile);
		Map<String, byte[]> parts = entries(good);

		List<byte[]> bad = new ArrayList<>();
		bad.add(new byte[0]);
		bad.add("this is not a zip".getBytes("UTF-8"));
		bad.add(Arrays.copyOf(good, good.length / 2));        //cut in half
		bad.add(Arrays.copyOf(good, 100));                    //cut inside the manifest
		//a manifest with one character changed
		bad.add(rezip(parts, ArchiveManifest.ENTRY_NAME, new String(parts.get(ArchiveManifest.ENTRY_NAME), "UTF-8").replace("WARRIOR", "WARRIOX").replace("run-fin-3", "run-fin-X").getBytes("UTF-8")));
		//one log with a flipped byte (its checksum no longer matches), all the others perfect
		byte[] flipped = parts.get("runs/fin3.apdlog").clone();
		flipped[flipped.length / 2] ^= 0x44;
		bad.add(rezip(parts, "runs/fin3.apdlog", flipped));
		//one log replaced by something that is not a log at all
		bad.add(rezip(parts, "runs/fin4.apdlog", "definitely not an APD log".getBytes("UTF-8")));
		for (int i = 0; i < bad.size(); i++){
			File clean2 = new File(base, "damaged" + i + "/apd");
			ArchiveImporter.Report r = ArchiveImporter.importArchive(clean2, new ByteArrayInputStream(bad.get(i)));
			assertFalse("damaged archive " + i + " was accepted: " + r.summary(), r.ok);
			assertEquals(0, r.imported);
			assertTrue("damaged archive " + i + " left logs behind", logsOnly(files(clean2)).isEmpty());
			assertFalse(new File(clean2, "import-tmp").exists());
		}
		//one more, imported into a history that already has runs: those runs are untouched too
		File populated = new File(base, "populated/apd");
		buildHistory(populated);
		Map<String, byte[]> before = logsOnly(files(populated));
		ArchiveImporter.Report r = ArchiveImporter.importArchive(populated, new ByteArrayInputStream(bad.get(5)));
		assertFalse(r.ok);
		Map<String, byte[]> after = logsOnly(files(populated));
		assertEquals(before.keySet(), after.keySet());
		for (String k : before.keySet()) assertTrue(k, Arrays.equals(before.get(k), after.get(k)));
	}

	private static byte[] rezip( Map<String, byte[]> parts, String replaceName, byte[] replacement ) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream z = new ZipOutputStream(out);
		z.putNextEntry(new ZipEntry(ArchiveManifest.ENTRY_NAME));
		z.write(replaceName.equals(ArchiveManifest.ENTRY_NAME) ? replacement : parts.get(ArchiveManifest.ENTRY_NAME));
		z.closeEntry();
		for (Map.Entry<String, byte[]> e : parts.entrySet()){
			if (e.getKey().equals(ArchiveManifest.ENTRY_NAME)) continue;
			z.putNextEntry(new ZipEntry(e.getKey()));
			z.write(e.getKey().equals(replaceName) ? replacement : e.getValue());
			z.closeEntry();
		}
		z.finish();
		return out.toByteArray();
	}

	@Test
	public void cancellationAndErrorsChangeNothingAndSayWhatHappened() throws Exception {
		ApdRecorder.configure(source, false);
		Map<String, byte[]> before = files(source);

		//cancelled save: no file appears
		cancel = true;
		chosen = null;
		String cancelledExport = await(new Action() {
			public void run( HistoryTransfer.Listener l ){
				HistoryTransfer.export(l);
			}
		});
		assertTrue(cancelledExport, cancelledExport.contains("cancelled"));
		assertEquals(0, exports.list().length);

		//cancelled open
		String cancelledImport = await(new Action() {
			public void run( HistoryTransfer.Listener l ){
				HistoryTransfer.importHistory(l);
			}
		});
		assertTrue(cancelledImport, cancelledImport.contains("cancelled"));

		//the chosen place cannot be written (a folder that does not exist)
		cancel = false;
		chosen = new File(base, "no/such/folder/history.zip");
		String failedExport = await(new Action() {
			public void run( HistoryTransfer.Listener l ){
				HistoryTransfer.export(l);
			}
		});
		assertTrue(failedExport, failedExport.toLowerCase().contains("failed"));
		assertFalse(chosen.exists());

		//the chosen file to import does not exist
		chosen = new File(base, "missing.zip");
		String failedImport = await(new Action() {
			public void run( HistoryTransfer.Listener l ){
				HistoryTransfer.importHistory(l);
			}
		});
		assertTrue(failedImport, failedImport.toLowerCase().contains("failed"));

		//the chosen file is not an archive at all
		chosen = put(exports, "junk.zip", "junk".getBytes("UTF-8"));
		String junk = await(new Action() {
			public void run( HistoryTransfer.Listener l ){
				HistoryTransfer.importHistory(l);
			}
		});
		assertTrue(junk, junk.startsWith("Nothing was imported"));

		//nothing about the history changed through any of this
		Map<String, byte[]> after = files(source);
		assertEquals(before.keySet(), after.keySet());
		for (String k : before.keySet()) assertTrue(k, Arrays.equals(before.get(k), after.get(k)));
	}

	@Test
	public void theApdSelfTestPassesAfterAMigration() throws Exception {
		ApdRecorder.configure(source, false);
		File zip = new File(exports, "h.zip");
		exportNow(zip);
		ApdRecorder.configure(clean, false);
		importNow(zip);
		ApdRecorder.reset();
		File work = new File(base, "selftest-work");
		assertTrue(work.mkdirs());
		SelfTest.Report r = SelfTest.run(work, 20000);
		UimsdSelfTest.run(work, r);
		ArchiveSelfTest.run(work, r);
		assertTrue(String.valueOf(r.lines), r.passed);
	}
}
