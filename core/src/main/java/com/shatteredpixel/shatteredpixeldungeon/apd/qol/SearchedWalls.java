/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
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
package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import java.util.BitSet;

/** Per-floor memory of deliberately searched, visible walls; no discovery or search rolls. */
public final class SearchedWalls {
    private static final String KEY = "apd_searched_walls_v1";
    private final BitSet cells = new BitSet();
    private final BitSet doorChecks = new BitSet();
    private static final String CHECKS_KEY="apd_door_checks_v1";

    public boolean mark(Level level, int cell) {
        if (!DoorSearchEligibility.supportsDoors(level) || cell < 0 || cell >= level.length()
                || level.map == null || cell >= level.map.length
                || level.heroFOV == null || cell >= level.heroFOV.length || !level.heroFOV[cell]
                || !wall(level.map[cell]) || cells.get(cell)) return false;
        cells.set(cell);
        return true;
    }

    /** Existing memories may be displayed from any *available* observed fog source.
     * Missing arrays never justify reading unobserved terrain or creating new marks. */
    public boolean shows(Level level, int cell) {
        return level != null && cell >= 0 && cell < level.length()
                && level.map != null && cell < level.map.length
                && cells.get(cell) && wall(level.map[cell]) && observed(level, cell);
    }

    private static boolean observed(Level level, int cell) {
        return (level.heroFOV != null && cell < level.heroFOV.length && level.heroFOV[cell])
                || (level.visited != null && cell < level.visited.length && level.visited[cell])
                || (level.mapped != null && cell < level.mapped.length && level.mapped[cell]);
    }

    /** Door geometry checks inspect adjacent observed tiles, not just the selected cell. */
    private static boolean completeObservation(Level level) {
        if (level == null) return false;
        int length = level.length();
        return level.map != null && level.map.length >= length
                && level.heroFOV != null && level.heroFOV.length >= length
                && level.visited != null && level.visited.length >= length
                && level.mapped != null && level.mapped.length >= length;
    }

    private static boolean wall(int tile) {
        // Secret doors look exactly like ordinary walls until the original search code discovers them.
        return DoorSearchEligibility.wallDisguise(tile);
    }

    /** Allocation-free traversal for refreshing only saved markers after observation. */
    public int nextMarked(int from){return cells.nextSetBit(Math.max(0,from));}
    public void clear() { cells.clear(); doorChecks.clear(); }
    public void invalidate(int cell){if(cell>=0){cells.clear(cell);doorChecks.clear(cell);}}
    public void recordCompletedDoorCheck(Level level,int cell,boolean guaranteed){
        if(level==null||cell<0||cell>=level.length())return;
        doorChecks.clear(cell);
        // A partial restoration cannot certify that a wall has no hidden door.
        // Keep the original paid-check semantics when all observed inputs are present.
        if(!completeObservation(level)||level.secret==null||level.secret.length<level.length())return;
        if(DoorSearchEligibility.supportsDoors(level)&&guaranteed&&level.heroFOV[cell]&&wall(level.map[cell])&&!level.secret[cell]
                &&level.map[cell]!=Terrain.SECRET_DOOR)doorChecks.set(cell);
    }
    public boolean confirmed(Level level,int cell){
        return completeObservation(level) && cell >= 0 && cell < level.length()
                && DoorSearchEligibility.candidate(level,cell) && level.heroFOV[cell] && doorChecks.get(cell);
    }
    public boolean attempted(int cell){return cell>=0&&cells.get(cell);}


    public void store(Bundle bundle) {
        // An untouched floor retains its original payload and level-generation fingerprints.
        if (!cells.isEmpty()) bundle.put(KEY, cells.stream().toArray());
        if (!doorChecks.isEmpty()) bundle.put(CHECKS_KEY,doorChecks.stream().toArray());
    }

    public void restore(Bundle bundle, int length) {
        clear();
        if (bundle.contains(CHECKS_KEY)) {
            // Bundle.getIntArray returns null when an existing save field is malformed.
            // Keep the other memory field independent rather than aborting floor load.
            int[] checks = bundle.getIntArray(CHECKS_KEY);
            if (checks != null) for (int cell : checks) {
                if (cell >= 0 && cell < length) doorChecks.set(cell);
            }
        }
        if (!bundle.contains(KEY)) return; // Legacy saves have no fabricated search history.
        int[] marks = bundle.getIntArray(KEY);
        if (marks == null) return;
        for (int cell : marks) {
            if (cell >= 0 && cell < length) cells.set(cell);
        }
    }
}
