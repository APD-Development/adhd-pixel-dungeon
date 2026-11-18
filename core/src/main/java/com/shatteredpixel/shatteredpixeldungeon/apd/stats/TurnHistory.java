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

package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

//Pure-Java turn index and cursor for a recorded run. This is deliberately a view of the
//raw evidence: turns without events stay empty and events from rolled-back play are not
//silently presented as part of the canonical run.
public final class TurnHistory {

	private static final long[] PRESET_STEPS = { 1, 10, 100, 1000 };

	private final NavigableMap<Long, List<ApdEvent>> byTurn;
	private final List<ApdEvent> beforeFirstTurn;
	private final boolean hasRecordedTurns;
	private final long lastTurn;
	private long turn = 1;

	private TurnHistory( List<List<ApdEvent>> segments ){
		NavigableMap<Long, List<ApdEvent>> mutable = new TreeMap<>();
		List<ApdEvent> preamble = new ArrayList<>();
		for (List<ApdEvent> events : segments){
			if (events == null) throw new IllegalArgumentException("segment events are required");
			for (ApdEvent event : events){
				if (event == null) throw new IllegalArgumentException("event is required");
			}
			//Event indexes restart in each raw segment, so rollback resolution must happen
			//inside its own segment before their canonical events are combined by turn.
			for (ApdEvent event : RunTimeline.of(events).canonical()){
				if (event.clock < 1){
					preamble.add(event);
				} else {
					List<ApdEvent> at = mutable.get(event.clock);
					if (at == null){
						at = new ArrayList<>();
						mutable.put(event.clock, at);
					}
					at.add(event);
				}
			}
		}

		NavigableMap<Long, List<ApdEvent>> frozen = new TreeMap<>();
		for (Map.Entry<Long, List<ApdEvent>> entry : mutable.entrySet()){
			frozen.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
		}
		byTurn = Collections.unmodifiableNavigableMap(frozen);
		beforeFirstTurn = Collections.unmodifiableList(new ArrayList<>(preamble));
		hasRecordedTurns = !byTurn.isEmpty();
		lastTurn = hasRecordedTurns ? byTurn.lastKey() : 1;
	}

	public static TurnHistory from( ReadResult result ){
		if (result == null) throw new IllegalArgumentException("read result is required");
		return new TurnHistory(Collections.singletonList(result.events));
	}

	//Each raw segment keeps its own event indexes and rollback boundaries. Callers order
	//the segments oldest first; events on the same turn retain that segment/file order.
	public static TurnHistory fromSegments( List<ReadResult> segments ){
		if (segments == null) throw new IllegalArgumentException("segments are required");
		List<List<ApdEvent>> events = new ArrayList<>();
		for (ReadResult segment : segments){
			if (segment == null) throw new IllegalArgumentException("read result is required");
			events.add(segment.events);
		}
		return new TurnHistory(events);
	}

	//Useful for one raw log or for a caller that has already resolved rollback branches.
	public static TurnHistory fromEvents( List<ApdEvent> events ){
		if (events == null) throw new IllegalArgumentException("events are required");
		return new TurnHistory(Collections.singletonList(events));
	}

	public boolean hasRecordedTurns(){
		return hasRecordedTurns;
	}

	public long turn(){
		return turn;
	}

	public long firstTurn(){
		return 1;
	}

	public long lastTurn(){
		return lastTurn;
	}

	//Run-start/checkpoint facts at clock zero remain available without relabelling them as
	//Turn 1. The future screen can show these as run metadata.
	public List<ApdEvent> beforeFirstTurn(){
		return beforeFirstTurn;
	}

	public List<ApdEvent> events(){
		return eventsAt(turn);
	}

	public List<ApdEvent> eventsAt( long requestedTurn ){
		List<ApdEvent> found = byTurn.get(requestedTurn);
		return found == null ? Collections.<ApdEvent>emptyList() : found;
	}

	public boolean hasEvents(){
		return !events().isEmpty();
	}

	public TurnHistory goTo( long requestedTurn ){
		if (requestedTurn <= 1) turn = 1;
		else if (requestedTurn >= lastTurn) turn = lastTurn;
		else turn = requestedTurn;
		return this;
	}

	public TurnHistory forward( long turns ){
		checkStep(turns);
		if (turn >= lastTurn - Math.min(turns, lastTurn)) turn = lastTurn;
		else turn += turns;
		return this;
	}

	public TurnHistory back( long turns ){
		checkStep(turns);
		turn = turn <= turns ? 1 : turn - turns;
		return this;
	}

	public static boolean isPresetStep( long turns ){
		for (long preset : PRESET_STEPS) if (preset == turns) return true;
		return false;
	}

	public static long[] presetSteps(){
		return PRESET_STEPS.clone();
	}

	private static void checkStep( long turns ){
		if (turns < 1) throw new IllegalArgumentException("turn step must be positive");
	}
}
