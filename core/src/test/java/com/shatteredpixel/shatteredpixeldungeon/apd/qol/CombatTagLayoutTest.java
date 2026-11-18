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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CombatTagLayoutTest {
    @Test public void stairsUseAbilityPositionExceptForDuelist() {
        for(HeroClass type:HeroClass.values())assertEquals(type==HeroClass.DUELIST?2:1,CombatTagLayout.stairRow(type==HeroClass.DUELIST));
    }
    @Test public void fourClericSlotsSitInTwoColumnsBesideAttack() {
        int[][] expected={{1,0},{2,0},{1,1},{2,1}};
        for(int i=0;i<4;i++)assertArrayEquals(expected[i],new int[]{CombatTagLayout.spellColumn(i),CombatTagLayout.spellRow(i)});
    }
    @Test public void allRolesRemainDistinctEvenWhenShortScreensWrapThem() {
        for(int rows=1;rows<=8;rows++)for(boolean cleric:new boolean[]{false,true})for(boolean duelist:new boolean[]{false,true}){
            List<int[]> slots=new ArrayList<>();slots.add(new int[]{0,0});slots.add(new int[]{0,CombatTagLayout.stairRow(duelist)});
            if(duelist)slots.add(new int[]{0,1});
            if(cleric)for(int i=0;i<4;i++)slots.add(new int[]{CombatTagLayout.spellColumn(i),CombatTagLayout.spellRow(i)});
            slots.add(new int[]{1,cleric?2:0});slots.add(new int[]{cleric?2:1,cleric?2:1});
            slots.add(new int[]{cleric?3:1,cleric?0:2});
            Set<String> occupied=new HashSet<>();
            for(int[] slot:slots){int[] placed=CombatTagLayout.fit(slot[0],slot[1],rows);assertTrue(Arrays.toString(placed),occupied.add(Arrays.toString(placed)));assertTrue(placed[1]>=0&&placed[1]<rows);}
        }
    }
    @Test public void fixedGridIsPreservedWhenThreeRowsFit() {
        for(int column=0;column<4;column++)for(int row=0;row<3;row++)assertArrayEquals(new int[]{column,row},CombatTagLayout.fit(column,row,10));
    }
    @Test public void alwaysVisibleAttackCannotActivateWithoutTargetOrHero() {
        HeadlessTestSupport.start();assertFalse(AttackIndicator.canActivate(null,null));
        for(HeroClass type:HeroClass.values()){Hero hero=new Hero();hero.heroClass=type;hero.ready=true;float before=hero.cooldown();assertFalse(AttackIndicator.canActivate(hero,null));assertEquals(before,hero.cooldown(),0f);}
    }
}
