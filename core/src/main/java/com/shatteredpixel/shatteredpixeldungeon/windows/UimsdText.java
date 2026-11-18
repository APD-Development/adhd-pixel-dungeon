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

import com.shatteredpixel.shatteredpixeldungeon.apd.query.CellText;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.UimsdStyle;
import com.shatteredpixel.shatteredpixeldungeon.ui.UimsdTable;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;

//APD: lays out a block of explanatory text for the UIMSD windows with a little structure, so a long explanation is easy to scan:
//  "# Title"       a section heading (gold, with a rule)
//  "! text"        a warning or an important note (amber)
//  "- text"        a bullet, indented
//  "[exact] text"  a line led by one of the table's value colours: [exact] [lower] [inferred] [unknown] [hidden] [legacy]
//                  (the tag is not shown; the line is drawn in the colour that kind of value has in the tables)
//  anything else   a paragraph. A blank line separates paragraphs.
final class UimsdText {

	private UimsdText(){}

	private static final int MARGIN = 2;

	//adds the text to 'into' and returns its height
		static float layout( Component into, int width, String text ){
		return layout(into, width, text, true);
	}

	static float layout( Component into, int width, String text, boolean structured ){
		if (text == null) text = "";
		float y = MARGIN;
		int w = width - 2 * MARGIN - 2;
		boolean first = true;
		for (String raw : text.split("\n")){
			String line = raw;
			if (line.trim().isEmpty()){
				y += 3;
				continue;
			}
			int color = UimsdStyle.TEXT;
			int size = UimsdStyle.SIZE_HELP;
			float indent = 0;
			boolean heading = false;
			if (structured && line.startsWith("# ")){
				heading = true;
				line = line.substring(2).toUpperCase();
				color = UimsdStyle.HEADING;
			} else if (structured && line.startsWith("! ")){
				line = line.substring(2);
				color = UimsdStyle.WARN;
			} else if (structured && line.startsWith("- ")){
				line = "- " + line.substring(2);
				indent = 3;
			} else {
				int close = structured && line.startsWith("[") ? line.indexOf(']') : -1;
				if (close > 0){
					int state = stateOf(line.substring(1, close));
					if (state >= 0){
						color = UimsdTable.color(state);
						line = line.substring(close + 1).trim();
					}
				}
			}
			if (heading){
				y += first ? 0 : 4;
			}
			RenderedTextBlock t = PixelScene.renderTextBlock(size);
			t.setHightlighting(structured);
			if (!structured){
				final RenderedTextBlock probe = PixelScene.renderTextBlock(size);
				probe.setHightlighting(false);
				try {
					line = com.shatteredpixel.shatteredpixeldungeon.apd.query.LongWordWrap.wrap(line, Math.max(1, (int) (w - indent)),
							part -> { probe.text(part); return probe.width(); });
				} finally { probe.destroy(); }
			}
			t.text(line);
			t.hardlight(color);
			t.maxWidth((int) (w - indent));
			t.setPos(MARGIN + indent, y);
			into.add(t);
			y = t.bottom() + 1;
			if (heading){
				ColorBlock rule = new ColorBlock(width - 2 * MARGIN, 1, UimsdStyle.RULE);
				rule.x = MARGIN;
				rule.y = y;
				into.add(rule);
				y += 3;
			} else {
				y += 1;
			}
			first = false;
		}
		return y + MARGIN;
	}

	private static int stateOf( String tag ){
		switch (tag){
			case "exact": return CellText.EXACT;
			case "lower": return CellText.LOWER;
			case "inferred": return CellText.INFERRED;
			case "unknown": return CellText.UNKNOWN;
			case "hidden": return CellText.HIDDEN;
			case "legacy": return CellText.LEGACY;
			default: return -1;
		}
	}
}
