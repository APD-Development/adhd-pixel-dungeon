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
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdItems;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ItemEvents;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import java.util.ArrayDeque;

/** Transient movement pickup queue. Every item still uses its ordinary pickup action. */
public final class GrassDewPickup {
    private Level level;
    private Heap heap;
    private final ArrayDeque<Item> items=new ArrayDeque<>();
    private HeroAction sourceAction;
    private int cell;

    /** Grass may generate a seed/stone and dew together; merge offers instead of losing either. */
    public void offer(Hero hero, Level level, int cell, Heap heap) {
        offer(hero,level,cell,heap,false);
    }

    /** Existing floor dew is eligible on a normal step, independently of grass generation. */
    public void offerDewOnStep(Hero hero, Level level, int cell, Heap heap) {
        offer(hero,level,cell,heap,true);
    }

    private void offer(Hero hero, Level level, int cell, Heap heap, boolean dewOnly) {
        if(hero==null || hero!=Dungeon.hero || level==null || level!=Dungeon.level
                || hero.pos!=cell || heap==null || heap.pos!=cell || heap.hidden
                || heap.type!=Heap.Type.HEAP || level.heaps==null || level.heaps.get(cell)!=heap)return;
        if(this.level!=level || this.heap!=heap || this.cell!=cell || sourceAction!=hero.curAction){
            clear();this.level=level;this.heap=heap;this.cell=cell;sourceAction=hero.curAction;
        }
        for(Item item:heap.items){
            if(item.quantity()>0 && (!dewOnly || item instanceof Dewdrop) && enabled(item)
                    && (!(item instanceof Dewdrop) || hasRoom(hero)) && !items.contains(item))items.add(item);
        }
        if(items.isEmpty())clear();
    }

    private static boolean enabled(Item item){
        // Preserve the existing saved on/off choice as the master pickup preference.
        return SPDSettings.autoCollectGrassDew() &&
                (item instanceof Dewdrop || item instanceof Plant.Seed || item instanceof Runestone);
    }

    public void clear() {
        level=null;heap=null;sourceAction=null;items.clear();
    }

    /** Called after movement/enemy turns and refreshed FOV; collect at most one stack per action. */
    public boolean collectIfSafe(Hero hero) {
        if(items.isEmpty()){clear();return false;}
        if(hero==null || hero!=Dungeon.hero || level!=Dungeon.level || !hero.isAlive()
                || hero.paralysed>0 || hero.resting || hero.pos!=cell
                || (hero.curAction!=null && hero.curAction!=sourceAction)
                || level.heaps==null || level.heaps.get(cell)!=heap || heap.type!=Heap.Type.HEAP || heap.hidden
                || (!SPDSettings.autoCollectDewWithEnemies() && hasVisibleEnemy(level))){
            clear();return false;
        }
        while(!items.isEmpty()){
            Item item=items.removeFirst(); // A failed/full pickup never loops or retries itself.
            if(!heap.items.contains(item) || item.quantity()<=0 || !enabled(item)
                    || item instanceof Dewdrop && !hasRoom(hero))continue;
            ApdItems.noteAcquireContext(ItemEvents.ACQ_PICKUP);
            if(!item.doPickUp(hero,cell))continue;
            heap.remove(item);
            if(!(item instanceof Dewdrop))com.shatteredpixel.shatteredpixeldungeon.utils.GLog.i(
                    com.shatteredpixel.shatteredpixeldungeon.messages.Messages.capitalize(
                    com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(hero,"you_now_have",item.name())));
            if(items.isEmpty())clear();
            return true;
        }
        clear();return false;
    }

    private static boolean hasRoom(Hero hero) {
        Waterskin skin = hero.belongings.getItem(Waterskin.class);
        return skin != null && !skin.isFull();
    }

    private static boolean hasVisibleEnemy(Level level) {
        // Same known-FOV/alignment criterion as Hero.checkVisibleMobs, without scanning unseen contents.
        for (Mob mob : level.mobs) {
            if (mob.alignment == Char.Alignment.ENEMY && mob.isAlive() && mob.pos >= 0
                    && mob.pos < level.heroFOV.length && level.heroFOV[mob.pos]) return true;
        }
        return false;
    }
}
