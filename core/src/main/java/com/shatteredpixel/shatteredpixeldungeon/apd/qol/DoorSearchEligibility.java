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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;

/** Public generation rules and observed terrain only. Never asks whether a secret actually exists. */
public final class DoorSearchEligibility {
    private DoorSearchEligibility() { }
    public static boolean wallDisguise(int tile) {
        return tile==Terrain.WALL || tile==Terrain.SECRET_DOOR;
    }
    public static boolean supportsDoors(Level level,int depth,int branch) {
        if(level==null)return false;
        // These public main-dungeon floors have no hidden-door generator. Floor 5 is deliberately NOT excluded.
        if(branch==0&&(depth==1||depth==10||depth==15||depth==20||depth==25||depth==26))return false;
        // Scripted maps and the vault have no hidden doors. Mining paints concealed connections as diggable WALL, not SECRET_DOOR.
        return !(level instanceof PrisonBossLevel || level instanceof CavesBossLevel || level instanceof CityBossLevel
                || level instanceof HallsBossLevel || level instanceof LastLevel || level instanceof DeadEndLevel
                || level instanceof VaultLevel || level instanceof MiningLevel);
    }
    public static boolean supportsDoors(Level level){return supportsDoors(level,Dungeon.depth,Dungeon.branch);}
    /** A search made from inside a gated special room must not create wall checkmarks.
     * Uses the hero's current room, not the searched wall's tile or hidden map data.
     * The room type survives opening its original keyed/barricaded entrance. */
    public static boolean insideGatedRoom(Level level,int heroPos) {
        if (!(level instanceof RegularLevel) || !((RegularLevel)level).apdHasRooms()
                || heroPos<0 || heroPos>=level.length()) return false;
        Room room=((RegularLevel)level).room(heroPos);
        if (!(room instanceof SpecialRoom)) return false;
        Room.Door entrance=((SpecialRoom)room).entrance();
        return entrance!=null && (entrance.type==Room.Door.Type.LOCKED
                || entrance.type==Room.Door.Type.CRYSTAL
                || entrance.type==Room.Door.Type.BARRICADE);
    }
    private static boolean known(Level l,int p){return p>=0&&p<l.length()&&(l.heroFOV[p]||l.visited[p]||l.mapped[p]);}
    private static boolean frame(Level l,int p){
        if(!known(l,p))return true;
        int t=l.map[p];
        // An existing doorway is an opening, not the solid side of another doorway.
        // Otherwise the wall end directly below a door looks eligible until its backing is explored.
        return wallDisguise(t)||t==Terrain.WALL_DECO;
    }
    private static boolean passage(Level l,int p){
        // Unobserved terrain is unknown, never a licence to inspect the actual map behind the wall.
        int w=l.width();
        if(p<w||p>=l.length()-w||p%w==0||p%w==w-1)return false;
        if(!known(l,p))return true;
        int t=l.map[p];
        // Ordinary room/connection entrances have an interior tile between successive doorways.
        // A visible door immediately in front/behind is not an interior passage tile.
        return (Terrain.flags[t]&Terrain.SOLID)==0&&t!=Terrain.DOOR&&t!=Terrain.OPEN_DOOR&&t!=Terrain.HERO_LKD_DR;
    }
    /** Keeps uncertainty separate from proof that the public geometry rules a doorway out.
     * Used only to preserve a previously selected investigation, never to advertise a mark. */
    public static boolean couldHaveDoor(Level l,int p){
        if(!supportsDoors(l)||!known(l,p)||!wallDisguise(l.map[p]))return false;
        int w=l.width();
        if(w<3||p<w||p>=l.length()-w||p%w==0||p%w==w-1)return false;
        return frame(l,p-1)&&frame(l,p+1)&&passage(l,p-w)&&passage(l,p+w)
                ||frame(l,p-w)&&frame(l,p+w)&&passage(l,p-1)&&passage(l,p+1);
    }
    /** Positive doorway feedback needs observed side frames and an observed approach.
     * The far side may remain unknown; its actual hidden terrain is never inspected. */
    public static boolean candidate(Level l,int p){
        if(!couldHaveDoor(l,p))return false;
        int w=l.width();
        return observedOrientation(l,p-1,p+1,p-w,p+w)
                ||observedOrientation(l,p-w,p+w,p-1,p+1);
    }
    private static boolean observedOrientation(Level l,int firstFrame,int secondFrame,int firstPassage,int secondPassage){
        return known(l,firstFrame)&&known(l,secondFrame)&&frame(l,firstFrame)&&frame(l,secondFrame)
                &&passage(l,firstPassage)&&passage(l,secondPassage)
                &&(known(l,firstPassage)||known(l,secondPassage));
    }

}
