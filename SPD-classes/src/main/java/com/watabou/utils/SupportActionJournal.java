package com.watabou.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * On-device, voluntary bug-report action chronology. Does not observe other apps,
 * capture typed text, access saves, or transmit anything.
 *
 * Events between successful report exports are persisted separately from the
 * bounded crash summary. A capped queue and file prevent diagnostics becoming a
 * second out-of-memory failure. Any lost coverage is explicitly recorded.
 */
public final class SupportActionJournal {
    private static final Object LOCK = new Object();
    private static final int QUEUE_LIMIT = 4096;
    // APD: 8 MiB rolling window. When full, the oldest half is dropped so the newest
    // events (the ones a bug report needs) are always kept and the file stays small.
    static final long FILE_LIMIT = 8L * 1024 * 1024;
    // While a report is being exported its byte range must not move; rotation waits.
    private static long exportGuardUntil = 0;
    private static final ArrayDeque<Entry> pending = new ArrayDeque<>();
    private static final ExecutorService writer = Executors.newSingleThreadExecutor(job -> {
        Thread thread = new Thread(job, "apd-action-journal");
        thread.setDaemon(true);
        return thread;
    });
    private static File root, logFile, pauseFile;
    private static boolean scheduled, full;
    // Explicit opt-in. The default is OFF even when the diagnostics directory exists.
    private static volatile boolean enabled;
    private static long lost;

    private static final class Entry {
        final long time;
        final String kind, detail;
        Entry(long time, String kind, String detail) {
            this.time = time; this.kind = kind; this.detail = detail;
        }
    }

    public static final class Checkpoint {
        final File file;
        final long length;
        Checkpoint(File file, long length) { this.file = file; this.length = length; }
    }

    private SupportActionJournal() {}

    public static boolean enabled() { return enabled; }

    /**
     * The player must acknowledge the in-game disclosure before calling true.
     * Turning logging off also erases the retained chronology and pause marker.
     */
    public static void setEnabled(boolean consented) {
        synchronized (LOCK) {
            // A disabled preference must erase even if this is a fresh process.
            // It may have crashed after saving opt-out but before deleting the file.
            if (enabled == consented && consented) return;
            enabled = consented;
            if (!consented) {
                pending.clear();
                lost = 0;
                full = false;
                try { if (logFile != null) safe(logFile).delete(); } catch (IOException ignored) {}
                try { if (pauseFile != null) safe(pauseFile).delete(); } catch (IOException ignored) {}
            } else {
                record("journal.enabled", "explicit_player_opt_in=true");
            }
        }
    }

    private static File safe(File file) throws IOException {
        if (!file.getAbsoluteFile().equals(file.getCanonicalFile()))
            throw new IOException("Action journal file alias refused");
        return file;
    }

    public static void configure(File folder) {
        synchronized (LOCK) {
            try {
                File canonical = folder.getCanonicalFile();
                if (canonical.equals(root)) return;
                if (!canonical.isDirectory() && !canonical.mkdirs())
                    throw new IOException("Cannot create action journal directory");
                root = canonical;
                logFile = safe(new File(root, "action-history.txt"));
                pauseFile = safe(new File(root, "activity-paused.txt"));
                pending.clear(); lost = 0; full = false;
            } catch (IOException ignored) {
                root = null; logFile = null; pauseFile = null;
                pending.clear(); lost = 0;
            }
        }
    }

    private static String clean(String value, int limit) {
        if (value == null) return "";
        value = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        return value.length() > limit ? value.substring(0, limit) + " [truncated]" : value;
    }

    //APD: one formatter, always used under LOCK, instead of a new one per journal line
    private static final SimpleDateFormat STAMP = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
    static { STAMP.setTimeZone(TimeZone.getTimeZone("UTC")); }

    private static String line(Entry e) {
        synchronized (LOCK) {
            return STAMP.format(new Date(e.time)) + " " + e.kind + " " + e.detail + "\n";
        }
    }

