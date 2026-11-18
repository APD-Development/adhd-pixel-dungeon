package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.SelfTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class UimsdSelfTestTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void theUimsdSelfTestPassesAndCleansUpAfterItself(){
		SelfTest.Report r = new SelfTest.Report();
		UimsdSelfTest.run(tmp.getRoot(), r);
		assertTrue(r.text(), r.passed);
		assertTrue(r.text().contains("UIMSD datasets and queries"));
		assertTrue(r.text().contains("reload from cache"));
		assertEquals("the self-test removes what it made", 0, tmp.getRoot().list().length);
	}

	@Test
	public void itFailsLoudlyIfItCannotWrite() throws Exception {
		java.io.File file = tmp.newFile("not-a-folder");
		SelfTest.Report r = new SelfTest.Report();
		UimsdSelfTest.run(new java.io.File(file, "inside"), r);
		assertTrue(!r.passed);
	}


	@Test
	public void finishedInternalIdentityIsVisibleWhileOpenIdentityStaysHidden() throws Exception {
		File root = tmp.newFolder("privacy");
		writeItemRun(root, "finished", true);
		writeItemRun(root, "open", false);

		Dataset items = new DatasetStore(root).load().dataset(Datasets.ITEMS);
		Query q = Query.of(Datasets.ITEMS).withShowInternal(true)
				.withScope(Scope.DEFAULT.withUnfinished(true).withRules(Scope.Rules.ALL, ""));
		QueryResult result = QueryEngine.run(items, q, "r1-selftest");
		ColumnDef trueClass = items.def.column("item.trueClass");

		int finishedRows = 0;
		int openRows = 0;
		for (Row row : result.rows){
			CellText text = CellFormatter.cell(q, row, trueClass);
			if (row.finished){
				finishedRows++;
				assertEquals("finished-run internal identity is readable", CellText.EXACT, text.state);
			} else {
				openRows++;
				assertEquals("open-run internal identity stays private", CellText.HIDDEN, text.state);
			}
		}
		assertEquals(1, finishedRows);
		assertEquals(1, openRows);
	}

	private static void writeItemRun( File root, String id, boolean finished ){
		File file = new File(new File(root, "runs"), id + ".apdlog");
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter writer =
				com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter.create(file,
						com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion.CURRENT, id, "selftest", 1L);
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.RunStart start =
				new com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.RunStart();
		start.runId = id;
		start.heroClass = "WARRIOR";
		start.rulesetId = "r1-selftest";
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.emitRunStart(writer, 0, false, start);

		com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.LevelEnter enter =
				new com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.LevelEnter();
		enter.depth = 1;
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.emitLevelEnter(writer, 1, enter);

		com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.Registered registered =
				new com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.Registered();
		registered.uid = "u";
		registered.trueClass = "items.weapon.melee.WarHammer";
		registered.trueLevel = 0;
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.emitRegistered(writer, 1, 0, registered);

		com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.Seen seen =
				new com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.Seen();
		seen.uid = "u";
		seen.state.name = "war hammer";
		seen.state.category = com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.CAT_WEAPON;
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.emitSeen(writer, 2, 0, seen);

		if (finished){
			com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.RunEnd end =
					new com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.RunEnd();
			end.outcome = com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.OUTCOME_DIED;
			end.deepest = 1;
			end.heroLevel = 1;
			com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents.emitRunEnd(writer, 3, end);
		}
		assertTrue("writer closes completely", writer.close(finished));
	}
}
