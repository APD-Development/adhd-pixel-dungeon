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

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//The Summary / Home page of UIMSD: a handful of cards with the numbers most players want (runs, wins, how each class does, the gear they
//use, what kills them, recent runs, records), each leading on to the full advanced view behind it.
//
//It is NOT a second statistics system. Everything is computed by the same query engine, over the same views, under the same scope and
//disclosure rules as the advanced screens, and every number is formatted by the same CellFormatter, so a "~", ">=" or "?" means here
//exactly what it means there and the totals always match what the advanced views show. Only columns that are never hidden information
//are used (a test checks it), and runs that are not finished are never counted. Pure data: the scene only draws it.
public final class UimsdHome {

	public static final class Line {
		public final boolean heading;
		public final String label;
		public final String value;
		public final String detail;
		public final int state;

		Line( boolean heading, String label, String value, String detail, int state ){
			this.heading = heading;
			this.label = label;
			this.value = value;
			this.detail = detail == null ? "" : detail;
			this.state = state;
		}
	}

	//a button that opens one of the advanced views
	public static final class Link {
		public final String label;
		public final String viewId;

		Link( String label, String viewId ){
			this.label = label;
			this.viewId = viewId;
		}
	}

	public static final class Card {
		public final String id;
		public String title;
		public final List<Line> lines = new ArrayList<>();
		public final List<Link> links = new ArrayList<>();

		Card( String id, String title ){
			this.id = id;
			this.title = title;
		}

		void heading( String text ){
			lines.add(new Line(true, text, "", "", CellText.EXACT));
		}

		void line( String label, String value, String detail, int state ){
			lines.add(new Line(false, label, value, detail, state));
		}

		void link( String label, String viewId ){
			links.add(new Link(label, viewId));
		}
	}

	public final List<Card> cards = new ArrayList<>();
	//finished runs the cards are about, and finished runs played under other rules that they leave out
	public final int runs;
	public final int runsElsewhere;
	public final boolean allRules;
	public final String scopeNote;

	private static final int LIST = 4;

	//not a table: the link on the Ask card opens the question builder
	public static final String ASK_LINK = "!ask";

	//true when the page covers runs played under more than one set of rules (only possible with all rules shown): its numbers are not comparable
	public final boolean mixedRulesets;

	private UimsdHome( int runs, int runsElsewhere, boolean allRules, int rulesets ){
		this.runs = runs;
		this.runsElsewhere = runsElsewhere;
		this.allRules = allRules;
		this.mixedRulesets = allRules && rulesets > 1;
		this.scopeNote = (allRules ? (mixedRulesets
				? "MIXED RULES: all finished runs, played under " + rulesets + " different sets of rules (difficulty, challenges, exotic-item setting). The numbers below are not directly comparable: tap Rules to see only your own, or open the Difficulty table to compare like with like."
				: "All finished runs, whatever rules they were played under.") : "Finished runs played under your current rules.")
				+ (!allRules && runsElsewhere > 0 ? " " + runsElsewhere + (runsElsewhere == 1 ? " run was" : " runs were") + " played under other rules (another difficulty, challenges or exotic setting): tap Rules to include them." : "")
				+ " Runs still going, runs with unknown or changed rules, and repeated imported copies are left out; table Options can include them.";
	}

	public boolean isEmpty(){
		return runs == 0;
	}

	//The empty Home must keep rule selection separate from run status: Rules never means "include unfinished".
	public String emptyMessage(){
		if (!isEmpty()) return "";
		if (!allRules && runsElsewhere > 0){
			return "No finished runs under your current rules. Tap Rules to include " + runsElsewhere
					+ (runsElsewhere == 1 ? " finished run" : " finished runs")
					+ " played under other rules. Runs still going are available in Tables > Options > Include runs still going.";
		}
		return "No finished runs yet. Runs still going are available in Tables > Options > Include runs still going. Home statistics appear when a recorded run ends.";
	}

	public Card card( String id ){
		for (Card c : cards) if (c.id.equals(id)) return c;
		return null;
	}

	//---- building ----

