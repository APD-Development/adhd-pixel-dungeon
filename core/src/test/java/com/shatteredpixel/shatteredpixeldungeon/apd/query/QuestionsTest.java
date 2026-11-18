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
import java.util.List;

//The question builder's engine: a question in the player's words becomes a query and an answer, and an unforeseen question about any
//recorded fact is answered by the same engine, with no code written for that question.
public class QuestionsTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String RS = "r1-aaaaaaaaaaaa";

	private static final FactKind CAST = FactKinds.register(FactKind.builder("test.q.cast", "Spell cast").area("Magic").subject("Spell").value("mana", "Mana spent").build());

	private SyntheticLog log( String id, String runId, String heroClass ){
		return new SyntheticLog(tmp.getRoot(), id, TelemetryVersion.CURRENT, 100).start(runId, 0, heroClass, RS, 0, 30, false);
	}

	private Catalog history(){
		log("a", "run-a", "CLERIC").enter(1).fact(CAST, "GuidingLight", 3).fact(CAST, "GuidingLight", 5).fact(CAST, "Smite", 8).tick(10).leave()
				.enter(2).tick(10).leave().end(LifecycleEvents.OUTCOME_DIED, "rat", 2, 2, 10).close(true);
		log("b", "run-b", "MAGE").enter(1).fact(CAST, "Smite", 2).tick(5).leave().end(LifecycleEvents.OUTCOME_WON, "none", 1, 1, 10).close(true);
		return new DatasetStore(tmp.getRoot()).load();
	}

	private static Question ask( String id ){
		return Question.about(id).withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
	}

	@Test
	public void aCountIsAnsweredInOneLine(){
		Catalog c = history();
		Questions.Answer a = Questions.answer(c, ask(FactDatasets.idOf(CAST)), RS);
		assertEquals(1, a.lines.size());
		assertEquals("4", a.lines.get(0).value);
		assertTrue(a.question.startsWith("How many spell casts"));
	}

	@Test
	public void anyRecordedColumnCanBeAverageGroupedAndFiltered(){
		Catalog c = history();
		Question q = ask(FactDatasets.idOf(CAST)).withMeasure(Question.Measure.AVERAGE, "fact.v.mana").withGroupBy(Arrays.asList(FactDatasets.SUBJECT))
				.addFilter(Filter.text("fact.runClass", Op.EQ, "CLERIC"));
		Questions.Answer a = Questions.answer(c, q, RS);
		assertEquals(2, a.lines.size());
		Questions.Line guiding = null;
		for (Questions.Line l : a.lines) if (l.label.equals("GuidingLight")) guiding = l;
		assertNotNull(guiding);
		assertEquals(4.0, guiding.number, 0.0);
		assertTrue(a.question, a.question.contains("average mana spent of spell casts"));
		assertTrue(a.question, a.question.contains("only where"));
	}

	@Test
	public void castsPerFloorUseEveryFloorOfTheGroupAsTheDenominator(){
		Catalog c = history();
		Question q = ask(FactDatasets.idOf(CAST)).withPer(PerUnit.Unit.FLOOR).withGroupBy(Arrays.asList("fact.runClass"));
		Questions.Answer a = Questions.answer(c, q, RS);
		Questions.Line cleric = null, mage = null;
		for (Questions.Line l : a.lines){
			if (l.label.equalsIgnoreCase("Cleric")) cleric = l; else mage = l;
		}
		assertNotNull(cleric);
		assertEquals("3 casts over the Cleric's 2 floors", 1.5, cleric.number, 1e-9);
		assertEquals("1.50", cleric.value);
		assertEquals(1.0, mage.number, 1e-9);
		assertTrue(a.question, a.question.contains("per floor visit"));
	}

	@Test
	public void perOnlyGoesWithCountsAndTotals(){
		Question q = ask(FactDatasets.idOf(CAST)).withPer(PerUnit.Unit.RUN).withMeasure(Question.Measure.AVERAGE, "fact.v.mana");
		assertTrue(Questions.unitsFor(q).isEmpty());
		assertEquals(null, q.per);
		assertFalse(Questions.unitsFor(ask(FactDatasets.idOf(CAST))).isEmpty());
		assertTrue("run history has no per-run denominators of its own", Questions.unitsFor(ask(Datasets.RUNS)).isEmpty());
	}

	@Test
	public void theChoicesAreGeneratedFromTheDatasets(){
		Catalog c = history();
		List<Dataset> sets = Questions.askable(c);
		boolean sawCasts = false;
		for (Dataset d : sets) if (d.def.id.equals(FactDatasets.idOf(CAST))) sawCasts = true;
		assertTrue("a kind declared in the registry is askable", sawCasts);
		DatasetDef def = c.dataset(FactDatasets.idOf(CAST)).def;
		assertTrue(Questions.measurable(def, false).contains(def.column("fact.v.mana")));
		assertFalse("a number that stands for words is not measurable", Questions.measurable(def, false).contains(def.column("fact.runOutcome")));
		assertEquals("words come before numbers", ColumnDef.Type.TEXT.equals(Questions.groupable(def, false).get(0).type) || ColumnDef.Type.ENUM.equals(Questions.groupable(def, false).get(0).type), true);
	}

	@Test
	public void aQuestionWithNothingToAnswerSaysWhy(){
		Catalog c = history();
		Question q = ask(FactDatasets.idOf(CAST)).addFilter(Filter.text("fact.runClass", Op.EQ, "ROGUE"));
		Questions.Answer a = Questions.answer(c, q, RS);
		assertTrue(a.isEmpty() || a.lines.get(0).value.equals("0"));
	}

	@Test
	public void turningOffHiddenDetailsRemovesOnlyInternalQuestionParts(){
		DatasetDef def = QuerySupport.def();
		Question hidden = Question.about(def.id).withShowInternal(true)
				.withMeasure(Question.Measure.TOTAL, "secret").withPer(PerUnit.Unit.RUN)
				.withGroupBy(Arrays.asList("secret", "name"))
				.addFilter(Filter.num("secret", Op.EQ, 99))
				.addFilter(Filter.text("name", Op.EQ, "visible"));
		Question visible = Questions.withInternalVisibility(hidden, def, false);
		assertFalse(visible.showInternal);
		assertEquals(Question.Measure.COUNT, visible.measure);
		assertEquals("", visible.column);
		assertEquals(PerUnit.Unit.RUN, visible.per);
		assertEquals(Arrays.asList("name"), visible.groupBy);
		assertEquals(1, visible.filters.size());
		assertEquals("name", visible.filters.get(0).column);
	}

	@Test
	public void staleHiddenSelectionsCannotDisagreeWithTheAnswer(){
		DatasetDef def = QuerySupport.def();
		Question stale = Question.about(def.id)
				.withMeasure(Question.Measure.TOTAL, "secret")
				.withGroupBy(Arrays.asList("secret", "name"))
				.addFilter(Filter.num("secret", Op.EQ, 99))
				.addFilter(Filter.text("name", Op.EQ, "visible"));
		Catalog catalog = new Catalog().add(QuerySupport.dataset(def, Arrays.asList(
				QuerySupport.row(def, "one", "visible", 5L, 0L, 99L, true, RS))));
		Questions.Answer answer = Questions.answer(catalog, stale, RS);
		assertFalse(answer.query.showInternal);
		assertEquals(Aggregate.Fn.COUNT, answer.query.group.aggregates.get(0).fn);
		assertEquals(Arrays.asList("name"), answer.query.group.keys);
		assertEquals(1, answer.query.filters.size());
		assertEquals("name", answer.query.filters.get(0).column);
		assertFalse(answer.question.toLowerCase().contains("secret"));
		assertEquals(1, answer.lines.size());
		assertEquals("1", answer.lines.get(0).value);
	}

	@Test
	public void complexityGrowsWithEverythingAsked(){
		Question plain = ask("x");
		Question busy = plain.withMeasure(Question.Measure.TOTAL, "a").withPer(PerUnit.Unit.HOUR).withGroupBy(Arrays.asList("a", "b"))
				.addFilter(Filter.num("a", Op.GT, 1)).addFilter(Filter.num("b", Op.LT, 5)).withShowInternal(true);
		assertTrue(busy.complexity() > plain.complexity() + 5);
	}
}
