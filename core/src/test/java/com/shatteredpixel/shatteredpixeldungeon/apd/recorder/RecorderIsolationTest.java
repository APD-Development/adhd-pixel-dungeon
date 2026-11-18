package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

//The recorder may READ the game, it may not change it or touch random numbers. Checked on its source, so a later edit
//that crosses the line fails the build.
public class RecorderIsolationTest {

	private static final String[] FORBIDDEN = {
			//randomness (UUID.randomUUID is allowed: it has its own generator and never touches SPD's)
			"com.watabou.utils.Random", "java.util.Random", "ThreadLocalRandom", "SecureRandom", "Math.random",
			"pushGenerator", "popGenerator", "seedCurDepth", "seedForDepth",
			//changing the game
			"Actor.", ".damage(", ".spend(", ".heal(", ".die(", ".live(", ".attach", ".detach(", ".collect(", ".drop(",
			"Dungeon.hero =", "Dungeon.level =", "Dungeon.depth =", "Dungeon.branch =", "Dungeon.seed =", "Dungeon.gold =",
			"Statistics.duration =", "Statistics.deepestFloor =", "Dungeon.challenges =",
			//Actor.id() hands out the next id from a global counter the first time it is called: that is game state, use apdPeekId()
			".id()",
			//APIs missing on Android 5.0 (conservative: APD's minSdk is now 26, see docs/APD-MIN-ANDROID.md)
			"java.nio.file", "String.join", "computeIfAbsent", "getOrDefault", ".stream(", "Optional", "List.of(", "Map.of(",
			"java.util.function", "removeIf", "forEach(",
	};

	//entries ending in " =" mean an assignment: "x = y" is caught, "x == y" is a read and is fine
	private static boolean matches( String line, String bad ){
		if (bad.endsWith(" =")){
			String head = bad.substring(0, bad.length() - 2);
			return java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(head) + "\\s*=(?!=)").matcher(line).find();
		}
		return line.contains(bad);
	}

	@Test
	public void recorderSourceNeverTouchesRandomnessOrChangesTheGame() throws Exception {
		File dir = new File(System.getProperty("apd.recorder.src"));
		File[] sources = dir.listFiles((d, name) -> name.endsWith(".java"));
		assertNotNull(sources);
		assertTrue(sources.length >= 5);
		List<String> problems = new ArrayList<>();
		for (File src : sources){
			boolean block = false;
			int n = 0;
			BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(src), "UTF-8"));
			try {
				String raw;
				while ((raw = r.readLine()) != null){
					n++;
					String line = raw.trim();
					if (block){ if (line.contains("*/")) block = false; continue; }
					if (line.startsWith("/*")){ if (!line.contains("*/")) block = true; continue; }
					if (line.startsWith("//")) continue;
					int c = line.indexOf("//");
					if (c >= 0) line = line.substring(0, c);
					for (String bad : FORBIDDEN){
						if (matches(line, bad)) problems.add(src.getName() + ":" + n + " uses \"" + bad + "\": " + line);
					}
				}
			} finally {
				r.close();
			}
		}
		if (!problems.isEmpty()){
			StringBuilder sb = new StringBuilder("recorder rules broken:\n");
			for (String p : problems) sb.append(p).append('\n');
			fail(sb.toString());
		}
	}
}
