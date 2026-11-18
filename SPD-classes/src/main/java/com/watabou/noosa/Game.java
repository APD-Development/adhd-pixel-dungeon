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

package com.watabou.noosa;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.utils.TimeUtils;
import com.watabou.glscripts.Script;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.input.ControllerHandler;
import com.watabou.input.InputHandler;
import com.watabou.input.KeyEvent;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Reflection;
import com.watabou.utils.Signal;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Game implements ApplicationListener {

	public static Game instance;
	
	// Size of the EGL surface view
	public static int width;
	public static int height;

	// Density: mdpi=1, hdpi=1.5, xhdpi=2...
	public static float density = 1;
	
	public static String version;
	public static int versionCode;
	
	// Current scene
	protected Scene scene;
	// New scene we are going to switch to
	protected Scene requestedScene;
	// true if scene switch is requested
	protected boolean requestedReset = true;
	// callback to perform logic during scene change
	protected SceneChangeCallback onChange;
	// New scene class
	protected static Class<? extends Scene> sceneClass;

	// One return parent. Gameplay and unknown scene types never opt in.
	private RetainedScene retainedReturn;
	private volatile boolean invalidateReturn;
	private static final class RetainedScene {
		final Scene scene;
		final Camera.State cameras;
		final Signal<PointerEvent> pointers;
		RetainedScene(Scene scene){
			this.scene = scene;
			scene.onRetain();
			scene.active = false;
			cameras = Camera.detach();
			pointers = PointerEvent.detachListeners();
		}
	}

	protected boolean isReturnMenu(Class<? extends Scene> target){ return false; }
	protected boolean retainForReturn(Scene current, Class<? extends Scene> target){ return false; }

	public static void invalidateRetainedReturn(){
		if (instance != null) instance.invalidateReturn = true;
	}

	private void clearRetainedReturn(){
		RetainedScene old = retainedReturn;
		retainedReturn = null;
		if (old == null) return;
		Signal<PointerEvent> activePointers = PointerEvent.swapListeners(old.pointers);
		try {
			old.scene.destroy();
			old.cameras.destroy();
		} finally {
			PointerEvent.swapListeners(activePointers);
		}
	}

	private void discardInvalidReturn(){
		if (invalidateReturn){
			invalidateReturn = false;
			clearRetainedReturn();
		}
	}
	
	public static float timeScale = 1f;
	public static float elapsed = 0f;
	public static float timeTotal = 0f;
	public static long realTime = 0;

	public static InputHandler inputHandler;
	
	public static PlatformSupport platform;
	
	public Game(Class<? extends Scene> c, PlatformSupport platform) {
		sceneClass = c;
		
		instance = this;
		this.platform = platform;
	}
	
	@Override
	public void create() {
		density = Gdx.graphics.getDensity();
		if (density == Float.POSITIVE_INFINITY){
			density = 100f / 160f; //assume 100PPI if density can't be found
		} else if (DeviceCompat.isDesktop()) {
			int dispWidth = Gdx.graphics.getDisplayMode().width;
			int dispHeight = Gdx.graphics.getDisplayMode().height;
			float reportedWidth = dispWidth / Gdx.graphics.getPpiX();
			float reportedHeight = dispHeight / Gdx.graphics.getPpiY();

			//this exists because Steam deck reports its display size as 4"x6.3" for some reason
			// as if in portrait, instead of 6.3"x4". This results in incorrect PPI measurements.
			// So we check that the orientation of the resolution and the display dimensions match.
			// If they don't, re-calculate density assuming reported dimensions are flipped.
			if (dispWidth > dispHeight != reportedWidth > reportedHeight){
				float realPpiX = dispWidth / reportedHeight;
				density = realPpiX / 160f;
			}
		}

		inputHandler = new InputHandler( Gdx.input );
		if (ControllerHandler.controllersSupported()){
			Controllers.addListener(new ControllerHandler());
		}

		//refreshes texture and vertex data stored on the gpu
		versionContextRef = Gdx.graphics.getGLVersion();
		Blending.useDefault();
		TextureCache.reload();
		Vertexbuffer.reload();
	}

	private GLVersion versionContextRef;
	
	@Override
	public void resize(int width, int height) {
		if (width == 0 || height == 0){
			return;
		}

		//If the EGL context was destroyed, we need to refresh some data stored on the GPU.
		// This checks that by seeing if GLVersion has a new object reference
		if (versionContextRef != Gdx.graphics.getGLVersion()) {
			invalidateRetainedReturn();
			versionContextRef = Gdx.graphics.getGLVersion();
			Blending.useDefault();
			TextureCache.reload();
			Vertexbuffer.reload();
		}

		if (height != Game.height || width != Game.width) {

			Game.width = width;
			Game.height = height;
			
			resetScene();
		}
	}

	//justResumed is a bit of a hack to improve start time metrics on Android,
	// as texture refreshing leads to slow warm starts. TODO would be nice to fix this properly
	private boolean justResumed = true;
    private long resumeStartedNanos = System.nanoTime();
    private boolean awaitingFirstDraw = true;
    private boolean awaitingScenePresentation = true;

	@Override
	public void render() {
		//prevents weird rare cases where the app is running twice
		if (instance != this){
			finish();
			return;
		}

		if (justResumed){
			discardInvalidReturn();
			justResumed = false;
			if (DeviceCompat.isAndroid()) return;
		}

		// The Android system Back callback queues into KeyEvent rather than
		// libGDX's input processor. If it arrived before this frame, drain all
		// pending input in its existing pointer/key/scroll order before drawing.
		// Otherwise drawing can block the Back handler for an additional frame.
		// Scene switches must still create the new scene before dispatch.
		if (!requestedReset && scene != null && inputHandler != null
				&& KeyEvent.hasPendingBackPress()) {
			inputHandler.processAllEvents();
			// Back may have requested a menu transition. Apply it before drawing,
			// rather than drawing the outgoing menu and waiting another frame.
			applyRequestedScene();
		}

		// A tap dispatched by the previous update has already chosen its scene.
		// Create it before this draw instead of showing another outgoing frame.
		applyRequestedScene();
		Camera.refreshMatrices();
		NoosaScript.get().resetCamera();
		NoosaScriptNoLighting.get().resetCamera();
		Gdx.gl.glDisable(Gdx.gl.GL_SCISSOR_TEST);
		Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
		draw();
        if(awaitingScenePresentation){
            awaitingScenePresentation=false;
            if(inputHandler!=null)inputHandler.scenePresented(System.nanoTime());
        }
        if (awaitingFirstDraw) {
            awaitingFirstDraw = false;
            com.watabou.utils.SupportDiagnostics.event("render.first_draw", "after_resume_ms="+((System.nanoTime()-resumeStartedNanos)/1000000L));
        }

		Gdx.gl.glDisable( Gdx.gl.GL_SCISSOR_TEST );
		
		step();
	}
	
	@Override
	public void pause() {
		com.watabou.utils.SupportDiagnostics.event("game.pause", scene == null ? "no scene" : scene.getClass().getSimpleName());
		com.watabou.utils.SupportDiagnostics.memoryEvent("game.pause");
		InputHandler.discardPendingEvents();
		if (scene != null) {
			scene.onPause();
		}
		
		Script.reset();
	}
	
	@Override
	public void resume() {
		com.watabou.utils.SupportDiagnostics.resetFrameClock();
		com.watabou.utils.SupportDiagnostics.event("game.resume", "");
		com.watabou.utils.SupportDiagnostics.memoryEvent("game.resume");
		resumeStartedNanos=System.nanoTime();awaitingFirstDraw=true;
		InputHandler.discardPendingEvents();
		invalidateRetainedReturn();
		justResumed = true;
	}
	
	public void finish(){
		Gdx.app.exit();
		
	}
	
	public void destroy(){
		clearRetainedReturn();
		if (scene != null) {
			scene.destroy();
			scene = null;
		}
		
		sceneClass = null;
		Music.INSTANCE.reset();
		Sample.INSTANCE.reset();
	}
	
	@Override
	public void dispose() {
		destroy();
	}
	
	public static void resetScene() {
		invalidateRetainedReturn();
		switchScene( instance.sceneClass );
	}

	public static void switchScene(Class<? extends Scene> c) {
		switchScene(c, null);
	}
	
	public static void switchScene(Class<? extends Scene> c, SceneChangeCallback callback) {
		com.watabou.utils.SupportDiagnostics.event("scene.request", c.getSimpleName());
		instance.sceneClass = c;
		instance.requestedReset = true;
		instance.onChange = callback;
	}
	
	public static Scene scene() {
		return instance.scene;
	}

	public static boolean switchingScene() {
		return instance.requestedReset;
	}

    public static boolean switchingTo(Class<? extends Scene> target) {
        return instance != null && instance.requestedReset && sceneClass == target;
    }
	
	protected void step() {
		applyRequestedScene();
		update();
	}

	protected void applyRequestedScene() {
		discardInvalidReturn();
		
		if (requestedReset) {
			requestedReset = false;
			
			requestedScene = retainedReturn != null && onChange == null
					&& retainedReturn.scene.getClass() == sceneClass
					? retainedReturn.scene : Reflection.newInstance(sceneClass);
			if (requestedScene != null){
				switchScene();
			}

		}
		
	}
	
	protected void draw() {
		if (scene != null) scene.draw();
	}
	
	protected void switchScene() {
        awaitingScenePresentation=true;
        long sceneStart=System.nanoTime();
        com.watabou.utils.SupportDiagnostics.event("scene.begin", requestedScene.getClass().getSimpleName());

		if (onChange != null || !isReturnMenu(sceneClass)
				|| (scene != null && scene.getClass() == sceneClass)) clearRetainedReturn();

		RetainedScene returning = retainedReturn != null && retainedReturn.scene == requestedScene
				? retainedReturn : null;
		if (returning != null){
			retainedReturn = null;
			if (scene != null) scene.destroy();
			Camera.restore(returning.cameras);
			PointerEvent.swapListeners(returning.pointers);
			scene = returning.scene;
			scene.active = true;
			scene.onRestore();
		} else {
			if (scene != null){
				if (retainedReturn == null && onChange == null && retainForReturn(scene, sceneClass)){
					retainedReturn = new RetainedScene(scene);
				} else scene.destroy();
			}
			Camera.reset();
			// Retained graphics own their buffers until return or invalidation.
			if (retainedReturn == null) Vertexbuffer.clear();
			scene = requestedScene;
			if (onChange != null) onChange.beforeCreate();
			scene.create();
			if (onChange != null) onChange.afterCreate();
		}
		onChange = null;
		
		Game.elapsed = 0f;
		Game.timeScale = 1f;
		Game.timeTotal = 0f;
        com.watabou.utils.SupportDiagnostics.resetFrameClock();
        com.watabou.utils.SupportDiagnostics.event("scene.ready", scene.getClass().getSimpleName()+" load_ms="+((System.nanoTime()-sceneStart)/1000000L));
	}

	protected void update() {
		com.watabou.utils.SupportDiagnostics.periodicMemoryEvent();
		com.watabou.utils.SupportDiagnostics.frame(scene);
		//game will not process more than 200ms of graphics time per frame
		float frameDelta = Math.min(0.2f, Gdx.graphics.getDeltaTime());
		Game.elapsed = Game.timeScale * frameDelta;
		Game.timeTotal += Game.elapsed;
		
		Game.realTime = TimeUtils.millis();

		inputHandler.processAllEvents();

		Music.INSTANCE.update();
		Sample.INSTANCE.update();
		scene.update();
		Camera.updateAll();
	}
	
	public static void reportException( Throwable tr ) {
		if (instance != null && Gdx.app != null) {
			instance.logException(tr);
		} else {
			//fallback if error happened in initialization
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			tr.printStackTrace(pw);
			pw.flush();
			System.err.println(sw.toString());
		}
	}
	
	protected void logException( Throwable tr ){
		com.watabou.utils.SupportDiagnostics.error(tr);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		tr.printStackTrace(pw);
		pw.flush();
		Gdx.app.error("GAME", sw.toString());
	}
	
	public static void runOnRenderThread(Callback c){
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				c.call();
			}
		});
	}
	
	public static void vibrate( int milliseconds ) {
		if (platform.supportsVibration()) {
			platform.vibrate(milliseconds);
		}
	}

	public interface SceneChangeCallback{
		void beforeCreate();
		void afterCreate();
	}
	
}



