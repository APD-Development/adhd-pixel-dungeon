package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.CrystalPathRoom;
import com.watabou.utils.Random;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

public class CrystalPathRewardsTest {
    private final com.watabou.noosa.Game originalGame = com.watabou.noosa.Game.instance;
    @After public void clearObserver() { LevelGenerationHarness.levelObserver = null; com.watabou.noosa.Game.instance = originalGame; }
    @Test public void allSixChambersAlwaysIncludeBothStandardRewards() throws Exception {
        new com.watabou.noosa.Game(null, null);
        LevelGenerationHarness.levelObserver = (depth, level) -> {
            if (depth != 1) return;
            int previousExotic = com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdExoticChance();
            try { for (int side = 0; side < 4; side++) for (int exotic : new int[]{0,80}) for (int testDepth : new int[]{1,6,11,16,22,24}) for (int seed = 0; seed < 64; seed++) {
                com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdExoticChance(exotic);
                com.shatteredpixel.shatteredpixeldungeon.Dungeon.depth = testDepth;
                level.heaps.clear();
                final int[][] doors = {{2,6},{10,6},{6,2},{6,10}};
                final int[] door = doors[side];
                CrystalPathRoom room = new CrystalPathRoom() {
                    @Override public Room.Door entrance() { return new Room.Door(door[0], door[1]); }
                };
                room.set(2, 2, 10, 10);
                Random.pushGenerator(seed);
                try { room.paint(level); } finally { Random.popGenerator(); }
                int experience = 0, transmutation = 0, total = 0;
                for (Heap heap : level.heaps.valueList()) for (Item item : heap.items) {
                    total++;
                    if (item.getClass() == PotionOfExperience.class || item.getClass() == ScrollOfTransmutation.class) {
                        assertTrue("guaranteed rewards only in hidden final chambers", heap.autoExplored);
                        assertEquals("exactly two crystal doors before the prize", 2, doorsTo(level, heap.pos, door));
                    }
                    if (item.getClass() == PotionOfExperience.class) experience++;
                    if (item.getClass() == ScrollOfTransmutation.class) transmutation++;
                }
                assertEquals("six chamber rewards", 6, total);
                assertEquals("guaranteed standard Experience", 1, experience);
                assertEquals("guaranteed standard Transmutation", 1, transmutation);
            }} finally { com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdExoticChance(previousExotic); }
        };
        LevelGenerationHarness.generate(7919L, 0, HeroClass.WARRIOR, LevelGenerationHarness.Context.VETERAN, 1);
    }
    private int doorsTo(com.shatteredpixel.shatteredpixeldungeon.levels.Level level, int goal, int[] door) {
        int[] distance = new int[level.length()]; java.util.Arrays.fill(distance, 999);
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        int start = level.pointToCell(new com.watabou.utils.Point(door[0] == 2 ? 3 : door[0] == 10 ? 9 : 6, door[1] == 2 ? 3 : door[1] == 10 ? 9 : 6)); distance[start]=0; queue.add(start);
        while(!queue.isEmpty()) { int cell=queue.remove(); for(int offset:com.watabou.utils.PathFinder.NEIGHBOURS4) {
            int next=cell+offset; if(next<0||next>=distance.length)continue;
            int terrain=level.map[next]; if(terrain==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WALL)continue;
            int cost=terrain==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.CRYSTAL_DOOR?1:0;
            if(distance[next]>distance[cell]+cost){distance[next]=distance[cell]+cost;queue.add(next);}
        }} return distance[goal];
    }
}
