package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only context around one recorded hero death.
 *
 * This joins only public observations that are already present in the UIMSD
 * catalog. Empty turns and missing fields remain unknown: this class does not
 * reconstruct events, infer causation, or claim that the hero survived them.
 */
public final class DeathContext {

    public static final int MAX_WINDOW_TURNS = 1000;

    public enum Outcome {
        REVIVED,
        FINAL,
        NON_FINAL,
        UNKNOWN
    }

    public static final class Entry {
        public final long turn;
        public final String text;
        public final boolean death;

        private Entry(long turn, String text, boolean death) {
            this.turn = turn;
            this.text = text;
            this.death = death;
        }
    }

    public final String runId;
    public final long deathTurn;
    public final long startTurn;
    public final long endTurn;
    public final int windowTurns;
    public final String cause;
    public final Outcome outcome;

    private final boolean available;
    private final List<Entry> entries;
    private final int recordedTurns;

    public DeathContext(Catalog catalog, Row death, int windowTurns) {
        if (catalog == null || death == null || death.runId == null || death.runId.isEmpty()) {
            throw new IllegalArgumentException("A recorded APD death is required");
        }
        if (windowTurns < 1 || windowTurns > MAX_WINDOW_TURNS) {
            throw new IllegalArgumentException("windowTurns");
        }

        this.runId = death.runId;
        this.windowTurns = windowTurns;

        Dataset deaths = catalog.dataset(Datasets.DEATHS);
        DatasetDef deathDef = deaths == null ? Datasets.deaths() : deaths.def;
        int turnColumn = deathDef.indexOf("death.turn");
        int causeColumn = deathDef.indexOf("death.cause");
        int revivedColumn = deathDef.indexOf("death.revived");
        int finalColumn = deathDef.indexOf("death.final");

        cause = known(death, causeColumn) && death.kind(causeColumn) == Row.K_TEXT
                ? death.text(causeColumn) : "";
        outcome = outcome(death, revivedColumn, finalColumn);

        if (!known(death, turnColumn) || death.kind(turnColumn) != Row.K_NUM || death.num(turnColumn) < 1) {
            available = false;
            deathTurn = 0;
            startTurn = 0;
            endTurn = 0;
            entries = Collections.emptyList();
            recordedTurns = 0;
            return;
        }

        available = true;
        deathTurn = death.num(turnColumn);
        endTurn = deathTurn;
        startTurn = Math.max(1, deathTurn - (long) windowTurns + 1);

        List<Entry> found = new ArrayList<>();
        Set<Long> turns = new HashSet<>();
        Row run = findRecordedRun(catalog, runId);
        if (run != null) {
            RunTurnBrowser browser = new RunTurnBrowser(catalog, run);
            for (long turn = startTurn; turn <= endTurn && turn <= browser.lastTurn; turn++) {
                browser.go(turn);
                if (browser.turn() != turn) continue;
                for (String event : browser.events()) {
                    found.add(new Entry(turn, event, false));
                    turns.add(turn);
                }
            }
        }

        found.add(new Entry(deathTurn,
                cause.isEmpty() ? "Death: cause not recorded" : "Death: killed by " + cause,
                true));
        turns.add(deathTurn);
        entries = Collections.unmodifiableList(found);
        recordedTurns = turns.size();
    }

    private static boolean known(Row row, int column) {
        return column >= 0 && column < row.size() && row.isKnown(column);
    }

    private static Outcome outcome(Row death, int revivedColumn, int finalColumn) {
        boolean revivedKnown = known(death, revivedColumn) && death.kind(revivedColumn) == Row.K_NUM;
        boolean finalKnown = known(death, finalColumn) && death.kind(finalColumn) == Row.K_NUM;
        boolean revived = revivedKnown && death.num(revivedColumn) == 1;
        boolean ended = finalKnown && death.num(finalColumn) == 1;

        if (revived && ended) return Outcome.UNKNOWN;
        if (revived) return Outcome.REVIVED;
        if (ended) return Outcome.FINAL;
        if (finalKnown && death.num(finalColumn) == 0) return Outcome.NON_FINAL;
        return Outcome.UNKNOWN;
    }

    private static Row findRecordedRun(Catalog catalog, String runId) {
        Dataset runs = catalog.dataset(Datasets.RUNS);
        if (runs == null) return null;
        Row imported = null;
        for (Row row : runs.rows) {
            if (!runId.equals(row.runId)) continue;
            if (!row.imported) return row;
            if (imported == null) imported = row;
        }
        return imported;
    }

    public boolean available() {
        return available;
    }

    public List<Entry> entries() {
        return entries;
    }

    public int recordedTurns() {
        return recordedTurns;
    }

    public int observedRecords() {
        return entries.size();
    }

    public String summary() {
        if (!available) {
            return "Death context unavailable: this record does not contain a death turn.\n"
                    + "Older runs may have missing details; no timeline was reconstructed.";
        }

        StringBuilder out = new StringBuilder("Death at turn ").append(deathTurn)
                .append(" (window ").append(startTurn).append("-").append(endTurn).append(")\n")
                .append("Killed by: ").append(cause.isEmpty() ? "not recorded" : cause).append("\n")
                .append("Outcome: ").append(outcomeText()).append("\n")
                .append("Observed records only: ").append(observedRecords()).append(" record(s) on ")
                .append(recordedTurns).append(" turn(s). Empty turns may be unrecorded; ")
                .append("this does not prove survival, causation, or absence of other events.");
        for (Entry entry : entries) {
            out.append("\n- Turn ").append(entry.turn).append(": ").append(entry.text);
        }
        return out.toString();
    }

    private String outcomeText() {
        switch (outcome) {
            case REVIVED:
                return "Revived after this death.";
            case FINAL:
                return "This death ended the run.";
            case NON_FINAL:
                return "Recorded as non-final; no revival is established by this record.";
            default:
                return "What happened after this death was not recorded.";
        }
    }
}
