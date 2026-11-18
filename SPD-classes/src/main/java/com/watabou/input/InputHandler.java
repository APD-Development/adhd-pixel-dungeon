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

package com.watabou.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.watabou.noosa.Game;
import com.watabou.utils.PointF;

public class InputHandler extends InputAdapter {

	private InputMultiplexer multiplexer;
	private boolean acceptedBackDown;
    private com.watabou.noosa.Scene acceptedBackScene;
    private final Input sourceInput;
    private long visibleSceneNanos;
    private boolean emulating;
    private final java.util.HashSet<Long> pressedTouches=new java.util.HashSet<>();

    /** Called after the new scene's first complete draw, not after every frame. */
    public synchronized void scenePresented(long completedDrawNanos){
        visibleSceneNanos=Math.max(visibleSceneNanos,completedDrawNanos);
        boolean sameScene=Game.instance!=null && acceptedBackScene==Game.scene();
        if(!sameScene && !KeyEvent.hasPendingBackPress()){acceptedBackDown=false;acceptedBackScene=null;}
        pressedTouches.clear();
        PointerEvent.discardPendingEvents();
    }
    private long touchKey(int pointer,int button){return ((long)pointer<<32)|(button&0xffffffffL);}
    private boolean obsolete(long source){
        if(source>0&&source<visibleSceneNanos){
            com.watabou.utils.SupportDiagnostics.event("input.obsolete","source_age_ms="+((System.nanoTime()-source)/1000000L));
            return true;
        }
        return false;
    }

	public InputHandler( Input input ){
        sourceInput=input;
		discardPendingEvents();
		multiplexer = new InputMultiplexer();
		input.setInputProcessor(multiplexer);
		addInputProcessor(this);
		// Keep the GDX route as a fallback when Android registers but does not
		// deliver its system callback. BackGate deduplicates only after delivery.
		input.setCatchKey( Input.Keys.BACK, true);
		input.setCatchKey( Input.Keys.MENU, true);
	}

	/** Queued input belongs to the foreground lifecycle that received it. */
	public static void discardPendingEvents(){
		PointerEvent.discardPendingEvents();
		KeyEvent.discardPendingEvents();
		ScrollEvent.discardPendingEvents();
	}

    private long sourceTime(){
        if(emulating)return 0;
        long time=sourceInput.getCurrentEventTime();
        return time>0 && time<=System.nanoTime() ? time : 0;
    }
    private PointerEvent stamp(PointerEvent event){event.sourceEventNanos=sourceTime();return event;}
    private KeyEvent stamp(KeyEvent event){event.sourceEventNanos=sourceTime();return event;}
    private ScrollEvent stamp(ScrollEvent event){event.sourceEventNanos=sourceTime();return event;}

	public void addInputProcessor(InputProcessor processor){
		multiplexer.addProcessor(0, processor);
	}

	public void removeInputProcessor(InputProcessor processor){
		multiplexer.removeProcessor(processor);
	}

	public synchronized void emulateTouch(int id, int button, boolean down){
		PointF hoverPos = PointerEvent.currentHoverPos();
		emulating=true;
		try{
		if (down){
			multiplexer.touchDown((int)hoverPos.x, (int)hoverPos.y, id, button);
		} else {
			multiplexer.touchUp((int)hoverPos.x, (int)hoverPos.y, id, button);
		}
		}finally{emulating=false;}
	}

	public synchronized void emulateDrag(int id){
		PointF hoverPos = PointerEvent.currentHoverPos();
		emulating=true;
		try{multiplexer.touchDragged((int)hoverPos.x, (int)hoverPos.y, id);}finally{emulating=false;}
	}
	
	public void processAllEvents(){
		PointerEvent.processPointerEvents();
        // A menu click may have requested another scene. Preserve Back/scroll
        // for that scene instead of delivering them to the outgoing title.
        if (Game.instance != null && Game.switchingScene()) return;
		KeyEvent.processKeyEvents();
		ScrollEvent.processScrollEvents();
	}
	
	// *********************
	// *** Pointer Input ***
	// *********************
	
	@Override
	public synchronized boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if(obsolete(sourceTime()))return true;
		if (screenX < 0 || screenX > Game.width || screenY < 0 || screenY > Game.height){
			return true;
		}

