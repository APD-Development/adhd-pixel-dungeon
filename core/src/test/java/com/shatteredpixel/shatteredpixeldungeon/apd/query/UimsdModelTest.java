/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
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
package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static com.shatteredpixel.shatteredpixeldungeon.apd.query.QuerySupport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//The view model and the table it feeds, driven with a made-up dataset that is not one of the shipped ones: nothing in them may
//depend on Runs, Items or any other particular dataset.
public class UimsdModelTest {

    @Test public void gameplayReleaseDropsTablesAndBackResultsButKeepsQuery()throws Exception{
        UimsdModel m=new UimsdModel(R,"rules");m.setCatalog(catalog());m.selectView("parts");
        Query before=m.query();m.releaseForGameplay();
        assertNull(m.catalog());assertNull(m.result());org.junit.Assert.assertSame(before,m.query());assertEquals(UimsdModel.State.EMPTY,m.state());
        for(String name:new String[]{"preparedHome","preparedHomeCatalog","listener"}){
            java.lang.reflect.Field f=UimsdModel.class.getDeclaredField(name);f.setAccessible(true);assertNull(name,f.get(m));
        }
        java.lang.reflect.Field back=UimsdModel.class.getDeclaredField("back");back.setAccessible(true);
        java.util.List<?> places=(java.util.List<?>)back.get(m);assertEquals(1,places.size());
        for(Object place:places)for(String name:new String[]{"result","dataset"}){java.lang.reflect.Field f=place.getClass().getDeclaredField(name);f.setAccessible(true);assertNull(name,f.get(place));}
        m.setCatalog(catalog());assertEquals(UimsdModel.State.READY,m.state());assertNotNull(m.result());
    }
    @Test public void gameplayReleaseRetiresAnInFlightLoaderBeforeItCanPublish()throws Exception{
        UimsdModel m=new UimsdModel(R,"rules");java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),finish=new java.util.concurrent.CountDownLatch(1);
        java.io.File root=java.nio.file.Files.createTempDirectory("apd-retired-load").toFile();
        try{
            m.loadForOpen(root,false,()->{entered.countDown();try{finish.await(10,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException expected){}});
            assertTrue(entered.await(10,java.util.concurrent.TimeUnit.SECONDS));m.releaseForGameplay();finish.countDown();Thread.sleep(100);
            assertEquals(UimsdModel.State.EMPTY,m.state());assertNull(m.catalog());assertNull(m.result());
        }finally{finish.countDown();root.delete();}
    }
    @Test public void catalogWorkingMemoryIsExclusiveWithoutBlockingTheCaller()throws Exception{
        java.lang.reflect.Field field=UimsdModel.class.getDeclaredField("CATALOG_LOAD_MEMORY");field.setAccessible(true);Object memory=field.get(null);
        UimsdModel next=new UimsdModel(R,"next");java.util.concurrent.CountDownLatch interactive=new java.util.concurrent.CountDownLatch(1);
        java.io.File root=java.nio.file.Files.createTempDirectory("apd-load-memory-ownership").toFile();
        try{
            synchronized(memory){
                next.loadForOpen(root,false,interactive::countDown);
                assertTrue("interactive flush must not wait for another decoder",interactive.await(10,java.util.concurrent.TimeUnit.SECONDS));
                next.awaitLoad(200);
                assertEquals("catalog work cannot allocate while another batch owns memory",UimsdModel.State.LOADING,next.state());assertNull(next.catalog());
            }
            next.awaitLoad(10000);assertEquals(UimsdModel.State.READY,next.state());assertNotNull(next.catalog());
        }finally{next.releaseForGameplay();root.delete();}
    }
    @Test public void cancelledCatalogReadDoesNotTouchRecordedFiles()throws Exception{
        java.io.File root=java.nio.file.Files.createTempDirectory("apd-cancelled-catalog").toFile();java.io.File history=new java.io.File(root,"keep.txt");
        java.nio.file.Files.write(history.toPath(),new byte[]{1,2,3});
        try{try{new DatasetStore(root).load(()->true);org.junit.Assert.fail("cancelled load");}catch(java.util.concurrent.CancellationException expected){}assertEquals(3,history.length());}
        finally{history.delete();root.delete();}
    }

