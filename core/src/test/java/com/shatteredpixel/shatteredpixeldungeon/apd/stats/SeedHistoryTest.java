package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.LegacyImporter;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.SeedHistory;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.GameSettings;
import com.watabou.utils.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import static org.junit.Assert.*;

/** Exercises the existing binary lifecycle format and retained ranking decoder, not a mock fact table. */
public class SeedHistoryTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static final long SEED = 123456789L;

    private File root() { return temporary.getRoot(); }

    private RunLogWriter start(String logId, String runId, int segment, long seed, int type, String build) {
        RunLogWriter writer = RunLogWriter.create(new File(new File(root(), "runs"), logId + ".apdlog"),
                TelemetryVersion.CURRENT, logId, build, 1);
        LifecycleEvents.RunStart start = new LifecycleEvents.RunStart();
        start.runId = runId; start.segment = segment; start.seed = seed; start.seedType = type;
        start.heroClass = "WARRIOR";
        start.customSeedText = type == LifecycleEvents.SEED_CUSTOM ? Long.toString(seed) : "";
        assertTrue(LifecycleEvents.emitRunStart(writer, 0, segment > 0, start));
        return writer;
    }

    private void end(RunLogWriter writer, long clock, int outcome, int depth, boolean knownDepth, String ranking) {
        LifecycleEvents.RunEnd end = new LifecycleEvents.RunEnd();
        end.outcome = outcome; end.depth = end.deepest = depth; end.heroLevel = 8; end.score = 2000;
        if (knownDepth) assertTrue(LifecycleEvents.emitRunEnd(writer, clock, end));
        else assertTrue(LifecycleEvents.emitRunEnd(writer, clock, end, LifecycleEvents.FLAG_DETAILS_UNKNOWN));
        if (ranking != null) {
            LegacyEvents.RankingLink link = new LegacyEvents.RankingLink(); link.gameId = ranking;
            assertTrue(LegacyEvents.emitRankingLink(writer, clock + 1, link));
        }
    }

    private void run(String id, long seed, int type, int outcome, int depth, String ranking) {
        RunLogWriter writer = start(id, id, 0, seed, type, "fixture");
        end(writer, 5, outcome, depth, true, ranking);
        assertTrue(writer.close(true, 10000));
    }

    private void enter(RunLogWriter writer, long clock, int depth, int branch) {
        LifecycleEvents.LevelEnter level = new LifecycleEvents.LevelEnter();
        level.depth = depth; level.branch = branch; level.entryMode = "DESCEND";
        assertTrue(LifecycleEvents.emitLevelEnter(writer, clock, level));
    }

    private LegacyEvents.Record old(String id, long seed, int depth) {
        LegacyEvents.Record record = new LegacyEvents.Record(); record.gameId = id;
        record.putNum(FactKeys.SEED, seed); record.putNum(FactKeys.OUTCOME, LifecycleEvents.OUTCOME_DIED);
        record.putNum(FactKeys.DAILY, 0);
        record.putText(FactKeys.HERO_CLASS, "WARRIOR"); record.putNum(FactKeys.HERO_LEVEL, 8); record.putNum(FactKeys.SCORE, 2000);
        if (depth >= 0) record.putNum(FactKeys.DEEPEST, depth); else record.putUnknown(FactKeys.DEEPEST);
        return record;
    }

    private void legacy(String id, LegacyEvents.Record... records) {
        RunLogWriter writer = RunLogWriter.create(new File(new File(root(), "legacy"), id + ".apdlog"),
                TelemetryVersion.CURRENT, id, "legacy-import", 1);
        int clock = 0;
        for (LegacyEvents.Record record : records) assertTrue(LegacyEvents.emitRecord(writer, clock++, record));
        assertTrue(writer.close(true, 10000));
    }

    private SeedHistory.Summary lookup(String text) { return SeedHistory.lookup(root(), text, null); }

    @Test public void currentRunIsExcludedFromScoringHistoryWithoutLosingEarlierAttempts() {
        run("prior-death", SEED, LifecycleEvents.SEED_CUSTOM, LifecycleEvents.OUTCOME_DIED, 3, null);
        RunLogWriter active = start("current", "current", 0, SEED,
                LifecycleEvents.SEED_CUSTOM, "fixture");
        enter(active, 1, 9, 0);
        assertTrue(active.close(false, 10000));

        SeedHistory.Summary all=lookup(Long.toString(SEED));
        assertEquals(2, all.attempts);
        SeedHistory.Summary earlier=SeedHistory.lookup(root(), Long.toString(SEED), null, "current");
        assertEquals(SeedHistory.Kind.MATCHING_LOCAL_HISTORY, earlier.kind);
        assertEquals(1, earlier.attempts);
        assertEquals(3, earlier.deepestFloor);
        assertEquals(0, earlier.unfinishedAttempts);
        assertEquals(2, SeedHistory.lookup(root(), Long.toString(SEED), null, null).attempts);
        // The excluded active run is not a reusable history source for the next score.
        SeedHistory.Summary first=SeedHistory.lookup(root(), Long.toString(SEED+1), null, "current");
        assertEquals(0, first.attempts);
    }

    @Test public void completedAndFailedAttemptsUseTheExactSeedAndDeepestRecordedFloor() {
        run("random-death", SEED, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 3, null);
        run("custom-win", SEED, LifecycleEvents.SEED_CUSTOM, LifecycleEvents.OUTCOME_WON, 26, null);
        run("another-seed", SEED + 1, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_WON, 99, null);
        SeedHistory.Summary result = lookup(DungeonSeed.convertToCode(SEED));
        assertEquals(SeedHistory.Kind.MATCHING_LOCAL_HISTORY, result.kind);
        assertEquals(2, result.attempts); assertEquals(26, result.deepestFloor);
        assertEquals(0, result.unfinishedAttempts);
        assertEquals(2, result.attemptsWithKnownDepth);
        assertEquals(result.attempts, lookup(Long.toString(SEED)).attempts);
        // Prior custom play is a history match, never a generated-locally or creator classification.
        assertNotEquals(SeedHistory.Kind.GENERATED_LOCALLY, result.kind);
        assertEquals(SeedHistory.Kind.UNKNOWN_IMPORTED, lookup("unrecorded pasted seed").kind);
    }

    @Test public void exactLogicalSegmentsAndCopiedLogsCountOnceAndLatestOpenSegmentIsNotFinished() throws Exception {
        run("one", SEED, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 4, "one-ranking");
        RunLogWriter resumed = start("one_1", "one", 1, SEED, LifecycleEvents.SEED_RANDOM, "fixture");
        end(resumed, 5, LifecycleEvents.OUTCOME_WON, 20, true, "one-ranking"); assertTrue(resumed.close(true, 10000));
        Files.copy(new File(root(), "runs/one_1.apdlog").toPath(), new File(root(), "runs/copied.apdlog").toPath());
        LegacyEvents.Record priorFinish = old("one-ranking", SEED, 20);
        priorFinish.putNum(FactKeys.OUTCOME, LifecycleEvents.OUTCOME_WON);
        legacy("one-ranking-copy", priorFinish);
        assertEquals(1, lookup(Long.toString(SEED)).attempts);
        assertEquals(20, lookup(Long.toString(SEED)).deepestFloor);
        RunLogWriter open = start("one_2", "one", 2, SEED, LifecycleEvents.SEED_RANDOM, "fixture");
        assertTrue(open.close(false, 10000));
        SeedHistory.Summary latest = lookup(Long.toString(SEED));
        assertEquals(SeedHistory.Kind.MATCHING_LOCAL_HISTORY, latest.kind);
        assertEquals(1, latest.attempts);
        assertEquals(1, latest.unfinishedAttempts);
        assertEquals(-1, latest.deepestFloor);
        assertEquals(0, latest.attemptsWithKnownDepth);
    }

    @Test public void unfinishedRecordedHistoryMatchesAndUsesCanonicalObservedDepth() throws Exception {
        RunLogWriter writer = start("unfinished", "unfinished", 0, SEED, LifecycleEvents.SEED_RANDOM, "fixture");
        enter(writer, 1, 3, 0);
        enter(writer, 2, 99, 1); // A side branch is not a deeper main-dungeon floor.
        LifecycleEvents.Save save = new LifecycleEvents.Save(); save.depth = 3;
        assertTrue(LifecycleEvents.emitSave(writer, 3, save));
        enter(writer, 4, 20, 0);
        LifecycleEvents.Rollback rollback = new LifecycleEvents.Rollback();
        rollback.kind = LifecycleEvents.ROLLBACK_MATCHED; rollback.matchedSaveIndex = 3; rollback.abandonedEvents = 1;
        assertTrue(LifecycleEvents.emitRollback(writer, 5, rollback));
        enter(writer, 6, 7, 0);
        assertTrue(writer.close(false, 10000));
        File raw = new File(root(), "runs/unfinished.apdlog");
        byte[] before = Files.readAllBytes(raw.toPath());
        SeedHistory.Summary result = lookup(Long.toString(SEED));
        assertEquals(SeedHistory.Kind.MATCHING_LOCAL_HISTORY, result.kind);
        assertEquals(1, result.attempts);
        assertEquals(1, result.unfinishedAttempts);
        assertEquals(7, result.deepestFloor);
        assertEquals(1, result.attemptsWithKnownDepth);
        assertArrayEquals(before, Files.readAllBytes(raw.toPath()));
    }

    @Test public void crashedHistoryWithoutDepthStillMatchesAndKeepsUnknownDepth() throws Exception {
        RunLogWriter writer = start("crashed", "crashed", 0, SEED, LifecycleEvents.SEED_CUSTOM, "fixture");
        assertTrue(writer.close(false, 10000));
        File raw = new File(root(), "runs/crashed.apdlog");
        byte[] frame = LogFormat.eventFrame(LogTestSupport.T_PLAIN, 0, 3, new byte[]{7});
        Files.write(raw.toPath(), Arrays.copyOf(frame, frame.length - 1), StandardOpenOption.APPEND);
        assertEquals(ReadStatus.TRUNCATED, RunLogReader.read(raw).status);
        byte[] before = Files.readAllBytes(raw.toPath());
        SeedHistory.Summary result = lookup(DungeonSeed.convertToCode(SEED));
        assertEquals(SeedHistory.Kind.MATCHING_LOCAL_HISTORY, result.kind);
        assertEquals(1, result.attempts);
        assertEquals(1, result.unfinishedAttempts);
        assertEquals(-1, result.deepestFloor);
        assertEquals(0, result.attemptsWithKnownDepth);
        assertTrue(result.incompleteHistory);
        assertArrayEquals(before, Files.readAllBytes(raw.toPath()));
    }

    @Test public void mixedFinishedAndOpenExactIdentitiesCountOnceWithoutResurrectingTerminalDepth() throws Exception {
        run("old-finish", SEED, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 26, "shared-ranking");
        RunLogWriter open = start("open-copy", "open-copy", 0, SEED, LifecycleEvents.SEED_CUSTOM, "fixture");
        enter(open, 1, 6, 0);
        LegacyEvents.RankingLink link = new LegacyEvents.RankingLink(); link.gameId = "shared-ranking";
        assertTrue(LegacyEvents.emitRankingLink(open, 2, link));
        assertTrue(open.close(false, 10000));
        Files.copy(new File(root(), "runs/open-copy.apdlog").toPath(), new File(root(), "runs/duplicate-open.apdlog").toPath());
        RunLogWriter unknown = start("other-open", "other-open", 0, SEED, LifecycleEvents.SEED_RANDOM, "fixture");
        assertTrue(unknown.close(false, 10000));
        legacy("retained", old("shared-ranking", SEED, 26), old("independent-finish", SEED, 4));
        LegacyImporter.Extract current = new LegacyImporter.Extract();
        current.fresh.add(old("shared-ranking", SEED, 26));
        SeedHistory.Summary result = SeedHistory.lookup(root(), Long.toString(SEED), current);
        assertEquals(SeedHistory.Kind.MATCHING_LOCAL_HISTORY, result.kind);
        assertEquals(3, result.attempts);
        assertEquals(2, result.unfinishedAttempts);
        assertEquals(6, result.deepestFloor);
        assertEquals(2, result.attemptsWithKnownDepth);
    }

    @Test public void rollbackRemovesTheAbandonedDeepFinish() {
        RunLogWriter writer = start("rollback", "rollback", 0, SEED, LifecycleEvents.SEED_CUSTOM, "fixture");
        LifecycleEvents.Save save = new LifecycleEvents.Save(); save.depth = 2;
        assertTrue(LifecycleEvents.emitSave(writer, 1, save));
        end(writer, 2, LifecycleEvents.OUTCOME_WON, 26, true, null);
        LifecycleEvents.Rollback rollback = new LifecycleEvents.Rollback();
        rollback.kind = LifecycleEvents.ROLLBACK_MATCHED; rollback.matchedSaveIndex = 1; rollback.abandonedEvents = 1;
        assertTrue(LifecycleEvents.emitRollback(writer, 3, rollback));
        end(writer, 4, LifecycleEvents.OUTCOME_DIED, 5, true, null); assertTrue(writer.close(true, 10000));
        SeedHistory.Summary result = lookup(Long.toString(SEED));
        assertEquals(1, result.attempts); assertEquals(5, result.deepestFloor);
    }

    @Test public void rankingIdentityDeduplicatesRawLegacyAndRetainedRecordsButProbableMatchesDoNot() {
        run("recorded", SEED, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 5, "same-ranking");
        legacy("import-a", old("same-ranking", SEED, 5), old("same-numbers-different-id", SEED, 5));
        legacy("import-b", old("same-ranking", SEED, 5));
        LegacyImporter.Extract current = new LegacyImporter.Extract();
        current.fresh.add(old("same-ranking", SEED, 5));
        current.fresh.add(old("new-retained", SEED, 8));
        SeedHistory.Summary result = SeedHistory.lookup(root(), Long.toString(SEED), current);
        assertEquals(3, result.attempts); assertEquals(8, result.deepestFloor);
        // Existing statistics would PROBABLE-link this ID-less lookalike; seed lookup must not.
        run("no-link", SEED + 1, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 5, null);
        legacy("import-c", old("probable-with-other-seed", SEED + 2, 5));
        assertEquals(1, lookup(Long.toString(SEED + 2)).attempts);
    }

    @Test public void absentDepthStaysUnknownAndExactRankingCanSupplyIt() {
        RunLogWriter unknown = start("unknown-depth", "unknown-depth", 0, SEED, LifecycleEvents.SEED_RANDOM, "fixture");
        end(unknown, 5, LifecycleEvents.OUTCOME_DIED, 0, false, "known-ranking"); assertTrue(unknown.close(true, 10000));
        SeedHistory.Summary first = lookup(Long.toString(SEED));
        assertEquals(1, first.attempts); assertEquals(-1, first.deepestFloor); assertEquals(0, first.attemptsWithKnownDepth);
        legacy("import-depth", old("known-ranking", SEED, 6), old("still-unknown", SEED, -1));
        SeedHistory.Summary withOld = lookup(Long.toString(SEED));
        assertEquals(2, withOld.attempts); assertEquals(6, withOld.deepestFloor); assertEquals(1, withOld.attemptsWithKnownDepth);
    }

    @Test public void exactRankingLinksMergeTransitivelyAndConflictingSeedIdentityIsNotTrusted() {
        run("first", SEED, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 5, "rank-one");
        run("second", SEED, LifecycleEvents.SEED_CUSTOM, LifecycleEvents.OUTCOME_DIED, 5, "rank-two");
        RunLogWriter old = start("bridge", "bridge", 0, SEED, LifecycleEvents.SEED_RANDOM, "fixture");
        end(old, 5, LifecycleEvents.OUTCOME_DIED, 5, true, "rank-one"); assertTrue(old.close(true, 10000));
        RunLogWriter next = start("bridge_1", "bridge", 1, SEED, LifecycleEvents.SEED_RANDOM, "fixture");
        end(next, 5, LifecycleEvents.OUTCOME_DIED, 5, true, "rank-two"); assertTrue(next.close(true, 10000));
        assertEquals(1, lookup(Long.toString(SEED)).attempts);
        run("conflict", SEED + 10, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 5, null);
        RunLogWriter conflict = start("conflict_1", "conflict", 1, SEED + 11, LifecycleEvents.SEED_RANDOM, "fixture");
        end(conflict, 5, LifecycleEvents.OUTCOME_WON, 26, true, null); assertTrue(conflict.close(true, 10000));
        SeedHistory.Summary rejected = lookup(Long.toString(SEED + 10));
        assertEquals(SeedHistory.Kind.UNKNOWN_IMPORTED, rejected.kind); assertTrue(rejected.incompleteHistory);
        run("conflicting-death", SEED + 20, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 5, "conflicting-ranking");
        run("conflicting-win", SEED + 20, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_WON, 5, "conflicting-ranking");
        assertEquals(0, lookup(Long.toString(SEED + 20)).attempts);
        run("cross-seed-first", SEED + 30, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 5, "cross-seed-ranking");
        run("cross-seed-second", SEED + 31, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 5, "cross-seed-ranking");
        assertEquals(0, lookup(Long.toString(SEED + 30)).attempts);
        assertEquals(0, lookup(Long.toString(SEED + 31)).attempts);
        run("raw-conflict", SEED + 40, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 5, "raw-legacy-conflict");
        legacy("conflicting-legacy", old("raw-legacy-conflict", SEED + 41, 5));
        assertEquals(0, lookup(Long.toString(SEED + 40)).attempts);
        assertEquals(0, lookup(Long.toString(SEED + 41)).attempts);
    }

    @Test public void redactedZeroDemoDailyReplayMissingStartAndFutureLogsCannotClaimHistory() {
        RunLogWriter redacted = start("redacted", "redacted", 0, 0, LifecycleEvents.SEED_RANDOM, LogHeader.REDACTED_PREFIX + "fixture");
        end(redacted, 1, LifecycleEvents.OUTCOME_DIED, 99, true, null); assertTrue(redacted.close(true, 10000));
        run("demo-synthetic", 0, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_WON, 99, null);
        RunLogWriter synthetic = start("synthetic", "synthetic", 0, 0, LifecycleEvents.SEED_RANDOM, "SYNTHETIC-DEMO");
        end(synthetic, 1, LifecycleEvents.OUTCOME_DIED, 99, true, null); assertTrue(synthetic.close(true, 10000));
        run("daily", 0, LifecycleEvents.SEED_DAILY, LifecycleEvents.OUTCOME_WON, 99, null);
        run("replay", 0, LifecycleEvents.SEED_DAILY_REPLAY, LifecycleEvents.OUTCOME_WON, 99, null);
        RunLogWriter missing = RunLogWriter.create(new File(root(), "runs/missing-start.apdlog"), TelemetryVersion.CURRENT, "missing-start", "fixture", 1);
        end(missing, 1, LifecycleEvents.OUTCOME_DIED, 99, true, null); assertTrue(missing.close(true, 10000));
        RunLogWriter future = RunLogWriter.create(new File(root(), "runs/future.apdlog"),
                new TelemetryVersion(TelemetryVersion.CURRENT.major + 1, 0), "future", "fixture", 1);
        LifecycleEvents.RunStart start = new LifecycleEvents.RunStart(); start.runId = "future";
        assertTrue(LifecycleEvents.emitRunStart(future, 0, false, start));
        end(future, 1, LifecycleEvents.OUTCOME_DIED, 99, true, null); assertTrue(future.close(true, 10000));
        assertEquals(SeedHistory.Kind.UNKNOWN_IMPORTED, lookup("0").kind);
        // Zero is a real seed only when it is explicitly recorded in an unredacted supported log.
        run("real-zero", 0, LifecycleEvents.SEED_CUSTOM, LifecycleEvents.OUTCOME_DIED, 2, null);
        SeedHistory.Summary result = lookup("AAA-AAA-AAA");
        assertEquals(1, result.attempts); assertEquals(2, result.deepestFloor);
    }

    @Test public void explicitNegativeOneIsRecordedHistoryNotAnEmptySelection() throws Exception {
        run("negative-one", -1, LifecycleEvents.SEED_CUSTOM, LifecycleEvents.OUTCOME_DIED, 6, null);
        File raw = new File(root(), "runs/negative-one.apdlog");
        byte[] before = Files.readAllBytes(raw.toPath());
        SeedHistory.Summary matching = lookup("-1");
        assertEquals(SeedHistory.Kind.MATCHING_LOCAL_HISTORY, matching.kind);
        assertEquals(-1, matching.seed);
        assertEquals(1, matching.attempts);
        assertEquals(6, matching.deepestFloor);
        assertEquals(1, matching.attemptsWithKnownDepth);
        assertEquals(SeedHistory.Kind.GENERATED_LOCALLY, lookup("").kind);
        assertEquals(SeedHistory.Kind.UNKNOWN_IMPORTED, lookup("-2").kind);
        assertArrayEquals(before, Files.readAllBytes(raw.toPath()));
    }

    @Test public void deletedRawRecordsDoNotSurviveInCachesAndLookupWritesNothing() throws Exception {
        run("deleted", SEED, LifecycleEvents.SEED_RANDOM, LifecycleEvents.OUTCOME_DIED, 7, null);
        DerivedStore store = new DerivedStore(root()); store.load();
        new StatsStore(root()).index().save();
        byte[] cache = Files.readAllBytes(store.cacheFile().toPath());
        byte[] index = Files.readAllBytes(new File(root(), "index.apdi").toPath());
        File raw = new File(root(), "runs/deleted.apdlog"); byte[] original = Files.readAllBytes(raw.toPath());
        assertEquals(1, lookup(Long.toString(SEED)).attempts);
        assertArrayEquals(original, Files.readAllBytes(raw.toPath()));
        assertTrue(raw.delete());
        assertEquals(SeedHistory.Kind.UNKNOWN_IMPORTED, lookup(Long.toString(SEED)).kind);
        assertArrayEquals(cache, Files.readAllBytes(store.cacheFile().toPath()));
        assertArrayEquals(index, Files.readAllBytes(new File(root(), "index.apdi").toPath()));
        File absent = new File(root(), "never-created");
        assertEquals(SeedHistory.Kind.UNKNOWN_IMPORTED, SeedHistory.lookup(absent, "123", null).kind);
        assertFalse(absent.exists());
    }

    private Rankings.Record recordBundle(String id, Long seed, Integer deepest, boolean ascending) {
        Rankings.Record record = new Rankings.Record(); record.gameID = id; record.win = false;
        record.daily = false; record.ascending = ascending; record.depth = 2;
        record.score = 12345; record.customSeed = "";
        record.heroClass = com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR;
        Bundle data = new Bundle(); if (seed != null) data.put("seed", seed.longValue());
        Bundle stats = new Bundle(); if (deepest != null) stats.put("maxDepth", deepest.intValue());
        data.put("stats", stats); record.gameData = data; return record;
    }

    @Test public void currentRankingFileAndOldSaveFieldsRemainReadOnlyAndUnknownWhenAbsent() throws Exception {
        HeadlessTestSupport.start();
        File file = FileUtils.getFileHandle(Rankings.RANKINGS_FILE).file();
        byte[] prior = file.isFile() ? Files.readAllBytes(file.toPath()) : null;
        try {
            Bundle rankings = new Bundle();
            Rankings.Record[] records = {recordBundle("retained-failed", SEED, 9, false),
                    recordBundle("old-ascent", SEED, null, true), recordBundle("old-no-seed", null, 26, false)};
            rankings.put("records", Arrays.asList(records)); rankings.put("total", 50); rankings.put("won", 12);
            FileUtils.bundleToFile(Rankings.RANKINGS_FILE, rankings);
            byte[] before = Files.readAllBytes(file.toPath());
            SeedHistory.Summary result = SeedHistory.lookup(root(), Long.toString(SEED), LegacyImporter.extractRankingsFile(10));
            assertEquals(2, result.attempts); assertEquals(9, result.deepestFloor); assertEquals(1, result.attemptsWithKnownDepth);
            assertArrayEquals(before, Files.readAllBytes(file.toPath()));
            Bundle restored = FileUtils.bundleFromFile(Rankings.RANKINGS_FILE);
            assertEquals(50, restored.getInt("total")); assertEquals(12, restored.getInt("won"));
            assertEquals(12345, restored.getBundleArray("records")[0].getInt("score"));
            assertFalse(restored.getBundleArray("records")[2].getBundle("gameData").contains("seed"));
            assertFalse(new File(root(), "legacy").exists());
        } finally {
            if (prior == null) Files.deleteIfExists(file.toPath()); else Files.write(file.toPath(), prior);
        }
    }

    @Test public void emptySelectionUsesActualRandomGeneratorAndSuppliedSelectionKeepsSavedSeedBehavior() throws Exception {
        HeadlessTestSupport.start();
        Field field = GameSettings.class.getDeclaredField("prefs"); field.setAccessible(true);
        Preferences prior = (Preferences) field.get(null);
        long oldSeed = Dungeon.seed; String oldCustom = Dungeon.customSeedText;
        boolean oldDaily = Dungeon.daily, oldReplay = Dungeon.dailyReplay;
        File prefs = new File(root(), "selection.xml");
        Random.pushGenerator(444);
        try {
            GameSettings.set(new HeadlessPreferences(new FileHandle(prefs)));
            Dungeon.daily = Dungeon.dailyReplay = false; SPDSettings.customSeed("");
            assertEquals(SeedHistory.Kind.GENERATED_LOCALLY, lookup(SPDSettings.customSeed()).kind);
            assertEquals(0, lookup(SPDSettings.customSeed()).unfinishedAttempts);
            Dungeon.initSeed();
            assertEquals("", Dungeon.customSeedText); assertTrue(Dungeon.seed >= 0 && Dungeon.seed < DungeonSeed.TOTAL_SEEDS);
            String generated = DungeonSeed.convertToCode(Dungeon.seed);
            for (String vowel : Arrays.asList("A", "E", "I", "O", "U")) assertFalse(generated.contains(vowel));
            String supplied = DungeonSeed.convertToCode(SEED);
            SPDSettings.customSeed(supplied);
            byte[] stored = Files.readAllBytes(prefs.toPath());
            assertEquals(SeedHistory.Kind.UNKNOWN_IMPORTED, lookup(SPDSettings.customSeed()).kind);
            assertArrayEquals(stored, Files.readAllBytes(prefs.toPath()));
            GameSettings.set(new HeadlessPreferences(new FileHandle(prefs)));
            Dungeon.initSeed(); assertEquals(SEED, Dungeon.seed); assertEquals(supplied, Dungeon.customSeedText);
            // Preserve the existing signed numeric-text seed conversion too, without changing it.
            run("signed-seed", -2, LifecycleEvents.SEED_CUSTOM, LifecycleEvents.OUTCOME_DIED, 4, null);
            assertEquals(1, lookup("-2").attempts);
            assertEquals(SeedHistory.Kind.MATCHING_LOCAL_HISTORY, lookup("-2").kind);
        } finally {
            Random.popGenerator(); GameSettings.set(prior); Dungeon.seed = oldSeed; Dungeon.customSeedText = oldCustom;
            Dungeon.daily = oldDaily; Dungeon.dailyReplay = oldReplay;
        }
    }
}
