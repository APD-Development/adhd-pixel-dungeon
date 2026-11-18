package com.shatteredpixel.shatteredpixeldungeon.apd.report;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellFormatter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Dataset;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Filter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.GroupDetail;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.GroupRow;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Op;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Query;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryEngine;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ResultTable;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Scope;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.SeedFloors;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.StatusText;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdHome;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ViewDef;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

//Builds Reports from the existing UIMSD architecture: the Home model, QueryResults, ResultTables, GroupDetails and the datasets. There is no
//second statistics system here: every number is a CellText (or AggText) that the tables themselves would show, with its state kept.
//Hidden information follows the same rule as the screens: only for finished runs, and only when the caller asks for it.
public final class ReportBuilder {

	private ReportBuilder(){}

	//a very large table is cut here, and the report says how many rows are not listed
	public static final int MAX_ROWS = 1000;

	private static final Scope EVERY_RUN = Scope.DEFAULT.withRules(Scope.Rules.ALL, "").withUnfinished(true).withUnknownRuleset(true).withMixedRuleset(true);

	static Report.Cell cell( CellText t ){
		return new Report.Cell(t.text, t.state, t.small);
	}

	private static Report start( String title, long now, String version, String... about ){
		Report r = new Report(title, now, version);
		for (String a : about) if (a != null && !a.isEmpty()) r.about.add(a);
		return r;
	}

	//---- lifetime / overall summary ----

	public static Report lifetime( Catalog cat, String currentRuleset, String rulesLabel, boolean allRules, long now, String version ){
		UimsdHome home = UimsdHome.build(cat, currentRuleset, allRules);
		Report r = start("UIMSD summary", now, version, home.scopeNote);
		if (rulesLabel != null && !rulesLabel.isEmpty()) r.about.add("Your current rules: " + rulesLabel + ".");
		if (home.isEmpty()){
			r.add(new Report.Note(Report.NoteKind.NOTE, "There are no finished runs in scope yet."));
			return r;
		}
		for (UimsdHome.Card c : home.cards){
			r.add(new Report.Heading(2, c.title));
			Report.Facts f = new Report.Facts(Report.Provenance.RECORDED);
			for (UimsdHome.Line l : c.lines){
				if (l.heading) f.section(l.label);
				else f.add(l.label, new Report.Cell(l.value, l.state), l.detail);
			}
			r.add(f);
		}
		return r;
	}

	//---- the current view / any query ----

	//where a result's rows come from, judged by its Basis column (seed floors) and recorded otherwise
	static Report.Provenance provenanceOf( QueryResult q ){
		DatasetDef def = q.def;
		ColumnDef basis = null;
		for (ColumnDef c : def.columns()) if (c.id.endsWith(".basis")) basis = c;
		if (basis == null) return Report.Provenance.RECORDED;
		boolean recorded = false, canonical = false;
		List<Row> rows = new ArrayList<>();
		if (q.isGrouped()){
			for (GroupRow g : q.groups) rows.addAll(g.rows);
		} else {
			rows.addAll(q.rows);
		}
		for (Row row : rows){
			if (!row.isKnown(basis.index())) continue;
			long v = row.num(basis.index());
			if (v == SeedFloors.BASIS_RECORDED) recorded = true;
			else if (v == SeedFloors.BASIS_COMPARISON){
				recorded = true;      //a comparison sets the run's own floors beside the replay: both kinds of data
				canonical = true;
			} else canonical = true;
		}
		if (canonical && recorded) return Report.Provenance.MIXED;
		return canonical ? Report.Provenance.CANONICAL : Report.Provenance.RECORDED;
	}

	static Report.Table tableOf( QueryResult q, String title, int maxRows ){
		int total = q.isGrouped() ? q.groups.size() : q.rows.size();
		ResultTable t = ResultTable.build(q, 0, Math.max(1, Math.min(Math.max(1, total), maxRows)));
		Report.Table out = new Report.Table(title, provenanceOf(q));
		for (ResultTable.Header h : t.headers){
			out.headers.add(h.label);
			out.coverage.add(h.coverage);
		}
		for (ResultTable.Line line : t.lines){
			Report.Cell[] cells = new Report.Cell[line.cells.length];
			for (int i = 0; i < cells.length; i++) cells[i] = cell(line.cells[i]);
			out.rows.add(cells);
		}
		out.omitted = Math.max(0, total - t.lines.size());
		return out;
	}

