package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//Histories and archives for the archive tests.
final class ArchiveSupport {

	private ArchiveSupport(){}

	static final TelemetryVersion V = TelemetryVersion.CURRENT;
	static final String RS = "r1-aaaaaaaaaaaa";
	static final String SECRET_CLASS = "items.rings.RingOfSecretHaste";
	static final String SECRET_ROOM = "Treasury of Secrets";
	static final String SECRET_SEED = "TOP-SECRET-SEED";

	//A history with: a finished run (hidden information, seed 777), an unfinished run (hidden information, seed 888), a run recorded in
	//two segments that ended in the second, an imported-record file, and a run recorded by older telemetry.
	static void history( File root ) throws Exception {
		TestLogs fin = new TestLogs(root, "fin", V, 1000).seeds(777L, "finished-seed").start("run-fin", 0, "WARRIOR", RS, 0, 30, false).enter(1);
		fin.registered("f-sword", "items.weapon.melee.Sword", 1, false, ItemEvents.ORIGIN_GENERATED).generated("f-sword", GenerationEvents.C_HEAP, "Hidden Room")
				.seen("f-sword", "sword", ItemEvents.CAT_WEAPON).acquired("f-sword", "sword", ItemEvents.CAT_WEAPON, false, 0).equip("f-sword", "sword", ItemEvents.CAT_WEAPON)
				.mobGenerated("actors.mobs.Gnoll").heroAttacks("rat", "actors.mobs.Rat", true).damageFromHero("rat", "actors.mobs.Rat", 3).heroKills("rat", "actors.mobs.Rat", 1)
				.end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 2, 100).close(true);

		TestLogs open = new TestLogs(root, "open", V, 2000).seeds(888L, SECRET_SEED).start("run-open", 0, "MAGE", RS, 0, 30, false).enter(1);
		open.registered("o-ring", SECRET_CLASS, 3, true, ItemEvents.ORIGIN_GENERATED).generated("o-ring", GenerationEvents.C_CHEST, SECRET_ROOM)
				.seen("o-ring", "ruby ring", ItemEvents.CAT_RING).acquired("o-ring", "ruby ring", ItemEvents.CAT_RING, false, 0)
				.registered("o-secret", "items.armor.PlateArmor", 9, false, ItemEvents.ORIGIN_GENERATED).mobGenerated("actors.mobs.Goo")
				.heroAttacks("rat", "actors.mobs.Rat", true).damageFromHero("rat", "actors.mobs.Rat", 4).tick(30).enter(2).close(false);

		TestLogs a = new TestLogs(root, "two-a", V, 3000).start("run-two", 0, "ROGUE", RS, 0, 30, false).enter(1);
		a.registered("t-dagger", "items.weapon.melee.Dagger", 0, false, ItemEvents.ORIGIN_GENERATED).seen("t-dagger", "dagger", ItemEvents.CAT_WEAPON).close(false);
		TestLogs b = new TestLogs(root, "two-b", V, 4000).start("run-two", 1, "ROGUE", RS, 0, 30, false).enter(2);
		b.heroAttacks("snake", "actors.mobs.Snake", true).end(LifecycleEvents.OUTCOME_WON, "", 26, 25, 9000).close(true);

		File legacy = new File(new File(root, "legacy"), "import-1.apdlog");
		RunLogWriter w = RunLogWriter.create(legacy, V, "import-1", "t", 5000);
		LegacyEvents.Totals t = new LegacyEvents.Totals();
		t.source = "rankings.dat";
		t.importedAtMillis = 5000;
		t.fields.put("legacy.totalNumber", LegacyEvents.Value.num(41));
		LegacyEvents.emitTotals(w, 1, t);
		w.close(true);

		TestLogs old = new TestLogs(root, "old", new TelemetryVersion(1, 1), 500).start("run-old", 0, "HUNTRESS", RS, 0, 0, false).enter(1);
		old.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 5).close(true);
	}

	static byte[] export( File root ) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ArchiveExporter.Report r = ArchiveExporter.export(root, out, "test", 123456);
		if (!r.ok) throw new IllegalStateException(r.problem);
		return out.toByteArray();
	}

	//a zip with these entries, in this order
	static byte[] zip( List<String> names, List<byte[]> contents ) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream z = new ZipOutputStream(out);
		for (int i = 0; i < names.size(); i++){
			z.putNextEntry(new ZipEntry(names.get(i)));
			z.write(contents.get(i));
			z.closeEntry();
		}
		z.finish();
		return out.toByteArray();
	}

	//a valid manifest for these files, then the files
	static byte[] archiveOf( Map<String, byte[]> files, Map<String, String> runIds ) throws Exception {
		ArchiveManifest m = new ArchiveManifest();
		m.createdMillis = 1;
		m.app = "t";
		m.telemetry = "1.5";
		m.container = 1;
		for (Map.Entry<String, byte[]> e : files.entrySet()){
			ArchiveManifest.Entry x = new ArchiveManifest.Entry();
			x.path = e.getKey();
			x.size = e.getValue().length;
			x.sha256 = ArchiveManifest.sha256Hex(e.getValue());
			x.runId = runIds.containsKey(e.getKey()) ? runIds.get(e.getKey()) : "";
			x.kind = e.getKey().startsWith("legacy/") ? ArchiveManifest.KIND_IMPORTED : ArchiveManifest.KIND_RUN;
			m.files.add(x);
		}
		List<String> names = new ArrayList<>();
		List<byte[]> contents = new ArrayList<>();
		names.add(ArchiveManifest.ENTRY_NAME);
		contents.add(m.encode().getBytes(ArchiveManifest.utf8()));
		for (Map.Entry<String, byte[]> e : files.entrySet()){
			names.add(e.getKey());
			contents.add(e.getValue());
		}
		return zip(names, contents);
	}

	static Map<String, byte[]> files( File root ) throws Exception {
		Map<String, byte[]> out = new TreeMap<>();
		walk(root, root, out);
		return out;
	}

	private static void walk( File base, File f, Map<String, byte[]> out ) throws Exception {
		File[] kids = f.listFiles();
		if (kids == null) return;
		for (File k : kids){
			if (k.isDirectory()) walk(base, k, out);
			else out.put(base.toURI().relativize(k.toURI()).getPath(), Files.readAllBytes(k.toPath()));
		}
	}

	//a minimal valid log that says it belongs to 'runId'
	static byte[] smallLog( File scratch, String logId, String runId ) throws Exception {
		TestLogs l = new TestLogs(scratch, logId, V, 7000).start(runId, 0, "WARRIOR", RS, 0, 0, false).enter(1);
		l.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		return Files.readAllBytes(l.file.toPath());
	}

	static List<String> names( String... n ){
		List<String> l = new ArrayList<>();
		Collections.addAll(l, n);
		return l;
	}

	static boolean contains( byte[] data, String text ){
		byte[] t = text.getBytes(java.nio.charset.Charset.forName("UTF-8"));
		outer:
		for (int i = 0; i + t.length <= data.length; i++){
			for (int j = 0; j < t.length; j++) if (data[i + j] != t[j]) continue outer;
			return true;
		}
		return false;
	}
}
