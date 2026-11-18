package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.LegacyImporter;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunIndex;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map;

//The INDEV-only demo data: it makes UIMSD look populated, it is always recognisable as made up, and it can never touch, replace,
//join or pass for real history.
public class DemoDataTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Ruleset RULES = Ruleset.of(30, 0);
	private static final TelemetryVersion V = TelemetryVersion.CURRENT;

	private File realRoot;
	private File demoRoot;

	@Before
	public void setUp() throws Exception {
		File parent = tmp.newFolder("data");
		realRoot = new File(parent, "apd");
		demoRoot = DemoData.demoRootFor(realRoot);
		//a little real history
		new SyntheticLog(realRoot, "real-1", V, 1000).start("real-run-1", 0, "WARRIOR", RULES.id, 0, 30, false).enter(1)
				.acquired("axe", "axe", ItemEvents.CAT_WEAPON, false, 0).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 10).close(true);
		new SyntheticLog(realRoot, "real-2", V, 2000).start("real-run-2", 0, "MAGE", RULES.id, 0, 30, false).enter(1).close(false);
	}

	@After
	public void tearDown(){
		ApdRecorder.reset();
	}

	private static Map<String, String> snapshot( File dir ) throws Exception {
		Map<String, String> m = new TreeMap<>();
		walk(dir, dir, m);
		return m;
	}

	private static void walk( File base, File f, Map<String, String> m ) throws Exception {
		File[] kids = f.listFiles();
		if (kids == null) return;
		for (File k : kids){
			if (k.isDirectory()) walk(base, k, m);
			else m.put(base.toURI().relativize(k.toURI()).getPath(), k.length() + ":" + java.util.Arrays.hashCode(Files.readAllBytes(k.toPath())));
		}
	}

	private int generate() throws Exception {
		return DemoGenerator.generate(demoRoot, RULES, 1_800_000_000_000L);
	}

	// ---- it fills UIMSD ----

	@Test
	public void theDemoHistoryIsLargeAndVaried() throws Exception {
		assertEquals(DemoGenerator.RUNS, generate());
		assertTrue(DemoGenerator.RUNS >= 50 && DemoGenerator.RUNS <= 100);
		Catalog cat = new DatasetStore(demoRoot).load();
		Dataset runs = cat.dataset(Datasets.RUNS);
		DatasetDef def = runs.def;

		Set<String> classes = new HashSet<>(), outcomes = new HashSet<>(), deepest = new HashSet<>(), challenges = new HashSet<>();
		int unknownOutcome = 0, lower = 0, unfinished = 0;
		for (Row r : runs.rows){
			add(classes, r, def.column(FactKeys.HERO_CLASS));
			add(outcomes, r, def.column(FactKeys.OUTCOME));
			add(deepest, r, def.column(FactKeys.DEEPEST));
			add(challenges, r, def.column(FactKeys.CHALLENGES));
			if (!r.finished) unfinished++;
			if (!r.isKnown(def.column(FactKeys.OUTCOME).index)) unknownOutcome++;
			for (ColumnDef c : def.columns()) if (r.isKnown(c.index) && r.prov(c.index) == com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact.PROV_INCOMPLETE) lower++;
		}
		assertTrue("runs: " + runs.rows.size(), runs.rows.size() >= 60);
		assertTrue("classes " + classes, classes.size() >= 5);
		assertTrue("outcomes " + outcomes, outcomes.size() >= 2);
		assertTrue("deepest floors " + deepest.size(), deepest.size() >= 10);
		assertTrue("challenge sets " + challenges.size(), challenges.size() >= 3);
		assertTrue("unfinished runs", unfinished >= 3);
		assertTrue("some values are unknown (older telemetry, unfinished runs)", unknownOutcome >= 1);
		assertTrue("some values are lower bounds", lower >= 1);
		assertTrue("more than one ruleset, so the default view hides some with a count", cat.rulesets.size() >= 2);

		//every view has something in it
		for (ViewDef v : cat.views()){
			Dataset ds = cat.datasetOf(v);
			assertTrue(v.id + " has rows", ds != null && !ds.rows.isEmpty());
			QueryResult r = QueryEngine.run(ds, v.initialQuery().withMinSample(0).withScope(Scope.DEFAULT.withUnfinished(true)), RULES.id);
			assertTrue(v.id + " shows something", r.stats.total > 0);
		}
	}

	private static void add( Set<String> set, Row r, ColumnDef c ){
		if (c != null && r.isKnown(c.index)) set.add(r.kind(c.index) == Row.K_TEXT ? r.text(c.index) : Long.toString(r.num(c.index)));
	}

	// ---- it is always recognisable ----

	@Test
	public void everyDemoRecordSaysItIsMadeUp() throws Exception {
		generate();
		assertTrue(DemoData.isSyntheticRoot(demoRoot));
		File[] logs = new File(demoRoot, "runs").listFiles();
		assertTrue(logs.length >= DemoGenerator.RUNS);
		for (File f : logs){
			String id = f.getName().substring(0, f.getName().length() - RunIndex.LOG_SUFFIX.length());
			assertTrue(id, DemoData.isSyntheticLogId(id));
			ReadResult r = RunLogReader.read(f);
			assertTrue(f.getName(), DemoData.isSyntheticBuild(r.header.writerBuild));
			LifecycleEvents.RunStart s = null;
			for (com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent e : r.events){
				if (e.type == EventTypes.RUN_START){
					s = LifecycleEvents.decodeRunStart(e, r);
					break;
				}
			}
			assertTrue(f.getName() + " has a run start", s != null);
			assertEquals(DemoData.VERSION_MARK, s.apdVersionName);
			assertEquals(DemoData.BUILD_MARK, s.platform);
			assertEquals(DemoData.BUILD_MARK, s.customSeedText);
			assertTrue(s.runId.startsWith(DemoData.RUN_PREFIX));
		}
		//and the game version column of the Runs table says so on every row
		Dataset runs = new DatasetStore(demoRoot).load().dataset(Datasets.RUNS);
		ColumnDef version = runs.def.column(FactKeys.GAME_VERSION);
		for (Row r : runs.rows){
			if (r.isKnown(version.index)) assertEquals(DemoData.VERSION_MARK, r.text(version.index));
		}
	}

	// ---- real history is never touched ----

	@Test
	public void generatingAndRemovingNeverChangeRealHistory() throws Exception {
		Map<String, String> before = snapshot(realRoot);
		assertFalse(before.isEmpty());
		generate();
		assertEquals("generating", before, snapshot(realRoot));
		generate();   //reset
		assertEquals("resetting", before, snapshot(realRoot));
		assertTrue(DemoData.delete(demoRoot));
		assertEquals("removing", before, snapshot(realRoot));
		assertFalse(demoRoot.exists());
		assertTrue("removing twice is fine", DemoData.delete(demoRoot));
	}

	@Test
	public void demoDataLivesBesideRealHistoryNeverInsideIt() throws Exception {
		assertFalse(demoRoot.getAbsolutePath().startsWith(realRoot.getAbsolutePath() + File.separator));
		assertFalse(realRoot.getAbsolutePath().startsWith(demoRoot.getAbsolutePath() + File.separator));
		generate();
		assertFalse(new File(realRoot, DemoData.MARKER_FILE).exists());
	}

	@Test
	public void removalOnlyDeletesRealDemoFolders() throws Exception {
		//the real history folder: not demo data, nothing deleted
		assertFalse(DemoData.delete(realRoot));
		assertTrue(new File(realRoot, "runs").isDirectory());
		//a folder named like the demo folder but without the marker
		File impostor = new File(tmp.newFolder("other"), DemoData.DIR_NAME);
		assertTrue(impostor.mkdirs());
		File precious = new File(impostor, "mine.txt");
		assertTrue(precious.createNewFile());
		assertFalse(DemoData.delete(impostor));
		assertTrue(precious.exists());
		//a marked folder with another name
		generate();
		File renamed = new File(demoRoot.getParentFile(), "something-else");
		assertTrue(demoRoot.renameTo(renamed));
		assertTrue(DemoData.isSyntheticRoot(renamed));
		assertFalse(DemoData.delete(renamed));
		assertTrue(renamed.exists());
	}

	@Test
	public void generationRefusesAFolderThatIsNotDemoData() throws Exception {
		File impostor = new File(tmp.newFolder("other2"), DemoData.DIR_NAME);
		assertTrue(impostor.mkdirs());
		File precious = new File(impostor, "mine.txt");
		assertTrue(precious.createNewFile());
		try {
			DemoGenerator.generate(impostor, RULES, 1);
			org.junit.Assert.fail("wrote into a folder that is not demo data");
		} catch (java.io.IOException expected){
		}
		assertTrue(precious.exists());
		assertEquals(1, impostor.list().length);
		//and it will not write over the real history folder either
		try {
			DemoGenerator.generate(realRoot, RULES, 1);
			org.junit.Assert.fail("wrote into the real history");
		} catch (java.io.IOException expected){
		}
	}

	// ---- the two never mix, and demo data never counts as real ----

	@Test
	public void realAndDemoViewsAreIsolated() throws Exception {
		generate();
		int realRuns = new DatasetStore(realRoot).load().dataset(Datasets.RUNS).rows.size();
		int demoRuns = new DatasetStore(demoRoot).load().dataset(Datasets.RUNS).rows.size();
		assertEquals(2, realRuns);
		assertTrue(demoRuns >= 60);

		//a demo log that ends up among real logs is ignored by the real history...
		File demoLog = new File(new File(demoRoot, "runs"), "demo-000" + RunIndex.LOG_SUFFIX);
		assertTrue(demoLog.isFile());
		File copy = new File(new File(realRoot, "runs"), demoLog.getName());
		Files.copy(demoLog.toPath(), copy.toPath());
		assertEquals(realRuns, new DatasetStore(realRoot).load().dataset(Datasets.RUNS).rows.size());
		//...and a real log among demo data is ignored by the demo view
		File realLog = new File(new File(realRoot, "runs"), "real-1" + RunIndex.LOG_SUFFIX);
		Files.copy(realLog.toPath(), new File(new File(demoRoot, "runs"), realLog.getName()).toPath());
		assertEquals(demoRuns, new DatasetStore(demoRoot).load().dataset(Datasets.RUNS).rows.size());
	}

	@Test
	public void demoDataCanNeverBecomeLegitimateHistory() throws Exception {
		generate();
		//the recorder will not record into it
		ApdRecorder.configure(demoRoot, true);
		assertNull(ApdRecorder.historyRoot());
		assertFalse(ApdRecorder.status().summary().isEmpty());
		ApdRecorder.reset();
		//the importer will not write records into it
		Map<String, String> before = snapshot(demoRoot);
		LegacyImporter.Extract x = new LegacyImporter.Extract();
		x.fileFound = true;
		LegacyImporter.write(demoRoot, x, 5);
		assertEquals(before, snapshot(demoRoot));
		//the markers that future export / upload / ranking code must reject
		assertTrue(DemoData.isSyntheticRoot(demoRoot));
		assertFalse(DemoData.isSyntheticRoot(realRoot));
		assertFalse(DemoData.isSyntheticLogId("real-1"));
		assertFalse(DemoData.isSyntheticBuild("4.0.8-apd"));
	}

	@Test
	public void onlyDevelopmentBuildsOfferDemoData() {
		String old = com.watabou.noosa.Game.version;
		try {
			com.watabou.noosa.Game.version = "4.0.8-apd";
			assertFalse(DemoData.available());
			com.watabou.noosa.Game.version = null;
			assertFalse(DemoData.available());
			com.watabou.noosa.Game.version = "4.0.8-apd-INDEV-b26";
			assertTrue(DemoData.available());
		} finally {
			com.watabou.noosa.Game.version = old;
		}
	}

	// ---- hidden information stays hidden ----

	@Test
	public void unfinishedDemoRunsNeverRevealHiddenInformationEvenWithItSwitchedOn() throws Exception {
		generate();
		Catalog cat = new DatasetStore(demoRoot).load();
		int checked = 0;
		for (ViewDef v : cat.views()){
			Dataset ds = cat.datasetOf(v);
			Query q = v.initialQuery().withMinSample(0).withScope(Scope.DEFAULT.withUnfinished(true).withRules(Scope.Rules.ALL, "")).withShowInternal(true);
			for (Row row : ds.rows){
				if (row.finished) continue;
				for (ColumnDef c : ds.def.columns()){
					if (!c.isInternal()) continue;
					CellText t = CellFormatter.cell(q, row, c);
					assertTrue(v.id + "/" + c.id + " of an unfinished run is " + t.state, t.state == CellText.HIDDEN);
					checked++;
				}
			}
		}
		assertTrue("the demo data has unfinished runs with hidden columns to protect", checked > 0);
	}

	// ---- aggregate-first views of the demo data ----

	@Test
	public void theDemoDataGivesDenseAggregateTablesWithEveryTypeOnce() throws Exception {
		generate();
		Catalog cat = new DatasetStore(demoRoot).load();
		Scope all = Scope.DEFAULT.withRules(Scope.Rules.ALL, "").withUnfinished(true);
		for (String id : new String[]{ "weapons", "armor", "rings", "trinkets", "artifacts", "items", "enemies" }){
			ViewDef v = cat.view(id);
			QueryResult r = QueryEngine.run(cat.datasetOf(v), v.initialQuery().withScope(all), RULES.id);
			assertTrue(id + " is grouped", r.isGrouped());
			assertTrue(id + " has several kinds: " + r.groups.size(), r.groups.size() >= 3);
			assertEquals("no type is hidden for having few instances", 0, r.stats.hiddenSmallGroups);
			//one row per kind: the same kind never appears twice
			Set<String> seen = new HashSet<>();
			for (GroupRow g : r.groups){
				StringBuilder k = new StringBuilder();
				for (ColumnDef c : r.groupKeys) k.append(CellFormatter.cell(r.query, g.sample, c).text).append('|');
				assertTrue(id + " lists " + k + " twice", seen.add(k.toString()));
			}
		}
		//enchanted weapons ("blazing sword") are the same type as plain ones: no more weapon types than weapon kinds exist in the demo
		ViewDef weapons = cat.view("weapons");
		QueryResult w = QueryEngine.run(cat.datasetOf(weapons), weapons.initialQuery().withScope(all), RULES.id);
		assertTrue("weapon types: " + w.groups.size(), w.groups.size() <= 12);
		for (GroupRow g : w.groups){
			String type = CellFormatter.cell(w.query, g.sample, w.groupKeys.get(0)).text.toLowerCase();
			for (String enchant : new String[]{ "blazing", "chilling", "vampiric", "shocking", "grim", "lucky", "projecting", "unstable" }){
				assertFalse("an enchantment became part of a type: " + type, type.startsWith(enchant));
			}
		}
		//the full path works on the demo data: type -> statistics -> instances -> run, and type -> runs -> run
		UimsdModel m = new UimsdModel(RULES.id, "demo");
		m.setCatalog(cat);
		m.setScope(all);
		m.selectView("weapons");
		GroupRow biggest = m.result().groups.get(0);
		assertTrue(GroupDetail.build(m.result(), biggest).lines.size() > 10);
		m.openGroupRows(biggest);
		assertEquals(biggest.n, m.result().rows.size());
		m.openLink(m.result().rows.get(0), m.dataset().def.links.get(0));
		assertEquals("runs", m.viewId());
		assertEquals(1, m.result().rows.size());
		m.back();
		m.back();
		int runs = m.groupRunCount(biggest);
		assertEquals(runs, m.openGroupRuns(biggest));
		assertEquals(runs, m.result().rows.size());
	}
}
