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

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.Difficulty;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.noosa.ui.Component;

//APD: the difficulty of the run you are about to start, on the character screen where the run is set up: one touch each for 1 to 5, and the
//level written out ("3 - Normal"). It only sets what the NEXT run starts with (SPDSettings.apdDifficulty); a run that has begun keeps the
//difficulty it started with (Dungeon.difficulty, saved with the run), so this never touches a run in progress. 3 is the normal game.
public class DifficultyBar extends Component {

	public static final float HEIGHT = 28f;

	private static final float GAP = 1f;

	private StyledButton label;
	private Segment[] segments;

	//what each level is called
	public static String nameOf( int level ){
		switch (level){
			case 1: return "Much easier";
			case 2: return "Easier";
			case 3: return "Normal";
			case 4: return "Harder";
			case 5: return "Much harder";
			default: return "";
		}
	}

	public static String describe(){
		return "Difficulty sets how tough the dungeon is for the run you are about to start. 3 is the normal game. Lower levels: weaker enemies "
				+ "and more loot. Higher levels: tougher enemies and less loot.\n\nThe difficulty is fixed when the run begins: changing it later never "
				+ "changes a run that is already going, and the run is recorded with the difficulty it was started on.";
	}

	@Override
	protected void createChildren(){
		label = new StyledButton(Chrome.Type.BLANK, "", 6){
			@Override
			protected void onClick(){
				ShatteredPixelDungeon.scene().addToFront(new WndMessage(describe()));
			}
		};
		add(label);

		segments = new Segment[Difficulty.MAX - Difficulty.MIN + 1];
		for (int i = 0; i < segments.length; i++){
			final int level = Difficulty.MIN + i;
			segments[i] = new Segment(level){
				@Override
				protected void onClick(){
					super.onClick();
					SPDSettings.apdDifficulty(level);
					refresh();
				}
			};
			add(segments[i]);
		}
		refresh();
	}

	//the level chosen for the next run
	public int level(){
		return SPDSettings.apdDifficulty();
	}

	public void refresh(){
		int level = SPDSettings.apdDifficulty();
		label.text("Difficulty " + level + ": " + nameOf(level));
		for (Segment s : segments) s.setSelected(s.level == level);
	}

	// Fade the complete composite with the rest of the character-select menu.
	// Disable the touch targets only once the controls have fully disappeared.
	public void fade(float alpha){
		boolean enabled = alpha > 0f;
		label.enable(enabled);
		label.alpha(alpha);
		for (Segment segment : segments){
			segment.enable(enabled);
			segment.alpha(alpha);
		}
	}

	@Override
	protected void layout(){
		float labelH = 11f;
		label.setRect(x, y, width, labelH);
		float each = (width - GAP * (segments.length - 1)) / segments.length;
		float top = y + labelH + 2f;
		for (int i = 0; i < segments.length; i++){
			segments[i].setRect(x + i * (each + GAP), top, each, Math.max(8f, height - labelH - 2f));
		}
	}

	//one of the five touch targets
	public static class Segment extends StyledButton {

		public final int level;
		private boolean selected;

		Segment( int level ){
			super(Chrome.Type.GREY_BUTTON_TR, Integer.toString(level), 8);
			this.level = level;
		}

		void setSelected( boolean on ){
			selected = on;
			textColor(on ? 0xFFFF44 : 0xBBBBBB);
			if (on) bg.hardlight(1.3f, 1.2f, 0.7f); else bg.resetColor();
		}

		@Override
		protected void onPointerUp(){
			super.onPointerUp();
			if (selected) bg.hardlight(1.3f, 1.2f, 0.7f);
		}
	}
}
