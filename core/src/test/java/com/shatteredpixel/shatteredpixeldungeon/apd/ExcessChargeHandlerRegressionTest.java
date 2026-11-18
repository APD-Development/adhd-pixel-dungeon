/* ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ScrollEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;
import java.lang.reflect.Field;
import java.util.HashSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Calls Wand.wandUsed, not a copied shield formula. Production is unchanged. */
public class ExcessChargeHandlerRegressionTest {
    private Hero oldHero;
    private Object oldUser, oldItem;
    private Field currentUser, currentItem;
    private HashSet<Actor> oldActors;
    private boolean oldItemRefresh;
    private TestHero hero;

    public static class TestHero extends Hero {
        int nextCalls;
        @Override public void next() { nextCalls++; }
        @Override public void interrupt() { }
    }

    public static class QuietSprite extends CharSprite {
        @Override public void add(State state) { }
        @Override public void remove(State state) { }
        @Override public void showStatusWithIcon(int color, String text, int icon, Object... args) { }
    }

    private static QuietSprite quietSprite() throws Exception {
        // Existing headless tests use this to avoid constructing GL resources.
        Field field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        return (QuietSprite) unsafe.getClass().getMethod("allocateInstance", Class.class)
                .invoke(unsafe, QuietSprite.class);
    }

    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        oldHero = Dungeon.hero;
        oldActors = Actor.all();
        oldItemRefresh = GameScene.updateItemDisplays;
        currentUser = Item.class.getDeclaredField("curUser");
        currentItem = Item.class.getDeclaredField("curItem");
        currentUser.setAccessible(true);
        currentItem.setAccessible(true);
        oldUser = currentUser.get(null);
        oldItem = currentItem.get(null);
        freshHero(2);
    }

    @After public void restore() throws Exception {
        for (Actor actor : Actor.all()) if (!oldActors.contains(actor)) Actor.remove(actor);
        Dungeon.hero = oldHero;
        currentUser.set(null, oldUser);
        currentItem.set(null, oldItem);
        GameScene.updateItemDisplays = oldItemRefresh;
    }

    private void freshHero(int rank) throws Exception {
        for (Actor actor : Actor.all()) if (!oldActors.contains(actor)) Actor.remove(actor);
        hero = new TestHero();
        hero.heroClass = HeroClass.MAGE;
        hero.subClass = HeroSubClass.BATTLEMAGE;
        Talent.initClassTalents(hero);
        Talent.initSubclassTalents(hero);
        hero.talents.get(2).put(Talent.EXCESS_CHARGE, rank);
        hero.HP = hero.HT = 20;
        hero.sprite = quietSprite();
        Dungeon.hero = hero;
    }

    private WandOfFrost staffWand(int level) throws Exception {
        WandOfFrost wand = new WandOfFrost();
        wand.levelKnown = wand.cursedKnown = wand.curChargeKnown = true;
        MagesStaff staff = new MagesStaff();
        // Install the real wand without invoking identification/catalog side effects.
        Field imbued = MagesStaff.class.getDeclaredField("wand");
        imbued.setAccessible(true);
        imbued.set(staff, wand);
        staff.level(level);
        staff.updateWand(false);
        hero.belongings.weapon = staff;
        staff.applyWandChargeBuff(hero);
        wand.curCharges = wand.maxCharges;
        wand.setCurrent(hero);
        return wand;
    }

    private int shield() {
        Barrier barrier = hero.buff(Barrier.class);
        return barrier == null ? 0 : barrier.shielding();
    }

    private void useAndCheckNormalCost(WandOfFrost wand) {
        int charges = wand.curCharges;
        float time = hero.cooldown();
        int callbacks = hero.nextCalls;
        wand.wandUsed();
        assertEquals(charges - 1, wand.curCharges);
        assertEquals(time + 1, hero.cooldown(), 0f);
        assertEquals(callbacks + 1, hero.nextCalls);
        assertEquals(20, hero.HP);
    }

    @Test public void realHandlerUsesExactPointSixtySevenFactorAtAllRanks() throws Exception {
        int[][] cases = {{0, 0, 0, 0}, {1, 1, 1, 2}, {2, 1, 3, 4},
                         {5, 3, 7, 10}, {25, 17, 34, 50}, {50, 34, 67, 101}};
        for (int rank = 1; rank <= 3; rank++) for (int[] row : cases) {
            freshHero(rank);
            WandOfFrost wand = staffWand(row[0]);
            assertEquals(row[0], wand.buffedLvl());
            useAndCheckNormalCost(wand);
            assertEquals("effective level " + row[0] + ", rank " + rank, row[rank], shield());
        }
    }

    @Test public void fullAndOverchargedStaffQualifyBeforeChargeIsSpent() throws Exception {
        for (int extra : new int[]{-1, 0, 2}) {
            freshHero(2);
            WandOfFrost wand = staffWand(25);
            wand.curCharges = wand.maxCharges + extra;
            useAndCheckNormalCost(wand);
            assertEquals(extra < 0 ? 0 : 34, shield());
            if (extra < 0) assertNull(hero.buff(Barrier.class));
        }
    }

    @Test public void standaloneUnchargedAndForeignOwnedWandsDoNotGrantShielding() throws Exception {
        for (int scope = 0; scope < 3; scope++) {
            freshHero(2);
            WandOfFrost wand = staffWand(25);
            if (scope == 0) {
                // Contains() distinguishes an ordinary carried wand from an imbued wand.
                hero.belongings.backpack.items.add(wand);
            } else if (scope == 1) {
                wand.stopCharging();
            } else {
                wand.stopCharging();
                TestHero other = new TestHero();
                wand.charge(other);
            }
            useAndCheckNormalCost(wand);
            assertEquals(0, shield());
            assertNull(hero.buff(Barrier.class));
        }
    }

    @Test public void effectiveWandBonusAndStrongerBarrierArePreservedWithoutExtraRng() throws Exception {
        WandOfFrost wand = staffWand(1);
        ScrollEmpower empower = Buff.affect(hero, ScrollEmpower.class);
        empower.reset(3);
        assertEquals(1, hero.belongings.weapon.trueLevel());
        assertEquals(3, wand.buffedLvl());
        Random.pushGenerator(433111);
        long expected;
        try { expected = Random.Long(); } finally { Random.popGenerator(); }
        Random.pushGenerator(433111);
        try {
            useAndCheckNormalCost(wand);
            assertEquals(4, shield());
            assertEquals(2, empower.remainingUses());
            hero.buff(Barrier.class).setShield(80);
            wand.curCharges = wand.maxCharges;
            useAndCheckNormalCost(wand);
            assertEquals(80, shield());
            assertEquals(1, empower.remainingUses());
            assertEquals(1, hero.belongings.weapon.trueLevel());
            assertEquals(expected, Random.Long());
        } finally { Random.popGenerator(); }
    }
}
