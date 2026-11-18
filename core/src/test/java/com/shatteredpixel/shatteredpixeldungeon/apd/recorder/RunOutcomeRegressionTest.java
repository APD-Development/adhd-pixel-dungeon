package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.apd.recorder.RecorderTestSupport.*;

public class RunOutcomeRegressionTest {
    @After public void cleanup(){ApdRecorder.reset();}

    private void check(Session s,String id,int outcome,boolean ascended)throws Exception {
        ReadResult r=s.read(id);
        assertEquals(ReadStatus.SEALED,r.status);
        assertEquals(1,ofType(r,EventTypes.RUN_END).size());
        LifecycleEvents.RunEnd end=LifecycleEvents.decodeRunEnd(ofType(r,EventTypes.RUN_END).get(0),r);
        assertEquals(outcome,end.outcome);
        assertTrue(end.ascendedKnown);
        assertEquals(ascended,end.ascended);
        assertEquals(outcome,RunFactsBuilder.fromLog(r,id).numOr(FactKeys.OUTCOME,-1));
        DatasetStore store=new DatasetStore(s.root);
        Dataset runs=store.load().dataset(Datasets.RUNS);
        Row row=null;
        for(Row candidate:runs.rows)if(candidate.runId.equals(id) && !candidate.duplicate)row=candidate;
        assertNotNull(row);
        assertEquals(outcome,row.num(runs.def.indexOf(FactKeys.OUTCOME)));
        assertEquals(ascended?1:0,row.num(runs.def.indexOf("run.ascended")));
        // Reopen both derived cache layers; they must agree with the sealed raw log.
        assertEquals(outcome,new DerivedStore(s.root).load().apdRows.get(0).numOr(FactKeys.OUTCOME,-1));
        assertNotNull(new DatasetStore(s.root).load().dataset(Datasets.RUNS));
    }

    @Test public void deathCleanupBeforeRankingDoesNotSealAsAbandoned()throws Exception {
        Session s=new Session();newRun(s,92001);String id=ApdRecorder.currentRunId();enterLevel(1);
        Dungeon.hero.HP=0;
        ApdCombat.onHeroDying(Hunger.class);
        Dungeon.deleteGameAfterDeath(s.slot,true);
        assertEquals("Death cleanup leaves final ranking recording to fail",id,ApdRecorder.currentRunId());
        Dungeon.fail(Hunger.class);
        check(s,id,LifecycleEvents.OUTCOME_DIED,false);
        ReadResult r=RunLogReader.read(s.log(id));
        assertEquals(Statistics.totalScore,LifecycleEvents.decodeRunEnd(ofType(r,EventTypes.RUN_END).get(0),r).score);
        assertEquals(1,ofType(r,EventTypes.RANKING_LINK).size());
    }

    @Test public void doomDeathRecordedBeforeCleanupStaysOneDeath()throws Exception {
        Session s=new Session();newRun(s,92002);String id=ApdRecorder.currentRunId();enterLevel(1);
        Dungeon.fail(Hunger.class);Dungeon.deleteGameAfterDeath(s.slot,true);
        check(s,id,LifecycleEvents.OUTCOME_DIED,false);
    }

    @Test public void explicitAbandonmentRemainsAbandonedEvenWithADeathEvent()throws Exception {
        Session s=new Session();newRun(s,92003);String id=ApdRecorder.currentRunId();enterLevel(1);
        ApdCombat.onHeroDying(Hunger.class);
        Dungeon.hero.HP=0;
        Dungeon.deleteGame(s.slot,true);
        check(s,id,LifecycleEvents.OUTCOME_ABANDONED,false);
    }

    @Test public void savedLoadedRunCanDieAfterCleanup()throws Exception {
        Session s=new Session();newRun(s,92004);String id=ApdRecorder.currentRunId();enterLevel(1);
        s.restartApp();Dungeon.loadGame(s.slot,true);Dungeon.level=Dungeon.loadLevel(s.slot);ApdRecorder.onLevelSwitched();
        assertEquals(id,ApdRecorder.currentRunId());
        Dungeon.deleteGameAfterDeath(s.slot,true);Dungeon.fail(Hunger.class);
        check(s,id,LifecycleEvents.OUTCOME_DIED,false);
    }

    @Test public void normalVictoryIsWonWithoutAscension()throws Exception {
        Session s=new Session();newRun(s,92005);String id=ApdRecorder.currentRunId();enterLevel(1);
        Statistics.ascended=false;Dungeon.win(Hunger.class);Dungeon.deleteGame(s.slot,true);
        check(s,id,LifecycleEvents.OUTCOME_WON,false);
    }

    @Test public void ascensionIsAWonRunWithAnExplicitAscensionFact()throws Exception {
        Session s=new Session();newRun(s,92006);String id=ApdRecorder.currentRunId();enterLevel(1);
        Statistics.ascended=true;Dungeon.win(Hunger.class);Dungeon.deleteGame(s.slot,true);
        check(s,id,LifecycleEvents.OUTCOME_WON,true);
    }
}
