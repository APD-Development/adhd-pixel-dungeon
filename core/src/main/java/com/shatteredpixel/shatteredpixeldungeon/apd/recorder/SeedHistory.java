package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ApdEvent;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.EventTypes;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LegacyFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LifecycleEvents;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.LogHeader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.ReadResult;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFacts;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunFactsBuilder;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunIndex;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunLogReader;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.RunTimeline;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.StatsStore;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Read-only seed lookup. Raw history and retained rankings are evidence, never proof of authorship. */
public final class SeedHistory {

    private SeedHistory() {}

    public enum Kind { GENERATED_LOCALLY, MATCHING_LOCAL_HISTORY, UNKNOWN_IMPORTED }

    public static final class Summary {
        public final Kind kind;
        public final long seed;
        /** Distinct recorded attempts, including those without a recorded final result. */
        public final int attempts;
        public final int unfinishedAttempts;
        /** Deepest final or canonical observed floor; -1 when no matching attempt recorded one. */
        public final int deepestFloor;
        public final int attemptsWithKnownDepth;
        public final boolean incompleteHistory;
        /** Exact deduplicated per-attempt deepest floors, only when ALL prior depths are known. */
        public final int[] priorDepths;

        private Summary(Kind kind, long seed, int attempts, int unfinishedAttempts, int deepestFloor,
                        int attemptsWithKnownDepth, boolean incompleteHistory, int[] priorDepths) {
            this.kind = kind;
            this.seed = seed;
            this.attempts = attempts;
            this.unfinishedAttempts = unfinishedAttempts;
            this.deepestFloor = deepestFloor;
            this.attemptsWithKnownDepth = attemptsWithKnownDepth;
            this.incompleteHistory = incompleteHistory;
            this.priorDepths = priorDepths == null ? null : priorDepths.clone();
        }
    }

    /**
     * An empty selection keeps the existing locally generated seed path. A supplied selection is
     * matched by the game's numeric seed, so its code and numeric spelling refer to the same seed.
     * Call on a worker: no cache, index, ranking, preference or save file is written.
     */
    public static Summary lookup(File root, String seedText, LegacyImporter.Extract rankings) {
        return lookup(root, seedText, rankings, null);
    }

    /** The active run may already exist as an unfinished log when its final score is saved.
     * Exclude that exact run ID so only earlier attempts affect its reuse score. */
    public static Summary lookup(File root, String seedText, LegacyImporter.Extract rankings,
                                 String excludeRunId) {
        if (seedText == null || seedText.isEmpty()) {
            return new Summary(Kind.GENERATED_LOCALLY, -1, 0, 0, -1, 0, false, new int[0]);
        }
        long seed = DungeonSeed.convertFromText(seedText);
        Scan scan = new Scan(seed);
        if (root != null && !new File(root, "SYNTHETIC-DEMO-DATA.txt").exists()) {
            scan.read(new File(root, "runs"), false);
            scan.read(new File(root, "legacy"), true);
        }
        if (rankings != null) {
            scan.incomplete |= rankings.unreadable > 0;
            for (LegacyEvents.Record record : rankings.fresh) scan.legacy(record);
        }
        if (excludeRunId != null && realId(excludeRunId)) scan.runs.remove(excludeRunId);
        return scan.finish();
    }

    private static boolean number(RunFacts facts, String key) {
        Fact f = facts.get(key);
        return f != null && f.isNum();
    }

    private static boolean finished(RunFacts facts) {
        long outcome = facts.numOr(FactKeys.OUTCOME, -1);
        return outcome == LifecycleEvents.OUTCOME_DIED || outcome == LifecycleEvents.OUTCOME_WON
                || outcome == LifecycleEvents.OUTCOME_ABANDONED;
    }

    private static int deepest(RunFacts facts) {
        return depth(facts, FactKeys.DEEPEST);
    }

    private static int depth(RunFacts facts, String key) {
        long depth = facts.numOr(key, -1);
        return depth >= 1 && depth <= Integer.MAX_VALUE ? (int) depth : -1;
    }

    private static boolean realId(String id) {
        return StatsStore.isValidRunId(id) && !id.startsWith("demo-");
    }

    private static final class Attempt {
        final long seed;
        long segment;
        long outcome;
        boolean terminal;
        int deepest;
        boolean conflict;
        final Set<String> rankingIds = new HashSet<>();

        Attempt(RunFacts facts) {
            seed = facts.numOr(FactKeys.SEED, -1);
            segment = facts.numOr(FactKeys.SEGMENT, 0);
            outcome = facts.numOr(FactKeys.OUTCOME, -1);
            terminal = finished(facts);
            deepest = depth(facts, terminal ? FactKeys.DEEPEST : FactKeys.DEEPEST_ENTERED);
            addRanking(facts);
        }

        void addRanking(RunFacts facts) {
            String id = facts.textOr(FactKeys.RANKING_ID, "");
            if (!id.isEmpty()) rankingIds.add(id);
        }

