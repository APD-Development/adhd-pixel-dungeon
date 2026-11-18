package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;
public class WarriorHealingExperimentTest {
    private static RecklessWarriorPolicy.Observation state(int hp,int potion,boolean enemy) {
        return new RecklessWarriorPolicy.Observation(hp,20,5,potion,enemy,false);
    }
    @Test public void healsOnlyWhenNextHitCouldKillAndAPotionIsHeld() {
        RecklessWarriorPolicy policy=new RecklessWarriorPolicy();
        assertEquals(RecklessWarriorPolicy.Action.ATTACK,policy.choose(state(6,1,true)));
        assertEquals(RecklessWarriorPolicy.Action.HEAL,policy.choose(state(5,1,true)));
        assertEquals(RecklessWarriorPolicy.Action.ATTACK,policy.choose(state(5,0,true)));
        assertEquals(RecklessWarriorPolicy.Action.EXPLORE,policy.choose(state(1,1,false)));
        assertEquals(RecklessWarriorPolicy.Action.STOP,policy.choose(state(0,1,true)));
    }
    private static class Fixture implements WarriorHealingExperiment.GameSession {
        int actions; boolean fly,closed,stuck;
        public RecklessWarriorPolicy.Observation observe(){return new RecklessWarriorPolicy.Observation(5,20,5,actions==0?1:0,true,actions>=1&&!stuck);}
        public boolean hasReceivedNormalFlyHealingDrop(){return fly;}
        public String fingerprint(){return "step="+actions;}
        public void perform(RecklessWarriorPolicy.Action action){if(!stuck)actions++;}
        public void close(){closed=true;}
    }
    @Test public void countsHeldPotionNeedBeforeFlyDropButNotAfterAndAlwaysClosesSession() {
        Fixture before=new Fixture();
        assertTrue(WarriorHealingExperiment.run(1,seed->before,20).neededHeldHealingBeforeFlyDrop);
        assertTrue(before.closed);
        Fixture after=new Fixture();after.fly=true;
        assertFalse(WarriorHealingExperiment.run(1,seed->after,20).neededHeldHealingBeforeFlyDrop);
        assertTrue(after.closed);
    }
    @Test public void stuckAndLimitedScenariosAreIncompleteRatherThanReportedAsSuccessfulRuns() {
        Fixture stuck=new Fixture();stuck.stuck=true;
        WarriorHealingExperiment.Result result=WarriorHealingExperiment.run(2,seed->stuck,20);
        assertFalse(result.completed);assertEquals("no state progress",result.stopReason);assertTrue(stuck.closed);
        Fixture limited=new Fixture(){public String fingerprint(){return "attempt="+actions;}public void perform(RecklessWarriorPolicy.Action a){actions++;}public RecklessWarriorPolicy.Observation observe(){return state(20,0,false);}};
        assertFalse(WarriorHealingExperiment.run(2,seed->limited,1).completed);
    }
    @Test public void batchRetainsEverySeedAndActionTraceWithoutClaimingRealGameEvidence() {
        List<WarriorHealingExperiment.Result> fixtures=WarriorHealingExperiment.runSeeds(100,100,seed->new Fixture(),20);
        assertEquals(100,fixtures.size());
        for(int i=0;i<100;i++){assertEquals(100+i,fixtures.get(i).seed);assertEquals(1,fixtures.get(i).trace.size());}
    }
    @Test public void terminalStateAfterTheLastAllowedActionStillCountsAsCompleted() {
        assertTrue(WarriorHealingExperiment.run(3,seed->new Fixture(),1).completed);
    }
    @Test public void failedHealingDoesNotCountAsAConsumedSurvivalPotion() {
        Fixture failed=new Fixture(){public void perform(RecklessWarriorPolicy.Action a){}public RecklessWarriorPolicy.Observation observe(){return state(5,1,true);}};
        WarriorHealingExperiment.Result result=WarriorHealingExperiment.run(4,seed->failed,1);
        assertFalse(result.neededHeldHealingBeforeFlyDrop);
        assertFalse(result.completed);
    }
    @Test public void adapterFailureIsRecordedAndDoesNotStopLaterSeeds() {
        Fixture broken=new Fixture(){public void perform(RecklessWarriorPolicy.Action a){throw new IllegalStateException("controlled failure");}};
        List<WarriorHealingExperiment.Result> results=WarriorHealingExperiment.runSeeds(10,2,seed->seed==10?broken:new Fixture(),2);
        assertFalse(results.get(0).completed);
        assertEquals("game adapter failure",results.get(0).stopReason);
        assertTrue(results.get(0).trace.get(1).contains("controlled failure"));
        assertTrue(broken.closed);
        assertTrue(results.get(1).completed);
    }
    @Test public void cleanupFailurePreventsReportingAnExperimentAsSuccessfullyCompleted() {
        Fixture broken=new Fixture(){public void close(){throw new IllegalStateException("cleanup failure");}};
        assertFalse(WarriorHealingExperiment.run(15,seed->broken,2).completed);
    }
}
