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

package com.shatteredpixel.shatteredpixeldungeon.apd.backendbridge;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.backend.Id;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SessionStoreTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Id ANN = Id.of(Id.Kind.PLAYER, "pl_ann");

	@Test
	public void aSessionIsKeptUntilItExpiresOrThePlayerSignsOut(){
		SessionStore s = new SessionStore(tmp.getRoot());
		assertNull("nobody is signed in at first", s.current(0));
		assertNull(s.tokenSource().token());
		assertTrue(s.signIn(ANN, "tok-1", 1000));
		SessionStore.Session cur = new SessionStore(tmp.getRoot()).current(500);
		assertNotNull("it survives a restart", cur);
		assertEquals(ANN, cur.player);
		assertEquals("tok-1", cur.token);
		assertNull("expired: gone, and removed", s.current(1000));
		assertFalse(new java.io.File(tmp.getRoot(), SessionStore.FILE_NAME).exists());
		assertTrue(s.signIn(ANN, "tok-2", System.currentTimeMillis() + 60_000));
		assertEquals("tok-2", s.tokenSource().token());
		s.signOut();
		assertNull(s.tokenSource().token());
		s.signOut();   //nothing to remove is fine
	}

	@Test
	public void unusableSessionsAreRefusedAndNeverWritten(){
		SessionStore s = new SessionStore(tmp.getRoot());
		assertFalse(s.signIn(null, "t", 5));
		assertFalse(s.signIn(Id.of(Id.Kind.DEVICE, "dv_1"), "t", 5));
		assertFalse(s.signIn(ANN, "", 5));
		assertFalse(s.signIn(ANN, "two\nlines", 5));
		assertFalse(new java.io.File(tmp.getRoot(), SessionStore.FILE_NAME).exists());
	}
}
