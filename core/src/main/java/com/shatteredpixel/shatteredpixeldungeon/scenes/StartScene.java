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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import java.util.List;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndGameInProgress;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.RectF;

import java.util.ArrayList;

public class StartScene extends PixelScene {
	
	private static final int SLOT_WIDTH = 120;
	private static final int SLOT_HEIGHT = 22;
	
	@Override
	public void create() {
		super.create();
		
		Badges.loadGlobal();
		Journal.loadGlobal();
		
		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		TitleBackground BG = new TitleBackground(w, h);
		add( BG );

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;
		
		ExitButton btnExit = new ExitButton();
		btnExit.setPos( insets.left + w - btnExit.width(), insets.top );
		add( btnExit );
		
		IconTitle title = new IconTitle( Icons.ENTER.get(), Messages.get(this, "title"));
		title.setSize(200, 0);
		title.setPos(
				insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f
		);
		align(title);
		add(title);
		
        // Six class choices are always visible; each opens a scrollable list
        // of exactly twenty entries. The existing SaveSlotButton visuals remain
        // unchanged elsewhere, but an unbounded stack of 120 is never rendered.
        float usable = Math.max(0f, insets.top + h - title.bottom() - 15f);
        float spacing = 2f;
        float buttonHeight = Math.min(24f, (usable - 5 * spacing) / 6f);
        float buttonWidth = Math.min(155f, w - 8f);
        float x = insets.left + (w - buttonWidth)/2f;
        float top = title.bottom() + Math.max(3f, (usable - 6*buttonHeight - 5*spacing)/2f);
        int n = 0;
        for (final HeroClass cls : HeroClass.values()) {
            int count = GamesInProgress.activeCount(cls);
            StyledButton classButton = new StyledButton(Chrome.Type.TOAST_TR,
                    Messages.titleCase(cls.title()) + "  " + count + "/20", 8) {
                @Override protected void onClick() {
                    super.onClick();
                    showClassSaves(cls);
                }
            };
            classButton.icon(Icons.get(cls));
            classButton.setRect(x, top + n*(buttonHeight+spacing), buttonWidth, buttonHeight);
            classButton.leftJustify = true;
            align(classButton);
            add(classButton);
            n++;
        }
        GamesInProgress.curSlot = 0;

		fadeIn();
		
	}

