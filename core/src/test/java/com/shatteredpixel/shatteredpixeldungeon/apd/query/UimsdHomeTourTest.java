package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

//The Home page, the guide topics and the interactive tour, on the generated sample history.
public class UimsdHomeTourTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Ruleset RULES = Ruleset.of(30, 0);

	private File realRoot, sampleRoot;
	private Catalog cat;

	@Before
	public void setUp() throws Exception {
		realRoot = new File(tmp.newFolder("data"), "apd");
		sampleRoot = DemoData.sampleRootFor(realRoot);
		DemoGenerator.generateSample(sampleRoot, RULES, 1_800_000_000_000L);
		cat = new DatasetStore(sampleRoot).load();
	}

	@After
	public void tearDown(){
		ApdRecorder.reset();
	}

	private UimsdModel model() {
		UimsdModel m = new UimsdModel(RULES.id, "sample");
		m.setCatalog(cat);
		return m;
	}

	@Test
	public void sampleIsMarkedAndSmall() throws Exception {
		assertTrue(sampleRoot.getName().equals(DemoData.SAMPLE_DIR_NAME));
		assertEquals(DemoGenerator.SAMPLE_RUNS, cat.dataset(Datasets.RUNS).rows.size());
		DemoData.delete(sampleRoot);
		assertFalse(sampleRoot.exists());
	}

	@Test
	public void homeShowsTheMainCards() {
		UimsdHome h = UimsdHome.build(cat, RULES.id, true);
		assertFalse(h.isEmpty());
		assertTrue(h.runs > 0);
		for (String id : new String[]{ "runs", "ask", "characters", "gear", "enemies", "recent", "records" }) assertNotNull(id, h.card(id));
		assertFalse(h.text().isEmpty());
		//every link goes to a view that exists
		for (UimsdHome.Card c : h.cards) for (UimsdHome.Link l : c.links){
			if (UimsdHome.ASK_LINK.equals(l.viewId)) continue;   //opens the question builder, not a table
			assertNotNull(l.viewId, cat.view(l.viewId));
		}
	}

	@Test
	public void broadViewsOverSeveralRulesetsSayMixedAndNarrowOnesDoNot() throws Exception {
		//all rules over a history that was played under several: the page, the cards and the report say so
		UimsdHome all = UimsdHome.build(cat, RULES.id, true);
		assertTrue(all.mixedRulesets);
		assertTrue(all.scopeNote, all.scopeNote.startsWith("MIXED RULES"));
		assertTrue(all.scopeNote.contains("not directly comparable"));
		assertTrue(all.card("records").title, all.card("records").title.endsWith("(mixed rules)"));
		assertTrue(all.text().contains("MIXED RULES"));
		String report = com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportRenderer.markdown(
				com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportBuilder.lifetime(cat, RULES.id, "", true, 1_800_000_000_000L, "test"));
		assertTrue(report.contains("MIXED RULES"));

		//only the player's own rules: nothing to compare across, no claim of mixing
		UimsdHome mine = UimsdHome.build(cat, RULES.id, false);
		assertFalse(mine.mixedRulesets);
		assertFalse(mine.scopeNote.contains("MIXED"));
		assertFalse(mine.card("records").title.contains("mixed"));

		//a table over all rulesets says it; the default table does not, and a table narrowed to one ruleset does not
		UimsdModel m = model();
		m.selectView("runs");
		StringBuilder before = new StringBuilder();
		for (StatusText.Line l : StatusText.lines(m.result(), "Runs", "")) before.append(l.text).append('\n');
		assertFalse(before.toString(), before.toString().contains("MIXED RULES"));
		m.setScope(m.query().scope.withRules(Scope.Rules.ALL, ""));
		StringBuilder broad = new StringBuilder();
		for (StatusText.Line l : StatusText.lines(m.result(), "Runs", "")) broad.append(l.text).append('\n');
		assertTrue(broad.toString(), broad.toString().contains("MIXED RULES"));
		m.setScope(m.query().scope.withRules(Scope.Rules.SPECIFIC, RULES.id));
		StringBuilder one = new StringBuilder();
		for (StatusText.Line l : StatusText.lines(m.result(), "Runs", "")) one.append(l.text).append('\n');
		assertFalse("narrowing to one ruleset is the way to compare like with like", one.toString().contains("MIXED RULES"));
	}

	@Test
	public void aHistoryPlayedUnderOneRulesetIsNeverCalledMixed() throws Exception {
		File root = tmp.newFolder("one");
		for (int i = 0; i < 3; i++){
			new SyntheticLog(root, "l" + i, com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion.CURRENT, 100L * (i + 1))
					.start("run-" + i, 0, "WARRIOR", RULES.id, 0, 30, false).enter(1)
					.end(com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		}
		Catalog one = new DatasetStore(root).load();
		UimsdHome h = UimsdHome.build(one, RULES.id, true);
		assertEquals(3, h.runs);
		assertFalse(h.mixedRulesets);
		assertFalse(h.scopeNote.contains("MIXED"));
	}

	@Test
	public void theGuideIsInSectionsHasAGlossaryAndEveryTableIsListedUnderASection() {
		java.util.Set<String> ids = new java.util.HashSet<>();
		java.util.List<String> sections = new ArrayList<>();
		for (UimsdGuide.Topic t : UimsdGuide.topics()){
			assertTrue("duplicate topic " + t.id, ids.add(t.id));
			assertFalse(t.id + " has no section", t.section.isEmpty());
			if (sections.isEmpty() || !sections.get(sections.size() - 1).equals(t.section)){
				assertFalse("section " + t.section + " is split in two", sections.contains(t.section));
				sections.add(t.section);
			}
		}
		assertTrue(ids.contains("glossary") && ids.contains("options") && ids.contains("marks"));
		String glossary = UimsdGuide.topic("glossary").text;
		for (String term : new String[]{ "Canonical", "Unknown", "At least", "Worked out", "Rules", "Hidden", "Recorded", "Known" }) assertTrue(term, glossary.contains(term));
		for (ViewDef v : cat.views()) assertFalse("table " + v.id + " is not listed under a section", v.group.isEmpty());
	}

	@Test
	public void homeWithNoRunsIsEmptyNotBroken() {
		Catalog empty = new DatasetStore(new File(tmp.getRoot(), "nothing")).load();
		UimsdHome h = UimsdHome.build(empty, RULES.id, false);
		assertTrue(h.isEmpty());
	}

	@Test
	public void emptyHomeSeparatesRulesFromRunStatus() throws Exception {
		Catalog empty = new DatasetStore(new File(tmp.getRoot(), "nothing-else")).load();
		UimsdHome none = UimsdHome.build(empty, RULES.id, false);
		assertTrue(none.emptyMessage().contains("No finished runs yet"));
		assertTrue(none.emptyMessage().contains("Tables > Options > Include runs still going"));

		File root = tmp.newFolder("other-rules");
		Ruleset other = Ruleset.of(40, 0);
		new SyntheticLog(root, "other", com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion.CURRENT, 100L)
				.start("run-other", 0, "WARRIOR", other.id, 0, 30, false).enter(1)
				.end(com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.OUTCOME_ABANDONED, "", 1, 1, 1).close(true);
		UimsdHome current = UimsdHome.build(new DatasetStore(root).load(), RULES.id, false);
		assertTrue(current.isEmpty());
		assertEquals(1, current.runsElsewhere);
		assertTrue(current.emptyMessage(), current.emptyMessage().contains("1 finished run played under other rules"));
		assertTrue(current.emptyMessage(), current.emptyMessage().contains("Runs still going are available"));
		assertFalse("all-rules Home contains the finished run", UimsdHome.build(new DatasetStore(root).load(), RULES.id, true).isEmpty());
	}

	@Test
	public void guideTopicsPointAtRealTourSteps() {
		assertTrue(UimsdGuide.topics().size() >= 10);
		Set<String> ids = new HashSet<>();
		for (UimsdGuide.Topic t : UimsdGuide.topics()){
			assertTrue(ids.add(t.id));
			//a topic is either shown on the real screen by a tour step, or has none ("")
			assertTrue(t.id, t.tourStep.isEmpty() || UimsdTutorial.indexOf(t.tourStep) >= 0);
			assertFalse(t.text.isEmpty());
		}
		String marks = UimsdGuide.topic("marks").text;
		for (String s : new String[]{ "?", ">=", "~" }) assertTrue(s, marks.contains(s));
	}

	@Test
	public void guideCoversTheNewerFeaturesAndTheMarksTheyUse() {
		for (String id : new String[]{ "reports", "seedfloors", "difficulty" }){
			UimsdGuide.Topic t = UimsdGuide.topic(id);
			assertNotNull(id, t);
			assertTrue(id, t.text.length() > 200);
		}
		assertTrue(UimsdGuide.topic("seedfloors").text.contains("CANONICAL"));
		assertTrue(UimsdGuide.topic("seedfloors").text.contains("not what the run would have met"));
		assertTrue(UimsdGuide.topic("difficulty").text.contains("?"));
		assertTrue(UimsdGuide.topic("reports").text.contains("hidden information"));
		assertTrue(UimsdTutorial.indexOf("reports") > UimsdTutorial.indexOf("coverage"));
		assertTrue(UimsdTutorial.indexOf("reports") < UimsdTutorial.indexOf("home"));
	}


	@Test
	public void guideNamesEverySupportedQuestionMeasure() {
		String ask = UimsdGuide.topic("ask").text.toLowerCase(java.util.Locale.ROOT);
		for (String measure : new String[]{ "how many", "total", "average", "highest", "lowest", "median" })
			assertTrue(measure, ask.contains(measure));
	}

	@Test
	public void homeShowsTheDifficultyCardWhenThereIsSomethingToCompare() {
		UimsdHome h = UimsdHome.build(cat, RULES.id, false);
		UimsdHome.Card c = h.card("difficulty");
		assertNotNull("the sample history has several difficulties", c);
		assertTrue(c.lines.size() >= 2);
		assertEquals("difficulty", c.links.get(0).viewId);
		boolean unknownGroup = false;
		for (UimsdHome.Line l : c.lines) if (l.label.startsWith("Not recorded")) unknownGroup = true;
		assertTrue("older runs are labelled not recorded, never as a level", unknownGroup);
		assertTrue(h.scopeNote, h.scopeNote.isEmpty() || !h.scopeNote.contains("balance revision"));
	}

	@Test
	public void tourRunsOnItsOwnWhenThePlayerDoesTheThings() {
		UimsdModel m = model();
		UimsdTutorial.prepare(m, 0);
		UimsdTutorial t = new UimsdTutorial(m, 0);
		//next through the intro steps
		while (t.step().expect == UimsdTutorial.Expect.NEXT) t.next();
		assertEquals(UimsdTutorial.Expect.VIEW, t.step().expect);
		assertFalse(t.check());
		m.showRoot("weapons");
		assertTrue(t.check());
		while (t.step().expect == UimsdTutorial.Expect.NEXT) t.next();
		assertEquals(UimsdTutorial.Expect.DRILL, t.step().expect);
		m.openGroupRows(m.result().groups.get(0));
		assertTrue(t.check());
		while (t.step().expect == UimsdTutorial.Expect.NEXT) t.next();
		assertEquals(UimsdTutorial.Expect.BACK, t.step().expect);
		assertTrue(m.back());
		assertTrue(t.check());
		//run the rest by skipping: it finishes
		int guard = 0;
		while (!t.isFinished() && guard++ < 100) t.next();
		assertTrue(t.isFinished());
	}

	@Test
	public void startingInTheMiddlePreparesTheScreen() {
		int deeper = UimsdTutorial.indexOf("deeper");
		UimsdModel m = model();
		UimsdTutorial.prepare(m, deeper);
		assertEquals("weapons", m.viewId());
		assertTrue(m.depth() > 0);
		UimsdTutorial t = new UimsdTutorial(m, deeper);
		assertEquals("deeper", t.step().id);
		UimsdModel m2 = model();
		UimsdTutorial.prepare(m2, UimsdTutorial.indexOf("filter"));
		assertEquals("weapons", m2.viewId());
		assertEquals(0, m2.depth());
	}

	@Test
	public void columnsLessonUngroupsTheTourInsteadOfOpeningTheGroupedTableTooltip() {
		UimsdModel player = model();
		player.showRoot("weapons");
		Query playerQuery = player.query();
		assertTrue("Weapons starts as a grouped summary", player.result().isGrouped());

		UimsdModel tourModel = model();
		tourModel.showRoot("weapons");
		UimsdTutorial tour = new UimsdTutorial(tourModel, UimsdTutorial.indexOf("sort"));
		assertTrue(tourModel.result().isGrouped());
		tour.next(); // The sort lesson advances into Columns, as in the player report.
		assertEquals("columns", tour.step().id);
		assertEquals("weapons", tourModel.viewId());
		assertFalse("Columns must be an editable ungrouped table", tourModel.result().isGrouped());
		assertFalse("Ungrouping must not accidentally finish the lesson", tour.check());

		String extra = null;
		for (ColumnDef c : tourModel.offeredColumns()) {
			if (!tourModel.query().columns.contains(c.id)) { extra = c.id; break; }
		}
		assertNotNull("A column can be enabled from this lesson", extra);
		tourModel.toggleColumn(extra);
		assertTrue("A real column selection completes step 10", tour.check());
		assertEquals("marks", tour.step().id);

		// The Guide can also jump directly into the Columns lesson.
		UimsdModel fromGuide = model();
		UimsdTutorial.prepare(fromGuide, UimsdTutorial.indexOf("columns"));
		assertEquals("weapons", fromGuide.viewId());
		assertFalse("Guide jumps must not hit the grouped-only explanation", fromGuide.result().isGrouped());

		// Neither tour route changes the separate player's view or underlying history.
		assertEquals(playerQuery, player.query());
		assertTrue(player.result().isGrouped());
		assertFalse(new File(realRoot, "x").exists());
	}

	@Test
	public void tourNeverChangesTheRealHistory() throws Exception {
		File before = new File(realRoot, "x");
		assertFalse(before.exists());
		assertFalse(new File(sampleRoot, "x").exists());
		//the sample folder is not the real folder, and only the marked folder may be deleted
		assertTrue(!sampleRoot.equals(realRoot));
		File fake = new File(tmp.newFolder("other"), DemoData.SAMPLE_DIR_NAME);
		assertTrue(fake.mkdirs());
		DemoData.delete(fake);
		assertTrue("unmarked folder must survive", fake.exists());
	}

	@Test
	public void panelPlacementKeepsOffTheTarget() {
		//no target: low position
		assertEquals(160f, GuideGeometry.placeTop(20, 200, 40, 0, 0, 0, 0, false), 0.001f);
		//target in the low area: panel goes up
		assertEquals(20f, GuideGeometry.placeTop(20, 200, 40, 10, 170, 50, 10, true), 0.001f);
		//target in the upper area: stays low
		assertEquals(160f, GuideGeometry.placeTop(20, 200, 40, 10, 30, 50, 10, true), 0.001f);
		//the panel is never taller than the room
		assertEquals(20f, GuideGeometry.placeTop(20, 60, 100, 0, 0, 0, 0, false), 0.001f);
		assertTrue(GuideGeometry.panelHeight(50, 8, 14, 200) <= 200 * 0.45f);
		assertTrue(GuideGeometry.panelHeight(0, 8, 14, 200) >= GuideGeometry.MIN_PANEL);
	}
}
