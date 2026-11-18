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


package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.ResultTable;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.TableGeometry;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.TableViewport;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.input.PointerEvent;
import com.shatteredpixel.shatteredpixeldungeon.ui.UimsdStyle;
import com.watabou.input.ScrollEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.NinePatch;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.watabou.noosa.ui.Component;

//APD: the table of a UIMSD screen. It draws whatever ResultTable it is given, for any dataset: it only knows headers, lines and
//cell states. The first column stays put while the rest scrolls sideways, the header row stays put while the rows scroll, and
//tapping a header sorts, tapping a row opens it. Cell colours show what kind of knowledge each value is.
public class UimsdTable extends Component {

	public interface Listener {
		void onSort( String columnId );
		void onRow( ResultTable.Line line );
	}

	//colour of a cell by what is known about it
	public static int color( int state ){
		switch (state){
			case CellText.LOWER: return 0xFFB347;      //at least this much
			case CellText.INFERRED: return 0xFFE066;   //inferred, not exact
			case CellText.LEGACY: return 0x77CCFF;     //from an imported ranking record
			case CellText.UNKNOWN: return 0x888888;
			case CellText.HIDDEN: return 0x9A7AAA;
			default: return 0xFFFFFF;
		}
	}

	//the colour of a value that rests on very few known values: the same colour, clearly fainter
	public static int dim( int rgb ){
		int r = ((rgb >> 16) & 0xFF) * 55 / 100, g = ((rgb >> 8) & 0xFF) * 55 / 100, b = (rgb & 0xFF) * 55 / 100;
		return (r << 16) | (g << 8) | b;
	}

	private static final int HEADER_COLOR = 0xFFFF88;
	private static final int STRIPE = 0x14FFFFFF;

	private Listener listener;
	private ResultTable table;
	private TableGeometry geo;
	private float builtWidth = -1;
	private boolean dirty = true;

	private ClickPane corner, header, firstCol;
	private BodyPane body;
	//the row last tapped stays marked while its details are open
	private int selected = -1;
	private ColorBlock selFirst, selBody;
	//soft edges that show the table goes on sideways
	private ColorBlock moreRight, moreLeft;

	public UimsdTable(){
		super();
	}

	public void setListener( Listener l ){
		listener = l;
	}

	public void setTable( ResultTable t ){
		TableViewport previous=captureViewport();
		table = t;
		geo = t == null ? null : TableGeometry.of(t);
		dirty = true;
		layout();
		restoreViewport(previous);
	}

	public TableViewport captureViewport(){
		return body==null || table==null ? null : TableViewport.capture(table,
				body.content().camera.scroll.x, body.content().camera.scroll.y);
	}

	public void restoreViewport(TableViewport viewport){
		if(viewport==null || table==null || body==null)return;
		float[] at=viewport.position(table,body.width(),body.height());
		body.scrollTo(at[0],at[1]);
		header.scrollTo(at[0],0);
		firstCol.scrollTo(0,at[1]);
	}

	@Override
	protected void createChildren(){
		body = new BodyPane();
		add(body);
		firstCol = new ClickPane(){
			@Override
			public void onClick( float cx, float cy ){
				rowTapped(cy);
			}
		};
		add(firstCol);
		header = new ClickPane(){
			@Override
			public void onClick( float cx, float cy ){
				headerTapped(cx + (firstWidth()));
			}
		};
		add(header);
		corner = new ClickPane(){
			@Override
			public void onClick( float cx, float cy ){
				headerTapped(cx);
			}
		};
		add(corner);
		moreRight = new ColorBlock(3, 1, 0x66FFE066);
		moreLeft = new ColorBlock(3, 1, 0x66FFE066);
		moreRight.visible = moreLeft.visible = false;
		add(moreRight);
		add(moreLeft);
	}

	private float firstWidth(){
		return geo == null ? 0 : Math.min(geo.firstWidth(), width * 0.5f);
	}