		if (pointer != ControllerHandler.CONTROLLER_POINTER_ID) {
			ControllerHandler.setControllerPointer(false);
			ControllerHandler.controllerActive = false;
		}
		if(button<3)pressedTouches.add(touchKey(pointer,button));

		if (button >= 3 && KeyBindings.isKeyBound( button + 1000 )) {
			KeyEvent.addKeyEvent( stamp(new KeyEvent( button + 1000, true ) ));
		} else if (button < 3) {
			PointerEvent.addPointerEvent(stamp(new PointerEvent(screenX, screenY, pointer, PointerEvent.Type.DOWN, button)));
		}
		return true;
	}
	
	@Override
	public synchronized boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if(obsolete(sourceTime()))return true;
		if(button<3&&!pressedTouches.remove(touchKey(pointer,button)))return true;

		if (button >= 3 && KeyBindings.isKeyBound( button + 1000 )) {
			KeyEvent.addKeyEvent( stamp(new KeyEvent( button + 1000, false ) ));
		} else if (button < 3) {
			PointerEvent.addPointerEvent(stamp(new PointerEvent(screenX, screenY, pointer, PointerEvent.Type.UP, button)));
		}
		return true;
	}

	@Override
	public synchronized boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
		if(obsolete(sourceTime()))return true;
		if(button<3&&!pressedTouches.remove(touchKey(pointer,button)))return true;

		if (button >= 3 && KeyBindings.isKeyBound( button + 1000 )) {
			KeyEvent.addKeyEvent( stamp(new KeyEvent( button + 1000, false ) ));
		} else if (button < 3) {
			PointerEvent.addPointerEvent(stamp(new PointerEvent(screenX, screenY, pointer, PointerEvent.Type.CANCEL, button)));
		}
		return true;
	}

	@Override
	public synchronized boolean touchDragged(int screenX, int screenY, int pointer) {
		if(obsolete(sourceTime()))return true;
		PointerEvent.addIfExisting(stamp(new PointerEvent(screenX, screenY, pointer, PointerEvent.Type.DOWN)));
		return true;
	}
	
	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		if(obsolete(sourceTime()))return true;
		if (ControllerHandler.controllerPointerActive()) {
			ControllerHandler.setControllerPointer(false);
			PointF hover = ControllerHandler.getControllerPointerPos();
			screenX = (int)hover.x;
			screenY = (int)hover.y;
		}
		PointerEvent.addPointerEvent(stamp(new PointerEvent(screenX, screenY, -1, PointerEvent.Type.HOVER)));
		return true;
	}
	
	// *****************
	// *** Key Input ***
	// *****************
	
	@Override
	public synchronized boolean keyDown( int keyCode ) {
		//one press of Back is one Back, whichever route it came by (see BackGate)
		long backTrace = 0;
		if (keyCode == Input.Keys.BACK) {
			backTrace = BackTrace.route("gdx-key");
			if (!BackGate.acceptLegacy(System.nanoTime(), sourceTime())) {
				BackTrace.rejected(backTrace, "BackGate legacy route suppressed");
				return true;
			}
			acceptedBackDown = true;
            acceptedBackScene = Game.instance==null?null:Game.scene();
			BackTrace.accepted(backTrace);
		}
		if (KeyBindings.isKeyBound( keyCode )) {
			KeyEvent.addKeyEvent( stamp(new KeyEvent( keyCode, true, backTrace ) ));
			BackTrace.stage(backTrace, "key-down-enqueued", "");
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public synchronized boolean keyUp( int keyCode ) {
		if (keyCode == Input.Keys.BACK) {
			BackGate.recordLegacyRelease(sourceTime());
			// A Back-bound native button clicks on release. Preserve the matching
			// UP for an accepted DOWN; discard only an orphan/duplicate release.
			if (!acceptedBackDown) return true;
			acceptedBackDown = false;
            acceptedBackScene = null;
		}
		if (KeyBindings.isKeyBound( keyCode )) {
			KeyEvent.addKeyEvent( stamp(new KeyEvent( keyCode, false ) ));
			return true;
		} else {
			return false;
		}
	}
	
	// ********************
	// *** Scroll Input ***
	// ********************
	
	@Override
	public boolean scrolled(float amountX, float amountY) {
		ScrollEvent.addScrollEvent( stamp(new ScrollEvent(PointerEvent.currentHoverPos(), amountY)));
		return true;
	}
}