	private static final String R = "r-now";
	private final DatasetDef things = QuerySupport.def();

	//a second made-up dataset, linked from the first
	private DatasetDef parts(){
		DatasetDef d = new DatasetDef("parts", "Parts", "made up");
		d.add(ColumnDef.text("thing", "Thing").shown());
		d.add(ColumnDef.num("weight", "Weight").shown());
		d.add(ColumnDef.num("hiddenWeight", "Hidden weight").internal());
		return d;
	}

	private Catalog catalog(){
		List<Row> rows = new ArrayList<>();
		for (int i = 0; i < 120; i++){
			Row r = row(things, "t" + i, "thing-" + i, (long) (i % 17), (long) (i % 3), (long) i, i % 10 != 0, R);
			if (i % 10 == 0) r.finished = false;
			rows.add(r);
		}
		DatasetDef p = parts();
		List<Row> prows = new ArrayList<>();
		for (int i = 0; i < 6; i++){
			Row r = new Row("p" + i, null, "run-t" + (i % 2 == 0 ? "1" : "2"), p.size());
			r.finished = true;
			r.ruleset = R;
			r.setText(0, "thing-" + (i % 2 == 0 ? 1 : 2), Fact.PROV_RECORDED);
			r.setNum(1, 5 + i, Fact.PROV_RECORDED);
			r.setNum(2, 50 + i, Fact.PROV_RECORDED);
			prows.add(r);
		}
		things.link(new LinkDef("Its parts", "parts", "name", "thing"));
		Catalog c = new Catalog();
		c.add(dataset(things, rows));
		c.add(dataset(p, prows));
		c.add(new ViewDef("all", "All things", "every thing", Query.of("things").withScope(Scope.DEFAULT.withUnfinished(true))
				.withSorts(Arrays.asList(new Sort("score", true)))));
		c.add(new ViewDef("kinds", "By kind", "grouped", Query.of("things").withScope(Scope.DEFAULT.withUnfinished(true))
				.withGroup(new GroupBy(Arrays.asList("kind"), Arrays.asList(Aggregate.count(), Aggregate.of(Aggregate.Fn.AVG, "score"))))
				.withMinSample(5)));
		c.add(new ViewDef("parts", "Parts", "the parts", Query.of("parts")));
		return c;
	}

	private UimsdModel model(){
		UimsdModel m = new UimsdModel(R, "made up rules");
		m.setCatalog(catalog());
		return m;
	}

	@Test public void initialHomePreparedOnlyForTheLoadedCatalogAndScope() throws Exception {
		java.io.File root = java.nio.file.Files.createTempDirectory("apd-prepared-home").toFile();
		try {
			UimsdModel m = new UimsdModel(R, "");
			m.load(root, true);
			m.awaitLoad(10000);
			assertEquals(UimsdModel.State.READY, m.state());
			Catalog loaded = m.catalog();
			assertNotNull("initial Home summary is prepared with the loaded catalog",m.preparedHome(loaded, true));
			assertNull("a different scope cannot reuse the prepared summary",m.preparedHome(loaded, false));
			m.setCatalog(catalog());
			assertNull("replacing the catalog retires the previous summary",m.preparedHome(m.catalog(), true));
		} finally {
			assertTrue(root.delete());
		}
	}

	@Test public void columnLayoutLoadAndCatalogPublishShareTheSameGeneration() throws Exception {
		java.io.File root = java.nio.file.Files.createTempDirectory("apd-layout-generation").toFile();
		try {
			UimsdModel first = new UimsdModel(R, "");
			first.load(root);
			first.awaitLoad(10000);
			assertEquals(UimsdModel.State.READY, first.state());
			first.showRoot("runs");
			List<String> wanted = Arrays.asList(
					com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.SCORE,
					com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.HERO_CLASS);
			first.setColumns(wanted);
			UimsdModel reopened = new UimsdModel(R, "");
			reopened.load(root);
			reopened.awaitLoad(10000);
			assertEquals(UimsdModel.State.READY, reopened.state());
			reopened.showRoot("runs");
			assertEquals("saved layout must be available with the loaded catalog", wanted, reopened.query().columns);
		} finally {
			java.io.File layout = new java.io.File(root, UimsdModel.COLUMN_LAYOUTS_FILE);
			if (layout.exists()) assertTrue(layout.delete());
			if (root.exists()) assertTrue(root.delete());
		}
	}

