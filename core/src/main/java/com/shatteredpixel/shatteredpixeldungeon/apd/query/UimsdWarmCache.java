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
package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Single-entry, fully validated UIMSD preload for the title menu.
 *
 * No disk work or catalog computation happens in prewarm(); all validation
 * occurs on a daemon worker. A warmed catalog is offered only when every
 * relevant raw log and column-layout file still has matching metadata and
 * SHA-256 content identity. The fingerprint is calculated only on preload and
 * open workers, never on the render thread. DatasetStore and SavedViews still
 * perform their full normal reads and cryptographic raw-log checks during preparation.
 */
public final class UimsdWarmCache {
    private static final Object LOCK = new Object();
    private static Entry ready;
    private static Job preparing;
    private static long generation;
    private static volatile String preparationFailure="";
    public static String preparationFailure(){return preparationFailure;}

    private UimsdWarmCache(){}

    private static final class Key {
        final File root;
        final String ruleset, label;
        final boolean allRules;
        Key(File root, String ruleset, String label, boolean allRules) {
            this.root = root.getAbsoluteFile();
            this.ruleset = ruleset;
            this.label = label;
            this.allRules = allRules;
        }
        boolean matches(Key key) {
            return root.equals(key.root) && ruleset.equals(key.ruleset)
                    && label.equals(key.label) && allRules == key.allRules;
        }
    }
    private static final class Entry {
        final Key key;
        final UimsdModel model;
        final String sourceStamp;
        Entry(Key key, UimsdModel model, String sourceStamp) {
            this.key = key;
            this.model = model;
            this.sourceStamp = sourceStamp;
        }
    }
    private static final class Job {
        final Key key;
        final long id;
        Thread thread;
        volatile UimsdModel model;
        Job(Key key, long id) {
            this.key = key;
            this.id = id;
        }
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    // Uses File canonicalization rather than newer filesystem APIs: core supports Android 5.
    // A symlinked storage root is valid; only aliases below that root are refused.
    private static boolean directChild(File parent, File child) throws IOException {
        return child.getCanonicalFile().equals(
                new File(parent.getCanonicalFile(), child.getName()).getAbsoluteFile());
    }

    private static String stamp(File root) {
        try {
            StringBuilder out = new StringBuilder(root.getAbsolutePath());
            byte[] buffer = new byte[64 * 1024];
            stampOne(out, new File(root, UimsdModel.COLUMN_LAYOUTS_FILE), buffer);
            stampOne(out, new File(root, DatasetStore.DEMO_MARKER_FILE), buffer);
            for (String folder : new String[]{"runs", "legacy"}) {
                File directory = new File(root, folder);
                // An alias below the chosen root is not a source of this history.
                if (!directChild(root, directory)) return null;
                out.append('|').append(folder).append('=').append(directory.isDirectory());
                File[] files = directory.listFiles();
                if (files == null) continue;
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File file : files) {
                    if (file.getName().endsWith(".apdlog")) {
                        if (!directChild(directory, file)) return null;
                        if (file.isFile()) stampOne(out, file, buffer);
                    }
                }
            }
            return out.toString();
        } catch (IOException | NoSuchAlgorithmException | RuntimeException error) {
            return null; // unreadable or uncertain source state must never be accepted as fresh
        }
    }
    private static void stampOne(StringBuilder out, File file, byte[] buffer)
            throws IOException, NoSuchAlgorithmException {
        // Layout/marker aliases also must not make optional title work read outside data.
        if (!directChild(file.getParentFile(), file)) throw new IOException("Aliased preload source");
        out.append('|').append(file.getName()).append(':').append(file.exists())
                .append(':').append(file.isFile()).append(':').append(file.length())
                .append(':').append(file.lastModified());
        if (!file.isFile()) return;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        out.append(':');
        for (byte value : digest.digest()) {
            int unsigned = value & 0xff;
            out.append(HEX[unsigned >>> 4]).append(HEX[unsigned & 0x0f]);
        }
    }

    // Optional preload must leave room for gameplay and foreground screens.
    static boolean withinMemoryBudget(long limit,long used,long sourceBytes){
        long budget=Math.min(64L*1024*1024,limit/4);
        return limit>0&&sourceBytes>=0&&sourceBytes<=budget/32&&used<=limit-limit/4-budget;
    }
    private static boolean canPrepare(File root){
        long bytes=0;
        for(String folder:new String[]{"runs","legacy"}){
            File directory=new File(root,folder);
            try{if(!directChild(root,directory))return false;}catch(IOException failure){return false;}
            File[] files=directory.listFiles();
            if(files!=null)for(File file:files)if(file.getName().endsWith(".apdlog")){
                try{if(!directChild(directory,file))return false;}catch(IOException failure){return false;}
                if(file.isFile()){
                    long length=file.length();if(length>Long.MAX_VALUE-bytes)return false;bytes+=length;
                }
            }
        }
        Runtime runtime=Runtime.getRuntime();
        return withinMemoryBudget(runtime.maxMemory(),runtime.totalMemory()-runtime.freeMemory(),bytes);
    }

