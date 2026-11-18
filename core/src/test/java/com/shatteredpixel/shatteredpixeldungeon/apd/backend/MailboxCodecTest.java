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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class MailboxCodecTest {

	@Test
	public void envelopesRoundTripAndADamagedItemIsSkipped(){
		List<Envelope> in = Arrays.asList(new Envelope("apd.evidence-request", 1, 1, "submission=sub_1\nkind=FULL_RUN_LOG\n"), new Envelope("apd.other", 2, 0, "a=b\n"));
		List<Envelope> out = MailboxCodec.decode(MailboxCodec.encode(in));
		assertEquals(2, out.size());
		assertEquals("apd.evidence-request", out.get(0).schema);
		assertEquals(1, out.get(0).minor);
		assertEquals("submission=sub_1\nkind=FULL_RUN_LOG\n", out.get(0).payload);
		assertEquals(2, out.get(1).major);

		String broken = "garbage\nx=y\n--\n" + MailboxCodec.encode(in.subList(1, 2));
		List<Envelope> partial = MailboxCodec.decode(broken);
		assertEquals("the damaged first item is skipped", 1, partial.size());
		assertEquals("apd.other", partial.get(0).schema);
		assertEquals(0, MailboxCodec.decode("").size());
		assertEquals(0, MailboxCodec.decode(null).size());
		assertEquals("an item that never closes is dropped", 0, MailboxCodec.decode("apd.x 1.0\na=b\n").size());
		try {
			MailboxCodec.encode(Arrays.asList(new Envelope("apd.x", 1, 0, "a=b\n--\nc=d\n")));
			fail("a payload line equal to the separator would split the item");
		} catch (IllegalArgumentException expected){
		}
	}
}