	@Test public void columnsPersistWhenNoRunHasCreatedTheHistoryFolder() throws Exception {
		java.nio.file.Path parent = java.nio.file.Files.createTempDirectory("apd-first-layout");
		java.io.File root = parent.resolve("history").toFile();
		try {
			assertFalse(root.exists());
			UimsdModel first = new UimsdModel(R, "");
			first.load(root); first.awaitLoad(10000); first.showRoot("runs");
			assertEquals(UimsdModel.State.READY, first.state());
			List<String> columns = Arrays.asList(
					com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.SCORE,
					com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.HERO_CLASS);
			first.setColumns(columns);
			assertTrue("layout must be saved even without recorded runs", new java.io.File(root, UimsdModel.COLUMN_LAYOUTS_FILE).isFile());
			UimsdModel second = new UimsdModel(R, "");
			second.load(root); second.awaitLoad(10000); second.showRoot("runs");
			assertEquals(columns, second.query().columns);
			assertFalse("saving preferences creates no run history", new java.io.File(root, "runs").exists());
		} finally {
			java.io.File layout = new java.io.File(root, UimsdModel.COLUMN_LAYOUTS_FILE);
			if (layout.exists()) assertTrue(layout.delete());
			if (root.exists()) assertTrue(root.delete());
			assertTrue(parent.toFile().delete());
		}
	}


	@Test
	public void anOlderBackgroundLoadCannotOverwriteANewerExplicitCatalog() throws Exception {
		java.io.File root = java.nio.file.Files.createTempDirectory("apd-stale-load").toFile();
		UimsdModel m = new UimsdModel(R, "");
		Catalog newer = catalog();
		//Holding the monitor makes the old loader complete its disk work but wait before installation.
		//The newer explicit catalogue is therefore installed first, deterministically reproducing the stale overwrite.
		synchronized (m){
			m.load(root);
			m.setCatalog(newer);
			assertTrue(newer == m.catalog());
			assertEquals("all", m.viewId());
		}
		m.awaitLoad(10000);
		assertTrue("the stale loader must not replace the newer catalogue", newer == m.catalog());
		assertEquals(UimsdModel.State.READY, m.state());
		assertEquals("all", m.viewId());
	}


	@Test
	public void aReplayFromAnOlderCatalogCannotReplaceNewerReadyState() throws Exception {
		UimsdModel m = model();
		Row run = m.result().rows.get(0);
		java.util.concurrent.CountDownLatch building = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
		m.setSeedFloorsProvider(new SeedFloors.Provider(){
			@Override public boolean applies( Catalog cat, Dataset ds, Row row ){ return true; }
			@Override public SeedFloors.Built build( Catalog cat, Row row ){
				building.countDown();
				try {
					if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("release timeout");
				} catch (InterruptedException e){
					throw new AssertionError(e);
				}
				SeedFloors.Spec spec = new SeedFloors.Spec();
				spec.runId = "stale-run";
				spec.ruleset = R;
				return SeedFloors.assemble(spec, new ArrayList<SeedFloors.Recorded>(), new ArrayList<ReplayFloor>());
			}
		});
		java.util.concurrent.atomic.AtomicInteger changes = new java.util.concurrent.atomic.AtomicInteger();
		m.setListener(new UimsdModel.Listener(){
			@Override public void changed(){ changes.incrementAndGet(); }
		});
		m.startSeedFloors(run);
		assertTrue(building.await(5, java.util.concurrent.TimeUnit.SECONDS));
		Catalog newer = catalog();
		m.setCatalog(newer);
		release.countDown();
		m.awaitSeedFloors(5000);
		assertTrue("the stale replay must not replace the newer catalogue", newer == m.catalog());
		assertEquals(UimsdModel.State.READY, m.state());
		assertEquals("all", m.viewId());
		assertEquals("", m.notice());
		assertEquals("no stale replay notification", 2, changes.get());
	}

