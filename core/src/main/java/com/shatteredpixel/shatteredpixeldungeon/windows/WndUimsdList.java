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

import com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.UimsdStyle;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.NinePatch;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.List;

//APD: the one list window all the UIMSD pickers use (columns, operators, values, views, options and so on). A list is made of ENTRIES:
//section headings and notes (not tappable), and items, each with a label, an optional grey description under it, and a marker for the
//choice that is on now. It is either a menu (tapping an item closes the window and reports it) or a set of switches (tapping toggles in
//place, "Done" closes). Scrolled when there are many, tapped with the same coordinate test the game's own scrolling lists use.
//The index a Picker receives counts ITEMS only (headings and notes do not count).
public class WndUimsdList extends Window {

	public interface Picker {
		void onPick( int index, boolean checked );
	}

	//one line (or small block) of a list
	public static final class Entry {
		public static final int HEADING = 0, ITEM = 1, NOTE = 2;

		public final int kind;
		public final String label;
		public final String detail;
		public boolean current;
		//a note drawn as a warning
		public boolean warn;
		//switches: null for an entry that is not a switch
		public Boolean checked;
		private boolean action;
		private int noteSize;

		private Entry( int kind, String label, String detail ){
			this.kind = kind;
			this.label = label == null ? "" : label;
			this.detail = detail == null ? "" : detail;
		}

		public static Entry heading( String text ){
			return new Entry(HEADING, text, "");
		}

		public static Entry note( String text ){
			return new Entry(NOTE, text, "");
		}

		public static Entry note(String text, int size){
			Entry entry = note(text);
			entry.noteSize = size;
			return entry;
		}

		public static Entry warning( String text ){
			Entry e = new Entry(NOTE, text, "");
			e.warn = true;
			return e;
		}

		public static Entry item( String label ){
			return new Entry(ITEM, label, "");
		}

		public static Entry action(String label){
			Entry entry = item(label);
			entry.action = true;
			return entry;
		}

		public static Entry item( String label, String detail ){
			return new Entry(ITEM, label, detail);
		}

		//marks the choice that is on now
		public Entry current( boolean on ){
			this.current = on;
			return this;
		}

		public Entry checked( boolean on ){
			this.checked = on;
			return this;
		}

		public boolean isItem(){
			return kind == ITEM;
		}
	}

	//plain labels as entries, for the simple pickers
	public static List<Entry> items( List<String> labels ){
		List<Entry> out = new ArrayList<>();
		for (String l : labels) out.add(Entry.item(l));
		return out;
	}

	private static final int MARGIN = 2;

	private final Picker picker;
	private final boolean switches;
	private final List<Entry> entries;
	//per entry: the label text and background (items only), and the span of the row inside the list (items and Done only, else null)
	private final List<RenderedTextBlock> labelTexts = new ArrayList<>();
	private final List<NinePatch> backs = new ArrayList<>();
	private final List<float[]> spans = new ArrayList<>();
	private final List<Integer> itemIndexOfEntry = new ArrayList<>();
	private ListPane pane;

	//menu or switches of plain labels: checks == null makes a menu. One check per label makes switches (and an extra "Done" closes it).
	public WndUimsdList( String title, String message, List<String> labels, List<Boolean> checks, Picker picker ){
		this(title, message, checks != null, withChecks(items(labels), checks), picker);
	}

	private static List<Entry> withChecks( List<Entry> entries, List<Boolean> checks ){
		if (checks != null) for (int i = 0; i < entries.size() && i < checks.size(); i++) entries.get(i).checked(checks.get(i));
		return entries;
	}

	//a menu of entries
	public static WndUimsdList menu( String title, String message, List<Entry> entries, Picker picker ){
		return new WndUimsdList(title, message, false, entries, picker);
	}

	//a set of switches (every item needs checked(...)); Done closes it
	public static WndUimsdList switches( String title, String message, List<Entry> entries, Picker picker ){
		return new WndUimsdList(title, message, true, entries, picker);
	}

