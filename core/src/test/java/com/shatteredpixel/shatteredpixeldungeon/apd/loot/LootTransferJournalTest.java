package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.Assert.*;

public class LootTransferJournalTest {
    private static byte[] data(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static Map<String, byte[]> afterimages() {
        Map<String, byte[]> result = new LinkedHashMap<>();
        result.put("game.dat", data("scroll consumed; pending shipment T; run unchanged"));
        result.put("depth8.dat", data("source items removed"));
        result.put("depth11.dat", data("ordinary pile with full items; delivered receipt T"));
        return result;
    }
    private static MemoryStorage initial() {
        MemoryStorage s = new MemoryStorage();
        s.files.put("game.dat", data("scroll owned; no shipment"));
        s.files.put("depth8.dat", data("source owns loot"));
        s.files.put("depth11.dat", data("no delivered loot"));
        return s;
    }
    private static void equalImages(MemoryStorage s, Map<String,byte[]> expected) {
        for (String name : expected.keySet()) assertArrayEquals(name, expected.get(name), s.files.get(name));
    }
    private static void fails(IOAction action) throws Exception {
        try { action.run(); fail("Expected failure"); } catch (IOException expected) { }
    }
    private interface IOAction { void run() throws Exception; }

    @Test public void successfulCommitContainsAllAfterimagesAndRetiresJournal() throws Exception {
        MemoryStorage s = initial();
        LootTransferJournal j = new LootTransferJournal(s);
        j.commit(afterimages());
        equalImages(s, afterimages());
        assertFalse(j.recover());
        j.requireClear();
    }

    @Test public void interruptionBeforeAndAfterEveryMutationRecoversExactlyOnce() throws Exception {
        // journal + three save replacements + journal removal, each before/after.
        for (int step = 1; step <= 5; step++) for (boolean after : new boolean[]{false, true}) {
            MemoryStorage s = initial();
            Map<String,byte[]> original = new LinkedHashMap<>(s.files);
            s.failStep = step; s.failAfter = after;
            fails(() -> new LootTransferJournal(s).commit(afterimages()));
            s.failStep = -1;
            LootTransferJournal reload = new LootTransferJournal(s);
            boolean prepared = s.files.containsKey(LootTransferJournal.JOURNAL);
            reload.recover();
            if (step == 1 && !after) equalImages(s, original);
            else equalImages(s, afterimages());
            assertEquals(step != 1 || after, prepared || step == 5 && after);
            int writes = s.mutations;
            assertFalse(reload.recover());
            assertEquals(writes, s.mutations);
        }
    }

    @Test public void recoveryItselfCanBeInterruptedAtEveryRemainingWrite() throws Exception {
        for (int step = 1; step <= 4; step++) for (boolean after : new boolean[]{false, true}) {
            MemoryStorage s = initial(); s.failStep = 1; s.failAfter = true;
            fails(() -> new LootTransferJournal(s).commit(afterimages()));
            s.mutations = 0; s.failStep = step; s.failAfter = after;
            fails(() -> new LootTransferJournal(s).recover());
            s.failStep = -1;
            new LootTransferJournal(s).recover();
            equalImages(s, afterimages());
            assertFalse(s.files.containsKey(LootTransferJournal.JOURNAL));
        }
    }

    @Test public void corruptAndTruncatedJournalNeverWritesOrDiscardsAnyBytes() throws Exception {
        MemoryStorage prepared = prepared();
        byte[] good = prepared.files.get(LootTransferJournal.JOURNAL);
        for (int size : new int[]{0, 1, 10, 32, good.length - 1, good.length}) {
            MemoryStorage s = initial(); byte[] bad = Arrays.copyOf(good, size);
            if (size == good.length) bad[good.length / 2] ^= 1;
            s.files.put(LootTransferJournal.JOURNAL, bad);
            Map<String,byte[]> original = new LinkedHashMap<>(s.files);
            fails(() -> new LootTransferJournal(s).recover());
            equalImages(s, original); assertEquals(0, s.mutations);
            fails(() -> new LootTransferJournal(s).requireClear());
        }
    }

    @Test public void futureVersionWithValidChecksumIsPreservedAndBlocked() throws Exception {
        MemoryStorage s = prepared(); byte[] journal = s.files.get(LootTransferJournal.JOURNAL).clone();
        journal[7] = 2;
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(journal, journal.length - 32));
        System.arraycopy(hash, 0, journal, journal.length - 32, 32);
        s.files.put(LootTransferJournal.JOURNAL, journal); s.mutations = 0;
        fails(() -> new LootTransferJournal(s).recover());
        assertArrayEquals(journal, s.files.get(LootTransferJournal.JOURNAL)); assertEquals(0, s.mutations);
    }

