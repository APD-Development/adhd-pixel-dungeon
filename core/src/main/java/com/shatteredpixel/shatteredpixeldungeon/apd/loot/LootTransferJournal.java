package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Write-ahead, whole-file afterimages for a rare loot transfer. Existing game and
 * level formats are opaque here. Callers must include consumed scroll/run state,
 * source removal, pending shipments and any destination receipt in ONE commit.
 *
 * A prepared journal is the commit point: recovery rolls forward, never repeats
 * item operations. Until recovery completes, callers MUST block ordinary saves,
 * run replacement/deletion and gameplay. Recover before deserializing the run.
 * This protects process interruption; directory durability after power loss is
 * platform-dependent and is not claimed by this implementation.
 */
public final class LootTransferJournal {
    static final String JOURNAL = "loot-transfer.journal";
    private static final int MAGIC = 0x4150444c, VERSION = 1, HASH_SIZE = 32;
    private static final Object LOCK = new Object();

    interface Storage {
        boolean exists(String name) throws IOException;
        byte[] read(String name) throws IOException;
        void replace(String name, byte[] contents) throws IOException;
        void remove(String name) throws IOException;
    }

    private final Storage storage;

    /** Supply the existing slot directory, never the global save root. */
    public LootTransferJournal(File slotDirectory) throws IOException {
        this(new DiskStorage(slotDirectory));
    }

    LootTransferJournal(Storage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    /** Does not treat an empty/corrupt journal as absent. */
    public void requireClear() throws IOException {
        synchronized (LOCK) {
            if (storage.exists(JOURNAL))
                throw new IOException("Loot transfer requires recovery before saving");
        }
    }

    /**
     * All serialization and semantic validation must finish before calling this.
     * On failure after preparation the caller must reload, not resume play.
     */
    public void commit(Map<String, byte[]> afterimages) throws IOException {
        synchronized (LOCK) {
            requireClear();
            // Own immutable byte snapshots, so later caller mutation cannot alter recovery.
            byte[] encoded = encode(afterimages);
            Map<String, byte[]> checked = decode(encoded);
            storage.replace(JOURNAL, encoded);
            // Verify the durable record before changing a single save file.
            if (!Arrays.equals(encoded, storage.read(JOURNAL)))
                throw new IOException("Loot journal write verification failed");
            apply(checked);
        }
    }

    /** Validate the ENTIRE journal before any write; preserve bad bytes for recovery. */
    public boolean recover() throws IOException {
        synchronized (LOCK) {
            if (!storage.exists(JOURNAL)) return false;
            Map<String, byte[]> checked = decode(storage.read(JOURNAL));
            apply(checked);
            return true;
        }
    }

    private void apply(Map<String, byte[]> afterimages) throws IOException {
        for (Map.Entry<String, byte[]> entry : afterimages.entrySet()) {
            // Equal files need no rewrite; replay is idempotent at every boundary.
            if (!storage.exists(entry.getKey())
                    || !Arrays.equals(storage.read(entry.getKey()), entry.getValue()))
                storage.replace(entry.getKey(), entry.getValue());
            if (!Arrays.equals(storage.read(entry.getKey()), entry.getValue()))
                throw new IOException("Loot transfer save verification failed: " + entry.getKey());
        }
        storage.remove(JOURNAL);
    }

    private static void validName(String name) throws IOException {
        if (name == null || !(name.equals("game.dat")
                || name.matches("depth[1-9][0-9]*(?:-branch[1-9][0-9]*)?\\.dat")))
            throw new IOException("Invalid loot transfer save path");
    }

    private static void validSet(Map<String, byte[]> images) throws IOException {
        if (images == null || images.size() < 2 || !images.containsKey("game.dat"))
            throw new IOException("Loot transfer requires run and floor afterimages");
        for (Map.Entry<String, byte[]> entry : images.entrySet()) {
            validName(entry.getKey());
            if (entry.getValue() == null || entry.getValue().length == 0)
                throw new IOException("Empty loot transfer save afterimage");
        }
    }

    private static byte[] hash(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static byte[] encode(Map<String, byte[]> images) throws IOException {
        validSet(images);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        out.writeInt(images.size());
        // Stable ordering, independent of the caller's map implementation.
        for (String name : new TreeSet<>(images.keySet())) {
            byte[] image = images.get(name).clone();
            out.writeUTF(name);
            out.writeInt(image.length);
            out.write(image);
        }
        out.flush();
        byte[] body = bytes.toByteArray();
        out.write(hash(body));
        out.flush();
        return bytes.toByteArray();
    }

    private static Map<String, byte[]> decode(byte[] encoded) throws IOException {
        if (encoded == null || encoded.length < 12 + HASH_SIZE)
            throw new IOException("Incomplete loot journal");
        byte[] body = Arrays.copyOf(encoded, encoded.length - HASH_SIZE);
        if (!MessageDigest.isEqual(hash(body),
                Arrays.copyOfRange(encoded, body.length, encoded.length)))
            throw new IOException("Corrupt loot journal checksum");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(body));
        if (in.readInt() != MAGIC || in.readInt() != VERSION)
            throw new IOException("Unsupported loot journal format");
        int count = in.readInt();
        // Bound allocations by actual available bytes, not trusted length fields.
        if (count < 2 || count > in.available() / 7)
            throw new IOException("Invalid loot journal file count");
        Map<String, byte[]> images = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String name = in.readUTF();
            validName(name);
            int length = in.readInt();
            if (length <= 0 || length > in.available() || images.containsKey(name))
                throw new IOException("Invalid loot journal afterimage");
            byte[] image = new byte[length];
            in.readFully(image);
            images.put(name, image);
        }
        if (in.available() != 0) throw new IOException("Trailing loot journal data");
        validSet(images);
        return images;
    }

    /** No delete-before-rename gap; a failed replacement leaves the journal intact. */
    private static final class DiskStorage implements Storage {
        private final File directory;
        DiskStorage(File directory) throws IOException {
            this.directory = directory.getCanonicalFile();
            if (!this.directory.isDirectory()) throw new IOException("Missing save slot directory");
        }
        private File file(String name) throws IOException {
            if (!name.equals(JOURNAL)) validName(name);
            File result = new File(directory, name);
            if (!directory.equals(result.getCanonicalFile().getParentFile()))
                throw new IOException("Loot save path escapes its slot");
            return result;
        }
        @Override public boolean exists(String name) throws IOException { return file(name).exists(); }
        @Override public byte[] read(String name) throws IOException {
            try (InputStream in = new FileInputStream(file(name));
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] block = new byte[8192];
                int size;
                while ((size = in.read(block)) != -1) out.write(block, 0, size);
                return out.toByteArray();
            }
        }
        @Override public void replace(String name, byte[] contents) throws IOException {
            File target = file(name);
            File temporary = new File(directory, name + ".loot-writing");
            // Refuse symlinked temporary paths too. Never truncate files outside this slot.
            if (!directory.equals(temporary.getCanonicalFile().getParentFile()))
                throw new IOException("Loot temporary path escapes its slot");
            try (FileOutputStream out = new FileOutputStream(temporary)) {
                out.write(contents);
                out.getFD().sync();
            }
            if (!temporary.renameTo(target))
                throw new IOException("Cannot atomically replace loot save: " + name);
        }
        @Override public void remove(String name) throws IOException {
            File target = file(name);
            if (target.exists() && !target.delete()) throw new IOException("Cannot retire loot journal");
        }
    }
}