	private void headerTapped( float tableX ){
		if (geo == null || table == null || listener == null) return;
		float first = firstWidth();
		int col;
		if (tableX < first) col = 0;
		else {
			//the rest of the columns, measured from the left edge of the table as built at this width
			col = geo.columnAt(tableX - first + geo.firstWidth());
		}
		if (col >= 0 && col < table.headers.size() && table.headers.get(col).sortable) listener.onSort(table.headers.get(col).id);
	}

	private void rowTapped( float contentY ){
		if (geo == null || table == null || listener == null) return;
		int r = geo.rowAt(contentY);
		if (r >= 0 && r < table.lines.size()){
			select(r);
			listener.onRow(table.lines.get(r));
		}
	}

	private void select( int r ){
		selected = r;
		if (selFirst != null) selFirst.killAndErase();
		if (selBody != null) selBody.killAndErase();
		selFirst = selBody = null;
		if (r < 0 || geo == null) return;
		float rowH = TableGeometry.ROW_HEIGHT;
		selFirst = new ColorBlock(firstWidth(), rowH, UimsdStyle.ROW_SELECTED);
		selFirst.y = r * rowH;
		firstCol.content().add(selFirst);
		selBody = new ColorBlock(geo.scrollingWidth(), rowH, UimsdStyle.ROW_SELECTED);
		selBody.y = r * rowH;
		body.content().add(selBody);
	}

	private class BodyPane extends ScrollPane {

		BodyPane(){
			super(new UimsdContent());
		}

		@Override
		public void onClick( float x, float y ){
			rowTapped(y);
		}
	}

	//a pane that scrolls only when told to and only reports taps (the table scrolls the body and moves these along)
	private static class ClickPane extends ScrollPane {

		ClickPane(){
			super(new UimsdContent());
		}

		@Override
		protected void createChildren(){
			controller = new ClickController();
			add(controller);
			thumb = new ColorBlock(1, 1, THUMB_COLOR);
			thumb.am = 0;
			add(thumb);
		}

		private class ClickController extends PointerController {
			@Override
			protected void onScroll( ScrollEvent event ){
			}

			@Override
			protected void onDrag( PointerEvent event ){
			}
		}
	}

	@Override
	public synchronized void update(){
		super.update();
		if (body != null && geo != null){
			float sx = body.content().camera.scroll.x, sy = body.content().camera.scroll.y;
			header.scrollTo(sx, 0);
			firstCol.scrollTo(0, sy);
			//an edge mark on the side where more columns wait
			float room = body.width();
			float more = geo.scrollingWidth() - room;
			boolean right = more > 1 && sx < more - 1, left = more > 1 && sx > 1;
			moreRight.visible = right;
			moreLeft.visible = left;
			moreRight.size(3, body.height() + TableGeometry.HEADER_HEIGHT);
			moreRight.x = x + width - 3;
			moreRight.y = y;
			moreLeft.size(3, body.height() + TableGeometry.HEADER_HEIGHT);
			moreLeft.x = x + firstWidth();
			moreLeft.y = y;
		}
	}

	@Override
	protected void layout(){
		if (body == null) return;
		if (geo == null || table == null || width <= 0 || height <= 0){
			if (dirty || builtWidth != width) clearContents();
			return;
		}
		float first = firstWidth();
		float scrolling = geo.scrollingWidth();
		float headerH = TableGeometry.HEADER_HEIGHT;
		corner.setRect(x, y, first, headerH);
		header.setRect(x + first, y, Math.max(0, width - first), headerH);
		firstCol.setRect(x, y + headerH, first, Math.max(0, height - headerH));
		body.setRect(x + first, y + headerH, Math.max(0, width - first), Math.max(0, height - headerH));
		if (dirty || builtWidth != width){
			TableViewport previous=captureViewport();
			rebuild(first, scrolling);
			dirty = false;
			builtWidth = width;
			restoreViewport(previous);
		}
	}

