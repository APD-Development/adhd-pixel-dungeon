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
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Dataset;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Filter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.PerUnit;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Question;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Questions;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Scope;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.SavedViews;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.StatusText;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdTopicSearch;

import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList.Entry;

import java.util.ArrayList;
import java.util.List;

//APD: the question builder, the friendly front door to everything UIMSD has recorded. A question is a sentence with blanks: how many / the average
//of / the highest ... [of what] per [floor visit] split by [spell] only where [class is Cleric]. Every blank is a short list of what you really have, with
//counts, so nobody has to know what a column or a filter is. The answer is one line for each group; "Open as a table" hands exactly the same question to
//the full table tools (filters, sorts, columns, groups) for anyone who wants to keep digging. Everything offered is generated from the datasets, so a new
//kind of recorded fact is askable the moment it exists.
public final class WndUimsdAsk {

	private WndUimsdAsk(){}

	//what the player was last asking, so coming back picks up where they were
	private static Question last;

	public interface Host {
		//open this question's query as a table with the full tools
		//'backToAnswer' reopens the answer the table came from (what Back should do from that table); may be null
		void openAsTable( com.shatteredpixel.shatteredpixeldungeon.apd.query.Query q, Runnable backToAnswer );
		//Where named questions live. Tests and hosts without persistent history may leave this null.
		default java.io.File root(){ return null; }
	}

	private static final int MAX_LINES = 40;

	//a window of the chain: it stays open under the next one, so Back returns to it
	private static void showKept( WndUimsdList w ){
		w.keepOpen();
		ShatteredPixelDungeon.scene().addToFront(w);
	}

	private static void show( com.shatteredpixel.shatteredpixeldungeon.ui.Window w ){
		ShatteredPixelDungeon.scene().addToFront(w);
	}

