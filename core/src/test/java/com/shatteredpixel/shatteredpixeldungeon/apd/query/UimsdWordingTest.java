package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.apd.report.Report;
import com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportBuilder;
import com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportRenderer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//A wording lint over everything UIMSD says to the player that is data rather than layout: guide, tour, views, columns, Home, notes and reports.
//It catches internal vocabulary leaking out (state names, ids, "null", leftover "placeholder"/TODO), doubled spaces, stray whitespace, and texts that
//are empty. It does not judge style: the final wording stays a human decision.
public class UimsdWordingTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String[] BANNED = { "PROV_", "INFERRED", "UNKNOWN", "CellText", "mergeKey", "null", "TODO", "FIXME", "placeholder", "Lorem",
			"undefined", "NaN", "Exception", "java.", "com.shatteredpixel" };

	//words of the database or of the code that a player should never have to learn: the Guide's glossary explains the real terms in plain words
	private static final String[] JARGON = { "ruleset", "instance", "dataset", "telemetry", "provenance", "bit mask", "enum", "query", "schema" };

	private static void jargon( List<String> problems, String where, String text ){
		if (text == null) return;
		String lower = text.toLowerCase();
		for (String j : JARGON) if (lower.contains(j)) problems.add(where + ": jargon '" + j + "' in: " + text);
	}

	private static void check( List<String> problems, String where, String text ){
		jargon(problems, where, text);
		if (text == null || text.trim().isEmpty()){
			problems.add(where + ": empty");
			return;
		}
		for (String b : BANNED) if (text.contains(b)) problems.add(where + ": contains '" + b + "' in: " + text);
		if (text.contains("  ") && !text.contains("\n") && !text.contains("   ")) problems.add(where + ": double space in: " + text);
		if (!text.equals(text.trim())) problems.add(where + ": leading/trailing whitespace: '" + text + "'");
		if (text.contains(" ,") || text.contains(" .") || text.contains("..") && !text.contains("...")) problems.add(where + ": odd punctuation in: " + text);
	}

	@Test
	public void noInternalVocabularyOrSloppyTextReachesThePlayer() throws Exception {
		HeadlessTestSupport.start();
		Ruleset rules = Ruleset.of(30, 0);
		File root = DemoData.sampleRootFor(new File(tmp.newFolder("data"), "apd"));
		DemoGenerator.generateSample(root, rules, 1_800_000_000_000L);
		Catalog cat = new DatasetStore(root).load();
		List<String> problems = new ArrayList<>();

		for (UimsdGuide.Topic t : UimsdGuide.topics()){
			check(problems, "guide " + t.id + " title", t.title);
			check(problems, "guide " + t.id, t.text);
		}
		for (UimsdTutorial.Step s : UimsdTutorial.steps()){
			check(problems, "tour " + s.id + " title", s.title);
			check(problems, "tour " + s.id, s.text);
		}
		for (ViewDef v : cat.views()){
			check(problems, "view " + v.id + " title", v.title);
			check(problems, "view " + v.id + " description", v.description);
		}
		for (Dataset d : cat.datasets()){
			check(problems, "dataset " + d.def.id + " title", d.def.title);
			check(problems, "dataset " + d.def.id + " description", d.def.description);
			for (ColumnDef c : d.def.columns()){
				check(problems, d.def.id + "." + c.id + " label", c.label);
				if (!c.group.isEmpty()) check(problems, d.def.id + "." + c.id + " group", c.group);
				if (!c.description.isEmpty()) check(problems, d.def.id + "." + c.id + " description", c.description);
				for (String w : c.labels.values()) check(problems, d.def.id + "." + c.id + " value label", w);
			}
		}
		for (Op o : Op.values()){
			check(problems, "operator " + o.name() + " word", o.word);
			check(problems, "operator " + o.name() + " hint", o.hint);
		}
		for (ViewDef v : cat.views()) check(problems, "view " + v.id + " section", v.group.isEmpty() ? "Other" : v.group);
		UimsdHome home = UimsdHome.build(cat, rules.id, true);
		check(problems, "home note", home.scopeNote);
		for (UimsdHome.Card c : home.cards){
			check(problems, "home card", c.title);
			for (UimsdHome.Line l : c.lines){
				check(problems, "home " + c.id + " label", l.label);
				if (!l.heading) check(problems, "home " + c.id + " value", l.value);
			}
			for (UimsdHome.Link l : c.links) check(problems, "home link", l.label);
		}
		ViewDef runs = cat.view("runs");
		QueryResult res = QueryEngine.run(cat.datasetOf(runs), runs.initialQuery(), rules.id);
		for (StatusText.Line l : StatusText.lines(res, "Runs", "")) check(problems, "status", l.text);
		//and the broadest table, which carries every kind of note
		QueryResult broad = QueryEngine.run(cat.datasetOf(runs), runs.initialQuery().withScope(Scope.DEFAULT.withRules(Scope.Rules.ALL, "").withUnfinished(true)
				.withUnknownRuleset(false)), rules.id);
		for (StatusText.Line l : StatusText.lines(broad, "Runs", "challenges 0, exotic 30%, difficulty 3")) check(problems, "status (all rules)", l.text);
		for (Report r : new Report[]{ ReportBuilder.lifetime(cat, rules.id, "", true, 0L, "test"), ReportBuilder.view(res, "Runs", "", 0L, "test") }){
			String md = ReportRenderer.markdown(r);
			check(problems, "report " + r.title, md.replace("\n", " ").replace("|", " ").replaceAll("\\s+", " ").trim());
		}
		assertTrue(problems.size() + " wording problems:\n" + String.join("\n", problems), problems.isEmpty());
	}
}
