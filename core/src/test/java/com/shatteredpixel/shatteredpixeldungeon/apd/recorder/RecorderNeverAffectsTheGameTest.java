package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.GoldenLevelHashTest;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.LevelGenerationHarness;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//Recording must not change how the game plays out. Three angles:
//  1. the recorder's source never touches randomness or changes game state (static check, RecorderIsolationTest)
//  2. the dungeons come out the same with recording on (golden baseline, GoldenLevelHashTest)
//  3. here: a played-through flow (arrive on levels, save) gives the same levels and the same save files either way
public class RecorderNeverAffectsTheGameTest {

	@After
	public void stop(){
		ApdRecorder.reset();
	}

	//---- helpers -------------------------------------------------------------------------------------

	//a saved game as sorted JSON, without the fields that legitimately differ (run id, time stamp, random item ids)
	private static String canonicalSave( int slot ) throws Exception {
		Bundle b = FileUtils.bundleFromFile(GamesInProgress.gameFile(slot));
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		Bundle.write(b, bytes, false);
		Object json = new org.json.JSONTokener(new String(bytes.toByteArray(), StandardCharsets.UTF_8)).nextValue();
		StringBuilder out = new StringBuilder();
		canon(json, out);
		return out.toString();
	}

	private static void canon( Object v, StringBuilder out ){
		if (v instanceof JSONObject){
			JSONObject o = (JSONObject) v;
			TreeMap<String, Object> sorted = new TreeMap<>();
			Iterator<?> keys = o.keys();
			while (keys.hasNext()){
				String k = (String) keys.next();
				if (k.equals("apd_run_id") || k.equals("apd_uid") || k.equals("last_played") || k.equals("set_id")) continue;
				sorted.put(k, o.opt(k));
			}
			out.append('{');
			boolean first = true;
			for (Map.Entry<String, Object> e : sorted.entrySet()){
				if (!first) out.append(',');
				first = false;
				out.append('"').append(e.getKey()).append("\":");
				canon(e.getValue(), out);
			}
			out.append('}');
		} else if (v instanceof JSONArray){
			JSONArray a = (JSONArray) v;
			out.append('[');
			for (int i = 0; i < a.length(); i++){
				if (i > 0) out.append(',');
				canon(a.opt(i), out);
			}
			out.append(']');
		} else {
			out.append(String.valueOf(v));
		}
	}

	private static List<String> flow( boolean record, long seed, int depth, int slot ) throws Exception {
		HeadlessTestSupport.start();
		ApdRecorder.configure(new File(HeadlessTestSupport.tempDir(), "parity-" + System.nanoTime()), record);
		List<LevelGenerationHarness.LevelPrint> prints =
				LevelGenerationHarness.generateFlow(seed, 0, HeroClass.WARRIOR, LevelGenerationHarness.Context.FRESH, depth);
		List<String> out = new ArrayList<>();
		for (LevelGenerationHarness.LevelPrint p : prints) out.add(p.line());
		if (record){
			//make sure the recorder really did work during this flow, so the comparison is not vacuous
			RecorderStatus st = ApdRecorder.status();
			assertTrue("recorder should have written events", st.eventsAccepted >= depth * 4);
			assertEquals(1, st.runLogs);
		}
		ApdRecorder.reset();
		return out;
	}

	//levels the golden baseline marks STABLE (identical across perturbed JVMs), for one case label
	private static boolean[] stableLevels( String caseLabel, int depth ) throws Exception {
		boolean[] stable = new boolean[depth];
		for (String line : Files.readAllLines(new File(System.getProperty("apd.golden.file")).toPath(), StandardCharsets.UTF_8)){
			if (!line.startsWith(caseLabel + " ")) continue;
			String[] parts = line.split(" ");
			int d = Integer.parseInt(parts[parts.length - 1].equals("") ? "0" : lineDepth(line));
			if (d >= 1 && d <= depth) stable[d - 1] = line.contains(" STABLE ");
		}
		return stable;
	}

	//"... STABLE <seed> <challenges> <depth> <class> ..." and "... UNSTABLE <seed> <challenges> <depth> <class> ..."
	private static String lineDepth( String line ){
		String[] p = line.split(" ");
		for (int i = 0; i < p.length; i++){
			if (p[i].equals("STABLE") || p[i].equals("UNSTABLE")) return p[i + 3];
		}
		return "0";
	}

	//---- tests ---------------------------------------------------------------------------------------

	@Test
	public void playedThroughLevelsAreTheSameWithRecordingOnOrOff() throws Exception {
		int depth = 8;
		for (long seed : new long[]{1L, 987654321L}){
			List<String> off = flow(false, seed, depth, 1);
			List<String> on = flow(true, seed, depth, 1);
			boolean[] stable = stableLevels("plain-" + seed + " WARRIOR FRESH", depth);
			int compared = 0;
			for (int i = 0; i < depth; i++){
				if (!stable[i]) continue;
				assertEquals("seed " + seed + " depth " + (i + 1) + " differs with recording on", off.get(i), on.get(i));
				compared++;
			}
			assertTrue("expected to compare most levels, compared " + compared, compared >= 5);
		}
	}

	@Test
	public void saveFilesAreIdenticalExceptForTheRunId() throws Exception {
		//the same run twice without recording first: if THIS differs the comparison below would mean nothing
		String off1 = savedGame(false, 4711L);
		String off2 = savedGame(false, 4711L);
		assertEquals("test precondition: an unrecorded run is not reproducible", off1, off2);

		String on = savedGame(true, 4711L);
		assertEquals("recording must not change what is saved (apart from apd_run_id and apd_uid)", off1, on);
	}

	private static String savedGame( boolean record, long seed ) throws Exception {
		HeadlessTestSupport.start();
		ApdRecorder.configure(new File(HeadlessTestSupport.tempDir(), "savepar-" + System.nanoTime()), record);
		com.watabou.noosa.Game.realTime = 1_700_000_000_000L;
		SPDSettings.challenges(0);
		SPDSettings.intro(false);
		Dungeon.daily = false;
		Dungeon.dailyReplay = false;
		Dungeon.customSeedText = "";
		Dungeon.seed = seed;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		GamesInProgress.randomizedClass = false;
		GamesInProgress.curSlot = 8;
		Dungeon.init();
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.level = Dungeon.newLevel();
		Dungeon.switchLevel(Dungeon.level, -1);
		String text = canonicalSave(8);
		if (record) assertNotNull("the recorded save must carry the run id", FileUtils.bundleFromFile(GamesInProgress.gameFile(8)).getString("apd_run_id"));
		ApdRecorder.reset();
		return text;
	}

	@Test
	public void theGameRunsNormallyWhenRecordingIsNotSetUpAtAll() throws Exception {
		HeadlessTestSupport.start();
		ApdRecorder.reset(); //never configured
		GamesInProgress.curSlot = 9;
		SPDSettings.challenges(0);
		Dungeon.seed = 5L;
		Dungeon.customSeedText = "";
		Dungeon.daily = false;
		GamesInProgress.selectedClass = HeroClass.MAGE;
		Dungeon.init();
		Dungeon.depth = 1;
		Dungeon.level = Dungeon.newLevel();
		Dungeon.switchLevel(Dungeon.level, -1);
		assertEquals(null, ApdRecorder.currentRunId());
		assertEquals(false, FileUtils.bundleFromFile(GamesInProgress.gameFile(9)).contains("apd_run_id"));
	}
}