	@Test
	public void columnOrderAndSelectionSurviveOpeningAnotherView(){
		UimsdModel m = model();
		m.setColumns(Arrays.asList("kind", "name", "score"));
		m.selectView("parts");
		m.showRoot("all");
		assertEquals(Arrays.asList("kind", "name", "score"), m.query().columns);
		m.moveColumn("score", -2);
		m.showRoot("parts");
		m.showRoot("all");
		assertEquals(Arrays.asList("score", "kind", "name"), m.query().columns);
	}

	@Test
	public void hiddenOnlySavedColumnsCannotReopenAsABlankTable(){
		UimsdModel m = model();
		m.setShowInternal(true);
		m.setColumns(Arrays.asList("secret"));
		assertEquals(1, m.table().headers.size());

		m.setShowInternal(false);
		m.showRoot("parts");
		m.showRoot("all");

		assertEquals("public defaults replace a hidden-only layout", 4, m.table().headers.size());
		assertFalse(m.query().showInternal);
	}

	@Test
	public void retiredSavedColumnsCannotReopenAsABlankTable(){
		UimsdModel m = model();
		//A column that existed in an older build can remain in the local layout file.
		m.setColumns(Arrays.asList("removed-in-newer-build"));
		m.showRoot("parts");
		m.showRoot("all");

		assertEquals("current defaults replace a wholly stale layout", 4, m.table().headers.size());
	}

	@Test
	public void columnChoicesSurviveARecreatedModelAndResetIsExplicit() throws Exception {
		java.io.File root = java.nio.file.Files.createTempDirectory("apd-column-layout").toFile();
		UimsdModel first = new UimsdModel(R, "");
		first.load(root);
		first.awaitLoad(10000);
		first.showRoot("runs");
		java.util.List<String> columns = Arrays.asList(
				com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.SCORE,
				com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.HERO_CLASS,
				com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.SEED_TYPE,
				com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.EXTERNAL_MODS);
		first.setColumns(columns);
		assertTrue(new java.io.File(root, UimsdModel.COLUMN_LAYOUTS_FILE).isFile());
		UimsdModel second = new UimsdModel(R, "");
		second.load(root);
		second.awaitLoad(10000);
		second.showRoot("runs");
		assertEquals(columns, second.query().columns);
		second.resetView();
		UimsdModel third = new UimsdModel(R, "");
		third.load(root);
		third.awaitLoad(10000);
		third.showRoot("runs");
		assertEquals(third.view().initialQuery().columns, third.query().columns);
	}

	@Test
	public void theModelOpensTheFirstViewAndLaysOutAPage(){
		UimsdModel m = model();
		assertEquals(UimsdModel.State.READY, m.state());
		assertEquals("all", m.viewId());
		ResultTable t = m.table();
		assertEquals(120, t.total);
		assertEquals(50, t.lines.size());
		assertEquals(3, t.pageCount());
		assertEquals("name, score, kind and depth are shown by default; secret and note are hidden", 4, t.headers.size());
	}

	@Test
	public void pagingNeverRunsOffTheEnd(){
		UimsdModel m = model();
		m.setPage(2);
		assertEquals(20, m.table().lines.size());
		m.setPage(99);
		assertEquals(2, m.page());
		m.setPage(0);
		assertEquals(50, m.table().lines.size());
		assertEquals(0, m.table().pageStart);
	}

	@Test
	public void headerClicksCycleTheSortAndTheHeaderSaysSo(){
		UimsdModel m = model();
		ResultTable t = m.table();
		ResultTable.Header score = null;
		for (ResultTable.Header h : t.headers) if (h.id.equals("score")) score = h;
		assertEquals(-1, score.sortDirection);   //the view starts sorted by score, descending
		assertEquals(1, score.sortRank);
		m.cycleSort("score");                    //descending -> off
		for (ResultTable.Header h : m.table().headers) if (h.id.equals("score")) assertEquals(0, h.sortDirection);
		m.cycleSort("name");
		m.cycleSort("name");
		for (ResultTable.Header h : m.table().headers) if (h.id.equals("name")) assertEquals(-1, h.sortDirection);
	}