	public static Report view( QueryResult q, String viewTitle, String rulesLabel, long now, String version ){
		Report r = start(viewTitle, now, version);
		for (StatusText.Line l : StatusText.lines(q, viewTitle, rulesLabel)) r.about.add(l.text);
		for (com.shatteredpixel.shatteredpixeldungeon.apd.query.Filter f : q.query.filters){
			r.about.add("Filter: " + StatusText.describe(q.def, f));
		}
		if (q.query.showInternal) r.add(new Report.Note(Report.NoteKind.WARN, "This report includes hidden information (true item identities and similar) of finished runs. Spoilers: share with care."));
		r.add(tableOf(q, "", MAX_ROWS));
		return r;
	}

	//---- one group (a class, an item kind, an enemy, a death cause...) ----

	public static Report group( QueryResult q, GroupRow g, String rulesLabel, long now, String version ){
		GroupDetail d = GroupDetail.build(q, g);
		Report r = start(d.title, now, version);
		for (StatusText.Line l : StatusText.lines(q, q.def.title, rulesLabel)) r.about.add(l.text);
		Report.Facts keys = new Report.Facts(provenanceOf(q));
		for (GroupDetail.Line l : d.keys) keys.add(l.label, new Report.Cell(l.value, l.state, l.small), l.detail);
		r.add(keys);
		Report.Facts f = new Report.Facts(provenanceOf(q));
		for (GroupDetail.Line l : d.lines){
			if (l.section) f.section(l.label);
			else f.add(l.label, new Report.Cell(l.value, l.state, l.small), l.detail);
		}
		r.add(f);
		return r;
	}

	//what a group is called: its key values ("Mage", "3 - Standard / 1")
	public static String groupName( QueryResult q, GroupRow g ){
		StringBuilder b = new StringBuilder();
		for (ColumnDef c : q.groupKeys){
			if (b.length() > 0) b.append(" / ");
			b.append(CellFormatter.cell(q.query, g.sample, c).text);
		}
		return b.length() == 0 ? "group" : b.toString();
	}

	//---- comparing two groups of the same table (two characters, two item kinds, two enemies...) ----

	public static Report compareGroups( QueryResult q, GroupRow a, GroupRow b, String rulesLabel, long now, String version ){
		GroupDetail da = GroupDetail.build(q, a), db = GroupDetail.build(q, b);
		Report r = start("Group comparison", now, version);
		for (StatusText.Line l : StatusText.lines(q, q.def.title, rulesLabel)) r.about.add(l.text);
		r.about.add("A: " + groupName(q, a) + " (" + a.n + (a.n == 1 ? " " + q.def.unitSingular() + ")" : " " + q.def.unitPlural() + ")"));
		r.about.add("B: " + groupName(q, b) + " (" + b.n + (b.n == 1 ? " " + q.def.unitSingular() + ")" : " " + q.def.unitPlural() + ")"));
		Report.Table t = new Report.Table("", provenanceOf(q));
		t.headers.add("");
		t.headers.add("A: " + groupName(q, a));
		t.headers.add("B: " + groupName(q, b));
		for (int i = 0; i < 3; i++) t.coverage.add("");
		//Both sheets come from the same dataset definition and query, so their non-section lines have the same order.
		//Labels are display text and may legitimately repeat in different sections; using them as map keys would make an
		//earlier row borrow the value of the last row with that label.
		java.util.List<GroupDetail.Line> other = new java.util.ArrayList<>();
		for (GroupDetail.Line l : db.lines) if (!l.section) other.add(l);
		int otherIndex = 0;
		for (GroupDetail.Line l : da.lines){
			if (l.section) continue;
			GroupDetail.Line o = otherIndex < other.size() ? other.get(otherIndex++) : null;
			Report.Cell cb = o == null ? new Report.Cell("?", CellText.UNKNOWN) : new Report.Cell(o.value, o.state, o.small);
			t.rows.add(new Report.Cell[]{ new Report.Cell(l.label, CellText.EXACT), new Report.Cell(l.value, l.state, l.small), cb });
		}
		r.add(t);
		return r;
	}

	//---- one run ----

	//a run in a few words (class, outcome, date: the first shown columns), for lists and report headings
	public static String describeRun( Catalog cat, Row run, boolean hidden ){
		return describe(cat.dataset(Datasets.RUNS), run, hidden);
	}

