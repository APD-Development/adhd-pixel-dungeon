package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Direct contracts for the immutable aggregate description used by query equality,
 * saved views, aggregate-column sorting and report headings.
 */
public class AggregateContractTest {

	@Test
	public void constructorsNormalizeNullTextAndInactiveWhereValues(){
		Aggregate empty = new Aggregate(Aggregate.Fn.SUM, null, 17, null, null,
				Long.MAX_VALUE, null, null);
		assertEquals("", empty.column);
		assertEquals("", empty.over);
		assertEquals("", empty.whereColumn);
		assertEquals("an absent condition has no active comparison value", 0, empty.whereValue);
		assertEquals("", empty.distinctBy);
		assertEquals("", empty.label);

		Aggregate conditioned = new Aggregate(Aggregate.Fn.SUM, "score", 0, "",
				"kind", Long.MIN_VALUE, "", "");
		assertEquals(Long.MIN_VALUE, conditioned.whereValue);
	}

	@Test
	public void factoriesAndFluentRefinementsKeepEveryMeaningfulField(){
		Aggregate rate = Aggregate.rate("outcome", 2);
		Aggregate refined = rate.where("class", 5).perDistinct("run").labeled("Win rate");

		assertEquals(Aggregate.Fn.RATE, refined.fn);
		assertEquals("outcome", refined.column);
		assertEquals(2, refined.target);
		assertEquals("class", refined.whereColumn);
		assertEquals(5, refined.whereValue);
		assertEquals("run", refined.distinctBy);
		assertEquals("Win rate", refined.label);
		assertEquals("", refined.over);

		assertEquals("", rate.whereColumn);
		assertEquals("", rate.distinctBy);
		assertEquals("", rate.label);

		Aggregate ratio = Aggregate.ratio("damage", "turns");
		assertEquals(Aggregate.Fn.RATIO, ratio.fn);
		assertEquals("damage", ratio.column);
		assertEquals("turns", ratio.over);
		assertEquals(Aggregate.Fn.COUNT_DISTINCT, Aggregate.distinct("run").fn);
		assertEquals(Aggregate.Fn.COUNT, Aggregate.count().fn);
	}

	@Test
	public void identityIncludesEverySemanticRefinementButNotTheDisplayLabel(){
		Aggregate plainRate = Aggregate.rate("outcome", 1);
		Aggregate labeledRate = plainRate.labeled("Wins");
		assertEquals(plainRate, labeledRate);
		assertEquals(plainRate.hashCode(), labeledRate.hashCode());

		assertNotEquals(plainRate, Aggregate.rate("outcome", 2));
		assertNotEquals(plainRate, plainRate.where("class", 1));
		assertNotEquals(plainRate, plainRate.perDistinct("run"));
		assertNotEquals(Aggregate.ratio("score", "turns"), Aggregate.ratio("score", "depth"));

		Set<String> ids = new HashSet<>();
		for (Aggregate aggregate : Arrays.asList(
				Aggregate.count(),
				Aggregate.of(Aggregate.Fn.COUNT_KNOWN, "score"),
				Aggregate.of(Aggregate.Fn.SUM, "score"),
				Aggregate.of(Aggregate.Fn.AVG, "score"),
				Aggregate.of(Aggregate.Fn.MIN, "score"),
				Aggregate.of(Aggregate.Fn.MAX, "score"),
				Aggregate.rate("score", 1),
				Aggregate.rate("score", 2),
				Aggregate.distinct("score"),
				Aggregate.of(Aggregate.Fn.MEDIAN, "score"),
				Aggregate.ratio("score", "turns"),
				plainRate.where("class", 1),
				plainRate.where("class", 2),
				plainRate.perDistinct("run"),
				plainRate.perDistinct("seed"))){
			ids.add(aggregate.id());
		}
		assertEquals("every semantically different aggregate needs its own sort/save id", 15, ids.size());

		assertFalse("a label must not enter the stable saved-query identity",
				labeledRate.id().contains("Wins"));
		assertEquals("unused targets do not change a SUM", Aggregate.of(Aggregate.Fn.SUM, "score"),
				new Aggregate(Aggregate.Fn.SUM, "score", Long.MAX_VALUE));
	}
}
