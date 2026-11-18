/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.apd.backend;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class OutboxStoreTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static Outbox sample(){
		Outbox box = new Outbox();
		RunSubmission s = new RunSubmission(Id.of(Id.Kind.RUN, "r1"), "rs", 1, 3, 0, "MAGE", RunSubmission.Outcome.WON, 26, 5000, 600_000, 77, "4.0.8-apd", 1, 8, Canon.hex(Canon.sha256("r1".getBytes())));
		Outbox.Item i = box.enqueue(Id.of(Id.Kind.OUTBOX, "ob_1"), RunSubmissionCodec.toEnvelope(s), 123);
		i.attempts = 3;
		i.nextAttemptAtMillis = 999;
		i.lastDetail = "busy\nreally";
		box.enqueue(Id.of(Id.Kind.OUTBOX, "ob_2"), new Envelope("apd.report", 1, 0, "line one\nline two"), 456);
		return box;
	}

	@Test
	public void theQueueSurvivesARestartExactly() throws Exception {
		File f = new File(tmp.getRoot(), "outbox.txt");
		Outbox a = sample();
		OutboxStore.save(a, f);
		Outbox b = new Outbox();
		OutboxStore.Loaded l = OutboxStore.load(b, f);
		assertEquals(2, l.items);
		assertEquals(0, l.skipped);
		for (int i = 0; i < 2; i++){
			Outbox.Item x = a.items().get(i), y = b.items().get(i);
			assertEquals(x.id, y.id);
			assertEquals(x.envelope.payload, y.envelope.payload);
			assertEquals(x.envelope.schema, y.envelope.schema);
			assertEquals(x.attempts, y.attempts);
			assertEquals(x.nextAttemptAtMillis, y.nextAttemptAtMillis);
			assertEquals(x.lastDetail, y.lastDetail);
		}
		assertFalse("no temporary file is left behind", new File(f.getPath() + ".tmp").exists());
	}

	@Test
	public void missingFileIsAnEmptyQueueAndDamageLosesOnlyTheDamagedLine() throws Exception {
		File f = new File(tmp.getRoot(), "outbox.txt");
		assertEquals(0, OutboxStore.load(new Outbox(), f).items);
		OutboxStore.save(sample(), f);
		String[] lines = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).split("\n");
		Files.write(f.toPath(), (lines[0] + "\nGARBAGE LINE\n" + lines[1].substring(0, 20) + "\n" + lines[1] + "\n").getBytes(StandardCharsets.UTF_8));
		OutboxStore.Loaded l = OutboxStore.load(new Outbox(), f);
		assertEquals(2, l.items);
		assertEquals(2, l.skipped);
	}

	@Test
	public void aHandEditedFileCannotGetLocalOnlyDataSent() throws Exception {
		File f = new File(tmp.getRoot(), "outbox.txt");
		String payload = java.util.Base64.getEncoder().encodeToString("secret history".getBytes(StandardCharsets.UTF_8));
		Files.write(f.toPath(), ("ob_9\t1\t0\t0\tapd.uimsd-history\t1\t0\t" + payload + "\t\n").getBytes(StandardCharsets.UTF_8));
		Outbox b = new Outbox();
		OutboxStore.Loaded l = OutboxStore.load(b, f);
		assertEquals(0, b.size());
		assertEquals(1, l.skipped);
	}

	@Test
	public void repeatedQueueIdsAreReportedAndCannotOverwriteTheFirstEntriesMetadata() throws Exception {
		File file = new File(tmp.getRoot(), "duplicate.txt");
		OutboxStore.save(sample(), file);
		List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
		String[] duplicate = lines.get(0).split("\t", -1);
		duplicate[2] = "99";
		Files.writeString(file.toPath(), String.join("\t", duplicate)+"\n", java.nio.file.StandardOpenOption.APPEND);
		Outbox box = new Outbox();
		OutboxStore.Loaded loaded = OutboxStore.load(box, file);
		assertEquals(2, loaded.items);
		assertEquals(1, loaded.skipped);
		assertEquals(3, box.items().get(0).attempts);
	}

	@Test
	public void invalidFirstLineDoesNotPreventLaterValidEntryWithTheSameId() throws Exception {
		File file = new File(tmp.getRoot(), "invalid-first.txt");
		OutboxStore.save(sample(), file);
		List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
		String[] damaged = lines.get(0).split("\t", -1);
		damaged[2] = "not a number";
		Files.writeString(file.toPath(), String.join("\t", damaged)+"\n"+String.join("\n", lines)+"\n");
		Outbox box = new Outbox();
		OutboxStore.Loaded loaded = OutboxStore.load(box, file);
		assertEquals(2, loaded.items);
		assertEquals(1, loaded.skipped);
		assertEquals(3, box.items().get(0).attempts);
	}

	@Test
	public void theOfflineGatewayChangesNothing(){
		Outbox box = sample();
		Outbox.FlushResult r = box.flush(new OfflineGateway(), 10 * Backoff.MAX_MILLIS);
		assertEquals(0, r.sent);
		assertEquals(2, box.size());
	}
}
