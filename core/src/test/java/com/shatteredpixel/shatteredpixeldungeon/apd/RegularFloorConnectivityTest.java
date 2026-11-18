package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import org.junit.Test;
import java.util.ArrayDeque;
import static org.junit.Assert.*;

//Geometry only: does not claim combat, quest/boss completion or phone acceptance.
public class RegularFloorConnectivityTest {
    @Test public void everyClassCanReachRegularFloorExitsIncludingTheOwnersReportedSeed() throws Exception {
        HeadlessTestSupport.start();
        int previousDifficulty = SPDSettings.apdDifficulty();
        int previousChallenges = SPDSettings.challenges();
        java.util.function.BiConsumer<Integer, Level> previous = LevelGenerationHarness.levelObserver;
        final int[] checked = {0};
        try {
            SPDSettings.apdDifficulty(3);
            for (long seed : new long[]{DungeonSeed.convertFromCode("BVS-BND-PVG"), 4242L}) {
                for (HeroClass hero : HeroClass.values()) {
                    LevelGenerationHarness.levelObserver = (depth, level) -> {
                        if (depth % 5 == 0) return; //Boss arenas intentionally gate progression.
                        assertReachable(level, depth != 1, "seed=" + seed + " hero=" + hero + " depth=" + depth);
                        checked[0]++;
                    };
                    LevelGenerationHarness.generate(seed, 0, hero, LevelGenerationHarness.Context.FRESH, 24);
                }
            }
            assertEquals(2 * HeroClass.values().length * 20, checked[0]);
            System.out.println("APD regular-floor exit geometry: " + checked[0] + " checks passed");
        } finally {
            LevelGenerationHarness.levelObserver = previous;
            SPDSettings.apdDifficulty(previousDifficulty);
            SPDSettings.challenges(previousChallenges);
        }
    }

    private static void assertReachable(Level level, boolean allowSearch, String context) {
        assertTrue(context + " entrance bounds", level.entrance() >= 0 && level.entrance() < level.length());
        assertTrue(context + " exit bounds", level.exit() >= 0 && level.exit() < level.length());
        boolean[] seen = new boolean[level.length()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(level.entrance());
        seen[level.entrance()] = true;
        while (!queue.isEmpty()) {
            int cell = queue.remove();
            if (cell == level.exit()) return;
            int x = cell % level.width(), y = cell / level.width();
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || nx >= level.width() || ny < 0 || ny >= level.height()) continue;
                int next = nx + ny * level.width();
                int terrain = level.map[next];
                if (!seen[next] && (next == level.exit() || (Terrain.flags[terrain] & Terrain.PASSABLE) != 0
                        || allowSearch && terrain == Terrain.SECRET_DOOR)) {
                    seen[next] = true;
                    queue.add(next);
                }
            }
        }
        fail(context + " has no traversable route from entrance to exit");
    }
}
