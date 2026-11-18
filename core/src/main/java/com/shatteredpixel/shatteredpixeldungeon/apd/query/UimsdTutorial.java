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
import java.util.Collections;
import java.util.List;

//The interactive tour of UIMSD: a list of steps shown over the real screen, each of which either just explains something (the player
//presses Next) or asks the player to do something and notices when they have, by looking at the model (the same one the screen draws).
//It uses a separate tour-owned model, loaded from the player's data or from small sample history.
//The Columns lesson may ungroup that tour view; neither real history nor the player's own model changes.
//Pure logic, so the whole tour can be tested without a screen.
public final class UimsdTutorial {

	//what the player has to do for a step to finish by itself
	public enum Expect { NEXT, VIEW, DRILL, BACK, FILTER, SORT, COLUMNS }

	public static final class Step {
		public final String id;
		public final String title;
		public final String text;
		public final Expect expect;
		//which element of the screen to point at ("" for none): the scene knows where each one is
		public final String target;

		Step( String id, String title, String text, Expect expect, String target ){
			this.id = id;
			this.title = title;
			this.text = text;
			this.expect = expect;
			this.target = target;
		}
	}

	private static final List<Step> STEPS;
	static {
		List<Step> s = new ArrayList<>();
		s.add(new Step("welcome", "Welcome", "This is UIMSD, the game's statistics system. The Home page sums up your finished runs. "
				+ "This tour walks you through the real screens in about two minutes. Tap Next to move on, or skip any step.", Expect.NEXT, "home.cards"));
		s.add(new Step("view", "Open a table", "Each card has buttons that open the full table behind it. Tap Weapons on the Gear card.", Expect.VIEW, "home.gear"));
		s.add(new Step("table", "Reading a table", "Each line is one KIND of weapon, with numbers about all of them: how many you met, how often you equipped them, how often you won. "
				+ "Tap a column heading to sort. Tap Next.", Expect.NEXT, "table.header"));
		s.add(new Step("open", "Open a line", "Tap the first line to see everything known about it. Then choose the button that opens its individual weapons (it starts with \"Open all\").",
				Expect.DRILL, "table.firstRow"));
		s.add(new Step("deeper", "One level down", "Now you are looking at the individual weapons of that kind, not the kind itself. "
				+ "The line under the table's name says where you are.", Expect.NEXT, "status"));
		s.add(new Step("back", "Going back", "Tap Back to go up one level.", Expect.BACK, "back"));
		s.add(new Step("filter", "A filter", "Tap Filter. Step 1: choose what to filter on. Step 2: choose how to compare. Step 3: choose a value. "
				+ "Only the lines that match stay in the table.", Expect.FILTER, "filter"));
		s.add(new Step("filter2", "More than one filter", "Add a second filter. Filters stack: a line has to pass all of them. "
				+ "Tap a filter's button above the table to change or remove it.", Expect.FILTER, "filter"));
		s.add(new Step("sort", "Sorting", "Tap a column heading to sort by it. Tap again to reverse it. Lines where the value is unknown (?) always come last.", Expect.SORT, "table.header"));
		s.add(new Step("columns", "Columns", "Tap Columns and switch a column on or off. There are far more columns than fit on the screen; they are grouped by topic.", Expect.COLUMNS, "columns"));
		s.add(new Step("marks", "The marks", "12 was recorded exactly. >=12 means at least 12. ~6 was worked out, not recorded exactly. ? means unknown: never recorded, and not zero. "
				+ "Hidden means it belongs to a run you are still playing.", Expect.NEXT, "table.firstRow"));
		s.add(new Step("sample", "Small samples", "A number based on fewer than 5 values is shown dimmed: a 100% win rate from 2 runs means little.", Expect.NEXT, "table.header"));
		s.add(new Step("coverage", "How much is known", "The Notes button says what you are looking at and what is being left out. Options > How much is known shows, column by column, "
				+ "how many values were recorded. Older runs lack some details, which is why some cells show ?.", Expect.NEXT, "notes"));
		s.add(new Step("reports", "Sharing a report", "Options has \"Share a report of this table\": the table you are looking at, with its filters, as text, Markdown, a web page or CSV. "
				+ "Home has a Report button for your overall numbers. Reports keep the marks and say which data is canonical (rebuilt from a seed) rather than recorded.", Expect.NEXT, "options"));
		s.add(new Step("home", "Home and Guide", "The Home button returns to the summary. The ? button opens the Guide, where you can read any topic "
				+ "again or take this tour again.", Expect.NEXT, "home"));
		STEPS = Collections.unmodifiableList(s);
	}