	public static UimsdHome build( Catalog cat, String currentRuleset, boolean allRules ){
		Scope scope = allRules ? Scope.DEFAULT.withRules(Scope.Rules.ALL, "") : Scope.DEFAULT;
		Dataset runsDs = cat == null ? null : cat.dataset(Datasets.RUNS);
		if (runsDs == null) return new UimsdHome(0, 0, allRules, 0);

		Query rq = Query.of(Datasets.RUNS).withScope(scope).withMinSample(0);
		QueryResult runs = QueryEngine.run(runsDs, rq, currentRuleset);
		int elsewhere = 0;
		if (!allRules){
			Query all = Query.of(Datasets.RUNS).withScope(Scope.DEFAULT.withRules(Scope.Rules.ALL, "")).withMinSample(0);
			elsewhere = Math.max(0, QueryEngine.run(runsDs, all, currentRuleset).rows.size() - runs.rows.size());
		}
		UimsdHome h = new UimsdHome(runs.rows.size(), elsewhere, allRules, runs.stats.rulesetsInScope);
		if (h.runs == 0) return h;

		h.cards.add(overview(runsDs, rq, runs));
		Card navigation = new Card("navigate", "Explore statistics");
		String[][] destinations = {{"runs", "Run history"}, {"characters", "Characters"}, {"items", "Equipment and items"}, {"enemies", "Enemies"}, {"bosses", "Bosses"}, {"challenges", "Challenges"}, {"difficulty", "Difficulty"}};
		for (String[] destination : destinations) if (cat.view(destination[0]) != null) navigation.link(destination[1], destination[0]);
		if (!navigation.links.isEmpty()) h.cards.add(navigation);
		//the friendly front door to everything recorded
		Card ask = new Card("ask", "Ask a question");
		ask.heading("Count it, add it up, average it, compare it");
		ask.line("Ask about", "anything you did", "Pick what you want to know in a few taps: how many casts per floor, your best floor as a Cleric, what you wore when you won.", CellText.EXACT);
		ask.link("Ask a question", ASK_LINK);
		h.cards.add(ask);
		Card c = characters(cat, scope, currentRuleset);
		if (c != null) h.cards.add(c);
		c = difficulty(cat, currentRuleset);
		if (c != null) h.cards.add(c);
		c = gear(cat, scope, currentRuleset);
		if (c != null) h.cards.add(c);
		c = enemies(cat, scope, currentRuleset);
		if (c != null) h.cards.add(c);
		h.cards.add(recent(cat, runsDs, scope, currentRuleset));
		c = records(runsDs, rq, runs);
		if (c != null) h.cards.add(c);
		//a record or a difficulty comparison taken across rulesets must say so on the card itself
		if (h.mixedRulesets){
			for (Card card : h.cards) if ((card.id.equals("records") || card.id.equals("difficulty")) && !card.title.endsWith("(mixed rules)")) card.title += " (mixed rules)";
		}
		return h;
	}

	private static String num( DatasetDef def, Query q, Aggregate a, List<Row> rows, int[] stateOut, String[] detailOut ){
		AggValue v = QueryEngine.evaluate(def, q, a, rows);
		if (v == null){
			stateOut[0] = CellText.HIDDEN;
			return "hidden";
		}
		CellFormatter.AggText t = CellFormatter.aggregate(def, a, v);
		stateOut[0] = t.state;
		detailOut[0] = t.detail;
		return t.text;
	}

	private static void stat( Card c, String label, DatasetDef def, Query q, Aggregate a, List<Row> rows ){
		int[] st = new int[1];
		String[] detail = new String[]{ "" };
		String text = num(def, q, a, rows, st, detail);
		if (st[0] == CellText.HIDDEN) return;   //needs hidden information: not shown at all
		c.line(label, text, detail[0], st[0]);
	}

	private static Card overview( Dataset ds, Query q, QueryResult r ){
		Card c = new Card("runs", "Your runs");
		DatasetDef def = ds.def;
		c.line("Finished runs", String.valueOf(r.rows.size()), "", CellText.EXACT);
		stat(c, "Won", def, q, Aggregate.count().where(FactKeys.OUTCOME, 1), r.rows);
		stat(c, "Died", def, q, Aggregate.count().where(FactKeys.OUTCOME, 0), r.rows);
		stat(c, "Abandoned", def, q, Aggregate.count().where(FactKeys.OUTCOME, 2), r.rows);
		stat(c, "Win rate", def, q, Aggregate.rate(FactKeys.OUTCOME, 1), r.rows);
		stat(c, "Deepest floor", def, q, Aggregate.of(Aggregate.Fn.MAX, FactKeys.DEEPEST), r.rows);
		stat(c, "Best score", def, q, Aggregate.of(Aggregate.Fn.MAX, FactKeys.SCORE), r.rows);
		c.link("All runs", "runs");
		return c;
	}

