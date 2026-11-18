/* ADHD Pixel Dungeon (C) 2026 Chris Charlton, GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevealedArea;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** The real thrown-weapon handler and real FOV, with only the heap drawing tail omitted. */
public class SeerShotBranchBindingTest {
    private static final class Arena extends Level {
        @Override protected boolean build() { return true; }
        @Override protected void createMobs() {}
        @Override protected void createItems() {}
    }

    private static final class LandingKnife extends ThrowingKnife {
        void land(Hero user, int cell) {
            Hero previousUser = curUser;
            // This existing flag skips Item.onThrow's final heap/sprite rendering only.
            spawnedForEffect = true;
            curUser = user;
            try {
                super.onThrow(cell);
            } finally {
                curUser = previousUser;
            }
        }
    }

    private Hero previousHero;
    private Level previousLevel;
    private int previousDepth, previousBranch, previousChallenges;
    private Field sceneField;
    private Object previousScene;
    private final Map<Field, Object> previousActorState = new LinkedHashMap<>();
    private Arena arena;
    private Hero hero;

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        previousHero = Dungeon.hero;
        previousLevel = Dungeon.level;
        previousDepth = Dungeon.depth;
        previousBranch = Dungeon.branch;
        previousChallenges = Dungeon.challenges;
        sceneField = field(GameScene.class, "scene");
        previousScene = sceneField.get(null);
        sceneField.set(null, null);
        for (String name : new String[]{"all", "chars", "ids", "current", "now", "nextID"}) {
            Field state = field(Actor.class, name);
            previousActorState.put(state, state.get(null));
        }
        field(Actor.class, "all").set(null, new HashSet<Actor>());
        field(Actor.class, "chars").set(null, new HashSet<Char>());
        field(Actor.class, "ids").set(null, new SparseArray<Actor>());
        field(Actor.class, "current").set(null, null);
        field(Actor.class, "now").setFloat(null, 0f);
        field(Actor.class, "nextID").setInt(null, 1);
        Dungeon.challenges = 0;
        Dungeon.depth = 12;

