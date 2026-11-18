package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Runs a supplied real-game adapter. Never substitutes synthetic probabilities for missing mechanics.
public final class WarriorHealingExperiment {
    public interface GameSession extends AutoCloseable {
        RecklessWarriorPolicy.Observation observe();
        boolean hasReceivedNormalFlyHealingDrop();
        String fingerprint();
        void perform(RecklessWarriorPolicy.Action action);
        @Override void close();
    }
    public interface Factory { GameSession start(long seed); }
    public static final class Result {
        public final long seed;
        public final boolean neededHeldHealingBeforeFlyDrop, completed;
        public final String stopReason;
        public final List<String> trace;
        Result(long seed, boolean needed, boolean completed, String stopReason, List<String> trace) {
            this.seed=seed; this.neededHeldHealingBeforeFlyDrop=needed; this.completed=completed;
            this.stopReason=stopReason; this.trace=Collections.unmodifiableList(new ArrayList<>(trace));
        }
    }
    public static Result run(long seed, Factory factory, int actionLimit) {
        if (actionLimit <= 0) throw new IllegalArgumentException("action limit must be positive");
        boolean needed=false;
        List<String> trace=new ArrayList<>();
        RecklessWarriorPolicy policy=new RecklessWarriorPolicy();
        try (GameSession game=factory.start(seed)) {
            String last=null;
            int stalled=0;
            for (int step=0; step<actionLimit; step++) {
                RecklessWarriorPolicy.Observation state=game.observe();
                RecklessWarriorPolicy.Action action=policy.choose(state);
                if (action==RecklessWarriorPolicy.Action.STOP)
                    return new Result(seed,needed,true,state.hp==0 ? "death" : "scenario finished",trace);
                boolean beforeFlyDrop = !game.hasReceivedNormalFlyHealingDrop();
                String fingerprint=game.fingerprint();
                stalled=fingerprint.equals(last) ? stalled+1 : 0;
                if (stalled>=8) return new Result(seed,needed,false,"no state progress",trace);
                last=fingerprint;
                trace.add(step+":"+action+":hp="+state.hp+"/"+state.maximumHp+":healing="+state.healingPotions+":state="+fingerprint);
                game.perform(action);
                // A proposed heal that fails must not become survival evidence.
                if (action==RecklessWarriorPolicy.Action.HEAL && beforeFlyDrop
                        && game.observe().healingPotions < state.healingPotions) needed=true;
            }
            RecklessWarriorPolicy.Observation finalState=game.observe();
            if (finalState.hp==0 || finalState.finished)
                return new Result(seed,needed,true,finalState.hp==0 ? "death" : "scenario finished",trace);
            return new Result(seed,needed,false,"action limit",trace);
        } catch (RuntimeException failure) {
            trace.add("failure:"+failure.getClass().getName()+":"+failure.getMessage());
            return new Result(seed,needed,false,"game adapter failure",trace);
        }
    }
    public static List<Result> runSeeds(long firstSeed, int count, Factory factory, int actionLimit) {
        if (count<=0) throw new IllegalArgumentException("count must be positive");
        List<Result> results=new ArrayList<>();
        for(int i=0;i<count;i++) results.add(run(Math.addExact(firstSeed,i),factory,actionLimit));
        return results;
    }
}
