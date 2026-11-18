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


package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ResultTable;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Sort;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;

import java.util.ArrayList;
import java.util.List;

//APD: the sort keys of a UIMSD table (up to three). Unknown values always sort last, whichever way a key goes.
public final class WndUimsdSort {

	private WndUimsdSort(){}

	private static String labelOf( ResultTable t, String id ){
		for (ResultTable.Header h : t.headers) if (h.id.equals(id)) return h.label;
		return id;
	}

	//a menu stays open under the next so Back returns to it; the last choice closes them all
	private static void keep( WndUimsdList w ){
		w.keepOpen();
		ShatteredPixelDungeon.scene().addToFront(w);
	}

	private static void apply( UimsdModel m, List<Sort> sorts ){
		WndUimsdList.closeAll();
		m.setSorts(sorts);
	}

	private static String way( boolean descending ){
		return descending ? "highest first" : "lowest first";
	}

	public static void show( final UimsdModel m ){
		final ResultTable table = m.table();
		if (table == null) return;
		final List<Sort> sorts = new ArrayList<>(m.query().sorts);
		List<WndUimsdList.Entry> entries = new ArrayList<>();
		int n = 0;
		if (sorts.isEmpty()){
			entries.add(WndUimsdList.Entry.note("The table is not sorted: rows come in their natural order."));
		} else {
			entries.add(WndUimsdList.Entry.heading("Sorted by"));
			for (int i = 0; i < sorts.size(); i++){
				entries.add(WndUimsdList.Entry.item((i + 1) + ". " + labelOf(table, sorts.get(i).column) + ", " + way(sorts.get(i).descending),
						i == 0 ? "tap to reverse or remove it" : "used when the keys above are equal; tap to reverse or remove it"));
				n++;
			}
		}
		entries.add(WndUimsdList.Entry.heading("Change"));
		final int addIndex = sorts.size() < 3 ? n++ : -1;
		if (addIndex >= 0) entries.add(WndUimsdList.Entry.item("Add a sort key", "sort by another column as well (up to three)"));
		final int clearIndex = sorts.isEmpty() ? -1 : n++;
		if (clearIndex >= 0) entries.add(WndUimsdList.Entry.item("Clear sorting", "go back to the natural order"));
		final int keys = sorts.size();
		keep(WndUimsdList.menu("Sort",
				"Tap a column heading in the table to sort by it quickly. Rows where the value is unknown always come last.", entries,
				new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				if (i == clearIndex){
					apply(m, new ArrayList<Sort>());
				} else if (i == addIndex){
					pickColumn(m, table, sorts);
				} else if (i < keys){
					editKey(m, sorts, i);
				}
			}
		}).restoreWith(() -> show(m)));
	}

	private static void editKey( final UimsdModel m, final List<Sort> sorts, final int index ){
		List<WndUimsdList.Entry> options = new ArrayList<>();
		options.add(WndUimsdList.Entry.item("Reverse it", "now " + way(!sorts.get(index).descending)));
		options.add(WndUimsdList.Entry.item("Remove it", "stop sorting by this column"));
		keep(WndUimsdList.menu("Sort key " + (index + 1), "", options, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				List<Sort> next = new ArrayList<>(sorts);
				if (i == 0) next.set(index, new Sort(sorts.get(index).column, !sorts.get(index).descending));
				else next.remove(index);
				apply(m, next);
			}
		}).restoreWith(() -> editKey(m, sorts, index)));
	}

	private static void pickColumn( final UimsdModel m, final ResultTable table, final List<Sort> sorts ){
		final List<ResultTable.Header> options = new ArrayList<>();
		List<WndUimsdList.Entry> entries = new ArrayList<>();
		for (ResultTable.Header h : table.headers){
			boolean used = false;
			for (Sort s : sorts) if (s.column.equals(h.id)) used = true;
			if (h.sortable && !used){
				options.add(h);
				entries.add(WndUimsdList.Entry.item(h.label));
			}
		}
		keep(WndUimsdList.menu("Sort by", "Only columns shown in the table can be sorted.", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				pickDirection(m, sorts, options.get(i).id, options.get(i).label);
			}
		}).restoreWith(() -> pickColumn(m, table, sorts)));
	}
	private static void pickDirection(final UimsdModel m, final List<Sort> sorts, final String id, String label){
		List<WndUimsdList.Entry> ways = new ArrayList<>();
		ways.add(WndUimsdList.Entry.item("Lowest first", "smallest numbers, earliest dates, A to Z"));
		ways.add(WndUimsdList.Entry.item("Highest first", "biggest numbers, latest dates, Z to A"));
		keep(WndUimsdList.menu("Sort " + label, "", ways, new WndUimsdList.Picker(){
			@Override
			public void onPick( int w, boolean c ){
				List<Sort> next = new ArrayList<>(sorts);
				next.add(new Sort(id, w == 1));
				apply(m, next);
			}
		}).restoreWith(() -> pickDirection(m, sorts, id, label)));
	}

}
