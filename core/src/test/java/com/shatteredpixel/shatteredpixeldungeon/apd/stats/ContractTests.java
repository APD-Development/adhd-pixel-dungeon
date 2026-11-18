package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Rules about the library as a whole rather than one class.
public class ContractTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static List<String> lines( File f ) throws Exception {
		List<String> out = new ArrayList<>();
		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
		try {
			String line;
			while ((line = r.readLine()) != null) out.add(line);
		} finally {
			r.close();
		}
		return out;
	}

	//---- event type ids are never reused ------------------------------------------------------------------

	@Test
	public void eventTypeRegistryMatchesTheCheckedInManifest() throws Exception {
		File manifest = new File(System.getProperty("apd.event.manifest"));
		Map<Integer, String> recorded = new LinkedHashMap<>();
		for (String line : lines(manifest)){
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#")) continue;
			String[] parts = line.split("\\s+");
			assertEquals("manifest line: " + line, 2, parts.length);
			int id = Integer.parseInt(parts[0]);
			assertTrue("id 0 is invalid", id > 0);
			assertTrue("duplicate id " + id + " in the manifest", recorded.put(id, parts[1]) == null);
		}

		//1. nothing in the manifest may be removed or change meaning
		for (Map.Entry<Integer, String> e : recorded.entrySet()){
			assertEquals("event type " + e.getKey() + " was removed or renamed. IDs are never reused or changed.",
					e.getValue(), EventTypes.all().get(e.getKey()));
		}
		//2. a new event type must be added to the manifest in the same change, so it is a conscious, reviewed step
		for (Map.Entry<Integer, String> e : EventTypes.all().entrySet()){
			assertEquals("new event type " + e.getKey() + " " + e.getValue() + " must be added to event_type_manifest.txt",
					e.getValue(), recorded.get(e.getKey()));
		}
		//3. names are unique too
		Set<String> names = new HashSet<>(EventTypes.all().values());
		assertEquals(EventTypes.all().size(), names.size());
	}

	//---- the library stays independent of the game ----------------------------------------------------------

	private static final String[] FORBIDDEN = {
			//randomness: the recorder must never touch SPD's seeded RNG, or any RNG
			"com.watabou.utils.Random", "java.util.Random", "ThreadLocalRandom", "Math.random", "SecureRandom",
			//the game and its engine
			"com.badlogic", "Gdx", "com.watabou", "Dungeon", "Actor", "Statistics", "Rankings",
			//APIs missing on Android 5.0: the conservative set the library was written against (APD's minSdk is now 26, so most of these
			//could be relaxed; kept for now, see docs/APD-MIN-ANDROID.md)
			"java.nio.file", "String.join", "computeIfAbsent", "getOrDefault", ".stream(", "Optional", "List.of(",
			"Map.of(", "Set.of(", "java.util.function", "Math.floorMod", "Math.floorDiv", "removeIf", "forEach(",
	};

	@Test
	public void libraryUsesNoRandomnessNoGameClassesAndNoApisMissingOnAndroid5() throws Exception {
		//the storage library, the query library (UIMSD's engine) and the archive library are all pure: no game, no randomness, no newer-Android APIs
		List<File> all = new ArrayList<>();
		for (String prop : new String[]{"apd.stats.src", "apd.query.src", "apd.archive.src"}){
			File[] some = new File(System.getProperty(prop)).listFiles((d, name) -> name.endsWith(".java"));
			assertNotNull(some);
			assertTrue(some.length > 5);
			for (File f : some) all.add(f);
		}
		File[] sources = all.toArray(new File[0]);
		List<String> problems = new ArrayList<>();
		for (File src : sources){
			boolean inBlockComment = false;
			int n = 0;
			for (String raw : lines(src)){
				n++;
				String line = raw.trim();
				//skip the licence header and comments, they may mention the game
				if (inBlockComment){
					if (line.contains("*/")) inBlockComment = false;
					continue;
				}
				if (line.startsWith("/*")){
					if (!line.contains("*/")) inBlockComment = true;
					continue;
				}
				if (line.startsWith("//")) continue;
				int comment = line.indexOf("//");
				if (comment >= 0) line = line.substring(0, comment);

				for (String bad : FORBIDDEN){
					if (line.contains(bad)) problems.add(src.getName() + ":" + n + " uses \"" + bad + "\": " + line);
				}
				if (line.startsWith("import com.shatteredpixel") && !line.contains("shatteredpixeldungeon.apd.stats.")
						&& !line.contains("shatteredpixeldungeon.apd.query.") && !line.contains("shatteredpixeldungeon.apd.archive.")){
					problems.add(src.getName() + ":" + n + " imports game code: " + line);
				}
			}
		}
		if (!problems.isEmpty()) fail("stats library rules broken:\n" + String.join("\n", problems));
	}

	//---- the Records history stays in step with the telemetry version -------------------------------------------

	@Test
	public void recordsHistoryDescribesTheCurrentTelemetryVersion() throws Exception {
		List<String> all = lines(new File("apd/apd_records_history.txt")); //tests run from the assets folder
		assertTrue(all.get(0).startsWith("APD records history, format 2"));
		Set<String> ids = new HashSet<>();
		boolean foundCurrent = false;
		int blocks = 0;
		for (String line : all){
			if (line.isEmpty() || line.startsWith("-") || !line.contains(" | ")) continue;
			//the only lines allowed to contain " | " are milestone headers, so a malformed header cannot hide
			assertTrue("malformed milestone header: " + line, line.matches(
					"[a-z0-9][a-z0-9.-]* \\| (-|telemetry \\d+\\.\\d+) \\| APD [^|]+ \\| \\d{4}-\\d{2}-\\d{2}"));
			String[] parts = line.split(" \\| ");
			assertEquals("milestone header needs 4 fields: " + line, 4, parts.length);
			assertTrue("milestone id used twice: " + parts[0], ids.add(parts[0]));
			blocks++;
			if (!parts[1].equals("-")){
				assertTrue("telemetry field must say 'telemetry X.Y': " + line, parts[1].startsWith("telemetry "));
				TelemetryVersion v = TelemetryVersion.parse(parts[1]);
				assertFalse("history mentions a telemetry version newer than the code: " + line,
						v.isNewerThan(TelemetryVersion.CURRENT));
				if (v.equals(TelemetryVersion.CURRENT)) foundCurrent = true;
			}
			assertTrue(parts[2].startsWith("APD "));
		}
		assertTrue(blocks >= 3);
		assertTrue("add a milestone for telemetry " + TelemetryVersion.CURRENT + " to apd_records_history.txt", foundCurrent);
	}

	//---- a whole run, crash and all -----------------------------------------------------------------------------

	@Test
	public void aRunSurvivesACrashAndCanBeContinuedAndSealed() throws Exception {
		File root = tmp.newFolder("apd");
		StatsStore store = new StatsStore(root);

		RunLogWriter w = store.startRun("run-1", "APD 4.0.8-apd", 1000);
		assertEquals(RunLogWriter.OpenStatus.OK, w.openStatus());
		for (int i = 0; i < 100; i++) w.event(LogTestSupport.T_STRINGS).clock(i).unsigned(i).string("floor" + (i % 5)).signed(i).emit();
		assertTrue(w.flush(10_000, true));
		//simulate the process dying: no close, and half a frame of a later event reached the disk
		File log = store.logFile("run-1");
		byte[] onDisk = LogTestSupport.bytes(log);
		byte[] half = LogFormat.eventFrame(LogTestSupport.T_PLAIN, 0, 1, new byte[]{1, 2, 3, 4});
		LogTestSupport.write(log, LogTestSupport.concat(onDisk, java.util.Arrays.copyOf(half, half.length - 4)));
		w.close(false); //only to stop the old writer's thread, it appends nothing

		//next start of the app: a new store object over the same folder
		StatsStore after = new StatsStore(root);
		List<LogRecovery.RepairReport> reports = after.recoverAll();
		assertEquals(1, reports.size());
		assertTrue(reports.get(0).truncated);
		assertEquals(RunIndexEntry.Status.OPEN, after.index().get("run-1").status);
		assertEquals(100, after.index().get("run-1").eventCount);

		RunLogWriter again = RunLogWriter.openForAppend(after.logFile("run-1"), TelemetryVersion.CURRENT);
		assertEquals(RunLogWriter.OpenStatus.OK, again.openStatus()); //already repaired, nothing more to cut
		assertTrue(again.event(LogTestSupport.T_STRINGS).clock(500).unsigned(500).string("floor1").signed(0).emit());
		assertTrue(again.close());
		after.refreshIndex("run-1");

		RunIndexEntry e = after.index().get("run-1");
		assertEquals(RunIndexEntry.Status.SEALED, e.status);
		assertEquals(101, e.eventCount);
		ReadResult r = RunLogReader.read(after.logFile("run-1"));
		assertEquals(ReadStatus.SEALED, r.status);
		assertEquals(5, r.strings.size());
		assertEquals(TelemetryVersion.CURRENT, r.header.telemetryVersion);
		assertEquals("APD 4.0.8-apd", r.header.writerBuild);
	}
}
