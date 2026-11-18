/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//Everything a UIMSD screen is, apart from the pixels: which view is open, the query being edited, the result, the page, and the
//moves between them (open a view, add a filter, click a header, follow a link, go back). It knows nothing about any particular
//dataset or any screen, so the same model drives every dataset and can be tested without a game. Not thread safe: use it from
//one thread (the screen's), except load(), which builds the catalog on its own thread and hands it over through the listener.
public final class UimsdModel {

    // Cancellation retires publication immediately, but a decoder may still own its
    // current batch. Serialize background catalog work so a replacement cannot
    // allocate a second working history until that batch has unwound.
    private static final Object CATALOG_LOAD_MEMORY = new Object();

	public interface Listener {
		//the state changed (called on the loading thread for the result of load(): a screen must hand over to its own thread)
		void changed();
	}

	public enum State { EMPTY, LOADING, READY, FAILED }

	public static final int PAGE_SIZE = 50;

	private static final class Place {
		final String viewId;
		final Query query;
		final int page;
		final long id;
		TableViewport viewport;
		QueryResult result;
		Dataset dataset;

		Place( String viewId, Query query, int page, long id, TableViewport viewport ){
			this.viewId = viewId;
			this.query = query;
			this.page = page;
			this.id = id;
			this.viewport = viewport;
		}
	}

	private volatile State state = State.EMPTY;
	private volatile String failure = "";
	private volatile Catalog catalog;
	private String viewId = "";
	private Query query = Query.of("");
	private QueryResult result;
	private int page;
	private final String currentRuleset;
	private final String currentRulesetLabel;
	private final List<Place> back = new ArrayList<>();
	private long nextPlaceId, placeId;
	private TableViewport viewport;

	// A scene can checkpoint the outgoing table after navigation has already
	// happened (including a worker's seed-replay completion). The unique place
	// token routes that checkpoint to its own Back entry, never the new table.
	public synchronized long placeId(){ return placeId; }
	public synchronized TableViewport viewport(){ return viewport; }
	public synchronized void rememberViewport(long id, TableViewport position){
		if (position == null) return;
		if (id == placeId){ viewport = position; return; }
		for (Place p : back) if (p.id == id){ p.viewport = position; return; }
	}

	private void newPlace(){ placeId = ++nextPlaceId; viewport = null; }
	private void pushPlace(){
		Place saved = new Place(viewId, query, page, placeId, viewport);
		saved.result = result;
		saved.dataset = dataset();
		back.add(saved);
		// Keep query results for only the four nearest Back destinations.
		// Older navigation entries remain usable through normal recomputation.
		if (back.size() > 4){
			Place older = back.get(back.size() - 5);
			older.result = null;
			older.dataset = null;
		}
		newPlace();
	}
	//Column choices are preferences for a view, separate from named saved queries.
	public static final String COLUMN_LAYOUTS_FILE = "column_layouts.txt";
	private SavedViews columnLayouts = new SavedViews();
	// Prepared alongside the history catalog off the render thread, so opening
	// Home doesn't run all its statistical queries in a blocking UI frame.
	private UimsdHome preparedHome;
	private Catalog preparedHomeCatalog;
	private boolean preparedHomeAllRules;
	private Listener listener;
	private volatile Thread loader;
	//Each new load (or an explicitly installed catalogue) retires older background completions.
	private long loadGeneration;

	private volatile File root;

	public UimsdModel( String currentRuleset, String currentRulesetLabel ){
		this.currentRuleset = currentRuleset;
		this.currentRulesetLabel = currentRulesetLabel == null ? "" : currentRulesetLabel;
	}

	public void setListener( Listener l ){
		this.listener = l;
	}

	private void fire(){
		Listener l = listener;
		if (l != null) l.changed();
	}

	//---- loading ----

	//Builds the catalog from the raw logs on a background thread. Never throws.
	public synchronized void load( final File root ){
		load(root, false);
	}

	// The viewer supplies its current Home scope; the common API retains the
	// default current-rules-only behavior for existing integrations and tests.
	public synchronized void load( final File root, final boolean homeAllRules ){
		load(root, homeAllRules, null, false);
	}