        arena = new Arena();
        arena.setSize(15, 15);
        arena.mobs = new HashSet<>();
        arena.blobs = new HashMap<>();
        arena.traps = new SparseArray<>();
        arena.heaps = new SparseArray<>();
        arena.plants = new SparseArray<>();
        arena.customTiles = new ArrayList<>();
        arena.customWalls = new ArrayList<>();
        Arrays.fill(arena.map, Terrain.EMPTY);
        for (int i = 0; i < 15; i++) {
            arena.map[at(i, 0)] = arena.map[at(i, 14)] = Terrain.WALL;
            arena.map[at(0, i)] = arena.map[at(14, i)] = Terrain.WALL;
        }
        arena.buildFlagMaps();
        arena.cleanWalls();
        Dungeon.level = arena;
        freshHero(HeroClass.WARRIOR, 1, 0);
    }

    @After public void restore() throws Exception {
        for (Map.Entry<Field, Object> entry : previousActorState.entrySet()) {
            entry.getKey().set(null, entry.getValue());
        }
        Dungeon.hero = previousHero;
        Dungeon.level = previousLevel;
        Dungeon.depth = previousDepth;
        Dungeon.branch = previousBranch;
        Dungeon.challenges = previousChallenges;
        if (sceneField != null) sceneField.set(null, previousScene);
        if (previousLevel != null) {
            PathFinder.setMapSize(previousLevel.width(), previousLevel.height());
        }
    }

    private int at(int x, int y) { return y * arena.width() + x; }

    private void freshHero(HeroClass heroClass, int rank, int branch) {
        for (Actor actor : Actor.all()) Actor.remove(actor);
        hero = new Hero();
        hero.heroClass = heroClass;
        Talent.initClassTalents(hero);
        hero.talents.get(2).put(Talent.SEER_SHOT, rank);
        hero.HP = hero.HT = 20;
        hero.pos = at(3, 7);
        hero.viewDistance = 2;
        hero.fieldOfView = arena.heroFOV;
        Dungeon.hero = hero;
        Dungeon.branch = branch;
        Actor.add(hero);
    }

    private boolean[] view() {
        boolean[] result = new boolean[arena.length()];
        arena.updateFieldOfView(hero, result);
        return result;
    }

    private void assertFootprint(int center, boolean expected, boolean[] view) {
        for (int offset : PathFinder.NEIGHBOURS9) {
            assertEquals("branch " + Dungeon.branch + ", rank "
                    + hero.pointsInTalent(Talent.SEER_SHOT) + ", cell " + (center + offset),
                    expected, view[center + offset]);
        }
    }

    private RevealedArea throwAt(int cell) {
        assertNull(Actor.findChar(cell));
        new LandingKnife().land(hero, cell);
        return hero.buff(RevealedArea.class);
    }

    private void assertFreshRevealAtEveryRank(int branch) {
        int center = at(9, 7);
        for (int rank = 1; rank <= 3; rank++) {
            freshHero(HeroClass.WARRIOR, rank, branch);
            boolean[] before = view();
            assertFootprint(center, false, before);
            RevealedArea reveal = throwAt(center);
            assertNotNull(reveal);
            // The observable FOV assertion comes before checking the recorded branch.
            boolean[] after = view();
            assertFootprint(center, true, after);
            for (int cell = 0; cell < after.length; cell++) {
                boolean inSquare = Math.abs(cell % arena.width() - 9) <= 1
                        && Math.abs(cell / arena.width() - 7) <= 1;
                assertEquals("extra visibility at " + cell, before[cell] || inSquare, after[cell]);
            }
            assertEquals(branch, reveal.branch);
            assertEquals(Dungeon.depth, reveal.depth);
            assertEquals(center, reveal.pos);
            assertEquals(5f * rank, reveal.cooldown(), 0f);
            assertEquals(20f, hero.buff(Talent.SeerShotCooldown.class).cooldown(), 0f);
            assertEquals(1, hero.buffs(RevealedArea.class).size());
        }
    }

    @Test public void sideBranchThrowRevealsTheActualThreeByThreeAreaAtEveryRank() {
        assertFreshRevealAtEveryRank(1);
    }

    @Test public void mainBranchThrowPreservesAreaDurationAndCooldownAtEveryRank() {
        assertFreshRevealAtEveryRank(0);
    }

    @Test public void savedRevealRemainsLimitedToItsOwnBranchAndDepth() {
        int center = at(9, 7);
        freshHero(HeroClass.WARRIOR, 3, 0);
        RevealedArea original = throwAt(center);
        Bundle saved = new Bundle();
        original.storeInBundle(saved);
        original.detach();
        RevealedArea loaded = new RevealedArea();
        loaded.restoreFromBundle(saved);
        assertTrue(loaded.attachTo(hero));
        assertEquals(15f, loaded.cooldown(), 0f);
        assertFootprint(center, true, view());

        Dungeon.branch = 1;
        assertFootprint(center, false, view());
        Dungeon.branch = 0;
        Dungeon.depth++;
        assertFootprint(center, false, view());
        Dungeon.depth--;
        assertFootprint(center, true, view());
    }

    @Test public void activeCooldownDoesNotMoveOrRefreshAnEarlierReveal() throws Exception {
        int originalCell = at(9, 7), laterCell = at(9, 11);
        RevealedArea original = throwAt(originalCell);
        Talent.SeerShotCooldown cooldown = hero.buff(Talent.SeerShotCooldown.class);
        field(Actor.class, "now").setFloat(null, 2f);
        Dungeon.branch = 1;
        assertSame(original, throwAt(laterCell));
        assertEquals(0, original.branch);
        assertEquals(originalCell, original.pos);
        assertEquals(3f, original.cooldown(), 0f);
        assertSame(cooldown, hero.buff(Talent.SeerShotCooldown.class));
        assertEquals(18f, cooldown.cooldown(), 0f);
        assertFootprint(originalCell, false, view());
        assertFootprint(laterCell, false, view());
        Dungeon.branch = 0;
        assertFootprint(originalCell, true, view());
        assertFootprint(laterCell, false, view());
    }

    @Test public void alternateThrowStillRequiresTalentDifferentClassAndEmptyCell() {
        int center = at(9, 7);
        freshHero(HeroClass.WARRIOR, 0, 1);
        assertNull(throwAt(center));
        assertNull(hero.buff(Talent.SeerShotCooldown.class));
        assertFootprint(center, false, view());

        freshHero(HeroClass.HUNTRESS, 3, 1);
        assertNull(throwAt(center));
        assertNull(hero.buff(Talent.SeerShotCooldown.class));
        assertFootprint(center, false, view());

        freshHero(HeroClass.WARRIOR, 3, 1);
        new LandingKnife().land(hero, hero.pos);
        assertNull(hero.buff(RevealedArea.class));
        assertNull(hero.buff(Talent.SeerShotCooldown.class));
        assertFootprint(center, false, view());
    }
}