	@Test
	public void filtersCanBeAddedEditedAndRemovedAndTheStatusSaysWhatTheyCost(){
		UimsdModel m = model();
		m.addFilter(Filter.num("score", Op.GE, 10));
		int matching = m.result().stats.matching;
		assertTrue(matching < 120 && matching > 0);
		m.addFilter(Filter.num("kind", Op.EQ, 1));
		assertTrue(m.result().stats.matching < matching);
		m.replaceFilter(0, Filter.num("score", Op.GE, 0));
		assertEquals(2, m.query().filters.size());
		m.removeFilter(1);
		assertEquals(1, m.query().filters.size());
		m.clearFilters();
		assertEquals(120, m.result().stats.matching);
	}

	@Test
	public void columnsCanBeToggledButNeverEmptied(){
		UimsdModel m = model();
		int before = m.table().headers.size();
		m.toggleColumn("depth");
		assertEquals(before + (m.query().columns.contains("depth") ? 1 : -1), m.table().headers.size());
		List<String> cols = new ArrayList<>(m.query().columns);
		for (String c : cols) m.toggleColumn(c);
		assertTrue(m.table().headers.size() >= 1);
	}

	@Test
	public void hiddenColumnsAreOnlyOfferedWhenHiddenInformationIsOnAndGoWhenItIsTurnedOff(){
		UimsdModel m = model();
		for (ColumnDef c : m.offeredColumns()) assertFalse(c.id, c.isInternal());
		m.setShowInternal(true);
		boolean sawSecret = false;
		for (ColumnDef c : m.offeredColumns()) if (c.id.equals("secret")) sawSecret = true;
		assertTrue(sawSecret);
		m.addFilter(Filter.num("secret", Op.GE, 3));
		m.setSorts(Arrays.asList(new Sort("secret", false), new Sort("name", false)));
		m.toggleColumn("secret");
		m.setShowInternal(false);
		assertTrue("filters on hidden columns are removed with them", m.query().filters.isEmpty());
		assertEquals(Arrays.asList(new Sort("name", false)), m.query().sorts);
		assertFalse(m.query().columns.contains("secret"));
		assertTrue(m.result().problems.isEmpty());
	}

	@Test
	public void disablingHiddenInformationRemovesEveryHiddenAggregateDependency(){
		UimsdModel m = model();
		m.setShowInternal(true);
		Aggregate safe = Aggregate.of(Aggregate.Fn.AVG, "score");
		Aggregate ratio = Aggregate.ratio("score", "secret");
		Aggregate conditioned = Aggregate.of(Aggregate.Fn.SUM, "score").where("secret", 2);
		Aggregate distinct = Aggregate.of(Aggregate.Fn.AVG, "score").perDistinct("secret");
		m.setGroup(new GroupBy(Arrays.asList("kind"), Arrays.asList(safe, ratio, conditioned, distinct)));
		assertEquals(4, m.query().group.aggregates.size());
		assertTrue(m.result().problems.isEmpty());

		m.setShowInternal(false);
		assertEquals(Arrays.asList(safe), m.query().group.aggregates);
		assertTrue("the cleaned query must not silently omit rejected aggregates", m.result().problems.isEmpty());
	}

	@Test
	public void switchingViewsKeepsOnlyTheHiddenInformationChoice(){
		UimsdModel m = model();
		m.setShowInternal(true);
		m.addFilter(Filter.num("score", Op.GE, 3));
		m.selectView("kinds");
		assertTrue(m.query().showInternal);
		assertTrue(m.query().filters.isEmpty());
		assertTrue(m.result().isGrouped());
		assertTrue(m.canGoBack());
		assertTrue(m.back());
		assertEquals("all", m.viewId());
		assertEquals(1, m.query().filters.size());   //back is exactly where the player was
	}

