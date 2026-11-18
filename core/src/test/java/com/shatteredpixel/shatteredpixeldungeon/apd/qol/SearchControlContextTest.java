/* ADHD Pixel Dungeon (C) 2026 Chris Charlton, GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Foresight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchControlContext.Action;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.CrystalChoiceRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.CrystalVaultRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ArmoryRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.LaboratoryRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.TreasuryRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.StorageRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.MagicWellRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ShopRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SacrificeRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.EmptyRoom;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ResumeIndicator;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndWallSearchResume;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** Real talent acquisition, eligibility, chooser dispatch and search costs; drawing is omitted. */
public class SearchControlContextTest {
    private static final int SIZE = 21;

    private static class Arena extends SewerLevel {
        void setRooms(Room... selected) { rooms = new ArrayList<>(Arrays.asList(selected)); }
    }

    public static class QuietSprite extends HeroSprite {
        @Override public void operate(int cell) { }
        @Override public void showStatus(int color, String text, Object... args) { }
    }

    private static class QuietHero extends Hero {
        int nextCalls;
        @Override public void next() { nextCalls++; }
    }

    private Hero oldHero;
    private Level oldLevel;
    private int oldDepth, oldBranch;
    private boolean oldTagDisappeared;
    private Field selector;
    private Object oldSelector;
    private final Map<Field, Object> oldActors = new LinkedHashMap<>();
    private Arena level;
    private QuietHero hero;
    private Method choose;

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        return type.cast(unsafe.getMethod("allocateInstance", Class.class)
                .invoke(field(unsafe, "theUnsafe").get(null), type));
    }

    private static int at(int x, int y) { return y * SIZE + x; }

    private boolean oldAdaptiveChoice;

    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        oldAdaptiveChoice = SPDSettings.adaptiveSearchChoice();
        SPDSettings.adaptiveSearchChoice(true); //off by default since Nov 18; these tests exercise it
        Random.pushGenerator(445202611L);
        oldHero = Dungeon.hero;
        oldLevel = Dungeon.level;
        oldDepth = Dungeon.depth;
        oldBranch = Dungeon.branch;
        oldTagDisappeared = GameScene.tagDisappeared;
        for (String name : new String[]{"all", "chars", "ids", "current", "now", "nextID"}) {
            Field state = field(Actor.class, name);
            oldActors.put(state, state.get(null));
        }
        field(Actor.class, "all").set(null, new HashSet<Actor>());
        field(Actor.class, "chars").set(null, new HashSet<Char>());
        field(Actor.class, "ids").set(null, new SparseArray<Actor>());
        field(Actor.class, "current").set(null, null);
        field(Actor.class, "now").setFloat(null, 0f);
        field(Actor.class, "nextID").setInt(null, 1);
        selector = field(GameScene.class, "cellSelector");
        oldSelector = selector.get(null);
        selector.set(null, allocate(CellSelector.class));
        choose = WndWallSearchResume.class.getDeclaredMethod("choose", Hero.class, Level.class, Action.class);
        choose.setAccessible(true);

        Dungeon.depth = 3;
        Dungeon.branch = 0;
        level = new Arena();
        level.setSize(SIZE, SIZE);
        level.setRooms();
        level.mobs = new HashSet<>();
        level.traps = new SparseArray<>();
        level.blobs = new HashMap<>();
        level.heaps = new SparseArray<>();
        level.plants = new SparseArray<>();
        level.locked = true; // Search timing can be checked without adding hunger in policy cases.
        Dungeon.level = level;
        clearTerrain();
        hero = new QuietHero();
        hero.pos = at(10, 10);
        hero.HP = hero.HT = 20;
        hero.ready = true;
        hero.fieldOfView = level.heroFOV;
        hero.sprite = allocate(QuietSprite.class);
        Dungeon.hero = hero;
        talents(HeroClass.WARRIOR, false, 0);
        hero.wallSearch.searched(hero); // A real remembered completed search, with no active command.
    }

    @After public void restore() throws Exception {
        SPDSettings.adaptiveSearchChoice(oldAdaptiveChoice);
        if (hero != null) for (Buff buff : new ArrayList<>(hero.buffs())) buff.detach();
        for (Map.Entry<Field, Object> entry : oldActors.entrySet()) entry.getKey().set(null, entry.getValue());
        selector.set(null, oldSelector);
        GameScene.tagDisappeared = oldTagDisappeared;
        Dungeon.hero = oldHero;
        Dungeon.level = oldLevel;
        Dungeon.depth = oldDepth;
        Dungeon.branch = oldBranch;
        if (oldLevel != null) PathFinder.setMapSize(oldLevel.width(), oldLevel.height());
        Random.popGenerator();
    }

    private void clearTerrain() {
        Arrays.fill(level.map, Terrain.EMPTY);
        Arrays.fill(level.secret, false);
        Arrays.fill(level.heroFOV, true);
        Arrays.fill(level.visited, true);
        Arrays.fill(level.mapped, false);
        Arrays.fill(level.passable, true);
        level.searchedWalls.clear();
    }

    /** Decorated side frames are valid supports but cannot be competing target candidates. */
    private int candidate(int dx, int dy) {
        int cell = hero.pos + dy * SIZE + dx;
        level.map[cell] = Terrain.WALL;
        level.map[cell - SIZE] = level.map[cell + SIZE] = Terrain.WALL_DECO;
        level.passable[cell] = level.passable[cell - SIZE] = level.passable[cell + SIZE] = false;
        assertTrue(DoorSearchEligibility.candidate(level, cell));
        return cell;
    }

    private void talents(HeroClass heroClass, boolean metamorph, int rank) {
        hero.heroClass = heroClass;
        hero.talents.clear();
        hero.metamorphedTalents.clear();
        Talent.initClassTalents(hero);
        if (metamorph) {
            Talent replaced = hero.talents.get(1).keySet().iterator().next();
            hero.metamorphedTalents.put(replaced, Talent.WIDE_SEARCH);
            hero.talents.clear();
            Talent.initClassTalents(hero);
        }
        for (int i = 0; i < rank; i++) hero.upgradeTalent(Talent.WIDE_SEARCH);
        assertEquals(rank, hero.pointsInTalent(Talent.WIDE_SEARCH));
    }

    private String memory() {
        Bundle bundle = new Bundle();
        hero.wallSearch.store(bundle);
        return bundle.toString();
    }

    private void select(Action action) throws Exception { choose.invoke(null, hero, level, action); }

    @Test public void openAreaPutsSearchFirstAndRetainsSavedManagementWithoutWallAction() {
        assertArrayEquals(new Action[]{Action.SEARCH, Action.SHOW_SAVED, Action.FORGET, Action.CANCEL},
                SearchControlContext.choices(hero, level));
        assertTrue(hero.wallSearch.exists());
        assertNull(hero.curAction);
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void chooserReturnsFreshOrderAfterCallerChangesItsArrayAndMemoryState() {
        Action[] first = SearchControlContext.choices(hero, level);
        assertArrayEquals(new Action[]{Action.SEARCH,
                Action.SHOW_SAVED, Action.FORGET, Action.CANCEL}, first);
        first[0] = Action.CANCEL;
        first[1] = Action.CANCEL;
        Action[] second = SearchControlContext.choices(hero, level);
        assertNotSame("a caller must never receive shared mutable chooser storage", first, second);
        assertArrayEquals("the first caller cannot change the next chooser's options",
                new Action[]{Action.SEARCH,
                        Action.SHOW_SAVED, Action.FORGET, Action.CANCEL}, second);

        hero.wallSearch.clear();
        assertArrayEquals("without remembered wall search, only ordinary search and cancel remain",
                new Action[]{Action.SEARCH, Action.CANCEL},
                SearchControlContext.choices(hero, level));
        hero.lastAction = new HeroAction.Move(hero.pos + 1);
        assertArrayEquals("movement is preferred and retained without wall-search memory",
                new Action[]{Action.RESUME_MOVEMENT, Action.SEARCH, Action.CANCEL},
                SearchControlContext.choices(hero, level));
        assertArrayEquals("a missing hero has no remembered or movement choices",
                new Action[]{Action.SEARCH, Action.CANCEL},
                SearchControlContext.choices(null, level));
        assertNull(hero.curAction);
        assertEquals(0, hero.nextCalls);
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void singleResultArrayPreservesEveryChooserStateWithoutGapsOrAliases() {
        boolean previousAdaptive = SPDSettings.adaptiveSearchChoice();
        try {
            SPDSettings.adaptiveSearchChoice(true);
            Action[][] expected = new Action[6][];
            Action[][] observed = new Action[6][];

            expected[0] = new Action[]{Action.SEARCH, Action.SHOW_SAVED,
                    Action.FORGET, Action.CANCEL};
            observed[0] = SearchControlContext.choices(hero, level);

            hero.lastAction = new HeroAction.Move(hero.pos + 1);
            expected[1] = new Action[]{Action.RESUME_MOVEMENT, Action.SEARCH,
                    Action.SHOW_SAVED, Action.FORGET, Action.CANCEL};
            observed[1] = SearchControlContext.choices(hero, level);

            candidate(1, 0);
            assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
            expected[2] = new Action[]{Action.WALL_SEARCH, Action.SEARCH, Action.SHOW_SAVED,
                    Action.RESUME_MOVEMENT, Action.FORGET, Action.CANCEL};
            observed[2] = SearchControlContext.choices(hero, level);

            SPDSettings.adaptiveSearchChoice(false);
            expected[3] = new Action[]{Action.RESUME_MOVEMENT, Action.SEARCH, Action.WALL_SEARCH,
                    Action.SHOW_SAVED, Action.FORGET, Action.CANCEL};
            observed[3] = SearchControlContext.choices(hero, level);

            hero.wallSearch.clear();
            expected[4] = new Action[]{Action.RESUME_MOVEMENT, Action.SEARCH, Action.CANCEL};
            observed[4] = SearchControlContext.choices(hero, level);

            hero.lastAction = null;
            expected[5] = new Action[]{Action.SEARCH, Action.CANCEL};
            observed[5] = SearchControlContext.choices(hero, level);

            for (int i = 0; i < expected.length; i++) {
                assertEquals("exact result length for chooser state " + i,
                        expected[i].length, observed[i].length);
                assertArrayEquals("stable ordering for chooser state " + i, expected[i], observed[i]);
                assertEquals("no duplicates or unfilled slots in chooser state " + i,
                        observed[i].length, new HashSet<>(Arrays.asList(observed[i])).size());
                for (Action action : observed[i]) assertNotNull(action);
            }
            assertArrayEquals(new Action[]{Action.SEARCH, Action.CANCEL},
                    SearchControlContext.choices(null, level));
            observed[0][0] = Action.CANCEL;
            assertArrayEquals("a caller cannot mutate future chooser options",
                    expected[5], SearchControlContext.choices(hero, level));
            assertNull(hero.curAction);
            assertEquals(0, hero.nextCalls);
            assertEquals(0f, hero.cooldown(), 0f);
        } finally {
            SPDSettings.adaptiveSearchChoice(previousAdaptive);
        }
    }

    @Test public void staleSavedFloorNeverOffersWallResumeEvenBesideCandidate() {
        int previousDepth = Dungeon.depth;
        try {
            Dungeon.depth = previousDepth + 1;
            candidate(1, 0);
            assertFalse(hero.wallSearch.matches(level));
            assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
            assertArrayEquals(new Action[]{Action.SEARCH, Action.SHOW_SAVED, Action.FORGET, Action.CANCEL},
                    SearchControlContext.choices(hero, level));
        } finally {
            Dungeon.depth = previousDepth;
        }
    }

    @Test public void onlyUnconfirmedObservedCandidatePrioritizesWallSearch() {
        int wall = candidate(1, 0);
        String saved = memory();
        assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
        level.searchedWalls.mark(level, wall); // A legacy attempt is not completed negative proof.
        assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
        level.searchedWalls.recordCompletedDoorCheck(level, wall, true);
        assertTrue(level.searchedWalls.confirmed(level, wall));
        assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
        assertEquals(saved, memory());
    }

    @Test public void ordinaryRoomWallSearchFollowsActualSearchReach() {
        int previousDepth = Dungeon.depth;
        boolean prior = SPDSettings.adaptiveSearchChoice();
        try {
            Dungeon.depth = 2;
            SPDSettings.adaptiveSearchChoice(true);
            hero.wallSearch.searched(hero);
            EmptyRoom ordinary = new EmptyRoom();
            ordinary.set(7, 7, 13, 13);
            level.setRooms(ordinary);
            int wall = candidate(2, 0);
            // Rogue's two-tile footprint reaches the wall from mid-room: Wall Search.
            talents(HeroClass.ROGUE, false, 0);
            assertTrue(WallSearchSession.covers(hero, level, hero.pos, wall));
            assertFalse(SearchControlContext.noHiddenDoorRoom(level, hero.pos));
            assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
            // A Warrior without Wide Search cannot touch it: ordinary Search.
            talents(HeroClass.WARRIOR, false, 0);
            assertFalse(WallSearchSession.covers(hero, level, hero.pos, wall));
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
            // Wide Search on any class extends reach to the wall again.
            talents(HeroClass.WARRIOR, true, 1);
            assertTrue(WallSearchSession.covers(hero, level, hero.pos, wall));
            assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
            SPDSettings.adaptiveSearchChoice(false);
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
        } finally {
            SPDSettings.adaptiveSearchChoice(prior);
            Dungeon.depth = previousDepth;
            level.setRooms();
            hero.pos = at(10, 10);
        }
    }

    @Test public void actualSearchOnlyMarksWallsInsideRealReach() throws Exception {
        int priorDepth = Dungeon.depth;
        boolean priorChoice = SPDSettings.adaptiveSearchChoice();
        try {
            Dungeon.depth = 2;
            SPDSettings.adaptiveSearchChoice(true);
            EmptyRoom room = new EmptyRoom();
            room.set(7, 7, 13, 13);
            level.setRooms(room);
            talents(HeroClass.WARRIOR, false, 0);
            int wall = candidate(2, 0);
            select(Action.SEARCH);
            assertEquals(2f, hero.cooldown(), 0f);
            assertFalse("a wall out of reach is never marked", level.searchedWalls.attempted(wall));
            level.searchedWalls.clear();
            hero.pos = at(11, 10);
            hero.ready = true;
            assertTrue(WallSearchSession.covers(hero, level, hero.pos, wall));
            select(Action.SEARCH);
            assertTrue("standing against an ordinary room wall records the search",
                    level.searchedWalls.attempted(wall));
            assertEquals(4f, hero.cooldown(), 0f);
        } finally {
            SPDSettings.adaptiveSearchChoice(priorChoice);
            Dungeon.depth = priorDepth;
            hero.pos = at(10, 10);
            level.setRooms();
        }
    }

    @Test public void floorTwelveWideSearchReachFromRoomCenterAllowsWallSearch() throws Exception {
        int priorDepth = Dungeon.depth;
        boolean priorChoice = SPDSettings.adaptiveSearchChoice();
        try {
            Dungeon.depth = 12;
            SPDSettings.adaptiveSearchChoice(true);
            hero.wallSearch.searched(hero);
            EmptyRoom room = new EmptyRoom();
            room.set(7, 7, 13, 13);
            level.setRooms(room);
            talents(HeroClass.ROGUE, false, 2);
            int wall = candidate(3, 0);
            assertTrue(WallSearchSession.covers(hero, level, hero.pos, wall));
            assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
            assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
            assertTrue(hero.wallSearch.select(hero, wall));
            talents(HeroClass.ROGUE, false, 0);
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
        } finally {
            SPDSettings.adaptiveSearchChoice(priorChoice);
            Dungeon.depth = priorDepth;
            hero.pos = at(10, 10);
            level.setRooms();
        }
    }

    @Test public void floorOneAndCrystalRoomsNeverPrioritizeAdaptiveWallChoice() {
        boolean prior = SPDSettings.adaptiveSearchChoice();
        int priorDepth = Dungeon.depth;
        try {
            SPDSettings.adaptiveSearchChoice(true);
            candidate(1, 0);
            Dungeon.depth = 1;
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
            Dungeon.depth = 2;
            for (Room room : new Room[]{new CrystalVaultRoom(), new CrystalChoiceRoom()}) {
                room.set(7, 7, 13, 13);
                level.setRooms(room);
                assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
            }
        } finally {
            SPDSettings.adaptiveSearchChoice(prior);
            Dungeon.depth = priorDepth;
            level.setRooms();
        }
    }

    @Test public void everyRoomThatCannotHideADoorDisablesWallSearch() throws Exception {
        int priorDepth = Dungeon.depth;
        boolean priorToggle = SPDSettings.adaptiveSearchChoice();
        try {
            Dungeon.depth = 13;
            SPDSettings.adaptiveSearchChoice(true);
            // Every special room has one entrance (the one the hero came through).
            for (Room room : new Room[]{new LaboratoryRoom(), new ArmoryRoom(), new StorageRoom(),
                    new CrystalVaultRoom(), new CrystalChoiceRoom(), new TreasuryRoom(),
                    new MagicWellRoom(), new ShopRoom(), new SacrificeRoom()}) {
                clearTerrain();
                room.set(7, 7, 13, 13);
                level.setRooms(room);
                hero.pos = at(12, 10);
                hero.ready = true;
                hero.wallSearch.clear();
                hero.wallSearch.searched(hero);
                talents(HeroClass.ROGUE, false, 2);
                int wall = candidate(1, 0);
                String name = room.getClass().getSimpleName();
                assertTrue(name, SearchControlContext.noHiddenDoorRoom(level, hero.pos));
                assertFalse(name, SearchControlContext.hasMeaningfulWall(hero, level));
                assertEquals(name, Action.SEARCH, SearchControlContext.preferred(hero, level));
                assertFalse(name, hero.wallSearch.select(hero, wall));
                assertNotNull(name, hero.wallSearch.resume(hero));
                float before = hero.cooldown();
                select(Action.SEARCH);
                assertFalse(name, level.searchedWalls.attempted(wall));
                assertEquals(name, before + 2f, hero.cooldown(), 0f);
                hero.ready = true;
            }
            // Standard rooms and hallways can hide doors and keep Wall Search.
            clearTerrain();
            EmptyRoom standard = new EmptyRoom();
            standard.set(7, 7, 13, 13);
            level.setRooms(standard);
            hero.pos = at(12, 10);
            hero.ready = true;
            hero.wallSearch.clear();
            hero.wallSearch.searched(hero);
            candidate(1, 0);
            assertFalse(SearchControlContext.noHiddenDoorRoom(level, hero.pos));
            assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
            assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
        } finally {
            SPDSettings.adaptiveSearchChoice(priorToggle);
            Dungeon.depth = priorDepth;
            hero.pos = at(10, 10);
            level.setRooms();
        }
    }

    @Test public void floorOneStartingRoomAllowsOrdinarySearchButNeverWallSearch() throws Exception {
        int priorDepth = Dungeon.depth;
        int priorBranch = Dungeon.branch;
        try {
            Dungeon.depth = 1;
            Dungeon.branch = 0;
            clearTerrain();
            EmptyRoom startingRoom = new EmptyRoom();
            startingRoom.set(7, 7, 13, 13);
            level.setRooms(startingRoom);
            hero.pos = at(10, 10);
            hero.ready = true;
            int wall = hero.pos + 1;
            level.map[wall] = Terrain.WALL;
            level.map[wall - SIZE] = level.map[wall + SIZE] = Terrain.WALL_DECO;
            level.passable[wall] = false;
            assertEquals(Terrain.WALL, level.map[wall]);
            assertFalse("floor one cannot generate concealed door candidates",
                    DoorSearchEligibility.supportsDoors(level));
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
            assertArrayEquals(new Action[]{Action.SEARCH, Action.SHOW_SAVED, Action.FORGET, Action.CANCEL},
                    SearchControlContext.choices(hero, level));
            assertFalse("Wall Search target selection must reject floor one",
                    hero.wallSearch.select(hero, wall));
            assertNotNull("Wall Search resume must reject floor one",
                    hero.wallSearch.resume(hero));
            float before = hero.cooldown();
            select(Action.SEARCH);
            assertEquals("ordinary Search is still usable", before+2f, hero.cooldown(), 0f);
            assertFalse("ordinary Search cannot create wall markers on floor one",
                    level.searchedWalls.attempted(wall));
        } finally {
            Dungeon.depth = priorDepth;
            Dungeon.branch = priorBranch;
            hero.pos = at(10, 10);
            level.setRooms();
        }
    }

    @Test public void baseClassDoesNotPretendWideSearchWasAcquired() {
        candidate(2, 0);
        for (HeroClass heroClass : HeroClass.values()) {
            talents(heroClass, false, 0);
            assertFalse(hero.hasTalent(Talent.WIDE_SEARCH));
            assertEquals(heroClass.toString(), heroClass == HeroClass.ROGUE,
                    SearchControlContext.hasMeaningfulWall(hero, level));
        }
        clearTerrain();
        candidate(3, 0);
        talents(HeroClass.ROGUE, false, 0);
        assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
    }

    @Test public void acquiredRogueWideSearchRetainsRoundedAndSquareSevenBySeven() {
        talents(HeroClass.ROGUE, false, 1);
        candidate(3, 0);
        assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
        clearTerrain();
        candidate(3, 3);
        assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
        hero.upgradeTalent(Talent.WIDE_SEARCH);
        assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
        clearTerrain();
        candidate(4, 0);
        assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
    }

    @Test public void actualMetamorphosisTalentsExpandOtherClassesOnlyToFiveByFive() {
        for (HeroClass heroClass : HeroClass.values()) {
            if (heroClass == HeroClass.ROGUE) continue;
            clearTerrain();
            candidate(2, 0);
            talents(heroClass, true, 0);
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            hero.upgradeTalent(Talent.WIDE_SEARCH);
            assertSame(heroClass, hero.heroClass);
            assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
            clearTerrain();
            candidate(2, 2);
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            hero.upgradeTalent(Talent.WIDE_SEARCH);
            assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
            clearTerrain();
            candidate(3, 0);
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
        }
    }

    @Test public void unseenBackingAndSecretFlagsCannotLeakIntoContextPreference() {
        int wall = candidate(1, 0), backing = wall + 1;
        level.heroFOV[backing] = level.visited[backing] = level.mapped[backing] = false;
        for (int tile = 0; tile < Terrain.flags.length; tile++) {
            level.map[backing] = tile;
            for (boolean secret : new boolean[]{false, true}) {
                level.secret[wall] = level.secret[backing] = secret;
                assertEquals("concealed backing " + tile, Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
            }
        }
        level.map[wall] = Terrain.SECRET_DOOR;
        assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
    }

    @Test public void unseenUnsupportedAndDoorlessWallsDoNotTakePriority() {
        int wall = candidate(1, 0);
        level.heroFOV[wall] = false;
        assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
        level.heroFOV[wall] = true;
        level.heroFOV[wall - SIZE] = level.visited[wall - SIZE] = false;
        assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
        level.heroFOV[wall - SIZE] = true;
        level.map[wall - SIZE] = Terrain.EMPTY;
        assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
        clearTerrain();
        candidate(1, 0);
        for (int depth : new int[]{1, 10, 15, 20, 25, 26}) {
            Dungeon.depth = depth;
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
        }
    }

    @Test public void doorlessFloorsOmitWallResumeButKeepNormalSearchAndSavedPosition() {
        String saved = memory();
        HeroAction.Move movement = new HeroAction.Move(hero.pos + 2);
        for (int depth : new int[]{1, 10, 15, 20, 25, 26}) {
            Dungeon.depth = depth;
            assertFalse("hidden doors cannot generate on floor " + depth,
                    DoorSearchEligibility.supportsDoors(level));
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
            assertArrayEquals("no wall resume on floor " + depth,
                    new Action[]{Action.SEARCH, Action.SHOW_SAVED, Action.FORGET, Action.CANCEL},
                    SearchControlContext.choices(hero, level));

            hero.lastAction = movement;
            assertArrayEquals("movement and saved-position controls survive on floor " + depth,
                    new Action[]{Action.RESUME_MOVEMENT, Action.SEARCH, Action.SHOW_SAVED,
                            Action.FORGET, Action.CANCEL},
                    SearchControlContext.choices(hero, level));
            hero.lastAction = null;
            assertEquals("viewing choices must not erase earlier wall-search memory", saved, memory());
        }
        Dungeon.depth = 3;
        assertTrue(DoorSearchEligibility.supportsDoors(level));
        assertArrayEquals("door-capable floor alone does not establish a meaningful wall",
                new Action[]{Action.SEARCH, Action.SHOW_SAVED, Action.FORGET, Action.CANCEL},
                SearchControlContext.choices(hero, level));
        candidate(1, 0);
        assertArrayEquals("a matching saved session and actual wall restore wall resume",
                new Action[]{Action.WALL_SEARCH, Action.SEARCH, Action.SHOW_SAVED,
                        Action.FORGET, Action.CANCEL}, SearchControlContext.choices(hero, level));
        assertEquals(0, hero.nextCalls);
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void bothActualCrystalRoomTypesApplyOnlyWhileHeroIsInside() {
        candidate(1, 0);
        for (Room room : new Room[]{new CrystalVaultRoom(), new CrystalChoiceRoom()}) {
            room.set(7, 7, 13, 13);
            level.setRooms(room);
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
            Heap chest = new Heap();
            chest.type = Heap.Type.CRYSTAL_CHEST;
            level.heaps.put(hero.pos + 1, chest);
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
            chest.type = Heap.Type.HEAP;
            level.heaps.clear();
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
            room.set(2, 2, 7, 7);
            assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
        }
        level.setRooms();
        assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
    }

    @Test public void movementFirstWhenWallsAreIrrelevantStillKeepsBothSearchChoices() throws Exception {
        HeroAction.Move movement = new HeroAction.Move(hero.pos + 2);
        hero.lastAction = movement;
        assertArrayEquals(new Action[]{Action.RESUME_MOVEMENT, Action.SEARCH, Action.SHOW_SAVED, Action.FORGET, Action.CANCEL},
                SearchControlContext.choices(hero, level));
        candidate(1, 0);
        assertArrayEquals(new Action[]{Action.WALL_SEARCH, Action.SEARCH, Action.SHOW_SAVED, Action.RESUME_MOVEMENT, Action.FORGET, Action.CANCEL},
                SearchControlContext.choices(hero, level));
        String saved = memory();
        select(Action.RESUME_MOVEMENT);
        assertSame(movement, hero.curAction);
        assertNull(hero.lastAction);
        assertEquals(1, hero.nextCalls);
        assertEquals(saved, memory());
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void contextRecomputesWithoutMutatingTimeCommandsMemoryMapOrRandomness() {
        candidate(2, 0);
        String saved = memory();
        int[] map = level.map.clone();
        boolean[] fov = level.heroFOV.clone(), visited = level.visited.clone(), mapped = level.mapped.clone();
        Random.pushGenerator(445);
        long expected = Random.Long();
        Random.popGenerator();
        Random.pushGenerator(445);
        try {
            for (int i = 0; i < 100; i++) {
                assertEquals(Action.SEARCH, SearchControlContext.choices(hero, level)[0]);
                hero.pos++;
                assertEquals(Action.WALL_SEARCH, SearchControlContext.choices(hero, level)[0]);
                hero.pos--;
            }
            assertEquals(expected, Random.Long());
        } finally { Random.popGenerator(); }
        talents(HeroClass.WARRIOR, true, 1);
        assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
        assertEquals(saved, memory());
        assertArrayEquals(map, level.map);
        assertArrayEquals(fov, level.heroFOV);
        assertArrayEquals(visited, level.visited);
        assertArrayEquals(mapped, level.mapped);
        assertNull(hero.curAction);
        assertNull(hero.lastAction);
        assertEquals(0, hero.nextCalls);
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void interruptedWallWalkAndRestoredMemoryNeverAutoResume() {
        int wall = candidate(1, 0);
        assertTrue(hero.wallSearch.select(hero, wall));
        hero.curAction = new WallSearchSession.Walk(hero.pos, level);
        hero.interrupt();
        String saved = memory();
        Bundle bundle = new Bundle();
        hero.wallSearch.store(bundle);
        hero.wallSearch.clear();
        hero.wallSearch.restore(bundle);
        hero.pos = at(3, 3);
        assertEquals(Action.SEARCH, SearchControlContext.choices(hero, level)[0]);
        assertEquals(saved, memory());
        assertNull(hero.curAction);
        assertNull(hero.lastAction);
        assertEquals(0, hero.nextCalls);
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void explicitOrdinaryChoiceUsesRealSearchAtItsUnchangedTwoTurnCost() throws Exception {
        HeroAction.Move movement = new HeroAction.Move(hero.pos + 3);
        hero.lastAction = movement;
        select(Action.SEARCH);
        assertEquals(2f, hero.cooldown(), 0f);
        assertEquals(1, hero.nextCalls);
        assertEquals("ordinary paid Search retains its original saved-session count", 2, hero.wallSearch.searches());
        assertSame(movement, hero.lastAction);
        assertNull(hero.curAction);
    }

    @Test public void explicitWallChoiceRetainsRealPaidSearchAndCompletionProof() throws Exception {
        int wall = candidate(1, 0);
        level.locked = false;
        select(Action.WALL_SEARCH);
        assertEquals(2f, hero.cooldown(), 0f);
        assertEquals(1, hero.nextCalls);
        assertEquals(2, hero.wallSearch.searches());
        assertTrue(level.searchedWalls.confirmed(level, wall));
        assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
    }

    @Test public void explicitWallChoiceStillHonorsExistingDangerAndMovementGuards() throws Exception {
        candidate(1, 0);
        String saved = memory();
        select(Action.WALL_SEARCH); // locked floor
        level.locked = false;
        hero.rooted = true;
        select(Action.WALL_SEARCH);
        hero.rooted = false;
        hero.paralysed = 1;
        select(Action.WALL_SEARCH);
        hero.paralysed = 0;
        ArrayList<Mob> enemies = new ArrayList<>();
        enemies.add(new Rat());
        field(Hero.class, "visibleEnemies").set(hero, enemies);
        assertEquals(1, hero.visibleEnemies());
        select(Action.WALL_SEARCH);
        assertEquals(saved, memory());
        assertNull(hero.curAction);
        assertEquals(0, hero.nextCalls);
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void staleBusyAndDeadChooserSelectionsCannotActOrForget() throws Exception {
        String saved = memory();
        for (Action action : Action.values()) {
            hero.ready = false;
            select(action);
            hero.ready = true;
            hero.HP = 0;
            select(action);
            hero.HP = 20;
            Dungeon.hero = new Hero();
            select(action);
            Dungeon.hero = hero;
            Dungeon.level = new GrassDewPickupTest.TestLevel();
            select(action);
            Dungeon.level = level;
        }
        assertEquals(saved, memory());
        assertNull(hero.curAction);
        assertEquals(0, hero.nextCalls);
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void forgetAndCancelRequireTheirOwnExplicitSelection() throws Exception {
        String saved = memory();
        SearchControlContext.choices(hero, level);
        select(Action.CANCEL);
        assertEquals(saved, memory());
        select(Action.FORGET);
        assertFalse(hero.wallSearch.exists());
        assertEquals(0, hero.nextCalls);
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void actualSearchFromInsideKeyedRoomDoesNotCreateCheckmarks() throws Exception {
        Object[][] gated = {
                {new ArmoryRoom(), Room.Door.Type.LOCKED},
                {new TreasuryRoom(), Room.Door.Type.LOCKED},
                {new CrystalVaultRoom(), Room.Door.Type.LOCKED},
                {new StorageRoom(), Room.Door.Type.BARRICADE},
                {new StorageRoom(), Room.Door.Type.CRYSTAL}
        };
        for (Object[] pair : gated) {
            hero.ready = true; // Each case must execute its own paid Search.
            clearTerrain();
            int wall = candidate(1, 0);
            SpecialRoom room = (SpecialRoom) pair[0];
            room.set(7, 7, 13, 13);
            Room.Door entrance = new Room.Door(7, 10);
            entrance.set((Room.Door.Type) pair[1]);
            room.connected.put(new EmptyRoom(), entrance);
            level.setRooms(room);
            assertSame(room, level.room(hero.pos));
            assertTrue(DoorSearchEligibility.candidate(level, wall));
            assertTrue(DoorSearchEligibility.insideGatedRoom(level, hero.pos));
            assertFalse("Gated rooms must never suggest adaptive wall-search context",
                    SearchControlContext.hasMeaningfulWall(hero, level));
            // A previous build may already have stored this mark in a saved game.
            assertTrue(level.searchedWalls.mark(level, wall));
            assertEquals("Previously saved checkmark must also be invisible inside a gated room",
                    -1, SearchedWallMarkers.visual(level, wall));
            level.searchedWalls.clear();
            int before = level.map[wall];
            float timeBefore = hero.cooldown();
            select(Action.SEARCH); // Actual ordinary paid search, not a mocked marker call.
            assertEquals("No checkmark after searching inside " + room.getClass().getSimpleName(),
                    false, level.searchedWalls.attempted(wall));
            assertEquals(before, level.map[wall]);
            assertEquals("Every completed Search spends precisely two normal turns",
                    timeBefore + 2f, hero.cooldown(), 0f);
        }
        // Ordinary rooms can hide doors, so searching against their wall records a check.
        clearTerrain();
        int ordinaryWall = candidate(1, 0);
        EmptyRoom ordinary = new EmptyRoom();
        ordinary.set(7, 7, 11, 13); // The eligible wall is now on this room's edge.
        level.setRooms(ordinary);
        hero.ready = true;
        assertFalse(DoorSearchEligibility.insideGatedRoom(level, hero.pos));
        select(Action.SEARCH);
        assertTrue(level.searchedWalls.attempted(ordinaryWall));
    }

    @Test public void contextualScanBoundsMatchActualClassTalentAndForesightFootprints() {
        // Each in-range wall must still be found; an immediately out-of-range
        // eligible wall must not be advertised, even when it is visible.
        for (HeroClass heroClass : HeroClass.values()) {
            talents(heroClass, false, 0);
            int base = heroClass == HeroClass.ROGUE ? 2 : 1;
            assertEquals(heroClass.toString(), base, WallSearchSession.searchRadius(hero, false));
            clearTerrain();
            candidate(base, 0);
            assertTrue(heroClass.toString(), SearchControlContext.hasMeaningfulWall(hero, level));
            clearTerrain();
            candidate(base + 1, 0);
            assertFalse(heroClass.toString(), SearchControlContext.hasMeaningfulWall(hero, level));
        }

        talents(HeroClass.WARRIOR, true, 1);
        assertEquals(2, WallSearchSession.searchRadius(hero, false));
        clearTerrain();
        candidate(2, 0);
        assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
        clearTerrain();
        candidate(2, 2); // Wide Search rank 1 has rounded corners.
        assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
        hero.upgradeTalent(Talent.WIDE_SEARCH);
        assertEquals(2, WallSearchSession.searchRadius(hero, false));
        assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
        clearTerrain();
        candidate(3, 0);
        assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));

        talents(HeroClass.ROGUE, false, 1);
        assertEquals(3, WallSearchSession.searchRadius(hero, false));
        clearTerrain();
        candidate(3, 0);
        assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
        clearTerrain();
        candidate(3, 3);
        assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
        hero.upgradeTalent(Talent.WIDE_SEARCH);
        assertEquals(3, WallSearchSession.searchRadius(hero, false));
        assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));

        talents(HeroClass.WARRIOR, false, 0);
        clearTerrain();
        candidate(8, 0);
        assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
        Buff.affect(hero, Foresight.class);
        assertEquals(Foresight.DISTANCE, WallSearchSession.searchRadius(hero, true));
        assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
    }

    @Test public void cachedForesightScanMatchesOrdinaryCoverageForEveryClassAndAcquiredRank() {
        // Compare every grid cell against the original public cover calculation.
        // Nothing in this comparison spends a Search turn or exposes hidden map data.
        int from = hero.pos;
        for (HeroClass heroClass : HeroClass.values()) {
            for (int rank = 0; rank <= 2; rank++) {
                talents(heroClass, heroClass != HeroClass.ROGUE, rank);
                for (int cell = 0; cell < level.length(); cell++) {
                    assertEquals("ordinary class=" + heroClass + " rank=" + rank + " cell=" + cell,
                            WallSearchSession.covers(hero, level, from, cell),
                            WallSearchSession.covers(hero, level, from, cell, false));
                }
            }
        }

        talents(HeroClass.WARRIOR, false, 0);
        Buff.affect(hero, Foresight.class);
        assertNotNull(hero.buff(Foresight.class));
        for (boolean visible : new boolean[]{true, false}) {
            Arrays.fill(level.heroFOV, visible);
            for (int cell = 0; cell < level.length(); cell++) {
                assertEquals("foresight visible=" + visible + " cell=" + cell,
                        WallSearchSession.covers(hero, level, from, cell),
                        WallSearchSession.covers(hero, level, from, cell, true));
            }
        }
        assertNull(hero.curAction);
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void existingForesightFootprintIsPreservedWithoutRevealingUnknownWalls() {
        candidate(8, 0);
        assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
        Buff.affect(hero, Foresight.class);
        assertTrue(SearchControlContext.hasMeaningfulWall(hero, level));
        Arrays.fill(level.heroFOV, false);
        assertTrue("Foresight can cover remembered terrain", SearchControlContext.hasMeaningfulWall(hero, level));
        Arrays.fill(level.visited, false);
        Arrays.fill(level.mapped, false); // Attaching actual Foresight already maps its current footprint.
        assertFalse("unknown terrain is not a presented candidate", SearchControlContext.hasMeaningfulWall(hero, level));
    }

    @Test public void actualResumeIndicatorRetainsDirectMovementWithoutSavedWallMemory() throws Exception {
        hero.wallSearch.clear();
        HeroAction.Move movement = new HeroAction.Move(hero.pos + 2);
        hero.lastAction = movement;
        ResumeIndicator indicator = allocate(ResumeIndicator.class);
        Method click = ResumeIndicator.class.getDeclaredMethod("onClick");
        click.setAccessible(true);
        hero.ready = false;
        click.invoke(indicator);
        assertSame(movement, hero.lastAction);
        hero.ready = true;
        click.invoke(indicator);
        assertSame(movement, hero.curAction);
        assertNull(hero.lastAction);
        assertEquals(1, hero.nextCalls);
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void previousFloorOrBranchMemoryNeverPrioritizesAnUnavailableWallResume() throws Exception {
        String saved = memory();
        candidate(1, 0);
        assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
        for (boolean otherBranch : new boolean[]{false, true}) {
            Dungeon.depth = otherBranch ? 3 : 4;
            Dungeon.branch = otherBranch ? 1 : 0;
            assertTrue("a current wall still exists", SearchControlContext.hasMeaningfulWall(hero, level));
            assertFalse(hero.wallSearch.matches(level));
            assertArrayEquals("a stale saved floor cannot offer a wall action",
                    new Action[]{Action.SEARCH, Action.SHOW_SAVED, Action.FORGET, Action.CANCEL},
                    SearchControlContext.choices(hero, level));
            HeroAction.Move movement = new HeroAction.Move(hero.pos + 2);
            hero.lastAction = movement;
            assertEquals(Action.RESUME_MOVEMENT, SearchControlContext.preferred(hero, level));
            select(Action.WALL_SEARCH);
            assertSame(movement, hero.lastAction);
            hero.lastAction = null;
            assertEquals(saved, memory());
            assertEquals(0, hero.nextCalls);
            assertEquals(0f, hero.cooldown(), 0f);
        }
    }

    @Test public void invalidContextAndGuardedWindowOpenNeverConstructAnAction() {
        assertFalse(SearchControlContext.hasMeaningfulWall(null, level));
        assertFalse(SearchControlContext.hasMeaningfulWall(hero, null));
        hero.pos = -1;
        assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
        hero.pos = level.length();
        assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
        hero.pos = at(10, 10);
        hero.ready = false;
        WndWallSearchResume.show();
        hero.ready = true;
        hero.HP = 0;
        WndWallSearchResume.show();
        assertNull(hero.curAction);
        assertEquals(0, hero.nextCalls);
        assertEquals(0f, hero.cooldown(), 0f);
    }

    @Test public void incompleteObservationArraysCannotPromoteAdaptiveWallChoice() {
        boolean prior = SPDSettings.adaptiveSearchChoice();
        boolean[] fov = level.heroFOV, visited = level.visited, mapped = level.mapped;
        int[] terrain = level.map;
        try {
            SPDSettings.adaptiveSearchChoice(true);
            candidate(1, 0);
            assertEquals(Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));

            level.heroFOV = null;
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            assertEquals(Action.SEARCH, SearchControlContext.preferred(hero, level));
            assertArrayEquals(new Action[]{Action.SEARCH, Action.SHOW_SAVED,
                    Action.FORGET, Action.CANCEL}, SearchControlContext.choices(hero, level));

            level.heroFOV = new boolean[hero.pos + 1];
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            level.heroFOV = fov;

            level.visited = null;
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            level.visited = visited;
            level.visited = new boolean[hero.pos + 1];
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            level.visited = visited;

            level.mapped = null;
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            level.mapped = mapped;
            level.mapped = new boolean[hero.pos + 1];
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            level.mapped = mapped;

            level.map = null;
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            level.map = new int[hero.pos + 1];
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            level.map = terrain;

            assertEquals("complete observed metadata restores normal adaptive selection",
                    Action.WALL_SEARCH, SearchControlContext.preferred(hero, level));
            assertNull(hero.curAction);
            assertEquals(0f, hero.cooldown(), 0f);
        } finally {
            SPDSettings.adaptiveSearchChoice(prior);
            level.heroFOV = fov;
            level.visited = visited;
            level.mapped = mapped;
            level.map = terrain;
        }
    }

    @Test public void roomCenterCheckHandlesMissingAndShortVisibilityWithoutReadingHiddenTiles() {
        boolean[] fov = level.heroFOV;
        EmptyRoom room = new EmptyRoom();
        room.set(7, 7, 13, 13);
        try {
            level.setRooms(room);
            assertTrue("complete observed room center is still recognized",
                    SearchControlContext.openRoomInterior(hero, level));
            level.heroFOV = null;
            assertFalse(SearchControlContext.openRoomInterior(hero, level));
            level.heroFOV = new boolean[hero.pos + 1];
            assertFalse(SearchControlContext.openRoomInterior(hero, level));
            assertNull(hero.curAction);
            assertEquals(0f, hero.cooldown(), 0f);
        } finally {
            level.heroFOV = fov;
            level.setRooms();
        }
    }
    @Test public void visibleTrapsMakeTheWholeRoomWorthAFullSearchEvenWithoutDoors() {
        int priorDepth = Dungeon.depth;
        try {
            Dungeon.depth = 1; // no hidden doors here, so only traps can make Search worthwhile
            EmptyRoom room = new EmptyRoom();
            room.set(7, 7, 13, 13);
            level.setRooms(room);
            level.traps = new SparseArray<>();
            hero.pos = at(10, 10);
            assertFalse(SearchControlContext.hasMeaningfulWall(hero, level));
            assertFalse(SearchControlContext.visibleTrapRoom(hero, level));
            // A hidden trap must never flip the decision (that would leak it).
            com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap hidden =
                    level.setTrap(new com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap(), at(8, 8));
            hidden.visible = false; // worn dart traps refuse hide(); force the hidden state directly
            assertFalse(SearchControlContext.visibleTrapRoom(hero, level));
            // A visible trap outside the room does not count.
            level.setTrap(new com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap().reveal(), at(3, 3));
            assertFalse(SearchControlContext.visibleTrapRoom(hero, level));
            // A visible trap in the hero's room does, anywhere in the room.
            level.setTrap(new com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap().reveal(), at(12, 12));
            assertTrue(SearchControlContext.visibleTrapRoom(hero, level));
        } finally {
            Dungeon.depth = priorDepth;
            level.traps = new SparseArray<>();
            level.setRooms();
            hero.pos = at(10, 10);
        }
    }

    /** Scheduled Nov 18: tap searches anywhere in a room that still has an unsearched door-capable wall. */
    @Test public void roomWideUnsearchedWallMakesTapSearchFromTheMiddle() {
        EmptyRoom room = new EmptyRoom();
        room.set(7, 7, 13, 13);
        level.setRooms(room);
        int wall = candidate(-3, 0); // west wall, out of the Warrior's radius-1 reach
        assertFalse("wall is outside search reach", SearchControlContext.hasMeaningfulWall(hero, level));
        assertTrue(SearchControlContext.roomHasSearchableWall(hero, level));
        assertTrue(SearchControlContext.fullSearchUseful(hero, level));

        level.searchedWalls.recordCompletedDoorCheck(level, wall, true);
        assertTrue(level.searchedWalls.confirmed(level, wall));
        assertFalse("a fully searched room taps to Inspect", SearchControlContext.roomHasSearchableWall(hero, level));
    }

    @Test public void roomWideRuleSkipsSpecialRoomsAndRoomlessLevels() {
        assertFalse("no rooms recorded", SearchControlContext.roomHasSearchableWall(hero, level));
        StorageRoom special = new StorageRoom();
        special.set(7, 7, 13, 13);
        level.setRooms(special);
        candidate(-3, 0);
        assertFalse(SearchControlContext.roomHasSearchableWall(hero, level));
        assertFalse(SearchControlContext.roomHasSearchableWall(null, level));
        assertFalse(SearchControlContext.roomHasSearchableWall(hero, null));
    }
}
