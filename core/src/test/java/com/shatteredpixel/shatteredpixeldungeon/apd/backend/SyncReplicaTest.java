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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SyncReplicaTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private static SyncRecord remote( String key, long version, long at, String device, boolean deleted, String value ){
		return new SyncRecord("settings", key, version, at, device, deleted, value);
	}

	@Test
	public void aLocalEditIsPendingUntilTheServerAcknowledgesIt(){
		SyncReplica r = new SyncReplica("dv_a");
		r.put("settings", "theme", "dark", 100);
		assertEquals("dark", r.get("settings", "theme"));
		assertEquals(1, r.pending().size());
		r.acknowledge(r.pending().get(0), 7);
		assertTrue(r.pending().isEmpty());
		assertEquals("dark", r.get("settings", "theme"));
	}

	@Test
	public void onlySyncableCollectionsAndSmallValuesAreAccepted(){
		SyncReplica r = new SyncReplica("dv_a");
		for (final String[] bad : new String[][]{ { "apd.run-log", "x" }, { "settings", "" } }){
			try {
				r.put(bad[0], bad[1], "v", 1);
				fail("accepted " + bad[0]);
			} catch (IllegalArgumentException expected){
			}
		}
		char[] big = new char[SyncReplica.MAX_VALUE_CHARS + 1];
		Arrays.fill(big, 'x');
		try {
			r.put("settings", "k", new String(big), 1);
			fail("large value accepted");
		} catch (IllegalArgumentException expected){
		}
		//a hostile server cannot plant a record in a collection that does not sync, or an oversized one
		assertTrue(r.applyRemote(Arrays.asList(new SyncRecord("apd.run-log", "x", 1, 1, "dv_b", false, "v"), remote("k", 1, 1, "dv_b", false, new String(big)))).isEmpty());
	}

	@Test
	public void aNewerRemoteChangeReplacesACleanRecordAndApplyingTwiceChangesNothing(){
		SyncReplica r = new SyncReplica("dv_a");
		List<SyncReplica.Change> first = r.applyRemote(Collections.singletonList(remote("theme", 3, 100, "dv_b", false, "light")));
		assertEquals(1, first.size());
		assertEquals("light", r.get("settings", "theme"));
		assertTrue(r.applyRemote(Collections.singletonList(remote("theme", 3, 100, "dv_b", false, "light"))).isEmpty());
		//an older version arriving late is ignored
		assertTrue(r.applyRemote(Collections.singletonList(remote("theme", 2, 999, "dv_b", false, "stale"))).isEmpty());
		assertEquals("light", r.get("settings", "theme"));
		assertEquals(1, r.applyRemote(Collections.singletonList(remote("theme", 4, 200, "dv_b", false, "dark"))).size());
	}

	@Test
	public void aDeleteTravelsAsATombstoneBothWays(){
		SyncReplica a = new SyncReplica("dv_a");
		a.applyRemote(Collections.singletonList(remote("view1", 1, 100, "dv_b", false, "q")));
		a.delete("settings", "view1", 200);
		assertNull(a.get("settings", "view1"));
		SyncRecord t = a.pending().get(0);
		assertTrue(t.deleted);
		a.acknowledge(t, 2);
		//another device receives the tombstone and removes its copy
		SyncReplica b = new SyncReplica("dv_c");
		b.applyRemote(Collections.singletonList(remote("view1", 1, 100, "dv_b", false, "q")));
		List<SyncReplica.Change> ch = b.applyRemote(Collections.singletonList(remote("view1", 2, 200, "dv_a", true, "")));
		assertEquals(1, ch.size());
		assertTrue(ch.get(0).deleted);
		assertNull(b.get("settings", "view1"));
	}

	@Test
	public void whenBothSidesChangedTheLaterEditWinsAndEveryDeviceAgrees(){
		//remote edit is later: it wins and our edit is dropped
		SyncReplica a = new SyncReplica("dv_a");
		a.applyRemote(Collections.singletonList(remote("k", 1, 10, "dv_b", false, "base")));
		a.put("settings", "k", "mine", 100);
		List<SyncReplica.Change> ch = a.applyRemote(Collections.singletonList(remote("k", 2, 150, "dv_b", false, "theirs")));
		assertEquals(1, ch.size());
		assertEquals("theirs", a.get("settings", "k"));
		assertTrue(a.pending().isEmpty());
		//our edit is later: it is kept, still pending, now based on the newer version
		SyncReplica b = new SyncReplica("dv_a");
		b.applyRemote(Collections.singletonList(remote("k", 1, 10, "dv_b", false, "base")));
		b.put("settings", "k", "mine", 300);
		assertTrue(b.applyRemote(Collections.singletonList(remote("k", 2, 150, "dv_b", false, "theirs"))).isEmpty());
		assertEquals("mine", b.get("settings", "k"));
		assertEquals(2, b.pending().get(0).serverVersion);
		//an exact time tie goes to the larger device id on both devices
		SyncReplica x = new SyncReplica("dv_a"), y = new SyncReplica("dv_z");
		x.put("settings", "t", "from-a", 500);
		y.put("settings", "t", "from-z", 500);
		x.applyRemote(Collections.singletonList(new SyncRecord("settings", "t", 1, 500, "dv_z", false, "from-z")));
		y.applyRemote(Collections.singletonList(new SyncRecord("settings", "t", 1, 500, "dv_a", false, "from-a")));
		assertEquals("from-z", x.get("settings", "t"));
		assertEquals("from-z", y.get("settings", "t"));
	}

	@Test
	public void anEditMadeAfterAnUploadWasSentIsNotLostByTheAcknowledgement(){
		SyncReplica r = new SyncReplica("dv_a");
		r.put("settings", "k", "one", 100);
		SyncRecord sent = r.pending().get(0);
		r.put("settings", "k", "two", 200);
		r.acknowledge(sent, 5);
		assertEquals(1, r.pending().size());
		assertEquals("two", r.get("settings", "k"));
	}

	@Test
	public void oldCleanTombstonesArePurgedButPendingOnesAreNot(){
		SyncReplica r = new SyncReplica("dv_a");
		r.applyRemote(Arrays.asList(remote("gone", 1, 100, "dv_b", true, ""), remote("keep", 1, 100, "dv_b", false, "v")));
		r.put("settings", "pendingDelete", "x", 100);
		r.delete("settings", "pendingDelete", 110);
		assertEquals(1, r.purgeTombstones(10_000, 1_000));
		assertEquals("v", r.get("settings", "keep"));
		assertEquals(1, r.pending().size());
	}

	@Test
	public void theReplicaSurvivesARestartAndIgnoresDamagedLines() throws Exception {
		File f = new File(tmp.getRoot(), "sync.txt");
		SyncReplica r = new SyncReplica("dv_a");
		r.applyRemote(Collections.singletonList(remote("a", 4, 100, "dv_b", false, "tab\there\nnewline")));
		r.put("settings", "b", "pending", 200);
		r.save(f);
		Files.write(f.toPath(), (new String(Files.readAllBytes(f.toPath()), "UTF-8") + "garbage\n").getBytes("UTF-8"));
		SyncReplica back = new SyncReplica("dv_a");
		assertEquals(1, back.load(f));
		assertEquals("tab\there\nnewline", back.get("settings", "a"));
		assertEquals("pending", back.get("settings", "b"));
		assertEquals(1, back.pending().size());
	}
}