	public static List<Step> steps(){
		return STEPS;
	}

	public static int indexOf( String stepId ){
		for (int i = 0; i < STEPS.size(); i++) if (STEPS.get(i).id.equals(stepId)) return i;
		return -1;
	}

	//Puts a model where a step expects it, for a tour started in the middle (from a Guide topic): a table to look at, one level down for
	//the steps about being down there. Uses only what the player could do with the screen's own buttons.
	public static void prepare( UimsdModel m, int stepIndex ){
		if (m.catalog() == null) return;
		if (stepIndex >= indexOf("table") && !"weapons".equals(m.viewId()) && m.catalog().view("weapons") != null) m.showRoot("weapons");
		if (stepIndex >= indexOf("deeper") && stepIndex <= indexOf("back") && m.canOpenGroups()){
			QueryResult r = m.result();
			if (r != null && r.isGrouped() && !r.groups.isEmpty()) m.openGroupRows(r.groups.get(0));
		}
		//The Weapons card opens a grouped summary. Grouped tables intentionally refuse
		//column editing, so a Guide jump to Columns must start from editable rows.
		if (stepIndex >= indexOf("columns") && m.query().group != null) m.setGroup(null);
	}

	//---- one run of the tour ----

	private static final class Snapshot {
		final String view;
		final int depth, filters;
		final List<Sort> sorts;
		final List<String> columns;

		Snapshot( UimsdModel m ){
			Query q = m.query();
			view = m.viewId();
			depth = m.depth();
			filters = q.filters.size();
			sorts = new ArrayList<>(q.sorts);
			columns = new ArrayList<>(q.columns);
		}
	}

	private final UimsdModel model;
	private int index;
	private Snapshot base;
	private boolean finished;

	public UimsdTutorial( UimsdModel model, int startAt ){
		this.model = model;
		this.index = Math.max(0, Math.min(STEPS.size() - 1, startAt));
		this.base = new Snapshot(model);
	}

	public UimsdTutorial( UimsdModel model ){
		this(model, 0);
	}

	public boolean isFinished(){
		return finished;
	}

	public int index(){
		return index;
	}

	public int count(){
		return STEPS.size();
	}

	public Step step(){
		return STEPS.get(index);
	}

	//Only the tour-owned model is adjusted; all real history stays intact.
	private void prepareColumnsStep(){
		if (step().expect == Expect.COLUMNS && model.query().group != null) model.setGroup(null);
	}

	//moves on (the Next and Skip buttons)
	public void next(){
		if (finished) return;
		if (index >= STEPS.size() - 1){
			finished = true;
			return;
		}
		index++;
		prepareColumnsStep();
		base = new Snapshot(model);
	}

	public void previous(){
		if (finished || index == 0) return;
		index--;
		prepareColumnsStep();
		base = new Snapshot(model);
	}

	//Looks at the model: if the player has done what the step asked, moves on and says true. Called whenever the model changed.
	public synchronized boolean check(){
		if (finished) return false;
		Step s = step();
		Snapshot now = new Snapshot(model);
		boolean done;
		switch (s.expect){
			case VIEW: done = !now.view.equals(base.view); break;
			case DRILL: done = now.depth > base.depth; break;
			case BACK: done = now.depth < base.depth; break;
			case FILTER: done = now.filters > base.filters; break;
			case SORT: done = !now.sorts.equals(base.sorts); break;
			case COLUMNS: done = !now.columns.equals(base.columns); break;
			default: done = false;
		}
		if (!done) return false;
		next();
		return true;
	}
}
