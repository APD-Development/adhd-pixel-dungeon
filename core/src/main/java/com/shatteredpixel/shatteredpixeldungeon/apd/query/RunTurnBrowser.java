package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import java.util.*;

/** Read-only chronological join of public UIMSD events in one recorded run.
 * Imported ranking-only records have no turn evidence; no events are synthesized. */
public final class RunTurnBrowser {
    public final String runId;
    public final long lastTurn;
    private long current = 1;
    private long step = 1;
    private final NavigableMap<Long,List<String>> timeline = new TreeMap<>();

    /** True only for a selected, exact public occurrence with one matching recorded run. */
    public static boolean canOpenAt(Catalog catalog, QueryResult source, Row occurrence) {
        return anchor(catalog,source,occurrence)!=null;
    }

    /**
     * Opens the canonical public timeline on the selected occurrence's exact turn.
     * Imported, duplicate, hidden, uncertain, stale and ambiguously-owned rows fail closed.
     */
    public static RunTurnBrowser openAt(Catalog catalog, QueryResult source, Row occurrence) {
        Anchor anchor=anchor(catalog,source,occurrence);
        if(anchor==null)throw new IllegalArgumentException("An exact recorded occurrence and matching run are required");
        RunTurnBrowser browser=new RunTurnBrowser(catalog,anchor.run);
        browser.go(anchor.turn);
        return browser;
    }

    private static Anchor anchor(Catalog catalog, QueryResult source, Row occurrence) {
        if(catalog==null || source==null || source.isGrouped() || !source.problems.isEmpty()
                || occurrence==null || !source.rows.contains(occurrence)
                || occurrence.imported || occurrence.duplicate || !occurrence.publicRow
                || occurrence.runId==null || occurrence.runId.isEmpty())return null;
        String id=source.def.id;
        String clock;
        if(Datasets.DEATHS.equals(id))clock="death.turn";
        else if(FactDatasets.isFactDataset(id))clock=FactDatasets.TURN;
        else if(EventDatasets.FLOORS.equals(id))clock="floor.enterTurn";
        else if(EventDatasets.EQUIPMENT.equals(id))clock="equip.start";
        else if(EventDatasets.isEventDataset(id))clock=EventDatasets.prefixOf(id)+".turn";
        else return null;
        ColumnDef turn=source.def.column(clock);
        if(turn==null || turn.index()>=occurrence.size() || !QueryEngine.open(source.query,occurrence,turn)
                || !occurrence.isKnown(turn.index()) || occurrence.prov(turn.index())!=Fact.PROV_RECORDED
                || occurrence.kind(turn.index())!=Row.K_NUM || occurrence.num(turn.index())<1)return null;
        Dataset runs=catalog.dataset(Datasets.RUNS);
        if(runs==null)return null;
        Row match=null;
        for(Row candidate:runs.rows)if(occurrence.runId.equals(candidate.runId)
                && !candidate.imported && !candidate.duplicate && candidate.publicRow){
            if(match!=null)return null;
            match=candidate;
        }
        return match==null ? null:new Anchor(match,occurrence.num(turn.index()));
    }

    private static final class Anchor {
        final Row run;
        final long turn;
        Anchor(Row run,long turn){this.run=run;this.turn=turn;}
    }

    public RunTurnBrowser(Catalog catalog, Row run) {
        if (catalog == null || run == null || run.runId == null || run.runId.isEmpty() )
            throw new IllegalArgumentException("A recorded APD run is required");
        runId = run.runId;
        long max = 1;
        Dataset runs = catalog.dataset(Datasets.RUNS);
        if (runs != null) {
            int ti = runs.def.indexOf(FactKeys.TURNS);
            if (ti >= 0 && run.isKnown(ti)) max = Math.max(1,run.num(ti));
        }
        for (Dataset ds : catalog.datasets()) {
            String id = ds.def.id;
            if (!EventDatasets.isEventDataset(id) && !FactDatasets.isFactDataset(id)) continue;
            if (EventDatasets.EQUIPMENT.equals(id)) {
                max = Math.max(max, equipmentTransitions(ds));
                continue;
            }
            if (EventDatasets.FLOORS.equals(id)) {
                max = Math.max(max, floorTransitions(ds));
                continue;
            }
            String clockName = FactDatasets.isFactDataset(id) ? FactDatasets.TURN : EventDatasets.prefixOf(id)+".turn";
            int ti = ds.def.indexOf(clockName);
            if (ti < 0) continue;
            for (Row r : ds.rows) {
                if (!runId.equals(r.runId) || !r.publicRow || !r.isKnown(ti)) continue;
                long t = r.num(ti);
                if (t < 1) continue;
                max = Math.max(max,t);
                StringBuilder line = new StringBuilder();
                if (FactDatasets.isFactDataset(id)) {
                    com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKind kind =
                        com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKinds.get(id.substring(FactDatasets.PREFIX.length()));
                    line.append(kind == null ? "Event" : kind.label);
                } else line.append(ds.def.title);
                int count=0;
                for (ColumnDef c : ds.def.columns()) {
                    if (c.isInternal() || !c.defaultVisible || c.index()==ti || !r.isKnown(c.index())) continue;
                    if (c.id.endsWith(".runClass") || c.id.endsWith(".runDifficulty")) continue;
                    line.append(count++==0 ? ": " : ", ").append(c.label).append(" ")
                        .append(CellFormatter.cellOpen(r,c).text);
                    if(count>=5)break;
                }
                List<String> at = timeline.get(t);
                if (at == null) timeline.put(t,at=new ArrayList<>());
                at.add(line.toString());
            }
        }
        lastTurn = max;
    }