	protected WndUimsdList( String title, String message, boolean switches, List<Entry> entries, Picker picker ){
		super();
		this.picker = picker;
		this.switches = switches;
		this.entries = new ArrayList<>(entries);
		int width = windowWidth();

		float pos = MARGIN;
		if (title != null && !title.isEmpty()){
			RenderedTextBlock t = PixelScene.renderTextBlock(title, UimsdStyle.SIZE_TITLE);
			t.hardlight(UimsdStyle.TITLE);
			t.maxWidth(width - 2 * MARGIN);
			t.setPos(MARGIN, pos);
			add(t);
			pos = t.bottom() + MARGIN;
		}
		if (message != null && !message.isEmpty()){
			if (message.length() > 100){
				final String explanation = message;
				StyledButton help = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "About this menu", 6){
					@Override protected void onClick(){
						ShatteredPixelDungeon.scene().addToFront(new WndUimsdText(title == null ? "UIMSD" : title, explanation, false));
					}
				};
				help.setRect(MARGIN,pos,width-2*MARGIN,16);add(help);pos += 18;
			} else {
				RenderedTextBlock m = PixelScene.renderTextBlock(message, UimsdStyle.SIZE_HELP);
				m.hardlight(UimsdStyle.MUTED);m.maxWidth(width-2*MARGIN);m.setPos(MARGIN,pos);add(m);
				pos=m.bottom()+2*MARGIN;
			}
		} else {
			pos += MARGIN;
		}

