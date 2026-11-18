package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

// Explicit experimental strategy, not a balance recommendation or production gameplay code.
public final class RecklessWarriorPolicy {
    public enum Action { HEAL, ATTACK, EXPLORE, STOP }
    public static final class Observation {
        public final int hp, maximumHp, nextHitUpperBound, healingPotions;
        public final boolean adjacentEnemy, finished;
        public Observation(int hp, int maximumHp, int nextHitUpperBound, int healingPotions,
                           boolean adjacentEnemy, boolean finished) {
            if (maximumHp <= 0 || hp < 0 || hp > maximumHp || nextHitUpperBound < 0 || healingPotions < 0)
                throw new IllegalArgumentException("invalid observed state");
            this.hp=hp; this.maximumHp=maximumHp; this.nextHitUpperBound=nextHitUpperBound;
            this.healingPotions=healingPotions; this.adjacentEnemy=adjacentEnemy; this.finished=finished;
        }
    }
    public Action choose(Observation state) {
        if (state.finished || state.hp == 0) return Action.STOP;
        if (state.adjacentEnemy && state.hp <= state.nextHitUpperBound && state.healingPotions > 0)
            return Action.HEAL;
        return state.adjacentEnemy ? Action.ATTACK : Action.EXPLORE;
    }
}
