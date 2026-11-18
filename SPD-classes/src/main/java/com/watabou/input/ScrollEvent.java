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

import com.watabou.utils.PointF;
import com.watabou.utils.Signal;

import java.util.ArrayList;

public class ScrollEvent {
    public final long diagnosticId=com.watabou.utils.SupportDiagnostics.nextInputId();
    public final long queuedNanos=System.nanoTime();
    public long sourceEventNanos; // GDX backend timestamp when available; zero means unknown.
	
	public PointF pos;
	public float amount;
	
	public ScrollEvent(PointF mousePos, float amount){
		this.amount = amount;
		this.pos = mousePos;
	}
	
	// **********************
	// *** Static members ***
	// **********************
	
	private static Signal<ScrollEvent> scrollSignal = new Signal<>( true );
	
	public static void addScrollListener( Signal.Listener<ScrollEvent> listener ){
		scrollSignal.add(listener);
	}
	
	public static void removeScrollListener( Signal.Listener<ScrollEvent> listener ){
		scrollSignal.remove(listener);
	}
	
	public static void clearListeners(){
		scrollSignal.removeAll();
	}
	
	//Accumulated key events
	private static ArrayList<ScrollEvent> scrollEvents = new ArrayList<>();

	public static synchronized void discardPendingEvents(){
        if(!scrollEvents.isEmpty())com.watabou.utils.SupportDiagnostics.event("input.discard","wheel_queued="+scrollEvents.size());
        scrollEvents.clear();
    }
	
	public static synchronized void addScrollEvent( ScrollEvent event ){
        com.watabou.utils.SupportDiagnostics.input("queued","wheel",event.diagnosticId,event.queuedNanos,event.sourceEventNanos);
		scrollEvents.add( event );
	}
	
	public static synchronized void processScrollEvents(){
		if (scrollEvents.isEmpty()) {
			return;
		}

		for (ScrollEvent k : scrollEvents){
            com.watabou.utils.SupportDiagnostics.input("dispatched","wheel",k.diagnosticId,k.queuedNanos,k.sourceEventNanos);
			scrollSignal.dispatch(k);
		}
		scrollEvents.clear();
	}
	
}