    private static void flushLocked() throws IOException {
        if (!enabled || logFile == null || (pending.isEmpty() && lost == 0)) return;
        safe(logFile);
        try (FileOutputStream out = new FileOutputStream(logFile, true)) {
            if (lost > 0) {
                byte[] warning = line(new Entry(System.currentTimeMillis(), "coverage.gap",
                        "events_lost=" + lost + " reason=writer_backlog")).getBytes(StandardCharsets.UTF_8);
                if (!full && logFile.length() + warning.length <= FILE_LIMIT) out.write(warning);
                lost = 0;
            }
            while (!pending.isEmpty()) {
                Entry e = pending.removeFirst();
                byte[] bytes = line(e).getBytes(StandardCharsets.UTF_8);
                if (full || logFile.length() + bytes.length > FILE_LIMIT) {
                    if (System.currentTimeMillis() >= exportGuardUntil) {
                        out.close();
                        trimOldestLocked();
                        pending.addFirst(e);
                        flushLocked();
                        return;
                    }
                    if (!full) {
                        byte[] note = line(new Entry(System.currentTimeMillis(), "coverage.limit",
                                "journal full while a report was exporting; events during the export are unavailable"))
                                .getBytes(StandardCharsets.UTF_8);
                        if (logFile.length() + note.length <= FILE_LIMIT) out.write(note);
                    }
                    full = true;
                    pending.clear();
                    break;
                }
                out.write(bytes);
            }
        }
    }

    /** Keeps the newest half of the journal (from a line boundary) and notes the trim. */
    private static void trimOldestLocked() throws IOException {
        long size = logFile.length();
        long keepFrom = Math.max(0, size - FILE_LIMIT / 2);
        File temp = safe(new File(root, "action-history.txt.tmp"));
        try (RandomAccessFile in = new RandomAccessFile(logFile, "r");
             FileOutputStream out = new FileOutputStream(temp)) {
            in.seek(keepFrom);
            if (keepFrom > 0) { int c; while ((c = in.read()) >= 0 && c != '\n') { } }
            out.write(line(new Entry(System.currentTimeMillis(), "coverage.trimmed",
                    "older events dropped to keep the journal under 8 MiB")).getBytes(StandardCharsets.UTF_8));
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            out.getFD().sync();
        }
        if (!logFile.delete() || !temp.renameTo(logFile)) throw new IOException("Could not trim action journal");
        full = false;
    }

    private static void drain() {
        synchronized (LOCK) {
            try { flushLocked(); }
            catch (IOException | RuntimeException ignored) { lost += pending.size(); pending.clear(); }
            finally { scheduled = false; LOCK.notifyAll(); }
        }
    }

    /** Called only with sanitized APD event categories, never keyboard values. */
    public static void record(String kind, String detail) {
        synchronized (LOCK) {
            if (!enabled || logFile == null) return;
            if (pending.size() >= QUEUE_LIMIT) { lost++; return; }
            pending.addLast(new Entry(System.currentTimeMillis(), clean(kind, 80), clean(detail, 300)));
            if (!scheduled) {
                scheduled = true;
                try { writer.execute(SupportActionJournal::drain); }
                catch (RuntimeException rejected) { scheduled = false; }
            }
        }
    }