	@Test
	public void aLinkOpensTheMatchingRowsOfAnotherDatasetWithTheRowsOwnScope(){
		UimsdModel m = model();
		Row thing = null;
		for (Row r : m.result().rows) if (r.id.equals("t1")) thing = r;
		assertNotNull(thing);
		m.openLink(thing, things.links.get(0));
		assertEquals("parts", m.viewId());
		assertEquals(3, m.result().rows.size());   //the three parts of thing-1
		assertEquals(1, m.query().filters.size());
		assertTrue(m.back());
		assertEquals("all", m.viewId());
	}

	@Test
	public void aLinkFromAnUnfinishedRowWidensTheScopeSoItIsNotEmpty(){
		UimsdModel m = model();
		Row open = null;
		for (Row r : m.result().rows) if (!r.finished) open = r;
		assertNotNull(open);
		Scope s = Scope.DEFAULT;
		assertFalse(s.includeUnfinished);
		//the target's own scope is the default (finished only): the link must widen it
		m.selectView("all");
		m.openLink(open, new LinkDef("same", "all", "name", "name"));
		assertTrue(m.query().scope.includeUnfinished);
	}

	@Test
	public void statusActionsTurnTheScopeSwitchesOn(){
		UimsdModel m = model();
		m.setScope(Scope.DEFAULT);
		assertTrue(m.result().stats.unfinished > 0);
		boolean found = false;
		for (StatusText.Line l : m.status()) if (l.action.equals(StatusText.ACT_UNFINISHED)) found = true;
		assertTrue(found);
		m.applyAction(StatusText.ACT_UNFINISHED);
		assertTrue(m.query().scope.includeUnfinished);
		assertEquals(0, m.result().stats.unfinished);
	}

	@Test
	public void groupedViewsListSmallGroupsAsHiddenAndTheStatusLineCanShowThem(){
		UimsdModel m = model();
		m.selectView("kinds");
		assertEquals(3, m.result().groups.size());
		m.setMinSample(1000);
		assertEquals(0, m.result().groups.size());
		assertEquals(3, m.result().stats.hiddenSmallGroups);
		m.applyAction(StatusText.ACT_SMALL_GROUPS);
		assertEquals(3, m.result().groups.size());
		ResultTable t = m.table();
		assertTrue(t.grouped);
		assertEquals("Kind", t.headers.get(0).label);
		assertEquals("Rows", t.headers.get(1).label);
		assertEquals("Avg Score", t.headers.get(2).label);
	}

	@Test
	public void theHeaderShowsHowMuchOfAColumnIsKnown(){
		UimsdModel m = model();
		ResultTable t = m.table();
		for (ResultTable.Header h : t.headers) if (h.id.equals("score")) assertEquals("120/120", h.coverage);
		m.addFilter(Filter.num("score", Op.LT, 3));
		for (ResultTable.Header h : m.table().headers) if (h.id.equals("score")) assertTrue(h.coverage, h.coverage.startsWith(m.result().stats.matching + "/"));
	}

	@Test
	public void theStatusStripExplainsEverythingThatWasLeftOut(){
		UimsdModel m = model();
		m.setScope(Scope.DEFAULT);
		m.addFilter(Filter.num("depth", Op.GE, 1));   //depth is unknown for every row
		StringBuilder all = new StringBuilder();
		for (StatusText.Line l : m.status()) all.append(l.text).append('\n');
		String text = all.toString();
		assertTrue(text, text.contains("never recorded"));
		assertTrue(text, text.contains("still going"));
		assertTrue(text, text.contains("All things"));
	}

	@Test
	public void theModelCanBeLoadedFromAnEmptyHistoryOnAThread() throws Exception {
		java.io.File dir = java.nio.file.Files.createTempDirectory("uimsd").toFile();
		UimsdModel m = new UimsdModel(R, "");
		final boolean[] told = new boolean[1];
		m.setListener(new UimsdModel.Listener(){
			@Override
			public void changed(){
				told[0] = true;
			}
		});
		m.load(dir);
		m.awaitLoad(10000);
		assertEquals(UimsdModel.State.READY, m.state());
		assertTrue(told[0]);
		assertEquals(0, m.result().rows.size());
		dir.delete();
	}