	private static String describe( Dataset runs, Row run, boolean hidden ){
		Query q = Query.of(Datasets.RUNS).withShowInternal(hidden);
		StringBuilder sb = new StringBuilder();
		int n = 0;
		for (ColumnDef c : runs.def.columns()){
			if (!c.defaultVisible || n >= 3) continue;
			sb.append(n++ > 0 ? ", " : "").append(CellFormatter.cell(q, run, c).text);
		}
		return sb.length() == 0 ? run.runId : sb.toString();
	}

	private static Query forRun( Catalog cat, String datasetId, String runColumn, String runId, boolean hidden ){
		Dataset ds = cat.dataset(datasetId);
		return Query.of(datasetId).withScope(EVERY_RUN).withShowInternal(hidden).withColumns(ds.def.defaultColumnIds())
				// Identifiers are case-sensitive, unlike player-entered text searches.
				.addFilter(Filter.text(runColumn, Op.GROUP_TEXT, runId));
	}

	public static Report run( Catalog cat, Row run, boolean includeHidden, String currentRuleset, long now, String version ){
		Dataset runs = cat.dataset(Datasets.RUNS);
		boolean hidden = includeHidden && run.finished;
		Report r = start("Run report", now, version);
		r.about.add(describe(runs, run, hidden));
		r.about.add(run.finished ? "A finished run." : "A run that is not finished: its hidden information is left out of this report.");
		if (hidden) r.add(new Report.Note(Report.NoteKind.WARN, "This report includes hidden information of the run (true identities and similar). Spoilers: share with care."));
		Query q = Query.of(Datasets.RUNS).withShowInternal(hidden);
		Report.Facts f = new Report.Facts(Report.Provenance.RECORDED);
		String lastGroup = null;
		for (ColumnDef c : runs.def.columns()){
			if (c.isInternal() && !hidden) continue;
			if (!c.group.equals(lastGroup)){
				f.section(c.group.isEmpty() ? "Details" : c.group);
				lastGroup = c.group;
			}
			CellText t = CellFormatter.cell(q, run, c);
			f.add(c.label, cell(t), t.state == CellText.EXACT ? "" : CellFormatter.explain(t.state));
		}
		r.add(new Report.Heading(2, "The run"));
		r.add(f);
		String[][] parts = { { Datasets.ITEMS, "item.run", "Items" }, { Datasets.ENEMIES, "enemy.run", "Enemies met" }, { Datasets.DEATHS, "death.run", "How it ended" } };
		for (String[] p : parts){
			if (cat.dataset(p[0]) == null) continue;
			QueryResult res = QueryEngine.run(cat.dataset(p[0]), forRun(cat, p[0], p[1], run.runId, hidden), currentRuleset);
			if (res.rows.isEmpty()) continue;
			r.add(new Report.Heading(2, p[2]));
			r.add(tableOf(res, "", MAX_ROWS));
		}
		return r;
	}

	//---- comparing two runs ----

	public static Report compareRuns( Catalog cat, Row a, Row b, boolean includeHidden, long now, String version ){
		Dataset runs = cat.dataset(Datasets.RUNS);
		boolean hidden = includeHidden && a.finished && b.finished;
		Report r = start("Run comparison", now, version);
		r.about.add("A: " + describe(runs, a, hidden) + (a.finished ? "" : " (not finished)"));
		r.about.add("B: " + describe(runs, b, hidden) + (b.finished ? "" : " (not finished)"));
		if (hidden) r.add(new Report.Note(Report.NoteKind.WARN, "This report includes hidden information. Spoilers: share with care."));
		Query qa = Query.of(Datasets.RUNS).withShowInternal(includeHidden && a.finished);
		Query qb = Query.of(Datasets.RUNS).withShowInternal(includeHidden && b.finished);
		Report.Table t = new Report.Table("", Report.Provenance.RECORDED);
		t.headers.add("");
		t.headers.add("Run A");
		t.headers.add("Run B");
		t.headers.add("B minus A");
		for (int i = 0; i < 4; i++) t.coverage.add("");
		for (ColumnDef c : runs.def.columns()){
			if (c.isInternal() && !hidden) continue;
			CellText ca = CellFormatter.cell(qa, a, c), cb = CellFormatter.cell(qb, b, c);
			String diff = "";
			if (c.type == ColumnDef.Type.NUM && ca.state == CellText.EXACT && cb.state == CellText.EXACT && a.isKnown(c.index()) && b.isKnown(c.index())
					&& c.format == ColumnDef.Format.PLAIN){
				BigInteger d = BigInteger.valueOf(b.num(c.index())).subtract(BigInteger.valueOf(a.num(c.index())));
				diff = (d.signum() > 0 ? "+" : "") + d;
			}
			t.rows.add(new Report.Cell[]{ new Report.Cell(c.label, CellText.EXACT), cell(ca), cell(cb), new Report.Cell(diff, CellText.EXACT) });
		}
		r.add(t);
		return r;
	}

