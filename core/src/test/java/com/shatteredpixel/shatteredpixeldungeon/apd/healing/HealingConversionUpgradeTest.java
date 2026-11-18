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
package com.shatteredpixel.shatteredpixeldungeon.apd.healing;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.replay.ReplayContext;
import com.watabou.utils.Bundle;
import org.junit.*;
import static org.junit.Assert.*;
public class HealingConversionUpgradeTest {
    private int chance,preference;private boolean known;
    @Before public void setup(){HeadlessTestSupport.start();chance=Dungeon.apdExotic;known=Dungeon.apdExoticKnown;preference=SPDSettings.apdExoticSetting();Dungeon.apdExoticKnown=true;Dungeon.apdExotic=80;SPDSettings.apdExoticChance(0);}
    @After public void restore(){Dungeon.apdExotic=chance;Dungeon.apdExoticKnown=known;SPDSettings.apdExoticChance(preference);}
    @Test public void continuingAnOlderRunAdoptsTheSelectedZeroWithoutRestarting(){
        Bundle old=new Bundle();old.put("apd_exotic",80);Object rules=Dungeon.customRules();
        HealingConversionUpgrade.applyToLoadedRun(old);assertEquals(0,Dungeon.apdExotic);assertEquals(0,SPDSettings.apdExoticChance());assertSame(rules,Dungeon.customRules());
    }
    @Test public void savedUpgradeDoesNotReapplyLaterNewRunPreferenceChanges(){
        Bundle save=new Bundle();HealingConversionUpgrade.applyToLoadedRun(save);HealingConversionUpgrade.store(save);
        assertEquals(2,save.getInt(HealingConversionUpgrade.KEY));SPDSettings.apdExoticChance(80);
        HealingConversionUpgrade.applyToLoadedRun(save);assertEquals(0,Dungeon.apdExotic);assertEquals(80,SPDSettings.apdExoticSetting());
    }
    @Test public void unrecordedHistoricalRulesAreNotFalselyMadeKnown(){
        Dungeon.apdExoticKnown=false;HealingConversionUpgrade.applyToLoadedRun(new Bundle());assertFalse(Dungeon.apdExoticKnown);
    }
    @Test public void historicalReplayContextIsNeverMigrated()throws Exception{
        java.lang.reflect.Field f=ReplayContext.class.getDeclaredField("thread");f.setAccessible(true);Object before=f.get(null);
        try{f.set(null,Thread.currentThread());HealingConversionUpgrade.applyToLoadedRun(new Bundle());assertEquals(80,Dungeon.apdExotic);}finally{f.set(null,before);}
    }
    @Test public void adoptionDoesNotConsumeCombatOrGenerationRandomNumbers(){
        long expected;com.watabou.utils.Random.pushGenerator(78417);try{expected=com.watabou.utils.Random.Long();}finally{com.watabou.utils.Random.popGenerator();}
        com.watabou.utils.Random.pushGenerator(78417);try{HealingConversionUpgrade.applyToLoadedRun(new Bundle());assertEquals(expected,com.watabou.utils.Random.Long());}finally{com.watabou.utils.Random.popGenerator();}
    }
}
