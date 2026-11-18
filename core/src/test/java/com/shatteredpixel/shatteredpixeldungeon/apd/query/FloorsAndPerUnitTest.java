package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKinds;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.TelemetryVersion;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;

//Floors (every visit, quiet ones included) and the "per run / per floor / per turn" measures that are counted against them.
public class FloorsAndPerUnitTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String RS = "r1-aaaaaaaaaaaa";

	private static final FactKind CAST = FactKinds.register(FactKind.builder("test.pu.cast", "Spell cast").area("Magic").subject("Spell").value("mana", "Mana").build());

	private SyntheticLog log( String id, String runId, String heroClass ){
		return new SyntheticLog(tmp.getRoot(), id, TelemetryVersion.CURRENT, 100).start(runId, 0, heroClass, RS, 0, 30, false);
	}

	private Catalog catalog(){
		return new DatasetStore(tmp.getRoot()).load();
	}

	private static Query all( String dataset ){
		return Query.of(dataset).withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
	}

	private void history(){
		//run 1 (Cleric): floor 1 with three casts and a fight, a quiet floor 2; run 2 (Mage): one floor, one cast
		log("a", "run-a", "CLERIC").enter(1).fact(CAST, "GuidingLight", 3).fact(CAST, "GuidingLight", 3).fact(CAST, "Smite", 5)
				.heroAttacks("rat", "Rat", true).heroAttacks("rat", "Rat", false).damageToHero("rat", "Rat", 3).heal(2, true, "regen").tick(10).leave()
				.enter(2).tick(20).leave().end(LifecycleEvents.OUTCOME_DIED, "rat", 2, 2, 10).close(true);
		log("b", "run-b", "MAGE").enter(1).fact(CAST, "Smite", 2).tick(5).leave().end(LifecycleEvents.OUTCOME_WON, "none", 1, 1, 10).close(true);
	}

	@Test
	public void everyFloorVisitIsARowEvenAQuietOne(){
		history();
		Dataset f = catalog().dataset(EventDatasets.FLOORS);
		assertNotNull(f);
		assertEquals(3, f.rows.size());
		DatasetDef d = f.def;
		Row first = f.rows.get(0), quiet = f.rows.get(1);
		assertEquals(1, first.num(d.indexOf("floor.depth")));
		assertEquals(2, first.num(d.indexOf("floor.attacksByHero")));
		assertEquals(1, first.num(d.indexOf("floor.hitsByHero")));
		assertEquals(3, first.num(d.indexOf("floor.damageTaken")));
		assertEquals(2, first.num(d.indexOf("floor.healed")));
		assertEquals("the casts that happened on the floor, counted from the registry", 3, first.num(d.indexOf(EventDatasets.factCountColumn(CAST))));
		assertEquals("a quiet floor is a row with zeros, not a missing row", 2, quiet.num(d.indexOf("floor.depth")));
		assertEquals(0, quiet.num(d.indexOf("floor.attacksByHero")));
		assertEquals(0, quiet.num(d.indexOf(EventDatasets.factCountColumn(CAST))));
		assertTrue(quiet.num(d.indexOf("floor.turns")) >= 20);
		assertEquals("CLERIC", first.text(d.indexOf("floor.runClass")));
	}

	@Test
	public void eachEventIsARowOfItsOwnDataset(){
		history();
		Catalog c = catalog();
		Dataset attacks = c.dataset(EventDatasets.ATTACKS);
		assertEquals(2, attacks.rows.size());
		int hit = attacks.def.indexOf("attack.hit");
		assertEquals(1, attacks.rows.get(0).num(hit));
		assertEquals(0, attacks.rows.get(1).num(hit));
		assertEquals("Rat", attacks.rows.get(0).text(attacks.def.indexOf("attack.defender")).isEmpty() ? "" : "Rat");
		assertEquals(1, c.dataset(EventDatasets.DAMAGE).rows.size());
		assertEquals(1, c.dataset(EventDatasets.HEALS).rows.size());
		//the true heal source is hidden information
		assertTrue(c.dataset(EventDatasets.HEALS).def.column("heal.source").isInternal());
	}

	@Test
	public void castsPerFloorAreMeasuredAgainstEveryFloorNotJustTheOnesWithCasts(){
		history();
		Catalog c = catalog();
		Dataset casts = c.dataset(FactDatasets.idOf(CAST));
		Query q = all(casts.def.id).withGroup(new GroupBy(Arrays.asList("fact.runClass"), Arrays.asList(Aggregate.count())));
		QueryResult res = QueryEngine.run(casts, q, RS);
		PerUnit.Result perFloor = PerUnit.compute(c, q, res, PerUnit.Unit.FLOOR, RS);
		PerUnit.Result perRun = PerUnit.compute(c, q, res, PerUnit.Unit.RUN, RS);
		int cleric = -1, mage = -1;
		for (int i = 0; i < res.groups.size(); i++){
			String cls = res.groups.get(i).sample.text(casts.def.indexOf("fact.runClass"));
			if (cls.equals("CLERIC")) cleric = i; else mage = i;
		}
		assertEquals(3.0, res.groups.get(cleric).aggregates.get(0).value, 0.0);
		assertEquals("the Cleric visited two floors", 2.0, perFloor.denominators[cleric], 0.0);
		assertEquals(1.5, perFloor.per(3, cleric), 1e-9);
		assertEquals("one Cleric run", 1.0, perRun.denominators[cleric], 0.0);
		assertEquals(3.0, perRun.per(3, cleric), 1e-9);
		assertEquals(1.0, perFloor.denominators[mage], 0.0);
	}

	@Test
	public void keysThatAreNotContextDoNotNarrowTheDenominator(){
		history();
		Catalog c = catalog();
		Dataset casts = c.dataset(FactDatasets.idOf(CAST));
		Query q = all(casts.def.id).withGroup(new GroupBy(Arrays.asList(FactDatasets.SUBJECT), Arrays.asList(Aggregate.count())));
		QueryResult res = QueryEngine.run(casts, q, RS);
		PerUnit.Result perFloor = PerUnit.compute(c, q, res, PerUnit.Unit.FLOOR, RS);
		for (int i = 0; i < res.groups.size(); i++) assertEquals("every spell is measured against the same three floors", 3.0, perFloor.denominators[i], 0.0);
	}

	@Test
	public void contextFiltersNarrowTheDenominatorToo(){
		history();
		Catalog c = catalog();
		Dataset casts = c.dataset(FactDatasets.idOf(CAST));
		Query q = all(casts.def.id).addFilter(Filter.text("fact.runClass", Op.EQ, "CLERIC"))
				.withGroup(new GroupBy(Arrays.asList(FactDatasets.SUBJECT), Arrays.asList(Aggregate.count())));
		QueryResult res = QueryEngine.run(casts, q, RS);
		PerUnit.Result perFloor = PerUnit.compute(c, q, res, PerUnit.Unit.FLOOR, RS);
		for (int i = 0; i < res.groups.size(); i++) assertEquals(2.0, perFloor.denominators[i], 0.0);
	}

	@Test
	public void timePlayedIsUnknownNotZeroWhenItWasNeverRecorded(){
		history();
		Catalog c = catalog();
		Dataset casts = c.dataset(FactDatasets.idOf(CAST));
		Query q = all(casts.def.id).withGroup(new GroupBy(Arrays.asList(FactDatasets.SUBJECT), Arrays.asList(Aggregate.count())));
		QueryResult res = QueryEngine.run(casts, q, RS);
		PerUnit.Result perHour = PerUnit.compute(c, q, res, PerUnit.Unit.HOUR, RS);
		for (double d : perHour.denominators) assertTrue(Double.isNaN(d));
		assertFalse(Double.isNaN(PerUnit.compute(c, q, res, PerUnit.Unit.TURN, RS).denominators[0]));
	}

	@Test
	public void timePlayedOnAFloorComesFromTheRunningTotalAndSurvivesARestart(){
		com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind T = com.shatteredpixel.shatteredpixeldungeon.apd.stats.GameFacts.TIME_ACTIVE;
		log("a", "run-a", "CLERIC").fact(T, "", 0).enter(1).fact(CAST, "Smite", 1).fact(T, "", 5000).leave()
				.enter(2).fact(T, "", 9000).leave()
				.enter(3).fact(T, "", 100).leave()   //the app was closed and opened again: the total started over
				.end(LifecycleEvents.OUTCOME_DIED, "rat", 3, 2, 10).close(true);
		Dataset f = catalog().dataset(EventDatasets.FLOORS);
		int col = f.def.indexOf("floor.activeMillis");
		assertEquals(5000, f.rows.get(0).num(col));
		assertEquals(4000, f.rows.get(1).num(col));
		assertEquals("a restart counts from zero, it is never negative", 100, f.rows.get(2).num(col));
		//and it is the "per hour" denominator
		Dataset casts = catalog().dataset(FactDatasets.idOf(CAST));
		Query q = all(casts.def.id).withGroup(new GroupBy(Arrays.asList(FactDatasets.SUBJECT), Arrays.asList(Aggregate.count())));
		PerUnit.Result perHour = PerUnit.compute(catalog(), q, QueryEngine.run(casts, q, RS), PerUnit.Unit.HOUR, RS);
		assertEquals(9100 / 3_600_000d, perHour.denominators[0], 1e-9);
	}

	@Test
	public void whatWasWornAndForHowLongIsADataset(){
		log("a", "run-a", "WARRIOR").enter(1).equip("w1", "worn shortsword", com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.CAT_WEAPON).tick(40)
				.unequip("w1", "worn shortsword", com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.CAT_WEAPON)
				.equip("a1", "leather armor", com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents.CAT_ARMOR).tick(10).leave()
				.end(LifecycleEvents.OUTCOME_DIED, "rat", 1, 1, 10).close(true);
		Dataset eq = catalog().dataset(EventDatasets.EQUIPMENT);
		assertEquals(2, eq.rows.size());
		DatasetDef d = eq.def;
		Row sword = eq.rows.get(0), armor = eq.rows.get(1);
		assertEquals("worn shortsword", sword.text(d.indexOf("equip.item")));
		assertEquals(0, sword.num(d.indexOf("equip.stillWorn")));
		assertTrue(sword.num(d.indexOf("equip.turns")) >= 40);
		assertEquals("armor still worn when the run ended: a lower bound", 1, armor.num(d.indexOf("equip.stillWorn")));
		assertEquals(com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact.PROV_INCOMPLETE, armor.prov(d.indexOf("equip.turns")));
	}
}