	private void clearContents(){
		selFirst = selBody = null;
		selected = -1;
		for (ScrollPane p : new ScrollPane[]{corner, header, firstCol, body}) p.content().clear();
	}

	private void rebuild( float first, float scrolling ){
		clearContents();
		float rowH = TableGeometry.ROW_HEIGHT, headerH = TableGeometry.HEADER_HEIGHT;
		float bodyH = geo.bodyHeight();
		corner.content().setSize(first, headerH);
		header.content().setSize(scrolling, headerH);
		firstCol.content().setSize(first, bodyH);
		body.content().setSize(scrolling, bodyH);

		//header cells
		for (int c = 0; c < table.headers.size(); c++){
			ResultTable.Header h = table.headers.get(c);
			Component target = c == 0 ? corner.content() : header.content();
			float left = c == 0 ? 0 : geo.lefts[c] - geo.firstWidth();
			float w = c == 0 ? first : geo.widths[c];
			NinePatch background = Chrome.get(Chrome.Type.GREY_BUTTON_TR);
			background.size(w,headerH);background.x=left;target.add(background);
			int chars = Math.max(3, (int) ((w - TableGeometry.PAD) / TableGeometry.CHAR_WIDTH));
			String[] lines = TableGeometry.twoLines(TableViewport.heading(h), chars);
			int labelColor = h.sortDirection == 0 ? HEADER_COLOR : 0xFFFFFF;
			RenderedTextBlock label = PixelScene.renderTextBlock(lines[0], 6);
			label.hardlight(labelColor);
			label.setPos(left + 2, 2);
			target.add(label);
			if (!lines[1].isEmpty()){
				RenderedTextBlock label2 = PixelScene.renderTextBlock(lines[1], 6);
				label2.hardlight(labelColor);
				label2.setPos(left + 2, 9.5f);
				target.add(label2);
			}
			if (!h.coverage.isEmpty()){
				RenderedTextBlock cov = PixelScene.renderTextBlock(TableGeometry.fit(h.coverage, chars), 5);
				cov.hardlight(0x999999);
				cov.setPos(left + 2, 19.5f);
				target.add(cov);
			}
			ColorBlock line = new ColorBlock(1, headerH, 0x33FFFFFF);
			line.x = left + w - 1;
			target.add(line);
		}
		ColorBlock rule = new ColorBlock(Math.max(first, scrolling), 1, 0x66FFFFFF);
		rule.y = headerH - 1;
		corner.content().add(rule);
		ColorBlock rule2 = new ColorBlock(scrolling, 1, 0x66FFFFFF);
		rule2.y = headerH - 1;
		header.content().add(rule2);

		//rows
		for (int r = 0; r < table.lines.size(); r++){
			ResultTable.Line line = table.lines.get(r);
			if (r % 2 == 1){
				ColorBlock s1 = new ColorBlock(first, rowH, STRIPE);
				s1.y = r * rowH;
				firstCol.content().add(s1);
				ColorBlock s2 = new ColorBlock(scrolling, rowH, STRIPE);
				s2.y = r * rowH;
				body.content().add(s2);
			}
			for (int c = 0; c < line.cells.length; c++){
				CellText cell = line.cells[c];
				float w = c == 0 ? first : geo.widths[c];
				float left = c == 0 ? 0 : geo.lefts[c] - geo.firstWidth();
				int chars = Math.max(3, (int) ((w - TableGeometry.PAD) / TableGeometry.CHAR_WIDTH));
				RenderedTextBlock t = PixelScene.renderTextBlock(TableGeometry.fit(cell.text, chars), 6);
				t.hardlight(cell.small ? dim(color(cell.state)) : color(cell.state));
				t.setPos(table.headers.get(c).numeric ? left + w - 2 - t.width() : left + 2, r * rowH + 2);
				(c == 0 ? firstCol.content() : body.content()).add(t);
			}
		}
	}
}

