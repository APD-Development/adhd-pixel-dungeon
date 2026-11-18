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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//Separates the events of a run log into the canonical timeline and abandoned branches.
//
//A ROLLBACK event says "the game was loaded from the save that matches SAVE marker S". Every game event with an event
//index between S and the ROLLBACK describes play that no longer exists (the app was killed, or an older save was
//reloaded), so it is ABANDONED. Abandoned events are never deleted and never merged into the canonical timeline:
//normal statistics use canonical(), history/forensic tools can use abandoned().
//
//Events that describe the recording itself (GAP, ROLLBACK, SESSION_START) are never abandoned.
//
//A ROLLBACK of kind UNMATCHED could not tell which events survived. Nothing is abandoned for it, and the events
//from there on are reported through uncertainFromIndex() so statistics can treat them with care.
public final class RunTimeline {

	private final List<ApdEvent> all;
	private final Set<Long> abandonedIndexes = new HashSet<>();
	private long uncertainFromIndex = -1;
	private final List<ApdEvent> rollbackEvents = new ArrayList<>();

	private RunTimeline( List<ApdEvent> all ){
		this.all = all;
	}

	//'events' must contain every event of the log (do not use ReadOptions.knownTypesOnly)
	public static RunTimeline of( List<ApdEvent> events ){
		RunTimeline t = new RunTimeline(events);
        List<long[]> ranges = new ArrayList<>();
        boolean ordered = true;
        long previous = Long.MIN_VALUE;
		for (ApdEvent e : events){
            if (e.index < previous) ordered = false;
            previous = e.index;
			if (e.type != EventTypes.ROLLBACK) continue;
			t.rollbackEvents.add(e);
			LifecycleEvents.Rollback r;
			try {
				r = LifecycleEvents.decodeRollback(e);
			} catch (FormatException bad){
				continue; //a rollback we cannot read abandons nothing
			}
			if (r.kind == LifecycleEvents.ROLLBACK_MATCHED && r.matchedSaveIndex >= 0){
                if (r.matchedSaveIndex < e.index) ranges.add(new long[]{r.matchedSaveIndex,e.index});
			} else if (r.kind == LifecycleEvents.ROLLBACK_UNMATCHED && t.uncertainFromIndex < 0){
				t.uncertainFromIndex = e.index;
			}
		}
        if (!ordered) {
            // Preserve the original behavior for callers supplying an unordered list.
            for (long[] range : ranges) for (ApdEvent event : events) {
                if (event.index <= range[0]) continue;
                if (event.index >= range[1]) break;
                if (!LifecycleEvents.isRecordingMetaEvent(event.type)) t.abandonedIndexes.add(event.index);
            }
        } else if (!ranges.isEmpty()) {
            java.util.Collections.sort(ranges, (a,b) -> Long.compare(a[0],b[0]));
            List<long[]> merged = new ArrayList<>();
            for (long[] range : ranges) {
                if (merged.isEmpty() || range[0] >= merged.get(merged.size()-1)[1]) merged.add(range);
                else {
                    long[] last=merged.get(merged.size()-1);
                    last[1]=Math.max(last[1],range[1]);
                }
            }
            int at=0;
            for (ApdEvent event : events) {
                while (at < merged.size() && event.index >= merged.get(at)[1]) at++;
                if (at == merged.size()) break;
                if (event.index > merged.get(at)[0] && !LifecycleEvents.isRecordingMetaEvent(event.type))
                    t.abandonedIndexes.add(event.index);
            }
        }
		return t;
	}

	public boolean isAbandoned( ApdEvent e ){
		return abandonedIndexes.contains(e.index);
	}

	//the events of the timeline that really happened, in order. This is what normal statistics use.
	public List<ApdEvent> canonical(){
		List<ApdEvent> out = new ArrayList<>();
		for (ApdEvent e : all) if (!abandonedIndexes.contains(e.index)) out.add(e);
		return out;
	}

	//the events of abandoned branches, in order
	public List<ApdEvent> abandoned(){
		List<ApdEvent> out = new ArrayList<>();
		for (ApdEvent e : all) if (abandonedIndexes.contains(e.index)) out.add(e);
		return out;
	}

	public int abandonedCount(){
		return abandonedIndexes.size();
	}

	public int rollbackCount(){
		return rollbackEvents.size();
	}

	//event index of the first UNMATCHED rollback, -1 if there is none
	public long uncertainFromIndex(){
		return uncertainFromIndex;
	}
}
