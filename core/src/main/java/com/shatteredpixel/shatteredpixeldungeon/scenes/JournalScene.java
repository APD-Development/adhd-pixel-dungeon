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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.TerrainFeaturesTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndJournal;
import com.watabou.noosa.Camera;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;

public class JournalScene extends PixelScene {

	public static final int WIDTH_P     = 126;
	public static final int WIDTH_L     = 216;

	public static int lastIDX = 0;
	private NinePatch journalPanel;
	private com.watabou.noosa.ui.Component currentTab;
	private final StyledButton[] tabButtons = new StyledButton[4];

	private void showSelectedTab(){
		if(currentTab != null){
			remove(currentTab);
			currentTab.destroy();
		}
		switch(lastIDX){
			case 1: currentTab = new WndJournal.CatalogTab(); break;
			case 2: currentTab = new WndJournal.GuideTab(); break;
			case 3: currentTab = new WndJournal.AlchemyTab(); break;
			default: currentTab = new WndJournal.BadgesTab(); break;
		}
		add(currentTab);
		currentTab.setRect(journalPanel.x + journalPanel.marginLeft(),
				journalPanel.y + journalPanel.marginTop(),
				journalPanel.width() - journalPanel.marginHor(),
				journalPanel.height() - journalPanel.marginVer());
		if(currentTab instanceof WndJournal.CatalogTab)
			((WndJournal.CatalogTab)currentTab).updateList();
		else if(currentTab instanceof WndJournal.GuideTab)
			((WndJournal.GuideTab)currentTab).updateList();
	}

	private void selectTab(int index){
		if(index < 0 || index >= tabButtons.length || index == lastIDX) return;
		lastIDX = index;
		showSelectedTab();
		for(int i=0;i<tabButtons.length;i++){
			StyledButton button = tabButtons[i];
			if(button==null)continue;
			button.setRect(button.left(),button.top(),button.width(),i == index ? 25 : 20);
			button.icon().brightness(i == index ? 1f : 0.6f);
			align(button);
		}
	}


	@Override
	public void create() {

		super.create();

		Dungeon.hero = null;
		Badges.loadGlobal();
		Journal.loadGlobal();

		Potion.clearColors();
		Scroll.clearLabels();
		Ring.clearGems();

		//need to re-initialize the texture here, as it may be invalid
		new TerrainFeaturesTilemap(new SparseArray<>(), new SparseArray<>());

		Music.INSTANCE.playTracks(
				new String[]{Assets.Music.THEME_1, Assets.Music.THEME_2},
				new float[]{1, 1},
				false);

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;

		RectF insets = getCommonInsets();

		TitleBackground BG = new TitleBackground(w, h);
		//BG added later

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		float top = 20;

		IconTitle title = new IconTitle( Icons.JOURNAL.get(), Messages.get(this, "title") );
		title.setSize(200, 0);
		title.setPos(
				insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (top - title.height()) / 2f
		);
		align(title);
		add(title);

		NinePatch panel = Chrome.get(Chrome.Type.TOAST);

		int pw = (landscape() ? WIDTH_L : WIDTH_P) + panel.marginHor();
		int ph = h - 50 + panel.marginVer();

		panel.size(pw, ph);
		panel.x = insets.left + (w - pw) / 2f;
		panel.y = insets.top + top;
		add(panel);

		journalPanel = panel;
		showSelectedTab();

		StyledButton btnBadges =  new StyledButton(Chrome.Type.GREY_BUTTON_TR, ""){
			@Override
			protected void onClick() {
				super.onClick();
				selectTab(0);
			}

			@Override
			protected String hoverText() {
				return Messages.get(WndJournal.BadgesTab.class, "title");
			}
		};
		btnBadges.icon(Icons.BADGES.get());
		btnBadges.setRect(panel.x, panel.y + ph - 3, pw/4f + 1.5f, lastIDX == 0 ? 25 : 20);
		align(btnBadges);
		if (lastIDX != 0) btnBadges.icon().brightness(0.6f);
		addToBack(btnBadges);
		tabButtons[0] = btnBadges;

		StyledButton btnCatalog =  new StyledButton(Chrome.Type.GREY_BUTTON_TR, ""){
			@Override
			protected void onClick() {
				super.onClick();
				selectTab(1);
			}
			@Override
			protected String hoverText() {
				return Messages.get(WndJournal.CatalogTab.class, "title");
			}
		};
		btnCatalog.icon(Icons.CATALOG.get());
		btnCatalog.setRect(btnBadges.right()-2, btnBadges.top(), pw/4f + 1.5f, lastIDX == 1 ? 25 : 20);
		align(btnCatalog);
		if (lastIDX != 1) btnCatalog.icon().brightness(0.6f);
		addToBack(btnCatalog);
		tabButtons[1] = btnCatalog;

		StyledButton btnGuide =  new StyledButton(Chrome.Type.GREY_BUTTON_TR, ""){
			@Override
			protected void onClick() {
				super.onClick();
				selectTab(2);
			}
			@Override
			protected String hoverText() {
				return Messages.get(WndJournal.GuideTab.class, "title");
			}
		};
		btnGuide.icon(new ItemSprite(ItemSpriteSheet.MASTERY));
		btnGuide.setRect(btnCatalog.right()-2, btnBadges.top(), pw/4f + 1.5f, lastIDX == 2 ? 25 : 20);
		align(btnGuide);
		if (lastIDX != 2) btnGuide.icon().brightness(0.6f);
		addToBack(btnGuide);
		tabButtons[2] = btnGuide;

		StyledButton btnAlchemy =  new StyledButton(Chrome.Type.GREY_BUTTON_TR, ""){
			@Override
			protected void onClick() {
				super.onClick();
				selectTab(3);
			}
			@Override
			protected String hoverText() {
				return Messages.get(WndJournal.AlchemyTab.class, "title");
			}
		};
		btnAlchemy.icon(Icons.ALCHEMY.get());
		btnAlchemy.setRect(btnGuide.right()-2, btnBadges.top(), pw/4f + 1.5f, lastIDX == 3 ? 25 : 20);
		align(btnAlchemy);
		if (lastIDX != 3) btnAlchemy.icon().brightness(0.6f);
		addToBack(btnAlchemy);
		tabButtons[3] = btnAlchemy;

		addToBack(BG);

		ExitButton btnExit = new ExitButton();
		btnExit.setPos( insets.left + w - btnExit.width(), insets.top );
		add( btnExit );

		fadeIn();
	}

	@Override
	public void destroy() {

		Badges.saveGlobal();

		super.destroy();
	}

	@Override
	protected void onBackPressed() {
		ShatteredPixelDungeon.switchNoFade( TitleScene.class );
	}

}
