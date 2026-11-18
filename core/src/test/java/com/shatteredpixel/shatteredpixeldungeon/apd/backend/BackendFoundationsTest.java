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

public class BackendFoundationsTest {

	@Test
	public void idsAreTypedAndValidated(){
		Id p = Id.of(Id.Kind.PLAYER, "pl_abc");
		assertEquals(p, Id.of(Id.Kind.PLAYER, "pl_abc"));
		assertNotEquals("same text, different kind is a different id", p, Id.of(Id.Kind.RUN, "pl_abc"));
		for (String bad : new String[]{ "", "a b", "a\nb", "a/b", new String(new char[81]).replace('\0', 'x') }){
			try {
				Id.of(Id.Kind.RUN, bad);
				fail("accepted " + bad);
			} catch (IllegalArgumentException expected){}
		}
		Id r = Id.random(Id.Kind.PLAYER, new java.security.SecureRandom());
		assertTrue(r.value.startsWith("pl_") && r.value.length() == 3 + 32);
	}

	@Test
	public void canonicalBytesCannotBeConfused(){
		byte[] a = new Canon("t", 1).str("a").str("bc").toBytes();
		byte[] b = new Canon("t", 1).str("ab").str("c").toBytes();
		assertFalse(Arrays.equals(a, b));
		assertFalse("another record type never matches", Arrays.equals(new Canon("x", 1).str("a").toBytes(), new Canon("y", 1).str("a").toBytes()));
		assertFalse("another version never matches", Arrays.equals(new Canon("x", 1).str("a").toBytes(), new Canon("x", 2).str("a").toBytes()));
		assertEquals(new Canon("t", 1).num(5).sha256Hex(), new Canon("t", 1).num(5).sha256Hex());
		assertNotEquals(new Canon("t", 1).num(5).sha256Hex(), new Canon("t", 1).num(6).sha256Hex());
	}

	@Test
	public void envelopeVersionRules(){
		Envelope e = new Envelope("s", 2, 3, "");
		assertEquals(Envelope.Compat.SAME, e.compatibleWith(2, 3));
		assertEquals("a newer minor is readable: unknown fields are skipped", Envelope.Compat.NEWER_MINOR_READABLE, e.compatibleWith(2, 1));
		assertEquals(Envelope.Compat.OLDER_MINOR_READABLE, e.compatibleWith(2, 9));
		assertEquals("a different major is a different contract", Envelope.Compat.UNSUPPORTED_MAJOR, e.compatibleWith(1, 3));
	}

	@Test
	public void trustLevelsOnlyClaimUpward(){
		assertTrue(TrustLevel.REPLAY_VERIFIED.satisfies(TrustLevel.SERVER_CHECKED));
		assertFalse(TrustLevel.CLIENT_CLAIM.satisfies(TrustLevel.SERVER_RECEIVED));
		assertFalse("a developer-issued record is a different axis", TrustLevel.REPLAY_VERIFIED.satisfies(TrustLevel.DEVELOPER_ISSUED));
		assertTrue(TrustLevel.DEVELOPER_ISSUED.satisfies(TrustLevel.DEVELOPER_ISSUED));
	}

	@Test
	public void everySchemaThatCanLeaveIsClassifiedAndRawHistoryStaysLocal(){
		assertEquals(DataPolicy.DataClass.LOCAL_ONLY, DataPolicy.classOf("apd.run-log"));
		assertEquals(DataPolicy.DataClass.LOCAL_ONLY, DataPolicy.classOf("apd.uimsd-history"));
		assertEquals(DataPolicy.DataClass.LOCAL_ONLY, DataPolicy.classOf("apd.floor-replay"));
		assertFalse(DataPolicy.mayLeaveDevice("apd.uimsd-history"));
		assertFalse("an unlisted schema cannot be sent", DataPolicy.mayLeaveDevice("apd.something-new"));
		assertTrue(DataPolicy.mayLeaveDevice(RunSubmission.SCHEMA));
		Outbox box = new Outbox();
		try {
			box.enqueue(Id.of(Id.Kind.OUTBOX, "ob_1"), new Envelope("apd.uimsd-history", 1, 0, "x"), 0);
			fail("local-only data was queued");
		} catch (IllegalArgumentException expected){}
		assertEquals(0, box.size());
	}

	//The shipped backend code must be unable to create authority, and must not depend on gameplay (it is a boundary, not part of the dungeon).
	@Test
	public void architectureBoundaries() throws Exception {
		File root = sourceRoot();
		File main = new File(root, "apd/backend");
		assertTrue("the backend sources are found", main.isDirectory());
		for (File f : main.listFiles()){
			String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
			for (String forbidden : new String[]{ "PrivateKey", "KeyPairGenerator", "initSign", "PKCS8", "SecretKey", "Mac.getInstance" }){
				assertFalse(f.getName() + " must not be able to sign or hold secrets: " + forbidden, src.contains(forbidden));
			}
			for (String line : src.split("\n")){
				if (line.startsWith("import ")){
					assertTrue(f.getName() + " may only import java.*: " + line, line.startsWith("import java."));
				}
			}
		}
		//nothing outside the backend package uses it yet except through the Gateway door; gameplay code must never reach grants or content approval
		List<String> users = new ArrayList<>();
		scan(root, new File[]{ main, new File(root, "apd/backendbridge") }, users);
		//Game-side entry points, all through the bridge and never the backend package itself: the developer-preview "queue for submission" actions in the run sheet, and the read-only "what stays on this device" summary in the table options.
		users.remove("WndUimsdRow.java");
		users.remove("WndUimsdOptions.java");
		assertTrue("game code must not use the backend package directly: " + users, users.isEmpty());
	}

	//the tests run from core/src/main/assets (see core/build.gradle), other tools from core/: look in both and fail loudly if neither has the sources
	private static File sourceRoot(){
		String rel = "com/shatteredpixel/shatteredpixeldungeon";
		for (String base : new String[]{ "src/main/java/", "../java/" }){
			File f = new File(base + rel);
			if (f.isDirectory()) return f;
		}
		throw new AssertionError("cannot find the core sources from " + new File(".").getAbsolutePath());
	}

	private static void scan( File dir, File[] skip, List<String> hits ) throws Exception {
		for (File f : dir.listFiles()){
			if (Arrays.asList(skip).contains(f)) continue;
			if (f.isDirectory()) scan(f, skip, hits);
			else if (f.getName().endsWith(".java")){
				String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
				if (src.contains("apd.backend")) hits.add(f.getName());
				if (f.getName().equals("WndUimsdRow.java")) assertFalse("the run sheet talks to the bridge only", src.contains("apd.backend."));
			}
		}
	}
}
