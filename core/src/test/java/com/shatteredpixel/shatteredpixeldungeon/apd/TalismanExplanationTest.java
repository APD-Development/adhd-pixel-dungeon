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
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.*;
import com.watabou.utils.Bundle;
import java.lang.reflect.*;
import org.junit.*;
import static org.junit.Assert.*;

public class TalismanExplanationTest {
    @Before public void start(){HeadlessTestSupport.start();}
    @Test public void publicRangeMatchesActualPrivateTargetCutoffAcrossUpgradesAndChargeStates()throws Exception{
        Field charge=Artifact.class.getDeclaredField("charge");charge.setAccessible(true);
        Method actual=TalismanOfForesight.class.getDeclaredMethod("maxDist");actual.setAccessible(true);
        Hero oldHero=Dungeon.hero;Level oldLevel=Dungeon.level;
        try{
            Dungeon.hero=null;Dungeon.level=null;
            for(int level=0;level<=10;level++)for(int stored:new int[]{0,4,5,6,17,50,99,100}){
                TalismanOfForesight item=new TalismanOfForesight();item.level(level);item.levelKnown=item.cursedKnown=true;charge.setInt(item,stored);
                Bundle before=new Bundle();item.storeInBundle(before);
                assertEquals((Float)actual.invoke(item),TalismanExplanation.range(item),0);
                String detail=ArtifactExplanation.details(item);
                assertTrue(detail.contains(TalismanExplanation.details(item)));assertTrue(ArtifactExplanation.summary(item).contains(TalismanExplanation.summary(item)));
                assertTrue(detail.contains((5+2*level)+" turns"));if(level<10)assertTrue(detail.contains((100+50*level)+" progress points"));else assertTrue(detail.contains("Maximum upgrade +10 reached"));
                Bundle after=new Bundle();item.storeInBundle(after);assertEquals(before.toString(),after.toString());
                assertNull(Dungeon.hero);assertNull(Dungeon.level);
            }
        }finally{Dungeon.hero=oldHero;Dungeon.level=oldLevel;}
    }
    @Test public void hiddenLevelAndCurseNeverExposeRangeOrPersonalEffect()throws Exception{
        for(int level:new int[]{0,3,10})for(boolean cursed:new boolean[]{false,true}){
            TalismanOfForesight item=new TalismanOfForesight();item.level(level);item.cursed=cursed;item.levelKnown=false;item.cursedKnown=false;
            assertTrue(Float.isNaN(TalismanExplanation.range(item)));assertEquals("",TalismanExplanation.summary(item));assertEquals("",TalismanExplanation.details(item));
            assertEquals("",ArtifactExplanation.summary(item));assertTrue(ArtifactExplanation.details(item).startsWith("Identify this artifact"));
        }
    }
}
