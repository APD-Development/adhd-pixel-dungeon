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

package com.shatteredpixel.shatteredpixeldungeon.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewConfiguration;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidAudio;
import com.badlogic.gdx.backends.android.AsynchronousAndroidAudio;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import com.shatteredpixel.shatteredpixeldungeon.services.news.News;
import com.shatteredpixel.shatteredpixeldungeon.services.news.NewsImpl;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.UpdateImpl;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.Updates;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.watabou.input.BackGate;
import com.watabou.input.BackTrace;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;

public class AndroidLauncher extends AndroidApplication {
	
	public static AndroidApplication instance;
	
	private static AndroidPlatformSupport support;
	
	@SuppressLint("SetTextI18n")
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			GdxNativesLoader.load();
			FreeType.initFreeType();
		} catch (Exception e){
			GdxNativesLoader.disableNativesLoading = true;
			AndroidMissingNativesHandler.error = e;
			Intent intent = new Intent(this, AndroidMissingNativesHandler.class);
			startActivity(intent);
			finish();
			//let initialization continue for a moment so that we can set up things libGDX expects to be set up
		}

		//there are some things we only need to set up on first launch
		if (instance == null) {

			instance = this;

			try {
				Game.version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			} catch (PackageManager.NameNotFoundException e) {
				Game.version = "???";
			}
			try {
				Game.versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			} catch (PackageManager.NameNotFoundException e) {
				Game.versionCode = 0;
			}

			Gdx.app = this;

			//we want to try and ID if we were installed by a 3rd party appstore.
			// If so we vary update and news checking a bit.
			String installer;
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
					installer = getPackageManager().getInstallSourceInfo(getPackageName()).getInstallingPackageName();
				} else {
					installer = getPackageManager().getInstallerPackageName(getPackageName());
				}
				if (installer == null) installer = "???";
			} catch (Exception e) {
				installer = "???";
			}

			//if we were installed by a known 3rd party appstore that auto-updates, disable update checking
			if (UpdateImpl.supportsUpdates()
					&& !installer.contains("fdroid") && !installer.contains("com.looker.droidify") && !installer.contains("com.uptodown")) {
				Updates.service = UpdateImpl.getUpdateService();
			}

			//APD: Community's 16+ gate asks Google Play first; asked only when the player opens Community
			com.shatteredpixel.shatteredpixeldungeon.apd.age.CommunityAgeGate.play = new PlayAgeSignals(this);
			if (!com.shatteredpixel.shatteredpixeldungeon.SPDSettings.COMMUNITY_GOOGLE_CLIENT_ID.isEmpty())
				com.shatteredpixel.shatteredpixeldungeon.apd.community.CommunityController.google = new GoogleCommunitySignIn(this, com.shatteredpixel.shatteredpixeldungeon.SPDSettings.COMMUNITY_GOOGLE_CLIENT_ID);

			//F-Droid specifically considers auto news checking to be an 'anti-feature', so default it to false
			if (NewsImpl.supportsNews()) {
				if (installer.contains("fdroid") || installer.contains("com.looker.droidify")){
					SPDSettings.newsDefault = false;
				}
				News.service = NewsImpl.getNewsService();
			}

			FileUtils.setDefaultFileProperties(Files.FileType.Local, "");

			// grab preferences directly using our instance first
			// so that we don't need to rely on Gdx.app, which isn't initialized yet.
			// Note that we use a different prefs name on android for legacy purposes,
			// this is the default prefs filename given to an android app (.xml is automatically added to it)
			SPDSettings.set(instance.getPreferences("ShatteredPixelDungeon"));

		} else {
			instance = this;
			com.shatteredpixel.shatteredpixeldungeon.apd.age.CommunityAgeGate.play = new PlayAgeSignals(this);
			if (!com.shatteredpixel.shatteredpixeldungeon.SPDSettings.COMMUNITY_GOOGLE_CLIENT_ID.isEmpty())
				com.shatteredpixel.shatteredpixeldungeon.apd.community.CommunityController.google = new GoogleCommunitySignIn(this, com.shatteredpixel.shatteredpixeldungeon.SPDSettings.COMMUNITY_GOOGLE_CLIENT_ID);
		}

        AndroidSupportDiagnostics.initialize(this);

		//Shattered still overrides the back gesture behaviour, but we need to do it in a new way
		// (API added in Android 13, functionality enforced in Android 16)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			//we post this to a runnable so that it's delayed and overrides
			// default GDX back handling, which only sends a key down event
			runnables.add(new Runnable() {
				@Override
				public void run() {
					if (instance != AndroidLauncher.this || isFinishing() || isDestroyed()) return;
					// Keep libGDX's route as a fallback: registration does not guarantee
					// Android will deliver this callback for every Back input source.
					// BackGate suppresses the one legacy copy only after this callback fires.
					getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, new OnBackInvokedCallback() {
						@Override
						public void onBackInvoked() {
							if (instance == AndroidLauncher.this) sendBack();
						}
					});
					// Only claim the route after registration succeeded. Legacy copies
					// can arrive hundreds of milliseconds after this callback.
					BackGate.setSystemBackOwner(AndroidLauncher.this);
				}
			});
		}

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.depth = 0;

		//we manage this ourselves
		config.useImmersiveMode = false;
		
		config.useCompass = false;
		config.useAccelerometer = false;
		
		//APD: the system file picker for exporting and importing the history (each activity gets a provider that listens to its results)
		ApdFiles.provider = new AndroidFileProvider(this);

		if (support == null) support = new AndroidPlatformSupport();
		else                 support.reloadGenerators();
		
		support.updateSystemUI();

		Button.longClick = ViewConfiguration.getLongPressTimeout()/1000f;
		
		initialize(new ShatteredPixelDungeon(support), config);
		requestHighestRefreshRate();
	}

	//APD: ask Android for the screen's highest refresh rate (e.g. 144 Hz) instead of the
	//60 Hz many phones give apps by default. Android may still decide otherwise.
	private void requestHighestRefreshRate(){
		//Developer build only (owner's request); other builds keep Android's default rate
		if (!com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.testBuild()) return;
		try {
			android.view.Display display = getWindowManager().getDefaultDisplay();
			android.view.Display.Mode current = display.getMode(), best = current;
			for (android.view.Display.Mode mode : display.getSupportedModes()){
				if (mode.getPhysicalWidth() == current.getPhysicalWidth()
						&& mode.getPhysicalHeight() == current.getPhysicalHeight()
						&& mode.getRefreshRate() > best.getRefreshRate()) best = mode;
			}
			android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
			if (params.preferredDisplayModeId != best.getModeId()){
				params.preferredDisplayModeId = best.getModeId();
				getWindow().setAttributes(params);
			}
			if (android.os.Build.VERSION.SDK_INT >= 30 && graphics != null && graphics.getView() instanceof android.view.SurfaceView){
				android.view.Surface surface = ((android.view.SurfaceView) graphics.getView()).getHolder().getSurface();
				if (surface != null && surface.isValid())
					surface.setFrameRate(best.getRefreshRate(), android.view.Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
			}
			com.watabou.utils.SupportDiagnostics.event("display.refresh", "requested_hz=" + best.getRefreshRate());
		} catch (RuntimeException unavailable){
			com.watabou.utils.SupportDiagnostics.event("display.refresh", "request_failed=" + unavailable.getClass().getSimpleName());
		}
	}

	@Override
	public AndroidAudio createAudio(Context context, AndroidApplicationConfiguration config) {
		return new AsynchronousAndroidAudio(context, config);
	}

	@Override
	protected void onResume() {
		com.watabou.utils.SupportDiagnostics.event("activity.resume", "");
        com.watabou.utils.SupportActionJournal.resumed(com.watabou.noosa.Game.scene() == null ? "unknown" : com.watabou.noosa.Game.scene().getClass().getSimpleName());
		com.watabou.utils.SupportDiagnostics.memoryEvent("activity.resume");
		//prevents weird rare cases where the app is running twice
		if (instance != this){
			finishAndRemoveTask();
		}
		super.onResume();
		requestHighestRefreshRate();
	}

	@Override protected void onDestroy() {
		// An old Activity must not release the newer Activity's callback ownership.
		BackGate.clearSystemBackOwner(this);
		super.onDestroy();
	}

    @Override protected void onPause() {
        com.watabou.utils.SupportDiagnostics.event("activity.pause", "");
        com.watabou.utils.SupportActionJournal.paused(com.watabou.noosa.Game.scene() == null ? "unknown" : com.watabou.noosa.Game.scene().getClass().getSimpleName());
        com.watabou.utils.SupportDiagnostics.memoryEvent("activity.pause");
        super.onPause();
    }

    @Override public void onTrimMemory(int level) {
        com.watabou.utils.SupportDiagnostics.event("android.trim_memory", "level="+level);
        com.watabou.utils.SupportDiagnostics.memoryEvent("android.trim_memory", level);
        super.onTrimMemory(level);
    }

    @Override public void onLowMemory() {
        com.watabou.utils.SupportDiagnostics.event("android.low_memory", "OS memory-pressure notification");
        com.watabou.utils.SupportDiagnostics.memoryEvent("android.low_memory");
        super.onLowMemory();
    }

	@SuppressLint("GestureBackNavigation")
	@Override
	public void onBackPressed() {
		//the game normally catches Back itself; if the system hands it to the activity instead, it is the same press (BackGate drops a duplicate)
		sendBack();
	}

	//one Back for the game, from whichever route it arrived by
	private static void sendBack() {
		//Timing-only diagnostics are packaged in the private developer APK. They do
		//not change Back behavior and do not run in the regular player APK.
		BackTrace.setEnabled(ApdFeatures.testBuild());
		long trace = BackTrace.route("android-system");
		if (!BackGate.acceptSystem()) {
			BackTrace.rejected(trace, "BackGate duplicate");
			return;
		}
		BackTrace.accepted(trace);
		KeyEvent.addKeyEvent(new KeyEvent(Input.Keys.BACK, true, trace));
		BackTrace.stage(trace, "key-down-enqueued", "");
		KeyEvent.addKeyEvent(new KeyEvent(Input.Keys.BACK, false, trace));
		// The Android callback does not itself pass through libGDX input.
		// Wake an on-demand or throttled renderer so the queued Back is handled.
		if (Gdx.graphics != null) Gdx.graphics.requestRendering();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		support.updateSystemUI();
	}
	
	@Override
	public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
		super.onMultiWindowModeChanged(isInMultiWindowMode);
		support.updateSystemUI();
	}
}
