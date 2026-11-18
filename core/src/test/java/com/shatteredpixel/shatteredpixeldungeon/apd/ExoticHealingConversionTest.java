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

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals;
import com.watabou.utils.Random;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;

public class ExoticHealingConversionTest {
    private Hero hero;
    private boolean known;
    private int exotic, preference, depth;
    @Before public void setup() {
        HeadlessTestSupport.start(); Badges.loadGlobal(); hero = Dungeon.hero; known = Dungeon.apdExoticKnown;
        depth = Dungeon.depth; Dungeon.depth = 1; // Conversion fixture must use an eligible spawn depth.
        exotic = Dungeon.apdExotic; preference = SPDSettings.apdExoticSetting();
        Random.pushGenerator(41);
        try { Potion.initColors(); Scroll.initLabels(); } finally { Random.popGenerator(); }
        Dungeon.apdExoticKnown = true;
    }
    @After public void restore() {
        Dungeon.depth = depth; Dungeon.hero = hero; Dungeon.apdExoticKnown = known; Dungeon.apdExotic = exotic;
        SPDSettings.apdExoticChance(preference);
    }
    private void trinket(int level) {
        Dungeon.hero = new Hero();
        if (level >= 0) {
            ExoticCrystals crystals = new ExoticCrystals(); crystals.level(level);
            assertTrue(crystals.collect());
        }
    }
    private static class Sample { Class<?> type; long next; }
    private Sample generate(Class<?> type, boolean defaults, int percent, long seed) {
        Generator.Category category = Potion.class.isAssignableFrom(type) ? Generator.Category.POTION : Generator.Category.SCROLL;
        Class<?>[] classes = category.classes;
        float[] probs = category.probs, original = category.defaultProbs, second = category.defaultProbs2, total = category.defaultProbsTotal;
        Long deckSeed = category.seed; int dropped = category.dropped;
        Dungeon.apdExotic = percent;
        Random.pushGenerator(seed);
        try {
            category.classes = new Class<?>[]{type}; category.probs = new float[]{1};
            category.defaultProbs = new float[]{1}; category.defaultProbs2 = null; category.defaultProbsTotal = null;
            category.seed = null; category.dropped = 0;
            Item item = defaults ? Generator.randomUsingDefaults(category) : Generator.random(category);
            Sample sample = new Sample(); sample.type = item.getClass(); sample.next = Random.Long(); return sample;
        } finally {
            category.classes = classes; category.probs = probs; category.defaultProbs = original;
            category.defaultProbs2 = second; category.defaultProbsTotal = total; category.seed = deckSeed; category.dropped = dropped;
            Random.popGenerator();
        }
    }
    @Test public void sliderCannotAlterOtherPotionOrScrollConversionsOrTheirRng() {
        List<Class<?>> ordinary = new ArrayList<>();
        ordinary.addAll(ExoticPotion.regToExo.keySet()); ordinary.remove(PotionOfHealing.class);
        ordinary.addAll(ExoticScroll.regToExo.keySet());
        for (int level = -1; level <= 3; level++) {
            trinket(level);
            for (Class<?> type : ordinary) for (boolean defaults : new boolean[]{false,true}) for (int seed=0;seed<32;seed++) {
                Sample vanilla = generate(type, defaults, 0, seed), overridden = generate(type, defaults, 80, seed);
                assertEquals(type.getSimpleName()+" trinket "+level, vanilla.type, overridden.type);
                assertEquals("Unrelated conversions preserve next RNG", vanilla.next, overridden.next);
            }
        }
    }
    @Test public void healingConvertsAtTheSelectedFloorInBothGeneratorPathsWithoutExtraRng() {
        for (int level=-1;level<=3;level++) {
            trinket(level);
            for (int percent : new int[]{0,20,40,60,80}) for(boolean defaults : new boolean[]{false,true}) for(int seed=0;seed<64;seed++) {
                float chance = percent/100f;
                boolean shield; long next;
                Random.pushGenerator(seed);
                try { Random.chances(new float[]{1}); shield=Random.Float()<chance; next=Random.Long(); }
                finally { Random.popGenerator(); }
                Sample actual = generate(PotionOfHealing.class, defaults, percent, seed);
                assertEquals(shield ? PotionOfShielding.class : PotionOfHealing.class, actual.type);
                assertEquals("Conversion retains vanilla RNG draw count", next, actual.next);
            }
        }
    }
    @Test public void healingOverrideUsesFrozenRunSettingAndTrinketKeepsVanillaStats() {
        for(int level=-1;level<=3;level++) {
            trinket(level); Dungeon.apdExotic=80; SPDSettings.apdExoticChance(0);
            assertEquals(ExoticCrystals.consumableExoticChance(level), ExoticCrystals.consumableExoticChance(), 0);
            assertEquals(.8f, ExoticCrystals.consumableExoticChance(PotionOfHealing.class), .000001f);
            assertEquals(ExoticCrystals.consumableExoticChance(level), ExoticCrystals.consumableExoticChance(PotionOfFrost.class), 0);
        }
    }
    @Test public void zeroAndEverySelectedHealingChanceOverrideEveryCrystalsLevel() {
        for(int level=-1;level<=3;level++){
            trinket(level);
            for(int percent=0;percent<=80;percent++){
                Dungeon.apdExotic=percent;
                assertEquals(percent/100f,ExoticCrystals.consumableExoticChance(PotionOfHealing.class),.000001f);
            }
        }
    }
    @Test public void actualBatDropsUseZeroEvenWithFullyUpgradedCrystals() {
        int before=Dungeon.LimitedDrops.BAT_HP.count;
        try{
            Dungeon.apdExotic=0;
            for(int level=-1;level<=3;level++){
                trinket(level);
                for(int seed=0;seed<64;seed++){
                    Random.pushGenerator(seed);
                    try{assertEquals(PotionOfHealing.class,new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bat().createLoot().getClass());}
                    finally{Random.popGenerator();}
                }
            }
        }finally{Dungeon.LimitedDrops.BAT_HP.count=before;}
    }
    @Test public void actualBatConversionsUseExactSavedChanceAndOneOriginalRandomDraw() {
        int before=Dungeon.LimitedDrops.BAT_HP.count;
        try{
            for(int level=-1;level<=3;level++)for(int percent:new int[]{0,20,40,60,80})for(int seed=0;seed<64;seed++){
                trinket(level);Dungeon.apdExotic=percent;boolean expected;long next;
                Random.pushGenerator(seed);try{expected=Random.Float()<percent/100f;next=Random.Long();}finally{Random.popGenerator();}
                Random.pushGenerator(seed);try{
                    Item item=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bat().createLoot();
                    assertEquals(expected?PotionOfShielding.class:PotionOfHealing.class,item.getClass());
                    assertEquals("mob conversion keeps original RNG draw count",next,Random.Long());
                }finally{Random.popGenerator();}
            }
        }finally{Dungeon.LimitedDrops.BAT_HP.count=before;}
    }
    @Test public void absoluteConversionSeparatesAllKnownLiveRulesFromHistoricalIdentities() {
        for(int percent : new int[]{0,20,40,60,80}) {
            com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset legacy = com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset.of(percent,0);
            com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset corrected = com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset.withHealingOnlyConversion(legacy);
            assertEquals(percent,corrected.exoticChance);
            assertNotEquals(legacy.id,corrected.id);
            assertTrue(corrected.text.endsWith(";exotic-scope=healing-absolute-v2"));
            assertEquals(legacy.id,com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset.of(percent,0).id);
        }
        com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset unknown = com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset.unknown();
        assertSame(unknown,com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset.withHealingOnlyConversion(unknown));
    }
}
