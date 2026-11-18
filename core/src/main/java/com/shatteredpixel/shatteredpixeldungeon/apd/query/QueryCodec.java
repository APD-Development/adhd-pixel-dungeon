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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */


package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import java.util.ArrayList;
import java.util.List;

//A query as text and back. This is the whole of what a saved query or a shared query will need to be: the format is line
//based, every line is "name<TAB>fields", unknown lines are skipped so a newer writer's extras do not break an older reader.
public final class QueryCodec {

	private QueryCodec(){}

	private static String esc( String s ){
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++){
			char c = s.charAt(i);
			switch (c){
				case '\\': b.append("\\\\"); break;
				case '\t': b.append("\\t"); break;
				case '\n': b.append("\\n"); break;
				case '|': b.append("\\p"); break;
				default: b.append(c);
			}
		}
		return b.toString();
	}

	private static String unesc( String s ){
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++){
			char c = s.charAt(i);
			if (c == '\\' && i + 1 < s.length()){
				char n = s.charAt(++i);
				b.append(n == 't' ? '\t' : n == 'n' ? '\n' : n == 'p' ? '|' : n);
			} else {
				b.append(c);
			}
		}
		return b.toString();
	}

	public static String encode( Query q ){
		StringBuilder b = new StringBuilder();
		b.append("dataset\t").append(esc(q.dataset)).append('\n');
		Scope s = q.scope;
		b.append("scope\t").append(s.rules.name()).append('\t').append(esc(s.ruleset)).append('\t')
				.append((s.includeUnknownRuleset ? 1 : 0) | (s.includeMixedRuleset ? 2 : 0) | (s.includeUnfinished ? 4 : 0)
						| (s.includeDuplicates ? 8 : 0) | (s.finishedOnly ? 16 : 0)).append('\n');
		for (Filter f : q.filters){
			b.append("filter\t").append(esc(f.column)).append('\t').append(f.op.name()).append('\t');
			for (int i = 0; i < f.nums.length; i++) b.append(i > 0 ? "," : "").append(f.nums[i]);
			b.append('\t');
			for (int i = 0; i < f.texts.length; i++) b.append(i > 0 ? "|" : "").append(esc(f.texts[i]));
			if (f.texts.length == 1 && f.texts[0].isEmpty()) b.append('\t').append(1);
			b.append('\n');
		}
		for (Sort so : q.sorts) b.append("sort\t").append(esc(so.column)).append('\t').append(so.descending ? 1 : 0).append('\n');
		for (String c : q.columns) b.append("column\t").append(esc(c)).append('\n');
		if (q.group != null){
			b.append("group");
			for (String k : q.group.keys) b.append('\t').append(esc(k));
			b.append('\n');
			for (Aggregate a : q.group.aggregates){
				b.append("agg\t").append(a.fn.name()).append('\t').append(esc(a.column)).append('\t').append(a.target);
				if (!a.over.isEmpty() || !a.whereColumn.isEmpty() || !a.distinctBy.isEmpty() || !a.label.isEmpty()){
					b.append('\t').append(esc(a.over)).append('\t').append(esc(a.whereColumn)).append('\t').append(a.whereValue).append('\t').append(esc(a.distinctBy)).append('\t').append(esc(a.label));
				}
				b.append('\n');
			}
		}
		b.append("show\t").append(q.showInternal ? 1 : 0).append('\n');
		b.append("min\t").append(q.minSample).append('\n');
		return b.toString();
	}

	public static Query decode( String text ){
		String dataset = "";
		Scope scope = Scope.DEFAULT;
		List<Filter> filters = new ArrayList<>();
		List<Sort> sorts = new ArrayList<>();
		List<String> columns = new ArrayList<>();
		List<String> keys = null;
		List<Aggregate> aggs = new ArrayList<>();
		boolean show = false;
		int min = 0;
		for (String line : text.split("\n")){
			if (line.isEmpty()) continue;
			String[] p = line.split("\t", -1);
			try {
				switch (p[0]){
					case "dataset": dataset = unesc(p[1]); break;
					case "scope": {
						int bits = Integer.parseInt(p[3]);
						scope = new Scope(Scope.Rules.valueOf(p[1]), unesc(p[2]), (bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0, (bits & 16) != 0);
						break;
					}
					case "filter": {
						long[] nums = new long[0];
						if (!p[3].isEmpty()){
							String[] n = p[3].split(",");
							nums = new long[n.length];
							for (int i = 0; i < n.length; i++) nums[i] = Long.parseLong(n[i]);
						}
						String[] texts = new String[0];
						if (!p[4].isEmpty()){
							String[] t = p[4].split("\\|", -1);
							texts = new String[t.length];
							for (int i = 0; i < t.length; i++) texts[i] = unesc(t[i]);
						}
						// Older records omit the count. New records distinguish no texts from one empty text.
						if (p.length >= 6) {
							int count = Integer.parseInt(p[5]);
							if (count == 1 && p[4].isEmpty()) texts = new String[]{""};
							if (count != texts.length) throw new IllegalArgumentException("text count");
						}
						filters.add(new Filter(unesc(p[1]), Op.valueOf(p[2]), nums, texts));
						break;
					}
					case "sort": sorts.add(new Sort(unesc(p[1]), p[2].equals("1"))); break;
					case "column": columns.add(unesc(p[1])); break;
					case "group":
						keys = new ArrayList<>();
						for (int i = 1; i < p.length; i++) keys.add(unesc(p[i]));
						break;
					case "agg":
						if (p.length >= 8) aggs.add(new Aggregate(Aggregate.Fn.valueOf(p[1]), unesc(p[2]), Long.parseLong(p[3]), unesc(p[4]), unesc(p[5]), Long.parseLong(p[6]), unesc(p[7]), p.length >= 9 ? unesc(p[8]) : ""));
						else aggs.add(new Aggregate(Aggregate.Fn.valueOf(p[1]), unesc(p[2]), Long.parseLong(p[3])));
						break;
					case "show": show = p[1].equals("1"); break;
					case "min": min = Integer.parseInt(p[1]); break;
					default: //a line from a newer writer
				}
			} catch (RuntimeException bad){
				throw new IllegalArgumentException("bad query line: " + line);
			}
		}
		//Older Turkish/Azeri builds wrote locale-specific aggregate sort IDs. Resolve
		//only aliases of this query's actual aggregates; never rewrite saved bytes or keys.
		if (keys != null) for (int i = 0; i < sorts.size(); i++){
			Sort sort = sorts.get(i);
			if (keys.contains(sort.column)) continue;
			for (Aggregate aggregate : aggs){
				if (aggregate.fn != Aggregate.Fn.MIN && aggregate.fn != Aggregate.Fn.MEDIAN
						&& aggregate.fn != Aggregate.Fn.COUNT_DISTINCT) continue;
				String id = aggregate.id();
				int separator = id.indexOf(':');
				String legacy = id.substring(0, separator).replace('i', '\u0131') + id.substring(separator);
				if (sort.column.equals(legacy)){
					sorts.set(i, new Sort(id, sort.descending));
					break;
				}
			}
		}
		return new Query(dataset, scope, filters, sorts, columns, keys == null ? null : new GroupBy(keys, aggs), show, min);
	}
}


