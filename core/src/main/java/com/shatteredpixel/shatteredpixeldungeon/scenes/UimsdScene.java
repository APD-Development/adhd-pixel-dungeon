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


package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.Query;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdAsk;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdWarmCache;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Filter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.GuideGeometry;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdHome;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdTutorial;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ResultTable;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.StatusText;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.TitleFit;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ViewDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ui.UimsdStyle;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.apd.TitleGeometry;
import com.watabou.noosa.Image;
import com.shatteredpixel.shatteredpixeldungeon.ui.UimsdHomePane;
import com.shatteredpixel.shatteredpixeldungeon.ui.UimsdTable;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdColumns;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdChoices;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdFilter;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdGuide;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdReport;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdRow;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdSort;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdStatus;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Group;
import com.watabou.noosa.PointerArea;
import com.watabou.utils.RectF;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//APD: UIMSD, the Ultimate Infinite Mega Statistics Database. This scene is only a screen for a UimsdModel: it draws the model's
//status, filters and table and passes taps back to it. It knows nothing about any particular dataset.
//
//It is opened by the UIMSD button of the title screen (the primary entry point) and, for testing, by "Open UIMSD (preview)" in
//APD Settings. It opens on the Home page (the summary: UimsdHome); the advanced tables are one tap away and Back from one returns to
//Home. The Guide and the interactive tour (UimsdTutorial) are reachable from both.
public class UimsdScene extends PixelScene {

	//UIMSD is the short name (buttons, navigation); the header of this screen shows the full name
	public static final String FULL_NAME = "Ultimate Infinite Mega Statistics Database";

	public static Class<? extends PixelScene> backTo = TitleScene.class;
	//the model outlives the scene so a screen rotation (which rebuilds the scene) keeps where the player was
	private static UimsdModel model;
	//INDEV only: the screen is showing the synthetic demo data instead of the real history
	private static boolean showingDemo;
	//the player's own model (the one the screen shows unless the tour is running on a model of its own)
	private static UimsdModel mainModel;
	private static File mainRoot;
	// Invalidates an off-thread title claim if another open or navigation wins first.
	private static long openGeneration;
	private static String rulesId = "", rulesLabel = "";
	//Home or the advanced tables; and whether Home counts runs under all rules
	private static volatile boolean atHome = true;
	private static boolean homeAllRules;
	private static UimsdHome cachedHome;
	private static Catalog cachedCatalog;
	private static boolean cachedAll;
	//the interactive tour: runs on a model of its own, on the player's data or on a small sample history kept in a folder of its own
	private static volatile UimsdTutorial tutorial;
    private static final Object TOUR_LOCK = new Object();
    private static final Object SAMPLE_LOCK = new Object();
    private static long tourGeneration;
	//what Back does when a table was opened from a question's answer and there is no earlier table to go back to
	private static Runnable returnTo;
	private static volatile boolean tutorialPreparing;
	private static volatile boolean showingSample;
	private static File sampleRoot;
	private boolean offerPending;
	private final Map<String, float[]> targets = new HashMap<>();

	private volatile boolean dirty = true;
	private Group ui;
	private ExitButton btnExit;
	// Timestamp only after a complete, interactive Home has actually been drawn.
	private volatile long firstReadyHomeDrawNanos;
	private long createDurationNanos, rebuildDurationNanos, firstHomeDrawDurationNanos;

	//A replay borrows the game's static state: never while a game is open behind this screen.
	public static com.shatteredpixel.shatteredpixeldungeon.apd.replay.FloorReplay.LiveGameCheck liveGameCheck(){
		return () -> backTo == GameScene.class ? "a game is open behind this screen" : null;
	}

    /** Called before level loading and when returning to gameplay; no persistent data is touched. */
    public static void releaseForGameplay(){
        ++openGeneration;
        UimsdWarmCache.releaseForGameplay();
        if(model!=null)model.releaseForGameplay();
        if(mainModel!=null&&mainModel!=model)mainModel.releaseForGameplay();
        cachedHome=null;cachedCatalog=null;
        com.watabou.utils.SupportDiagnostics.event("memory.release_stats", "gameplay: derived catalogs and pending preload released");
        com.watabou.utils.SupportDiagnostics.memoryEvent("uimsd.released_for_gameplay");
    }

    /** A new open must not retain the previous viewer's complete derived history. */
    private static void retirePreviousViewer(){
        if(model!=null)model.releaseForGameplay();
        if(mainModel!=null && mainModel!=model)mainModel.releaseForGameplay();
        cachedHome=null;cachedCatalog=null;
        com.watabou.utils.SupportDiagnostics.event("uimsd.retire_previous", "derived viewer catalogs released; player history unchanged");
        com.watabou.utils.SupportDiagnostics.memoryEvent("uimsd.previous_retired");
    }

	//Opens UIMSD again where Back would have gone (after switching between demo and real data, or deleting the demo data).
	public static void reopen(){
		openNow(backTo);
	}

	//Every way into UIMSD comes here; the gate decides whether it is open yet (it always is while the unlock gate is off).
	public static void open( Class<? extends PixelScene> from ){
		com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate.open(from);
	}

	// Start a complete, validated Home model while the player is at the title.
	// All file scans and statistics computation run on the prewarm worker.
	public static void prewarmTitle(){
		if (!com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate.canPrewarm()) return;
		File root = ApdRecorder.historyRoot();
		if (root == null) return;
		if (DemoData.viewingDemo() && (DemoData.available() || DemoData.isSyntheticRoot(DemoData.demoRootFor(root)))){
			root = DemoData.demoRootFor(root);
			if (!DemoData.isSyntheticRoot(root)) return;
		}
		Ruleset rules = Ruleset.forNewRun();
		String label = "challenges " + SPDSettings.challenges() + ", exotic "
				+ (rules.isUnknown() ? "not recorded (older run)" : rules.exoticChance + "%")
				+ ", difficulty " + SPDSettings.apdDifficulty();
		UimsdWarmCache.prewarm(root, rules.id, label, homeAllRules);
	}

