package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;

import java.util.ArrayList;
import java.util.List;

//A small made-up dataset (not one of the shipped ones) for testing the engine: the engine and the UI must work for any dataset.
final class QuerySupport {

	private QuerySupport(){}

	static final int NAME = 0, SCORE = 1, KIND = 2, SECRET = 3, DEPTH = 4, NOTE = 5;

	static DatasetDef def(){
		DatasetDef d = new DatasetDef("things", "Things", "made up");
		d.add(ColumnDef.text("name", "Name").shown());
		d.add(ColumnDef.num("score", "Score").shown().merge(ColumnDef.Merge.SUM));
		d.add(ColumnDef.enumeration("kind", "Kind").shown().label(0, "red").label(1, "green").label(2, "blue"));
		d.add(ColumnDef.num("secret", "Secret").internal());
		d.add(ColumnDef.num("depth", "Depth").shown().merge(ColumnDef.Merge.MAX));
		d.add(ColumnDef.text("note", "Note").internal());
		return d;
	}

	static Row row( DatasetDef d, String id, String name, Long score, Long kind, Long secret, boolean finished, String ruleset ){
		return rowIn(d, "run-" + id, id, name, score, kind, secret, finished, ruleset);
	}

	static Row rowIn( DatasetDef d, String runId, String id, String name, Long score, Long kind, Long secret, boolean finished, String ruleset ){
		Row r = new Row(id, null, runId, d.size());
		r.finished = finished;
		r.ruleset = ruleset;
		if (name != null) r.setText(NAME, name, Fact.PROV_RECORDED);
		if (score != null) r.setNum(SCORE, score, Fact.PROV_RECORDED);
		if (kind != null) r.setNum(KIND, kind, Fact.PROV_RECORDED);
		if (secret != null) r.setNum(SECRET, secret, Fact.PROV_RECORDED);
		return r;
	}

	static Dataset dataset( DatasetDef d, List<Row> rows ){
		List<Dataset.RunInfo> runs = new ArrayList<>();
		java.util.Set<String> seen = new java.util.HashSet<>();
		for (Row r : rows) if (seen.add(r.runId)) runs.add(new Dataset.RunInfo(r.runId, r.finished, r.ruleset, true, r.duplicate));
		return new Dataset(d, rows, runs);
	}

	static Query allScopes( Query q ){
		return q.withScope(new Scope(Scope.Rules.ALL, "", true, true, true, true));
	}

	static List<String> names( QueryResult r ){
		List<String> out = new ArrayList<>();
		for (Row row : r.rows) out.add(row.text(NAME));
		return out;
	}
}
