package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class BossesFinishedScopeTest {
	private ViewDef bosses(){
		for (ViewDef view : Datasets.allViews()) if (view.id.equals("bosses")) return view;
		throw new AssertionError("Bosses view missing");
	}

	@Test public void summaryAndInstancesExcludeUnfinishedEvenAfterScopeChanges(){
		DatasetDef def = Datasets.enemies();
		Row finished = row(def, "finished", true);
		Row live = row(def, "live", false);
		Dataset data = QuerySupport.dataset(def, Arrays.asList(finished, live));
		for (Query original : Arrays.asList(bosses().initialQuery(), bosses().instanceQuery())){
			Scope scope = original.scope.withRules(Scope.Rules.ALL, "").withUnfinished(true)
					.withUnknownRuleset(true).withMixedRuleset(true).withDuplicates(true);
			assertTrue(scope.finishedOnly);
			assertFalse(scope.includeUnfinished);
			QueryResult result = QueryEngine.run(data, original.withScope(scope), "rules");
			assertEquals(1, result.stats.matching);
			assertEquals(1, result.stats.unfinished);
		}
	}

	private Row row(DatasetDef def, String id, boolean finished){
		Row row = new Row(id, id, id, def.size());
		row.ruleset = "rules";
		row.finished = finished;
		row.setNum(def.indexOf("enemy.boss"), 1, Fact.PROV_RECORDED);
		row.setText(def.indexOf("enemy.class"), "Goo", Fact.PROV_RECORDED);
		return row;
	}

	@Test public void aSavedBossQueryKeepsItsFinishedOnlyRestriction(){
		Query original = bosses().initialQuery();
		Query reopened = QueryCodec.decode(QueryCodec.encode(original));
		assertTrue(reopened.scope.finishedOnly);
		assertFalse(reopened.scope.withUnfinished(true).includeUnfinished);
		assertEquals(original.scope, reopened.scope);
	}

	@Test public void unrelatedViewsCanStillIncludeUnfinishedRuns(){
		assertTrue(Query.of("runs").scope.withUnfinished(true).includeUnfinished);
		assertFalse(Query.of("runs").scope.finishedOnly);
	}

	@Test public void aFreshScopeFromTheOptionsCannotRemoveTheViewRestriction(){
		UimsdModel model = new UimsdModel("rules", "");
		Catalog catalog = new Catalog();
		catalog.add(QuerySupport.dataset(Datasets.enemies(), java.util.Collections.emptyList()));
		catalog.add(bosses());
		model.setCatalog(catalog);
		model.setScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
		assertTrue(model.query().scope.finishedOnly);
		assertFalse(model.query().scope.includeUnfinished);
	}
}
