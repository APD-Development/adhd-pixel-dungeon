package com.shatteredpixel.shatteredpixeldungeon.apd.demo;

import java.io.File;
import java.util.concurrent.Executor;

/** Coalesces validated history reads without making title rendering wait for disk. */
public final class TitleUnlockProbe {
    public interface Counter { int count(File root); }
    private final Executor worker;
    private final Counter counter;
    private File requestedRoot;
    private long generation;
    private boolean requested;
    private Boolean earned;
    public TitleUnlockProbe(Executor worker, Counter counter){this.worker=worker;this.counter=counter;}
    public synchronized void invalidate(){generation++;requested=false;earned=null;requestedRoot=null;}
    public synchronized boolean earned(File root){return root!=null&&root.equals(requestedRoot)&&Boolean.TRUE.equals(earned);}
    public synchronized void request(final File root){
        if(root==null)return;
        if(requested&&root.equals(requestedRoot))return;
        final long ticket=++generation;
        requestedRoot=root;requested=true;earned=null;
        try{
            worker.execute(()->{
                Boolean result=null;
                try{result=counter.count(root)>=UnlockProgression.TEN_RUNS;}
                catch(Exception failure){/* Unknown history must never grant an unlock. Retry on next title. */}
                synchronized(TitleUnlockProbe.this){
                    if(ticket!=generation)return;
                    earned=result;
                    if(result==null)requested=false;
                }
            });
        }catch(RuntimeException failure){requested=false;}
    }
}
