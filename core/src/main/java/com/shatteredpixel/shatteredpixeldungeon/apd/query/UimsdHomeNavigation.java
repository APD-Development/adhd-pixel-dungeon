package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;

/** Presentation shortcuts reuse ordinary queries and their existing disclosure/scope rules. */
public final class UimsdHomeNavigation {
 private UimsdHomeNavigation() {}
 public static Query outcome(String destination, boolean allRules) {
  if (!destination.matches("!outcome:[012]")) throw new IllegalArgumentException("Unknown Home destination");
  int value = destination.charAt(destination.length()-1)-'0';
  Scope scope = allRules ? Scope.DEFAULT.withRules(Scope.Rules.ALL, "") : Scope.DEFAULT;
  return Query.of(Datasets.RUNS).withScope(scope).addFilter(Filter.num(FactKeys.OUTCOME, Op.EQ, value));
 }
}
