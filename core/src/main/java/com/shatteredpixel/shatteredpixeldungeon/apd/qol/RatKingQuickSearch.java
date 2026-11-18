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

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Bulk;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.RatKingRoom;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import java.util.*;

/** Floor-five-only, paid stationary action. Planning is pure; payment runs through the ordinary actor scheduler. */
public final class RatKingQuickSearch {
    private RatKingQuickSearch() { }
    public static boolean floorFive(Level level){return Dungeon.depth==5&&Dungeon.branch==0&&level instanceof SewerBossLevel;}
    public static boolean unlocked(Level level){
        if(!SPDSettings.ratKingQuickSearch()||!floorFive(level)||!((SewerBossLevel)level).apdGooDefeated)return false;
        if(level.mobs!=null)for(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob:level.mobs)if(mob instanceof Goo&&mob.isAlive())return false;
        return true;
    }
    /** Never called outside the explicitly enabled, defeated-Goo Floor 5 exception. */
    public static int entrance(Level level){
        if(!unlocked(level)||!((RegularLevel)level).apdHasRooms())return -1;
        Room rat=((RegularLevel)level).room(RatKingRoom.class);if(rat==null)return -1;
        int found=-1;
        for(int y=rat.top;y<=rat.bottom;y++)for(int x=rat.left;x<=rat.right;x++){
            if(x!=rat.left&&x!=rat.right&&y!=rat.top&&y!=rat.bottom)continue;
            int cell=x+y*level.width();if(cell<0||cell>=level.length())return -1;
            if(level.map[cell]==Terrain.SECRET_DOOR){if(found>=0)return -1;found=cell;}
        }
        return found;
    }
    public interface MoveCost { float delay(int from,int to,float elapsed); }
    public interface SearchPoint { boolean allowed(int cell,float elapsed); default float delay(int cell,float elapsed){return 2f;} }
    public static final class Quote {public final float walking,search,total;public Quote(float walking,float search){this.walking=walking;this.search=search;this.total=walking+search;}}
    /** Dijkstra for ordinary legal steps. Expiring speed bonuses have FIFO arrival costs. */
    public static float minimum(int width,boolean[] legal,int start,MoveCost cost,SearchPoint goal){return minimumPlan(width,legal,start,cost,goal).total;}
    public static Quote minimumPlan(int width,boolean[] legal,int start,MoveCost cost,SearchPoint goal){
        if(width<1||legal==null||start<0||start>=legal.length||!legal[start])return new Quote(Float.POSITIVE_INFINITY,2f);
        float[] best=new float[legal.length];Arrays.fill(best,Float.POSITIVE_INFINITY);best[start]=0;
        PriorityQueue<float[]> q=new PriorityQueue<>(Comparator.comparingDouble(a->a[1]));q.add(new float[]{start,0});
        while(!q.isEmpty()){
            float[] state=q.poll();int at=(int)state[0];float time=state[1];if(time!=best[at])continue;
            if(goal.allowed(at,time))return new Quote(time,goal.delay(at,time));
            int x=at%width,y=at/width;
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                if(dx==0&&dy==0)continue;int nx=x+dx,ny=y+dy;
                if(nx<0||nx>=width||ny<0)continue;int next=nx+ny*width;
                if(next>=legal.length||!legal[next])continue;
                float step=cost.delay(at,next,time);if(!Float.isFinite(step)||step<0)continue;
                float arrival=time+step;if(arrival<best[next]){best[next]=arrival;q.add(new float[]{next,arrival});}
            }
        }
        return new Quote(Float.POSITIVE_INFINITY,2f);
    }
    private static boolean active(Hero hero,Class<? extends Buff> type,float after){Buff b=hero.buff(type);return b!=null&&(!(b instanceof FlavourBuff)||b.cooldown()>after);}
    public static String unavailable(Hero hero){
        if(hero==null||!hero.isAlive()||!hero.ready)return "Wait until you can act.";
        if(hero.visibleEnemies()>0)return "An enemy is visible. Quick Search has not started.";
        if(hero.rooted||hero.paralysed>0||Dungeon.level.locked)return "You cannot perform Quick Search right now.";
        // Never invent a minimum for non-FIFO speed changes, movement-only clocks, or shifting ring/vision effects.
        if(hero.buff(Cripple.class)!=null||hero.buff(Slow.class)!=null||hero.buff(Chill.class)!=null||hero.buff(GreaterHaste.class)!=null
                ||hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass.timeFreeze.class)!=null
                ||hero.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle.TimeBubble.class)!=null
                ||hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower.naturesPowerTracker.class)!=null
                ||hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection.AuraBuff.class)!=null
                ||hero.buff(AdrenalineSurge.class)!=null&&hero.belongings.armor()!=null&&hero.belongings.armor().STRReq()>hero.STR()-hero.buff(AdrenalineSurge.class).boost()||hero.buff(EnhancedRings.class)!=null
                ||hero.buff(MagicImmune.class)!=null||hero.buff(Dread.class)!=null||hero.buff(Blindness.class)!=null
                ||hero.buff(Shadows.class)!=null||hero.buff(AscensionChallenge.class)!=null
                ||hero.buff(Momentum.class)!=null&&hero.buff(Momentum.class).speedMultiplier()!=1)
            return "Quick Search cannot price your temporary movement effect reliably. Wait for it to end, or switch Quick Search off to search normally.";
        return null;
    }
    private static float speed(Hero hero,Level level,int cell,float after){
        float value=hero.apdBaseWalkingSpeed();
        if(active(hero,Stamina.class,after))value*=1.5f;
        if(active(hero,Adrenaline.class,after))value*=2f;
        if(active(hero,Haste.class,after))value*=3f;
        value*=RingOfHaste.speedMultiplier(hero);
        if(hero.belongings.armor()!=null)value=hero.belongings.armor().speedFactor(hero,value);
        int flow=hero.glyphLevel(Flow.class),bulk=hero.glyphLevel(Bulk.class),swift=hero.glyphLevel(Swiftness.class);
        float proc=Armor.Glyph.genericProcChanceMultiplier(hero);
        if(flow>=0&&level.water[cell])value*=(2f+0.5f*flow)*proc;
        if(bulk>=0&&(level.map[cell]==Terrain.DOOR||level.map[cell]==Terrain.OPEN_DOOR))value*=proc/3f;
        if(swift>=0&&!enemyNear(level,cell))value*=(1.2f+0.04f*swift)*proc;
        return value;
    }
    private static boolean enemyNear(Level level,int cell){
        int[] distance=new int[level.length()];Arrays.fill(distance,-1);ArrayDeque<Integer> q=new ArrayDeque<>();q.add(cell);distance[cell]=0;
        while(!q.isEmpty()){
            int from=q.removeFirst();for(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob:level.mobs)
                if(mob.alignment==Char.Alignment.ENEMY&&mob.isAlive()&&mob.pos==from)return true;
            if(distance[from]>=3)continue;
            int x=from%level.width(),y=from/level.width();
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                int nx=x+dx,ny=y+dy;if(nx<0||nx>=level.width()||ny<0||ny>=level.height())continue;int next=nx+ny*level.width();
                if(distance[next]<0&&level.passable[next]){distance[next]=distance[from]+1;q.add(next);}
            }
        }return false;
    }
    private static boolean searchFrom(Hero hero,Level level,int from,int door,float after){
        boolean foresight=active(hero,Foresight.class,after);
        if(!WallSearchSession.footprint(hero,level,from,door,foresight))return false;
        if(foresight)return true;
        int sense=1;for(Buff buff:hero.buffs(MindVision.class))if(!(buff instanceof FlavourBuff)||buff.cooldown()>after)sense=Math.max(sense,((MindVision)buff).distance);
        if(active(hero,MagicalSight.class,after))sense=Math.max(sense,MagicalSight.DISTANCE);
        int dx=Math.abs(from%level.width()-door%level.width()),dy=Math.abs(from/level.width()-door/level.width());
        if(sense>1&&dx<=sense&&dy<=sense&&dx<=ShadowCaster.rounding[sense][dy])return true;
        float view=hero.viewDistance*(1f+0.25f*hero.pointsInTalent(Talent.FARSIGHT))*EyeOfNewt.visionRangeMultiplier();
        boolean[] fov=new boolean[level.length()];boolean[] blocking=level.losBlocking;
        if(hero.subClass==HeroSubClass.WARDEN){blocking=blocking.clone();for(int i=0;i<blocking.length;i++)if(level.map[i]==Terrain.HIGH_GRASS||level.map[i]==Terrain.FURROWED_GRASS)blocking[i]=false;}
        ShadowCaster.castShadow(from%level.width(),from/level.width(),level.width(),fov,blocking,Math.round(view));return fov[door];
    }
    public static float quote(Hero hero,Level level,int door){return plan(hero,level,door).total;}
    public static Quote plan(Hero hero,Level level,int door){
        if(hero==null||level==null||door<0||door>=level.length())return new Quote(Float.POSITIVE_INFINITY,2f);
        boolean[] legal=new boolean[level.length()];
        for(int cell=0;cell<legal.length;cell++)legal[cell]=level.passable[cell]&&!level.pit[cell];
        legal[hero.pos]=true;legal[door]=false; // Never enter the undiscovered room or borrow a hidden-door shortcut.
        return minimumPlan(level.width(),legal,hero.pos,(from,to,time)->1f/speed(hero,level,to,time)/(active(hero,Speed.class,time)?2f:1f),new SearchPoint(){
            public boolean allowed(int cell,float time){return searchFrom(hero,level,cell,door,time);}
            public float delay(int cell,float time){return active(hero,Speed.class,time)?1f:2f;}
        });
    }
    public static boolean start(Hero hero){
        int door=entrance(Dungeon.level);if(door<0){if(hero!=null)hero.ratSearch=null;return false;}
        String blocked=unavailable(hero);if(blocked!=null){GLog.w(blocked);return true;}
        Payment saved=hero.ratSearch;
        if(saved==null||saved.anchor!=hero.pos||!saved.matches(Dungeon.level,door)){
            Quote price=plan(hero,Dungeon.level,door);float cost=price.total;
            if(!Float.isFinite(cost)){GLog.w("No legitimate walking-and-search route reaches the Rat King's entrance.");return true;}
            saved=new Payment(door,cost,hero.pos,Dungeon.level.width(),Dungeon.level.height(),price.search);hero.ratSearch=saved;
        }
        hero.beginRatQuickSearch(saved);return true;
    }
    public static final class Payment {
        public final int door,anchor,width,height;public final float total,searchCost;
        public float remaining;public boolean hungerCharged;
        public Payment(int door,float cost,int anchor,int width,int height){this(door,cost,anchor,width,height,2f);}
        public Payment(int door,float cost,int anchor,int width,int height,float searchCost){this.door=door;total=remaining=cost;this.anchor=anchor;this.width=width;this.height=height;this.searchCost=searchCost;}
        public boolean matches(Level level,int currentDoor){return floorFive(level)&&currentDoor==door&&width==level.width()&&height==level.height();}
    }
    public static final class Action extends HeroAction {public final Level level;public final Payment payment;public Action(Level level,Payment payment){this.level=level;this.payment=payment;dst=payment.door;}}
    public static boolean advance(Hero hero,Action action){
        Level level=Dungeon.level;Payment p=action.payment;
        if(!hero.isAlive()){hero.ratSearch=null;hero.curAction=null;return false;}
        if(action.level!=level||!unlocked(level)||hero.pos!=p.anchor||hero.visibleEnemies()>0||hero.rooted||hero.paralysed>0||level.locked
                ||p.door<0||p.door>=level.length()||entrance(level)!=p.door||level.map[p.door]!=Terrain.SECRET_DOOR){hero.interrupt();hero.finishRatQuickSearch();return false;}
        if(p.remaining<=0){
            level.discover(p.door);level.mapped[p.door]=true;
            if(ShatteredPixelDungeon.scene() instanceof GameScene)ScrollOfMagicMapping.discover(p.door);GameScene.updateFog(p.door,1);
            hero.ratSearch=null;hero.curAction=null;GLog.i("Quick Search revealed only the Rat King's entrance.");hero.finishRatQuickSearch();return false;
        }
        if(!p.hungerCharged&&p.remaining<=p.searchCost){
            TalismanOfForesight.Foresight talisman=hero.buff(TalismanOfForesight.Foresight.class);
            Buff.affect(hero,Hunger.class).affectHunger(2f-(talisman!=null&&talisman.isCursed()?12f:6f));p.hungerCharged=true;
        }
        float duration=Math.min(1f,p.remaining);
        // Preserve the boundary so the ordinary manual-search hunger surcharge is applied exactly once.
        if(p.remaining>p.searchCost&&p.remaining-duration<p.searchCost)duration=p.remaining-p.searchCost;
        p.remaining=Math.max(0,p.remaining-duration);hero.spendAndNextConstant(duration);return false;
    }
    public static void store(Hero hero,Bundle bundle){
        Payment p=hero.ratSearch;if(p==null)return;Bundle b=new Bundle();b.put("door",p.door);b.put("anchor",p.anchor);b.put("width",p.width);b.put("height",p.height);b.put("total",p.total);b.put("search_cost",p.searchCost);b.put("remaining",p.remaining);b.put("hunger",p.hungerCharged);bundle.put("apd_rat_search_payment_v1",b);
    }
    public static void restore(Hero hero,Bundle bundle){
        hero.ratSearch=null;
        if(bundle==null||!bundle.contains("apd_rat_search_payment_v1"))return;
        Bundle b=bundle.getBundle("apd_rat_search_payment_v1");
        // A damaged partial payment must not create a valid-looking zero-valued
        // door, anchor or remaining time via Bundle's missing-field defaults.
        // search_cost alone remains optional for the original legacy payment format.
        if(b==null||!b.contains("width")||!b.contains("height")||!b.contains("door")
                ||!b.contains("anchor")||!b.contains("total")||!b.contains("remaining")
                ||!b.contains("hunger"))return;
        int w=b.getInt("width"),h=b.getInt("height"),door=b.getInt("door"),anchor=b.getInt("anchor");float total=b.getFloat("total"),left=b.getFloat("remaining"),phase=b.contains("search_cost")?b.getFloat("search_cost"):2f;
        if(w<1||h<1||(long)w*h>Integer.MAX_VALUE||door<0||door>=(long)w*h||anchor<0||anchor>=(long)w*h||!Float.isFinite(total)||!Float.isFinite(phase)||phase<=0||phase>2||total<phase||!Float.isFinite(left)||left<0||left>total)return;
        Payment p=new Payment(door,total,anchor,w,h,phase);p.remaining=left;p.hungerCharged=b.getBoolean("hunger");hero.ratSearch=p;
    }
}
