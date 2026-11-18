/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;

public class SavedViewsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static Query q( String dataset, int minSample ){
		return new Query(dataset, Scope.DEFAULT, new ArrayList<Filter>(), new ArrayList<Sort>(), new ArrayList<String>(), null, false, minSample);
	}

	@Test public void viewsCanBeSavedBeforeTheFirstRunCreatesHistory() throws Exception {
		File root = new File(tmp.getRoot(), "first-launch/history");
		assertFalse(root.exists());
		SavedViews views = new SavedViews();
		views.save("Empty history view", q("runs", 0), 1);
		views.store(SavedViews.fileIn(root));
		assertTrue(root.isDirectory());
		assertEquals("runs", SavedViews.load(SavedViews.fileIn(root)).get("Empty history view").query().dataset);
	}

	@Test
	public void aSavedViewComesBackAsTheSameQuestion() throws Exception {
		File f = SavedViews.fileIn(tmp.getRoot());
		SavedViews s = new SavedViews();
		assertEquals(SavedViews.Result.SAVED, s.save("  My eleven filters ", q("runs", 5), 100));
		assertEquals(SavedViews.Result.SAVED, s.save("weapons", q("items", 0), 200));
		s.store(f);
		assertFalse(new File(f.getPath() + ".tmp").exists());
		SavedViews back = SavedViews.load(f);
		assertEquals(0, back.skippedOnLoad);
		assertEquals(2, back.list().size());
		assertEquals("a name is found whatever its case", "runs", back.get("my ELEVEN filters").query().dataset);
		assertEquals(QueryCodec.encode(q("runs", 5)), QueryCodec.encode(back.get("My eleven filters").query()));
		assertEquals(100, back.get("My eleven filters").createdAtMillis);
	}

	@Test
	public void namesAreValidatedReplacedRenamedAndDeleted(){
		SavedViews s = new SavedViews();
		assertEquals(SavedViews.Result.BAD_NAME, s.save("", q("runs", 0), 1));
		assertEquals(SavedViews.Result.BAD_NAME, s.save("a\nb", q("runs", 0), 1));
		StringBuilder big = new StringBuilder();
		for (int i = 0; i <= SavedViews.MAX_NAME; i++) big.append('x');
		assertEquals(SavedViews.Result.BAD_NAME, s.save(big.toString(), q("runs", 0), 1));
		assertEquals(SavedViews.Result.SAVED, s.save("a", q("runs", 0), 1));
		assertEquals("the same name replaces that view", SavedViews.Result.REPLACED, s.save("A", q("items", 0), 2));
		assertEquals(1, s.list().size());
		assertEquals("items", s.get("a").query().dataset);
		s.save("b", q("runs", 0), 3);
		assertFalse("a name that is taken cannot be used", s.rename("a", "B"));
		assertTrue(s.rename("a", "c"));
		assertNull(s.get("a"));
		assertNotNull(s.get("c"));
		assertTrue(s.delete("C"));
		assertFalse(s.delete("C"));
		assertEquals(1, s.list().size());
	}

	@Test
	public void thereIsALimit(){
		SavedViews s = new SavedViews();
		for (int i = 0; i < SavedViews.MAX_VIEWS; i++) assertEquals(SavedViews.Result.SAVED, s.save("v" + i, q("runs", 0), i));
		assertEquals(SavedViews.Result.TOO_MANY, s.save("one more", q("runs", 0), 0));
		assertEquals("replacing is still allowed at the limit", SavedViews.Result.REPLACED, s.save("v1", q("items", 0), 0));
	}

	@Test
	public void missingOrDamagedFilesLoseOnlyTheDamage() throws Exception {
		File f = SavedViews.fileIn(tmp.getRoot());
		assertEquals(0, SavedViews.load(f).list().size());
		SavedViews s = new SavedViews();
		s.save("one", q("runs", 0), 1);
		s.save("two", q("items", 0), 2);
		s.store(f);
		String[] lines = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).split("\n");
		Files.write(f.toPath(), (lines[0] + "\nnot a line\n" + lines[1] + "\n" + lines[1] + "\n").getBytes(StandardCharsets.UTF_8));
		SavedViews back = SavedViews.load(f);
		assertEquals(2, back.list().size());
		assertEquals("the garbage line and the repeated name", 2, back.skippedOnLoad);
	}

	@Test
	public void aViewThisBuildCannotReadIsReportedNotCrashed(){
		SavedViews s = new SavedViews();
		s.save("ok", q("runs", 0), 1);
		assertNotNull(s.get("ok").query());
		List<SavedViews.View> l = s.list();
		assertEquals(1, l.size());
	}

	@Test
	public void savedCombinedQueryReopensAndDeletionSurvivesReload() throws Exception {
		Query original = Query.of("runs")
				.addFilter(Filter.text(FactKeys.HERO_CLASS, Op.EQ, "WARRIOR"))
				.withSorts(java.util.Arrays.asList(new Sort(FactKeys.SCORE, true)))
				.withColumns(java.util.Arrays.asList(FactKeys.SCORE, FactKeys.HERO_CLASS, FactKeys.SEED_TYPE, FactKeys.EXTERNAL_MODS));
		File file = SavedViews.fileIn(tmp.getRoot());
		SavedViews views = new SavedViews();
		assertEquals(SavedViews.Result.SAVED, views.save("Warriors", original, 100));
		views.store(file);
		SavedViews reopened = SavedViews.load(file);
		assertEquals(QueryCodec.encode(original), QueryCodec.encode(reopened.get("Warriors").query()));
		DatasetDef def = Datasets.runs();
		List<Row> rows = java.util.Arrays.asList(run(def, "a", "WARRIOR", 100L), run(def, "b", "WARRIOR", 100L),
				run(def, "c", "WARRIOR", 50L), run(def, "mage", "MAGE", 999L), run(def, "unknown", "WARRIOR", null));
		Dataset data = QuerySupport.dataset(def, rows);
		Query loaded = reopened.get("Warriors").query();
		QueryResult descending = QueryEngine.run(data, loaded, "rules");
		assertTrue(descending.problems.toString(), descending.problems.isEmpty());
		assertEquals(java.util.Arrays.asList("a", "b", "c", "unknown"), ids(descending));
		assertEquals(original.columns, columnIds(descending));
		assertEquals(java.util.Arrays.asList("c", "a", "b", "unknown"), ids(QueryEngine.run(data,
				loaded.withSorts(java.util.Arrays.asList(new Sort(FactKeys.SCORE, false))), "rules")));
		QueryResult empty = QueryEngine.run(data, loaded.addFilter(Filter.num(FactKeys.SCORE, Op.GT, 2000)), "rules");
		assertTrue(empty.rows.isEmpty());
		assertTrue(empty.problems.isEmpty());
		assertEquals(original.columns, columnIds(empty));
		assertTrue(reopened.delete("Warriors"));
		reopened.store(file);
		assertTrue(SavedViews.load(file).list().isEmpty());
	}

	private Row run(DatasetDef def, String id, String heroClass, Long score){
		Row row = new Row(id, id, id, def.size());
		row.finished = true;
		row.ruleset = "rules";
		row.setText(def.indexOf(FactKeys.HERO_CLASS), heroClass, Fact.PROV_RECORDED);
		if (score != null) row.setNum(def.indexOf(FactKeys.SCORE), score, Fact.PROV_RECORDED);
		row.setNum(def.indexOf(FactKeys.SEED_TYPE), 0, Fact.PROV_RECORDED);
		row.setNum(def.indexOf(FactKeys.EXTERNAL_MODS), 0, Fact.PROV_RECORDED);
		return row;
	}
	private List<String> ids(QueryResult result){
		List<String> ids = new ArrayList<>();
		for (Row row : result.rows) ids.add(row.id);
		return ids;
	}
	private List<String> columnIds(QueryResult result){
		List<String> ids = new ArrayList<>();
		for (ColumnDef column : result.columns) ids.add(column.id);
		return ids;
	}
    @Test public void savingRenamingOrDeletingCannotRewriteAnIncompleteLoad() throws Exception {
        for (String damage : new String[]{"malformed", "duplicate", "invalid-utf8"}) {
            for (String operation : new String[]{"save", "rename", "delete"}) {
                File target = new File(tmp.getRoot(), damage + "-" + operation + ".txt");
                SavedViews initial = new SavedViews();
                initial.save("Existing", q("runs", 0), 1);
                initial.store(target);
                String valid = Files.readString(target.toPath());
                String damaged = damage.equals("duplicate") ? valid : damage.equals("invalid-utf8")
                        ? java.util.Base64.getEncoder().encodeToString(new byte[]{(byte) 0xc3, 0x28})
                            + valid.substring(valid.indexOf('\t'))
                        : "unreadable saved view\n";
                Files.writeString(target.toPath(), valid + damaged);
                byte[] original = Files.readAllBytes(target.toPath());
                SavedViews loaded = SavedViews.load(target);
                assertEquals(1, loaded.skippedOnLoad);
                assertNotNull("valid views remain readable", loaded.get("Existing"));
                if (operation.equals("save")) loaded.save("New", q("runs", 1), 2);
                else if (operation.equals("rename")) assertTrue(loaded.rename("Existing", "Renamed"));
                else assertTrue(loaded.delete("Existing"));
                try {
                    loaded.store(target);
                    fail("An incomplete load must never silently discard " + damage + " during " + operation);
                } catch (java.io.IOException expected) {
                    assertTrue(expected.getMessage(), expected.getMessage().contains("preserved"));
                }
                assertArrayEquals("all original bytes survive", original, Files.readAllBytes(target.toPath()));
                assertFalse("refusal does not write a partial replacement", new File(target + ".tmp").exists());
            }
        }
    }

    @Test public void anUnreadableQueryIsPreservedWhileOtherValidViewsCanStillBeSaved() throws Exception {
        File target = new File(tmp.getRoot(), "future-query.txt");
        String line = java.util.Base64.getEncoder().encodeToString("Future view".getBytes(StandardCharsets.UTF_8))
                + "\t1\t" + java.util.Base64.getEncoder().encodeToString("dataset\truns\nscope\tFUTURE_RULES\t\t0\n".getBytes(StandardCharsets.UTF_8)) + "\n";
        Files.writeString(target.toPath(), line);
        SavedViews loaded = SavedViews.load(target);
        assertEquals(0, loaded.skippedOnLoad);
        assertNull(loaded.get("Future view").query());
        loaded.save("Supported view", q("runs", 0), 2);
        loaded.store(target);
        SavedViews reopened = SavedViews.load(target);
        assertEquals("dataset\truns\nscope\tFUTURE_RULES\t\t0\n", reopened.get("Future view").encodedQuery);
        assertNotNull(reopened.get("Supported view").query());
        assertTrue(Files.readString(target.toPath()).startsWith(line));
    }

	@Test public void anInvalidDestinationDirectoryIsNeverDeletedToSaveViews() throws Exception {
		File target = tmp.newFolder("destination");
		SavedViews views = new SavedViews();
		views.save("one", q("runs", 0), 1);
		try {
			views.store(target);
			fail("a directory is not a saved-view file");
		} catch (java.io.IOException expected) {
			assertTrue("preserve the existing directory", target.isDirectory());
		}
	}

	@Test public void aFailedTemporaryWritePreservesThePreviousViews() throws Exception {
		File target = SavedViews.fileIn(tmp.getRoot());
		SavedViews views = new SavedViews();
		views.save("old", q("runs", 0), 1);
		views.store(target);
		byte[] original = Files.readAllBytes(target.toPath());
		assertTrue(new File(target.getPath() + ".tmp").mkdir());
		views.save("new", q("items", 0), 2);
		try {
			views.store(target);
			fail("the temporary path is unavailable");
		} catch (java.io.IOException expected) {
			assertArrayEquals(original, Files.readAllBytes(target.toPath()));
			assertEquals(1, SavedViews.load(target).list().size());
		}
	}

	@Test public void separateInstancesSerializeTheirAtomicStore() throws Exception {
		File target = new File(tmp.getRoot(), "concurrent-saved-views.txt");
		SavedViews first = new SavedViews();
		SavedViews second = new SavedViews();
		first.save("first", q("runs", 0), 1);
		second.save("second", q("items", 0), 2);

		CountDownLatch firstOpened = new CountDownLatch(1);
		CountDownLatch releaseFirst = new CountDownLatch(1);
		CountDownLatch secondStarted = new CountDownLatch(1);
		CountDownLatch secondOpened = new CountDownLatch(1);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread a = new Thread(new Runnable(){
			@Override public void run(){
				try {
					first.store(target, new SavedViews.StoreHook(){
						@Override public void afterTempOpened( File temp ) throws InterruptedException {
							firstOpened.countDown();
							releaseFirst.await();
						}
					});
				} catch (Throwable t){
					failure.compareAndSet(null, t);
				}
			}
		}, "first-saved-view-store");
		Thread b = new Thread(new Runnable(){
			@Override public void run(){
				secondStarted.countDown();
				try {
					second.store(target, new SavedViews.StoreHook(){
						@Override public void afterTempOpened( File temp ){
							secondOpened.countDown();
						}
					});
				} catch (Throwable t){
					failure.compareAndSet(null, t);
				}
			}
		}, "second-saved-view-store");

		a.start();
		assertTrue(firstOpened.await(5, TimeUnit.SECONDS));
		b.start();
		assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
		try {
			assertFalse("a second instance must not open the shared temp file", secondOpened.await(200, TimeUnit.MILLISECONDS));
		} finally {
			releaseFirst.countDown();
		}
		a.join(5000);
		b.join(5000);
		assertFalse(a.isAlive());
		assertFalse(b.isAlive());
		assertNull(failure.get());
		assertTrue(secondOpened.await(1, TimeUnit.SECONDS));
		SavedViews loaded = SavedViews.load(target);
		assertEquals(0, loaded.skippedOnLoad);
		assertEquals(1, loaded.list().size());
		assertNotNull(loaded.get("second"));
		assertFalse(new File(target.getPath() + ".tmp").exists());
	}

}