	//---- seed floors: recorded and canonical, never blurred ----

	public static Report seedFloors( Catalog cat, String runId, String currentRuleset, long now, String version ){
		Report r = start("Seed floors", now, version, "Run " + runId);
		//the rules the run was played under: its difficulty and balance version decide what the canonical floors are built from
		Dataset runs = cat.dataset(Datasets.RUNS);
		if (runs != null){
			for (Row run : runs.rows){
				if (!runId.equals(run.runId)) continue;
				Query rq = Query.of(Datasets.RUNS);
				ColumnDef dc = runs.def.column(com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.DIFFICULTY);
				ColumnDef bc = runs.def.column(com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.DIFFICULTY_BALANCE);
				if (dc != null){
					String d = CellFormatter.cell(rq, run, dc).text;
					String b = bc != null && run.isKnown(bc.index()) ? ", balance version " + run.num(bc.index()) : "";
					r.about.add("Difficulty: " + (run.isKnown(dc.index()) ? d + b : "not recorded (the run is older than difficulty); canonical floors are built for the standard difficulty 3"));
				}
				break;
			}
		}
		if (cat.dataset(SeedFloors.ITEMS) == null){
			r.add(new Report.Note(Report.NoteKind.NOTE, "The seed floors of this run have not been built yet. Open them from the run first."));
			return r;
		}
		//which floors are recorded and which are canonical, from the Basis column
		Dataset items = cat.dataset(SeedFloors.ITEMS);
		int floorCol = items.def.indexOf("sf.floor"), basisCol = items.def.indexOf("sf.basis");
		Map<Integer, Integer> basisOfFloor = new LinkedHashMap<>();
		for (Row row : items.rows) if (row.isKnown(floorCol) && row.isKnown(basisCol)) basisOfFloor.put((int) row.num(floorCol), (int) row.num(basisCol));
		TreeSet<Integer> recorded = new TreeSet<>(), canonical = new TreeSet<>();
		for (Map.Entry<Integer, Integer> e : basisOfFloor.entrySet()) (e.getValue() == SeedFloors.BASIS_RECORDED ? recorded : canonical).add(e.getKey());
		Dataset creatures = cat.dataset(SeedFloors.MOBS);
		if (creatures != null){
			int fc = creatures.def.indexOf("sm.floor");
			for (Row row : creatures.rows) if (row.isKnown(fc)) canonical.add((int) row.num(fc));
		}
		Report.Facts f = new Report.Facts(canonical.isEmpty() ? Report.Provenance.RECORDED : Report.Provenance.MIXED);
		f.add("Floors with the run's own record", new Report.Cell(Integer.toString(recorded.size()), CellText.EXACT), "authoritative");
		f.add("Floors shown as canonical", new Report.Cell(Integer.toString(canonical.size()), CellText.INFERRED), "a replay of the seed, not recorded history");
		r.add(new Report.Heading(2, "Summary"));
		r.add(f);
		String[][] views = { { SeedFloors.ITEMS, "Items on every floor" }, { SeedFloors.MOBS, "Creatures on the canonical floors" },
				{ SeedFloors.COMPARE, "Recorded against canonical, floor by floor" } };
		for (String[] v : views){
			Dataset ds = cat.dataset(v[0]);
			ViewDef vd = cat.view(v[0]);
			if (ds == null || vd == null) continue;
			QueryResult res = QueryEngine.run(ds, vd.initialQuery().withShowInternal(true), currentRuleset);
			if (res.rows.isEmpty()) continue;
			r.add(new Report.Heading(2, v[1]));
			r.add(tableOf(res, "", MAX_ROWS));
		}
		return r;
	}
}
