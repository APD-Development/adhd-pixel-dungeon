/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.watabou.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Small, local-only support trace. Never reads saves, player history or other apps. */
public final class SupportDiagnostics {
    private static final Object LOCK=new Object();
    private static final int LIMIT=65536, EVENTS=128, MEMORY_EVENTS=48;
    private static final long MEMORY_INTERVAL_NANOS=30_000_000_000L;
    private static final ArrayDeque<String> trace=new ArrayDeque<>();
    // Keep memory trends independently: high-volume touch events otherwise evict them.
    private static final ArrayDeque<String> memoryTrace=new ArrayDeque<>();
    private static final ExecutorService writer=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"apd-diagnostics");t.setDaemon(true);return t;});
    private static File directory;
    private static String environment="Not initialized",previous="No previous session trace available.";
    private static String writeFailure="";
    private static long revision;
    //APD: the trace file is rewritten at most once a second; important events flush at once
    private static final long WRITE_INTERVAL_MILLIS=1000;
    private static boolean flushNow;
    private static int traceChars;
    private static final java.util.concurrent.atomic.AtomicLong inputIds=new java.util.concurrent.atomic.AtomicLong();
    public static volatile java.util.function.Supplier<String> platformSnapshot=()->"Platform details unavailable.";
    // The game's saved settings (APD: every APD setting and whether it's saved or still on its default), for bug reports.
    public static volatile java.util.function.Supplier<String> appSettings=()->"";
    private static boolean scheduled;
    private static long lastPeriodicMemoryNanos;
    private static final byte[] EMERGENCY="Uncaught failure; detailed capture failed, possibly because memory was exhausted. This is not proof of an out-of-memory cause.\n".getBytes(StandardCharsets.UTF_8);
    private SupportDiagnostics(){}
    public static long nextInputId(){return inputIds.incrementAndGet();}
    public static void input(String phase,String kind,long id,long queuedNanos,long sourceNanos){
        try {
            long now=System.nanoTime();
            event("input."+phase,"id="+id+" kind="+kind+" queued_nanos="+queuedNanos+" app_queue_ms="+Math.max(0,(now-queuedNanos)/1000000L)
                    +" source_nanos="+sourceNanos+" source_age_ms="+(sourceNanos>0?Long.toString(Math.max(0,(now-sourceNanos)/1000000L)):"unavailable"));
        }catch(RuntimeException|OutOfMemoryError unavailable){writeFailure="Input diagnostic capture unavailable";}
    }

    public static void configure(File folder,String details) {
        synchronized(LOCK){
            try{
                File target=folder.getCanonicalFile();
                if(target.equals(directory))return; // Activity recreation is not a new process session.
                if(!target.isDirectory()&&!target.mkdirs())throw new IOException("Cannot create diagnostics folder");
                directory=target;environment=shortText(details,2048);
                previous=read(new File(target,"session.txt"));
                trace.clear();memoryTrace.clear();traceChars=0;lastPeriodicMemoryNanos=0;revision++;
                if(!previous.isEmpty())atomic(new File(target,"previous-session.txt"),previous);
                else previous="No previous session trace available.";
            }catch(IOException e){writeFailure=e.getClass().getSimpleName();}
        }
        event("process.start", "");
    }

    private static String shortText(String text,int limit){
        if(text==null)return "";return text.length()<=limit?text:text.substring(0,limit)+" [truncated]";
    }
    // The configured root is canonical; reject aliases before reading or writing diagnostics.
    private static File directDiagnosticFile(File file)throws IOException{
        if(!file.getAbsoluteFile().equals(file.getCanonicalFile()))throw new IOException("Diagnostic file alias refused");
        return file;
    }
    public static void event(String kind,String detail){
        try { synchronized(LOCK){
            // Keep CR and LF from turning one diagnostic record into multiple apparent events.
            String line=System.currentTimeMillis()+" "+shortText(kind,80).replace('\r',' ').replace('\n',' ')
                    +" "+shortText(detail,"memory.spike.thread".equals(kind)?2400:700).replace('\r',' ').replace('\n',' ');
            // Preserve selected safe scene and memory milestones in the extended,
            // opt-in report chronology. Never persist exception messages or typed values.
            if (kind != null && (kind.equals("scene.request") || kind.equals("scene.begin")
                    || kind.equals("scene.ready") || kind.equals("game.pause")
                    || kind.equals("game.resume") || kind.equals("memory.sample")
                    || kind.equals("input.discard") || kind.equals("ui.scale"))) {
                SupportActionJournal.record(kind,detail);
            } else if ("error".equals(kind)) {
                String type=detail==null?"unknown":detail.split(":",2)[0];
                SupportActionJournal.record("error.class",type);
            }
            trace.addLast(line);traceChars+=line.length()+1;
            if ("memory.sample".equals(kind) || "frame.hitch".equals(kind)
                    || kind.startsWith("memory.spike")) {
                memoryTrace.addLast(line);
                while (memoryTrace.size()>MEMORY_EVENTS+40) memoryTrace.removeFirst();
            }
            while(trace.size()>EVENTS || traceChars>14000)traceChars-=trace.removeFirst().length()+1;
            revision++;
            if (kind != null && (kind.equals("error") || kind.equals("game.pause") || kind.equals("activity.pause")
                    || kind.equals("android.low_memory") || kind.equals("android.trim_memory")
                    || kind.equals("report.requested") || kind.equals("process.start"))) {
                flushNow=true;LOCK.notifyAll();
            }
            if(directory!=null&&!scheduled){scheduled=true;writer.execute(SupportDiagnostics::drain);}
        }
        } catch(RuntimeException | OutOfMemoryError unavailable) { writeFailure="Diagnostic capture unavailable"; }
    }
    /** A bounded phase observation, without forcing GC or inspecting player history. */
    public static void memoryEvent(String phase){
        memoryEvent(phase, "");
    }
    /** Keep the OS trim severity alongside the heap sample even if touch events flood the recent-event ring. */
    public static void memoryEvent(String phase,int trimLevel){
        memoryEvent(phase, " trim_level="+trimLevel);
    }
    private static void memoryEvent(String phase,String context){
        try{
            Runtime runtime=Runtime.getRuntime();
            long committed=runtime.totalMemory(),free=runtime.freeMemory();
            event("memory.sample","phase="+shortText(phase,80)+" used_bytes="+(committed-free)
                    +" committed_bytes="+committed+" limit_bytes="+runtime.maxMemory()+context);
        }catch(RuntimeException | OutOfMemoryError unavailable){
            writeFailure="Memory diagnostic capture unavailable";
        }
    }
    /** On the render thread, sample at most once every 30 wall-clock seconds.
     * Avoids per-frame allocations, forced GC and any reads of player history. */
    //APD: Developer-build stutter probe. Off everywhere else; costs one nanoTime per frame.
    public static volatile boolean hitchProbe;
    private static long lastFrameNanos, lastUsedBytes;
    private static final long HITCH_NANOS=100_000_000L, IGNORE_NANOS=5_000_000_000L;
    /** Called once per frame on the render thread. Logs frames slower than 100 ms with memory in use. */
    public static void frame(Object scene){
        if(!hitchProbe)return;
        long now=System.nanoTime(),gap=now-lastFrameNanos;
        lastFrameNanos=now;
        if(gap<HITCH_NANOS||gap>IGNORE_NANOS)return; // a gap over 5 s is the app being paused, not a stutter
        try{
            Runtime runtime=Runtime.getRuntime();
            long used=runtime.totalMemory()-runtime.freeMemory();
            long delta=lastUsedBytes==0?0:used-lastUsedBytes;
            lastUsedBytes=used;
            event("frame.hitch","ms="+(gap/1000000L)+" scene="+(scene==null?"none":scene.getClass().getSimpleName())+" used_bytes="+used
                    +" change_since_last_hitch="+delta+" limit_bytes="+runtime.maxMemory());
        }catch(RuntimeException|OutOfMemoryError unavailable){writeFailure="Hitch capture unavailable";}
    }
    //APD Developer build: memory spike watcher. Samples the heap 5x a second on its own
    //thread; when the heap grows by 40 MB within a second (or passes 75% of the limit),
    //it records what every thread is doing at that moment, at most once per 10 seconds.
    private static Thread spikeWatcher;
    public static synchronized void startSpikeWatcher(){
        if(spikeWatcher!=null)return;
        spikeWatcher=new Thread(()->{
            Runtime runtime=Runtime.getRuntime();
            long[] recent=new long[5];int index=0;long lastDump=0;
            while(true){
                try{Thread.sleep(200);}catch(InterruptedException stop){return;}
                long used=runtime.totalMemory()-runtime.freeMemory(),limit=runtime.maxMemory();
                long oldest=recent[index];recent[index]=used;index=(index+1)%recent.length;
                long now=System.currentTimeMillis();
                boolean growing=oldest>0&&used-oldest>40L*1024*1024;
                boolean high=used>limit*3/4;
                if((growing||high)&&now-lastDump>10_000){
                    lastDump=now;
                    try{
                        event("memory.spike","used_bytes="+used+" grew_last_second="+(oldest>0?used-oldest:0)+" limit_bytes="+limit);
                        for(Map.Entry<Thread,StackTraceElement[]> e:Thread.getAllStackTraces().entrySet()){
                            Thread t=e.getKey();
                            if(t==Thread.currentThread()||e.getValue().length==0)continue;
                            StringBuilder b=new StringBuilder("thread=").append(t.getName()).append(" state=").append(t.getState());
                            StackTraceElement[] frames=e.getValue();
                            for(int i=0;i<Math.min(12,frames.length);i++)b.append(" | ").append(frames[i]);
                            event("memory.spike.thread",b.toString());
                        }
                        synchronized(LOCK){flushNow=true;LOCK.notifyAll();}
                    }catch(RuntimeException|OutOfMemoryError ignored){}
                }
            }
        },"apd-memory-spike-watcher");
        spikeWatcher.setDaemon(true);
        spikeWatcher.setPriority(Thread.MIN_PRIORITY);
        spikeWatcher.start();
    }

    /** A scene change or resume legitimately takes long; do not count that frame. */
    public static void resetFrameClock(){lastFrameNanos=System.nanoTime();}

    public static void periodicMemoryEvent(){
        long now=System.nanoTime();
        if (lastPeriodicMemoryNanos!=0 && now-lastPeriodicMemoryNanos>=0
                && now-lastPeriodicMemoryNanos<MEMORY_INTERVAL_NANOS) return;
        lastPeriodicMemoryNanos=now;
        memoryEvent("game.active");
    }
    public static void error(Throwable error){
        if(error==null)return;
        try {
            event("error",error.getClass().getName()+": "+shortText(error.getMessage(),300));
            StackTraceElement[] frames=error.getStackTrace();
            for(int i=0;i<Math.min(frames.length,12);i++)event("at",frames[i].toString());
        } catch(RuntimeException | OutOfMemoryError unavailable) {
            // Diagnostic allocation must never replace the failure being reported.
            writeFailure="Error diagnostic capture unavailable";
        }
    }
    private static String snapshot(){
        StringBuilder out=new StringBuilder("APD diagnostic session v1\n").append(environment).append('\n');
        Runtime r=Runtime.getRuntime();
        out.append("Java heap bytes: used=").append(r.totalMemory()-r.freeMemory()).append(" committed=").append(r.totalMemory()).append(" limit=").append(r.maxMemory()).append('\n');
        try { String settings=appSettings.get(); if(settings!=null && !settings.isEmpty()) out.append("APD SETTINGS (unset = nothing saved, default applies)\n").append(settings); }
        catch(RuntimeException unavailable) { out.append("APD SETTINGS unavailable\n"); }
        out.append("MEMORY HISTORY (bounded, most recent first not guaranteed by crash)\n");
        for(String line:memoryTrace)out.append(line).append('\n');
        out.append("RECENT EVENTS\n");
        for(String line:trace)out.append(line).append('\n');
        return shortText(out.toString(),LIMIT);
    }
    private static void drain(){
        try { for(;;){
            File target;String contents;long written;
            synchronized(LOCK){target=directory;contents=snapshot();written=revision;}
            try{atomic(new File(target,"session.txt"),contents);}
            catch(IOException e){synchronized(LOCK){writeFailure=e.getClass().getSimpleName();}}
            synchronized(LOCK){
                if(written==revision){scheduled=false;flushNow=false;LOCK.notifyAll();return;}
                //more events arrived: coalesce them for up to a second instead of rewriting at once
                long until=System.currentTimeMillis()+WRITE_INTERVAL_MILLIS;
                while(!flushNow){
                    long left=until-System.currentTimeMillis();
                    if(left<=0)break;
                    try{LOCK.wait(left);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();break;}
                }
                flushNow=false;
            }
        }
        } catch(RuntimeException | OutOfMemoryError unavailable) { synchronized(LOCK){writeFailure="Diagnostic writer unavailable";scheduled=false;LOCK.notifyAll();} }
    }
    private static void atomic(File file,String contents)throws IOException {
        File temp=new File(file.getPath()+".tmp");
        directDiagnosticFile(file);
        directDiagnosticFile(temp);
        try(FileOutputStream out=new FileOutputStream(temp)){out.write(contents.getBytes(StandardCharsets.UTF_8));}
        if(!temp.renameTo(file))throw new IOException("Cannot replace diagnostic trace");
    }
    private static String read(File file)throws IOException {
        // A symlinked session/crash file must not leak unrelated saved data into a report.
        if(!file.getAbsoluteFile().equals(file.getCanonicalFile()) || !file.isFile())return "";
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
            byte[] block=new byte[4096];int n,left=LIMIT;
            while(left>0&&(n=in.read(block,0,Math.min(block.length,left)))!=-1){bytes.write(block,0,n);left-=n;}
            return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    /** Call from an export worker. A report is a snapshot, not a diagnosis. */
    public static String report(String platformDetails){
        synchronized(LOCK){
            String crash="";
            try{if(directory!=null)crash=read(new File(directory,"last-crash.txt"));}catch(IOException e){crash="Unavailable: "+e.getClass().getSimpleName();}
            return "APD SUPPORT REPORT v1\nGenerated: "+new Date()+"\n"
                    +"This report records observations. Missing evidence does not rule out a failure.\n"
                    +"No saved games, inventory contents, run histories or device identifiers are included.\n\n"
                    +shortText(platformDetails,16384)+"\n\nCURRENT SESSION\n"+snapshot()
                    +"\nPREVIOUS SESSION\n"+previous+"\nLAST RECORDED UNCAUGHT FAILURE (may be older)\n"+crash
                    +"\nDiagnostic persistence status: "+(writeFailure.isEmpty()?"no write failure recorded":writeFailure)+"\n";
        }
    }
    public static String report(){
        String platform;
        try{platform=platformSnapshot.get();}catch(Throwable unavailable){platform="Platform snapshot unavailable: "+unavailable.getClass().getSimpleName();}
        return report(platform);
    }
    /** Android's existing crash handler still receives the original exception. */
    public static void uncaught(Thread thread,Throwable failure){
        try{
            error(failure);File root;String text;
            synchronized(LOCK){root=directory;text=snapshot();}
            if(root!=null)try(FileOutputStream out=new FileOutputStream(directDiagnosticFile(new File(root,"last-crash.txt")))){out.write(text.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
        }catch(Throwable captureFailed){
            try{File root=directory;if(root!=null)try(FileOutputStream out=new FileOutputStream(directDiagnosticFile(new File(root,"last-crash.txt")))){out.write(EMERGENCY);}}catch(Throwable ignored){}
        }
    }
    public static boolean awaitWrites(long millis)throws InterruptedException {
        long deadline=System.currentTimeMillis()+millis;
        synchronized(LOCK){flushNow=true;LOCK.notifyAll();while(scheduled){long left=deadline-System.currentTimeMillis();if(left<=0)return false;LOCK.wait(left);}return true;}
    }
}