	private static QueryResult viewRows( Catalog cat, String viewId, Scope scope, String ruleset ){
		ViewDef v = cat.view(viewId);
		if (v == null) return null;
		Dataset ds = cat.datasetOf(v);
		if (ds == null) return null;
		return QueryEngine.run(ds, v.initialQuery().withScope(scope).withMinSample(0), ruleset);
	}

	private static String keyText( QueryResult r, GroupRow g ){
		StringBuilder b = new StringBuilder();
		for (ColumnDef col : r.groupKeys){
			if (b.length() > 0) b.append(" / ");
			b.append(CellFormatter.cell(r.query, g.sample, col).text);
		}
		return b.toString();
	}

	private static Card characters( Catalog cat, Scope scope, String ruleset ){
		QueryResult r = viewRows(cat, "characters", scope, ruleset);
		if (r == null || !r.isGrouped() || r.groups.isEmpty()) return null;
		Card c = new Card("characters", "Characters");
		DatasetDef def = r.def;
		for (int i = 0; i < r.groups.size() && i < LIST; i++){
			GroupRow g = r.groups.get(i);
			int[] st = new int[1];
			String[] d1 = new String[]{ "" }, d2 = new String[]{ "" };
			String win = num(def, r.query, Aggregate.rate(FactKeys.OUTCOME, 1), g.rows, st, d1);
			int s1 = st[0];
			String deepest = num(def, r.query, Aggregate.of(Aggregate.Fn.MAX, FactKeys.DEEPEST), g.rows, st, d2);
			int state = Math.max(s1, st[0]) == CellText.HIDDEN ? CellText.EXACT : worst(s1, st[0]);
			c.line(keyText(r, g), g.n + (g.n == 1 ? " run" : " runs") + ", " + win + " won, deepest floor " + deepest, "", state);
		}
		c.link("All characters", "characters");
		return c;
	}

	//Runs by difficulty, across every ruleset (each difficulty is its own ruleset, so the page's rules choice would hide the others). Only shown when
	//there is something to compare: at least two groups (for example runs from before difficulty existed next to recorded ones).
	private static Card difficulty( Catalog cat, String ruleset ){
		QueryResult r = viewRows(cat, "difficulty", Scope.DEFAULT.withRules(Scope.Rules.ALL, ""), ruleset);
		if (r == null || !r.isGrouped() || r.groups.size() < 2) return null;
		Card c = new Card("difficulty", "Difficulty");
		DatasetDef def = r.def;
		for (int i = 0; i < r.groups.size() && i < LIST + 2; i++){
			GroupRow g = r.groups.get(i);
			ColumnDef level = r.groupKeys.get(0);
			String label = g.sample.isKnown(level.index()) ? CellFormatter.cell(r.query, g.sample, level).text : "Not recorded (older runs)";
			if (r.groupKeys.size() > 1 && g.sample.isKnown(r.groupKeys.get(1).index()) && g.sample.num(r.groupKeys.get(1).index()) > 0){
				label += " (balance " + g.sample.num(r.groupKeys.get(1).index()) + ")";
			}
			int[] st = new int[1];
			String[] d1 = new String[]{ "" };
			String win = num(def, r.query, Aggregate.rate(FactKeys.OUTCOME, 1), g.rows, st, d1);
			int state = st[0] == CellText.HIDDEN ? CellText.EXACT : st[0];
			c.line(label, g.n + (g.n == 1 ? " run" : " runs") + ", " + win + " won", "", state);
		}
		//a group is one difficulty, but its runs can still differ in challenges or exotic setting: say so instead of implying they are alike
		if (r.stats.rulesetsInScope > r.groups.size()){
			c.title = "Difficulty (mixed rules)";
			c.lines.add(0, new Line(false, "Mixed rules", "not directly comparable",
					"some of these runs used other challenges or exotic settings; open the Challenges table to compare like with like", CellText.EXACT));
		}
		c.link("Difficulty table", "difficulty");
		return c;
	}