    private static String loadFailureMessage(Throwable error){
        return error instanceof OutOfMemoryError
                ? "Not enough memory to open statistics. Your saved games and recorded history have not been deleted. Close statistics and try again."
                : String.valueOf(error);
    }

	public interface OpenClaimListener {
		void claimed(UimsdModel model);
	}

	// Claiming the title preload is deliberately separate from instance loading:
	// acceptance requires the viewer to own the exact validated mutable model,
	// not a second model that merely shares its catalog.
	public static Thread claimForOpen(final File root, final String ruleset, final String label,
			final boolean homeAllRules, final Runnable beforeRead, final OpenClaimListener listener){
		Thread claim = new Thread(new Runnable(){
			@Override
			public void run(){
				UimsdModel opened;
				try {
					if (beforeRead != null) beforeRead.run();
					opened = UimsdWarmCache.takeForOpen(root, ruleset, label, homeAllRules);
					if (opened == null){
						Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
						opened = new UimsdModel(ruleset, label);
						opened.load(root, homeAllRules);
					}
				} catch (Throwable error){
					opened = new UimsdModel(ruleset, label);
					synchronized (opened){
						opened.root = root;
						opened.failure = loadFailureMessage(error);
						opened.state = State.FAILED;
					}
				}
				listener.claimed(opened);
			}
		}, "uimsd-open-claim");
		claim.setDaemon(true);
		claim.setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 2));
		claim.start();
		return claim;
	}

	// Retained for non-scene integrations; the title path uses claimForOpen so
	// it can preserve the prepared model's identity as well as its catalog.
	public synchronized void loadForOpen(File root, boolean homeAllRules, Runnable beforeRead){
		load(root, homeAllRules, beforeRead, true);
	}

	private synchronized void load(final File root, final boolean homeAllRules,
	                              final Runnable beforeRead, final boolean useWarm){
		retireSeedFloorsLocked();
        // Replacement loads must not retain the previous complete history beside the new one.
        catalog=null;result=null;
        for(Place place:back){place.result=null;place.dataset=null;}
		preparedHome = null;
		preparedHomeCatalog = null;
		this.root = root;
		// Do not read or decode saved column layouts on the render thread.
		// This method is called directly before the UIMSD scene transition.
		// The worker publishes its complete layout + catalog as one generation.
		columnLayouts = new SavedViews();
		state = State.LOADING;
		failure = "";
		final long generation = ++loadGeneration;
		Thread t = new Thread(new Runnable(){
			@Override
			public void run(){
				try {
					if (beforeRead != null) beforeRead.run();
					synchronized (UimsdModel.this){
						if (generation != loadGeneration) return;
					}
					UimsdModel warm = useWarm ? UimsdWarmCache.takeForOpen(
							root, currentRuleset, currentRulesetLabel, homeAllRules) : null;
					// A cold rebuild can be substantial; only the short, interactive
					// flush/freshness/cache-claim phase receives elevated priority.
					if (useWarm && warm == null)
						Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
					SavedViews loadedColumnLayouts = warm == null
							? SavedViews.load(root == null ? null : new File(root, COLUMN_LAYOUTS_FILE)) : warm.columnLayouts;
                    synchronized(CATALOG_LOAD_MEMORY){
                    if(loadRetired(generation))return;
					Catalog c = warm == null ? new DatasetStore(root).load(() -> loadRetired(generation)) : warm.catalog;
					// These are read-only queries. Computing the initial Home here
					// keeps them off the frame that displays the finished catalog.
					UimsdHome initialHome = warm == null ? UimsdHome.build(c, currentRuleset, homeAllRules)
							: warm.preparedHome(c, homeAllRules);
					synchronized (UimsdModel.this){
						if (generation != loadGeneration) return;
						retireSeedFloorsLocked();
						columnLayouts = loadedColumnLayouts;
						preparedHome = initialHome;
						preparedHomeCatalog = c;
						preparedHomeAllRules = homeAllRules;
						setCatalogLocked(c);
					}
                    }
				} catch (Throwable e){
					synchronized (UimsdModel.this){
						if (generation != loadGeneration) return;
						failure = loadFailureMessage(e);
						state = State.FAILED;
					}
				}
				fire();
			}
		}, "uimsd-load");
		t.setDaemon(true);
		if (useWarm) t.setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 2));
		loader = t;
		t.start();
	}

    /** Drop derived tables while gameplay needs the heap; keep query/navigation preferences. */
    public synchronized void releaseForGameplay(){
        ++loadGeneration;
        retireSeedFloorsLocked();
        Thread old=loader;
        loader=null;
        if(old!=null)old.interrupt();
        if(seedFloorsLoader!=null)seedFloorsLoader.interrupt();
        catalog=null;result=null;preparedHome=null;preparedHomeCatalog=null;listener=null;
        for(Place place:back){place.result=null;place.dataset=null;}
        state=State.EMPTY;
    }
    private synchronized boolean loadRetired(long generation){return generation!=loadGeneration;}

	public void awaitLoad( long millis ){
		Thread t = loader;
		try {
			if (t != null) t.join(millis);
		} catch (InterruptedException ignored){
		}
	}

	public synchronized void setCatalog( Catalog c ){
		retireSeedFloorsLocked();
		preparedHome = null;
		preparedHomeCatalog = null;
		++loadGeneration;
		setCatalogLocked(c);
		fire();
	}

	private void setCatalogLocked( Catalog c ){
		catalog = c;
		ViewDef first = c.view(viewId) != null ? c.view(viewId) : c.views().isEmpty() ? null : c.views().get(0);
		if (first == null){
			state = State.FAILED;
			failure = "There is nothing to show.";
			return;
		}
		viewId = first.id;
		query = initialQuery(first, false);
		page = 0;
		back.clear();
		newPlace();
		recompute();
		state = State.READY;
	}

	// Only a matching, fully loaded catalog and Home scope may reuse the
	// worker's summary; a replaced catalog or toggled scope must recompute.
	public synchronized UimsdHome preparedHome(Catalog expected, boolean allRules){
		return expected != null && expected == catalog
				&& expected == preparedHomeCatalog && allRules == preparedHomeAllRules
				? preparedHome : null;
	}

	//the folder of the history this model shows (where the player's saved views are kept too); null before load
	public File root(){
		return root;
	}

	public State state(){
		return state;
	}

	public String failure(){
		return failure;
	}

	public Catalog catalog(){
		return catalog;
	}

	//---- reading ----

	public synchronized Query query(){
		return query;
	}

	public synchronized QueryResult result(){
		return result;
	}

	public synchronized String viewId(){
		return viewId;
	}

	public synchronized ViewDef view(){
		return catalog == null ? null : catalog.view(viewId);
	}

	public synchronized Dataset dataset(){
		return catalog == null ? null : catalog.dataset(query.dataset);
	}

	public synchronized int page(){
		return page;
	}

	//the current rules in words (for notes and reports)
	public String currentRulesetLabel(){
		return currentRulesetLabel == null ? "" : currentRulesetLabel;
	}

	public String currentRuleset(){
		return currentRuleset;
	}

	public synchronized ResultTable table(){
		return result == null ? null : ResultTable.build(result, page, PAGE_SIZE);
	}

	//Measures and chart data for the current grouped result. Drawing stays in the phone UI.
	public synchronized List<UimsdChart.Measure> chartMeasures(){
		return result == null ? new ArrayList<UimsdChart.Measure>() : UimsdChart.measures(result);
	}

	public synchronized UimsdChart.Series chart( String measureId, int maxPoints ){
		return result == null ? null : UimsdChart.build(result, measureId, maxPoints);
	}

	public synchronized List<StatusText.Line> status(){
		ViewDef v = view();
		return result == null ? new ArrayList<StatusText.Line>() : StatusText.lines(result, v == null ? "" : v.title, currentRulesetLabel);
	}

	//how many places Back would pass through (0 at the top): the tutorial watches it to see a drill-down or a Back happen
	public synchronized int depth(){
		return back.size();
	}

	public synchronized boolean canGoBack(){
		return !back.isEmpty();
	}

	//the columns that may be offered to the player right now (hidden ones only when hidden information is on)
	public synchronized List<ColumnDef> offeredColumns(){
		List<ColumnDef> out = new ArrayList<>();
		Dataset ds = dataset();
		if (ds == null) return out;
		for (ColumnDef c : ds.def.columns()) if (!c.isInternal() || query.showInternal) out.add(c);
		return out;
	}

	//a value a TEXT column holds in the entries the player may look at: the raw value (what a filter compares), how it reads in the table, how often it occurs
	public static final class Choice {
		public final String value;
		public final String label;
		public final int count;

		Choice( String value, String label, int count ){
			this.value = value;
			this.label = label;
			this.count = count;
		}
	}

	//The values a TEXT column actually holds, most frequent first (at most 'limit'), so a filter can offer them as choices instead of asking for typed text.
	//Only values the player may see: a hidden column of a run that is not finished contributes nothing.
	public synchronized List<Choice> choicesFor( ColumnDef c, int limit ){
		List<Choice> out = new ArrayList<>();
		Dataset ds = dataset();
		if (ds == null || c == null || c.type != ColumnDef.Type.TEXT) return out;
		java.util.Map<String, int[]> counts = new java.util.LinkedHashMap<>();
		java.util.Map<String, String> labels = new java.util.HashMap<>();
		for (Row r : ds.rows){
			boolean legit = r.publicRow || (query.showInternal && r.finished);
			if (!legit || !QueryEngine.open(query, r, c) || !r.isKnown(c.index())) continue;
			String raw = r.text(c.index());
			int[] n = counts.get(raw);
			if (n == null){
				counts.put(raw, n = new int[1]);
				labels.put(raw, raw.isEmpty() ? "(empty)" : CellFormatter.cell(query, r, c).text);
			}
			n[0]++;
		}
		for (java.util.Map.Entry<String, int[]> e : counts.entrySet()) out.add(new Choice(e.getKey(), labels.get(e.getKey()), e.getValue()[0]));
		java.util.Collections.sort(out, new java.util.Comparator<Choice>(){
			@Override
			public int compare( Choice a, Choice b ){
				if (a.count != b.count) return a.count > b.count ? -1 : 1;
				return a.label.compareTo(b.label);
			}
		});
		return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
	}

	//---- changing ----

	private void recompute(){
		Dataset ds = catalog == null ? null : catalog.dataset(query.dataset);
		result = ds == null ? null : QueryEngine.run(ds, query, currentRuleset);
		clampPage();
	}

	private void clampPage(){
		int total = result == null ? 0 : result.isGrouped() ? result.groups.size() : result.rows.size();
		int maxPage = Math.max(0, (total + PAGE_SIZE - 1) / PAGE_SIZE - 1);
		if (page > maxPage) page = maxPage;
	}

	private void apply( Query q ){
		query = q;
		page = 0;
		recompute();
		fire();
		restoreHiddenAfterSeed();
	}

	//Seed floors turns hidden information on because that view needs it; the player's own choice comes back when they leave the seed views
	//(by Back, by opening another view, or by following a link out of them).
	private Boolean hiddenBeforeSeed;

	private void restoreHiddenAfterSeed(){
		if (hiddenBeforeSeed == null || SeedFloors.isSeedView(viewId)) return;
		boolean previous = hiddenBeforeSeed;
		hiddenBeforeSeed = null;
		if (query.showInternal != previous) setShowInternal(previous);
	}

	//open a view. The player's wish for hidden information carries over; everything else starts from the view.
	//---- seed floors (canonical replay of a finished run's seed) ----

	private String notice = "";

	//a short line for the screen about work in progress or why something is unavailable ("" when there is none)
	public synchronized String notice(){
		return notice;
	}

	public synchronized void clearNotice(){
		if (!notice.isEmpty()){
			notice = "";
			fire();
		}
	}

	private SeedFloors.Provider seedFloors;
	private long seedFloorsGeneration;
	private volatile Thread seedFloorsLoader;

	private void retireSeedFloorsLocked(){
		++seedFloorsGeneration;
		if (notice.startsWith("Replaying the seed")) notice = "";
	}

	//the game-aware layer that can replay a run's seed (null: the feature is off, as in plain library tests)
	public synchronized void setSeedFloorsProvider( SeedFloors.Provider p ){
		retireSeedFloorsLocked();
		seedFloors = p;
	}

	//Used by retained tests and orderly callers that must wait for an in-flight replay handoff.
	void awaitSeedFloors( long millis ){
		Thread t = seedFloorsLoader;
		try {
			if (t != null) t.join(millis);
		} catch (InterruptedException ignored){
		}
	}

	//true for a row that seed floors can be asked for (a finished run)
	public synchronized boolean canSeedFloors( Row row ){
		Dataset ds = dataset();
		return seedFloors != null && seedFloors.applies(catalog, ds, row);
	}

	//Replays the run's seed on a worker thread and opens the seed floors. Never throws: a run that cannot be replayed (or a replay that is
	//refused because a game is live) becomes a notice.
	public void startSeedFloors( final Row run ){
		final Catalog cat;
		final SeedFloors.Provider provider;
		final long generation;
		synchronized (this){
			if (!canSeedFloors(run)) return;
			//the seed views are replaced by each run's: places that point at an earlier run's would show this run's data
			for (int i = back.size() - 1; i >= 0; i--) if (SeedFloors.isSeedView(back.get(i).viewId)) back.remove(i);
			cat = catalog;
			provider = seedFloors;
			generation = ++seedFloorsGeneration;
			notice = "Replaying the seed (this takes a few seconds)...";
		}
		fire();
		Thread t = new Thread(new Runnable(){
			@Override
			public void run(){
				String result = "";
				try {
					SeedFloors.Built b = provider.build(cat, run);
					synchronized (UimsdModel.this){
						if (generation != seedFloorsGeneration) return;
						SeedFloors.install(cat, b);
						if (!SeedFloors.isSeedView(viewId)) hiddenBeforeSeed = query.showInternal;
						pushPlace();
						// Publish the completed state together; observers must not see the new view with the old busy notice.
						notice = "";
						viewId = SeedFloors.ITEMS;
						apply(cat.view(SeedFloors.ITEMS).initialQuery().withShowInternal(true));
					}
				} catch (SeedFloors.NotAvailable e){
					result = e.getMessage();
				} catch (Throwable e){
					result = "Seed floors failed: " + e;
				}
				synchronized (UimsdModel.this){
					if (generation != seedFloorsGeneration) return;
					notice = result;
				}
				fire();
			}
		}, "uimsd-seed-floors");
		t.setDaemon(true);
		seedFloorsLoader = t;
		t.start();
	}

	public synchronized void selectView( String id ){
		if (catalog == null || catalog.view(id) == null) return;
		pushPlace();
		viewId = id;
		apply(initialQuery(catalog.view(id), query.showInternal));
	}

	//open a view as a new starting point, with nothing to go Back to (used from Home, so Back from there leads to Home)
	public synchronized void showRoot( String id ){
		if (catalog == null || catalog.view(id) == null) return;
		back.clear();
		newPlace();
		viewId = id;
		apply(initialQuery(catalog.view(id), query.showInternal));
	}

	//Opens a question's query as an ordinary table, with the full query tools. The table is the one for the question's dataset; the question's
	//conditions, grouping and sorting are applied on top. Back from there leads to where the player was.
	public synchronized void showQuery( Query q ){
		if (catalog == null || catalog.view(q.dataset) == null) return;
		back.clear();
		newPlace();
		viewId = q.dataset;
		apply(q);
	}

    //Research evidence follows an explicit query and retains Back to the inspected table.
    public synchronized boolean openResearchQuery(Query q){
        if(catalog==null || q==null || catalog.view(q.dataset)==null || catalog.dataset(q.dataset)==null)return false;
        QueryResult check=QueryEngine.run(catalog.dataset(q.dataset),q,currentRuleset);
        if(!check.problems.isEmpty()){notice="This evidence query no longer matches the available schema.";fire();return false;}
        pushPlace();viewId=q.dataset;query=q;page=0;result=check;fire();restoreHiddenAfterSeed();return true;
    }

	//back to how the open view started
	public synchronized void resetView(){
		ViewDef v = view();
		if (v != null){
			apply(v.initialQuery().withShowInternal(query.showInternal));
			rememberColumns(query.columns);
		}
	}

	private Query initialQuery(ViewDef v, boolean showInternal){
		Query initial = v.initialQuery().withShowInternal(showInternal);
		SavedViews.View saved = columnLayouts.get(v.id);
		Query layout = saved == null ? null : saved.query();
		Dataset ds = catalog == null ? null : catalog.dataset(initial.dataset);
		if (layout == null || ds == null) return initial;
		List<String> usable = new ArrayList<>();
		for (String id : layout.columns){
			ColumnDef c = ds.def.column(id);
			if (c != null && (!c.isInternal() || showInternal) && !usable.contains(id)) usable.add(id);
		}
		//A layout can outlive columns removed by a newer build, or contain only hidden columns from a session
		//where hidden information was enabled. Never let that turn an otherwise healthy view into a blank table.
		return usable.isEmpty() ? initial : initial.withColumns(usable);
	}

	private void rememberColumns(List<String> columns){
		columnLayouts.save(viewId, Query.of(query.dataset).withColumns(columns), System.currentTimeMillis());
		if (root == null) return;
		try {
			columnLayouts.store(new File(root, COLUMN_LAYOUTS_FILE));
		} catch (java.io.IOException failure){
			notice = "Column layout changed for this session, but could not be saved.";
			fire();
		}
	}

	public synchronized void addFilter( Filter f ){
		apply(query.addFilter(f));
	}

	public synchronized void replaceFilter( int index, Filter f ){
		if (index >= 0 && index < query.filters.size()) apply(query.replaceFilter(index, f));
	}

	public synchronized void removeFilter( int index ){
		if (index >= 0 && index < query.filters.size()) apply(query.removeFilter(index));
	}

	public synchronized void clearFilters(){
		apply(query.withFilters(new ArrayList<Filter>()));
	}

	public synchronized void cycleSort( String column ){
		apply(query.cycleSort(column));
	}

	public synchronized void setSorts( List<Sort> sorts ){
		apply(query.withSorts(sorts));
	}

	public synchronized void setColumns( List<String> columns ){
		apply(query.withColumns(columns));
		rememberColumns(columns);
	}

	public synchronized void toggleColumn( String id ){
		Dataset ds = dataset();
		if (ds == null) return;
		List<String> cols = new ArrayList<>(query.columns.isEmpty() ? ds.def.defaultColumnIds() : query.columns);
		if (cols.contains(id)){
			if (cols.size() > 1) cols.remove(id);
		} else {
			cols.add(id);
		}
		setColumns(cols);
	}

	//moves a shown column left or right; the first column is the one that stays fixed on screen
	public synchronized void moveColumn( String id, int delta ){
		Dataset ds = dataset();
		if (ds == null) return;
		List<String> cols = new ArrayList<>(query.columns.isEmpty() ? ds.def.defaultColumnIds() : query.columns);
		List<String> moved = ColumnOrder.move(cols, id, delta);
		if (!moved.equals(cols)) setColumns(moved);
	}

	public synchronized void setScope( Scope s ){
		if (query.scope.finishedOnly) s = s.withFinishedOnly(true);
		apply(query.withScope(s));
	}

	public synchronized void setMinSample( int n ){
		apply(query.withMinSample(n));
	}

	public synchronized void setGroup( GroupBy g ){
		List<Sort> sorts = new ArrayList<>();
		apply(query.withGroup(g).withSorts(sorts));
	}

	//Hidden information on or off. Turning it off also takes out everything in the query that uses it.
	public synchronized void setShowInternal( boolean on ){
		Query q = query.withShowInternal(on);
		if (!on){
			Dataset ds = dataset();
			if (ds != null){
				List<Filter> filters = new ArrayList<>();
				for (Filter f : q.filters){
					ColumnDef c = ds.def.column(f.column);
					if (c == null || !c.isInternal()) filters.add(f);
				}
				List<Sort> sorts = new ArrayList<>();
				for (Sort s : q.sorts){
					ColumnDef c = ds.def.column(s.column);
					if (c == null || !c.isInternal()) sorts.add(s);
				}
				List<String> cols = new ArrayList<>();
				for (String id : q.columns){
					ColumnDef c = ds.def.column(id);
					if (c == null || !c.isInternal()) cols.add(id);
				}
				GroupBy g = q.group;
				if (g != null){
					List<String> keys = new ArrayList<>();
					for (String k : g.keys){
						ColumnDef c = ds.def.column(k);
						if (c == null || !c.isInternal()) keys.add(k);
					}
					List<Aggregate> aggs = new ArrayList<>();
					for (Aggregate a : g.aggregates){
						ColumnDef value = a.column.isEmpty() ? null : ds.def.column(a.column);
						ColumnDef over = a.over.isEmpty() ? null : ds.def.column(a.over);
						ColumnDef where = a.whereColumn.isEmpty() ? null : ds.def.column(a.whereColumn);
						ColumnDef distinct = a.distinctBy.isEmpty() ? null : ds.def.column(a.distinctBy);
						if ((value == null || !value.isInternal()) && (over == null || !over.isInternal())
								&& (where == null || !where.isInternal()) && (distinct == null || !distinct.isInternal())){
							aggs.add(a);
						}
					}
					g = keys.isEmpty() && aggs.isEmpty() ? null : new GroupBy(keys, aggs);
				}
				q = q.withFilters(filters).withSorts(sorts).withColumns(cols).withGroup(g);
			}
		}
		apply(q);
	}

	public synchronized void setPage( int p ){
		page = Math.max(0, p);
		// Pagination changes only the viewport over the existing exact result.
		clampPage();
		fire();
	}

	//Follow a link from a row to the matching rows of another dataset. The scope is widened just enough for the row's own run to
	//be in it, so a drill-down never comes up empty because of a scope the player did not choose.
	//the scope widened just enough for this row's own run to be in it
	private Scope widen( Scope s, Row row ){
		if (!row.finished) s = s.withUnfinished(true);
		if (row.ruleset == null) s = s.withUnknownRuleset(true);
		else if (row.ruleset.equals(Row.RULESET_MIXED)) s = s.withMixedRuleset(true);
		else if (s.rules == Scope.Rules.CURRENT && !row.ruleset.equals(currentRuleset)) s = s.withRules(Scope.Rules.SPECIFIC, row.ruleset);
		else if (s.rules == Scope.Rules.SPECIFIC && !row.ruleset.equals(s.ruleset)) s = s.withRules(Scope.Rules.ALL, "");
		if (row.duplicate) s = s.withDuplicates(true);
		return s;
	}

	//Aggregate-first views: a grouped table (one row per kind of thing) whose groups can be opened. True when the table on screen
	//is a group table of a view that declares individual rows behind its groups.
	public synchronized boolean canOpenGroups(){
		ViewDef v = view();
		return v != null && v.instanceQuery() != null && query.group != null && result != null && result.isGrouped();
	}

	//Open the individual rows of one group: the same query without the grouping, with the group's key values as filters (so every
	//filter and scope the player set still applies, and the numbers match the group's). The instances can be sorted, filtered and
	//opened like any other rows, and each leads on to its run.
	public synchronized void openGroupRows( GroupRow g ){
		if (!canOpenGroups()) return;
		Query iq = view().instanceQuery();
		Query q = query.withGroup(null).withSorts(iq.sorts).withColumns(iq.columns).withMinSample(0);
		for (ColumnDef c : result.groupKeys){
			//the sample of a group stands for its rows only where the key is shown: the "hidden" bucket holds rows of runs that are not over, and
			//their real value must not become a filter the player can read
			if (!QueryEngine.open(query, g.sample, c)){
				notice = "That group is hidden information of runs that are not over, so its rows cannot be listed.";
				fire();
				return;
			}
			if (!g.sample.isKnown(c.index)) q = q.addFilter(Filter.unknown(c.id));
			else if (c.type == ColumnDef.Type.TEXT) q = q.addFilter(Filter.text(c.id, Op.GROUP_TEXT, g.sample.text(c.index)));
			else q = q.addFilter(Filter.num(c.id,
                    g.sample.prov(c.index) == com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact.PROV_INCOMPLETE
                            ? Op.GROUP_BOUND : Op.GROUP_VALUE, g.sample.num(c.index)));
		}
		pushPlace();
		apply(q);
	}

	//the link from the rows of the open dataset to the Runs, null when there is none
	public synchronized LinkDef runsLink(){
		Dataset ds = dataset();
		if (ds == null || catalog == null) return null;
		for (LinkDef l : ds.def.links) if (l.targetView.equals("runs") && catalog.view("runs") != null) return l;
		return null;
	}

	//how many different runs the rows of a group come from
	public synchronized int groupRunCount( GroupRow g ){
		LinkDef link = runsLink();
		if (link == null || result == null) return 0;
		Dataset from = dataset();
		int col = from.def.indexOf(link.ourColumn);
		if (col < 0) return 0;
		java.util.Set<String> ids = new java.util.HashSet<>();
		for (Row r : g.rows) if (QueryEngine.open(query, r, from.def.column(col)) && r.isKnown(col)) ids.add(r.text(col));
		return ids.size();
	}

	//Open the runs that contain the rows of one group (the distinct runs of its rows). The scope is widened just enough for every one
	//of those runs to be in it. Returns how many runs there are, 0 when nothing was opened.
	public synchronized int openGroupRuns( GroupRow g ){
		LinkDef link = runsLink();
		if (link == null || result == null) return 0;
		Dataset from = dataset();
		int col = from.def.indexOf(link.ourColumn);
		if (col < 0) return 0;
		ViewDef target = catalog.view(link.targetView);
		java.util.Set<String> ids = new java.util.LinkedHashSet<>();
		Scope s = query.scope;
		for (Row r : g.rows){
			if (!QueryEngine.open(query, r, from.def.column(col)) || !r.isKnown(col)) continue;
			if (ids.add(r.text(col))) s = widen(s, r);
		}
		if (ids.isEmpty()) return 0;
		pushPlace();
		viewId = target.id;
		apply(target.initialQuery().withScope(s).withShowInternal(query.showInternal)
				.addFilter(Filter.text(link.targetColumn, Op.IDENTIFIER_IN, ids.toArray(new String[0]))));
		return ids.size();
	}

	public synchronized void openLink( Row row, LinkDef link ){
		if (catalog == null || catalog.view(link.targetView) == null) return;
		Dataset from = dataset();
		if (from == null) return;
		int col = from.def.indexOf(link.ourColumn);
		if (col < 0 || !row.isKnown(col)) return;
		ViewDef target = catalog.view(link.targetView);
		Dataset to = catalog.datasetOf(target);
		ColumnDef tc = to.def.column(link.targetColumn);
		if (tc == null) return;
		Scope s = widen(query.scope, row);
		Filter f = tc.type == ColumnDef.Type.TEXT ? Filter.text(tc.id, Op.GROUP_TEXT, row.text(col)) : Filter.num(tc.id, Op.EQ, row.num(col));
		pushPlace();
		viewId = target.id;
		apply(target.initialQuery().withScope(s).withShowInternal(query.showInternal).addFilter(f));
	}

	public synchronized boolean back(){
		if (back.isEmpty()) return false;
		Place p = back.remove(back.size() - 1);
		viewId = p.viewId;
		query = p.query;
		page = p.page;
		placeId = p.id;
		viewport = p.viewport;
		if (p.result != null && p.result.query == query && p.dataset == dataset()){
			result = p.result;
			clampPage();
		} else {
			recompute();
		}
		fire();
		restoreHiddenAfterSeed();
		return true;
	}

	//Switches one of the scope's switches (what tapping a status line does).
	public synchronized void applyAction( String action ){
		Scope s = query.scope;
		switch (action){
			case StatusText.ACT_UNFINISHED: apply(query.withScope(s.withUnfinished(true))); break;
			case StatusText.ACT_UNKNOWN_RULESET: apply(query.withScope(s.withUnknownRuleset(true))); break;
			case StatusText.ACT_MIXED_RULESET: apply(query.withScope(s.withMixedRuleset(true))); break;
			case StatusText.ACT_OTHER_RULESETS: apply(query.withScope(s.withRules(Scope.Rules.ALL, ""))); break;
			case StatusText.ACT_DUPLICATES: apply(query.withScope(s.withDuplicates(true))); break;
			case StatusText.ACT_SMALL_GROUPS: apply(query.withMinSample(0)); break;
			case StatusText.ACT_HIDDEN_INFO: setShowInternal(true); break;
			default:
		}
	}
}


