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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.food.*;

/** Waiting preferences only. Enemy/manual interruptions and the actor clock remain authoritative. */
public final class WaitingPolicy {
    private WaitingPolicy(){}
    public enum Turn { CONTINUE, STOP, ATE }
    public static boolean critical(Hero hero){
        return hero!=null&&hero.isAlive()&&SPDSettings.waitStopCritical()
                &&(long)hero.HP*100<=(long)hero.HT*SPDSettings.waitCriticalPercent();
    }
    public static boolean safeFood(Food food){
        if(food==null||food.quantity()<=0||!food.isIdentified()||food.cursed||!Float.isFinite(food.energy)||food.energy<=0)return false;
        Class<?> type=food.getClass();
        // Exact classes intentionally exclude unknown subclasses, raw/enchanted/holiday/special food.
        return type==Food.class||type==SmallRation.class||type==ChargrilledMeat.class||type==StewedMeat.class;
    }
    public static Food availableFood(Hero hero){
        if(hero==null||hero.belongings==null)return null;
        Food best=null;
        for(Food food:hero.belongings.getAllItems(Food.class))
            if(safeFood(food)&&(best==null||food.energy<best.energy))best=food;
        return best;
    }
    public static boolean canAutoEat(Hero hero){
        Hunger hunger=hero==null?null:hero.buff(Hunger.class);
        if(hunger==null||!(SPDSettings.autoEatWhenHungry()?hunger.isHungry():hunger.isStarving()))return false;
        return canAutoEatAt(hero,SPDSettings.autoEatWhenHungry()?Hunger.HUNGRY:Hunger.STARVING);
    }
    private static boolean canAutoEatAt(Hero hero,float hungerLevel){
        if(hero==null||hero!=Dungeon.hero||!hero.resting||!hero.isAlive()||hero.paralysed>0
                ||hero.curAction!=null||hero.visibleEnemies()>0||!SPDSettings.autoEatWhileWaiting()||hero.buff(WellFed.class)!=null
                ||Dungeon.level==null||Dungeon.level.locked)return false;
        if(hero.buff(Hunger.class)==null||hungerLevel<(SPDSettings.autoEatWhenHungry()?Hunger.HUNGRY:Hunger.STARVING))return false;
        Food food=availableFood(hero);
        // Require positive normal nourishment; never consume a zero-energy item.
        return food!=null&&com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomResourceHooks.hungerEnergy(food.energy)>0;
    }
    /** Starvation damage/elapsed time already happened; never refund or bypass it. */
    public static boolean interruptOnStarving(Hero hero){
        return hero==null||!hero.resting||SPDSettings.waitStopStarving()&&!canAutoEatAt(hero,Hunger.STARVING);
    }
    public static boolean interruptOnStarvationDamage(Hero hero){
        return hero==null||!hero.resting||SPDSettings.waitStopStarving()&&!canAutoEatAt(hero,Hunger.STARVING);
    }
    public static Turn beforeWaitTurn(Hero hero){
        if(hero==null||!hero.resting||!hero.isAlive())return Turn.STOP;
        if(critical(hero))return Turn.STOP;
        if(canAutoEat(hero)){
            Food food=availableFood(hero);
            // The ordinary action detaches one unit, applies hunger/talents/statistics and pays eatingTime().
            // Item.execute cancels resting, so restore only this known-safe continuation after execution.
            if(!hero.resting)return Turn.STOP;
            long cancellation=hero.apdWaitCancellationEpoch;
            food.execute(hero,Food.AC_EAT);
            // Exactly one cancellation belongs to Item.execute. Never overwrite an additional user/danger interrupt.
            if(hero.isAlive()&&hero.curAction==null&&hero.apdWaitCancellationEpoch==cancellation+1)hero.resting=true;
            return Turn.ATE;
        }
        // A deferred starvation-onset interrupt must still fire if food vanished or became unavailable.
        Hunger hunger=hero.buff(Hunger.class);
        if(SPDSettings.autoEatWhileWaiting()&&SPDSettings.waitStopStarving()&&hunger!=null&&hunger.isStarving())return Turn.STOP;
        return Turn.CONTINUE;
    }
}