        void merge(RunFacts facts) {
            if (seed != facts.numOr(FactKeys.SEED, -1)) conflict = true;
            long nextSegment = facts.numOr(FactKeys.SEGMENT, 0);
            boolean nextTerminal = finished(facts);
            int nextDeepest = depth(facts, nextTerminal ? FactKeys.DEEPEST : FactKeys.DEEPEST_ENTERED);
            if (nextSegment > segment) {
                segment = nextSegment;
                outcome = facts.numOr(FactKeys.OUTCOME, -1);
                terminal = nextTerminal;
                deepest = nextDeepest;
            } else if (nextSegment == segment) {
                // Identical copied logs count once. Conflicting copies cannot establish an exact result.
                if (outcome != facts.numOr(FactKeys.OUTCOME, -1) || deepest != nextDeepest) conflict = true;
            }
            addRanking(facts);
        }
    }

    private static final class Scan {
        final long seed;
        final Map<String, Attempt> runs = new HashMap<>();
        final Map<String, RunFacts> oldRecords = new HashMap<>();
        final Set<String> conflictingRankings = new HashSet<>();
        boolean incomplete;

        Scan(long seed) { this.seed = seed; }

        void read(File dir, boolean legacy) {
            File[] files = dir.listFiles();
            if (files == null) {
                if (dir.exists()) incomplete = true;
                return;
            }
            Arrays.sort(files);
            for (File file : files) {
                if (!file.isFile() || !file.getName().endsWith(RunIndex.LOG_SUFFIX)) continue;
                String id = file.getName().substring(0, file.getName().length() - RunIndex.LOG_SUFFIX.length());
                if (!realId(id)) continue;
                try {
                    long length = file.length(), modified = file.lastModified();
                    ReadResult read = RunLogReader.read(file);
                    if (!file.isFile() || file.length() != length || file.lastModified() != modified) {
                        incomplete = true;
                        continue;
                    }
                    if (read.header == null || read.newerThanReader()) {
                        incomplete = true;
                        continue;
                    }
                    String build = read.header.writerBuild;
                    if (!realId(read.header.runId) || build.startsWith(LogHeader.REDACTED_PREFIX)
                            || build.contains("SYNTHETIC-DEMO")) continue;
                    incomplete |= !read.status.isClean() || read.gapEvents > 0;
                    if (legacy) {
                        for (ApdEvent event : RunTimeline.of(read.events).canonical()) {
                            if (event.type == EventTypes.LEGACY_RECORD) {
                                try {
                                    legacy(LegacyEvents.decodeRecord(event, read));
                                } catch (RuntimeException malformed) {
                                    incomplete = true;
                                }
                            }
                        }
                    } else {
                        RunFacts facts = RunFactsBuilder.fromLog(read, id);
                        String runId = facts.textOr(FactKeys.RUN_ID, "");
                        long type = facts.numOr(FactKeys.SEED_TYPE, -1);
                        if (!realId(runId) || !number(facts, FactKeys.SEED)
                                || (type != LifecycleEvents.SEED_RANDOM && type != LifecycleEvents.SEED_CUSTOM)) continue;
                        Attempt previous = runs.get(runId);
                        if (previous == null) runs.put(runId, new Attempt(facts));
                        else previous.merge(facts);
                    }
                } catch (IOException | RuntimeException unreadable) {
                    incomplete = true;
                }
            }
        }

        void legacy(LegacyEvents.Record record) {
            if (record == null || record.gameId == null || record.gameId.isEmpty()
                    || record.gameId.startsWith("demo-")) return;
            RunFacts facts = LegacyFacts.fromRecord(record, "retained-record");
            if (!number(facts, FactKeys.SEED) || facts.numOr(FactKeys.DAILY, 0) != 0 || !finished(facts)) return;
            RunFacts prior = oldRecords.get(record.gameId);
            if (prior == null) {
                oldRecords.put(record.gameId, facts);
            } else if (prior.numOr(FactKeys.SEED, -1) != facts.numOr(FactKeys.SEED, -1)
                    || prior.numOr(FactKeys.OUTCOME, -1) != facts.numOr(FactKeys.OUTCOME, -1)
                    || (deepest(prior) >= 0 && deepest(facts) >= 0 && deepest(prior) != deepest(facts))) {
                conflictingRankings.add(record.gameId);
            } else if (deepest(prior) < 0 && deepest(facts) >= 0) {
                oldRecords.put(record.gameId, facts);
            }
        }

