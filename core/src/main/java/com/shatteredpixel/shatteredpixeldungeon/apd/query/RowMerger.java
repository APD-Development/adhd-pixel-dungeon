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

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

//Combines the rows that one logical run recorded in several log segments into one row, column by column, following each
//column's Merge rule. The raw logs are not touched; this is a view. What it will not do is pretend: when a part could not
//record a value (an older telemetry version, a gap) a sum or a maximum becomes a LOWER BOUND, never an exact number, and a
//value that a rule cannot establish stays unknown.
public final class RowMerger {

	private RowMerger(){}

	//worst provenance wins: a result is only as exact as its weakest part
	private static int worst( int a, int b ){
		return rank(a) >= rank(b) ? a : b;
	}

	private static int rank( int prov ){
		switch (prov){
			case Fact.PROV_INCOMPLETE: return 4;
			case Fact.PROV_INFERRED: return 3;
			case Fact.PROV_LEGACY: return 2;
			default: return 1;
		}
	}

	//groups rows by merge key (rows without one are kept as they are) and merges each group. Parts must be in the order the
	//segments were recorded.
	public static List<Row> mergeAll( DatasetDef def, List<Row> rows ){
		Map<String, List<Row>> groups = new LinkedHashMap<>();
		List<Object> order = new ArrayList<>();   //a Row (unmerged) or a String key
		for (Row r : rows){
			if (r.mergeKey == null){
				order.add(r);
				continue;
			}
			List<Row> g = groups.get(r.mergeKey);
			if (g == null){
				g = new ArrayList<>();
				groups.put(r.mergeKey, g);
				order.add(r.mergeKey);
			}
			g.add(r);
		}
		List<Row> out = new ArrayList<>(order.size());
		for (Object o : order){
			if (o instanceof Row) out.add((Row) o);
			else {
				List<Row> g = groups.get((String) o);
				out.add(g.size() == 1 ? g.get(0) : merge(def, g));
			}
		}
		return out;
	}

	public static Row merge( DatasetDef def, List<Row> parts ){
		Row first = parts.get(0);
		Row last = parts.get(parts.size() - 1);
		Row out = new Row(first.id, first.mergeKey, first.runId, def.size());
		out.ruleset = first.ruleset;
		out.finished = last.finished;
		out.imported = first.imported;
		out.duplicate = first.duplicate;
		boolean anyPublic = false;
		TreeSet<String> versions = new TreeSet<>();
		for (Row p : parts){
			anyPublic |= p.publicRow;
			if (!p.telemetry.isEmpty()) versions.add(p.telemetry);
		}
		out.publicRow = anyPublic;
		StringBuilder v = new StringBuilder();
		for (String s : versions) v.append(v.length() > 0 ? "+" : "").append(s);
		out.telemetry = v.toString();

		for (ColumnDef c : def.columns()){
			int i = c.index;
			switch (c.merge){
				case SAME:
					for (Row p : parts) if (p.isKnown(i)){ copy(out, p, i); break; }
					break;
				case LAST:
					if (last.isKnown(i)) copy(out, last, i);
					break;
				case ANY: {
					boolean anyKnown = false, anyTrue = false;
					int prov = Fact.PROV_RECORDED;
					int falseProv = Fact.PROV_RECORDED;
					int unknownParts = 0;
					for (Row p : parts){
						if (!p.isKnown(i)){ unknownParts++; continue; }
						anyKnown = true;
						if (p.num(i) != 0){
							anyTrue = true;
							prov = worst(prov, p.prov(i));
						} else falseProv = worst(falseProv, p.prov(i));
					}
					if (anyTrue) out.setNum(i, 1, prov);
					else if (anyKnown && unknownParts == 0) out.setNum(i, 0, falseProv);
					break;
				}
				default: {   //SUM, MAX
					if (c.type == ColumnDef.Type.TEXT){
						for (Row p : parts) if (p.isKnown(i)){ copy(out, p, i); break; }
						break;
					}
					long acc = 0;
					BigInteger wideSum = null;
					boolean any = false;
					int prov = Fact.PROV_RECORDED;
					boolean missing = false;
					for (Row p : parts){
						if (!p.isKnown(i)){
							missing = true;
							continue;
						}
						long n = p.num(i);
						if (!any) acc = n;
						else if (c.merge == ColumnDef.Merge.SUM) {
							if (wideSum != null) wideSum = wideSum.add(BigInteger.valueOf(n));
							else {
								long next = acc + n;
								if (((acc ^ next) & (n ^ next)) < 0)
									wideSum = BigInteger.valueOf(acc).add(BigInteger.valueOf(n));
								else acc = next;
							}
						} else acc = Math.max(acc, n);
						any = true;
						prov = worst(prov, p.prov(i));
					}
					if (any){
						if (wideSum != null) {
							// Raw segments are retained; never label a wrapped total exact.
							if (wideSum.bitLength() > 63) continue;
							acc = wideSum.longValue();
						}
						//a part that knew nothing may have held more: the result is at least this much
						//(the number of distinct things across segments is only ever bounded below by the largest part)
						boolean lower = missing || (c.merge == ColumnDef.Merge.LOWER && parts.size() > 1);
						out.setNum(i, acc, lower ? Fact.PROV_INCOMPLETE : prov);
					}
				}
			}
		}
		return out;
	}

	private static void copy( Row to, Row from, int col ){
		if (from.kind(col) == Row.K_NUM) to.setNum(col, from.num(col), from.prov(col));
		else to.setText(col, from.text(col), from.prov(col));
	}
}

