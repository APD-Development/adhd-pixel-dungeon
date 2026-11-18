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

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.Counter;
import com.shatteredpixel.shatteredpixeldungeon.apd.humor.Location;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

// APD Settings > About / Developer > The Grudge: how often (frequency) and where (five switches) the developer may interrupt with a
// Cleric remark, the humor setting, and the four counters. Opening it never triggers a popup; the first time, one small joke is shown.
public class WndClericGrudge extends Window {

	private static final int WIDTH = 120;
	private static final int MARGIN = 3;
	private static final int BTN = 16;

	// The surprise popup: the line, a plain acknowledgement, and an immediate, saved way to stop all surprises.
	static Window popup( String text ){
		return new WndOptions( "DEVELOPER NOTE", text, "Fine.", "Turn off these messages" ) {
			@Override
			protected void onSelect( int index ){
				if (index == 1) GrudgeHooks.turnOff();
			}
		};
	}

	// The one-time warning when a Cleric run is started: one button, and the run starts however it is closed (button or Back).
	static Window firstRun( String text, final Runnable proceed ){
		return new WndOptions( "A WORD ABOUT CLERIC", text, "Fine." ) {
			private boolean started;

			private void go(){
				if (started) return;
				started = true;
				proceed.run();
			}

			@Override
			protected void onSelect( int index ){
				go();
			}

			@Override
			public void onBackPressed(){
				super.onBackPressed();
				go();
			}
		};
	}

	private RedButton btnFrequency;

	public WndClericGrudge(){
		super();
		RenderedTextBlock title = PixelScene.renderTextBlock( "THE GRUDGE" + (com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.testBuild() ? "\n(PRIVATE TEST BUILD)" : ""), 9 );
		title.hardlight( 0xFFFF44 );
		title.maxWidth( WIDTH - 2 * MARGIN );
		title.setPos( MARGIN, MARGIN );
		add( title );
		float top = title.bottom() + 2 * MARGIN;

		Component content = new Component();
		// Create the scrolling touch area BEFORE any controls, so its broad hit target
		// cannot swallow button and checkbox taps. Same ordering as WndAPDSettings.
		ScrollPane pane = new ScrollPane( content );
		float pos = 0;
		int w = WIDTH - 2 * MARGIN;

		Counter c = GrudgeHooks.counter();
		//only what really happened to this player: how many different remarks they have been shown (never a made-up history, never how many exist)
		String[] counters = { c.heardText(), c.appreciationHeardText() };
		for (String text : counters){
			RenderedTextBlock t = PixelScene.renderTextBlock( text, 6 );
			t.maxWidth( w );
			t.setPos( 0, pos );
			content.add( t );
			pos = t.bottom() + MARGIN;
		}

		String joke = GrudgeHooks.discovery();
		if (joke != null){
			RenderedTextBlock j = PixelScene.renderTextBlock( joke, 5 );
			j.maxWidth( w );
			j.setPos( 0, pos );
			content.add( j );
			pos = j.bottom() + MARGIN;
		}

		btnFrequency = new RedButton( frequencyLabel() ){
			@Override
			protected void onClick(){
				GrudgeHooks.setFrequency( GrudgeHooks.frequency().next() );
				text( frequencyLabel() );
			}
		};
		btnFrequency.setRect( 0, pos, w, BTN );
		content.add( btnFrequency );
		pos = btnFrequency.bottom() + MARGIN;

		RenderedTextBlock where = PixelScene.renderTextBlock( "Where the messages may appear:", 5 );
		where.maxWidth( w );
		where.setPos( 0, pos );
		content.add( where );
		pos = where.bottom() + MARGIN;

		for (final Location loc : Location.values()){
			CheckBox box = new CheckBox( loc.label ){
				@Override
				protected void onClick(){
					super.onClick();
					GrudgeHooks.prefs().setEnabled( loc, checked() );
				}
			};
			box.checked( GrudgeHooks.prefs().enabled( loc ) );
			box.setRect( 0, pos, w, BTN );
			content.add( box );
			pos = box.bottom() + 1;
		}
		pos += MARGIN;

		//the one shared humor preference lives in APD Settings only (age is never inferred); this page just says what it is
		RenderedTextBlock humor = PixelScene.renderTextBlock( "Humor: " + (SPDSettings.apdAdultHumor() ? "Adult" : "Clean") + ". Change it in APD Settings.", 5 );
		humor.maxWidth( w );
		humor.setPos( 0, pos );
		content.add( humor );
		pos = humor.bottom() + MARGIN;

		RedButton reset = new RedButton( "Forget what I've heard" ){
			@Override
			protected void onClick(){
				ShatteredPixelDungeon.scene().addToFront( new WndOptions( "FORGET EVERYTHING?",
						"This clears the list of messages you have heard, so they can all come back. It cannot be undone.",
						"Forget it", "Keep it" ){
					@Override
					protected void onSelect( int index ){
						if (index == 0){
							GrudgeHooks.resetHistory();
							WndClericGrudge.this.hide();
						}
					}
				} );
			}
		};
		reset.setRect( 0, pos, w, BTN );
		content.add( reset );
		pos = reset.bottom();

		content.setSize( w, pos );
		float h = WindowGeometry.paneHeight( content.height(), WindowGeometry.maxPane( PixelScene.uiCamera.height, top, 0 ) );
		//size the window first: it moves the window's camera, and the pane works out its screen position when it is placed
		resize( WIDTH, (int)(top + h + MARGIN) );
		add( pane );
		pane.setRect( MARGIN, top, w, h );
		pane.allowDragFromButtons();
	}

	private static String frequencyLabel(){
		return "Surprise messages: " + GrudgeHooks.frequency().label;
	}
}
