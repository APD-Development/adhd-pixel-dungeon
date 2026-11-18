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

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;

//APD: a deliberate, permanent black screen. This is not a crash: nothing is thrown and the game
//keeps running, it just never lets the player past this point. Only closing the app gets out.
public class WndClericRemoved extends Window {

	public static final int ATTEMPTS_FOR_BADGE = 10;

	public WndClericRemoved() {
		super();

		//the normal window frame is hidden, a fully opaque black block covers the whole screen
		chrome.visible = false;
		shadow.visible = false;

		ColorBlock black = new ColorBlock(PixelScene.uiCamera.width, PixelScene.uiCamera.height, 0xFF000000);
		black.camera = PixelScene.uiCamera;
		add(black);
	}

	//counts the attempt, persists it, shows the black screen, and awards the badge on the 10th try
	public static void trigger() {
		int attempts = SPDSettings.apdClericAttempts() + 1;
		SPDSettings.apdClericAttempts(attempts);

		ShatteredPixelDungeon.scene().addToFront(new WndClericRemoved());

		if (attempts >= ATTEMPTS_FOR_BADGE && !Badges.isUnlocked(Badges.Badge.APD_FUCK_CLERIC)) {
			Badges.unlock(Badges.Badge.APD_FUCK_CLERIC);
			//the app is expected to be force closed from here, so save right away
			Badges.saveGlobal(true);
			PixelScene.showBadge(Badges.Badge.APD_FUCK_CLERIC);
		}
	}

	@Override
	public void onBackPressed() {
		//deliberately nothing, there is no way out except closing the app
	}

	@Override
	public void hide() {
		//deliberately nothing
	}
}
