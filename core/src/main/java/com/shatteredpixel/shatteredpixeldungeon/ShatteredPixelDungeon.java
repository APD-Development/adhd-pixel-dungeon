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

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.WelcomeScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AboutScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.JournalScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.RankingsScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.StartScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CommunityScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.ChangesScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.NewsScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.SupporterScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PlatformSupport;

public class ShatteredPixelDungeon extends Game {

	//rankings from v1.2.3 and older use a different score formula, so this reference is kept
	public static final int v1_2_3 = 628;

	//savegames from versions older than v3.1.1 are no longer supported, and data from them is ignored
	public static final int v3_1_1 = 850;
	public static final int v3_2_1 = 861; //last version for Android 4.4- and Java 8
	public static final int v3_2_5 = 877;
	public static final int v3_3_0 = 883;

	//starting here we are doing 2 version codes per public update, so use code-1 to get both
	public static final int v4_0_0 = 909;
	
	public ShatteredPixelDungeon( PlatformSupport platform ) {
		super( sceneClass == null ? WelcomeScene.class : sceneClass, platform );

		//pre-v3.3.0
		com.watabou.utils.Bundle.addAlias(
				com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey.class,
				"com.shatteredpixel.shatteredpixeldungeon.items.keys.SkeletonKey" );

	}
	
	@Override
	protected void update() {
		super.update();
		//APD: the Cleric grudge (does nothing while its switch is off)
		com.shatteredpixel.shatteredpixeldungeon.windows.GrudgeHooks.tick();
	}

	@Override
	public void create() {
		super.create();

		updateSystemUI();
		SPDAction.loadBindings();
		//APD: existing players keep the settings that used to be on by default (new installs start with them off)
		SPDSettings.keepOldDefaultsForExistingPlayers();
		com.watabou.utils.SupportDiagnostics.appSettings = SPDSettings::apdSettingsReport;

        // Android configured private storage before GDX preferences became available.
        // Journal collection starts only after an explicit, persisted player opt-in.
        com.watabou.utils.SupportActionJournal.setEnabled(SPDSettings.apdExtendedBugLogging());
        // Developer build only: log frames slower than 100 ms (stutter probe).
        com.watabou.utils.SupportDiagnostics.hitchProbe = com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.testBuild();
        if (com.watabou.utils.SupportDiagnostics.hitchProbe) com.watabou.utils.SupportDiagnostics.startSpikeWatcher();
		
		Music.INSTANCE.enable( SPDSettings.music() );
		Music.INSTANCE.volume( SPDSettings.musicVol()*SPDSettings.musicVol()/100f );
		Sample.INSTANCE.enable( SPDSettings.soundFx() );
		Sample.INSTANCE.volume( SPDSettings.SFXVol()*SPDSettings.SFXVol()/100f );

		Sample.INSTANCE.load( Assets.Sounds.all );

		//APD: set up run history recording (on by default, see SPDSettings.apdRecording)
		ApdRecorder.configureDefault( SPDSettings.apdRecording() );

		//APD: a used install is not a new player, so SPD's first-run tutorial (closed floor-1 start, no Main Menu) ends for it
		com.shatteredpixel.shatteredpixeldungeon.apd.ApdIntro.endIfNotNew();
		
	}

	@Override
	public void finish() {
		if (!DeviceCompat.isiOS()) {
			super.finish();
		} else {
			//can't exit on iOS (Apple guidelines), so just go to title screen
			switchScene(TitleScene.class);
		}
	}

	public static void switchNoFade(Class<? extends PixelScene> c){
		switchNoFade(c, null);
	}

	public static void switchNoFade(Class<? extends PixelScene> c, SceneChangeCallback callback) {
		PixelScene.noFade = true;
		switchScene( c, callback );
	}
	
	public static void seamlessResetScene(SceneChangeCallback callback) {
		invalidateRetainedReturn();
		if (scene() instanceof PixelScene){
			((PixelScene) scene()).saveWindows();
			switchNoFade((Class<? extends PixelScene>) sceneClass, callback );
		} else {
			resetScene();
		}
	}
	
	public static void seamlessResetScene(){
		seamlessResetScene(null);
	}
	
	@Override
	protected void switchScene() {
		super.switchScene();
		if (scene instanceof PixelScene){
			((PixelScene) scene).restoreWindows();
			// A retained title skips create(); replenish the single-use UIMSD
			// preload on return as well as on the first title construction.
			if (scene instanceof TitleScene) UimsdScene.prewarmTitle();
		}
	}

	@Override
	protected boolean isReturnMenu(Class<? extends com.watabou.noosa.Scene> target){
		return target == TitleScene.class || target == AboutScene.class
				|| target == JournalScene.class || target == RankingsScene.class
				|| target == StartScene.class || target == HeroSelectScene.class
				|| target == APDTextScene.class || target == CommunityScene.class
				|| target == UimsdScene.class || target == ChangesScene.class
				|| target == NewsScene.class || target == SupporterScene.class;
	}

	@Override
	protected boolean retainForReturn(com.watabou.noosa.Scene current,
			Class<? extends com.watabou.noosa.Scene> target){
		return DeviceCompat.isAndroid() && current instanceof TitleScene
				&& target != TitleScene.class && isReturnMenu(target)
				&& ((PixelScene) current).canRetainMenu();
	}
	
	@Override
	public void resize( int width, int height ) {
		if (width == 0 || height == 0){
			return;
		}

		if (scene instanceof PixelScene &&
				(height != Game.height || width != Game.width)) {
			PixelScene.noFade = true;
			((PixelScene) scene).saveWindows();
		}

		super.resize( width, height );

		updateDisplaySize();

	}
	
	@Override
	public void destroy(){
		super.destroy();
		GameScene.endActorThread();
	}
	
	public void updateDisplaySize(){
		platform.updateDisplaySize();
	}

	public static void updateSystemUI() {
		platform.updateSystemUI();
	}
}
