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
package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;

/** One batched tile layer below normal fog/blockers, with no input handlers. */
public final class SearchedWallTilemap extends DungeonTilemap {
    private final java.util.BitSet projected=new java.util.BitSet();
    private final java.util.BitSet refresh=new java.util.BitSet();
    public SearchedWallTilemap() {
        super("interfaces/apd_searched_wall.png");
        applyAppearance();
        map(Dungeon.level.map, Dungeon.level.width());
    }
    @Override protected com.watabou.noosa.NoosaScript script(){
        // Tilemap defaults to NoLighting, which intentionally ignores hardlight().
        return com.watabou.noosa.NoosaScript.get();
    }
    public void applyAppearance(){
        hardlight(com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallAppearance.color(
                com.shatteredpixel.shatteredpixeldungeon.SPDSettings.searchedWallColor()).rgb);
    }
    @Override public synchronized void updateMap(){
        super.updateMap();projected.clear();
        for(int p=0;p<data.length;p++)if(data[p]>=0)projected.set(p);
    }
    @Override public synchronized void updateMapCell(int cell){
        super.updateMapCell(cell);
        if(Dungeon.level.insideMap(cell)){
            for(int delta:com.watabou.utils.PathFinder.NEIGHBOURS9)rememberProjection(cell+delta);
        }else rememberProjection(cell);
    }
    private void rememberProjection(int cell){if(cell>=0&&cell<data.length)projected.set(cell,data[cell]>=0);}
    public synchronized void refreshKnownGeometry(){
        if(data==null||data.length!=Dungeon.level.length()||map!=Dungeon.level.map)return;
        // Include old drawn marks: an invalidated saved bit must not leave a ghost until a scene rebuild.
        // Reuse sparse bitsets so ordinary movement does not scan/allocate an entire floor.
        refresh.clear();refresh.or(projected);int width=Dungeon.level.width();
        for(int p=Dungeon.level.searchedWalls.nextMarked(0);p>=0;p=Dungeon.level.searchedWalls.nextMarked(p+1)){
            refresh.set(p);if(p>=width)refresh.set(p-width);
        }
        for(int p=refresh.nextSetBit(0);p>=0;p=refresh.nextSetBit(p+1))
            if(data[p]!=getTileVisual(p,map[p],false))updateMapCell(p);
    }
    @Override protected int getTileVisual(int pos, int tile, boolean flat) {
        return com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallMarkers.visual(Dungeon.level, pos);
    }
}