    private long equipmentTransitions(Dataset ds) {
        long max = 1;
        int start = ds.def.indexOf("equip.start");
        int end = ds.def.indexOf("equip.end");
        for (Row row : ds.rows) {
            if (!runId.equals(row.runId) || !row.publicRow) continue;
            if (known(row,start) && row.num(start) >= 1) {
                long turn = row.num(start);
                max = Math.max(max,turn);
                add(turn,transition(ds,row,"Equipment put on",
                        "equip.item","equip.slot","equip.depth","equip.level"));
            }
            if (known(row,end) && row.num(end) >= 1) {
                long turn = row.num(end);
                max = Math.max(max,turn);
                add(turn,transition(ds,row,"Equipment taken off",
                        "equip.item","equip.slot"));
            }
        }
        return max;
    }

    private long floorTransitions(Dataset ds) {
        long max = 1;
        int enter = ds.def.indexOf("floor.enterTurn");
        int exit = ds.def.indexOf("floor.exitTurn");
        for (Row row : ds.rows) {
            if (!runId.equals(row.runId) || !row.publicRow) continue;
            if (known(row,enter) && row.num(enter) >= 1) {
                long turn = row.num(enter);
                max = Math.max(max,turn);
                add(turn,transition(ds,row,"Floor entered",
                        "floor.depth","floor.entry","floor.levelClass","floor.feeling"));
            }
            if (known(row,exit) && row.num(exit) >= 1) {
                long turn = row.num(exit);
                max = Math.max(max,turn);
                add(turn,transition(ds,row,"Floor left",
                        "floor.depth","floor.exit"));
            }
        }
        return max;
    }

    private static boolean known(Row row, int column) {
        return column >= 0 && column < row.size() && row.isKnown(column);
    }

    private static String transition(Dataset ds, Row row, String title, String... columns) {
        StringBuilder line = new StringBuilder(title);
        int count = 0;
        for (String id : columns) {
            ColumnDef column = column(ds.def,id);
            if (column == null || !row.isKnown(column.index())) continue;
            line.append(count++ == 0 ? ": " : ", ").append(column.label).append(" ")
                    .append(CellFormatter.cellOpen(row,column).text);
        }
        return line.toString();
    }

    private static ColumnDef column(DatasetDef def, String id) {
        for (ColumnDef column : def.columns()) if (id.equals(column.id)) return column;
        return null;
    }

    private void add(long turn, String event) {
        List<String> at = timeline.get(turn);
        if (at == null) timeline.put(turn,at=new ArrayList<>());
        at.add(event);
    }

    public long turn(){return current;}
    public long step(){return step;}
    public void step(long amount){if(amount<1)throw new IllegalArgumentException("step");step=amount;}
    public void go(long t){current=Math.max(1,Math.min(lastTurn,t));}
    public void next(){current = current>=lastTurn-step ? lastTurn : current+step;}
    public void previous(){current = current<=step ? 1 : current-step;}
    public List<String> events(){List<String> v=timeline.get(current);return v==null?Collections.emptyList():Collections.unmodifiableList(v);}
    public int recordedTurns(){return timeline.size();}
    public String summary(){
        StringBuilder out=new StringBuilder("Turn ").append(current).append(" / ").append(lastTurn)
          .append("\nStep: ").append(step).append(" turn(s)\n");
        List<String> items=events();
        if(items.isEmpty()) return out.append("\nNo recorded event for this turn.\nOlder runs may have missing details.").toString();
        for(String item:items) out.append("\n- ").append(item);
        return out.toString();
    }
}
