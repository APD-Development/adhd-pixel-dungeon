package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import org.junit.Test;
import static org.junit.Assert.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
public class UimsdHomeNavigationTest {
 @Test public void eachOutcomeUsesNormalRunFiltering(){for(int n=0;n<3;n++){
  Query q=UimsdHomeNavigation.outcome("!outcome:"+n,false);
  assertEquals(Datasets.RUNS,q.dataset);assertSame(Scope.DEFAULT,q.scope);
  assertEquals(1,q.filters.size());assertEquals(Filter.num(FactKeys.OUTCOME,Op.EQ,n),q.filters.get(0));
  assertFalse(q.showInternal);assertTrue(q.columns.isEmpty());assertTrue(q.sorts.isEmpty());
 }}
 @Test public void mixedScopeKeepsDefaultIsolation(){Query q=UimsdHomeNavigation.outcome("!outcome:1",true);
  assertEquals(Scope.DEFAULT.withRules(Scope.Rules.ALL,""),q.scope);
 }
 @Test(expected=IllegalArgumentException.class) public void invalidDestinationCannotBecomeAnUnfilteredTable(){UimsdHomeNavigation.outcome("!outcome:99",false);}
}
