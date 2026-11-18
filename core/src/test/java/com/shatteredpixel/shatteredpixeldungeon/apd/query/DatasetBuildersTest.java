package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogWriter;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DatasetBuildersTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String RS = "r1-aaaaaaaaaaaa";
	private static final TelemetryVersion V14 = new TelemetryVersion(1, 4);

	private File root(){
		return tmp.getRoot();
	}

	private SyntheticLog log( String id, TelemetryVersion v, long createdAt ){
		return new SyntheticLog(root(), id, v, createdAt);
	}

	private Catalog load(){
		return new DatasetStore(root()).load();
	}

	private static Row find( Dataset ds, String idContains ){
		for (Row r : ds.rows) if (r.id.contains(idContains)) return r;
		return null;
	}

	private static long num( Dataset ds, Row r, String col ){
		return r.num(ds.def.indexOf(col));
	}

	private static boolean known( Dataset ds, Row r, String col ){
		return r.isKnown(ds.def.indexOf(col));
	}

	private static int prov( Dataset ds, Row r, String col ){
		return r.prov(ds.def.indexOf(col));
	}

	private static String text( Dataset ds, Row r, String col ){
		return r.text(ds.def.indexOf(col));
	}

	//---- runs ----

	@Test
	public void aRunBecomesOneRowWithItsFacts(){
		log("a", V14, 100).start("run-a", 0, "WARRIOR", RS, 0, 30, false).enter(1).heroAttacks("rat", "Rat", true).enter(2)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 3, 5, 1234).close(true);
		Dataset runs = load().dataset(Datasets.RUNS);
		assertEquals(1, runs.rows.size());
		Row r = runs.rows.get(0);
		assertEquals("WARRIOR", text(runs, r, FactKeys.HERO_CLASS));
		assertEquals(1234, num(runs, r, FactKeys.SCORE));
		assertEquals(LifecycleEvents.OUTCOME_DIED, num(runs, r, FactKeys.OUTCOME));
		assertEquals(1, num(runs, r, "run.source"));
		assertEquals(1, num(runs, r, FactKeys.HERO_ATTACKS));
		assertTrue(r.finished);
		assertEquals(RS, r.ruleset);
		assertTrue(r.publicRow);
		assertEquals("seed is hidden information", true, runs.def.column(FactKeys.SEED).isInternal());
	}

	@Test
	public void whatAnOlderTelemetryVersionCouldNotRecordIsUnknownNotZero(){
		log("old", new TelemetryVersion(1, 1), 100).start("run-old", 0, "MAGE", RS, 0, 0, false).enter(1)
				.end(LifecycleEvents.OUTCOME_WON, "", 26, 20, 9999).close(true);
		Dataset runs = load().dataset(Datasets.RUNS);
		Row r = runs.rows.get(0);
		assertFalse(known(runs, r, FactKeys.HERO_ATTACKS));
		assertFalse(known(runs, r, FactKeys.GEN_ITEMS));
		assertTrue(known(runs, r, FactKeys.SCORE));
	}

	@Test
	public void theSegmentsOfOneRunAreMergedAndATotalFromAnOlderSegmentIsALowerBound(){
		//segment 0 was recorded by telemetry 1.1 (no combat), segment 1 by 1.4
		log("s0", new TelemetryVersion(1, 1), 100).start("run-x", 0, "ROGUE", RS, 0, 0, false).enter(1).close(false);
		log("s1", V14, 200).start("run-x", 1, "ROGUE", RS, 0, 0, false).enter(2).heroAttacks("rat", "Rat", true).heroAttacks("rat", "Rat", false)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 2, 4, 500).close(true);
		Dataset runs = load().dataset(Datasets.RUNS);
		assertEquals("one logical run", 1, runs.rows.size());
		Row r = runs.rows.get(0);
		assertEquals(2, num(runs, r, "run.segments"));
		assertEquals(2, num(runs, r, FactKeys.HERO_ATTACKS));
		assertEquals("the first segment could not count attacks: the total is only a lower bound", Fact.PROV_INCOMPLETE, prov(runs, r, FactKeys.HERO_ATTACKS));
		assertEquals(500, num(runs, r, FactKeys.SCORE));
		assertEquals(Fact.PROV_RECORDED, prov(runs, r, FactKeys.SCORE));
		assertEquals("1.1+1.4", r.telemetry);
		assertTrue("the newest segment ended", r.finished);
		assertEquals(2, num(runs, r, FactKeys.LEVELS_ENTERED));
	}

	@Test
	public void aRunWhoseNewestSegmentIsStillOpenIsNotFinished(){
		log("s0", V14, 100).start("run-y", 0, "ROGUE", RS, 0, 0, false).enter(1).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		log("s1", V14, 200).start("run-y", 1, "ROGUE", RS, 0, 0, false).enter(1).close(false);   //play went on after the end was undone
		Dataset runs = load().dataset(Datasets.RUNS);
		assertEquals(1, runs.rows.size());
		assertFalse("hidden information of this run must stay hidden", runs.rows.get(0).finished);
	}

	@Test
	public void equalTimestampBoundarySegmentNumbersUseNumericOrder(){
		log("zero", V14, 100).start("run-extreme", 0, "ROGUE", RS, 0, 0, false).enter(1)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		log("max", V14, 100).start("run-extreme", Integer.MAX_VALUE, "ROGUE", RS, 0, 0, false).enter(2).close(false);
		Dataset runs = load().dataset(Datasets.RUNS);
		assertEquals("both segments are one run", 1, runs.rows.size());
		assertFalse("the highest valid same-time segment is newest and still open", runs.rows.get(0).finished);
		assertEquals(2, num(runs, runs.rows.get(0), FactKeys.LEVELS_ENTERED));
	}

	@Test
	public void aRulesetChangeDuringARunMakesItMixedNotCurrent(){
		log("a", V14, 100).start("run-a", 0, "WARRIOR", RS, 0, 30, false).enter(1).rulesetChange(RS, "r1-bbbbbbbbbbbb")
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		log("b", V14, 200).start("run-b", 0, "WARRIOR", RS, 0, 30, false).enter(1).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Dataset runs = load().dataset(Datasets.RUNS);
		Row a = find(runs, "a"), b = find(runs, "b");
		assertEquals(Row.RULESET_MIXED, a.ruleset);
		assertEquals(RS, b.ruleset);
		QueryResult r = QueryEngine.run(runs, Query.of("runs"), RS);
		assertEquals(1, r.rows.size());
		assertEquals(1, r.stats.mixedRuleset);
	}

	@Test
	public void aLogThatCannotBeReadStillGivesARowThatSaysSo() throws Exception {
		File f = new File(new File(root(), "runs"), "junk.apdlog");
		f.getParentFile().mkdirs();
		java.io.FileOutputStream out = new java.io.FileOutputStream(f);
		out.write(new byte[]{1, 2, 3, 4, 5});
		out.close();
		Dataset runs = load().dataset(Datasets.RUNS);
		assertEquals(1, runs.rows.size());
		assertEquals("unreadable", text(runs, runs.rows.get(0), FactKeys.STATUS));
		assertFalse(runs.rows.get(0).finished);
		assertNull("an unreadable log has an unknown ruleset, never the current one", runs.rows.get(0).ruleset);
	}

	//---- items ----

	@Test
	public void itemsKeepWhatThePlayerKnewAndSeparateWhatTheyDidNot(){
		SyntheticLog l = log("a", V14, 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1);
		//a sword the player found and wielded, a ring left lying, and a potion on a floor the player never explored
		l.registered("sword", "items.weapon.melee.Sword", 2, true, ItemEvents.ORIGIN_GENERATED).generated("sword", GenerationEvents.C_HEAP, "Room 3");
		l.registered("ring", "items.rings.RingOfWealth", -1, false, ItemEvents.ORIGIN_GENERATED);
		l.registered("potion", "items.potions.PotionOfHealing", 0, false, ItemEvents.ORIGIN_GENERATED).enter(2);
		l.seen("sword", "sword", ItemEvents.CAT_WEAPON).seen("ring", "ruby ring", ItemEvents.CAT_RING)
				.acquired("sword", "sword", ItemEvents.CAT_WEAPON, false, 0).equip("sword", "sword", ItemEvents.CAT_WEAPON).tick(50)
				.enter(3).tick(100).unequip("sword", "sword", ItemEvents.CAT_WEAPON).truthChanged("sword", 3, true)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 3, 4, 100).close(true);
		Dataset items = load().dataset(Datasets.ITEMS);
		assertEquals(3, items.rows.size());
		Row sword = find(items, "/sword"), ring = find(items, "/ring"), potion = find(items, "/potion");

		assertTrue(sword.publicRow);
		assertEquals(Datasets.FATE_EQUIPPED, num(items, sword, "item.fate"));
		assertEquals(1, num(items, sword, "item.equipCount"));
		assertEquals("equipped from the equip event to the unequip event (each event is one clock step)", 152, num(items, sword, "item.turnsEquipped"));
		assertEquals("floors: the one it was equipped on and the one entered while equipped", 2, num(items, sword, "item.floorsEquipped"));
		assertEquals(2, num(items, sword, "item.firstSeenDepth"));
		assertFalse("the player never knew the level", known(items, sword, "item.visibleLevel"));
		//hidden truth is recorded, in hidden columns
		assertEquals("items.weapon.melee.Sword", text(items, sword, "item.trueClass"));
		assertEquals("the latest true level", 3, num(items, sword, "item.trueLevel"));
		assertEquals(1, num(items, sword, "item.cursed"));
		assertEquals("Room 3", text(items, sword, "item.room"));
		assertTrue(items.def.column("item.trueClass").isInternal());

		assertTrue(ring.publicRow);
		assertEquals("a ring the player saw and left behind", Datasets.FATE_LEFT_BEHIND, num(items, ring, "item.fate"));
		assertEquals("ruby ring", text(items, ring, "item.name"));   //the name the player knew, not the true class

		assertFalse("an item only the generator knew about is not a public row", potion.publicRow);
		assertEquals(Datasets.FATE_NEVER_SEEN, num(items, potion, "item.fate"));
		assertFalse(known(items, potion, "item.name"));
	}

	@Test
	public void timeEquippedInARunStillGoingIsALowerBound(){
		log("a", V14, 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1).acquired("axe", "axe", ItemEvents.CAT_WEAPON, false, 0)
				.equip("axe", "axe", ItemEvents.CAT_WEAPON).tick(40).close(false);
		Dataset items = load().dataset(Datasets.ITEMS);
		Row axe = items.rows.get(0);
		assertEquals(Fact.PROV_INCOMPLETE, prov(items, axe, "item.turnsEquipped"));
		assertFalse(axe.finished);
	}

	@Test
	public void aHiddenEventLaterThanAnythingVisibleNeverMovesAPublicNumber(){
		File a = new File(root(), "a"), b = new File(root(), "b");
		new SyntheticLog(a, "x", V14, 100).start("run-x", 0, "WARRIOR", RS, 0, 0, false).enter(1)
				.acquired("axe", "axe", ItemEvents.CAT_WEAPON, false, 0).equip("axe", "axe", ItemEvents.CAT_WEAPON).tick(40).close(false);
		new SyntheticLog(b, "x", V14, 100).start("run-x", 0, "WARRIOR", RS, 0, 0, false).enter(1)
				.acquired("axe", "axe", ItemEvents.CAT_WEAPON, false, 0).equip("axe", "axe", ItemEvents.CAT_WEAPON).tick(40).hiddenAt(900).close(false);
		Dataset ia = new DatasetStore(a).load().dataset(Datasets.ITEMS), ib = new DatasetStore(b).load().dataset(Datasets.ITEMS);
		assertEquals(num(ia, ia.rows.get(0), "item.turnsEquipped"), num(ib, ib.rows.get(0), "item.turnsEquipped"));
	}

	@Test
	public void rowsAreListedInTheOrderThePlayerMetThemNotTheOrderTheGeneratorMadeThem(){
		log("a", V14, 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1)
				.registered("first-made", "items.A", 0, false, ItemEvents.ORIGIN_GENERATED)
				.registered("second-made", "items.B", 0, false, ItemEvents.ORIGIN_GENERATED)
				.seen("second-made", "b-item", ItemEvents.CAT_POTION).seen("first-made", "a-item", ItemEvents.CAT_POTION)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Dataset items = load().dataset(Datasets.ITEMS);
		assertEquals("b-item", text(items, items.rows.get(0), "item.name"));
		assertEquals("a-item", text(items, items.rows.get(1), "item.name"));
	}

	@Test
	public void aKnownLevelIsKeptAndNeverRevealedAfterwards(){
		log("a", V14, 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1).acquired("ring", "ring of haste +2", ItemEvents.CAT_RING, true, 2)
				.registered("ring", "items.rings.RingOfHaste", 2, false, ItemEvents.ORIGIN_OBSERVED)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Dataset items = load().dataset(Datasets.ITEMS);
		assertEquals(2, num(items, items.rows.get(0), "item.visibleLevel"));
	}

	@Test
	public void telemetryThatPredatesItemsGivesNoItemRowsAndTheRunIsNotCountedAsCovered(){
		log("old", new TelemetryVersion(1, 1), 100).start("run-old", 0, "MAGE", RS, 0, 0, false).enter(1).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		log("new", V14, 200).start("run-new", 0, "MAGE", RS, 0, 0, false).enter(1).acquired("x", "x", ItemEvents.CAT_FOOD, false, 0)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Dataset items = load().dataset(Datasets.ITEMS);
		assertEquals(1, items.rows.size());
		QueryResult r = QueryEngine.run(items, Query.of("items"), RS);
		assertEquals(2, r.stats.runsInScope);
		assertEquals("only one of the two runs can have item rows at all", 1, r.stats.runsCovered);
	}

	@Test
	public void theSameItemInTwoSegmentsIsOneRowWithMergedNumbers(){
		log("s0", V14, 100).start("run-z", 0, "WARRIOR", RS, 0, 0, false).enter(1).acquired("sword", "sword", ItemEvents.CAT_WEAPON, false, 0)
				.equip("sword", "sword", ItemEvents.CAT_WEAPON).tick(30).unequip("sword", "sword", ItemEvents.CAT_WEAPON).close(false);
		log("s1", V14, 200).start("run-z", 1, "WARRIOR", RS, 0, 0, false).enter(1).equip("sword", "sword", ItemEvents.CAT_WEAPON).tick(20)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Dataset items = load().dataset(Datasets.ITEMS);
		assertEquals(1, items.rows.size());
		Row r = items.rows.get(0);
		assertEquals(2, num(items, r, "item.equipCount"));
		assertEquals("31 in the first segment, 21 up to the end of the second", 52, num(items, r, "item.turnsEquipped"));
		assertEquals(Datasets.FATE_EQUIPPED, num(items, r, "item.fate"));
		assertTrue(r.finished);
	}

	//---- enemies ----

	@Test
	public void enemiesAreCountedFromWhatThePlayerSawAndTheirTrueClassStaysHidden(){
		SyntheticLog l = log("a", V14, 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1)
				.mobGenerated("actors.mobs.Rat").mobGenerated("actors.mobs.Rat").mobGenerated("actors.mobs.Gnoll");
		l.heroAttacks("rat", "actors.mobs.Rat", true).damageFromHero("rat", "actors.mobs.Rat", 4).heroKills("rat", "actors.mobs.Rat", 1)
				.enemyAttacks("rat", "actors.mobs.Rat", true).damageToHero("rat", "actors.mobs.Rat", 2).enemyAttacks("rat", "actors.mobs.Rat", false)
				.heroDies("rat", CombatEvents.SRC_CHAR).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		Dataset en = load().dataset(Datasets.ENEMIES);
		assertEquals("the rat the player fought, and the gnoll nobody met", 2, en.rows.size());
		Row rat = find(en, "|rat"), gnoll = find(en, "class:actors.mobs.Gnoll");
		assertTrue(rat.publicRow);
		assertEquals("rat", text(en, rat, "enemy.name"));
		assertEquals(1, num(en, rat, "enemy.attacksByHero"));
		assertEquals(2, num(en, rat, "enemy.attacksOnHero"));
		assertEquals(1, num(en, rat, "enemy.hitsOnHero"));
		assertEquals(4, num(en, rat, "enemy.damageFromHero"));
		assertEquals(2, num(en, rat, "enemy.damageToHero"));
		assertEquals(1, num(en, rat, "enemy.kills"));
		assertEquals(1, num(en, rat, "enemy.heroDeaths"));
		assertEquals(2, num(en, rat, "enemy.spawned"));
		assertEquals("actors.mobs.Rat", text(en, rat, "enemy.class"));
		assertTrue(en.def.column("enemy.class").isInternal());
		assertFalse("an enemy only the generator knew about is not a public row", gnoll.publicRow);
		assertEquals(1, num(en, gnoll, "enemy.spawned"));
		assertEquals("never met means zero fights, and that is recorded", 0, num(en, gnoll, "enemy.attacksOnHero"));
	}

	@Test
	public void enemyRowsDoNotExistForRunsRecordedBeforeCombat(){
		log("old", new TelemetryVersion(1, 2), 100).start("run-old", 0, "MAGE", RS, 0, 0, false).enter(1).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		assertEquals(0, load().dataset(Datasets.ENEMIES).rows.size());
	}

	//---- deaths ----

	@Test
	public void deathsRecordEachDeathAndWhichOneEndedTheRun(){
		log("a", V14, 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(2).heroDies("rat", CombatEvents.SRC_CHAR).revived()
				.enter(3).heroDies("poison", CombatEvents.SRC_BUFF).end(LifecycleEvents.OUTCOME_DIED, "Poison", 3, 6, 700).close(true);
		Dataset d = load().dataset(Datasets.DEATHS);
		assertEquals(2, d.rows.size());
		Row first = d.rows.get(0), second = d.rows.get(1);
		assertEquals(1, num(d, first, "death.revived"));
		assertEquals(0, num(d, first, "death.final"));
		assertEquals(2, num(d, first, "death.depth"));
		assertEquals(0, num(d, second, "death.revived"));
		assertEquals(1, num(d, second, "death.final"));
		assertEquals("poison", text(d, second, "death.cause"));
		assertEquals(CombatEvents.SRC_BUFF, num(d, second, "death.causeKind"));
		assertEquals(6, num(d, second, "death.heroLevel"));
	}

	@Test
	public void aRunThatEndedInDeathBeforeDeathEventsExistStillHasADeathRowWithWhatIsKnown(){
		log("old", new TelemetryVersion(1, 1), 100).start("run-old", 0, "MAGE", RS, 0, 0, false).enter(1)
				.end(LifecycleEvents.OUTCOME_DIED, "Gnoll", 4, 7, 300).close(true);
		Dataset d = load().dataset(Datasets.DEATHS);
		assertEquals(1, d.rows.size());
		Row r = d.rows.get(0);
		assertEquals("Gnoll", text(d, r, "death.cause"));
		assertFalse("older telemetry cannot say whether the hero was revived", known(d, r, "death.revived"));
		assertEquals(1, num(d, r, "death.final"));
	}

	@Test
	public void aWonOrAbandonedRunHasNoDeathRow(){
		log("w", V14, 100).start("run-w", 0, "MAGE", RS, 0, 0, false).enter(1).end(LifecycleEvents.OUTCOME_WON, "", 26, 25, 99999).close(true);
		log("x", V14, 200).start("run-x", 0, "MAGE", RS, 0, 0, false).enter(1).end(LifecycleEvents.OUTCOME_ABANDONED, "", 1, 1, 1).close(true);
		assertEquals(0, load().dataset(Datasets.DEATHS).rows.size());
	}

	//---- imported ranking records ----

	private void importFile( String name, long created, LegacyEvents.Record... records ) throws Exception {
		File f = new File(new File(root(), "legacy"), name + ".apdlog");
		RunLogWriter w = RunLogWriter.create(f, TelemetryVersion.CURRENT, name, "test", created);
		long c = 0;
		for (LegacyEvents.Record r : records) LegacyEvents.emitRecord(w, c++, r);
		w.close(true);
	}

	private LegacyEvents.Record rec( String id, String cls, int outcome, int deepest, int level, long score, String date ){
		LegacyEvents.Record r = new LegacyEvents.Record();
		r.gameId = id;
		r.putText(FactKeys.HERO_CLASS, cls);
		r.putNum(FactKeys.OUTCOME, outcome);
		r.putNum(FactKeys.DEEPEST, deepest);
		r.putNum(FactKeys.HERO_LEVEL, level);
		r.putNum(FactKeys.SCORE, score);
		if (date != null) r.putText(FactKeys.DATE, date); else r.putUnknown(FactKeys.DATE);
		r.putUnknown("legacy.itemsCrafted");
		r.putNum("legacy.enemiesSlain", 0);
		return r;
	}

	@Test
	public void legacyDatesRejectImpossibleCalendarDaysButKeepRealLeapDays(){
		assertEquals(Long.MIN_VALUE, RowBuilders.parseDate("2025-02-29"));
		assertEquals(Long.MIN_VALUE, RowBuilders.parseDate("2024-04-31"));
		assertEquals(Long.MIN_VALUE, RowBuilders.parseDate("1900-02-29"));
		assertTrue(RowBuilders.parseDate("2024-02-29") != Long.MIN_VALUE);
		assertTrue(RowBuilders.parseDate("2000-02-29") != Long.MIN_VALUE);
	}

	@Test
	public void importedRecordsKeepUnknownFieldsUnknownAndHaveAnUnknownRuleset() throws Exception {
		importFile("legacy-import-1", 100, rec("g1", "MAGE", 0, 4, 7, 900, "2025-03-04"), rec("g2", "MAGE", 1, 26, 24, 5000, null));
		Dataset runs = load().dataset(Datasets.RUNS);
		assertEquals(2, runs.rows.size());
		Row g1 = find(runs, "g1"), g2 = find(runs, "g2");
		assertEquals(2, num(runs, g1, "run.source"));
		assertEquals("2025-03-04", CellFormatter.date(num(runs, g1, FactKeys.START_MILLIS)));
		assertEquals(Fact.PROV_LEGACY, prov(runs, g1, FactKeys.START_MILLIS));
		assertFalse("a record without a date has an unknown start", known(runs, g2, FactKeys.START_MILLIS));
		assertFalse(known(runs, g1, "legacy.itemsCrafted"));
		assertTrue("a recorded zero stays a zero", known(runs, g1, "legacy.enemiesSlain"));
		assertEquals(0, num(runs, g1, "legacy.enemiesSlain"));
		assertNull(g1.ruleset);
		assertTrue(g1.finished);
		assertTrue(g1.imported);
		//hidden by default: the ruleset is unknown, and the count says so
		QueryResult r = QueryEngine.run(runs, Query.of("runs"), RS);
		assertEquals(0, r.rows.size());
		assertEquals(2, r.stats.unknownRuleset);
		assertEquals(2, QueryEngine.run(runs, Query.of("runs").withScope(Scope.DEFAULT.withUnknownRuleset(true)), RS).rows.size());
	}

	@Test
	public void anImportedRecordThatIsTheSameRunAsARecordedOneIsNotCountedTwice() throws Exception {
		log("a", V14, 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1).rankingLink("g-exact")
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 5, 8, 2000).close(true);
		log("b", V14, 200).start("run-b", 0, "ROGUE", RS, 0, 0, false).enter(1).end(LifecycleEvents.OUTCOME_DIED, "Rat", 3, 6, 800).close(true);
		importFile("legacy-import-1", 300, rec("g-exact", "WARRIOR", 0, 5, 8, 2000, "2025-01-01"),   //exact link
				rec("g-probable", "ROGUE", 0, 3, 6, 800, "2025-01-02"),                                   //one run fits: probable link
				rec("g-unrelated", "HUNTRESS", 0, 9, 9, 9, "2025-01-03"));
		Dataset runs = load().dataset(Datasets.RUNS);
		assertEquals(5, runs.rows.size());
		assertTrue(find(runs, "g-exact").duplicate);
		assertTrue(find(runs, "g-probable").duplicate);
		assertFalse(find(runs, "g-unrelated").duplicate);
		QueryResult r = QueryEngine.run(runs, Query.of("runs").withScope(Scope.DEFAULT.withUnknownRuleset(true)), RS);
		assertEquals("two recorded runs and the unrelated record; the two duplicates are counted once", 3, r.rows.size());
		assertEquals(2, r.stats.duplicates);
	}

	@Test
	public void twoIdenticalLookingRunsMakeARecordAmbiguousSoItIsNotLinked() throws Exception {
		for (String id : new String[]{"a", "b"}){
			log(id, V14, id.equals("a") ? 100 : 200).start("run-" + id, 0, "WARRIOR", RS, 0, 0, false).enter(1)
					.end(LifecycleEvents.OUTCOME_DIED, "Rat", 5, 8, 2000).close(true);
		}
		importFile("legacy-import-1", 300, rec("g1", "WARRIOR", 0, 5, 8, 2000, "2025-01-01"));
		assertFalse(find(load().dataset(Datasets.RUNS), "g1").duplicate);
	}


 @Test
 public void aReopenedRunCannotHideAnImportedFinishedRunByProbableMatch() throws Exception {
  log("ended", V14, 100).start("run-reopened",0,"WARRIOR",RS,0,0,false).enter(1)
   .end(LifecycleEvents.OUTCOME_DIED,"Rat",5,8,2000).close(true);
  log("resumed", V14, 200).start("run-reopened",1,"WARRIOR",RS,0,0,false).enter(1).close(false);
  importFile("legacy-import-1",300,rec("g-reopened","WARRIOR",0,5,8,2000,"2025-01-01"));
  Dataset runs=load().dataset(Datasets.RUNS);
  Row reopened=null;
  for(Row candidate:runs.rows) if("run-reopened".equals(candidate.runId)) reopened=candidate;
  assertNotNull(reopened);
  assertFalse(reopened.finished);
  assertFalse("unfinished run cannot be a probable match for a completed ranking",find(runs,"g-reopened").duplicate);
  Dataset cached=load().dataset(Datasets.RUNS);
  assertFalse("cache reload must preserve the imported ranking",find(cached,"g-reopened").duplicate);
 }

	//---- the row cache ----

	private static String signature( Dataset d ){
		StringBuilder b = new StringBuilder();
		for (Row r : d.rows){
			b.append(r.id).append('|').append(r.mergeKey).append('|').append(r.runId).append('|').append(r.finished).append(r.publicRow)
					.append(r.imported).append(r.duplicate).append('|').append(r.ruleset).append('|').append(r.telemetry).append('[');
			for (int i = 0; i < d.def.size(); i++){
				b.append(r.kind(i)).append(':').append(r.prov(i)).append(':').append(r.asText(i)).append(',');
			}
			b.append(']');
		}
		return b.toString();
	}

	private static String signature( Catalog c ){
		StringBuilder b = new StringBuilder();
		for (Dataset d : c.datasets()) b.append(d.def.id).append('=').append(signature(d)).append(d.runs.size()).append(';');
		return b.toString();
	}

	private void someHistory() throws Exception {
		log("a", V14, 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1).mobGenerated("actors.mobs.Rat").registered("sword", "S", 1, false, 7)
				.seen("sword", "sword", ItemEvents.CAT_WEAPON).acquired("sword", "sword", ItemEvents.CAT_WEAPON, false, 0)
				.equip("sword", "sword", ItemEvents.CAT_WEAPON).heroAttacks("rat", "actors.mobs.Rat", true).tick(10).heroDies("rat", 0)
				.end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 10).close(true);
		log("b", V14, 200).start("run-b", 0, "MAGE", RS, 0, 0, false).enter(1).close(false);
		importFile("legacy-import-1", 300, rec("g1", "MAGE", 0, 4, 7, 900, "2025-03-04"));
	}

	@Test
	public void theCacheIsUsedWhileCurrentAndGivesIdenticalRows() throws Exception {
		someHistory();
		DatasetStore store = new DatasetStore(root());
		Catalog first = store.load();
		assertEquals(3, first.logsRebuilt);
		assertEquals(0, first.logsCached);
		Catalog second = store.load();
		assertEquals(0, second.logsRebuilt);
		assertEquals(3, second.logsCached);
		assertEquals(signature(first), signature(second));
	}

	@Test
	public void aDeletedOrDamagedCacheIsRebuiltIdentically() throws Exception {
		someHistory();
		DatasetStore store = new DatasetStore(root());
		String original = signature(store.load());
		store.deleteCache();
		Catalog rebuilt = store.load();
		assertEquals(3, rebuilt.logsRebuilt);
		assertEquals(original, signature(rebuilt));
		//damage one cache file, truncate another
		File[] caches = store.rowsDir().listFiles((d, n) -> n.endsWith(".apdr"));
		assertEquals(3, caches.length);
		java.io.FileOutputStream out = new java.io.FileOutputStream(caches[0]);
		out.write(new byte[]{9, 9, 9});
		out.close();
		byte[] whole = java.nio.file.Files.readAllBytes(caches[1].toPath());
		out = new java.io.FileOutputStream(caches[1]);
		out.write(whole, 0, whole.length / 2);
		out.close();
		Catalog repaired = store.load();
		assertEquals(2, repaired.logsRebuilt);
		assertEquals(1, repaired.logsCached);
		assertEquals(original, signature(repaired));
	}

	@Test
	public void aLogThatGrewIsTheOnlyOneRebuilt() throws Exception {
		someHistory();
		DatasetStore store = new DatasetStore(root());
		store.load();
		//the open run goes on: reopen its log and add events
		File b = new File(new File(root(), "runs"), "b.apdlog");
		RunLogWriter w = RunLogWriter.openForAppend(b, V14);
		LifecycleEvents.LevelEnter l = new LifecycleEvents.LevelEnter();
		l.depth = 2;
		LifecycleEvents.emitLevelEnter(w, 50, l);
		w.close(false);
		Catalog c = store.load();
		assertEquals(1, c.logsRebuilt);
		assertEquals(2, c.logsCached);
	}

	@Test
	public void deletingTheRawLogLosesTheRowsEvenThoughTheCacheRemains() throws Exception {
		someHistory();
		DatasetStore store = new DatasetStore(root());
		store.load();
		assertTrue(new File(new File(root(), "runs"), "a.apdlog").delete());
		Dataset runs = store.load().dataset(Datasets.RUNS);
		assertNull("the cache is not a second source of truth", find(runs, "run-a"));
	}

	//---- views ----

	@Test
	public void everyShippedViewRunsWithoutProblemsOnRealRows() throws Exception {
		someHistory();
		Catalog cat = load();
		//the hand-made views, plus a table for every generated dataset that has rows (the play-by-play ones and each registered kind of fact)
		int generated = 0;
		for (Dataset d : cat.datasets()) if (!d.rows.isEmpty() && (EventDatasets.isEventDataset(d.def.id) || FactDatasets.isFactDataset(d.def.id))) generated++;
		assertEquals(13 + generated, cat.views().size());
		for (ViewDef v : cat.views()){
			Dataset ds = cat.datasetOf(v);
			assertNotNull(v.id, ds);
			QueryResult r = QueryEngine.run(ds, v.initialQuery(), RS);
			assertTrue(v.id + ": " + r.problems, r.problems.isEmpty());
		}
	}

	@Test
	public void weaponsIsAnItemsViewAndCharactersIsARunsGrouping() throws Exception {
		someHistory();
		Catalog cat = load();
		//weapons start as one row per kind of weapon, and the individual weapons are one level down
		QueryResult weapons = QueryEngine.run(cat.dataset(Datasets.ITEMS), cat.view("weapons").initialQuery(), RS);
		assertTrue(weapons.isGrouped());
		assertEquals(1, weapons.groups.size());
		QueryResult individual = QueryEngine.run(cat.dataset(Datasets.ITEMS), cat.view("weapons").instanceQuery(), RS);
		assertEquals(1, individual.rows.size());
		QueryResult armor = QueryEngine.run(cat.dataset(Datasets.ITEMS), cat.view("armor").instanceQuery(), RS);
		assertEquals(0, armor.rows.size());
		Query chars = cat.view("characters").initialQuery().withMinSample(0).withScope(Scope.DEFAULT.withUnfinished(true));
		QueryResult g = QueryEngine.run(cat.dataset(Datasets.RUNS), chars, RS);
		assertTrue(g.isGrouped());
		assertEquals(2, g.groups.size());
	}

	@Test
	public void theChallengesViewReadsAllRulesetsButGroupsByWhatMakesARuleset() throws Exception {
		log("a", V14, 100).start("run-a", 0, "WARRIOR", RS, 0, 0, false).enter(1).end(LifecycleEvents.OUTCOME_DIED, "Rat", 1, 1, 1).close(true);
		log("b", V14, 200).start("run-b", 0, "WARRIOR", "r1-bbbbbbbbbbbb", 5, 30, false).enter(1).end(LifecycleEvents.OUTCOME_WON, "", 26, 25, 9).close(true);
		Catalog cat = load();
		Query q = cat.view("challenges").initialQuery().withMinSample(0);
		assertEquals(Scope.Rules.ALL, q.scope.rules);
		QueryResult g = QueryEngine.run(cat.dataset(Datasets.RUNS), q, RS);
		assertEquals("two different challenge setups are two groups, never averaged together", 2, g.groups.size());
		assertEquals(2, cat.rulesets.size());
	}

	@Test
	public void anEmptyHistoryGivesEmptyDatasets(){
		Catalog cat = load();
		for (Dataset d : cat.datasets()) assertEquals(0, d.rows.size());
		assertEquals(13, cat.views().size());
	}
}
