package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetStore;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.GroupRow;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryEngine;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ReplayFloor;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.SeedFloors;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ViewDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

//Reports: built from the real UIMSD data, rendered to four formats, with the marks, the legend and the canonical provenance intact.
public class ReportTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Ruleset RULES = Ruleset.of(30, 0);
	private static final long NOW = 1_800_000_000_000L;

	private Catalog cat;

	@Before
	public void setUp() throws Exception {
		HeadlessTestSupport.start();
		File root = DemoData.sampleRootFor(new File(tmp.newFolder("data"), "apd"));
		DemoGenerator.generateSample(root, RULES, NOW);
		cat = new DatasetStore(root).load();
	}

	private static String all( Report r ){
		return ReportRenderer.markdown(r) + ReportRenderer.text(r) + ReportRenderer.html(r) + ReportRenderer.csv(r);
	}

	private Row aFinishedRun( boolean wantFinished ){
		for (Row r : cat.dataset(Datasets.RUNS).rows) if (r.finished == wantFinished) return r;
		return null;
	}

	@Test
	public void lifetimeReportHasTheHomeNumbers() {
		Report r = ReportBuilder.lifetime(cat, RULES.id, "", true, NOW, "test");
		String md = ReportRenderer.markdown(r);
		assertTrue(md, md.contains("# UIMSD summary"));
		assertTrue(md.contains("Finished runs"));
		assertTrue(md.contains("Win rate"));
		assertFalse("a recorded-only report must not carry the canonical notice", md.contains("CANONICAL DATA"));
		assertFalse(r.hasCanonical());
	}

	@Test
	public void everyStateKeepsItsMarkAndTheLegendNamesOnlyTheMarksUsed() {
		Report r = new Report("Marks", NOW, "test");
		Report.Table t = new Report.Table("T", Report.Provenance.RECORDED);
		t.headers.add("a");
		t.headers.add("b");
		t.coverage.add("");
		t.coverage.add("1/2");
		t.rows.add(new Report.Cell[]{ new Report.Cell("12", CellText.EXACT), new Report.Cell(">=12", CellText.LOWER) });
		t.rows.add(new Report.Cell[]{ new Report.Cell("~6", CellText.INFERRED), new Report.Cell("?", CellText.UNKNOWN) });
		r.add(t);
		for (String out : new String[]{ ReportRenderer.markdown(r), ReportRenderer.text(r), ReportRenderer.html(r).replace("&gt;", ">"), ReportRenderer.csv(r) }){
			assertTrue(out, out.contains(">=12") && out.contains("~6") && out.contains("?"));
		}
		String md = ReportRenderer.markdown(r);
		assertTrue(md.contains("at least this much"));
		assertTrue(md.contains("inferred or canonical"));
		assertTrue(md.contains("unknown: never recorded"));
		assertFalse("hidden was not used, so it is not in the legend", md.contains("hidden information of a run that is still going"));
		assertFalse(md.contains("taken from an imported ranking record"));
	}

	@Test
	public void runReportShowsThePublicRunAndHidesSecretsOfUnfinishedRuns() {
		Row finished = aFinishedRun(true);
		assertNotNull(finished);
		Report plain = ReportBuilder.run(cat, finished, false, RULES.id, NOW, "test");
		String plainText = ReportRenderer.markdown(plain);
		assertTrue(plainText.contains("## The run"));
		assertFalse("hidden information was not asked for", plainText.contains("\nSeed") || plainText.contains("- Seed:"));
		Report hidden = ReportBuilder.run(cat, finished, true, RULES.id, NOW, "test");
		String hiddenText = ReportRenderer.markdown(hidden);
		assertTrue(hiddenText.contains("- Seed:"));
		assertTrue(hiddenText.contains("Spoilers"));

		//a run that is still going never gives its secrets, even when they are asked for
		boolean was = finished.finished;
		finished.finished = false;
		try {
			Report unfinished = ReportBuilder.run(cat, finished, true, RULES.id, NOW, "test");
			String t = ReportRenderer.markdown(unfinished);
			assertFalse(t.contains("- Seed:"));
			assertTrue(t.contains("not finished"));
		} finally {
			finished.finished = was;
		}
	}

	@Test
	public void viewGroupAndComparisonReportsUseTheTablesOwnNumbers() {
		ViewDef chars = cat.view("characters");
		QueryResult res = QueryEngine.run(cat.datasetOf(chars), chars.initialQuery().withScope(chars.initialQuery().scope.withRules(
				com.shatteredpixel.shatteredpixeldungeon.apd.query.Scope.Rules.ALL, "")), RULES.id);
		assertFalse(res.groups.isEmpty());
		Report view = ReportBuilder.view(res, "Characters", "", NOW, "test");
		Report.Table t = view.firstTable();
		assertNotNull(t);
		assertEquals(res.groups.size(), t.rows.size());
		GroupRow g = res.groups.get(0);
		String groupText = ReportRenderer.text(ReportBuilder.group(res, g, "", NOW, "test"));
		assertTrue(groupText, groupText.contains("Runs") || groupText.length() > 100);

		//two groups of the same table side by side (two characters)
		if (res.groups.size() >= 2){
			Report gc = ReportBuilder.compareGroups(res, res.groups.get(0), res.groups.get(1), "", NOW, "test");
			Report.Table gt = gc.firstTable();
			assertEquals(3, gt.headers.size());
			assertTrue(gt.rows.size() > 3);
			assertTrue(ReportRenderer.markdown(gc).contains("A: " + ReportBuilder.groupName(res, res.groups.get(0))));
			assertTrue(gt.headers.get(2).startsWith("B: "));
		}

		Row a = null, b = null;
		for (Row r : cat.dataset(Datasets.RUNS).rows){
			if (!r.finished) continue;
			if (a == null) a = r;
			else if (b == null){
				b = r;
				break;
			}
		}
		assertNotNull(b);
		Report cmp = ReportBuilder.compareRuns(cat, a, b, false, NOW, "test");
		assertEquals(4, cmp.firstTable().headers.size());
		assertTrue(ReportRenderer.markdown(cmp).contains("B minus A"));
	}

	@Test
	public void aComparisonWithAnUnfinishedRunNeverCarriesHiddenInformationOfEitherRun() {
		Row fin = null, other = null;
		for (Row r : cat.dataset(Datasets.RUNS).rows){
			if (!r.finished) continue;
			if (fin == null) fin = r;
			else if (other == null) other = r;
		}
		assertNotNull(other);
		com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetDef d = cat.dataset(Datasets.RUNS).def;
		//the baseline: two finished runs with hidden information asked for DO show it
		String both = ReportRenderer.markdown(ReportBuilder.compareRuns(cat, fin, other, true, NOW, "test"));
		assertTrue(both.contains("Seed"));
		String seedA = String.valueOf(fin.num(d.indexOf(com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.SEED)));

		boolean was = other.finished;
		other.finished = false;
		try {
			for (boolean unfinishedIsB : new boolean[]{ true, false }){
				Report r = unfinishedIsB ? ReportBuilder.compareRuns(cat, fin, other, true, NOW, "test")
						: ReportBuilder.compareRuns(cat, other, fin, true, NOW, "test");
				String t = all(r);
				assertFalse("no internal column when one run is unfinished", t.contains("Seed"));
				assertFalse("the finished run's seed value stays out too", t.contains(seedA));
				assertFalse(t.contains("Spoilers"));
				assertTrue(t.contains("(not finished)"));
			}
		} finally {
			other.finished = was;
		}
	}

	@Test
	public void reportsFromDemoOrSampleDataSayTheyAreSyntheticInEveryFormatAndRealDataDoesNot() {
		assertTrue("the sample folder is synthetic", cat.synthetic);
		Report r = ReportBuilder.lifetime(cat, RULES.id, "", true, NOW, "test");
		assertFalse(r.synthetic);
		r.synthetic = true;      //what the report window sets for a synthetic catalog
		for (String out : new String[]{ ReportRenderer.markdown(r), ReportRenderer.text(r), ReportRenderer.html(r), ReportRenderer.csv(r) }){
			assertTrue(out, out.contains("SYNTHETIC DEMO DATA") && out.contains("NOT real play history"));
		}
		r.synthetic = false;
		assertFalse(all(r).contains("SYNTHETIC"));
		assertFalse("a catalog that was not built from a demo folder is not synthetic", new Catalog().synthetic);
	}

	private static SeedFloors.Spec spec(){
		SeedFloors.Spec s = new SeedFloors.Spec();
		s.runId = "run-x";
		s.heroClass = "WARRIOR";
		s.reached = 3;
		s.ruleset = RULES.id;
		return s;
	}

	private static ReplayFloor floor( int depth, String... classes ){
		ReplayFloor f = new ReplayFloor();
		f.depth = depth;
		for (String c : classes){
			ReplayFloor.Item i = new ReplayFloor.Item();
			i.trueClass = c;
			i.container = "HEAP";
			f.items.add(i);
		}
		ReplayFloor.Mob m = new ReplayFloor.Mob();
		m.mobClass = "actors.mobs.Rat";
		f.mobs.add(m);
		return f;
	}

	@Test
	public void aSeedComparisonTableIsNotLabelledRecordedOnly() {
		List<ReplayFloor> canonical = new ArrayList<>();
		for (int d = 1; d <= 3; d++) canonical.add(floor(d, "items.food.Food"));
		List<SeedFloors.Recorded> recorded = new ArrayList<>();
		SeedFloors.Recorded x = new SeedFloors.Recorded();
		x.floor = 1;
		x.trueClass = "items.food.Food";
		recorded.add(x);
		Catalog c = new Catalog();
		SeedFloors.install(c, SeedFloors.assemble(spec(), recorded, canonical));
		com.shatteredpixel.shatteredpixeldungeon.apd.query.ViewDef v = c.view(SeedFloors.COMPARE);
		QueryResult res = QueryEngine.run(c.dataset(SeedFloors.COMPARE), v.initialQuery(), RULES.id);
		assertFalse(res.rows.isEmpty());
		assertEquals(Report.Provenance.MIXED, ReportBuilder.provenanceOf(res));
	}

	@Test
	public void seedFloorsReportKeepsRecordedAndCanonicalApartEverywhere() {
		List<ReplayFloor> canonical = new ArrayList<>();
		for (int d = 1; d <= 6; d++) canonical.add(floor(d, "items.food.Food", "items.potions.PotionOfHealing"));
		List<SeedFloors.Recorded> recorded = new ArrayList<>();
		SeedFloors.Recorded x = new SeedFloors.Recorded();
		x.floor = 1;
		x.trueClass = "items.food.Food";
		recorded.add(x);
		Catalog c = new Catalog();
		SeedFloors.install(c, SeedFloors.assemble(spec(), recorded, canonical));
		Report r = ReportBuilder.seedFloors(c, "run-x", RULES.id, NOW, "test");
		assertTrue(r.hasCanonical());
		for (String out : new String[]{ ReportRenderer.markdown(r), ReportRenderer.text(r), ReportRenderer.html(r), ReportRenderer.csv(r) }){
			assertTrue("canonical notice missing", out.contains("CANONICAL DATA"));
			assertTrue("not the exact counterfactual", out.contains("NOT what this run would have met"));
		}
		String md = ReportRenderer.markdown(r);
		assertTrue(md.contains("Canonical (run never got here)"));
		assertTrue(md.contains("Recorded (the run's own log)"));
		assertTrue(md.contains("RECORDED and CANONICAL entries"));
		//canonical values carry the ~ mark in every format, recorded ones do not
		Report.Table items = r.firstTable();
		int basisCol = items.headers.indexOf("Basis"), itemCol = items.headers.indexOf("Item");
		assertTrue(basisCol >= 0 && itemCol >= 0);
		int recordedRows = 0, canonicalRows = 0;
		for (Report.Cell[] row : items.rows){
			boolean isRecorded = row[basisCol].text.startsWith("Recorded");
			if (isRecorded){
				recordedRows++;
				assertEquals(CellText.EXACT, row[itemCol].state);
			} else {
				canonicalRows++;
				assertEquals("canonical values are marked", CellText.INFERRED, row[itemCol].state);
				assertTrue(row[itemCol].text.startsWith("~"));
			}
		}
		assertEquals(1, recordedRows);
		assertTrue(canonicalRows > 5);
		assertTrue(ReportRenderer.csv(r).contains("CANONICAL"));
	}

	@Test
	public void renderersEscapeAndQuoteSafely() {
		Report r = new Report("<b>x</b> & \"y\"", NOW, "t");
		Report.Table t = new Report.Table("T", Report.Provenance.RECORDED);
		t.headers.add("h");
		t.coverage.add("");
		t.rows.add(new Report.Cell[]{ new Report.Cell("<script>alert(1)</script> & \"q\"", CellText.EXACT) });
		t.rows.add(new Report.Cell[]{ new Report.Cell("=HYPERLINK(\"http://x\")", CellText.EXACT) });
		t.rows.add(new Report.Cell[]{ new Report.Cell("\t=1+1", CellText.EXACT) });
		t.rows.add(new Report.Cell[]{ new Report.Cell("  @SUM(1,1)", CellText.EXACT) });
		t.rows.add(new Report.Cell[]{ new Report.Cell("a,b\nc", CellText.EXACT) });
		t.rows.add(new Report.Cell[]{ new Report.Cell("-5", CellText.EXACT) });
		t.rows.add(new Report.Cell[]{ new Report.Cell("  -5", CellText.EXACT) });
		t.rows.add(new Report.Cell[]{ new Report.Cell("a|b", CellText.EXACT) });
		r.add(t);
		String html = ReportRenderer.html(r);
		assertFalse(html.contains("<script>"));
		assertTrue(html.contains("&lt;script&gt;"));
		assertTrue(html.contains("&quot;q&quot;"));
		String csv = ReportRenderer.csv(r);
		assertTrue("formula cell must be neutralised", csv.contains("'=HYPERLINK"));
		assertTrue("tab-prefixed formula must be neutralised", csv.contains("'\t=1+1"));
		assertTrue("space-prefixed formula must be neutralised", csv.contains("'  @SUM(1,1)"));
		assertTrue(csv.contains("\"a,b\nc\""));
		assertTrue("plain negative numbers stay numbers", csv.contains("\r\n-5\r\n"));
		assertTrue("space-prefixed negative numbers stay numbers", csv.contains("\r\n  -5\r\n"));
		assertTrue(ReportRenderer.markdown(r).contains("a\\|b"));
		//deterministic
		assertEquals(ReportRenderer.markdown(r), ReportRenderer.markdown(r));
		assertEquals(html, ReportRenderer.html(r));
	}

	@Test
	public void saveGoesThroughTheFilePickerAndSharingFallsBackCleanly() throws Exception {
		final String[] seen = new String[2];
		final ByteArrayOutputStream sink = new ByteArrayOutputStream();
		ApdFiles.provider = new ApdFiles.Provider(){
			@Override
			public void chooseExport( String suggestedName, ApdFiles.ExportTarget target ){
				seen[0] = suggestedName;
				seen[1] = "(no mime)";
				target.onStream(sink, "chosen.md");
			}

			@Override
			public void chooseExport( String suggestedName, String mime, ApdFiles.ExportTarget target ){
				seen[0] = suggestedName;
				seen[1] = mime;
				target.onStream(sink, "chosen");
			}

			@Override
			public void chooseImport( ApdFiles.ImportSource source ){}

			@Override
			public String where(){
				return "x";
			}
		};
		try {
			Report r = ReportBuilder.lifetime(cat, RULES.id, "", true, NOW, "test");
			final String[] message = new String[1];
			ReportExport.Done done = new ReportExport.Done(){
				@Override
				public void finished( String m ){
					synchronized (message){
						message[0] = m;
						message.notifyAll();
					}
				}
			};
			ReportExport.save(tmp.getRoot(), r, ReportExport.Format.HTML, done);
			synchronized (message){
				long end = System.currentTimeMillis() + 5000;
				while (message[0] == null && System.currentTimeMillis() < end) message.wait(100);
			}
			assertEquals("text/html", seen[1]);
			assertTrue(seen[0], seen[0].startsWith("apd-uimsd-summary-") && seen[0].endsWith(".html"));
			assertTrue(message[0], message[0].startsWith("Saved"));
			String written = new String(sink.toByteArray(), Charset.forName("UTF-8"));
			assertEquals(ReportRenderer.html(r), written);
			//no share sheet on this platform (the default provider method): a clear message, never a crash
			message[0] = null;
			ReportExport.share(tmp.getRoot(), r, done);
			assertTrue(message[0], message[0].contains("Save the report as a file"));
		} finally {
			ApdFiles.provider = null;
		}
	}

	@Test
	public void folderFallbackWritesTheFile() throws Exception {
		ApdFiles.provider = null;
		Report r = ReportBuilder.lifetime(cat, RULES.id, "", true, NOW, "test");
		final String[] message = new String[1];
		ReportExport.save(tmp.getRoot(), r, ReportExport.Format.MARKDOWN, new ReportExport.Done(){
			@Override
			public void finished( String m ){
				synchronized (message){
					message[0] = m;
					message.notifyAll();
				}
			}
		});
		synchronized (message){
			long end = System.currentTimeMillis() + 5000;
			while (message[0] == null && System.currentTimeMillis() < end) message.wait(100);
		}
		File[] files = new File(tmp.getRoot(), "exports").listFiles();
		assertNotNull(message[0], files);
		assertEquals(1, files.length);
		assertEquals(ReportRenderer.markdown(r), new String(Files.readAllBytes(files[0].toPath()), Charset.forName("UTF-8")));
	}

	@Test
	public void theReportPackageStaysFreeOfGameCode() throws Exception {
		File dir = new File("src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/report");
		if (!dir.isDirectory()) return;
		for (File f : dir.listFiles()){
			for (String line : Files.readAllLines(f.toPath())){
				if (!line.startsWith("import com.shatteredpixel.shatteredpixeldungeon.")) continue;
				assertTrue(f.getName() + ": " + line, line.contains(".apd."));
			}
		}
	}
}
