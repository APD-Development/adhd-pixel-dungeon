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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.watabou.utils.GameSettings;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;

public class HealingConfirmationTest {
    @Test public void inclusiveBoundariesUseExactHpAndDoNotLeakUnknownPotions() {
        assertFalse(HealingConfirmation.required(true,-1,100,100));
        for(int threshold=0;threshold<=100;threshold++) {
            assertTrue(HealingConfirmation.required(true,threshold,threshold,100));
            if(threshold>0) assertFalse(HealingConfirmation.required(true,threshold,threshold-1,100));
            assertFalse(HealingConfirmation.required(false,threshold,100,100));
        }
        assertFalse(HealingConfirmation.required(true,50,1,0));
        assertFalse(HealingConfirmation.required(true,50,1,3));
        assertTrue(HealingConfirmation.required(true,50,2,3));
        assertTrue(HealingConfirmation.required(true,100,Integer.MAX_VALUE,Integer.MAX_VALUE));
    }
    @Test public void preferenceDefaultsToOneTapAndSurvivesReloadWithoutTouchingRuns() throws Exception {
        withPreferences(file->{
            assertEquals(-1,SPDSettings.healingConfirmationThreshold());
            for(int value:new int[]{0,50,100,-1}) {
                SPDSettings.healingConfirmationThreshold(value);
                GameSettings.set(new HeadlessPreferences(new FileHandle(file)));
                assertEquals(value,SPDSettings.healingConfirmationThreshold());
            }
        });
    }
    @Test public void cancellingConfirmationDoesNotConsumeItemOrTurn() throws Exception {
        withPreferences(file->{
            SPDSettings.healingConfirmationThreshold(50);
            Hero old=Dungeon.hero; Hero hero=new Hero(); Dungeon.hero=hero;
            try {
                hero.HP=hero.HT=20; hero.ready=true;
                CapturedPotion potion=new CapturedPotion(); 
                hero.belongings.backpack.items.add(potion);
                float before=hero.cooldown();
                potion.execute(hero,PotionOfHealing.AC_DRINK);
                assertNotNull(potion.confirmed);
                assertTrue(hero.belongings.backpack.contains(potion));
                assertEquals(1,potion.quantity()); assertEquals(before,hero.cooldown(),0f);
                assertEquals(20,hero.HP); assertTrue(hero.ready);
                // Closing/cancelling the native popup does not invoke the captured continuation.
            } finally { Dungeon.hero=old; }
        });
    }
    @Test public void categoryHotbarUsesOneTapOnlyForKnownHealingWhenOff() {
        assertTrue(HealingConfirmation.categoryDirectDrink(true, -1));
        assertFalse(HealingConfirmation.categoryDirectDrink(false, -1));
        for (int threshold=0; threshold<=100; threshold++) {
            assertFalse(HealingConfirmation.categoryDirectDrink(true, threshold));
        }
    }
    @Test public void offBypassesConfirmationAndDrinksThroughRealPotionExecute() throws Exception {
        withPreferences(file -> {
            SPDSettings.healingConfirmationThreshold(-1);
            Hero previous=Dungeon.hero; Hero hero=new Hero(); Dungeon.hero=hero;
            try {
                hero.HT=20; hero.HP=10; hero.ready=true;
                CapturedPotion potion=new CapturedPotion() {
                    @Override protected void drink(Hero h) { confirmedDrinkCount++; }
                };
                java.lang.reflect.Field selector=com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("cellSelector");
                selector.setAccessible(true);
                Object previousSelector=selector.get(null);
                java.lang.reflect.Field field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                sun.misc.Unsafe unsafe=(sun.misc.Unsafe)field.get(null);
                selector.set(null,unsafe.allocateInstance(com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector.class));
                try {
                    potion.execute(hero,PotionOfHealing.AC_DRINK);
                    assertNull(potion.confirmed);
                    assertEquals(1,potion.confirmedDrinkCount);
                } finally { selector.set(null,previousSelector); }
                assertEquals(-1,SPDSettings.healingConfirmationThreshold());
            } finally { Dungeon.hero=previous; }
        });
    }
    @Test public void offQuickslotReturnsTheOneKnownHealingStackWithoutUsingIt() {
        CapturedPotion potion=new CapturedPotion();potion.quantity(3);
        assertSame(potion,HealingConfirmation.directQuickslotPotion(java.util.Collections.singletonList(potion),-1));
        assertEquals(3,potion.quantity());assertNull(potion.confirmed);
    }
    @Test public void ambiguousAndNonHealingQuickslotsRetainTheirChooser() {
        CapturedPotion potion=new CapturedPotion();
        assertNull(HealingConfirmation.directQuickslotPotion(java.util.Collections.emptyList(),-1));
        assertNull(HealingConfirmation.directQuickslotPotion(java.util.Arrays.asList(potion,new CapturedPotion()),-1));
        assertNull(HealingConfirmation.directQuickslotPotion(java.util.Collections.singletonList(new com.shatteredpixel.shatteredpixeldungeon.items.food.Food()),-1));
    }
    @Test public void unknownOrConfirmationEnabledPotionsNeverUseTheOffShortcut() {
        CapturedPotion unknown=new CapturedPotion(){@Override public boolean isKnown(){return false;}};
        assertNull(HealingConfirmation.directQuickslotPotion(java.util.Collections.singletonList(unknown),-1));
        for(int threshold=0;threshold<=100;threshold++)
            assertNull(HealingConfirmation.directQuickslotPotion(java.util.Collections.singletonList(new CapturedPotion()),threshold));
    }
    private static class CapturedPotion extends PotionOfHealing {
        Runnable confirmed;
        int confirmedDrinkCount;
        CapturedPotion() { anonymous = true; }
        @Override protected void requestHealingConfirmation(Hero hero,Runnable confirmed){this.confirmed=confirmed;}
    }
    private interface Check { void run(File file) throws Exception; }
    private static void withPreferences(Check check) throws Exception {
        HeadlessTestSupport.start();
        java.lang.reflect.Field f=GameSettings.class.getDeclaredField("prefs");f.setAccessible(true);
        Preferences old=(Preferences)f.get(null);
        File file=File.createTempFile("healing-confirmation-",".xml",HeadlessTestSupport.tempDir());file.delete();
        try { GameSettings.set(new HeadlessPreferences(new FileHandle(file)));check.run(file); }
        finally { GameSettings.set(old);file.delete(); }
    }
}