    /** Android lifecycle observation. A pause is not proof of a deliberate exit. */
    public static void paused(String scene) {
        synchronized (LOCK) {
            if (!enabled || pauseFile == null) return;
            try {
                safe(pauseFile);
                if (!pauseFile.isFile()) {
                    try (FileOutputStream out = new FileOutputStream(pauseFile)) {
                        out.write(Long.toString(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
                        out.getFD().sync();
                    }
                }
            } catch (IOException ignored) { record("coverage.gap", "could_not_persist_pause"); }
        }
        record("activity.background", "scene=" + clean(scene, 80));
    }

    /** Reports wall-clock time away, without claiming to know why the player left. */
    public static void resumed(String scene) {
        long now = System.currentTimeMillis(), earlier = -1;
        synchronized (LOCK) {
            if (enabled && pauseFile != null) {
                try {
                    safe(pauseFile);
                    if (pauseFile.isFile()) {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                                new FileInputStream(pauseFile), StandardCharsets.UTF_8))) {
                            earlier = Long.parseLong(in.readLine());
                        }
                        if (!pauseFile.delete()) record("coverage.gap", "could_not_clear_pause_state");
                    }
                } catch (IOException | NumberFormatException ignored) {
                    record("coverage.gap", "could_not_read_previous_pause");
                }
            }
        }
        if (!enabled) return;
        if (earlier > 0 && now >= earlier)
            record("activity.foreground", "scene=" + clean(scene, 80)
                    + " away_ms=" + (now - earlier) + " duration_is_wall_clock=true");
        else
            record("activity.foreground", "scene=" + clean(scene, 80)
                    + (earlier > now ? " away_duration_unavailable=clock_changed" : " away_duration_unavailable=no_prior_pause"));
    }

    /** Flushes the pre-export stream, then identifies exactly which bytes belong to this report. */
    public static Checkpoint checkpoint() throws IOException {
        synchronized (LOCK) {
            if (!enabled || logFile == null) return new Checkpoint(null, 0);
            flushLocked();
            exportGuardUntil = System.currentTimeMillis() + 5 * 60 * 1000L;
            return new Checkpoint(logFile, logFile.length());
        }
    }

    public static void export(OutputStream out, Checkpoint checkpoint) throws IOException {
        out.write(("\nAPD ACTION HISTORY SINCE LAST SUCCESSFUL REPORT (UTC)\n"
                + "Android background/foreground observations do not establish why a player left.\n"
                + "Touches and commands are recorded only inside APD; typed content is not recorded.\n"
                + "No automatic upload occurs. Coverage gaps or size limits are noted when known.\n")
                .getBytes(StandardCharsets.UTF_8));
        if (checkpoint.file == null) {
            out.write("Action history unavailable.\n".getBytes(StandardCharsets.UTF_8));
            return;
        }
        safe(checkpoint.file);
        try (InputStream in = new FileInputStream(checkpoint.file)) {
            long remaining = checkpoint.length; int n;
            byte[] buffer = new byte[8192];
            while (remaining > 0 && (n = in.read(buffer, 0, (int)Math.min(buffer.length, remaining))) >= 0) {
                if (n == 0) continue;
                out.write(buffer, 0, n);
                remaining -= n;
            }
            if (remaining != 0) throw new IOException("Action history changed during export");
        }
    }

    /** A report export ended (saved or not); trimming may move bytes again. */
    public static void exportFinished() {
        synchronized (LOCK) { exportGuardUntil = 0; }
    }

    /** Discard only the bytes included in a successfully written report. */
    public static void acknowledge(Checkpoint checkpoint) throws IOException {
        synchronized (LOCK) {
            if (!enabled || logFile == null || checkpoint.file == null || !checkpoint.file.equals(logFile)) return;
            flushLocked();
            long size = logFile.length();
            if (size < checkpoint.length) throw new IOException("Action history changed before acknowledgement");
            File temp = safe(new File(root, "action-history.txt.tmp"));
            try (RandomAccessFile in = new RandomAccessFile(logFile, "r");
                 FileOutputStream out = new FileOutputStream(temp)) {
                in.seek(checkpoint.length);
                byte[] buffer = new byte[8192];
                long left = size - checkpoint.length;
                while (left > 0) {
                    int n = in.read(buffer, 0, (int)Math.min(buffer.length, left));
                    if (n < 0) throw new EOFException();
                    out.write(buffer, 0, n);
                    left -= n;
                }
                out.getFD().sync();
            }
            if (!temp.renameTo(logFile)) throw new IOException("Could not acknowledge action journal");
            full = false;
            exportGuardUntil = 0;
        }
    }
}
