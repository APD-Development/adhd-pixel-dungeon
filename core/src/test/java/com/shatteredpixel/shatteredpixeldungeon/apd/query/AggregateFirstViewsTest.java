package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//The aggregate-first hierarchy: a kind of thing (all glaives) -> its statistics -> its individual instances -> their run, and
//kind -> the runs that contain it -> a run. Built on the generic grouped query, group drill-down and links.
public class AggregateFirstViewsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String RS = "r1-aaaaaaaaaaaa";
	private static final TelemetryVersion V = new TelemetryVersion(1, 4);
	private static final int WEAPON = ItemEvents.CAT_WEAPON;

	private File root;

	private SyntheticLog log( String id, long created ){
		return new SyntheticLog(root, id, V, created);
	}

	private Catalog load(){
		return new DatasetStore(root).load();
	}

	private SyntheticLog sword( SyntheticLog l, String uid, String name, boolean acquire, boolean equip ){
		l.registered(uid, "items.weapon.melee.Sword", 1, false, ItemEvents.ORIGIN_GENERATED).seen(uid, name, WEAPON);
		if (acquire) l.acquired(uid, name, WEAPON, false, 0);
		if (equip) l.equip(uid, name, WEAPON).tick(10);
		return l;
	}

	private QueryResult group( Catalog c, String viewId, Query q ){
		return QueryEngine.run(c.dataset(c.view(viewId).dataset()), q, RS);
	}

	private GroupDetail detailOf( Catalog c, String viewId, String typeText ){
		QueryResult r = group(c, viewId, c.view(viewId).initialQuery());
		for (GroupRow g : r.groups){
			if (CellFormatter.cell(r.query, g.sample, r.groupKeys.get(r.groupKeys.size() - 1)).text.equals(typeText)) return GroupDetail.build(r, g);
		}
		throw new AssertionError("no group " + typeText + " in " + r.groups.size());
	}

	private static String value( GroupDetail d, String label ){
		for (GroupDetail.Line l : d.lines) if (!l.section && l.label.equals(label)) return l.value;
		throw new AssertionError("no statistic " + label);
	}

	// ---- one type, however the name changes ----

	@Test
	public void anEnchantedWeaponIsStillTheSameTypeOfWeapon() throws Exception {
		root = tmp.newFolder("a");
		SyntheticLog l = log("a", 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1);
		sword(l, "s1", "sword", true, true);
		sword(l, "s2", "blazing sword", true, false);
		sword(l, "s3", "vampiric sword", false, false);
		l.registered("d1", "items.weapon.melee.Dagger", 0, false, ItemEvents.ORIGIN_GENERATED).seen("d1", "dagger", WEAPON);
		l.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Catalog c = load();
		QueryResult r = group(c, "weapons", c.view("weapons").initialQuery());
		assertTrue(r.isGrouped());
		assertEquals("one row per kind of weapon, not per name", 2, r.groups.size());
		GroupRow sw = r.groups.get(0);   //most instances first
		assertEquals(3, sw.n);
		assertEquals("Sword", CellFormatter.cell(r.query, sw.sample, r.groupKeys.get(0)).text);
		assertEquals("Dagger", CellFormatter.cell(r.query, r.groups.get(1).sample, r.groupKeys.get(0)).text);
	}

	@Test
	public void aRingIsOnlyItsRealKindOnceThePlayerLearnedIt(){
		try {
			root = tmp.newFolder("b");
		} catch (java.io.IOException e){
			throw new RuntimeException(e);
		}
		//run 1: the ring is never identified, its kind stays unknown to the player
		log("one", 100).start("run-1", 0, "MAGE", RS, 0, 0, false).enter(1)
				.registered("r1", "items.rings.RingOfHaste", 1, false, ItemEvents.ORIGIN_GENERATED)
				.seen("r1", "ruby ring", ItemEvents.CAT_RING).acquired("r1", "ruby ring", ItemEvents.CAT_RING, false, 0)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		//run 2: the player learned what ruby rings are
		log("two", 200).start("run-2", 0, "MAGE", RS, 0, 0, false).enter(1)
				.registered("r2", "items.rings.RingOfHaste", 1, false, ItemEvents.ORIGIN_GENERATED)
				.classKnown("ruby ring", "items.rings.RingOfHaste", ItemEvents.CAT_RING)
				.seen("r2", "ring of haste", ItemEvents.CAT_RING).acquired("r2", "ring of haste", ItemEvents.CAT_RING, true, 2)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Catalog c = load();
		Dataset items = c.dataset("items");
		ColumnDef t = items.def.column("item.type");
		Row unknownKind = null, knownKind = null;
		for (Row r : items.rows){
			if (r.id.equals("run-1/r1")) unknownKind = r;
			if (r.id.equals("run-2/r2")) knownKind = r;
		}
		assertEquals("marked ~ because it is the name standing in for the kind", "~ruby ring", CellFormatter.cell(Query.of("items"), unknownKind, t).text);
		assertEquals("what the player saw stands in, and is marked as such", Fact.PROV_INFERRED, unknownKind.prov(t.index));
		assertEquals("Ring of Haste", CellFormatter.cell(Query.of("items"), knownKind, t).text);
		assertEquals(Fact.PROV_RECORDED, knownKind.prov(t.index));
	}

	@Test
	public void ARunRecordedWithoutItemIdentityFallsBackToTheNameAndIsNotGuessed() throws Exception {
		root = tmp.newFolder("c");
		log("old", 100).start("run-old", 0, "WARRIOR", RS, 0, 0, false).enter(1)
				.seen("w1", "war hammer", WEAPON).acquired("w1", "war hammer", WEAPON, false, 0)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Catalog c = load();
		Row r = c.dataset("items").rows.get(0);
		ColumnDef t = c.dataset("items").def.column("item.type");
		assertEquals("~war hammer", CellFormatter.cell(Query.of("items"), r, t).text);
		assertEquals(Fact.PROV_INFERRED, r.prov(t.index));
	}

	@Test
	public void rareTypesAreNotHiddenAndEveryViewStartsGrouped() throws Exception {
		root = tmp.newFolder("d");
		SyntheticLog l = log("a", 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1);
		sword(l, "s1", "sword", true, false);
		l.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Catalog c = load();
		QueryResult r = group(c, "weapons", c.view("weapons").initialQuery());
		assertEquals("a type seen once is listed", 1, r.groups.size());
		assertEquals(0, r.stats.hiddenSmallGroups);
		for (String id : new String[]{ "items", "weapons", "armor", "rings", "trinkets", "artifacts", "enemies" }){
			assertNotNull(id, c.view(id).initialQuery().group);
			assertNotNull(id + " has individual rows behind its groups", c.view(id).instanceQuery());
		}
		assertNull("runs are individual already", c.view("runs").instanceQuery());
		assertNull(c.view("deaths").instanceQuery());
	}

	// ---- the denser table ----

	@Test
	public void theWeaponsTableIsADenseStatisticsTable() throws Exception {
		root = tmp.newFolder("e");
		SyntheticLog l = log("a", 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1);
		sword(l, "s1", "sword", true, true);
		l.end(LifecycleEvents.OUTCOME_WON, "", 26, 20, 5).close(true);
		Catalog c = load();
		QueryResult r = group(c, "weapons", c.view("weapons").initialQuery());
		ResultTable t = ResultTable.build(r, 0, 50);
		List<String> labels = new ArrayList<>();
		for (ResultTable.Header h : t.headers) labels.add(h.label);
		assertEquals("[Type, Items, Runs, Acq %, Equip %, Wins, Win %, Avg floor, Avg lvl, Avg turns, ID %]", labels.toString());
		ResultTable.Line line = t.lines.get(0);
		assertEquals("Sword", line.cells[0].text);
		assertEquals("1", line.cells[1].text);   //instances
		assertEquals("1", line.cells[2].text);   //runs
		assertEquals("100%", line.cells[3].text); //acquired of seen: compact, the counts are in the detail
		assertEquals("100%", line.cells[4].text);
		assertEquals("1", line.cells[5].text);   //wins
		assertEquals("100%", line.cells[6].text);
		assertTrue("one value is a small sample", line.cells[3].small);
	}

	// ---- statistics with the right denominators ----

	@Test
	public void acquiredIsOutOfSeenEquippedIsOutOfAcquiredAndRunsCountOnce() throws Exception {
		root = tmp.newFolder("f");
		//run-a (won): s1 only seen, s2 acquired but never equipped. run-b (died): s3 and s4 acquired and equipped
		SyntheticLog a = log("a", 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1);
		sword(a, "s1", "sword", false, false);
		sword(a, "s2", "sword", true, false);
		a.end(LifecycleEvents.OUTCOME_WON, "", 26, 20, 5).close(true);
		SyntheticLog b = log("b", 200).start("run-b", 0, "MAGE", RS, 0, 0, false).enter(1);
		sword(b, "s3", "sword", true, true);
		sword(b, "s4", "sword", true, true);
		b.end(LifecycleEvents.OUTCOME_DIED, "Rat", 3, 4, 6).close(true);
		Catalog c = load();
		GroupDetail d = detailOf(c, "weapons", "Sword");
		assertEquals("4", value(d, "Items"));
		assertEquals("2", value(d, "Runs containing it"));
		assertEquals("acquired / seen = 3/4", "75% (3/4)", value(d, "Acquired"));
		assertEquals("equipped / acquired = 2/3", "67% (2/3)", value(d, "Equipped"));
		assertEquals("equipped / seen is its own statistic = 2/4", "50% (2/4)", value(d, "Equipped, of those seen"));
		assertEquals("each run once: 1 of 2", "50% (1/2)", value(d, "Win rate, runs containing it"));
		assertEquals("both runs acquired one", "50% (1/2)", value(d, "Win rate, runs that acquired it"));
		assertEquals("only the run that died equipped one", "0% (0/1)", value(d, "Win rate, runs that equipped it"));
		assertEquals("1", value(d, "Runs won"));
		assertEquals("1", value(d, "Runs died in"));
	}

	@Test
	public void valuesThatWereNeverKnownStayUnknownInTheSheet() throws Exception {
		root = tmp.newFolder("g");
		SyntheticLog a = log("a", 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1);
		sword(a, "s1", "sword", true, false);   //its level was never known
		a.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Catalog c = load();
		GroupDetail d = detailOf(c, "weapons", "Sword");
		assertEquals("?", value(d, "Level, average"));
		assertEquals("?", value(d, "Level, median"));
		assertEquals("0", value(d, "Items with a known level"));
		assertEquals("never equipped, so no time equipped to average", "?", value(d, "Turns equipped, average"));
	}

	// ---- drilling down ----

	private UimsdModel model( Catalog c ){
		UimsdModel m = new UimsdModel(RS, "test");
		m.setCatalog(c);
		return m;
	}

	@Test
	public void aTypeOpensItsInstancesAndTheirRunsAndBack() throws Exception {
		root = tmp.newFolder("h");
		SyntheticLog a = log("a", 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1);
		sword(a, "s1", "sword", true, false);
		sword(a, "s2", "blazing sword", true, true);
		a.registered("d1", "items.weapon.melee.Dagger", 0, false, ItemEvents.ORIGIN_GENERATED).seen("d1", "dagger", WEAPON);
		a.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		SyntheticLog b = log("b", 200).start("run-b", 0, "MAGE", RS, 0, 0, false).enter(1);
		sword(b, "s3", "sword", true, false);
		b.end(LifecycleEvents.OUTCOME_WON, "", 26, 1, 1).close(true);
		Catalog c = load();
		UimsdModel m = model(c);
		m.selectView("weapons");
		assertTrue(m.canOpenGroups());
		GroupRow sword = null;
		for (GroupRow g : m.result().groups) if (g.n == 3) sword = g;
		assertNotNull(sword);

		//type -> instances (the same query without the grouping, with the type as a filter)
		m.openGroupRows(sword);
		assertFalse(m.result().isGrouped());
		assertEquals(3, m.result().rows.size());
		boolean typeFilter = false;
		for (Filter f : m.query().filters) if (f.column.equals("item.type")) typeFilter = true;
		assertTrue("the group's type shows as a filter the player can see and remove", typeFilter);
		assertFalse(m.canOpenGroups());

		//instance -> its run
		Row inst = m.result().rows.get(0);
		LinkDef link = m.dataset().def.links.get(0);
		m.openLink(inst, link);
		assertEquals("runs", m.viewId());
		assertEquals(1, m.result().rows.size());
		assertTrue(m.back());
		assertEquals(3, m.result().rows.size());
		assertTrue(m.back());
		assertTrue("back at the aggregate table", m.result().isGrouped());
		assertEquals(2, m.result().groups.size());

		//type -> the runs containing it -> a run
		assertEquals(2, m.groupRunCount(sword));
		assertEquals(2, m.openGroupRuns(sword));
		assertEquals("runs", m.viewId());
		assertEquals(2, m.result().rows.size());
		assertTrue(m.back());
		assertTrue(m.result().isGrouped());
	}

	@Test
	public void theDrillKeepsTheFiltersThePlayerSet() throws Exception {
		root = tmp.newFolder("i");
		SyntheticLog a = log("a", 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1);
		sword(a, "s1", "sword", true, true);
		sword(a, "s2", "sword", true, false);
		a.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Catalog c = load();
		UimsdModel m = model(c);
		m.selectView("weapons");
		m.addFilter(Filter.num("item.equipped", Op.EQ, 1));
		GroupRow g = m.result().groups.get(0);
		assertEquals(1, g.n);
		m.openGroupRows(g);
		assertEquals("the instances match the group's count, filter included", 1, m.result().rows.size());
	}

	@Test
	public void runsContainingATypeIncludeRunsOutsideTheCurrentScopeSoTheListNeverComesUpEmpty() throws Exception {
		root = tmp.newFolder("j");
		log("a", 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1)
				.registered("s1", "items.weapon.melee.Sword", 1, false, ItemEvents.ORIGIN_GENERATED).seen("s1", "sword", WEAPON)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		log("b", 200).start("run-b", 0, "WARRIOR", "r1-bbbbbbbbbbbb", 5, 30, false).enter(1)
				.registered("s2", "items.weapon.melee.Sword", 1, false, ItemEvents.ORIGIN_GENERATED).seen("s2", "sword", WEAPON)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Catalog c = load();
		UimsdModel m = model(c);
		m.selectView("weapons");
		m.setScope(new Scope(Scope.Rules.ALL, "", false, false, false, false));
		GroupRow g = m.result().groups.get(0);
		assertEquals(2, g.n);
		assertEquals(2, m.openGroupRuns(g));
		assertEquals(2, m.result().rows.size());
	}

	// ---- run facts on the rows ----

	@Test
	public void itemAndEnemyRowsCarryTheirRunsOutcomeAndUnfinishedRunsStayUnknown() throws Exception {
		root = tmp.newFolder("k");
		SyntheticLog a = log("a", 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1);
		sword(a, "s1", "sword", true, false);
		a.heroAttacks("rat", "actors.mobs.Rat", true);
		a.end(LifecycleEvents.OUTCOME_WON, "", 26, 20, 5).close(true);
		SyntheticLog o = log("o", 200).start("run-o", 0, "MAGE", RS, 0, 0, false).enter(1);
		sword(o, "s2", "sword", true, false);
		o.close(false);
		Catalog c = load();
		DatasetDef def = c.dataset("items").def;
		for (Row r : c.dataset("items").rows){
			ColumnDef outcome = def.column("item.runOutcome"), cls = def.column("item.runClass");
			if (r.runId.equals("run-a")){
				assertEquals(1, r.num(outcome.index));
				assertEquals("WARRIOR", r.text(cls.index));
			} else {
				assertFalse("a run that is not over has no outcome yet", r.isKnown(outcome.index));
				assertEquals("MAGE", r.text(cls.index));
			}
		}
		DatasetDef edef = c.dataset("enemies").def;
		Row rat = c.dataset("enemies").rows.get(0);
		assertEquals(1, rat.num(edef.indexOf("enemy.runOutcome")));
	}

	// ---- enemies ----

	@Test
	public void enemiesAreAggregatedByTypeThenEncountersThenRuns() throws Exception {
		root = tmp.newFolder("l");
		for (int i = 0; i < 3; i++){
			SyntheticLog l = log("r" + i, 100 + i).start("run-" + i, 0, "WARRIOR", RS, 0, 0, false).enter(1);
			l.heroAttacks("rat", "actors.mobs.Rat", true).heroAttacks("rat", "actors.mobs.Rat", false).enemyAttacks("rat", "actors.mobs.Rat", true);
			l.damageFromHero("rat", "actors.mobs.Rat", 3).heroKills("rat", "actors.mobs.Rat", 1);
			l.end(i == 0 ? LifecycleEvents.OUTCOME_WON : LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		}
		Catalog c = load();
		QueryResult r = group(c, "enemies", c.view("enemies").initialQuery());
		assertEquals(1, r.groups.size());
		GroupDetail d = GroupDetail.build(r, r.groups.get(0));
		assertEquals("rat", d.title);
		assertEquals("3", value(d, "Encounters"));
		assertEquals("3", value(d, "Runs met in"));
		assertEquals("hits per attack: the hero hit once in two attacks, three times over", "50% (3/6)", value(d, "The hero's hit rate on it"));
		assertEquals("100% (3/3)", value(d, "Its hit rate on the hero"));
		assertEquals("33% (1/3)", value(d, "Win rate, runs it was met in"));
		UimsdModel m = model(c);
		m.selectView("enemies");
		m.openGroupRows(m.result().groups.get(0));
		assertEquals(3, m.result().rows.size());
	}

	// ---- unknown rules ----

	@Test
	public void aRunThatNeverRecordedItsExoticSettingHasUnknownRulesAndIsNeverGivenTodaysSetting() throws Exception {
		root = tmp.newFolder("unk");
		log("a", 100).start("run-known", 0, "WARRIOR", RS, 0, 30, false).enter(1).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		//what the recorder writes for a run saved before the exotic setting was part of the run: no ruleset, no exotic value
		log("b", 200).start("run-old", 0, "MAGE", "", 0, LifecycleEvents.EXOTIC_NOT_RECORDED, false).enter(1).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Catalog c = load();
		Dataset runs = c.dataset(Datasets.RUNS);
		Row known = null, old = null;
		for (Row r : runs.rows){
			if ("run-known".equals(r.runId)) known = r;
			if ("run-old".equals(r.runId)) old = r;
		}
		int exotic = runs.def.indexOf(FactKeys.EXOTIC_CHANCE);
		assertTrue(known.isKnown(exotic));
		assertEquals(30, known.num(exotic));
		assertEquals("not recorded: unknown, not 0 and not the setting of today", false, old.isKnown(exotic));
		assertEquals("no ruleset", null, old.ruleset);
		//it is outside "my rules" and shows up only when unknown rules are asked for
		QueryResult mine = QueryEngine.run(runs, Query.of(Datasets.RUNS), RS);
		assertEquals(1, mine.rows.size());
		QueryResult withUnknown = QueryEngine.run(runs, Query.of(Datasets.RUNS).withScope(Scope.DEFAULT.withUnknownRuleset(true)), RS);
		assertEquals(2, withUnknown.rows.size());
		try {
			SeedFloors.spec(c, old);
			org.junit.Assert.fail("a run without a recorded exotic setting cannot be replayed with a guess");
		} catch (SeedFloors.NotAvailable expected){
			assertTrue(expected.getMessage().contains("not recorded"));
		}
	}

	// ---- hidden information ----

	@Test
	public void openingTheHiddenGroupOfAnUnfinishedRunNeverTurnsItsTrueValueIntoAFilter() throws Exception {
		Catalog c = new DatasetStore(history("leak", "items.rings.RingOfHaste", true)).load();
		UimsdModel m = model(c);
		m.selectView("rings");
		m.setScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
		m.setShowInternal(true);
		m.setGroup(new GroupBy(java.util.Arrays.asList("item.trueClass"), java.util.Arrays.asList(Aggregate.count())));
		assertTrue(m.canOpenGroups());
		int tried = 0;
		for (GroupRow g : m.result().groups){
			m.openGroupRows(g);
			for (Filter f : m.query().filters){
				assertFalse("a hidden value became a filter: " + f, String.valueOf(f).contains("RingOfHaste") || String.valueOf(f).contains("RingOfWealth"));
			}
			while (m.back()) {}
			tried++;
		}
		assertTrue(tried > 0);
	}

	private File history( String name, String secretClass, boolean extraHidden ) throws Exception {
		root = new File(tmp.getRoot(), name);
		SyntheticLog fin = log("fin", 100).start("run-fin", 0, "WARRIOR", RS, 0, 0, false).enter(1);
		sword(fin, "f-sword", "sword", true, true);
		fin.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 2, 100).close(true);
		SyntheticLog open = log("open", 200).start("run-open", 0, "MAGE", RS, 0, 0, false).enter(1);
		open.registered("o-ring", secretClass, 3, true, ItemEvents.ORIGIN_GENERATED).generated("o-ring", GenerationEvents.C_CHEST, "Vault " + secretClass);
		open.seen("o-ring", "ruby ring", ItemEvents.CAT_RING).acquired("o-ring", "ruby ring", ItemEvents.CAT_RING, false, 0).equip("o-ring", "ruby ring", ItemEvents.CAT_RING);
		open.registered("o-sword", "items.weapon.melee.Sword", 5, false, ItemEvents.ORIGIN_GENERATED).seen("o-sword", "sword", WEAPON).acquired("o-sword", "sword", WEAPON, false, 0);
		if (extraHidden){
			open.registered("o-secret", "items.weapon.melee.Sword", 9, false, ItemEvents.ORIGIN_GENERATED).generated("o-secret", GenerationEvents.C_LOCKED_CHEST, "Treasury");
			open.registered("o-secret-2", "items.rings.RingOfWealth", 2, false, ItemEvents.ORIGIN_GENERATED);
			open.mobGenerated("actors.mobs.Goo");
		}
		open.close(false);
		return root;
	}

	private String sheets( Catalog c, boolean show, Scope scope ){
		StringBuilder b = new StringBuilder();
		for (ViewDef v : c.views()){
			Query q = v.initialQuery();
			if (q.group == null) continue;
			QueryResult r = QueryEngine.run(c.dataset(q.dataset), q.withShowInternal(show).withScope(scope), RS);
			b.append(v.id).append(':');
			if (!r.isGrouped()) continue;
			for (GroupRow g : r.groups){
				b.append('[');
				for (ColumnDef k : r.groupKeys) b.append(CellFormatter.cell(r.query, g.sample, k).text).append('/');
				b.append(g.n).append('\n').append(GroupDetail.build(r, g).text()).append(']');
			}
		}
		return b.toString();
	}

	@Test
	public void statisticsSheetsNeverDependOnTheHiddenInformationOfAnUnfinishedRun() throws Exception {
		Catalog a = new DatasetStore(history("a", "items.rings.RingOfHaste", false)).load();
		Catalog b = new DatasetStore(history("b", "items.rings.RingOfElements", true)).load();
		Scope[] scopes = { Scope.DEFAULT.withUnfinished(true), new Scope(Scope.Rules.ALL, "", true, true, true, true) };
		for (Scope s : scopes){
			//with hidden information off the unfinished run contributes only what the player could see, so nothing may differ
			assertEquals("hidden information leaked into a statistics sheet", sheets(a, false, s), sheets(b, false, s));
		}
		//the ring's real kind is hidden while the run is going, even though it is the stable type of the item
		QueryResult r = QueryEngine.run(a.dataset("items"), a.view("rings").initialQuery().withScope(scopes[0]).withShowInternal(true), RS);
		for (GroupRow g : r.groups) assertEquals("~ruby ring", CellFormatter.cell(r.query, g.sample, r.groupKeys.get(0)).text);
	}

	@Test
	public void withHiddenInformationOnTheUnfinishedRunsSheetsStillMatchThoseOfTheOtherHistory() throws Exception {
		Catalog a = new DatasetStore(history("c", "items.rings.RingOfHaste", false)).load();
		Catalog b = new DatasetStore(history("d", "items.rings.RingOfElements", true)).load();
		//with it on, only FINISHED runs may differ; here the finished run is identical in both, so everything must match
		Scope s = new Scope(Scope.Rules.ALL, "", true, true, true, true);
		assertEquals(sheets(a, true, s), sheets(b, true, s));
	}
}
