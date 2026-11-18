package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//The promises of UIMSD checked end to end, on rows built from real logs by the real builders:
//  - nothing observable depends on hidden information of a run that is not finished
//  - hidden information of a finished run can be read (when asked for)
//  - reading never changes the raw history
public class UimsdSafetyTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String RS = "r1-aaaaaaaaaaaa";
	private static final TelemetryVersion V14 = new TelemetryVersion(1, 4);

	//a finished run, and a run still going. 'secretClass' and the hidden extras differ between the two histories compared.
	private File history( String name, String secretClass, int hiddenLevel, boolean extraHiddenItems, boolean extraHiddenMob ) throws Exception {
		File root = new File(tmp.getRoot(), name);
		new SyntheticLog(root, "fin", V14, 100).start("run-fin", 0, "WARRIOR", RS, 0, 0, false).enter(1)
				.registered("f-sword", "items.weapon.melee.Sword", 1, false, ItemEvents.ORIGIN_GENERATED).generated("f-sword", GenerationEvents.C_HEAP, "Room 1")
				.registered("f-unseen", "items.potions.PotionOfHealing", 0, false, ItemEvents.ORIGIN_GENERATED)
				.seen("f-sword", "sword", ItemEvents.CAT_WEAPON).acquired("f-sword", "sword", ItemEvents.CAT_WEAPON, false, 0)
				.mobGenerated("actors.mobs.Rat").mobGenerated("actors.mobs.Gnoll")
				.heroAttacks("rat", "actors.mobs.Rat", true).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 2, 100).close(true);
		SyntheticLog open = new SyntheticLog(root, "open", V14, 200).start("run-open", 0, "MAGE", RS, 0, 0, false).enter(1);
		open.registered("o-ring", secretClass, hiddenLevel, true, ItemEvents.ORIGIN_GENERATED).generated("o-ring", GenerationEvents.C_CHEST, "Vault " + secretClass);
		open.seen("o-ring", "ruby ring", ItemEvents.CAT_RING).acquired("o-ring", "ruby ring", ItemEvents.CAT_RING, false, 0)
				.equip("o-ring", "ruby ring", ItemEvents.CAT_RING);
		open.mobGenerated("actors.mobs.Rat");
		if (extraHiddenMob) open.mobGenerated("actors.mobs.Goo").mobGenerated("actors.mobs.Goo");
		if (extraHiddenItems){
			open.registered("o-secret-1", "items.armor.PlateArmor", 3, false, ItemEvents.ORIGIN_GENERATED).generated("o-secret-1", GenerationEvents.C_LOCKED_CHEST, "Treasury");
			open.registered("o-secret-2", "items.rings.RingOfWealth", 2, false, ItemEvents.ORIGIN_GENERATED).generated("o-secret-2", GenerationEvents.C_HEAP, "Secret room");
		}
		open.heroAttacks("rat", "actors.mobs.Rat", true).damageFromHero("rat", "actors.mobs.Rat", 4).close(false);
		return root;
	}

	private static String observe( QueryResult r ){
		StringBuilder b = new StringBuilder();
		QueryResult.Stats s = r.stats;
		b.append(r.columns.size()).append(r.problems).append('|').append(s.total).append(',').append(s.inScope).append(',').append(s.matching)
				.append(',').append(s.otherRulesets).append(',').append(s.unknownRuleset).append(',').append(s.mixedRuleset).append(',')
				.append(s.unfinished).append(',').append(s.duplicates).append(',').append(s.hiddenRows).append(',').append(s.hiddenSmallGroups)
				.append(',').append(s.runsInScope).append(',').append(s.runsCovered).append('|');
		for (QueryResult.FilterReport f : s.filters){
			b.append(f.in).append(':').append(f.matched).append(':').append(f.notMatched).append(':').append(f.unknown).append(':')
					.append(f.uncertain).append(':').append(f.hidden).append(';');
		}
		for (QueryResult.ColumnCoverage c : r.coverage){
			b.append(c.column.id).append('=').append(c.total).append('/').append(c.known).append('/').append(c.hidden).append(';');
		}
		b.append('|');
		for (Row row : r.rows){
			b.append(row.id.replaceAll("[^|/ ]*$", "")).append('[');
			for (ColumnDef c : r.columns) b.append(CellFormatter.cell(r.query, row, c).text).append(',');
			b.append(']');
		}
		if (r.isGrouped()){
			for (GroupRow g : r.groups){
				b.append('{').append(g.n).append(':');
				for (ColumnDef k : r.groupKeys) b.append(CellFormatter.cell(r.query, g.sample, k).text).append(',');
				for (int i = 0; i < g.aggregates.size(); i++){
					b.append(CellFormatter.aggregate(r.def, r.aggregates.get(i), g.aggregates.get(i)).text).append('/').append(g.aggregates.get(i).contributing).append(',');
				}
				b.append('}');
			}
		}
		return b.toString();
	}

	private static List<Query> queries( Catalog cat ){
		List<Query> out = new ArrayList<>();
		for (ViewDef v : cat.views()){
			DatasetDef def = cat.datasetOf(v).def;
			List<String> all = new ArrayList<>();
			for (ColumnDef c : def.columns()) all.add(c.id);
			Scope[] scopes = { Scope.DEFAULT, Scope.DEFAULT.withUnfinished(true), new Scope(Scope.Rules.ALL, "", true, true, true, true) };
			for (Scope sc : scopes){
				for (boolean show : new boolean[]{false, true}){
					Query base = v.initialQuery().withScope(sc).withShowInternal(show);
					out.add(base);
					out.add(base.withColumns(all));
					for (ColumnDef c : def.columns()){
						if (c.type == ColumnDef.Type.TEXT){
							out.add(base.withColumns(all).addFilter(Filter.text(c.id, Op.CONTAINS, "a")));
							out.add(base.withColumns(all).addFilter(Filter.unknown(c.id)));
						} else {
							out.add(base.withColumns(all).addFilter(Filter.num(c.id, Op.GE, 0)));
							out.add(base.withColumns(all).addFilter(Filter.unknown(c.id)));
						}
						out.add(base.withColumns(all).withSorts(Arrays.asList(new Sort(c.id, false))));
						out.add(base.withColumns(all).withSorts(Arrays.asList(new Sort(c.id, true))));
					}
					for (ColumnDef c : def.columns()){
						out.add(base.withGroup(new GroupBy(Arrays.asList(c.id), Arrays.asList(Aggregate.count(), Aggregate.of(Aggregate.Fn.COUNT_KNOWN, c.id)))));
					}
				}
			}
		}
		return out;
	}

	@Test
	public void nothingObservableDependsOnTheHiddenInformationOfARunThatIsNotFinished() throws Exception {
		Catalog a = new DatasetStore(history("a", "items.rings.RingOfHaste", 1, false, false)).load();
		Catalog b = new DatasetStore(history("b", "items.rings.RingOfElements", 5, true, true)).load();
		List<Query> queries = queries(a);
		assertTrue(queries.size() > 500);
		int checked = 0;
		for (Query q : queries){
			String oa = observe(QueryEngine.run(a.dataset(q.dataset), q, RS));
			String ob = observe(QueryEngine.run(b.dataset(q.dataset), q, RS));
			assertEquals("hidden information of the unfinished run leaked through " + QueryCodec.encode(q), oa, ob);
			checked++;
		}
		System.out.println("APD-PERF safety: " + checked + " queries compared, none differed");
	}

	@Test
	public void theUnfinishedRunsHiddenItemsAndEnemiesAreNeverListedNorCounted() throws Exception {
		Catalog c = new DatasetStore(history("c", "items.rings.RingOfHaste", 1, true, true)).load();
		Query q = c.view("items").instanceQuery().withShowInternal(true).withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
		QueryResult r = QueryEngine.run(c.dataset("items"), q, RS);
		List<String> ids = new ArrayList<>();
		for (Row row : r.rows) ids.add(row.id);
		assertTrue(ids.toString(), ids.contains("run-fin/f-sword"));
		assertTrue("hidden items of a FINISHED run can be read", ids.contains("run-fin/f-unseen"));
		assertTrue("the ring the player picked up is a public row", ids.contains("run-open/o-ring"));
		assertFalse("undiscovered items of the run being played are not rows at all", ids.contains("run-open/o-secret-1"));
		assertFalse(ids.contains("run-open/o-secret-2"));
		//and the ring's hidden truth is masked
		Row ring = null;
		for (Row row : r.rows) if (row.id.equals("run-open/o-ring")) ring = row;
		ColumnDef trueClass = c.dataset("items").def.column("item.trueClass");
		assertEquals(CellText.HIDDEN, CellFormatter.cell(q, ring, trueClass).state);
		assertEquals("ruby ring", CellFormatter.cell(q, ring, c.dataset("items").def.column("item.name")).text);

		QueryResult en = QueryEngine.run(c.dataset("enemies"), c.view("enemies").instanceQuery().withShowInternal(true)
				.withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true)), RS);
		for (Row row : en.rows) assertFalse("an enemy the player never met in the unfinished run is not listed", row.id.contains("run-open|actors.mobs.Goo"));
		assertTrue(en.rows.size() >= 2);
	}

	@Test
	public void onceARunIsFinishedItsHiddenInformationCanBeRead() throws Exception {
		File root = history("d", "items.rings.RingOfHaste", 1, true, false);
		//the open run now ends
		Catalog before = new DatasetStore(root).load();
		assertFalse(before.dataset("runs").rows.get(0).finished && before.dataset("runs").rows.get(1).finished);
		File open = new File(new File(root, "runs"), "open.apdlog");
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter w =
				com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter.openForAppend(open, V14);
		LifecycleEvents.RunEnd e = new LifecycleEvents.RunEnd();
		e.outcome = LifecycleEvents.OUTCOME_DIED; e.cause = "Rat"; e.deepest = 1; e.heroLevel = 1; e.score = 5; e.turns = 99;
		LifecycleEvents.emitRunEnd(w, 90, e);
		w.close(true);
		Catalog after = new DatasetStore(root).load();
		Query q = after.view("items").instanceQuery().withShowInternal(true).withScope(Scope.DEFAULT);
		QueryResult r = QueryEngine.run(after.dataset("items"), q, RS);
		List<String> ids = new ArrayList<>();
		for (Row row : r.rows) ids.add(row.id);
		assertTrue("the secrets of the finished run are readable now", ids.contains("run-open/o-secret-1"));
		Row ring = null;
		for (Row row : r.rows) if (row.id.equals("run-open/o-ring")) ring = row;
		assertEquals("items.rings.RingOfHaste", CellFormatter.cell(q, ring, after.dataset("items").def.column("item.trueClass")).text);
	}

	@Test
	public void readingTheHistoryNeverChangesTheRawLogs() throws Exception {
		File root = history("e", "items.rings.RingOfHaste", 1, true, true);
		File[] logs = new File(root, "runs").listFiles((d, n) -> n.endsWith(".apdlog"));
		byte[][] before = new byte[logs.length][];
		for (int i = 0; i < logs.length; i++) before[i] = Files.readAllBytes(logs[i].toPath());
		UimsdModel m = new UimsdModel(RS, "");
		m.load(root);
		m.awaitLoad(10000);
		for (ViewDef v : m.catalog().views()){
			m.selectView(v.id);
			m.setShowInternal(true);
			m.table();
			m.status();
		}
		for (int i = 0; i < logs.length; i++) assertArrayEquals(logs[i].getName(), before[i], Files.readAllBytes(logs[i].toPath()));
		assertTrue(new File(root, "derived").isDirectory());
	}

	@Test
	public void theCombatNamesInEnemyRowsAreOnlyWhatThePlayerSaw() throws Exception {
		Catalog c = new DatasetStore(history("f", "items.rings.RingOfHaste", 1, false, true)).load();
		Query q = c.view("enemies").instanceQuery().withShowInternal(false).withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
		QueryResult r = QueryEngine.run(c.dataset("enemies"), q, RS);
		for (ColumnDef col : r.columns) assertFalse(col.id, col.isInternal());
		for (Row row : r.rows){
			assertTrue(row.publicRow);
			assertEquals("rat", CellFormatter.cell(q, row, c.dataset("enemies").def.column("enemy.name")).text.toLowerCase().replace("rat", "rat"));
		}
	}
}
