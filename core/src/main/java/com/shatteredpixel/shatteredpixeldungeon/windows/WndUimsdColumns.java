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
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;

import java.util.ArrayList;
import java.util.List;

//APD: which columns the table shows. Columns made of hidden information are only listed while hidden information is on.
public final class WndUimsdColumns {

	private WndUimsdColumns(){}

	public static void show( final UimsdModel m ){
		if (m.dataset() == null) return;
		if (m.result() != null && m.result().isGrouped()){
			ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("UIMSD", "A grouped table shows its group columns and its totals. "
					+ "Use Options, Group by, to change them, or open another view.", false));
			return;
		}
		final List<ColumnDef> cols = m.offeredColumns();
		List<String> shown = m.query().columns.isEmpty() ? m.dataset().def.defaultColumnIds() : m.query().columns;
		List<WndUimsdList.Entry> entries = new ArrayList<>();
		String last = null;
		for (ColumnDef c : cols){
			String g = c.group.isEmpty() ? "Details" : c.group.equals("Hidden") ? "Hidden information (finished runs only)" : c.group;
			if (!g.equals(last)){
				entries.add(WndUimsdList.Entry.heading(g));
				last = g;
			}
			String d = c.description;
			entries.add(WndUimsdList.Entry.item(c.label, d.length() > 90 ? d.substring(0, 88) + ".." : d).checked(shown.contains(c.id)));
		}
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.switches("Columns",
				"Tick the columns you want in the table, then tap Done. At least one column stays selected. The first column stays in place while the rest scroll sideways.", entries,
				new WndUimsdList.Picker(){
					@Override
					public void onPick( int i, boolean checked ){
						toggleSelection(m, cols, entries, i);
					}
				}).restoreWith(() -> show(m)));
	}

	//The model can refuse removal of the last visible column. Keep the picker consistent with its decision.
	static void toggleSelection(UimsdModel m, List<ColumnDef> cols, List<WndUimsdList.Entry> entries, int index){
		m.toggleColumn(cols.get(index).id);
		List<String> shown = m.query().columns;
		int item = 0;
		for (WndUimsdList.Entry entry : entries){
			if (entry.isItem()) entry.checked(shown.contains(cols.get(item++).id));
		}
	}

	//the shown columns in their order; tapping one offers to move it
	public static void arrange( final UimsdModel m ){
		if (m.dataset() == null) return;
		if (m.result() != null && m.result().isGrouped()){
			ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("UIMSD", "A grouped table has a fixed layout. Open another view to arrange its columns.", false));
			return;
		}
		final List<String> shown = new ArrayList<>(m.query().columns.isEmpty() ? m.dataset().def.defaultColumnIds() : m.query().columns);
		List<WndUimsdList.Entry> entries = new ArrayList<>();
		for (int i = 0; i < shown.size(); i++){
			ColumnDef c = null;
			for (ColumnDef x : m.offeredColumns()) if (x.id.equals(shown.get(i))) c = x;
			entries.add(WndUimsdList.Entry.item((i + 1) + ". " + (c == null ? shown.get(i) : c.label), "Tap to move it."));
		}
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Column order", "Left to right as in the table. Tap a column to move it.", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				moveOne(m, shown.get(i));
			}
		}).restoreWith(() -> arrange(m)));
	}

	private static void moveOne( final UimsdModel m, final String id ){
		List<WndUimsdList.Entry> entries = new ArrayList<>();
		entries.add(WndUimsdList.Entry.item("Move left", "One place towards the front."));
		entries.add(WndUimsdList.Entry.item("Move right", "One place towards the back."));
		entries.add(WndUimsdList.Entry.item("Make first", "It becomes the first column, the one that stays in place while the rest scroll."));
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Move column", "", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				m.moveColumn(id, i == 0 ? -1 : i == 1 ? 1 : -1000);
				arrange(m);
			}
		}).restoreWith(() -> moveOne(m, id)));
	}
}