	//Opens UIMSD from a screen. 'from' is where Back returns to.
	public static void openNow( Class<? extends PixelScene> from ){
        com.watabou.utils.SupportDiagnostics.memoryEvent("uimsd.open_begin");
		com.watabou.utils.SupportDiagnostics.event("uimsd.open", (from == GameScene.class ? "from=game" : "from=menu") + " prewarm_failure=" + UimsdWarmCache.preparationFailure());
		backTo = from == GameScene.class ? GameScene.class : TitleScene.class;
		final Class<? extends PixelScene> targetBackTo = backTo;
		final com.watabou.noosa.Scene openingScene = ShatteredPixelDungeon.scene();
		File root = ApdRecorder.historyRoot();
		//demo data can be shown in any build once it exists (the Demo Unlock makes it in release builds too)
		showingDemo = DemoData.viewingDemo() && (DemoData.available() || DemoData.isSyntheticRoot(DemoData.demoRootFor(root)));
		if (showingDemo){
			root = DemoData.demoRootFor(root);
			if (!DemoData.isSyntheticRoot(root)) root = null;   //no demo data has been generated
		}
		Ruleset rules = from == GameScene.class ? Ruleset.current() : Ruleset.forNewRun();
		rulesId = rules.id;
		int level = from == GameScene.class ? com.shatteredpixel.shatteredpixeldungeon.Dungeon.difficulty : SPDSettings.apdDifficulty();
		boolean levelKnown = from != GameScene.class || com.shatteredpixel.shatteredpixeldungeon.Dungeon.difficultyKnown;
		rulesLabel = "challenges " + (from == GameScene.class ? com.shatteredpixel.shatteredpixeldungeon.Dungeon.challenges : SPDSettings.challenges()) + ", exotic " + (rules.isUnknown() ? "not recorded (older run)" : rules.exoticChance + "%") + ", difficulty " + level + (levelKnown ? "" : " (older run)");
		endTutorial();
		removeLeftoverSample();
        retirePreviousViewer();
		atHome = true;
		com.shatteredpixel.shatteredpixeldungeon.apd.replay.FloorReplay.setLiveGameCheck(liveGameCheck());
		final long generation = ++openGeneration;
		final File openRoot = root;
		final String openRulesId = rulesId, openRulesLabel = rulesLabel;
		final boolean openAllRules = homeAllRules;
		if (openRoot == null){
			completeOpen(generation, openingScene, targetBackTo, null, null);
			return;
		}
		// Flush and complete freshness validation run on a short prioritized
		// worker. It hands the exact validated READY model to the render thread;
		// a cache miss returns to normal priority before starting a cold load.
		UimsdModel.claimForOpen(openRoot, openRulesId, openRulesLabel, openAllRules,
				ApdRecorder::flushForReading, new UimsdModel.OpenClaimListener(){
			@Override
			public void claimed(final UimsdModel claimed){
				com.watabou.noosa.Game.runOnRenderThread(new com.watabou.utils.Callback(){
					@Override
					public void call(){
						completeOpen(generation, openingScene, targetBackTo, openRoot, claimed);
					}
				});
			}
		});
	}

	private static void completeOpen(long generation, com.watabou.noosa.Scene openingScene,
			Class<? extends PixelScene> targetBackTo, File root, UimsdModel opened){
		if (generation != openGeneration || ShatteredPixelDungeon.scene() != openingScene) {
            if(opened!=null)opened.releaseForGameplay();
            return;
        }
		backTo = targetBackTo;
		model = opened;
		if (model != null)
			model.setSeedFloorsProvider(com.shatteredpixel.shatteredpixeldungeon.apd.replay.SeedFloorsRunner.INSTANCE);
		mainModel = model;
		mainRoot = root;
		//from the title screen it switches the way the other title screens do (no fade through black)
		if (backTo == TitleScene.class) ShatteredPixelDungeon.switchNoFade(UimsdScene.class);
		else ShatteredPixelDungeon.switchScene(UimsdScene.class);
	}

	@Override
	public void create(){
		long createStart = System.nanoTime();
		super.create();
		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		// Reuse APD's existing title artwork. The inherited animated backdrop
		// allocates many VBOs and loads several atlases on the first Home frame.
		Image bg = new Image(landscape() ? "splashes/title/apd_bg_land.png" : "splashes/title/apd_bg_port.png");
		bg.scale.set(TitleGeometry.backgroundCoverScale(w, h, bg.width(), bg.height(), Camera.main.zoom));
		bg.x = (w - bg.width()) / 2f;
		bg.y = (h - bg.height()) / 2f;
		align(bg);
		add(bg);
		add(new com.watabou.noosa.ColorBlock(w, h, 0x66000000));

		ui = new Group();
		add(ui);
		//the visible exit button is the same as the Back key: it walks back through UIMSD, it does not jump out (its default goes straight to the title)
		btnExit = new ExitButton(){
			@Override
			protected void onClick(){
				onBackPressed();
			}
		};
		RectF insets = getCommonInsets();
		btnExit.setPos(insets.left + (w - insets.left - insets.right) - btnExit.width(), insets.top);
		add(btnExit);

		if (model != null){
			model.setListener(new UimsdModel.Listener(){
				@Override
				public void changed(){
					com.watabou.utils.SupportDiagnostics.event("uimsd.model", "state=" + (model == null ? "none" : model.state()) + (model != null && model.state() == UimsdModel.State.FAILED ? " failure=" + model.failure() : ""));
                    com.watabou.utils.SupportDiagnostics.memoryEvent("uimsd.model_"+(model==null?"none":model.state()));
					dirty = true;
				}
			});
		}
		dirty = true;
		//the tour is offered once, the first time UIMSD is opened; the Guide is always there
		if (!SPDSettings.apdUimsdGuideSeen() && mainModel != null && tutorial == null && com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate.tourAvailable()){
			SPDSettings.apdUimsdGuideSeen(true);
			offerPending = true;
		}
		// A completed title preload is built by draw(), after scene and camera
		// creation. draw() calls rebuildIfNeeded() before super.draw(), so the
		// first visible frame is still the complete Home without doing expensive
		// text layout inside create().
		createDurationNanos = System.nanoTime() - createStart;
	}