        Summary finish() {
            int attempts = 0, unfinished = 0, known = 0, deepest = -1;
            ArrayList<Integer> depths = new ArrayList<>();
            // Only a null/empty selection means generated locally. A supplied numeric -1
            // is a valid recorded seed; never reuse the empty-selection sentinel here.
            // Validate exact identities before filtering by the requested seed. A conflicting copy
            // must not become trustworthy simply because the other seed was filtered out first.
            Map<String, Long> rankingSeeds = new HashMap<>();
            for (Attempt run : runs.values()) {
                for (String id : run.rankingIds) {
                    if (run.conflict) conflictingRankings.add(id);
                    rememberSeed(rankingSeeds, id, run.seed);
                }
            }
            for (Map.Entry<String, RunFacts> entry : oldRecords.entrySet()) {
                rememberSeed(rankingSeeds, entry.getKey(), entry.getValue().numOr(FactKeys.SEED, -1));
            }
            Set<String> exactRankingIds = new HashSet<>();
            Map<String, Attempt> counted = new TreeMap<>();
            Map<String, String> parents = new HashMap<>();
            Map<String, Attempt> open = new TreeMap<>();
            for (Map.Entry<String, Attempt> entry : runs.entrySet()) {
                Attempt run = entry.getValue();
                if (run.conflict || conflictingRanking(run) || run.seed != seed) continue;
                String owner = "run:" + entry.getKey();
                for (String id : run.rankingIds) {
                    parents.put(find(parents, "ranking:" + id), find(parents, owner));
                    exactRankingIds.add(id);
                }
            }
            for (Map.Entry<String, Attempt> entry : new TreeMap<>(runs).entrySet()) {
                Attempt run = entry.getValue();
                if (run.conflict || conflictingRanking(run)) { incomplete = true; continue; }
                if (run.seed != seed) continue;
                String owner = find(parents, "run:" + entry.getKey());
                if (!run.terminal) {
                    Attempt prior = open.get(owner);
                    if (prior == null) {
                        open.put(owner, run);
                    } else {
                        if (prior.outcome != run.outcome || (prior.deepest >= 0 && run.deepest >= 0 && prior.deepest != run.deepest)) {
                            prior.conflict = true;
                            incomplete = true;
                        } else if (prior.deepest < 0) prior.deepest = run.deepest;
                        prior.rankingIds.addAll(run.rankingIds);
                    }
                    continue;
                }
                // A ranking link is exact identity evidence. A probable statistics link is never used.
                Attempt prior = counted.get(owner);
                if (prior == null) {
                    counted.put(owner, run);
                } else {
                    if (prior.outcome != run.outcome || (prior.deepest >= 0 && run.deepest >= 0 && prior.deepest != run.deepest)) {
                        prior.conflict = true;
                        incomplete = true;
                    } else if (prior.deepest < 0) prior.deepest = run.deepest;
                    prior.rankingIds.addAll(run.rankingIds);
                }
            }
            for (Map.Entry<String, Attempt> entry : counted.entrySet()) {
                Attempt run = entry.getValue();
                if (run.conflict || open.containsKey(entry.getKey())) continue;
                if (run.deepest < 0) {
                    for (String id : run.rankingIds) {
                        RunFacts old = oldRecords.get(id);
                        if (old != null && !conflictingRankings.contains(id)
                                && old.numOr(FactKeys.SEED, -1) == seed) {
                            run.deepest = Math.max(run.deepest, deepest(old));
                        }
                    }
                }
                attempts++;
                depths.add(run.deepest);
                if (run.deepest >= 0) { known++; deepest = Math.max(deepest, run.deepest); }
            }
            for (Map.Entry<String, Attempt> entry : open.entrySet()) {
                Attempt run = entry.getValue();
                Attempt priorFinish = counted.get(entry.getKey());
                if (run.conflict || (priorFinish != null && priorFinish.conflict)) {
                    incomplete = true;
                    continue;
                }
                // An unfinished identity is still recorded use. Only its canonical observed depth
                // is available; an older terminal copy or ranking must not supply a final result.
                attempts++;
                unfinished++;
                depths.add(run.deepest);
                if (run.deepest >= 0) { known++; deepest = Math.max(deepest, run.deepest); }
            }
            for (Map.Entry<String, RunFacts> entry : oldRecords.entrySet()) {
                if (conflictingRankings.contains(entry.getKey())) { incomplete = true; continue; }
                RunFacts facts = entry.getValue();
                if (facts.numOr(FactKeys.SEED, -1) != seed || exactRankingIds.contains(entry.getKey())) continue;
                attempts++;
                int depth = deepest(facts);
                depths.add(depth);
                if (depth >= 0) { known++; deepest = Math.max(deepest, depth); }
            }
            int[] completeDepths = null;
            if (attempts > 0 && !incomplete && known == attempts && depths.size() == attempts) {
                completeDepths = new int[depths.size()];
                for (int i = 0; i < depths.size(); i++) completeDepths[i] = depths.get(i);
            }
            return new Summary(attempts > 0 ? Kind.MATCHING_LOCAL_HISTORY : Kind.UNKNOWN_IMPORTED,
                    seed, attempts, unfinished, deepest, known, incomplete, completeDepths);
        }

        private void rememberSeed(Map<String, Long> seeds, String id, long seed) {
            Long previous = seeds.get(id);
            if (previous != null && previous.longValue() != seed) conflictingRankings.add(id);
            else seeds.put(id, seed);
        }

        private boolean conflictingRanking(Attempt run) {
            for (String id : run.rankingIds) if (conflictingRankings.contains(id)) return true;
            return false;
        }

        private String find(Map<String, String> parents, String key) {
            String root = key;
            while (parents.containsKey(root) && !parents.get(root).equals(root)) root = parents.get(root);
            while (!key.equals(root)) {
                String next = parents.get(key);
                parents.put(key, root);
                key = next;
            }
            return root;
        }
    }
}
