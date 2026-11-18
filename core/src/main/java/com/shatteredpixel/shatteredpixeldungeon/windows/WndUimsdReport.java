package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Dataset;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Datasets;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.GroupRow;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.SeedFloors;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.report.Report;
import com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportBuilder;
import com.shatteredpixel.shatteredpixeldungeon.apd.report.ReportExport;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//APD: where a player turns what UIMSD shows into a report to share or keep. One window offers how (share as text, or save as Markdown, a
//web page or CSV); the report itself is built from the same data as the screens (apd.report) when a choice is made. Wording is placeholder.
public final class WndUimsdReport {

	private WndUimsdReport(){}

	public interface Source {
		Report build();
	}

	private static File base(){
		File root = ApdRecorder.historyRoot();
		return root == null || root.getParentFile() == null ? new File(".") : root.getParentFile();
	}

	private static void tell( final String message ){
		Game.runOnRenderThread(new Callback(){
			@Override
			public void call(){
				ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("UIMSD", message, false));
			}
		});
	}

	public static void show( final UimsdModel model, String what, final Source source ){
		final ReportExport.Format[] formats = ReportExport.Format.values();
		String[] options = new String[formats.length + 1];
		options[0] = "Share as text";
		for (int i = 0; i < formats.length; i++) options[i + 1] = "Save: " + formats[i].label;
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdChoices("Report", what + "\n\nReports keep the marks (? unknown, >= at least, ~ worked out) and say which data is canonical.", options){
			@Override
			protected void onSelect( int index ){
				Report r;
				try {
					r = source.build();
					//made-up data must never pass for real history once the report is shared
					r.synthetic = model != null && model.catalog() != null && model.catalog().synthetic;
				} catch (Throwable e){
					tell("The report could not be made: " + e);
					return;
				}
				ReportExport.Done done = new ReportExport.Done(){
					@Override
					public void finished( String message ){
						tell(message);
					}
				};
				if (index == 0) ReportExport.share(base(), r, done);
				else ReportExport.save(base(), r, formats[index - 1], done);
			}
		}.restoreWith(() -> show(model, what, source)));
	}

	//---- the reports a screen can offer ----

	public static void ofView( final UimsdModel m ){
		final QueryResult q = m.result();
		if (q == null) return;
		final String title = m.view() == null ? q.def.title : m.view().title;
		show(m, "The table you are looking at, with its filters, sorting, columns and the choice of which runs count.", new Source(){
			@Override
			public Report build(){
				return ReportBuilder.view(q, title, m.currentRulesetLabel(), System.currentTimeMillis(), Game.version);
			}
		});
	}

	public static void ofLifetime( final UimsdModel m, final boolean allRules ){
		final Catalog cat = m.catalog();
		if (cat == null) return;
		show(m, "A summary of your runs: wins, deaths, characters, gear, enemies and records.", new Source(){
			@Override
			public Report build(){
				return ReportBuilder.lifetime(cat, m.currentRuleset(), m.currentRulesetLabel(), allRules, System.currentTimeMillis(), Game.version);
			}
		});
	}

	public static void ofGroup( final UimsdModel m, final GroupRow g ){
		final QueryResult q = m.result();
		if (q == null) return;
		show(m, "This group and its statistics.", new Source(){
			@Override
			public Report build(){
				return ReportBuilder.group(q, g, m.currentRulesetLabel(), System.currentTimeMillis(), Game.version);
			}
		});
	}

	//a list of the other groups of this table, to compare this one with
	public static void compareGroupWith( final UimsdModel m, final GroupRow g ){
		final QueryResult q = m.result();
		if (q == null || !q.isGrouped()) return;
		final List<GroupRow> others = new ArrayList<>();
		for (GroupRow o : q.groups) if (o != g) others.add(o);
		if (others.isEmpty()){
			tell("This table has no other group to compare with.");
			return;
		}
		chooseComparison(others, o -> ReportBuilder.groupName(q, o) + "  (" + o.n + ")", o -> {
			show(m, "These two groups side by side.", new Source(){
				@Override public Report build(){
					return ReportBuilder.compareGroups(q, g, o, m.currentRulesetLabel(), System.currentTimeMillis(), Game.version);
				}
			});
		}, 0);
	}

	public static void ofRun( final UimsdModel m, final Row run ){
		final Catalog cat = m.catalog();
		if (cat == null) return;
		final boolean hidden = m.query().showInternal;
		show(m, "This run: its details, items, enemies and how it ended." + (hidden ? "\nHidden information is included (spoilers)." : ""), new Source(){
			@Override
			public Report build(){
				return ReportBuilder.run(cat, run, hidden, m.currentRuleset(), System.currentTimeMillis(), Game.version);
			}
		});
	}

	public static void ofSeedFloors( final UimsdModel m, final String runId ){
		final Catalog cat = m.catalog();
		if (cat == null) return;
		show(m, "Every floor of the seed: recorded floors from the run's log, other floors canonical (rebuilt from the seed, not history), and how they compare.", new Source(){
			@Override
			public Report build(){
				return ReportBuilder.seedFloors(cat, runId, m.currentRuleset(), System.currentTimeMillis(), Game.version);
			}
		});
	}

	//a list of the other runs to choose one to compare with
	public static void compareWith( final UimsdModel m, final Row run ){
		final Catalog cat = m.catalog();
		Dataset runs = cat == null ? null : cat.dataset(Datasets.RUNS);
		if (runs == null) return;
		final List<Row> others = new ArrayList<>();
		final ColumnDef start = runs.def.column(com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.START_MILLIS);
		for (Row r : runs.rows) if (r != run && r.finished && !r.duplicate) others.add(r);
		if (start != null){
			Collections.sort(others, new Comparator<Row>(){
				@Override
				public int compare( Row a, Row b ){
					long x = a.isKnown(start.index()) ? a.num(start.index()) : 0, y = b.isKnown(start.index()) ? b.num(start.index()) : 0;
					return x < y ? 1 : x > y ? -1 : 0;
				}
			});
		}
		if (others.isEmpty()){
			tell("There is no other finished run to compare with.");
			return;
		}
		chooseComparison(others, r -> ReportBuilder.describeRun(cat, r, false), other -> {
			final boolean hidden = m.query().showInternal;
			show(m, "This run next to the one you chose.", new Source(){
				@Override public Report build(){
					return ReportBuilder.compareRuns(cat, run, other, hidden, System.currentTimeMillis(), Game.version);
				}
			});
		}, 0);
	}

	//Render only one small native list at a time, while keeping every comparison candidate reachable.
	private static <T> void chooseComparison(final List<T> choices,
			final java.util.function.Function<T, String> label,
			final java.util.function.Consumer<T> selected, int requestedPage){
		final com.shatteredpixel.shatteredpixeldungeon.apd.query.ChoicePage page =
				new com.shatteredpixel.shatteredpixeldungeon.apd.query.ChoicePage(choices.size(), requestedPage);
		List<String> labels = new ArrayList<>();
		for (int i = page.start; i < page.end; i++) labels.add(label.apply(choices.get(i)));
		final int count = labels.size();
		if (page.hasPrevious()) labels.add("Previous page");
		if (page.hasNext()) labels.add("Next page");
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdList("Compare with",
				"Choose a comparison. Page " + (page.page + 1) + " of " + page.pageCount + ".",
				labels, null, (index, checked) -> {
					if (index < count) selected.accept(choices.get(page.start + index));
					else if (page.hasPrevious() && index == count)
						chooseComparison(choices, label, selected, page.page - 1);
					else chooseComparison(choices, label, selected, page.page + 1);
				}).restoreWith(() -> chooseComparison(choices, label, selected, page.page)));
	}

	//the table of runs (the only one with run-level reports); kept here so the generic windows never name a shipped dataset
	public static boolean isRunsTable( com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetDef def ){
		return Datasets.RUNS.equals(def.id);
	}

	public static boolean isSeedView( UimsdModel m ){
		String id = m.viewId();
		return SeedFloors.ITEMS.equals(id) || SeedFloors.MOBS.equals(id) || SeedFloors.COMPARE.equals(id);
	}

	//the run the open seed floors belong to ("" when there are no rows)
	public static String seedRunId( UimsdModel m ){
		Catalog cat = m.catalog();
		Dataset d = cat == null ? null : cat.dataset(SeedFloors.ITEMS);
		return d == null || d.rows.isEmpty() ? "" : d.rows.get(0).runId;
	}
}

