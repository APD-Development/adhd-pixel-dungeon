package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Golden baseline for seeded dungeon generation.
//
//Each line of the golden file is one generated level (a case, a depth) and either
//  STABLE   - came out byte-identical in every one of RECORD_JVMS fresh, deliberately perturbed JVMs, with its fingerprint
//  UNSTABLE - came out differently depending on JVM history (an existing SPD property, see docs/APD-STATS-PHASE0.md)
//This historical pre-recorder fixture uses its original twelve-scroll pool. The
//approved Gathering row is covered separately by LootScrollGenerationTest; do not
//rewrite the old baseline merely because a new item was deliberately added.
//Only STABLE levels are compared. A future recorder build must reproduce every STABLE line exactly.
//
//The baseline was recorded from the code as it stood before any statistics recorder existed.
//To intentionally re-record it (only after a deliberate generation change) run:
//  ./gradlew :core:test -Papd.golden.update=true
public class GoldenLevelHashTest {

	private static final int RECORD_JVMS = 24;

	//one level as seen by one run: key is "label|depth", value is the harness line
	static Map<String, String> parse( List<String> lines ){
		Map<String, String> out = new LinkedHashMap<>();
		for (String l : lines){
			if (!l.startsWith("APDLINE ")) continue;
			String body = l.substring("APDLINE ".length());
			int first = body.indexOf('|');
			int second = body.indexOf('|', first + 1);
			out.put(body.substring(0, second), body.substring(second + 1));
		}
		return out;
	}

	private static List<String> runChild( int perturb ) throws Exception {
		return runChild(perturb, "legacy-scrolls");
	}

	static List<String> runChild( int perturb, String mode ) throws Exception {
		String java = new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath();
		ProcessBuilder pb = new ProcessBuilder(java, "-cp", System.getProperty("apd.test.classpath"),
				GoldenChildMain.class.getName(), Integer.toString(perturb), mode);
		pb.redirectErrorStream(true);
		Process process = pb.start();
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))){
			String line;
			while ((line = reader.readLine()) != null) lines.add(line);
		}
		assertEquals("child JVM " + perturb + " failed", 0, process.waitFor());
		return lines;
	}

	static String hashOf( String harnessLine ){
		return harnessLine.substring(harnessLine.indexOf("sha256=") + 7);
	}

    // Owner-approved Floor 5 room redesign: retain the original archive untouched,
    // require all eleven new Floor 5 fingerprints, and compare every other floor
    // to its original hash. Gold conservation/RNG/orientations have separate real-room tests.
    private static String reviewedHash(File golden,String key,String historical) throws Exception {
        if(!key.endsWith("|5"))return hashOf(historical);
        Map<String,String> reviewed=new LinkedHashMap<>();
        for(String row:Files.readAllLines(new File(golden.getParentFile(),"golden_rat_king_floor5_20261104.tsv").toPath(),StandardCharsets.UTF_8)){
            String[] parts=row.split("\t");assertEquals(2,parts.length);assertTrue(parts[0].endsWith("|5"));
            assertEquals(64,parts[1].length());assertEquals(null,reviewed.put(parts[0],parts[1]));
        }
        assertEquals("All original Floor 5 cases remain covered",GoldenCases.all().size(),reviewed.size());
        assertTrue("Missing reviewed Rat King case: "+key,reviewed.containsKey(key));
        return reviewed.get(key);
    }

	@Test
	public void generatedLevelsMatchGoldenBaseline() throws Exception {
		File golden = new File(System.getProperty("apd.golden.file"));

		if (Boolean.parseBoolean(System.getProperty("apd.golden.update", "false"))){
			Map<String, Set<String>> hashes = new LinkedHashMap<>();
			Map<String, String> sample = new LinkedHashMap<>();
			for (int k = 0; k < RECORD_JVMS; k++){
				Map<String, String> run = parse(runChild(k * 173 + 11));
				for (Map.Entry<String, String> e : run.entrySet()){
					sample.putIfAbsent(e.getKey(), e.getValue());
					hashes.computeIfAbsent(e.getKey(), x -> new LinkedHashSet<>()).add(hashOf(e.getValue()));
				}
			}

			List<String> lines = new ArrayList<>();
			int stable = 0;
			for (String key : sample.keySet()){
				String label = key.substring(0, key.indexOf('|'));
				if (hashes.get(key).size() == 1){
					lines.add(label + " STABLE " + sample.get(key));
					stable++;
				} else {
					String s = sample.get(key);
					lines.add(label + " UNSTABLE " + s.substring(0, s.indexOf(" sha256=")).replaceAll(" \\d+x\\d+ mobs.*", "")
							+ " variants=" + hashes.get(key).size() + "/" + RECORD_JVMS);
				}
			}
			golden.getParentFile().mkdirs();
			Files.write(golden.toPath(), (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
			System.out.println("APD-GOLDEN wrote " + lines.size() + " levels: " + stable + " stable, "
					+ (lines.size() - stable) + " unstable, recorded over " + RECORD_JVMS + " JVMs, to " + golden);
			return;
		}

		assertTrue("golden file is missing: " + golden, golden.exists());
		List<String> expected = Files.readAllLines(golden.toPath(), StandardCharsets.UTF_8);

		//compare in a fresh child JVM, so the result does not depend on what this test JVM did before
		Map<String, String> actual = parse(runChild(0));
		assertEquals("number of generated levels", expected.size(), actual.size());

		int compared = 0, i = 0;
		for (Map.Entry<String, String> e : actual.entrySet()){
			String exp = expected.get(i++);
			if (!exp.contains(" STABLE ")) continue;
			String expectedHash = reviewedHash(golden,e.getKey(),exp);
			assertEquals("golden mismatch for " + e.getKey() + " (line " + i + ")", expectedHash, hashOf(e.getValue()));
			compared++;
		}
		System.out.println("APD-GOLDEN compared " + compared + " stable levels of " + expected.size());
	}

	//The same baseline, but with APD run recording switched on while the dungeons are generated.
	//Dungeon.init() starts a recorded run for every case, so the recorder hooks really run.
	@Test
	public void stableLevelsStillMatchTheGoldenBaselineWithTheRecorderOn() throws Exception {
		File golden = new File(System.getProperty("apd.golden.file"));
		if (Boolean.parseBoolean(System.getProperty("apd.golden.update", "false"))) return;
		List<String> expected = Files.readAllLines(golden.toPath(), StandardCharsets.UTF_8);
		Map<String, String> actual = parse(runChild(0, "legacy-scrolls-rec"));
		assertEquals("number of generated levels", expected.size(), actual.size());
		int compared = 0, i = 0;
		for (Map.Entry<String, String> e : actual.entrySet()){
			String exp = expected.get(i++);
			if (!exp.contains(" STABLE ")) continue;
			assertEquals("recording changed level generation for " + e.getKey() + " (line " + i + ")",
					reviewedHash(golden,e.getKey(),exp), hashOf(e.getValue()));
			compared++;
		}
		System.out.println("APD-GOLDEN (recorder on) compared " + compared + " stable levels of " + expected.size());
	}
}
