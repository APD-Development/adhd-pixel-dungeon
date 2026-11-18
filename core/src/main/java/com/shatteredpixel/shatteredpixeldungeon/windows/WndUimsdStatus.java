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
import com.shatteredpixel.shatteredpixeldungeon.apd.query.StatusText;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;

import java.util.ArrayList;
import java.util.List;

//APD: every line of the status strip in full. A line that says something was left out can be tapped to include it.
public final class WndUimsdStatus {

	private WndUimsdStatus(){}

	public static void show( final UimsdModel m ){
		final List<StatusText.Line> lines = m.status();
		List<WndUimsdList.Entry> entries = new ArrayList<>();
		final List<String> actions = new ArrayList<>();
		boolean anyAction = false;
		for (StatusText.Line l : lines) if (!l.action.isEmpty()) anyAction = true;
		entries.add(WndUimsdList.Entry.heading("This table"));
		for (StatusText.Line l : lines){
			if (l.action.isEmpty()) entries.add(l.kind == StatusText.WARN ? WndUimsdList.Entry.warning(l.text) : WndUimsdList.Entry.note(l.text));
		}
		if (anyAction){
			entries.add(WndUimsdList.Entry.heading("Left out (tap to include)"));
			for (StatusText.Line l : lines){
				if (l.action.isEmpty()) continue;
				entries.add(WndUimsdList.Entry.item(l.text));
				actions.add(l.action);
			}
		}
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Notes",
				"What this table is showing, and what it is leaving out.", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				if (i < actions.size()) m.applyAction(actions.get(i));
			}
		}).restoreWith(() -> show(m)));
	}
}
