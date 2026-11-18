/* ADHD Pixel Dungeon (C) 2026 Chris Charlton, GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.utils.Bundle;
import java.math.BigInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class SeedReuseScoreTest {
    private static final long BASE=330393;

    private static Bundle legacy(int version,long score,int attempts,int prior,int current,
                                 int bps,long earned) {
        Bundle b=new Bundle();
        b.put(SeedReuseScore.VERSION_KEY,version);
        b.put(SeedReuseScore.BASE_KEY,score);
        b.put(SeedReuseScore.EARNED_KEY,earned);
        b.put(SeedReuseScore.ATTEMPTS_KEY,attempts);
        b.put(SeedReuseScore.PRIOR_DEPTH_KEY,prior);
        b.put(SeedReuseScore.CURRENT_DEPTH_KEY,current);
        b.put(SeedReuseScore.RETENTION_KEY,bps);
        b.put(SeedReuseScore.KNOWN_KEY,true);
        return b;
    }

    @Test public void sevenPreviousMaxNineToTwentySixConservativeScoreIs219228() {
        SeedReuseScore.Result r=SeedReuseScore.calculate(BASE,7,9,26,true);
        assertEquals(6635,r.retentionBasisPoints);
        assertEquals(219228,r.earned);
        assertEquals(111165,r.deduction);
        assertEquals(BASE,r.before);
        assertFalse(r.individualDepthsKnown);
    }

    @Test public void fullHistorySixFloorFourOneFloorNineScoreIs255571() {
        int[] depths={4,4,4,4,4,4,9};
        SeedReuseScore.Result r=SeedReuseScore.calculate(BASE,7,9,26,true,depths);
        assertEquals(7735,r.retentionBasisPoints);
        assertEquals(255571,r.earned);
        assertEquals(74822,r.deduction);
        assertTrue(r.individualDepthsKnown);
        Bundle b=new Bundle();
        r.store(b);
        assertArrayEquals(depths,b.getIntArray(SeedReuseScore.DEPTHS_KEY));
        assertEquals(255571,SeedReuseScore.earnedScore(b,BASE));
    }

    @Test public void repeatFullRunDecaysByExactThreeFifths() {
        assertEquals(198236,SeedReuseScore.calculate(BASE,1,26,26,true,new int[]{26}).earned);
        assertEquals(71365,SeedReuseScore.calculate(BASE,3,26,26,true,new int[]{26,26,26}).earned);
        assertEquals(9249,SeedReuseScore.calculate(BASE,7,26,26,true,
                new int[]{26,26,26,26,26,26,26}).earned);
        assertEquals(0,SeedReuseScore.calculate(0,7,26,26,true,
                new int[]{26,26,26,26,26,26,26}).earned);
    }

    @Test public void repeatFloorOneTwentyOneTimesStillProtectsUnseenFloors() {
        int[] depths=new int[21]; java.util.Arrays.fill(depths,1);
        SeedReuseScore.Result r=SeedReuseScore.calculate(BASE,21,1,26,true,depths);
        assertEquals(9615,r.retentionBasisPoints);
        assertEquals(317686,r.earned);
        assertEquals(317686,SeedReuseScore.calculate(BASE,21,1,26,true).earned);
        assertTrue(SeedReuseScore.calculate(BASE,21,1,1,true,depths).earned>0);
    }

    @Test public void finalScoreRoundHalfUpTieAndMinimumOnePoint() {
        assertEquals(56,SeedReuseScore.calculate(125,3,4,4,true,new int[]{4,2,1}).earned);
        int[] depths=new int[100];
        java.util.Arrays.fill(depths,26);
        assertEquals(1,SeedReuseScore.calculate(BASE,100,26,26,true,depths).earned);
        assertEquals(1,SeedReuseScore.calculate(1,100,26,26,true,depths).earned);
        assertEquals(0,SeedReuseScore.calculate(0,100,26,26,true,depths).earned);
        assertEquals(BASE,SeedReuseScore.calculate(BASE,0,-1,26,true,new int[0]).earned);
    }

    @Test public void exactIntegerMathHandlesTenThousandPriorRuns() {
        int[] depths=new int[10000];
        java.util.Arrays.fill(depths,26);
        SeedReuseScore.Result r=SeedReuseScore.calculate(Long.MAX_VALUE,10000,26,26,true,depths);
        assertEquals(1,r.earned);
        assertEquals(0,r.retentionBasisPoints);
        assertEquals(Long.MAX_VALUE-1,r.deduction);
        Bundle b=new Bundle();r.store(b);
        assertEquals(1,SeedReuseScore.earnedScore(b,Long.MAX_VALUE));
    }

    @Test public void moreDepthCannotDecreaseRetentionAndMoreVisitsCannotIncreaseIt() {
        for (int n=1;n<=12;n++) {
            int[] depths=new int[n],more=new int[n+1];
            java.util.Arrays.fill(depths,4);
            java.util.Arrays.fill(more,4);
            int last=-1;
            for (int current=1;current<=26;current++) {
                SeedReuseScore.Result r=SeedReuseScore.calculate(BASE,n,4,current,true,depths);
                if (last>=0) assertTrue(r.retentionBasisPoints>=last);
                last=r.retentionBasisPoints;
                assertTrue(SeedReuseScore.calculate(BASE,n+1,4,current,true,more).earned<=r.earned);
            }
        }
    }

    @Test public void conservativeSummaryNeverExceedsRecordedDetailedHistory() {
        for (int n=1;n<=12;n++) {
            int[] depths=new int[n];java.util.Arrays.fill(depths,4);depths[n-1]=9;
            for (int current=1;current<=26;current++) {
                long exact=SeedReuseScore.calculate(BASE,n,9,current,true,depths).earned;
                long fallback=SeedReuseScore.calculate(BASE,n,9,current,true).earned;
                assertTrue(fallback<=exact);
            }
        }
        try {
            SeedReuseScore.calculate(BASE,7,9,26,true,new int[]{9});
            fail("Unverified partial individual history must not be accepted");
        } catch (IllegalArgumentException expected) {}
    }

    @Test public void realRankingAndSaveReloadUseFinalScoreNotJustUi() {
        Rankings.Record record=new Rankings.Record();
        record.score=(int)BASE;
        record.heroClass=HeroClass.WARRIOR;
        record.gameID="rational";
        record.customSeed="ABC-DEF-GHI";
        record.date="2026-11-14";
        record.version="v4.0.8";
        record.gameData=new Bundle();
        SeedReuseScore.calculate(BASE,7,9,26,true).store(record.gameData);
        assertEquals(219228,record.earnedScore());
        Rankings.Record lower=new Rankings.Record();
        lower.gameID="lower";lower.customSeed=record.customSeed;lower.score=200000;
        assertTrue(Rankings.scoreComparator.compare(record,lower)<0);
        Bundle save=new Bundle();record.storeInBundle(save);
        Rankings.Record reloaded=new Rankings.Record();reloaded.restoreFromBundle(save);
        assertEquals(219228,reloaded.earnedScore());
        assertEquals((int)BASE,reloaded.score);
    }

    @Test public void migrationV1ThroughV3UsesConservativeRecordedHistory() {
        Bundle v1=legacy(1,BASE,7,9,26,0,0);
        Bundle v2=legacy(2,BASE,7,9,26,1700,56166);
        Bundle v3=legacy(3,BASE,7,9,26,6650,219713);
        for(Bundle saved:new Bundle[]{v1,v2,v3}) {
            assertEquals(219228,SeedReuseScore.earnedScore(saved,BASE));
            assertTrue(SeedReuseScore.migrate(saved));
            assertEquals(6,saved.getInt(SeedReuseScore.VERSION_KEY));
            assertEquals(219228,saved.getLong(SeedReuseScore.EARNED_KEY));
            assertFalse(saved.getBoolean(SeedReuseScore.EXACT_KEY));
            assertFalse(SeedReuseScore.migrate(saved));
        }
    }

    @Test public void migrationV4DetailedAndSummaryKeepsEvidence() {
        Bundle exact=legacy(4,BASE,7,9,26,7981,263679);
        exact.put(SeedReuseScore.EXACT_KEY,true);
        exact.put(SeedReuseScore.DEPTHS_KEY,new int[]{4,4,4,4,4,4,9});
        Bundle summary=legacy(4,BASE,7,9,26,6538,216026);
        summary.put(SeedReuseScore.EXACT_KEY,false);
        assertEquals(255571,SeedReuseScore.earnedScore(exact,BASE));
        assertEquals(219228,SeedReuseScore.earnedScore(summary,BASE));
        assertTrue(SeedReuseScore.migrate(exact));
        assertTrue(SeedReuseScore.migrate(summary));
        assertEquals(6,exact.getInt(SeedReuseScore.VERSION_KEY));
        assertArrayEquals(new int[]{4,4,4,4,4,4,9},exact.getIntArray(SeedReuseScore.DEPTHS_KEY));
        assertEquals(255571,exact.getLong(SeedReuseScore.EARNED_KEY));
        assertEquals(219228,summary.getLong(SeedReuseScore.EARNED_KEY));
        assertFalse(summary.getBoolean(SeedReuseScore.EXACT_KEY));
    }

    @Test public void migrationV5ExponentialDetailedAndSummaryKeepsEvidence() {
        Bundle exact=legacy(5,BASE,7,9,26,7751,256098);
        exact.put(SeedReuseScore.EXACT_KEY,true);
        exact.put(SeedReuseScore.DEPTHS_KEY,new int[]{4,4,4,4,4,4,9});
        Bundle summary=legacy(5,BASE,7,9,26,6643,219480);
        summary.put(SeedReuseScore.EXACT_KEY,false);
        assertEquals(255571,SeedReuseScore.earnedScore(exact,BASE));
        assertEquals(219228,SeedReuseScore.earnedScore(summary,BASE));
        assertTrue(SeedReuseScore.migrate(exact));
        assertTrue(SeedReuseScore.migrate(summary));
        assertEquals(6,exact.getInt(SeedReuseScore.VERSION_KEY));
        assertEquals(255571,exact.getLong(SeedReuseScore.EARNED_KEY));
        assertArrayEquals(new int[]{4,4,4,4,4,4,9},exact.getIntArray(SeedReuseScore.DEPTHS_KEY));
        assertEquals(219228,summary.getLong(SeedReuseScore.EARNED_KEY));
        assertFalse(SeedReuseScore.migrate(exact));
        assertFalse(SeedReuseScore.migrate(summary));
    }

    @Test public void corruptedReceiptsAreRejectedNotRepriced() {
        Bundle fake=legacy(5,BASE,7,9,26,6643,219481);
        fake.put(SeedReuseScore.EXACT_KEY,false);
        assertNull(SeedReuseScore.restore(fake));
        assertFalse(SeedReuseScore.migrate(fake));
        Bundle good=new Bundle();
        SeedReuseScore.calculate(BASE,7,9,26,true,new int[]{4,4,4,4,4,4,9}).store(good);
        good.put(SeedReuseScore.DEPTHS_KEY,new int[]{9,9,9,9,9,9,9});
        assertNull(SeedReuseScore.restore(good));
        assertEquals(BASE,SeedReuseScore.earnedScore(good,BASE));
    }

    @Test public void unknownImportedSeedAndUnscoredHistoryNeverInventPenalty() {
        Bundle empty=new Bundle();
        assertEquals(BASE,SeedReuseScore.earnedScore(empty,BASE));
        assertFalse(SeedReuseScore.migrate(empty));
        assertEquals(BASE,SeedReuseScore.calculate(BASE,7,-1,26,false).earned);
        assertEquals(BASE,SeedReuseScore.calculate(BASE,0,-1,26,true,new int[0]).earned);
        assertEquals(BASE,SeedReuseScore.calculate(BASE,7,-1,26,true).earned);
    }
}