	@Test
	public void theGenericPartsOfUimsdNeverNameAShippedDatasetOrColumn() throws Exception {
		String[] generic = { "QueryEngine", "Query", "Filter", "Sort", "Aggregate", "GroupBy", "Scope", "Op", "ColumnDef", "DatasetDef", "Row",
				"RowMerger", "CellFormatter", "CellText", "ResultTable", "StatusText", "UimsdModel", "QueryCodec", "Dataset", "QueryResult" };
		java.io.File dir = new java.io.File(System.getProperty("apd.query.src"));
		for (String name : generic){
			String text = new String(java.nio.file.Files.readAllBytes(new java.io.File(dir, name + ".java").toPath()), "UTF-8");
			for (String bad : new String[]{"FactKeys", "\"run.", "\"item.", "\"enemy.", "\"death.", "Datasets."}){
				assertFalse(name + " mentions " + bad, text.contains(bad));
			}
		}
	}

	//Seed floors are installed into the live catalog from a worker thread while the screen reads it.
	@Test
	public void theCatalogCanBeReadWhileSeedFloorsAreInstalled() throws Exception {
		final Catalog c = catalog();
		final java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
		final java.util.concurrent.atomic.AtomicBoolean stop = new java.util.concurrent.atomic.AtomicBoolean();
		Thread reader = new Thread(new Runnable(){
			@Override
			public void run(){
				try {
					while (!stop.get()){
						c.views();
						c.datasets();
						c.dataset("things");
						c.view("all");
					}
				} catch (Throwable e){
					failure.set(e);
				}
			}
		});
		reader.start();
		try {
			for (int i = 0; i < 20000 && failure.get() == null; i++){
				c.add(new ViewDef("extra" + i, "Extra", "x", Query.of("parts")));
			}
		} finally {
			stop.set(true);
			reader.join();
		}
		assertEquals(null, failure.get());
	}

	@Test
	public void filterChoicesAreTheValuesTheDataReallyHoldMostFrequentFirstAndNeverTheHiddenOnesOfARunStillGoing() {
		List<Row> rows = new ArrayList<>();
		String[] names = { "mage", "warrior", "mage", "rogue", "mage", "warrior" };
		for (int i = 0; i < names.length; i++){
			boolean finished = i != 5;      //the last warrior is still going
			Row r = row(things, "c" + i, names[i], (long) i, 0L, 1L, finished, R);
			r.setText(NOTE, (finished ? "secret-" : "open-secret-") + names[i], Fact.PROV_RECORDED);
			rows.add(r);
		}
		Catalog c = new Catalog();
		c.add(dataset(things, rows));
		c.add(new ViewDef("all", "All", "x", Query.of("things").withScope(Scope.DEFAULT.withUnfinished(true))));
		UimsdModel m = new UimsdModel(R, "");
		m.setCatalog(c);
		ColumnDef name = things.column("name"), note = things.column("note");
		List<UimsdModel.Choice> ch = m.choicesFor(name, 10);
		assertEquals(3, ch.size());
		assertEquals("mage", ch.get(0).value);
		assertEquals(3, ch.get(0).count);
		assertEquals("warrior", ch.get(1).value);
		assertEquals(2, ch.get(1).count);
		assertEquals("rogue", ch.get(2).value);
		assertEquals(1, m.choicesFor(name, 1).size());
		//a hidden column offers nothing while hidden information is off, and never the value of a run that is not over
		assertTrue(m.choicesFor(note, 10).isEmpty());
		m.setShowInternal(true);
		for (UimsdModel.Choice x : m.choicesFor(note, 10)) assertFalse("a run still going leaked: " + x.value, x.value.startsWith("open-"));
		assertFalse(m.choicesFor(note, 10).isEmpty());
		//only text columns have choices (numbers and enumerations have their own)
		assertTrue(m.choicesFor(things.column("score"), 10).isEmpty());
	}
}