		//The window is sized BEFORE the list is placed: resizing a window moves its camera, and a scroll pane works out where it is on
		//screen when it is placed, so placing it first leaves its contents offset from the window.
		pane = new ListPane();
		Component content = pane.content();
		int n = this.entries.size() + (switches ? 1 : 0);
		float[] heights = new float[n];
		List<RenderedTextBlock> labelsMade = new ArrayList<>();
		List<RenderedTextBlock> detailsMade = new ArrayList<>();
		int textWidth = WindowGeometry.rowTextWidth(width);
		for (int i = 0; i < n; i++){
			Entry e = i < this.entries.size() ? this.entries.get(i) : null;
			RenderedTextBlock label, detail = null;
			if (e == null){
				label = PixelScene.renderTextBlock("Done", UimsdStyle.SIZE_TEXT);
				heights[i] = WindowGeometry.rowHeight(label.height());
			} else if (e.kind == Entry.HEADING){
				label = PixelScene.renderTextBlock(e.label.toUpperCase(), UimsdStyle.SIZE_HELP);
				heights[i] = label.height() + 6;
			} else if (e.kind == Entry.NOTE){
				label = PixelScene.renderTextBlock(e.label, e.noteSize > 0 ? e.noteSize : UimsdStyle.SIZE_META);
				heights[i] = label.height() + 4;
			} else {
				label = PixelScene.renderTextBlock(itemText(e), UimsdStyle.SIZE_TEXT);
				float h = label.height();
				if (!e.detail.isEmpty()){
					detail = PixelScene.renderTextBlock(e.detail, UimsdStyle.SIZE_META);
					h += 1 + detail.height();
				}
				heights[i] = WindowGeometry.rowHeight(h);
			}
			label.maxWidth(textWidth);
			if (detail != null) detail.maxWidth(textWidth);
			//a text block lays itself out when its width is set, so the heights are taken again after it
			if (e != null && e.kind == Entry.ITEM){
				float h = label.height() + (detail == null ? 0 : 1 + detail.height());
				heights[i] = e.action ? WindowGeometry.actionHeight(h) : WindowGeometry.rowHeight(h);
			} else if (e == null){
				heights[i] = WindowGeometry.rowHeight(label.height());
			} else if (e.kind == Entry.HEADING){
				heights[i] = label.height() + 6;
			} else {
				heights[i] = label.height() + 4;
			}
			labelsMade.add(label);
			detailsMade.add(detail);
		}
		float[] tops = WindowGeometry.stack(heights);
		int itemCount = 0;
		for (int i = 0; i < n; i++){
			Entry e = i < this.entries.size() ? this.entries.get(i) : null;
			RenderedTextBlock label = labelsMade.get(i);
			RenderedTextBlock detail = detailsMade.get(i);
			if (e != null && e.kind == Entry.HEADING){
				label.hardlight(UimsdStyle.HEADING);
				label.setPos(WindowGeometry.ROW_PAD + 1, tops[i] + 3);
				content.add(label);
				ColorBlock rule = new ColorBlock(width - 2 * MARGIN, 1, UimsdStyle.RULE);
				rule.x = MARGIN;
				rule.y = tops[i] + heights[i] - 2;
				content.add(rule);
				labelTexts.add(null);
				backs.add(null);
				spans.add(null);
				itemIndexOfEntry.add(-1);
				continue;
			}
			if (e != null && e.kind == Entry.NOTE){
				label.hardlight(e.warn ? UimsdStyle.WARN : UimsdStyle.MUTED);
				label.setPos(WindowGeometry.ROW_PAD + 1, tops[i] + 2);
				content.add(label);
				labelTexts.add(null);
				backs.add(null);
				spans.add(null);
				itemIndexOfEntry.add(-1);
				continue;
			}
			NinePatch bg = Chrome.get(Chrome.Type.GREY_BUTTON_TR);
			bg.size(width, heights[i] - 1);
			bg.y = tops[i];
			if (e != null && (e.current || Boolean.TRUE.equals(e.checked))) bg.hardlight(0xAADD99);
			content.add(bg);
			float textH = label.height() + (detail == null ? 0 : 1 + detail.height());
			float ty = tops[i] + (heights[i] - 1 - textH) / 2f;
			label.hardlight(e == null ? UimsdStyle.TITLE : colorFor(e));
			label.setPos(WindowGeometry.ROW_PAD + 1, ty);
			content.add(label);
			if (detail != null){
				detail.hardlight(UimsdStyle.MUTED);
				detail.setPos(WindowGeometry.ROW_PAD + 1, ty + label.height() + 1);
				content.add(detail);
			}
			labelTexts.add(label);
			backs.add(bg);
			spans.add(new float[]{tops[i], heights[i]});
			itemIndexOfEntry.add(e == null ? -2 : itemCount++);
		}
		content.setSize(width, tops[n]);
		float h = WindowGeometry.paneHeight(tops[n], WindowGeometry.maxPane(PixelScene.uiCamera.height, pos, 0));
		resize(width, (int) (pos + h));
		add(pane);
		pane.setRect(0, pos, width, h);
	}

	//the width every UIMSD dialog uses on this screen
	public static int windowWidth(){
		return WindowGeometry.width(PixelScene.landscape(), PixelScene.uiCamera.width);
	}

	private String itemText( Entry e ){
		String t = e.label;
		if (e.checked != null) t = (e.checked ? "[x] " : "[ ] ") + t;
		if (e.current && e.checked == null) t += "   (current)";
		return t;
	}

	private int colorFor( Entry e ){
		return e.current || Boolean.TRUE.equals(e.checked) ? UimsdStyle.CURRENT : UimsdStyle.TEXT;
	}

	private class ListPane extends ScrollPane {

		ListPane(){
			super(new Component());
		}

		@Override
		public void onClick( float x, float y ){
			for (int i = 0; i < spans.size(); i++){
				float[] s = spans.get(i);
				if (s == null) continue;
				if (y >= s[0] && y < s[0] + s[1]){
					picked(i);
					return;
				}
			}
		}
	}

	// ---- going back through a chain of menus ----
	//A menu that opens another menu (Add a filter: the column, then the comparison, then the value) stays open underneath, so Back from the second
	//one returns to the first exactly as it was. When the chain ends in a choice, closeAll() closes every menu of it at once.
	private static final List<WndUimsdList> CHAIN = new ArrayList<>();
	private boolean keepOpen;
	private Runnable restorer;

	public WndUimsdList restoreWith(Runnable restorer){
		this.restorer = restorer;
		return this;
	}

	@Override
	public Runnable restoreOnSceneReset(){
		return restorer;
	}


	public WndUimsdList keepOpen(){
		keepOpen = true;
		synchronized (CHAIN){
			if (!CHAIN.contains(this)) CHAIN.add(this);
		}
		return this;
	}

	//closes every menu that was kept open for a chain
	public static void closeAll(){
		List<WndUimsdList> all;
		synchronized (CHAIN){
			all = new ArrayList<>(CHAIN);
			CHAIN.clear();
		}
		for (WndUimsdList w : all){
			try {
				w.hide();
			} catch (RuntimeException ignored){
			}
		}
	}

	@Override
	public void hide(){
		synchronized (CHAIN){
			CHAIN.remove(this);
		}
		super.hide();
	}

	@Override
	public void destroy(){
		synchronized (CHAIN){
			CHAIN.remove(this);
		}
		super.destroy();
	}

	//the Done entry of a list of switches. Back closes the list without it.
	protected void onDone(){
		hide();
	}

	private void picked( int entry ){
		if (entry >= entries.size()){
			onDone();      //Done
			return;
		}
		Entry e = entries.get(entry);
		int item = itemIndexOfEntry.get(entry);
		if (switches && e.checked != null){
			e.checked = !e.checked;
			if (picker != null) picker.onPick(item, e.checked);
			//A constrained picker can change other entries (for example, replacing the oldest split field).
			//Render the final selection, rather than only the row that was tapped.
			for (int i = 0; i < entries.size(); i++){
				Entry choice = entries.get(i);
				if (!choice.isItem() || choice.checked == null) continue;
				labelTexts.get(i).text(itemText(choice));
				labelTexts.get(i).hardlight(colorFor(choice));
				backs.get(i).hardlight(choice.current || Boolean.TRUE.equals(choice.checked) ? 0xAADD99 : 0xFFFFFF);
			}
		} else {
			if (!keepOpen) hide();
			if (picker != null) picker.onPick(item, false);
		}
	}
}
