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

import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdGuide;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

//APD: one topic of the UIMSD Guide: a title, a scrolling explanation, and buttons to be shown it on the real screen or to close.
public class WndUimsdTopic extends Window {

	private final UimsdGuide.Topic savedTopic;
	private final boolean savedCanShow;
	private final Runnable savedShow;
	private final ScrollPane pane;
	private static final int MARGIN = 2;
	private static final int BUTTON_H = 16;

	public WndUimsdTopic( final UimsdGuide.Topic topic, boolean canShow, final Runnable show ){
		super();
		savedTopic = topic; savedCanShow = canShow; savedShow = show;
		int width = WndUimsdList.windowWidth();
		RenderedTextBlock tt = PixelScene.renderTextBlock(topic.title, 9);
		tt.hardlight(TITLE_COLOR);
		tt.maxWidth(width - 2 * MARGIN);
		tt.setPos(MARGIN, MARGIN);
		add(tt);
		float pos = tt.bottom() + 2 * MARGIN;

		Component content = new Component();
		content.setSize(width, UimsdText.layout(content, width, topic.text));
		pane = new ScrollPane(content);
		float h = WindowGeometry.paneHeight(content.height(), WindowGeometry.maxPane(PixelScene.uiCamera.height, pos, BUTTON_H + 2 * MARGIN));
		//size the window first: it moves the window's camera, and the pane works out its screen position when it is placed
		resize(width, (int) (pos + h + BUTTON_H + 2 * MARGIN));
		add(pane);
		pane.setRect(0, pos, width, h);

		float by = pos + h + MARGIN;
		if (canShow){
			float half = (width - 3 * MARGIN) / 2f;
			RedButton showBtn = new RedButton("Show me", 7){
				@Override
				protected void onClick(){
					hide();
					show.run();
				}
			};
			showBtn.setRect(MARGIN, by, half, BUTTON_H);
			add(showBtn);
			RedButton close = new RedButton("Close", 7){
				@Override
				protected void onClick(){
					hide();
				}
			};
			close.setRect(2 * MARGIN + half, by, half, BUTTON_H);
			add(close);
		} else {
			RedButton close = new RedButton("Close", 7){
				@Override
				protected void onClick(){
					hide();
				}
			};
			close.setRect(MARGIN, by, width - 2 * MARGIN, BUTTON_H);
			add(close);
		}
	}
	@Override
	public Runnable restoreOnSceneReset(){
		final UimsdGuide.Topic topic = savedTopic;
		final boolean canShow = savedCanShow;
		final Runnable show = savedShow;
		final float scrollX = pane.content().camera.scroll.x, scrollY = pane.content().camera.scroll.y;
		return () -> {
			WndUimsdTopic restored = new WndUimsdTopic(topic, canShow, show);
			restored.pane.scrollTo(scrollX, scrollY);
			com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon.scene().addToFront(restored);
		};
	}

}

