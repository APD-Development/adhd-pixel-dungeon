package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

/** A dialog can resolve once, including Back and repeated/late input. No game time or RNG. */
public final class ConfirmationDecision {
    private Runnable confirmed, cancelled;
    private boolean decided;
    public ConfirmationDecision(Runnable confirmed,Runnable cancelled){this.confirmed=confirmed;this.cancelled=cancelled;}
    public void choose(boolean confirm){
        Runnable action;
        synchronized(this){if(decided)return;decided=true;action=confirm?confirmed:cancelled;confirmed=cancelled=null;}
        if(action!=null)action.run();
    }
    public synchronized boolean decided(){return decided;}
}