	@Override
	public void draw(){
		long drawStart = System.nanoTime();
		// The worker can publish after update(). Build ready content before the
		// next draw instead of spending another frame on the loading screen.
		if (rebuildIfNeeded()) Camera.refreshMatrices();
		super.draw();
		if (firstReadyHomeDrawNanos == 0 && !dirty && atHome && model != null
				&& model.state() == UimsdModel.State.READY && targets.containsKey("home.cards")){
			long readyDraw = System.nanoTime();
			firstHomeDrawDurationNanos = readyDraw - drawStart;
			firstReadyHomeDrawNanos = readyDraw;
		}
	}

	private boolean rebuildIfNeeded(){
		if (dirty && !hasWindow(this)){
			dirty = false;
			rebuild();
			return true;
		}
		return false;
	}

	@Override
	public synchronized void update(){
		if (offerPending){
			offerPending = false;
			offerTour();
		}
		UimsdTutorial t = tutorial;
		if (t != null && !tutorialPreparing){
			if (t.isFinished()){
				endTutorial();
				atHome = true;
				dirty = true;
			} else if (t.check()){
				dirty = true;
			}
		}
        // A UIMSD modal (especially Columns) can change the table model
        // while staying open. Rebuilding the screen behind that window creates
        // new pointer listeners AFTER the modal's listeners, so on phones they
        // can capture subsequent taps through the apparently visible popup.
        // Keep the dirty flag until the last window closes. Window picker rows
        // still redraw their own checked state immediately.
        rebuildIfNeeded();
        super.update();
	}

	private void rebuild(){
		long rebuildStart = System.nanoTime();
		com.watabou.utils.SupportDiagnostics.event("uimsd.layout.begin", "home=" + atHome + " state=" + (model == null ? "none" : model.state()));
		rememberTableViewport();
		activeTable = null;
		erase(ui);
		ui.destroy();
		ui = new Group();
		add(ui);
		bringToFront(btnExit);
		build();
		if (activeTable != null) activeTable.restoreViewport(tableModel.viewport());
		rebuildDurationNanos = System.nanoTime() - rebuildStart;
		com.watabou.utils.SupportDiagnostics.event("uimsd.layout.ready", "ms=" + (rebuildDurationNanos / 1000000L) + " home=" + atHome);
	}

	private UimsdTable activeTable;
	private UimsdModel tableModel;
	private long tablePlaceId;

	private void rememberTableViewport(){
		if (activeTable != null && tableModel != null)
			tableModel.rememberViewport(tablePlaceId, activeTable.captureViewport());
	}

	@Override public void destroy(){
		// The model survives rotation; its table cameras do not.
		rememberTableViewport();
		super.destroy();
	}

	//---- drawing the model ----

	private float x0, contentW, y;

	private RenderedTextBlock text( String s, int size, int color, float maxW ){
		RenderedTextBlock t = renderTextBlock(size);
		t.text(s, (int) maxW);
		t.hardlight(color);
		ui.add(t);
		return t;
	}

	//remembers where an element of the screen is, so the tour can point at it
	private void reg( String id, float x, float y, float w, float h ){
		targets.put(id, new float[]{ x, y, w, h });
	}

	private void build(){
		targets.clear();
		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();
		final float margin = 4;
		x0 = insets.left + margin;
		contentW = w - insets.left - insets.right - 2 * margin;
		y = insets.top + 4;

		//the header is the full name, sized to fit in the room left of the exit button (UIMSD stays the short name elsewhere)
		float room = contentW - Math.max(0, btnExit.width() - margin) - 2;
		RenderedTextBlock title = text("UIMSD", 12, Window.TITLE_COLOR, room);
		title.setPos(x0, y);
		y = title.bottom() + 1;
		RenderedTextBlock subtitle = text(FULL_NAME, 6, UimsdStyle.MUTED, room);
		subtitle.setPos(x0, y);
		y = subtitle.bottom() + 1;

		//which build this is, so a device test can tell what is installed
		RenderedTextBlock build = text("build " + com.watabou.noosa.Game.version, 5, 0x999999, contentW);
		build.setPos(x0, y);
		y = build.bottom() + 3;

		if (showingSample || showingDemo){
			StyledButton dataIndicator = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "DEMO DATA", 6){
				@Override protected void onClick(){
					UimsdScene.this.addToFront(new WndUimsdText("Fictional statistics", "These records are made-up test data. They are stored separately from your real run history and never count toward the ten-run unlock.", false));
				}
			};
			dataIndicator.icon(com.shatteredpixel.shatteredpixeldungeon.ui.Icons.INFO.get());
            dataIndicator.textColor(UimsdStyle.WARN);
            dataIndicator.setRect(x0, y, Math.min(contentW,88), 16);ui.add(dataIndicator);y += 18;
		}

		if (tutorialPreparing){
			RenderedTextBlock t = text("Preparing the tour...", 8, 0xCCCCCC, contentW);
			t.setPos(x0, y + 20);
			return;
		}
		if (model == null){
			RenderedTextBlock t = text(showingDemo ? "No demo data yet. Generate it in APD Settings." : "History recording is not set up, so there is nothing to show yet.", 7, 0xFFFFFF, contentW);
			t.setPos(x0, y);
			return;
		}
		UimsdModel.State state = model.state();
		if (state == UimsdModel.State.LOADING || state == UimsdModel.State.EMPTY){
			RenderedTextBlock t = text("Reading your history...", 8, 0xCCCCCC, contentW);
			t.setPos(x0, y + 20);
			return;
		}
		if (state == UimsdModel.State.FAILED){
			RenderedTextBlock t = text("UIMSD could not read your history, and nothing was changed.\n" + model.failure(), 7, 0xFF8888, contentW);
			t.setPos(x0, y + 20);
			return;
		}