    @Test public void pendingJournalRejectsNewTransferAndOrdinarySaveGuard() throws Exception {
        MemoryStorage s = prepared(); byte[] journal = s.files.get(LootTransferJournal.JOURNAL).clone();
        s.failStep = -1; s.mutations = 0;
        fails(() -> new LootTransferJournal(s).requireClear());
        fails(() -> new LootTransferJournal(s).commit(afterimages()));
        assertArrayEquals(journal, s.files.get(LootTransferJournal.JOURNAL)); assertEquals(0, s.mutations);
    }

    @Test public void invalidPathsOrIncompleteFileSetsFailBeforePreparation() throws Exception {
        for (String bad : new String[]{"../game.dat", "/game.dat", "other/game.dat", "depth0.dat",
                "depth1-branch0.dat", "depth1.dat.spdtmp", LootTransferJournal.JOURNAL}) {
            MemoryStorage s = initial(); Map<String,byte[]> images = afterimages(); images.put(bad, data("bad"));
            fails(() -> new LootTransferJournal(s).commit(images)); assertEquals(0, s.mutations);
        }
        MemoryStorage s = initial();
        fails(() -> new LootTransferJournal(s).commit(Collections.singletonMap("game.dat", data("alone"))));
        Map<String,byte[]> images = afterimages(); images.put("depth8.dat", new byte[0]);
        fails(() -> new LootTransferJournal(s).commit(images)); assertEquals(0, s.mutations);
    }

    @Test public void storageThatLiesAboutWritingJournalCannotModifySaveFiles() throws Exception {
        MemoryStorage s = initial(); s.corruptJournalWrite = true;
        fails(() -> new LootTransferJournal(s).commit(afterimages()));
        equalImages(s, initial().files); assertEquals(1, s.mutations);
        fails(() -> new LootTransferJournal(s).recover());
    }

    @Test public void binaryPayloadsAndBranchFloorsRoundTripWithoutTransformation() throws Exception {
        MemoryStorage s = initial(); Map<String,byte[]> images = afterimages();
        byte[] binary = new byte[256 * 1024]; new Random(382).nextBytes(binary);
        images.put("depth12-branch1.dat", binary);
        new LootTransferJournal(s).commit(images); equalImages(s, images);
    }

    @Test public void actualDiskStorageCommitsReplacesAndLeavesUnrelatedFilesIntact() throws Exception {
        File dir = Files.createTempDirectory("apd-loot-journal-").toFile();
        try {
            Files.write(new File(dir,"game.dat").toPath(), data("old run"));
            Files.write(new File(dir,"depth8.dat").toPath(), data("old floor"));
            Files.write(new File(dir,"history.jsonl").toPath(), data("untouched history"));
            LootTransferJournal j = new LootTransferJournal(dir); j.commit(afterimages());
            for (String name : afterimages().keySet())
                assertArrayEquals(afterimages().get(name), Files.readAllBytes(new File(dir,name).toPath()));
            assertArrayEquals(data("untouched history"), Files.readAllBytes(new File(dir,"history.jsonl").toPath()));
            assertFalse(j.recover());
            // An abandoned pre-rename temporary image never constitutes a commit.
            Files.write(new File(dir,LootTransferJournal.JOURNAL + ".loot-writing").toPath(), data("partial"));
            assertFalse(j.recover());
            j.commit(afterimages());
            assertFalse(new File(dir,LootTransferJournal.JOURNAL + ".loot-writing").exists());
        } finally { for (File file : dir.listFiles()) assertTrue(file.delete()); assertTrue(dir.delete()); }
    }

    private static MemoryStorage prepared() throws Exception {
        MemoryStorage s = initial(); s.failStep = 1; s.failAfter = true;
        fails(() -> new LootTransferJournal(s).commit(afterimages()));
        s.failStep = -1; return s;
    }

    private static final class MemoryStorage implements LootTransferJournal.Storage {
        final Map<String,byte[]> files = new LinkedHashMap<>();
        int mutations, failStep = -1; boolean failAfter, corruptJournalWrite;
        @Override public boolean exists(String name) { return files.containsKey(name); }
        @Override public byte[] read(String name) throws IOException {
            if (!files.containsKey(name)) throw new IOException("Missing " + name);
            return files.get(name).clone();
        }
        private void before() throws IOException {
            mutations++; if (mutations == failStep && !failAfter) throw new IOException("Injected before mutation");
        }
        private void after() throws IOException {
            if (mutations == failStep && failAfter) throw new IOException("Injected after mutation");
        }
        @Override public void replace(String name, byte[] bytes) throws IOException {
            before(); byte[] owned = bytes.clone();
            if (corruptJournalWrite && name.equals(LootTransferJournal.JOURNAL)) owned[0] ^= 1;
            files.put(name, owned); after();
        }
        @Override public void remove(String name) throws IOException { before(); files.remove(name); after(); }
    }
}
