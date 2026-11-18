package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap;
import com.watabou.utils.Random;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class SimulationNavigationTest {
    private static class EmptyLevel extends Level {
        protected boolean build() { return true; }
        protected void createMobs() {}
        protected void createItems() {}
    }
    private Level level() {
        Level level = new EmptyLevel(); level.setSize(3,3);
        level.mobs = new java.util.HashSet<>();
        level.traps = new com.watabou.utils.SparseArray<>();
        Arrays.fill(level.visited,true); Arrays.fill(level.passable,true);
        return level;
    }
    private Hero hero() { Hero hero = new Hero(); hero.pos=4; hero.fieldOfView=new boolean[9]; return hero; }
    @Test public void visibleNpcAndEnemiesBlockMovementButUnseenOccupantsDoNotLeak() {
        Level level=level(); Hero hero=hero();
        Ghost ghost=new Ghost(); ghost.pos=1; level.mobs.add(ghost);
        Rat rat=new Rat(); rat.pos=5; level.mobs.add(rat);
        hero.fieldOfView[1]=true;
        boolean[] walkable=SimulationNavigation.walkable(level,hero);
        assertFalse(walkable[1]); assertTrue(walkable[5]);
        hero.fieldOfView[5]=true;
        assertFalse(SimulationNavigation.walkable(level,hero)[5]);
        assertTrue("terrain is untouched",level.passable[1]);
    }
    @Test public void excludesUnknownTerrainPitsAndKnownActiveTrapsButNotSecretTraps() {
        Level level=level(); Hero hero=hero(); level.visited[0]=false; level.pit[2]=true;
        WornDartTrap trap=new WornDartTrap(); trap.pos=6; trap.visible=true; level.traps.put(6,trap);
        WornDartTrap hidden=new WornDartTrap(); hidden.pos=7; hidden.visible=false; level.traps.put(7,hidden);
        boolean[] pass=SimulationNavigation.walkable(level,hero);
        assertFalse(pass[0]); assertFalse(pass[2]); assertFalse(pass[6]); assertTrue(pass[7]);
        level.mapped[0]=true; trap.active=false;
        pass=SimulationNavigation.walkable(level,hero); assertTrue(pass[0]); assertTrue(pass[6]);
    }
    @Test public void explorationAvoidsNpcInteractionInsteadOfRepeatedlyChoosingAnOccupiedTile() {
        Level level=level(); Hero hero=hero();
        Ghost ghost=new Ghost(); ghost.pos=1; level.mobs.add(ghost); hero.fieldOfView[1]=true;
        int[] visits=new int[9]; Arrays.fill(visits,2); visits[1]=0;
        assertEquals(1,KnownMapExplorer.step(3,4,level.visited,level.passable,visits,-1));
        int next=KnownMapExplorer.step(3,4,level.visited,SimulationNavigation.walkable(level,hero),visits,-1);
        assertNotEquals(1,next); assertTrue(next>=0);
    }
    @Test public void inspectingMovementDoesNotConsumeGameplayRandomness() {
        Level level=level(); Hero hero=hero(); int expected;
        Random.pushGenerator(79139); try { expected=Random.Int(); } finally { Random.popGenerator(); }
        Random.pushGenerator(79139);
        try { SimulationNavigation.walkable(level,hero); assertEquals(expected,Random.Int()); }
        finally { Random.popGenerator(); }
    }
}
