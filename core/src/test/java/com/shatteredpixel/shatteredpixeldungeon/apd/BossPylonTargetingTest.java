/* ADHD Pixel Dungeon (C) 2026 Chris Charlton, GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.BossPylonTargeting;
import org.junit.Test;
import static org.junit.Assert.*;

/** Targeting rules only: no change to damage or Pylon activation. */
public class BossPylonTargetingTest {
    @Test public void activePylonBeatsOtherVisibleReachableEnemiesButInactivePylonDoesNot() {
        HeadlessTestSupport.start();
        int previous = Dungeon.challenges;
        try {
            Dungeon.challenges=0;
            Rat rat=new Rat();
            Pylon pylon=new Pylon();
            assertEquals(Char.Alignment.NEUTRAL,pylon.alignment);
            assertFalse(BossPylonTargeting.isActivePylon(pylon));
            assertFalse(BossPylonTargeting.prefer(pylon,rat));
            pylon.alignment=Char.Alignment.ENEMY;
            assertTrue(BossPylonTargeting.isActivePylon(pylon));
            assertTrue(BossPylonTargeting.prefer(pylon,rat));
            assertFalse(BossPylonTargeting.prefer(rat,pylon));
        } finally { Dungeon.challenges=previous; }
    }
    @Test public void meleeCandidateSelectionNeverChoosesInactiveOrUnreachablePylons(){
        HeadlessTestSupport.start();
        int previous=Dungeon.challenges;
        try {
            Dungeon.challenges=0;
            Rat rat=new Rat();
            Pylon inactive=new Pylon();
            Pylon active=new Pylon();
            active.alignment=Char.Alignment.ENEMY;
            java.util.List<com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob> available=
                    java.util.Arrays.asList(rat,inactive,active);
            assertSame(active,BossPylonTargeting.activeAttackablePylon(available));
            // Eligibility is provided by the caller's normal melee reach filter.
            assertNull(BossPylonTargeting.activeAttackablePylon(java.util.Arrays.asList(rat,inactive)));
            assertNull(BossPylonTargeting.activeAttackablePylon(java.util.Collections.emptyList()));
            assertNull(BossPylonTargeting.activeAttackablePylon(null));
        } finally { Dungeon.challenges=previous; }
    }
    @Test public void onlySuperchargedDM300IsReplacedByActivePylon() {
        HeadlessTestSupport.start();
        int previous = Dungeon.challenges;
        try {
            Dungeon.challenges=0;
            DM300 dm=new DM300();
            Pylon pylon=new Pylon();
            pylon.alignment=Char.Alignment.ENEMY;
            assertFalse(BossPylonTargeting.replaceInvulnerableBoss(dm,pylon));
            dm.supercharged=true;
            assertTrue(BossPylonTargeting.replaceInvulnerableBoss(dm,pylon));
            pylon.alignment=Char.Alignment.NEUTRAL;
            assertFalse(BossPylonTargeting.replaceInvulnerableBoss(dm,pylon));
            pylon.alignment=Char.Alignment.ENEMY;
            assertFalse(BossPylonTargeting.replaceInvulnerableBoss(new Rat(),pylon));
        } finally { Dungeon.challenges=previous; }
    }
}