    /**
     * Begin preparing the real Home before the player taps UIMSD.
     * Repeated title appearances do not create concurrent duplicate loaders.
     */
    public static void prewarm(File root, String ruleset, String label, boolean allRules) {
        if (root == null) return;
        final Key key = new Key(root, ruleset, label, allRules);
        final Job job;
        final Job superseded;
        final Thread worker;
        synchronized (LOCK) {
            if (preparing != null && preparing.key.matches(key)) return;
            job = new Job(key, ++generation);
            worker = new Thread(() -> {
                try {
                    String before = stamp(key.root);
                    if (before == null) return;
                    synchronized (LOCK) {
                        // A cancelled worker can finish its filesystem stamp after the
                        // replacement has already published. It must not release that model.
                        if(preparing!=job||job.id!=generation)return;
                        if (ready != null && ready.key.matches(key)
                                && before.equals(ready.sourceStamp)) return;
                        if(ready!=null){ready.model.releaseForGameplay();ready=null;}
                    }
                    if(!canPrepare(key.root))return; // explicit UIMSD opening still reads complete history normally
                    UimsdModel prepared = new UimsdModel(key.ruleset, key.label);
                    synchronized(LOCK){
                        if(preparing!=job||job.id!=generation)return;
                        job.model=prepared;
                        prepared.load(key.root, key.allRules);
                    }
                    prepared.awaitLoad(60000);
                    String after = stamp(key.root);
                    if (prepared.state() != UimsdModel.State.READY ||
                            after == null || !before.equals(after)) return;
                    synchronized (LOCK) {
                        if (preparing == job && job.id == generation){
                            ready = new Entry(key, prepared, after);
                            preparationFailure="";
                        }
                    }
                } catch(OutOfMemoryError exhausted){
                    preparationFailure="Background statistics preparation stopped: insufficient memory";
                } catch(RuntimeException failed){
                    preparationFailure="Background statistics preparation failed";
                } finally {
                    UimsdModel abandoned = null;
                    synchronized (LOCK) {
                        if (preparing == job) {
                            preparing = null;
                            if (job.model != null && (ready == null || ready.model != job.model))
                                abandoned = job.model;
                        }
                    }
                    if (abandoned != null) abandoned.releaseForGameplay();
                }
            }, "uimsd-title-prewarm");
            worker.setDaemon(true);
            worker.setPriority(Thread.MIN_PRIORITY);
            job.thread = worker;
            superseded = preparing;
            if (superseded != null && ready != null && ready.model == superseded.model)
                ready = null;
            preparing = job;
        }
        retire(superseded);
        // Starting only while this job still owns the slot closes the narrow
        // publish/cancel race between concurrent title appearances.
        synchronized (LOCK) {
            if (preparing == job && job.id == generation) worker.start();
        }
    }

    private static void retire(Job job) {
        if (job == null) return;
        UimsdModel model = job.model;
        if (model != null) model.releaseForGameplay();
        Thread thread = job.thread;
        if (thread != null) thread.interrupt();
    }

    /** Foreground ownership transfer: optional preparation must not build a second catalog. */
    public static UimsdModel takeForOpen(File root,String ruleset,String label,boolean allRules){
        final Entry candidate;
        final Job pending;
        synchronized(LOCK){
            ++generation;
            candidate=ready;ready=null;
            pending=preparing;preparing=null;
        }
        // Retire outside LOCK: model callbacks must not run under the cache lock.
        if(pending!=null){
            if(pending.model!=null && (candidate==null || pending.model!=candidate.model))
                pending.model.releaseForGameplay();
            if(pending.thread!=null)pending.thread.interrupt();
        }
        if(candidate==null)return null;
        Key key=root==null?null:new Key(root,ruleset,label,allRules);
        String now=key!=null&&candidate.key.matches(key)?stamp(key.root):null;
        if(now!=null && now.equals(candidate.sourceStamp) && candidate.model.state()==UimsdModel.State.READY)
            return candidate.model;
        candidate.model.releaseForGameplay();
        return null;
    }

    /** Return a fully READY model or nothing; never waits for the loader. */
    public static UimsdModel takeReady(File root, String ruleset, String label, boolean allRules) {
        if (root == null) return null;
        final Key key = new Key(root, ruleset, label, allRules);
        final Entry candidate;
        synchronized (LOCK) {
            candidate = ready;
            if (candidate == null || !candidate.key.matches(key)) return null;
        }
        String now = stamp(key.root);
        UimsdModel accepted = null;
        UimsdModel discarded = null;
        synchronized (LOCK) {
            if (candidate != ready) return null;
            ready = null; // only one viewer can own a mutable model
            if (now != null && now.equals(candidate.sourceStamp)
                    && candidate.model.state() == UimsdModel.State.READY)
                accepted = candidate.model;
            else
                discarded = candidate.model;
        }
        // A cold replacement may allocate another complete catalog immediately.
        // Drop the rejected catalog deterministically instead of retaining both
        // histories until the garbage collector happens to run.
        if (discarded != null) discarded.releaseForGameplay();
        return accepted;
    }

    /** Cancel optional title preparation and drop its strong model reference before a game loads. */
    public static void releaseForGameplay(){
        synchronized(LOCK){
            ++generation;
            if(ready!=null)ready.model.releaseForGameplay();
            ready=null;
            Job old=preparing;preparing=null;
            if(old!=null){
                if(old.model!=null)old.model.releaseForGameplay();
                if(old.thread!=null)old.thread.interrupt();
            }
        }
    }

    static void awaitPrewarmForTest(long millis) throws InterruptedException {
        Thread thread;
        synchronized (LOCK) {
            thread = preparing == null ? null : preparing.thread;
        }
        if (thread != null) thread.join(millis);
    }

    static void resetForTest() {
        synchronized (LOCK) {
            ++generation;
            preparing = null;
            ready = null;
        }
    }
}