	//the worse (less certain) of two cell states, for a line made of several numbers
	private static int worst( int a, int b ){
		return rank(a) >= rank(b) ? a : b;
	}

	private static int rank( int state ){
		switch (state){
			case CellText.UNKNOWN: return 5;
			case CellText.LOWER: return 4;
			case CellText.INFERRED: return 3;
			case CellText.LEGACY: return 2;
			default: return 1;
		}
	}

	private static Card gear( Catalog cat, Scope scope, String ruleset ){
		Card c = new Card("gear", "Gear you use");
		boolean any = false;
		String[][] kinds = { { "weapons", "Weapons" }, { "armor", "Armor" } };
		for (String[] k : kinds){
			QueryResult r = viewRows(cat, k[0], scope, ruleset);
			if (r == null || !r.isGrouped()) continue;
			//how many runs equipped one: each run counted once however many it equipped
			final Aggregate equippedRuns = Aggregate.count().where("item.equipped", 1).perDistinct("item.run");
			final List<GroupRow> groups = new ArrayList<>(r.groups);
			final List<Double> score = new ArrayList<>();
			final DatasetDef def = r.def;
			final Query q = r.query;
			List<GroupRow> keep = new ArrayList<>();
			for (GroupRow g : groups){
				AggValue v = QueryEngine.evaluate(def, q, equippedRuns, g.rows);
				if (v != null && v.known && v.value > 0){
					keep.add(g);
					score.add(v.value);
				}
			}
			if (keep.isEmpty()) continue;
			Integer[] order = new Integer[keep.size()];
			for (int i = 0; i < order.length; i++) order[i] = i;
			final List<Double> sc = score;
			java.util.Arrays.sort(order, new Comparator<Integer>() {
				@Override
				public int compare( Integer a, Integer b ){
					return Double.compare(sc.get(b), sc.get(a));
				}
			});
			c.heading(k[1]);
			for (int i = 0; i < order.length && i < 3; i++){
				GroupRow g = keep.get(order[i]);
				AggValue v = QueryEngine.evaluate(def, q, equippedRuns, g.rows);
				CellFormatter.AggText t = CellFormatter.aggregate(def, equippedRuns, v);
				c.line(keyText(r, g), "equipped in " + t.text + (v.value == 1 ? " run" : " runs"), t.detail, t.state);
			}
			c.link(k[1], k[0]);
			any = true;
		}
		return any ? c : null;
	}

	private static Card enemies( Catalog cat, Scope scope, String ruleset ){
		QueryResult r = viewRows(cat, "enemies", scope, ruleset);
		if (r == null || !r.isGrouped() || r.groups.isEmpty()) return null;
		Card c = new Card("enemies", "Enemies");
		final DatasetDef def = r.def;
		final Query q = r.query;
		c.heading("Met most often");
		for (int i = 0; i < r.groups.size() && i < 3; i++){
			GroupRow g = r.groups.get(i);
			c.line(keyText(r, g), g.n + (g.n == 1 ? " encounter" : " encounters"), "", CellText.EXACT);
		}
		//what ended runs: the enemies with the most hero deaths
		final Aggregate kills = Aggregate.of(Aggregate.Fn.SUM, "enemy.heroDeaths");
		List<GroupRow> deadly = new ArrayList<>();
		final java.util.Map<GroupRow, AggValue> deathTotals = new java.util.IdentityHashMap<>();
		for (GroupRow g : r.groups){
			AggValue v = QueryEngine.evaluate(def, q, kills, g.rows);
			if (v != null && v.known && v.value > 0) { deadly.add(g); deathTotals.put(g, v); }
		}
		Collections.sort(deadly, new Comparator<GroupRow>() {
			@Override
			public int compare( GroupRow a, GroupRow b ){
				return Double.compare(deathTotals.get(b).value, deathTotals.get(a).value);
			}
		});
		if (!deadly.isEmpty()){
			c.heading("Ended your runs");
			for (int i = 0; i < deadly.size() && i < 3; i++){
				GroupRow g = deadly.get(i);
				AggValue v = deathTotals.get(g);
				CellFormatter.AggText t = CellFormatter.aggregate(def, kills, v);
				c.line(keyText(r, g), t.text + (v.value == 1 ? " death" : " deaths"), t.detail, t.state);
			}
		}
		c.link("All enemies", "enemies");
		if (cat.view("deaths") != null) c.link("All deaths", "deaths");
		return c;
	}