		float bottomLimit;
		if (atHome) bottomLimit = buildHome(h, insets);
		else bottomLimit = buildAdvanced(h, insets);
		buildTour(bottomLimit, insets);
	}

	//---- the starter the Guide uses ----

	private static final WndUimsdGuide.Starter starter = new WndUimsdGuide.Starter(){
		@Override
		public void startTour( int stepIndex ){
			if (ShatteredPixelDungeon.scene() instanceof UimsdScene){
				((UimsdScene) ShatteredPixelDungeon.scene()).startTour(stepIndex);
			}
		}
	};

	private void openGuide(){
		WndUimsdGuide.show(starter);
	}

	//---- Home ----

	private UimsdHome homeData(){
		Catalog c = model.catalog();
		if (c == null) return null;
		if (cachedHome == null || cachedCatalog != c || cachedAll != homeAllRules){
			UimsdHome prepared = model.preparedHome(c, homeAllRules);
			cachedHome = prepared == null ? UimsdHome.build(c, model.currentRuleset(), homeAllRules) : prepared;
			cachedCatalog = c;
			cachedAll = homeAllRules;
		}
		return cachedHome;
	}

    private void openSearch(){
        if(model==null || model.catalog()==null)return;
        com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdSearch.open(
                model.catalog(),UimsdScene::openSearchResult);
    }

    private static void openSearchResult(String viewId){
        if(ShatteredPixelDungeon.scene() instanceof UimsdScene){
            ((UimsdScene)ShatteredPixelDungeon.scene()).openView(viewId);
        }
    }

	private void openView( String viewId ){
		model.showRoot(viewId);
		atHome = false;
		dirty = true;
	}

	//draws the Home page; returns the bottom of the room it used (what the tour panel may sit above)
	private float buildHome( int h, RectF insets ){
		UimsdHome home = homeData();
		float bottom = h - insets.bottom - 4;
		if (home == null) return bottom;
		float gap = 2, each = (contentW - 2 * gap)/3f;
		StyledButton guide = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Guide", 7){
			@Override protected void onClick(){ openGuide(); }
		};
		guide.setRect(x0, y, each, 18);ui.add(guide);reg("guide", x0, y, each, 18);
		StyledButton report = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Report", 7){
			@Override protected void onClick(){ WndUimsdReport.ofLifetime(model, homeAllRules); }
		};
		report.setRect(guide.right()+gap, y, each, 18);ui.add(report);reg("report", report.left(), y, each, 18);
		StyledButton tables = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Tables", 7){
			@Override protected void onClick(){ atHome=false;dirty=true; }
		};
		tables.setRect(report.right()+gap, y, each, 18);ui.add(tables);reg("tables", tables.left(), y, each, 18);
		y += 20;
        StyledButton search = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Search UIMSD", 7){
            @Override protected void onClick(){openSearch();}
        };
        search.setRect(x0,y,(contentW-2)*.6f,16);ui.add(search);
        StyledButton research=new StyledButton(Chrome.Type.GREY_BUTTON_TR,"Research lab",6){
            @Override protected void onClick(){com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdResearch.show(model);}
        };
        research.setRect(x0+(contentW-2)*.6f+2,y,(contentW-2)*.4f,16);ui.add(research);
        reg("search",x0,y,(contentW-2)*.6f,16);
        y+=18;
		StyledButton rules = new StyledButton(Chrome.Type.GREY_BUTTON_TR, home.mixedRulesets ? "Rules: Mixed" : homeAllRules ? "Rules: all" : "Rules: current", 6){
			@Override protected void onClick(){ homeAllRules=!homeAllRules;dirty=true; }
		};
		rules.setRect(x0,y,contentW-22,18);ui.add(rules);
		StyledButton scopeInfo = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "?", 7){
			@Override protected void onClick(){UimsdScene.this.addToFront(new WndUimsdText("Rules and record coverage", home.scopeNote, false));}
		};
		scopeInfo.setRect(rules.right()+2,y,20,18);ui.add(scopeInfo);
		y += 20;

		if (home.isEmpty()){
			buildFirstRun(home, bottom);
			return bottom;
		}

		final UimsdHomePane pane = new UimsdHomePane();
		pane.setListener(new UimsdHomePane.Listener(){
			@Override
			public void onLink( String viewId ){
				if (viewId.startsWith("!outcome:")){
					model.showQuery(com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdHomeNavigation.outcome(viewId, homeAllRules));
					atHome=false;dirty=true;return;
				}
				if (UimsdHome.ASK_LINK.equals(viewId)){
					WndUimsdAsk.open(model.catalog(), model.currentRuleset(), new WndUimsdAsk.Host(){
						@Override
						public void openAsTable( final Query q, final Runnable backToAnswer ){
							model.showQuery(q);
							returnTo = backToAnswer;
							atHome = false;
							//showQuery notifies the current scene's model listener.
						}
						@Override public File root(){ return model.root(); }
					});
					return;
				}
				openView(viewId);
			}
		});
		ui.add(pane);
		pane.setHome(home);
		pane.setRect(x0, y, contentW, Math.max(40, bottom - y));
		reg("home.cards", x0, y, contentW, Math.max(40, bottom - y));
		//while the tour asks for the Gear card's Weapons button, bring it into view and point at it
		UimsdTutorial t = tutorial;
		float[] box = pane.linkBox("weapons");
		if (box != null && t != null && "home.gear".equals(t.step().target)){
			pane.scrollTo(0, Math.max(0, box[1] - 24));
			float sy = pane.content().camera.scroll.y;
			reg("home.gear", x0 + box[0], y + box[1] - sy, box[2], box[3]);
		}
		return bottom;
	}

	//What the Home page shows before there is anything to count: what UIMSD is, what will appear here, and the tour as the main thing to do.
	private void buildFirstRun( UimsdHome home, float bottom ){
		RenderedTextBlock welcome = text("Welcome to UIMSD", UimsdStyle.SIZE_TITLE, UimsdStyle.TITLE, contentW);
		welcome.setPos(x0, y + 2);
		y = welcome.bottom() + 2;
		RenderedTextBlock what = text(home.emptyMessage(), UimsdStyle.SIZE_HELP, UimsdStyle.TEXT, contentW);
		what.setPos(x0, y);y = what.bottom() + 4;

		StyledButton tour = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Take the tour (2 minutes)", 8){
			@Override
			protected void onClick(){
				startTour(0);
			}
		};
		tour.setRect(x0, y, contentW, 22);
		ui.add(tour);
		reg("tour", x0, y, contentW, 22);
		y += 24;
		RenderedTextBlock sampleNote = text("The tour uses a small made-up history, so it works even before you have played. It never touches your own.", UimsdStyle.SIZE_META, UimsdStyle.FAINT, contentW);
		sampleNote.setPos(x0, y);
		y = sampleNote.bottom() + 4;
		StyledButton read = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Read the Guide", 7){
			@Override
			protected void onClick(){
				openGuide();
			}
		};
		read.setRect(x0, y, contentW, 16);
		ui.add(read);
	}

    public static void showResearchEvidence(UimsdModel m,com.shatteredpixel.shatteredpixeldungeon.apd.query.Query q){
        if(m.openResearchQuery(q) && com.watabou.noosa.Game.scene() instanceof UimsdScene){
            atHome=false;((UimsdScene)com.watabou.noosa.Game.scene()).dirty=true;
        }
    }

	//---- the advanced tables (everything UIMSD could always do) ----

	private float buildAdvanced( int h, RectF insets ){
		//home, view, back and guide
		ViewDef view = model.view();
		float homeW = 30, helpW = 14, backW = model.canGoBack() ? 32 : 0, gap = 2;
		float viewW = contentW - homeW - helpW - backW - gap * (model.canGoBack() ? 3 : 2);
		float cx0 = x0;
		StyledButton homeBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Home", 7){
			@Override
			protected void onClick(){
				atHome = true;
				dirty = true;
			}
		};
		homeBtn.setRect(cx0, y, homeW, 16);
		ui.add(homeBtn);
		reg("home", cx0, y, homeW, 16);
		cx0 += homeW + gap;
		StyledButton viewBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Tables", 7){
			@Override
			protected void onClick(){
				pickView();
			}
		};
		viewBtn.setRect(cx0, y, viewW, 16);
		ui.add(viewBtn);
		reg("view", cx0, y, viewW, 16);
		cx0 += viewW + gap;
		if (model.canGoBack()){
			StyledButton back = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Back", 8){
				@Override
				protected void onClick(){
					model.back();
				}
			};
			back.setRect(cx0, y, backW, 16);
			ui.add(back);
			reg("back", cx0, y, backW, 16);
			cx0 += backW + gap;
		}
		StyledButton help = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "?", 8){
			@Override
			protected void onClick(){
				openGuide();
			}
		};
		help.setRect(cx0, y, helpW, 16);
		ui.add(help);
		reg("guide", cx0, y, helpW, 16);
		y += 18;
        StyledButton search = new StyledButton(Chrome.Type.GREY_BUTTON_TR,"Search UIMSD",6){
            @Override protected void onClick(){openSearch();}
        };
        search.setRect(x0,y,(contentW-2)*.6f,16);ui.add(search);
        StyledButton research=new StyledButton(Chrome.Type.GREY_BUTTON_TR,"Research lab",6){
            @Override protected void onClick(){com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdResearch.show(model);}
        };
        research.setRect(x0+(contentW-2)*.6f+2,y,(contentW-2)*.4f,16);ui.add(research);y+=18;

		//The complete table description remains in the existing Tables picker.

		//work in progress / why something is unavailable (seed floors)
		String notice = model.notice();
		if (!notice.isEmpty()){
			RenderedTextBlock n = text(notice, UimsdStyle.SIZE_HELP, UimsdStyle.WARN, contentW);
			n.setPos(x0, y);
			y = n.bottom() + 3;
		}

		//status: the headline, then the most important note (a warning if there is one)
		List<StatusText.Line> lines = model.status();
		RenderedTextBlock head = text(lines.isEmpty() ? "" : lines.get(0).text, UimsdStyle.SIZE_HELP, UimsdStyle.TEXT, contentW - 46);
		head.setPos(x0, y);
		float statusBottom = head.bottom();
		int shown = 1;
		StatusText.Line second = null;
		for (StatusText.Line l : lines) if (l.kind == StatusText.WARN){ second = l; break; }
		if (second == null && lines.size() > 1) second = lines.get(1);
		if (second != null){
			RenderedTextBlock sec = text(second.text, UimsdStyle.SIZE_META, second.kind == StatusText.WARN ? UimsdStyle.WARN : UimsdStyle.MUTED, contentW - 46);
			sec.setPos(x0, statusBottom + 1);
			statusBottom = sec.bottom();
			shown = 2;
		}
		int notes = lines.size() - shown;
		StyledButton statusBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, notes > 0 ? "Notes (" + notes + ")" : "Notes", 6){
			@Override
			protected void onClick(){
				WndUimsdStatus.show(model);
			}
		};
		statusBtn.setRect(x0 + contentW - 44, y, 44, 14);
		ui.add(statusBtn);
		reg("notes", x0 + contentW - 44, y, 44, 14);
		reg("status", x0, y, contentW - 46, Math.max(statusBottom - y, 10));
		y = Math.max(statusBottom, y + 14) + 3;

		//All conditions remain editable in a scrolling native picker; their count does not consume table height.
		final List<Filter> filters = model.query().filters;
		if (!filters.isEmpty()){
			StyledButton conditions = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Conditions (" + filters.size() + ")", 6){
				@Override protected void onClick(){
					showConditions();
				}
			};
			conditions.setRect(x0,y,contentW,16);ui.add(conditions);y += 18;
		}

		//bottom controls first, so the table gets what is left
		final ResultTable table = model.table();
		float bottom = h - insets.bottom - 4;
		int controlsPerRow = contentW < 160 ? 2 : 4;
		float controlH = 18;
		float bar = controlsPerRow == 2 ? 2 * controlH : controlH;
		float barTop = bottom - bar;
		String[] names = { "Filter", "Sort", "Columns", "Options" };
		String[] ids = { "filter", "sort", "columns", "options" };
		float bwid = (contentW - 2 * (controlsPerRow-1)) / controlsPerRow;
		for (int i = 0; i < 4; i++){
			final int which = i;
			String shownName = names[i];
			if (i == 0 && !model.query().filters.isEmpty()) shownName += " (" + model.query().filters.size() + ")";
			if (i == 1 && !model.query().sorts.isEmpty()) shownName += " (" + model.query().sorts.size() + ")";
			StyledButton b = new StyledButton(Chrome.Type.GREY_BUTTON_TR, shownName, 6){
				@Override
				protected void onClick(){
					switch (which){
						case 0: WndUimsdFilter.add(model); break;
						case 1: WndUimsdSort.show(model); break;
						case 2: WndUimsdColumns.show(model); break;
						default: WndUimsdOptions.show(model);
					}
				}
			};
			float bx = x0 + (i % controlsPerRow) * (bwid + 2);
			float by = barTop + (i / controlsPerRow) * controlH;
			b.setRect(bx, by, bwid, controlH - 1);
			ui.add(b);
			reg(ids[i], bx, by, bwid, controlH - 1);
		}
		float tableBottom = barTop - 2;
		if (table != null && table.pageCount() > 1){
			float py = barTop - 16;
			StyledButton prev = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "<", 8){
				@Override
				protected void onClick(){
					model.setPage(model.page() - 1);
				}
			};
			prev.setRect(x0, py, 20, 14);
			prev.enable(table.page() > 0);
			ui.add(prev);
			StyledButton next = new StyledButton(Chrome.Type.GREY_BUTTON_TR, ">", 8){
				@Override
				protected void onClick(){
					model.setPage(model.page() + 1);
				}
			};
			next.setRect(x0 + contentW - 20, py, 20, 14);
			next.enable(table.page() < table.pageCount() - 1);
			ui.add(next);
			int from = table.pageStart + 1, to = table.pageStart + table.lines.size();
			RenderedTextBlock pg = text((table.grouped ? "Groups " : model.dataset().def.countLabel + " ") + from + "-" + to + " of " + table.total + "   (page " + (table.page() + 1) + " of " + table.pageCount() + ")", UimsdStyle.SIZE_HELP, UimsdStyle.MUTED, contentW - 46);
			pg.setPos(x0 + (contentW - pg.width()) / 2f, py + (14 - pg.height()) / 2f);
			tableBottom = py - 2;
		}

		if (table == null) return barTop;
		//a table wider than the screen says so, and how many columns are off to the right
		com.shatteredpixel.shatteredpixeldungeon.apd.query.TableGeometry geoNow = com.shatteredpixel.shatteredpixeldungeon.apd.query.TableGeometry.of(table);
		if (geoNow.totalWidth > contentW + 1){
			int off = 0;
			for (int c = 0; c < geoNow.lefts.length; c++) if (geoNow.lefts[c] + geoNow.widths[c] > contentW + 1) off++;
			RenderedTextBlock more = text("Swipe the table sideways: " + off + (off == 1 ? " more column" : " more columns") + " to the right.", UimsdStyle.SIZE_META, UimsdStyle.FAINT, contentW);
			more.setPos(x0, y);
			y = more.bottom() + 2;
		}
		UimsdTable t = new UimsdTable();
		activeTable = t;
		tableModel = model;
		tablePlaceId = model.placeId();
		t.setListener(new UimsdTable.Listener(){
			@Override
			public void onSort( String columnId ){
				model.cycleSort(columnId);
			}

			@Override
			public void onRow( ResultTable.Line line ){
				WndUimsdRow.show(model, line);
			}
		});
		ui.add(t);
		t.setRect(x0, y, contentW, Math.max(40, tableBottom - y));
		t.setTable(table);
		reg("table.header", x0, y, contentW, com.shatteredpixel.shatteredpixeldungeon.apd.query.TableGeometry.HEADER_HEIGHT);
		reg("table.firstRow", x0, y + com.shatteredpixel.shatteredpixeldungeon.apd.query.TableGeometry.HEADER_HEIGHT, contentW,
				com.shatteredpixel.shatteredpixeldungeon.apd.query.TableGeometry.ROW_HEIGHT);
		if (table.lines.isEmpty()){
			RenderedTextBlock none = text("Nothing matches. Open Notes to see what is being left out, or Options to widen what counts.", 7, UimsdStyle.MUTED, contentW);
			none.setPos(x0, y + TableGeometryHeader() + 6);
		}
		return barTop;
	}

	private static float TableGeometryHeader(){
		return com.shatteredpixel.shatteredpixeldungeon.apd.query.TableGeometry.HEADER_HEIGHT;
	}

	private void showConditions(){
		List<WndUimsdList.Entry> entries = new ArrayList<>();
		for (Filter filter : model.query().filters) entries.add(WndUimsdList.Entry.item(StatusText.describe(model.dataset().def, filter)));
		UimsdScene.this.addToFront(WndUimsdList.menu("Conditions", "Tap a condition to change or remove it. All conditions must match.", entries,
				(index, checked) -> WndUimsdFilter.chip(model, index)).restoreWith(() -> {
			com.watabou.noosa.Scene active = com.watabou.noosa.Game.scene();
			if (active instanceof UimsdScene) ((UimsdScene) active).showConditions();
		}));
	}

	private void pickView(){
		final List<ViewDef> views = model.catalog() == null ? new ArrayList<ViewDef>() : model.catalog().views();
		//grouped by section, in the order the sections first appear; the table you are looking at is marked
		List<String> sections = new ArrayList<>();
		for (ViewDef v : views) if (!sections.contains(v.group)) sections.add(v.group);
		final List<ViewDef> order = new ArrayList<>();
		List<WndUimsdList.Entry> entries = new ArrayList<>();
		for (String section : sections){
			entries.add(WndUimsdList.Entry.heading(section.isEmpty() ? "More tables" : section));
			for (ViewDef v : views){
				if (!v.group.equals(section)) continue;
				order.add(v);
				entries.add(WndUimsdList.Entry.item(v.title, v.description).current(v.id.equals(model.viewId())));
			}
		}
		ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Tables",
				"Each table looks at your history in a different way. Tap one to open it.", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				model.selectView(order.get(i).id);
			}
		}).restoreWith(() -> {
			com.watabou.noosa.Scene active = com.watabou.noosa.Game.scene();
			if (active instanceof UimsdScene) ((UimsdScene) active).pickView();
		}));
	}

	//---- the interactive tour ----

	//the tour panel and the highlight, over everything else
	private void buildTour( float bottomLimit, RectF insets ){
		UimsdTutorial t = tutorial;
		if (t == null || t.isFinished()) return;
		UimsdTutorial.Step step = t.step();
		float pw = contentW;
		RenderedTextBlock title = text("Tour " + (t.index() + 1) + "/" + t.count() + ": " + step.title, 7, Window.TITLE_COLOR, pw - 8);
		RenderedTextBlock body = text(step.text, 6, 0xFFFFFF, pw - 8);
		float buttonsH = 14;
		float ph = Math.max(GuideGeometry.MIN_PANEL, 4 + title.height() + 2 + body.height() + 3 + buttonsH + 4);
		ph = Math.min(ph, (Camera.main.height - insets.top - insets.bottom) * 0.5f);
		float[] target = step.target.isEmpty() ? null : targets.get(step.target);
		float topLimit = insets.top + 4 + 16;
		float py = GuideGeometry.placeTop(topLimit, bottomLimit, ph, target == null ? 0 : target[0], target == null ? 0 : target[1],
				target == null ? 0 : target[2], target == null ? 0 : target[3], target != null);

		//the highlight
		if (target != null){
			float tx = target[0] - 2, ty = target[1] - 2, tw = target[2] + 4, th = target[3] + 4;
			int c = 0xFFFFFF00;
			ColorBlock[] edges = {
					new ColorBlock(tw, 2, c), new ColorBlock(tw, 2, c), new ColorBlock(2, th, c), new ColorBlock(2, th, c) };
			edges[0].x = tx; edges[0].y = ty;
			edges[1].x = tx; edges[1].y = ty + th - 2;
			edges[2].x = tx; edges[2].y = ty;
			edges[3].x = tx + tw - 2; edges[3].y = ty;
			for (ColorBlock e : edges) ui.add(e);
		}

		//the panel: its own area swallows taps, so they do not reach the table underneath
		PointerArea block = new PointerArea(x0, py, pw, ph);
		block.blockLevel = PointerArea.ALWAYS_BLOCK;
		ui.add(block);
		ColorBlock bg = new ColorBlock(pw, ph, 0xEE0A1420);
		bg.x = x0;
		bg.y = py;
		ui.add(bg);
		ColorBlock edge = new ColorBlock(pw, 1, 0xFFFFFF88);
		edge.x = x0;
		edge.y = py;
		ui.add(edge);
		bringToFrontOf(title);
		bringToFrontOf(body);
		title.setPos(x0 + 4, py + 4);
		body.setPos(x0 + 4, title.bottom() + 2);
		float by = py + ph - buttonsH - 4;
		boolean last = t.index() >= t.count() - 1;
		float bw = (pw - 12) / 3f;
		StyledButton next = new StyledButton(Chrome.Type.GREY_BUTTON_TR, step.expect == UimsdTutorial.Expect.NEXT ? (last ? "Finish" : "Next") : "Skip step", 7){
			@Override
			protected void onClick(){
				UimsdTutorial tt = tutorial;
				if (tt != null){
					tt.next();
					dirty = true;
				}
			}
		};
		next.setRect(x0 + 4, by, bw, buttonsH);
		ui.add(next);
		StyledButton prev = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Previous", 7){
			@Override
			protected void onClick(){
				UimsdTutorial tt = tutorial;
				if (tt != null){
					tt.previous();
					dirty = true;
				}
			}
		};
		prev.setRect(x0 + 6 + bw, by, bw, buttonsH);
		prev.enable(t.index() > 0);
		ui.add(prev);
		StyledButton exit = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Exit tour", 7){
			@Override
			protected void onClick(){
				endTutorial();
				atHome = true;
				dirty = true;
			}
		};
		exit.setRect(x0 + 8 + 2 * bw, by, bw, buttonsH);
		ui.add(exit);
	}

	//puts a text block (created earlier, under the panel) on top of the panel
	private void bringToFrontOf( RenderedTextBlock t ){
		ui.remove(t);
		ui.add(t);
	}

	//the tour can use the player's own runs when there are enough of them to show every kind of screen
	private void offerTour(){
		UimsdScene.this.addToFront(new WndUimsdChoices("New to UIMSD?", "Take a short tour of the real screens. You can skip any step, and the Guide button brings it back whenever you want.",
				"Start the tour", "Later"){
			@Override
			protected void onSelect( int index ){
				if (index == 0) ((UimsdScene) ShatteredPixelDungeon.scene()).startTour(0);
			}
		}.restoreWith(() -> ((UimsdScene) ShatteredPixelDungeon.scene()).offerTour()));
	}

	private void chooseTourData(final int stepIndex){
		UimsdScene.this.addToFront(new WndUimsdChoices("Tour", "Which data should the tour use? Your own runs, or a small made-up sample history (nothing in your history is changed either way).",
				"My own runs", "Sample data"){
			@Override
			protected void onSelect( int index ){
				((UimsdScene) ShatteredPixelDungeon.scene()).beginTour(stepIndex, index == 0);
			}
		}.restoreWith(() -> ((UimsdScene) ShatteredPixelDungeon.scene()).chooseTourData(stepIndex)));
	}

	private boolean ownDataIsEnough(){
		if (mainModel == null || mainModel.state() != UimsdModel.State.READY || mainModel.catalog() == null) return false;
		UimsdHome home = UimsdHome.build(mainModel.catalog(), rulesId, false);
		return home.runs >= 5 && home.card("gear") != null;
	}

	private void startTour( final int stepIndex ){
		if (!com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate.tourAvailable()){
			UimsdScene.this.addToFront(new WndUimsdText("UIMSD", "The guided tour opens after you finish 10 runs of your own. Demo runs do not count toward it.", false));
			return;
		}
		if (mainModel == null || ApdRecorder.historyRoot() == null && mainRoot == null) return;
		if (tutorial != null) endTutorial();
		if (ownDataIsEnough()){
			chooseTourData(stepIndex);
		} else {
			beginTour(stepIndex, false);
		}
	}

	//The loading worker can outlive the scene that launched it. Resolve the current scene
	//on the render thread, both at completion and for later changes to its tour model.
	private static void invalidateTourScene(){
		com.watabou.noosa.Game.runOnRenderThread(() -> {
			if (com.watabou.noosa.Game.instance == null) return;
			com.watabou.noosa.Scene active = com.watabou.noosa.Game.scene();
			if (active instanceof UimsdScene) ((UimsdScene) active).dirty = true;
		});
	}


    private static boolean currentTour(long generation){
        synchronized(TOUR_LOCK){return generation == tourGeneration;}
    }

    private void beginTour(final int stepIndex, final boolean own){
        final long generation;
        synchronized(TOUR_LOCK){
            generation = ++tourGeneration;
            tutorialPreparing = true;
        }
        dirty = true;
        final File requestedOwnRoot = mainRoot;
        File recordedRoot = ApdRecorder.historyRoot();
        final File requestedSampleBase = recordedRoot != null ? recordedRoot : mainRoot;
        final String requestedRulesId = rulesId, requestedRulesLabel = rulesLabel;
        final Ruleset sampleRules = Ruleset.forNewRun();
        Thread worker = new Thread(() -> {
            // Preparing/deleting the shared sample folder must not overlap another request.
            synchronized(SAMPLE_LOCK){
                if (!currentTour(generation)) return;
                File root = own ? requestedOwnRoot : DemoData.sampleRootFor(requestedSampleBase);
                boolean published = false;
                try {
                    if (!own) DemoGenerator.generateSample(root, sampleRules, System.currentTimeMillis());
                    UimsdModel tm = new UimsdModel(requestedRulesId, requestedRulesLabel);
                    tm.load(root);
                    tm.awaitLoad(30000);
                    if (tm.state() != UimsdModel.State.READY)
                        throw new IllegalStateException("the tour's data could not be read");
                    UimsdTutorial.prepare(tm, stepIndex);
                    UimsdTutorial prepared = new UimsdTutorial(tm, stepIndex);
                    synchronized(TOUR_LOCK){
                        if (generation != tourGeneration) return;
                        sampleRoot = own ? null : root;
                        showingSample = !own;
                        atHome = stepIndex <= UimsdTutorial.indexOf("view");
                        model = tm;
                        tm.setListener(UimsdScene::invalidateTourScene);
                        tutorial = prepared;
                        published = true;
                    }
                } catch (Throwable error){
                    synchronized(TOUR_LOCK){
                        if (generation == tourGeneration){
                            tutorial = null;
                            showingSample = false;
                            model = mainModel;
                        }
                    }
                } finally {
                    synchronized(TOUR_LOCK){
                        if (generation == tourGeneration) tutorialPreparing = false;
                    }
                    if (!own && !published) deleteSampleLater(root);
                    if (currentTour(generation)) invalidateTourScene();
                }
            }
        }, "apd-uimsd-tour");
        worker.setDaemon(true);
        worker.start();
    }

	//stops the tour: back to the player's own model, and the sample history (if any) is deleted

    private static void endTutorial(){
        synchronized(TOUR_LOCK){
            ++tourGeneration;
            tutorial = null;
            tutorialPreparing = false;
            showingSample = false;
            if (mainModel != null && model != mainModel){
                if (model != null) model.setListener(null);
                model = mainModel;
            }
            deleteSampleLater();
        }
    }

    private static void deleteSampleLater(){
        final File root = sampleRoot;
        sampleRoot = null;
        deleteSampleLater(root);
    }

    private static void deleteSampleLater(final File root){
        if (root == null) return;
        Thread cleanup = new Thread(() -> {
            synchronized(SAMPLE_LOCK){
                synchronized(TOUR_LOCK){
                    if (root.equals(sampleRoot)) return; // A newer request owns this sample now.
                }
                DemoData.delete(root); // Existing marker/name guard protects real history.
            }
        }, "apd-uimsd-sample-delete");
        cleanup.setDaemon(true);
        cleanup.start();
    }

    // A sample left by a closed app is removed through the same serialized cleanup path.
    private static void removeLeftoverSample(){
        File history = ApdRecorder.historyRoot();
        File sample = history == null ? null : DemoData.sampleRootFor(history);
        if (sample != null && sample.exists()) deleteSampleLater(sample);
    }

	@Override
	protected void onBackPressed(){
		com.watabou.utils.DeviceCompat.log("APDUI", "UIMSD Back: tutorial=" + (tutorial != null) + " finished=" + (tutorial != null && tutorial.isFinished())
				+ " preparing=" + tutorialPreparing + " atHome=" + atHome + " canGoBack=" + (model != null && model.canGoBack()));
		if (tutorialPreparing){
			//Preparation is part of the tour. Back cancels it immediately and advances the
			//generation so its worker can never publish a stale model after this press.
			endTutorial();
			atHome = true;
			dirty = true;
			return;
		}
		//a tour in progress ends first: Back leaves the tour, it does not leave the screen
		if (tutorial != null && !tutorial.isFinished()){
			endTutorial();
			atHome = true;
			dirty = true;
			return;
		}
		if (!atHome && model != null){
			if (model.canGoBack()){
				model.back();
			} else {
				//a table opened from a question's answer goes back to that answer, with the question as it was
				Runnable back = returnTo;
				returnTo = null;
				atHome = true;
				dirty = true;
				if (back != null) back.run();
			}
			return;
		}
		returnTo = null;
		endTutorial();
		if (model != null) model.setListener(null);
        retirePreviousViewer();
		if (backTo == TitleScene.class) ShatteredPixelDungeon.switchNoFade(backTo);
		else ShatteredPixelDungeon.switchScene(backTo);
	}
}







