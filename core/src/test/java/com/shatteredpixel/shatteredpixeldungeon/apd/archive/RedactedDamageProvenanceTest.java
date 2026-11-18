package com.shatteredpixel.shatteredpixeldungeon.apd.archive;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import static org.junit.Assert.*;

public class RedactedDamageProvenanceTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private byte[] log() throws Exception {
        TestLogs l = new TestLogs(tmp.newFolder(), "redacted-damage", TelemetryVersion.CURRENT, 1000)
                .start("redacted-damage", 0, "MAGE", ArchiveSupport.RS, 0, 0, false).enter(1);
        l.heroAttacks("rat", "actors.mobs.Rat", true).close(false);
        return Files.readAllBytes(l.file.toPath());
    }

    private byte[] concat(byte[] left, byte[] right) {
        byte[] out = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, out, left.length, right.length);
        return out;
    }

    private void incompleteCopy(byte[] damaged, ReadStatus expected) {
        byte[] original = damaged.clone();
        ReadResult before = RunLogReader.parse(damaged);
        assertEquals(expected, before.status);
        RunFacts facts = RunFactsBuilder.fromLog(before, "before");
        assertEquals(1, facts.numOr(FactKeys.HERO_ATTACKS, -1));
        assertEquals(Fact.PROV_INCOMPLETE, facts.get(FactKeys.HERO_ATTACKS).prov);
        ReadResult after = RunLogReader.parse(LogRedactor.redact(damaged).bytes);
        RunFacts redacted = RunFactsBuilder.fromLog(after, "after");
        assertEquals(1, redacted.numOr(FactKeys.HERO_ATTACKS, -1));
        assertEquals("Redaction must preserve incomplete-source knowledge", Fact.PROV_INCOMPLETE,
                redacted.get(FactKeys.HERO_ATTACKS).prov);
        assertEquals(1, after.gapEvents);
        ApdEvent gap = after.events.get(after.events.size() - 1);
        assertEquals(EventTypes.GAP, gap.type);
        assertEquals(before.events.get(before.events.size() - 2).clock, gap.clock);
        PayloadReader payload = gap.reader(after);
        assertEquals(5, payload.unsigned());
        assertEquals("unknown loss must not expose hidden byte/event counts", 0, payload.unsigned());
        assertArrayEquals(original, damaged);
    }

    @Test public void truncatedSourceMustNotBecomeAnExactCleanHistory() throws Exception {
        byte[] clean = log();
        byte[] extra = LogFormat.eventFrame(EventTypes.ATTACK, 0, 0, new byte[]{1, 2});
        byte[] damaged = Arrays.copyOf(clean, clean.length + extra.length - 1);
        System.arraycopy(extra, 0, damaged, clean.length, extra.length - 1);
        incompleteCopy(damaged, ReadStatus.TRUNCATED);
    }

    @Test public void corruptAndMismatchedSourcesKeepTheirIncompleteWarning() throws Exception {
        byte[] clean = log();
        incompleteCopy(concat(clean, new byte[]{0, 1, 2, 3, 4}), ReadStatus.CORRUPT);
        incompleteCopy(concat(clean, LogFormat.sealFrame(9999, 0)), ReadStatus.SEAL_MISMATCH);
    }

    @Test public void unreadablePublicPayloadIsNotSilentlyDroppedWithoutAWarning() throws Exception {
        byte[] damaged = concat(log(), LogFormat.eventFrame(EventTypes.ATTACK, 0, 0, new byte[0]));
        assertEquals(ReadStatus.UNSEALED, RunLogReader.parse(damaged).status);
        LogRedactor.Result result = LogRedactor.redact(damaged);
        assertEquals(1, result.eventsUnreadable);
        ReadResult redacted = RunLogReader.parse(result.bytes);
        assertEquals(1, redacted.gapEvents);
        assertEquals(Fact.PROV_INCOMPLETE, RunFactsBuilder.fromLog(redacted, "after").get(FactKeys.HERO_ATTACKS).prov);
    }

    @Test public void cleanUnsealedAndSealedSourcesRemainExactWithoutInventedGaps() throws Exception {
        byte[] clean = log();
        ReadResult read = RunLogReader.parse(clean);
        byte[] sealed = concat(clean, LogFormat.sealFrame(read.eventCount, read.lastClock));
        for (byte[] bytes : new byte[][]{clean, sealed, concat(sealed, new byte[]{1,2,3})}) {
            ReadResult redacted = RunLogReader.parse(LogRedactor.redact(bytes).bytes);
            assertEquals(0, redacted.gapEvents);
            assertEquals(Fact.PROV_RECORDED, RunFactsBuilder.fromLog(redacted, "clean").get(FactKeys.HERO_ATTACKS).prov);
        }
    }

    @Test public void damagedExportImportKeepsCountsIncompleteAndOriginalHistoryIntact() throws Exception {
        File source = tmp.newFolder();
        TestLogs l = new TestLogs(source, "export-damage", TelemetryVersion.CURRENT, 1)
                .start("export-damage", 0, "MAGE", ArchiveSupport.RS, 0, 0, false).enter(1);
        l.heroAttacks("rat", "actors.mobs.Rat", true).close(false);
        byte[] original = concat(Files.readAllBytes(l.file.toPath()), new byte[]{0,1,2,3});
        Files.write(l.file.toPath(), original);
        byte[] archive = ArchiveSupport.export(source);
        File target = tmp.newFolder();
        ArchiveImporter.Report imported = ArchiveImporter.importArchive(target, new ByteArrayInputStream(archive));
        assertTrue(imported.problem, imported.ok);
        ReadResult read = RunLogReader.read(new File(target, "runs/export-damage.apdlog"));
        assertEquals(1, read.gapEvents);
        assertEquals(Fact.PROV_INCOMPLETE, RunFactsBuilder.fromLog(read, "imported").get(FactKeys.HERO_ATTACKS).prov);
        assertArrayEquals(original, Files.readAllBytes(l.file.toPath()));
    }
}