	public static void open( Catalog cat, String ruleset, Host host ){
		if (cat == null) return;
		Question q = last;
		if (q == null || cat.dataset(q.dataset) == null){
			List<Dataset> sets = Questions.askable(cat);
			if (sets.isEmpty()){
				ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("UIMSD", "There is nothing recorded to ask about yet. Play a run and ask again.", false));
				return;
			}
			q = Question.about(sets.get(0).def.id);
		}
		builder(cat, ruleset, host, q);
	}

	//the question as it stands, one tap per blank
	public static void builder( final Catalog cat, final String ruleset, final Host host, final Question q ){
		last = q;
		//a changed question replaces the builder (and any picker left open over it)
		WndUimsdList.closeAll();
		Dataset ds = cat.dataset(q.dataset);
		if (ds == null) return;
		final DatasetDef def = ds.def;
		List<Entry> entries = new ArrayList<>();
		entries.add(Entry.heading("Your question"));
		entries.add(Entry.note(Questions.describe(q, def) + "?"));
		entries.add(Entry.heading("Change it"));
		final int ABOUT = 0, MEASURE = 1, PER = 2, SPLIT = 3, WHERE = 4, RUNS = 5, HIDDEN = 6, ASK = 7, SEARCH = 8, WINDOW = 9;
		final List<Integer> ids = new ArrayList<>();
		entries.add(Entry.item("Search UIMSD topics", "find recorded subjects and public fields by name, offline")); ids.add(SEARCH);
        if(com.shatteredpixel.shatteredpixeldungeon.apd.query.EventDatasets.EQUIPMENT.equals(q.dataset)){
            entries.add(Entry.item("Damage after equipping", "Choose a matching equipment occurrence and an explicit following-turn window."));ids.add(WINDOW);
        }
		entries.add(Entry.item("About: " + def.title, "what to look at")); ids.add(ABOUT);
		entries.add(Entry.item("Measure: " + q.measure.phrase + (q.measure.needsColumn() && def.column(q.column) != null ? " " + def.column(q.column).label.toLowerCase() : ""),
				"count them, or add up, average or find the highest of a number")); ids.add(MEASURE);
		if (!Questions.unitsFor(q).isEmpty()){
			entries.add(Entry.item("Per: " + (q.per == null ? "nothing" : q.per.singular), "divide by what you did: runs, floors, turns or time played")); ids.add(PER);
		}
		entries.add(Entry.item("Split by: " + (q.groupBy.isEmpty() ? "nothing" : joinLabels(def, q.groupBy)), "one answer for each class, spell, floor ...")); ids.add(SPLIT);
		entries.add(Entry.item("Only where: " + (q.filters.isEmpty() ? "no conditions" : q.filters.size() + (q.filters.size() == 1 ? " condition" : " conditions")),
				"narrow it to some runs, items or floors")); ids.add(WHERE);
		entries.add(Entry.item("Which runs: " + (q.scope.rules == Scope.Rules.ALL ? "every run" : "your current rules"),
				"runs played under other rules, unfinished runs and older recordings can be included")); ids.add(RUNS);
		entries.add(Entry.item("Hidden details: " + (q.showInternal ? "included for finished runs" : "left out"),
				"what you could not have known during a run is shown only once the run is over")); ids.add(HIDDEN);
		entries.add(Entry.heading("Then"));
		entries.add(Entry.item("Show the answer", "")); ids.add(ASK);
		final Question cur = q;
		showKept(WndUimsdList.menu("Ask a question", "Everything UIMSD has recorded can be asked about. Change any part of the sentence, then show the answer.", entries,
				new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				switch (ids.get(i)){
					case SEARCH: searchTopics(cat, ruleset, host, cur); break;
                    case WINDOW:
                        UimsdModel windowModel=new UimsdModel(ruleset, "");windowModel.setCatalog(cat);
                        windowModel.showQuery(Questions.toQuery(cur,def).withGroup(null));
                        WndUimsdResearch.equipmentWindows(windowModel);break;
					case ABOUT: pickDataset(cat, ruleset, host, cur); break;
					case MEASURE: pickMeasure(cat, ruleset, host, cur, def); break;
					case PER: pickPer(cat, ruleset, host, cur); break;
					case SPLIT: pickSplit(cat, ruleset, host, cur, def); break;
					case WHERE: editFilters(cat, ruleset, host, cur); break;
					case RUNS: builder(cat, ruleset, host, cur.withScope(cur.scope.rules == Scope.Rules.ALL ? Scope.DEFAULT
							: new Scope(Scope.Rules.ALL, "", true, true, true, true))); break;
					case HIDDEN: builder(cat, ruleset, host, Questions.withInternalVisibility(cur, def, !cur.showInternal)); break;
					default: answer(cat, ruleset, host, cur);
				}
			}
		}).restoreWith(() -> builder(cat, ruleset, host, q)));
	}


    // Narrow offline search of registered subjects and PUBLIC field names.
    // Results go through the existing query builder; this is not an AI or raw-history search.
    private static void searchTopics(final Catalog cat, final String ruleset, final Host host, final Question q) {
        show(new WndUimsdTextInput("Search UIMSD", "Try 'how many runs by class' or search recorded topics. This never searches raw values.",
                "", 100, false, "Search", "Cancel", (positive, words) -> {
                    if (positive) topicResults(cat, ruleset, host, q, words);
                }));
    }

    private static void topicResults(final Catalog cat, final String ruleset, final Host host,
                                     final Question q, final String words) {
        final List<UimsdTopicSearch.Hit> hits = UimsdTopicSearch.find(cat, words);
        final Question interpreted = UimsdTopicSearch.interpret(cat, words, q);
        List<Entry> entries = new ArrayList<>();
        if (interpreted != null) {
            Dataset matched = cat.dataset(interpreted.dataset);
            entries.add(Entry.item("Open this question", Questions.describe(interpreted, matched.def)));
        }
        if (hits.isEmpty() && interpreted == null)
            entries.add(Entry.note("No supported question or public topic matches. Try a name, or 'average score of runs by class'."));
        for (UimsdTopicSearch.Hit hit : hits) entries.add(Entry.item(hit.label, hit.detail));
        entries.add(Entry.item("Search again", "look for another topic"));
        showKept(WndUimsdList.menu("Search UIMSD", "Search public topics or simple questions (how many, total, average, highest, lowest, median; optional 'by' a public field). Other phrases are not interpreted.",
                entries, new WndUimsdList.Picker() {
                    @Override public void onPick(int i, boolean checked) {
                        if (interpreted != null && i == 0) {
                            builder(cat, ruleset, host, interpreted);
                            return;
                        }
                        int hitIndex = i - (interpreted == null ? 0 : 1);
                        if (hitIndex == hits.size()) {
                            searchTopics(cat, ruleset, host, q);
                            return;
                        }
                        UimsdTopicSearch.Hit hit = hits.get(hitIndex);
                        Dataset selected = cat.dataset(hit.datasetId);
                        if (selected != null) builder(cat, ruleset, host, hit.question(q, selected.def));
                    }
                }).restoreWith(() -> topicResults(cat, ruleset, host, q, words)));
    }

	private static String joinLabels( DatasetDef def, List<String> ids ){
		StringBuilder b = new StringBuilder();
		for (String id : ids){
			ColumnDef c = def.column(id);
			if (b.length() > 0) b.append(" and ");
			b.append(c == null ? id : c.label.toLowerCase());
		}
		return b.toString();
	}

	// ---- the blanks ----

	private static void pickDataset( final Catalog cat, final String ruleset, final Host host, final Question q ){
		final List<Dataset> sets = Questions.askable(cat);
		List<Entry> entries = new ArrayList<>();
		for (Dataset d : sets) entries.add(Entry.item(d.def.title, d.rows.size() + " " + d.def.countLabel.toLowerCase() + " recorded").current(d.def.id.equals(q.dataset)));
		show(WndUimsdList.menu("About", "What do you want to know about?", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				builder(cat, ruleset, host, q.withDataset(sets.get(i).def.id));
			}
		}).restoreWith(() -> pickDataset(cat, ruleset, host, q)));
	}

	private static void pickMeasure( final Catalog cat, final String ruleset, final Host host, final Question q, final DatasetDef def ){
		final Question.Measure[] ms = Question.Measure.values();
		List<Entry> entries = new ArrayList<>();
		for (Question.Measure m : ms){
			boolean possible = !m.needsColumn() || !Questions.measurable(def, q.showInternal).isEmpty();
			if (!possible) continue;
			entries.add(Entry.item(m.phrase.substring(0, 1).toUpperCase() + m.phrase.substring(1), m.needsColumn() ? "of a number you pick next" : "of " + def.countLabel.toLowerCase()).current(m == q.measure));
		}
		final List<Question.Measure> offered = new ArrayList<>();
		for (Question.Measure m : ms) if (!m.needsColumn() || !Questions.measurable(def, q.showInternal).isEmpty()) offered.add(m);
		show(WndUimsdList.menu("Measure", "What do you want to work out?", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				final Question.Measure m = offered.get(i);
				if (!m.needsColumn()){
					builder(cat, ruleset, host, q.withMeasure(m, ""));
					return;
				}
				pickNumber(cat, ruleset, host, q, def, m);
			}
		}).restoreWith(() -> pickMeasure(cat, ruleset, host, q, def)));
	}

	private static void pickNumber(final Catalog cat, final String ruleset, final Host host,
	                               final Question q, final DatasetDef def, final Question.Measure m){
		final List<ColumnDef> cols = Questions.measurable(def, q.showInternal);
		List<Entry> es = new ArrayList<>();
		for (ColumnDef c : cols) es.add(Entry.item(c.label, c.description).current(c.id.equals(q.column)));
		show(WndUimsdList.menu("Which number?", "The number to " + (m == Question.Measure.TOTAL ? "add up" : "look at") + ".", es, new WndUimsdList.Picker(){
			@Override
			public void onPick( int j, boolean c ){
				builder(cat, ruleset, host, q.withMeasure(m, cols.get(j).id));
			}
		}).restoreWith(() -> pickNumber(cat, ruleset, host, q, def, m)));
	}

	private static void pickPer( final Catalog cat, final String ruleset, final Host host, final Question q ){
		final List<PerUnit.Unit> units = Questions.unitsFor(q);
		List<Entry> entries = new ArrayList<>();
		entries.add(Entry.item("Nothing", "just the number itself").current(q.per == null));
		for (PerUnit.Unit u : units){
			entries.add(Entry.item(u.singular.substring(0, 1).toUpperCase() + u.singular.substring(1), "divided by the " + u.plural + " you played").current(u == q.per));
		}
		show(WndUimsdList.menu("Per", "Divide the answer by how much you played, so runs of different lengths can be compared. Quiet floors and short runs count too.", entries,
				new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				builder(cat, ruleset, host, q.withPer(i == 0 ? null : units.get(i - 1)));
			}
		}).restoreWith(() -> pickPer(cat, ruleset, host, q)));
	}

	private static void pickSplit( final Catalog cat, final String ruleset, final Host host, final Question q, final DatasetDef def ){
		pickSplitPending(cat, ruleset, host, q, def, new ArrayList<>(q.groupBy));
	}

	private static void pickSplitPending(final Catalog cat, final String ruleset, final Host host,
										  final Question q, final DatasetDef def, final List<String> chosen){
		final List<ColumnDef> cols = Questions.groupable(def, q.showInternal);
		List<Entry> entries = new ArrayList<>();
		String lastGroup = null;
		for (ColumnDef c : cols){
			String g = c.group.isEmpty() ? "More" : c.group;
			if (!g.equals(lastGroup)){
				entries.add(Entry.heading(g));
				lastGroup = g;
			}
			entries.add(Entry.item(c.label, c.description).checked(chosen.contains(c.id)));
		}
		show(new WndUimsdList("Split by", "Tick up to two things to get one answer for each. Nothing ticked gives a single answer. Then Done.", true, entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				String id = cols.get(i).id;
				if (checked){
					if (!chosen.contains(id)) chosen.add(id);
					while (chosen.size() > 2) chosen.remove(0);
				} else {
					chosen.remove(id);
				}
				//The oldest choice is replaced at the two-field limit; its check must disappear too.
				int item = 0;
				for (Entry entry : entries) if (entry.isItem()) entry.checked(chosen.contains(cols.get(item++).id));
			}
		}){
			@Override
			protected void onDone(){
				hide();
				builder(cat, ruleset, host, q.withGroupBy(chosen));
			}
		}.restoreWith(() -> pickSplitPending(cat, ruleset, host, q, def, new ArrayList<>(chosen))));
	}

	private static void editFilters( final Catalog cat, final String ruleset, final Host host, final Question q ){
		//the full filter workflow, borrowed: it works on a table model, so a scratch one holds the question's conditions while they are edited
		final UimsdModel scratch = new UimsdModel(ruleset, "");
		scratch.setCatalog(cat);
		scratch.showQuery(Questions.toQuery(q, cat.dataset(q.dataset).def));
		final DatasetDef def = cat.dataset(q.dataset).def;
		List<Entry> entries = new ArrayList<>();
		if (q.filters.isEmpty()) entries.add(Entry.note("No conditions: the whole history is looked at."));
		else for (Filter f : q.filters) entries.add(Entry.item(StatusText.describe(def, f), "tap to remove"));
		entries.add(Entry.heading("Add"));
		entries.add(Entry.item("Add a condition", "for example: class is Cleric, difficulty is at least 4, floor is 5"));
		final int count = q.filters.size();
		showKept(WndUimsdList.menu("Only where", "Conditions narrow the question. All of them have to be true.", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				if (i < count){
					builder(cat, ruleset, host, q.removeFilter(i));
					return;
				}
				scratch.setListener(new UimsdModel.Listener(){
					@Override
					public void changed(){
						if (scratch.query().filters.size() != count) builder(cat, ruleset, host, q.withFilters(scratch.query().filters));
					}
				});
				WndUimsdFilter.add(scratch);
			}
		}).restoreWith(() -> editFilters(cat, ruleset, host, q)));
	}

	// ---- the answer ----

	public static void answer( final Catalog cat, final String ruleset, final Host host, final Question q ){
		final Questions.Answer a = Questions.answer(cat, q, ruleset);
		List<Entry> entries = new ArrayList<>();
		if (a.isEmpty()){
			entries.add(Entry.note(a.note));
		} else {
			boolean single = a.lines.size() == 1 && a.lines.get(0).label.equals("All");
			int shown = 0;
			for (Questions.Line l : a.lines){
				if (shown++ >= MAX_LINES) break;
				entries.add(single ? Entry.item(l.value, l.detail) : Entry.item(l.label + ":  " + l.value, l.detail));
			}
			if (a.lines.size() > MAX_LINES) entries.add(Entry.note((a.lines.size() - MAX_LINES) + " more. Open it as a table to see them all."));
			if (!a.note.isEmpty()) entries.add(Entry.note(a.note));
		}
		entries.add(Entry.heading("Next"));
		entries.add(Entry.item("Open as a table", "the same question with every filter, sort and column tool"));
		final boolean canSave = host != null && host.root() != null;
		if (canSave) entries.add(Entry.item("Save this question...", "Keep the exact wording and choices under a name, then ask it again as history grows."));
		entries.add(Entry.item("Change the question", ""));
		final List<Entry> all = entries;
		showKept(WndUimsdList.menu("Answer", a.question + "?", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				int items = 0;
				for (Entry e : all) if (e.isItem()) items++;
				int open = items - (canSave ? 3 : 2);
				if (i == open){
					WndUimsdList.closeAll();
					if (host != null) host.openAsTable(a.query, new Runnable(){
						@Override
						public void run(){
							//Back from the table: the question and its answer, as they were
							builder(cat, ruleset, host, q);
							answer(cat, ruleset, host, q);
						}
					});
				} else if (canSave && i == open + 1){
					saveQuestion(cat, host, q);
				} else if (i == items - 1){
					builder(cat, ruleset, host, q);
				}
			}
		}).restoreWith(() -> answer(cat, ruleset, host, q)));
	}

	private static void saveQuestion( final Catalog cat, final Host host, final Question q ){
		final Dataset dataset = cat == null ? null : cat.dataset(q.dataset);
		if (host == null || host.root() == null || dataset == null) return;
		show(new WndUimsdTextInput("Save this question", "A name for this question (up to " + SavedViews.MAX_NAME + " characters). An existing saved view with the same name is replaced.",
				"", SavedViews.MAX_NAME, false, "Save", "Cancel", (positive, text) -> {
			if (!positive) return;
			SavedViews views = SavedViews.load(SavedViews.fileIn(host.root()));
			SavedViews.Result result = views.saveQuestion(text, q, dataset.def, System.currentTimeMillis());
			try {
				if (result == SavedViews.Result.SAVED || result == SavedViews.Result.REPLACED)
					views.store(SavedViews.fileIn(host.root()));
			} catch (java.io.IOException failure){
				show(new WndUimsdText("UIMSD", "The question could not be saved: " + failure.getMessage(), false));
				return;
			}
			String message = result == SavedViews.Result.SAVED ? "Question saved."
					: result == SavedViews.Result.REPLACED ? "Question saved, replacing the view with that name."
					: result == SavedViews.Result.TOO_MANY ? "You have " + SavedViews.MAX_VIEWS + " saved views. Delete one first."
					: "That is not a usable name.";
			show(new WndUimsdText("UIMSD", message, false));
		}));
	}
}
