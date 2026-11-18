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
import com.watabou.noosa.ui.Cursor;
import com.watabou.utils.PointF;
import com.watabou.utils.Signal;

import java.util.ArrayList;
import java.util.HashMap;

public class PointerEvent {
    public final long diagnosticId=com.watabou.utils.SupportDiagnostics.nextInputId();
    public final long queuedNanos=System.nanoTime();
    public long sourceEventNanos; // GDX backend timestamp when available; zero means unknown.

	public enum Type {
		DOWN,
		UP,
		CANCEL,
		HOVER
	}

	//buttons
	public static final int NONE = -1;
	public static final int LEFT = Input.Buttons.LEFT;
	public static final int RIGHT = Input.Buttons.RIGHT;
	public static final int MIDDLE = Input.Buttons.MIDDLE;
	public static final int BACK = Input.Buttons.BACK; //currently unused
	public static final int FORWARD = Input.Buttons.FORWARD;//currently unused

	public PointF start;
	public PointF current;
	public int id;
	public Type type;
	public int button;
	public boolean handled; //for hover events, to ensure hover always ends even with overlapping elements
	//Set only by scrollable pages that share a gesture with child controls. A swipe is not a click.
	public boolean scrolled;

	public PointerEvent( int x, int y, int id, Type type){
		this(x, y, id, type, NONE);
	}

	public PointerEvent( int x, int y, int id, Type type, int button){
		if (Cursor.isCursorCaptured()){
			x = Game.width/2;
			y = Game.width/2;
		}
		start = current = new PointF(x, y);
		this.id = id;
		this.type = type;
		handled = false;
		this.button = button;
	}
	
	public void update( PointerEvent other ){
		this.current = other.current;
	}
	
	public void update( int x, int y ){
		current.set( x, y );
	}
	
	public PointerEvent up() {
		if (type == Type.DOWN) type = Type.UP;
		return this;
	}

	public PointerEvent cancel() {
		if (type == Type.DOWN) type = Type.CANCEL;
		return this;
	}

	public PointerEvent handle(){
		handled = true;
		return this;
	}
	
	// **********************
	// *** Static members ***
	// **********************
	
	private static Signal<PointerEvent> pointerSignal = new Signal<>( true );

	/** Move a dormant scene's listeners out of dispatch without changing their order. */
	public static synchronized Signal<PointerEvent> detachListeners(){
		return swapListeners(new Signal<>(true));
	}

	public static synchronized Signal<PointerEvent> swapListeners(Signal<PointerEvent> listeners){
		Signal<PointerEvent> previous = pointerSignal;
		pointerSignal = listeners;
		return previous;
	}
	
	public static void addPointerListener( Signal.Listener<PointerEvent> listener ){
		pointerSignal.add(listener);
	}
	
	public static void removePointerListener( Signal.Listener<PointerEvent> listener ){
		pointerSignal.remove(listener);
	}
	
	public static void clearListeners(){
		pointerSignal.removeAll();
	}
	
	// Accumulated pointer events
	private static ArrayList<PointerEvent> pointerEvents = new ArrayList<>();
	private static HashMap<Integer, PointerEvent> activePointers = new HashMap<>();

	/** A lifecycle boundary cancels held pointers; it must never turn them into clicks. */
	public static synchronized void discardPendingEvents(){
        if(!pointerEvents.isEmpty() || !activePointers.isEmpty())com.watabou.utils.SupportDiagnostics.event("input.discard","touch_queued="+pointerEvents.size()+" held="+activePointers.size());
		for (PointerEvent event : new ArrayList<>(activePointers.values())) pointerSignal.dispatch(event.cancel());
		activePointers.clear();
		pointerEvents.clear();
	}

	private static PointF lastHoverPos = new PointF();

	public static PointF currentHoverPos(){
		if (lastHoverPos.x == 0 && lastHoverPos.y == 0){
			lastHoverPos.x = Game.width/2;
			lastHoverPos.y = Game.height/2;
		}
		return lastHoverPos.clone();
	}

	public static void setHoverPos(PointF pos){
		lastHoverPos.set(pos);
	}
	
	public static synchronized void addPointerEvent( PointerEvent event ){
		//APD: a moving finger queues a DOWN every frame; tracing each one rewrote the diagnostic
		//file continuously while dragging. Trace only the first press, release and cancel.
		boolean dragStep = event.type == Type.DOWN && activePointers.containsKey(event.id)
				&& activePointers.get(event.id).type == Type.DOWN;
		if(event.type!=Type.HOVER && !dragStep)com.watabou.utils.SupportDiagnostics.input("queued","touch."+event.type,event.diagnosticId,event.queuedNanos,event.sourceEventNanos);
		pointerEvents.add( event );
	}

	public static synchronized void addIfExisting( PointerEvent event ){
		if (activePointers.containsKey(event.id)) {
			pointerEvents.add(event);
		}
	}

	public static boolean clearKeyboardThisPress = true;
	
	public static synchronized void processPointerEvents(){
		if (pointerEvents.isEmpty()){
			return;
		}

		//handle any hover events separately first as we may need to add drag events
		boolean hovered = false;
		for (PointerEvent p : pointerEvents){
			if (p.type == Type.HOVER){
				lastHoverPos.set(p.current);
				pointerSignal.dispatch(p);
				hovered = true;
			}
		}

		for (PointerEvent p : pointerEvents){
			if (p.type == Type.HOVER){
				continue;
			}
            //APD: a finger held down sends a DOWN every frame while it moves; trace and journal
            //only the first press, the release and cancels, not every drag step.
            boolean dragStep = p.type == Type.DOWN && activePointers.containsKey(p.id)
                    && activePointers.get(p.id).type == Type.DOWN;
            if (!dragStep) com.watabou.utils.SupportDiagnostics.input("dispatched","touch."+p.type,p.diagnosticId,p.queuedNanos,p.sourceEventNanos);
            if (!dragStep) com.watabou.utils.SupportActionJournal.record("touch."+p.type,
                    "id="+p.diagnosticId+" pointer="+p.id+" x="+(int)p.current.x+" y="+(int)p.current.y);
			clearKeyboardThisPress = true;
			if (activePointers.containsKey(p.id)){
				PointerEvent existing = activePointers.get(p.id);
				existing.current = p.current;
				if (existing.type == p.type){
					pointerSignal.dispatch( null );
				} else if (p.type == Type.DOWN) {
					pointerSignal.dispatch( existing );
				} else if (p.type == Type.UP){
					activePointers.remove(existing.id);
					pointerSignal.dispatch(existing.up());
				} else if (p.type == Type.CANCEL){
					activePointers.remove(existing.id);
					pointerSignal.dispatch(existing.cancel());
				}
			} else {
				if (p.type == Type.DOWN) {
					activePointers.put(p.id, p);
				}
				pointerSignal.dispatch(p);
			}
			if (clearKeyboardThisPress){
				//most press events should clear the keyboard
				Game.platform.setOnscreenKeyboardVisible(false, false);
			}
		}
		pointerEvents.clear();

		//add drag events for any emulated presses
		if (hovered && activePointers.containsKey(ControllerHandler.CONTROLLER_POINTER_ID)){
			Game.inputHandler.emulateDrag(ControllerHandler.CONTROLLER_POINTER_ID);
		}
	}

}


