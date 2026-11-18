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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Foresight;
import com.shatteredpixel.shatteredpixeldungeon.apd.navigation.StairNavigation;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.watabou.utils.*;

/** A remembered investigation, never a persisted active command or a hidden-map query. */
public final class WallSearchSession {
    private static final String KEY="apd_wall_search_session_v1";
    private int floor,branch,width,height,anchor=-1,target=-1,searches;
    public int anchor(){return anchor;}
    public int target(){return target;}
    public int floor(){return floor;}
    public int searches(){return searches;}
    public boolean exists(){return anchor>=0;}
    public void clear(){anchor=target=-1;searches=0;}
    public boolean matches(Level level){return exists()&&level!=null&&floor==Dungeon.depth&&branch==Dungeon.branch&&width==level.width()&&height==level.height()&&anchor<level.length();}
    public static boolean wall(int tile){return DoorSearchEligibility.wallDisguise(tile); }
    /** Same footprint arithmetic as Hero.search; unseen map contents are never consulted. */
    public static boolean covers(Hero hero,Level level,int from,int cell){
        if(hero==null||level==null||from<0||from>=level.length()||cell<0||cell>=level.length()||cell==from)return false;
        return covers(hero,level,from,cell,hero.buff(Foresight.class)!=null);
    }
    /** Contextual scanning may reuse a single already-observed Foresight state.
     * This still uses the exact ordinary Search footprint and visibility rules. */
    static boolean covers(Hero hero,Level level,int from,int cell,boolean foresight){
        if(hero==null||level==null||from<0||from>=level.length()||cell<0||cell>=level.length()||cell==from)return false;
        return footprint(hero,level,from,cell,foresight)
                && (foresight || (level.heroFOV != null && cell < level.heroFOV.length && level.heroFOV[cell]));
    }
    /** Maximum radius of the existing Hero.search footprint, not an expanded range.
     * Shared with contextual scans so ordinary classes do not inspect Rogue-only cells. */
    static int searchRadius(Hero hero, boolean foresight){
        if(foresight)return Foresight.DISTANCE;
        return (hero.heroClass==HeroClass.ROGUE?2:1)+(hero.hasTalent(Talent.WIDE_SEARCH)?1:0);
    }
    public static boolean footprint(Hero hero,Level level,int from,int cell,boolean foresight){
        if(hero==null||level==null||from<0||from>=level.length()||cell<0||cell>=level.length()||cell==from)return false;
        int distance=searchRadius(hero,foresight);
        boolean circular=hero.pointsInTalent(Talent.WIDE_SEARCH)==1;
        if(foresight)circular=true;
        int cx=from%level.width(),cy=from/level.width(),x=cell%level.width(),y=cell/level.width();
        if(Math.abs(y-cy)>distance)return false;
        int left;
        int[] rounding=ShadowCaster.rounding[distance];
        if(!circular)left=cx-distance;
        else if(rounding[Math.abs(cy-y)]<Math.abs(cy-y))left=cx-rounding[Math.abs(cy-y)];
        else{left=distance;while(rounding[left]<rounding[Math.abs(cy-y)])left--;left=cx-left;}
        int right=Math.min(level.width()-1,cx+cx-left);left=Math.max(0,left);
        return x>=left&&x<=right;
    }
    private void begin(Hero hero,int selected){
        Level l=Dungeon.level;floor=Dungeon.depth;branch=Dungeon.branch;width=l.width();height=l.height();anchor=hero.pos;target=selected;searches=0;
    }
    public boolean select(Hero hero,int cell){
        Level l=Dungeon.level;
        if(hero==null||!hero.isAlive()||!hero.ready||l==null||!DoorSearchEligibility.supportsDoors(l)||cell<0||cell>=l.length()||l.heroFOV==null||cell>=l.heroFOV.length||!l.heroFOV[cell]||!DoorSearchEligibility.candidate(l,cell)||!covers(hero,l,hero.pos,cell))return false;
        // Target selection is not a licence to search distant walls from a room center.
        if(SearchControlContext.noHiddenDoorRoom(l,hero.pos))return false;
        if(!matches(l)||anchor!=hero.pos||target!=cell)begin(hero,cell);return true;
    }
    public void searched(Hero hero){
        if(!DoorSearchEligibility.supportsDoors(Dungeon.level))return;
        if(!matches(Dungeon.level)||anchor!=hero.pos)begin(hero,-1);
        if(searches<Integer.MAX_VALUE)searches++;
    }
    public String resume(Hero hero){
        Level l=Dungeon.level;
        if(hero==null||!hero.isAlive()||!hero.ready)return "Wait until you can act.";
        if(!matches(l))return "The saved search is on another floor or is no longer available.";
        if(!DoorSearchEligibility.supportsDoors(l))return "Hidden doors do not generate on this floor. Ordinary Search still works.";
        if(SearchControlContext.noHiddenDoorRoom(l,anchor))return "Saved wall-search position is in a room that cannot hide a door. Ordinary Search remains available.";
        if(target>=0&&!DoorSearchEligibility.couldHaveDoor(l,target))return "The saved tile cannot be a doorway. Ordinary Search still works.";
        if(hero.visibleEnemies()>0)return "An enemy is visible. Your search position is still remembered.";
        if(hero.rooted||hero.paralysed>0||l.locked)return "You cannot return to the search position right now.";
        if(!StairNavigation.safeStep(l,hero,anchor))return "The saved search position is blocked or unsafe.";
        if(hero.pos==anchor){hero.interrupt();hero.lastAction=null;hero.search(true);return null;}
        boolean[] passable=new boolean[l.length()];for(int p=0;p<passable.length;p++)passable[p]=StairNavigation.safeStep(l,hero,p);
        passable[hero.pos]=true;
        PathFinder.Path path=Dungeon.findPath(hero,anchor,passable,hero.fieldOfView,true);
        if(path==null||path.isEmpty())return "There is no safe discovered route back to the search position.";
        hero.beginWallSearchWalk(new Walk(anchor,l),path);return null;
    }
    public static final class Walk extends HeroAction.Move {
        public final Level level;
        public Walk(int cell,Level level){super(cell,Mode.TRAVEL);this.level=level;}
    }
    public void store(Bundle bundle){
        if(!exists())return;Bundle b=new Bundle();b.put("floor",floor);b.put("branch",branch);b.put("width",width);b.put("height",height);b.put("anchor",anchor);b.put("target",target);b.put("searches",searches);bundle.put(KEY,b);
    }
    public void restore(Bundle bundle){
        clear();if(!bundle.contains(KEY))return;Bundle b=bundle.getBundle(KEY);
        for(String key:new String[]{"floor","branch","width","height","anchor","target","searches"})if(!b.contains(key))return;
        int w=b.getInt("width"),h=b.getInt("height"),a=b.getInt("anchor"),t=b.getInt("target");
        if(w<1||h<1||(long)w*h>Integer.MAX_VALUE||a<0||a>=(long)w*h||t<-1||t>=(long)w*h||b.getInt("floor")<1||b.getInt("branch")<0||b.getInt("searches")<0)return;
        floor=b.getInt("floor");branch=b.getInt("branch");width=w;height=h;anchor=a;target=t;searches=b.getInt("searches");
    }
}
