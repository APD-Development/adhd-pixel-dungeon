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
import com.watabou.noosa.Game;
import com.watabou.utils.Signal;

import java.util.ArrayList;

public class KeyEvent {
    public final long diagnosticId=com.watabou.utils.SupportDiagnostics.nextInputId();
    public final long queuedNanos=System.nanoTime();
    public long sourceEventNanos; // GDX backend timestamp when available; zero means unknown.
	
	public int code;
	public boolean pressed;
	public long backTraceId;
	
	public KeyEvent( int code, boolean pressed ) {
		this(code, pressed, 0);
	}

	public KeyEvent( int code, boolean pressed, long backTraceId ) {
		this.code = code;
		this.pressed = pressed;
		this.backTraceId = backTraceId;
	}
	
	// **********************
	// *** Static members ***
	// **********************
	
	private static Signal<KeyEvent> keySignal = new Signal<>( true );
	
	public static void addKeyListener( Signal.Listener<KeyEvent> listener ){
		keySignal.add(listener);
	}
	
	public static void removeKeyListener( Signal.Listener<KeyEvent> listener ){
		keySignal.remove(listener);
	}
	
	public static void clearListeners(){
		keySignal.removeAll();
	}
	
	//Accumulated key events
	private static ArrayList<KeyEvent> keyEvents = new ArrayList<>();

	public static synchronized void discardPendingEvents(){
        if(!keyEvents.isEmpty())com.watabou.utils.SupportDiagnostics.event("input.discard","keys_queued="+keyEvents.size());
        keyEvents.clear();
    }
	
	public static synchronized void addKeyEvent( KeyEvent event ){
        com.watabou.utils.SupportDiagnostics.input("queued",event.code==Input.Keys.BACK?"back":"key (value not recorded)",event.diagnosticId,event.queuedNanos,event.sourceEventNanos);
		keyEvents.add( event );
	}
	
	// Scan only queued events. A pending Android Back press should not wait
	// behind the next frame's rendering; other input keeps its normal order.
	static boolean containsBackPress(Iterable<KeyEvent> events){
		for (KeyEvent event : events){
			if (event.pressed && event.code == Input.Keys.BACK) return true;
		}
		return false;
	}

	public static synchronized boolean hasPendingBackPress(){
		return containsBackPress(keyEvents);
	}

	public static synchronized void processKeyEvents(){
		if (keyEvents.isEmpty()) {
			return;
		}

		for (KeyEvent k : keyEvents){
            com.watabou.utils.SupportDiagnostics.input("dispatched",k.code==Input.Keys.BACK?"back":"key (value not recorded)",k.diagnosticId,k.queuedNanos,k.sourceEventNanos);
            com.watabou.utils.SupportActionJournal.record("key.event",
                    "id="+k.diagnosticId+" kind="+(k.code==Input.Keys.BACK?"back":"key_redacted")+" pressed="+k.pressed);
			boolean tracedBack = k.pressed && k.code == Input.Keys.BACK && k.backTraceId != 0;
			if (tracedBack) BackTrace.beginDispatch(k.backTraceId);
			try {
				if (KeyBindings.getActionForKey(k) == GameAction.LEFT_CLICK){
					Game.inputHandler.emulateTouch(ControllerHandler.CONTROLLER_POINTER_ID, PointerEvent.LEFT, k.pressed);
					if (KeyBindings.bindingKey) keySignal.dispatch(k);
				} else if (KeyBindings.getActionForKey(k) == GameAction.RIGHT_CLICK){
					Game.inputHandler.emulateTouch(ControllerHandler.CONTROLLER_POINTER_ID, PointerEvent.RIGHT, k.pressed);
					if (KeyBindings.bindingKey) keySignal.dispatch(k);
				} else if (KeyBindings.getActionForKey(k) == GameAction.MIDDLE_CLICK){
					Game.inputHandler.emulateTouch(ControllerHandler.CONTROLLER_POINTER_ID, PointerEvent.MIDDLE, k.pressed);
					if (KeyBindings.bindingKey) keySignal.dispatch(k);
				} else {
					keySignal.dispatch(k);
				}
			} finally {
				if (tracedBack) BackTrace.endDispatch(k.backTraceId);
			}
		}
		keyEvents.clear();
	}

	public static boolean isKeyboardKey(int keyCode){
		return keyCode == 0 || !ControllerHandler.icControllerKey(keyCode);
	}
}
