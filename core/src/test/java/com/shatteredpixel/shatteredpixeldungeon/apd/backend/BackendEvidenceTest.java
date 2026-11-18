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

public class BackendEvidenceTest {

	private static final String D = Canon.hex(Canon.sha256("log".getBytes()));
	private static final Evidence.Request R = new Evidence.Request(Id.of(Id.Kind.SUBMISSION, "sub_1"), Evidence.Kind.FULL_RUN_LOG, 1000);

	@Test
	public void evidenceIsOnlyProvidedWithConsentBeforeTheDeadlineForTheSameLog(){
		assertEquals(Evidence.Decision.NEEDS_CONSENT, Evidence.decide(R, false, 500, D, D));
		assertEquals(Evidence.Decision.PROVIDE, Evidence.decide(R, true, 500, D, D));
		assertEquals(Evidence.Decision.EXPIRED, Evidence.decide(R, true, 1001, D, D));
		assertEquals("an edited log is never provided", Evidence.Decision.LOG_CHANGED, Evidence.decide(R, true, 500, Canon.hex(Canon.sha256("other".getBytes())), D));
		assertEquals(Evidence.Decision.LOG_CHANGED, Evidence.decide(R, true, 500, null, D));
	}

	@Test
	public void theServerChecksWhatItReceivedAgainstTheSubmittedDigest(){
		assertTrue(Evidence.matchesSubmission("log".getBytes(), D));
		assertFalse(Evidence.matchesSubmission("log2".getBytes(), D));
	}
}