    /** Existing and empty slots are visually distinct, and the list scrolls
     *  on narrow portrait devices without shrinking text into unreadability. */
    private static void showClassSaves(final HeroClass cls) {
        final List<Integer> slots = GamesInProgress.slotsFor(cls);
        final List<WndUimsdList.Entry> rows = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            GamesInProgress.Info game = GamesInProgress.check(slot);
            String prefix = "Run " + (i+1) + "  ";
            if (game == null && GamesInProgress.gameExists(slot)) {
                rows.add(WndUimsdList.Entry.item(prefix + "Save needs inspection",
                        "Existing save file preserved; slot is not available for a new run"));
            } else if (game == null) {
                rows.add(WndUimsdList.Entry.item(prefix + "Empty", "Start a new " + cls.title() + " run"));
            } else {
                String diff = game.difficultyKnown ? Integer.toString(game.difficulty) : "?";
                String rules = !game.customRulesKnown ? "rules ?" : game.customRulesChanged ? "custom rules" : "normal rules";
                int challenges = Integer.bitCount(game.challenges);
                long days = Math.max(0L, (Game.realTime - game.lastPlayed) / 86400000L);
                String age = game.lastPlayed <= 0 ? "last played ?" : days == 0 ? "played today" : "played " + days + "d ago";
                rows.add(WndUimsdList.Entry.item(prefix + "Floor " + game.depth + "  Lv " + game.level,
                        "Difficulty " + diff + " | " + challenges + " challenges | " + rules + " | " + age));
            }
        }
        ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu(
                Messages.titleCase(cls.title()) + "  " + GamesInProgress.activeCount(cls) + "/20",
                "Choose a saved run to continue, or an empty slot to begin.",
                rows, (index, checked) -> {
                    if (index < 0 || index >= slots.size()) return;
                    int slot = slots.get(index);
                    GamesInProgress.Info existing = GamesInProgress.check(slot);
                    if (existing != null) {
                        ShatteredPixelDungeon.scene().addToFront(new WndGameInProgress(slot));
                    } else if (GamesInProgress.gameExists(slot)) {
                        ShatteredPixelDungeon.scene().addToFront(new WndMessage(
                                "This save is unreadable or from an incompatible version. It has not been deleted or replaced."));
                    } else if (GamesInProgress.isAvailableNewSlot(cls, slot)) {
                        GamesInProgress.selectedClass = cls;
                        GamesInProgress.curSlot = slot;
                        ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
                    } else {
                        ShatteredPixelDungeon.scene().addToFront(new WndMessage(
                                "This class already has 20 active runs."));
                    }
                }));
    }

	@Override
	protected void onBackPressed() {
		ShatteredPixelDungeon.switchNoFade( TitleScene.class );
	}
	
	private static class SaveSlotButton extends Button {
		
		private NinePatch bg;
		
		private Image hero;
		private RenderedTextBlock name;
		private RenderedTextBlock lastPlayed;
		
		private Image steps;
		private BitmapText depth;
		private Image classIcon;
		private BitmapText level;
		
		private int slot;
		private boolean newGame;
		
		@Override
		protected void createChildren() {
			super.createChildren();
			
			bg = Chrome.get(Chrome.Type.TOAST_TR);
			add( bg );
			
			name = PixelScene.renderTextBlock(9);
			add(name);

			lastPlayed = PixelScene.renderTextBlock(6);
			add(lastPlayed);
		}
		
		public void set( int slot ){
			this.slot = slot;
			GamesInProgress.Info info = GamesInProgress.check(slot);
			newGame = info == null;
			if (newGame){
				name.text( Messages.get(StartScene.class, "new"));
				
				if (hero != null){
					remove(hero);
					hero = null;
					remove(steps);
					steps = null;
					remove(depth);
					depth = null;
					remove(classIcon);
					classIcon = null;
					remove(level);
					level = null;
				}
			} else {
				
				if (info.subClass != HeroSubClass.NONE){
					name.text(Messages.titleCase(info.subClass.title()));
				} else {
					name.text(Messages.titleCase(info.heroClass.title()));
				}
				
				if (hero == null){
					hero = new Image(info.heroClass.spritesheet(), 0, 15*info.armorTier, 12, 15);
					add(hero);
					
					steps = new Image(Icons.get(Icons.STAIRS));
					add(steps);
					depth = new BitmapText(PixelScene.pixelFont);
					add(depth);
					
					classIcon = new Image(Icons.get(info.heroClass));
					add(classIcon);
					level = new BitmapText(PixelScene.pixelFont);
					add(level);
				} else {
					hero.copy(new Image(info.heroClass.spritesheet(), 0, 15*info.armorTier, 12, 15));
					
					classIcon.copy(Icons.get(info.heroClass));
				}

				long diff = Game.realTime - info.lastPlayed;
				if (diff > 99L * 30 * 24 * 60 * 60_000){
					lastPlayed.text(" "); //show no text for >99 months ago
				} else if (diff < 60_000){
					lastPlayed.text(Messages.get(StartScene.class, "one_minute_ago"));
				} else if (diff < 2 * 60 * 60_000){
					lastPlayed.text(Messages.get(StartScene.class, "minutes_ago", diff / 60_000));
				} else if (diff < 2 * 24 * 60 * 60_000){
					lastPlayed.text(Messages.get(StartScene.class, "hours_ago", diff / (60 * 60_000)));
				} else if (diff < 2L * 30 * 24 * 60 * 60_000){
					lastPlayed.text(Messages.get(StartScene.class, "days_ago", diff / (24 * 60 * 60_000)));
				} else {
					lastPlayed.text(Messages.get(StartScene.class, "months_ago", diff / (30L * 24 * 60 * 60_000)));
				}
				
				depth.text(Integer.toString(info.depth));
				depth.measure();
				
				level.text(Integer.toString(info.level));
				level.measure();
				
				if (info.challenges > 0){
					name.hardlight(Window.TITLE_COLOR);
					lastPlayed.hardlight(Window.TITLE_COLOR);
					depth.hardlight(Window.TITLE_COLOR);
					level.hardlight(Window.TITLE_COLOR);
				} else {
					name.resetColor();
					lastPlayed.resetColor();
					depth.resetColor();
					level.resetColor();
				}

				if (info.daily){
					if (info.dailyReplay){
						steps.hardlight(1f, 0.5f, 2f);
					} else {
						steps.hardlight(0.5f, 1f, 2f);
					}
				} else if (!info.customSeed.isEmpty()){
					steps.hardlight(1f, 1.5f, 0.67f);
				}
				
			}
			
			layout();
		}
		
		@Override
		protected void layout() {
			super.layout();
			
			bg.x = x;
			bg.y = y;
			bg.size( width, height );
			
			if (hero != null){
				hero.x = x+8;
				hero.y = y + (height - hero.height())/2f;
				align(hero);
				
				name.setPos(
						hero.x + hero.width() + 6,
						y + (height - name.height() - lastPlayed.height() - 2)/2f
				);
				align(name);

				lastPlayed.setPos(
						hero.x + hero.width() + 6,
						name.bottom()+2
				);
				
				classIcon.x = x + width - 24 + (16 - classIcon.width())/2f;
				classIcon.y = y + (height - classIcon.height())/2f;
				align(classIcon);
				
				level.x = classIcon.x + (classIcon.width() - level.width()) / 2f;
				level.y = classIcon.y + (classIcon.height() - level.height()) / 2f + 1;
				align(level);
				
				steps.x = x + width - 40 + (16 - steps.width())/2f;
				steps.y = y + (height - steps.height())/2f;
				align(steps);
				
				depth.x = steps.x + (steps.width() - depth.width()) / 2f;
				depth.y = steps.y + (steps.height() - depth.height()) / 2f + 1;
				align(depth);
				
			} else {
				name.setPos(
						x + (width - name.width())/2f,
						y + (height - name.height())/2f
				);
				align(name);
			}
			
			
		}
		
		@Override
		protected void onClick() {
			if (newGame) {
				GamesInProgress.selectedClass = null;
				GamesInProgress.curSlot = slot;
				ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
			} else {
				ShatteredPixelDungeon.scene().add( new WndGameInProgress(slot));
			}
		}
	}
}