	private static Card recent( Catalog cat, Dataset runsDs, Scope scope, String ruleset ){
		Card c = new Card("recent", "Recent runs");
		QueryResult r = viewRows(cat, "runs", scope, ruleset);
		if (r == null) return c;
		DatasetDef def = r.def;
		for (int i = 0; i < r.rows.size() && i < 5; i++){
			Row row = r.rows.get(i);
			CellText date = CellFormatter.cell(r.query, row, def.column(FactKeys.START_MILLIS));
			CellText cls = CellFormatter.cell(r.query, row, def.column(FactKeys.HERO_CLASS));
			CellText out = CellFormatter.cell(r.query, row, def.column(FactKeys.OUTCOME));
			CellText deep = CellFormatter.cell(r.query, row, def.column(FactKeys.DEEPEST));
			CellText score = CellFormatter.cell(r.query, row, def.column(FactKeys.SCORE));
			int state = worst(worst(out.state, deep.state), score.state);
			c.line(date.text + " - " + cls.text, out.text + ", floor " + deep.text + ", score " + score.text, "", state == CellText.HIDDEN ? CellText.EXACT : state);
		}
		c.link("All runs", "runs");
		return c;
	}

	private static Card records( Dataset ds, Query q, QueryResult r ){
		Card c = new Card("records", "Records");
		DatasetDef def = ds.def;
		record(c, "Best score", def, q, r.rows, FactKeys.SCORE, true, -1);
		record(c, "Deepest floor", def, q, r.rows, FactKeys.DEEPEST, true, -1);
		record(c, "Highest level", def, q, r.rows, FactKeys.HERO_LEVEL, true, -1);
		record(c, "Most HP healed in a run", def, q, r.rows, FactKeys.HEALED, true, -1);
		record(c, "Fastest win (turns)", def, q, r.rows, FactKeys.TURNS, false, 1);
		if (c.lines.isEmpty()) return null;
		c.link("All runs", "runs");
		return c;
	}

	//the run with the highest (or lowest) value of a column, among the runs where it is known; 'outcome' >= 0 limits it to runs with that outcome
	private static void record( Card c, String label, DatasetDef def, Query q, List<Row> rows, String col, boolean highest, int outcome ){
		ColumnDef cd = def.column(col);
		ColumnDef oc = def.column(FactKeys.OUTCOME);
		if (cd == null || cd.isInternal()) return;
		Row best = null;
		for (Row row : rows){
			if (!row.isKnown(cd.index)) continue;
			if (outcome >= 0 && (!row.isKnown(oc.index) || row.num(oc.index) != outcome)) continue;
			if (highest && row.num(cd.index) <= 0 && !col.equals(FactKeys.SCORE)) continue;
			if (best == null || (highest ? row.num(cd.index) > best.num(cd.index) : row.num(cd.index) < best.num(cd.index))) best = row;
		}
		if (best == null) return;
		CellText v = CellFormatter.cell(q, best, cd);
		CellText cls = CellFormatter.cell(q, best, def.column(FactKeys.HERO_CLASS));
		CellText date = CellFormatter.cell(q, best, def.column(FactKeys.START_MILLIS));
		c.line(label, v.text, cls.text + ", " + date.text, v.state);
	}

	//the page as plain text: for tests, logs and anything that cannot draw
	public String text(){
		StringBuilder b = new StringBuilder(scopeNote).append('\n');
		for (Card c : cards){
			b.append('\n').append(c.title).append('\n');
			for (Line l : c.lines){
				if (l.heading) b.append("  ").append(l.label).append(":\n");
				else b.append("  ").append(l.label).append(": ").append(l.value).append(l.detail.isEmpty() ? "" : "  (" + l.detail + ")").append('\n');
			}
		}
		return b.toString();
	}
}
