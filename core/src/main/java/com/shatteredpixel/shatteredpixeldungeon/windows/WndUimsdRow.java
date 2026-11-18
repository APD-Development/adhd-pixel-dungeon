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
import com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge.SubmissionQueue;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Aggregate;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellFormatter;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ColumnDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.DatasetDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.GroupDetail;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.GroupRow;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.LinkDef;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.QueryResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ResultTable;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.RunTurnBrowser;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Row;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdModel;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.UimsdStyle;
import com.shatteredpixel.shatteredpixeldungeon.ui.UimsdTable;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.NinePatch;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.List;

//APD: everything about one row (or one group) of a UIMSD table, value by value, with what kind of knowledge each value is, and
//the links to the matching rows of other datasets. Built only from the dataset's definition.
public class WndUimsdRow extends Window {

	private static final int MARGIN = 2;
	private final UimsdModel model;
	private final ResultTable.Line selected;

	public WndUimsdRow( final UimsdModel m, ResultTable.Line line ){
		super();
		model = m;
		selected = line;
		int width = WndUimsdList.windowWidth();
		QueryResult r = m.result();
		DatasetDef def = r.def;

		final List<SheetLine> sheet = new ArrayList<>();
		final List<Action> actions = new ArrayList<>();
		final List<Action> shareActions = new ArrayList<>();
		final Row row = line.row;
		String title;
		if (row != null){
			title = r.def.title;
			String lastGroup = null;
			for (ColumnDef c : def.columns()){
				if (c.isInternal() && !r.query.showInternal) continue;
				CellText t = CellFormatter.cell(r.query, row, c);
				if (!c.group.equals(lastGroup)){
					sheet.add(SheetLine.section(c.group.isEmpty() ? "Details" : c.group.equals("Hidden") ? "Hidden information" : c.group));
					lastGroup = c.group;
				}
				sheet.add(new SheetLine(c.label, t.text, t.state == CellText.EXACT ? "" : CellFormatter.explain(t.state), t.state, t.small));
			}
			if (!row.telemetry.isEmpty()) sheet.add(new SheetLine("Recorded with", "recording version " + row.telemetry, "", CellText.EXACT, false));
			if (m.canSeedFloors(row)){
				actions.add(new Action("Seed floors (canonical)", new Runnable(){
					@Override
					public void run(){
						m.startSeedFloors(row);
					}
				}));
			}
			//Developer preview: queue this finished run locally for a future submission feature. Nothing is sent anywhere (no server exists yet).
			if (com.watabou.utils.DeviceCompat.isDebug() && WndUimsdReport.isRunsTable(def) && row.finished && !row.imported && !row.runId.isEmpty() && m.root() != null){
				if (SubmissionQueue.isQueued(m.root(), row.runId)){
					actions.add(new Action("Show what is queued for this run", new Runnable(){
						@Override
						public void run(){
							String text = SubmissionQueue.preview(m.root(), row.runId);
							ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("UIMSD", text == null ? "This run is not queued." : text, false));
						}
					}));
					actions.add(new Action("Take this run out of the queue", new Runnable(){
						@Override
						public void run(){
							boolean gone = SubmissionQueue.withdraw(m.root(), row.runId);
							ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("UIMSD", gone ? "Removed from the queue." : "It could not be removed.", false));
						}
					}));
				}
				actions.add(new Action("Queue for submission (preview, sends nothing)", new Runnable(){
					@Override
					public void run(){
						SubmissionQueue.Queued q = SubmissionQueue.enqueueRun(m.root(), row.runId, com.watabou.noosa.Game.version, System.currentTimeMillis());
						String msg;
						switch (q.result){
							case QUEUED: msg = "Queued on this device. Nothing was sent: there is no server yet."; break;
							case ALREADY_QUEUED: msg = "This run is already queued."; break;
							case REFUSED: msg = "This run cannot be queued: " + q.detail + "."; break;
							default: msg = q.detail;
						}
						ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("UIMSD", msg, false));
					}
				}));
			}
			for (final LinkDef l : def.links){
				if (m.catalog() != null && m.catalog().view(l.targetView) != null && row.isKnown(def.indexOf(l.ourColumn))){
					actions.add(new Action(l.label, new Runnable(){
						@Override
						public void run(){
							m.openLink(row, l);
						}
					}));
				}
			}
			if (RunTurnBrowser.canOpenAt(m.catalog(), r, row))
				actions.add(new Action("Run history at this turn", () -> {
					try {
						ShatteredPixelDungeon.scene().addToFront(new WndRunTurnBrowser(
								RunTurnBrowser.openAt(m.catalog(), r, row)));
					} catch (IllegalArgumentException stale) {
						ShatteredPixelDungeon.scene().addToFront(new WndMessage(
								"This occurrence or its recorded run changed. Reopen the table."));
					}
				}));
			if (WndUimsdResearch.canEquipmentWindow(r))
                actions.add(new Action("Damage after equipping", () -> WndUimsdResearch.equipmentWindow(m,r,row)));
			if (WndUimsdResearch.canDamageRecoveryWindow(r,row))
                actions.add(new Action("Healing after damage", () -> WndUimsdResearch.damageRecoveryWindow(m,r,row)));
			if (WndDeathContext.canOpen(r,row))
                actions.add(new Action("Death context", () -> WndDeathContext.show(m,row)));
			if (WndUimsdReport.isRunsTable(def)){
                actions.add(new Action("Run DNA", () -> WndUimsdResearch.dnaRun(m,row)));
                actions.add(new Action("Luck laboratory", () -> WndUimsdResearch.luckRun(m,row)));
                if (row.runId != null && !row.runId.isEmpty() && m.catalog() != null){
                    actions.add(new Action("Turn-by-turn history", () -> {
                        ShatteredPixelDungeon.scene().addToFront(new WndRunTurnBrowser(
                                new com.shatteredpixel.shatteredpixeldungeon.apd.query.RunTurnBrowser(m.catalog(),row)));
                    }));
                }
				if (row.finished){
					shareActions.add(new Action("Compare with another run...", new Runnable(){
						@Override
						public void run(){
							WndUimsdReport.compareWith(m, row);
						}
					}));
				}
				shareActions.add(new Action("Report for this run", new Runnable(){
					@Override
					public void run(){
						WndUimsdReport.ofRun(m, row);
					}
				}));
			}
		} else {
			final GroupRow g = line.group;
			GroupDetail detail = GroupDetail.build(r, g);
			title = detail.title;
			sheet.add(SheetLine.section(detail.rows + " " + (detail.rows == 1 ? def.unitSingular() : def.unitPlural()) + " in this group"));
			for (GroupDetail.Line k : detail.keys) sheet.add(new SheetLine(k.label, k.value, k.detail, k.state, false));
			for (GroupDetail.Line l : detail.lines){
				if (l.section) sheet.add(SheetLine.section(l.label));
				else sheet.add(new SheetLine(l.label, l.value, (l.detail + (l.explanation.isEmpty() ? "" : (l.detail.isEmpty() ? "" : "; ") + l.explanation)), l.state, l.small));
			}
			if (m.canOpenGroups()){
				int n = g.n;
				actions.add(new Action((n == 1 ? "Open the " : "Open all ") + n + " " + (n == 1 ? def.unitSingular() : def.unitPlural()), new Runnable(){
					@Override
					public void run(){
						m.openGroupRows(g);
					}
				}));
			}
			final int runs = m.groupRunCount(g);
			if (runs > 0 && m.canOpenGroups()){
				actions.add(new Action("Open the " + runs + (runs == 1 ? " run" : " runs") + " it appears in", new Runnable(){
					@Override
					public void run(){
						m.openGroupRuns(g);
					}
				}));
			}
			shareActions.add(new Action("Compare with another group...", new Runnable(){
				@Override
				public void run(){
					WndUimsdReport.compareGroupWith(m, g);
				}
			}));
			shareActions.add(new Action("Report for this group", new Runnable(){
				@Override
				public void run(){
					WndUimsdReport.ofGroup(m, g);
				}
			}));
		}
		//where to go first, then what to do with it
		actions.addAll(shareActions);

		RenderedTextBlock tt = PixelScene.renderTextBlock(title, UimsdStyle.SIZE_TITLE);
		tt.hardlight(UimsdStyle.TITLE);
		tt.maxWidth(width - 2 * MARGIN);
		tt.setPos(MARGIN, MARGIN);
		add(tt);
		float pos = tt.bottom() + 2 * MARGIN;

		final Component content = new Component();
		float contentH = sheet(content, width, sheet);
		//Actions share the scrolling area with the details. Fixed-height buttons below
		//the pane clipped long labels and could push the window past the screen.
		final List<float[]> actionSpans = new ArrayList<>();
		for (Action act : actions){
			RenderedTextBlock label = PixelScene.renderTextBlock(act.label, 7);
			label.maxWidth(WindowGeometry.rowTextWidth(width));
			float height = WindowGeometry.actionHeight(label.height());
			NinePatch bg = Chrome.get(Chrome.Type.GREY_BUTTON_TR);
			bg.size(width - 2 * MARGIN, height);
			bg.x = MARGIN;
			bg.y = contentH;
			content.add(bg);
			label.setPos(WindowGeometry.ROW_PAD + 1, contentH + (height - label.height()) / 2f);
			content.add(label);
			actionSpans.add(new float[]{contentH, contentH + height});
			contentH += height + MARGIN;
		}
		content.setSize(width, contentH);
		ScrollPane pane = new ScrollPane(content){
			@Override
			public void onClick(float x, float y){
				if (x < MARGIN || x >= width - MARGIN) return;
				for (int i = 0; i < actionSpans.size(); i++){
					float[] span = actionSpans.get(i);
					if (y >= span[0] && y < span[1]){
						WndUimsdRow.this.hide();
						actions.get(i).run.run();
						return;
					}
				}
			}
		};
		float h = WindowGeometry.paneHeight(content.height(), WindowGeometry.maxPane(PixelScene.uiCamera.height, pos, MARGIN));
		float total = pos + h + MARGIN;
		//size the window first: it moves the window's camera, and the pane works out its screen position when it is placed
		resize(width, (int) total);
		add(pane);
		pane.setRect(0, pos, width, h);
	}

	//one line of the sheet: a section heading, or a label with its value (coloured by what kind of knowledge it is) and a small note under it
	private static final class SheetLine {
		final boolean section;
		final String label, value, note;
		final int state;
		final boolean small;

		SheetLine( String label, String value, String note, int state, boolean small ){
			this.section = false;
			this.label = label;
			this.value = value;
			this.note = note == null ? "" : note;
			this.state = state;
			this.small = small;
		}

		private SheetLine( String section ){
			this.section = true;
			this.label = section;
			this.value = "";
			this.note = "";
			this.state = CellText.EXACT;
			this.small = false;
		}

		static SheetLine section( String text ){
			return new SheetLine(text);
		}
	}

	//lays the sheet out in two columns (labels grey on the left, values on the right) and returns its height
	private static float sheet( Component into, int width, List<SheetLine> lines ){
		float labelW = (width - 3 * MARGIN) * 0.42f, valueW = (width - 3 * MARGIN) - labelW;
		float y = MARGIN;
		for (SheetLine l : lines){
			if (l.section){
				y += 3;
				RenderedTextBlock t = PixelScene.renderTextBlock(l.label.toUpperCase(), UimsdStyle.SIZE_HELP);
				t.hardlight(UimsdStyle.HEADING);
				t.maxWidth(width - 2 * MARGIN);
				t.setPos(MARGIN, y);
				into.add(t);
				y = t.bottom() + 1;
				ColorBlock rule = new ColorBlock(width - 2 * MARGIN, 1, UimsdStyle.RULE);
				rule.x = MARGIN;
				rule.y = y;
				into.add(rule);
				y += 3;
				continue;
			}
			RenderedTextBlock label = PixelScene.renderTextBlock(l.label, UimsdStyle.SIZE_HELP);
			label.hardlight(UimsdStyle.MUTED);
			label.maxWidth((int) labelW);
			label.setPos(MARGIN, y);
			into.add(label);
			int color = UimsdTable.color(l.state);
			RenderedTextBlock value = PixelScene.renderTextBlock(l.value, UimsdStyle.SIZE_HELP);
			value.hardlight(l.small ? UimsdTable.dim(color) : color);
			value.maxWidth((int) valueW);
			value.setPos(2 * MARGIN + labelW, y);
			into.add(value);
			float h = Math.max(label.height(), value.height());
			if (!l.note.isEmpty()){
				RenderedTextBlock note = PixelScene.renderTextBlock(l.note, UimsdStyle.SIZE_META);
				note.hardlight(UimsdStyle.FAINT);
				note.maxWidth((int) valueW);
				note.setPos(2 * MARGIN + labelW, y + value.height() + 1);
				into.add(note);
				h = Math.max(label.height(), value.height() + 1 + note.height());
			}
			y += h + 3;
		}
		return y + 2;
	}

	private static final class Action {
		final String label;
		final Runnable run;

		Action( String label, Runnable run ){
			this.label = label;
			this.run = run;
		}
	}

	@Override
	public Runnable restoreOnSceneReset(){
		final UimsdModel m = model;
		final ResultTable.Line line = selected;
		return () -> show(m, line);
	}

	public static void show( UimsdModel m, ResultTable.Line line ){
		ShatteredPixelDungeon.scene().addToFront(new WndUimsdRow(m, line));
	}
}
