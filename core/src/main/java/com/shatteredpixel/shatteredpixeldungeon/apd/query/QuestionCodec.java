/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

//Stable local encoding for the friendly question-builder choices. The executable
//Query remains stored beside it, so a future build which cannot understand a newer
//question format can still preserve the original bytes without silently changing it.
public final class QuestionCodec {

	private static final int VERSION = 1;

	private QuestionCodec(){}

	public static String encode( Question question ){
		if (question == null) throw new IllegalArgumentException("question");
		Aggregate aggregate = question.measure == Question.Measure.COUNT
				? Aggregate.count() : Aggregate.of(question.measure.fn, question.column);
		Query query = Query.of(question.dataset).withScope(question.scope)
				.withFilters(question.filters).withShowInternal(question.showInternal)
				.withGroup(new GroupBy(question.groupBy, Arrays.asList(aggregate)));
		String encodedQuery = Base64.getEncoder().encodeToString(
				QueryCodec.encode(query).getBytes(StandardCharsets.UTF_8));
		return "question\t" + VERSION + "\nper\t"
				+ (question.per == null ? "" : question.per.name())
				+ "\nquery\t" + encodedQuery + "\n";
	}

	public static Question decode( String text ){
		if (text == null) throw new IllegalArgumentException("question");
		Integer version = null;
		PerUnit.Unit per = null;
		String encodedQuery = null;
		for (String line : text.split("\n")){
			if (line.isEmpty()) continue;
			String[] parts = line.split("\t", -1);
			try {
				switch (parts[0]){
					case "question": version = Integer.parseInt(parts[1]); break;
					case "per": per = parts[1].isEmpty() ? null : PerUnit.Unit.valueOf(parts[1]); break;
					case "query": encodedQuery = parts[1]; break;
					default: //same-version additions are ignored until this reader needs them
				}
			} catch (RuntimeException bad){
				throw new IllegalArgumentException("bad question line: " + line, bad);
			}
		}
		if (version == null || version != VERSION || encodedQuery == null)
			throw new IllegalArgumentException("unsupported question format");
		Query query;
		try {
			query = QueryCodec.decode(new String(Base64.getDecoder().decode(encodedQuery), StandardCharsets.UTF_8));
		} catch (RuntimeException bad){
			throw new IllegalArgumentException("bad question query", bad);
		}
		if (query.group == null || query.group.aggregates.size() != 1)
			throw new IllegalArgumentException("question aggregate");
		Aggregate aggregate = query.group.aggregates.get(0);
		Question.Measure measure = measure(aggregate.fn);
		String column = measure.needsColumn() ? aggregate.column : "";
		if (measure.needsColumn() && column.isEmpty()) throw new IllegalArgumentException("question column");
		return new Question(query.dataset, measure, column, per, query.group.keys,
				query.filters, query.scope, query.showInternal);
	}

	private static Question.Measure measure( Aggregate.Fn fn ){
		for (Question.Measure measure : Question.Measure.values())
			if (measure.fn == fn) return measure;
		throw new IllegalArgumentException("unsupported question measure: " + fn);
	}
}
