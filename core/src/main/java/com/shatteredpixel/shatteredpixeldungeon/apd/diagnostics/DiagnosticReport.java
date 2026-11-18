package com.shatteredpixel.shatteredpixeldungeon.apd.diagnostics;

import com.shatteredpixel.shatteredpixeldungeon.apd.archive.ApdFiles;
import com.watabou.utils.FileUtils;
import com.watabou.utils.SupportDiagnostics;
import com.watabou.utils.SupportActionJournal;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.Deflater;

public final class DiagnosticReport {
    private DiagnosticReport(){}
    public interface Listener { void finished(String message); }
    // Thread creation/start can fail under memory pressure after the file picker
    // has already transferred stream ownership. Close that stream and complete
    // the request once; do not leave the player with a silent export failure.
    interface WorkerLauncher { void launch(Runnable job); }
    static void launchExportWorker(OutputStream stream, Listener once, Runnable job, WorkerLauncher launcher){
        try{
            launcher.launch(job);
        }catch(OutOfMemoryError exhausted){
            try{stream.close();}catch(IOException ignored){}
            once.finished("The diagnostic report could not start because memory was exhausted.");
        }catch(RuntimeException failure){
            try{stream.close();}catch(IOException ignored){}
            once.finished("The diagnostic report could not start: "+failure.getClass().getSimpleName());
        }
    }
    /** Standard is byte-for-byte the original support report; Extended adds the on-device action journal. */
    public static void export(Listener listener) { export(listener, false); }

    public static void export(Listener listener, boolean extended){
        SupportDiagnostics.event("report.requested",extended?"extended":"");
        SimpleDateFormat date=new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT);date.setTimeZone(TimeZone.getTimeZone("UTC"));
        String name="APD-diagnostic-"+(extended?"extended-":"")+date.format(new Date())+"-UTC."+(extended?"zip":"txt");
        // Platform pickers may fail before calling any ExportTarget callback. Report
        // that failure once, without misreporting a stream already accepted by the picker.
        final java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.concurrent.atomic.AtomicBoolean streamAccepted = new java.util.concurrent.atomic.AtomicBoolean();
        Listener once = message -> {
            if (finished.compareAndSet(false, true)) listener.finished(message);
        };
        try {
        ApdFiles.provider(FileUtils.getFileHandle("apd-diagnostics").file()).chooseExport(name,extended?"application/zip":"text/plain",new ApdFiles.ExportTarget(){
            public void onStream(OutputStream stream,String displayName){
                streamAccepted.set(true);
                launchExportWorker(stream, once, ()->{
                    String result="The diagnostic report could not be saved.";
                    SupportActionJournal.Checkpoint checkpoint=null;
                    boolean failed=false;
                    try(OutputStream out=stream){
                        if(extended){
                            checkpoint=SupportActionJournal.checkpoint();
                            // Maximum standard Java/Android DEFLATE compression, streaming to
                            // the export provider. Never accumulate the journal in memory.
                            // Closing ZIP writes the central directory before acknowledging.
                            // The outer try owns the provider stream: do not close it twice.
                            ZipOutputStream zip=new ZipOutputStream(out, StandardCharsets.UTF_8);
                                zip.setLevel(Deflater.BEST_COMPRESSION);
                                zip.putNextEntry(new ZipEntry("APD-diagnostic.txt"));
                                zip.write(SupportDiagnostics.report().getBytes(StandardCharsets.UTF_8));
                                zip.closeEntry();
                                String notes=BugNotes.read();
                                if(!notes.isEmpty()){
                                    zip.putNextEntry(new ZipEntry("APD-bug-notes.txt"));
                                    zip.write(notes.getBytes(StandardCharsets.UTF_8));
                                    zip.closeEntry();
                                }
                                zip.putNextEntry(new ZipEntry("APD-action-history.txt"));
                                SupportActionJournal.export(zip,checkpoint);
                                zip.closeEntry();
                                zip.finish();
                                zip.flush();
                        } else {
                            // Preserve the original Standard bug report bytes and MIME.
                            out.write(SupportDiagnostics.report().getBytes(StandardCharsets.UTF_8));
                            // Developer build bug notes, only when some exist (otherwise unchanged bytes)
                            out.write(withBugNotes("",BugNotes.read()).getBytes(StandardCharsets.UTF_8));
                            out.flush();
                        }
                    }catch(IOException|RuntimeException error){
                        failed=true;
                        result="The diagnostic report could not be saved: "+error.getClass().getSimpleName();
                    }catch(OutOfMemoryError exhausted){
                        failed=true;
                        result="The diagnostic report could not be saved because memory was exhausted during export.";
                    }
                    if(!failed){
                        result=(extended?"Compressed Extended bug report saved.":"Diagnostic report saved.")+" You can attach this file to your bug report.\n\n"+displayName;
                        if(extended){
                            try { SupportActionJournal.acknowledge(checkpoint); }
                            catch(IOException incomplete){
                                result+="\\nThe action journal could not be reset; the next report may repeat earlier events.";
                            }
                        }
                    }
                    if(extended) SupportActionJournal.exportFinished();
                    once.finished(result);
                }, job->{
                    Thread worker=new Thread(job,"apd-report-export");
                    worker.setDaemon(true);
                    worker.start();
                });
            }
            public void onCancelled(){once.finished("Diagnostic report cancelled. No report was saved.");}
            public void onFailed(String why){once.finished("The diagnostic report could not be opened for saving: "+why);}
        });
        } catch(RuntimeException pickerFailure){
            // A provider can throw before it opens the picker. If it already handed
            // off a stream, the worker still owns the real save result.
            if (!streamAccepted.get()) once.finished("The diagnostic report could not be opened for saving: "
                    +pickerFailure.getClass().getSimpleName());
        }
    }

    /** Standard report tail: unchanged when there are no notes, otherwise a "Bug notes" section. */
    public static String withBugNotes(String report,String notes){
        if(notes==null||notes.isEmpty())return report;
        return report+"\n\n=== Bug notes ===\n"+notes;
    }
}
