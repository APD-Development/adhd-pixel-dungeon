package com.shatteredpixel.shatteredpixeldungeon.apd.age;

import static org.junit.Assert.*;

import com.shatteredpixel.shatteredpixeldungeon.apd.age.CommunityAgeGate.PlaySignal;
import com.shatteredpixel.shatteredpixeldungeon.apd.age.CommunityAgeGate.Result;

import org.junit.Test;

/** Community's 16+ gate: Play first, then birth month/year stored only as pass/fail, with no retry. */
public class CommunityAgeGateTest {
	static final class MemStore implements CommunityAgeGate.Store {
		String value; int writes;
		public String get(){ return value; }
		public void put(String v){ value = v; writes++; }
	}

	@Test public void playDecidesWhenItKnows(){
		MemStore s = new MemStore();
		assertEquals(Result.UNLOCKED, CommunityAgeGate.check(new PlaySignal(16, 17), s));
		assertEquals(Result.UNLOCKED, CommunityAgeGate.check(new PlaySignal(18, null), s));
		assertEquals(Result.LOCKED, CommunityAgeGate.check(new PlaySignal(13, 15), s));
		assertEquals(Result.LOCKED, CommunityAgeGate.check(new PlaySignal(0, 12), s));
		assertNull("Play's answer is never stored", s.value);
	}

	@Test public void noPlayAnswerAsksForBirthDate(){
		MemStore s = new MemStore();
		assertEquals(Result.ASK_BIRTH_DATE, CommunityAgeGate.check(PlaySignal.none(), s));
		assertEquals(Result.ASK_BIRTH_DATE, CommunityAgeGate.check(null, s));
	}

	@Test public void birthDateStoresOnlyPassOrFail_andCantBeRetried(){
		MemStore s = new MemStore();
		assertEquals(Result.LOCKED, CommunityAgeGate.answer(6, 2012, 11, 2026, s));
		assertEquals("fail", s.value);
		assertEquals("a different year doesn't get a second try", Result.LOCKED, CommunityAgeGate.answer(1, 1990, 11, 2026, s));
		assertEquals(1, s.writes);
		assertEquals(Result.LOCKED, CommunityAgeGate.check(PlaySignal.none(), s));

		MemStore ok = new MemStore();
		assertEquals(Result.UNLOCKED, CommunityAgeGate.answer(3, 1995, 11, 2026, ok));
		assertEquals("pass", ok.value);
		assertEquals(Result.UNLOCKED, CommunityAgeGate.check(PlaySignal.none(), ok));
	}

	@Test public void sixteenthBirthdayEdge_currentMonthCountsAsNotYet(){
		assertEquals(Result.UNLOCKED, CommunityAgeGate.answer(10, 2010, 11, 2026, new MemStore()));  // turned 16 last month
		assertEquals(Result.LOCKED, CommunityAgeGate.answer(11, 2010, 11, 2026, new MemStore()));    // turns 16 this month
		assertEquals(Result.LOCKED, CommunityAgeGate.answer(12, 2010, 11, 2026, new MemStore()));    // next month
		assertEquals(Result.UNLOCKED, CommunityAgeGate.answer(12, 2009, 11, 2026, new MemStore()));
	}

	@Test public void playOverridesTheStoredAnswerBothWays(){
		MemStore failed = new MemStore(); failed.value = "fail";
		assertEquals(Result.UNLOCKED, CommunityAgeGate.check(new PlaySignal(18, null), failed));
		MemStore passed = new MemStore(); passed.value = "pass";
		assertEquals(Result.LOCKED, CommunityAgeGate.check(new PlaySignal(13, 15), passed));
		assertEquals("pass", passed.value);
	}

	@Test public void nonsenseDatesAreRefusedAndNothingIsStored(){
		MemStore s = new MemStore();
		for (int[] bad : new int[][]{{0, 2000}, {13, 2000}, {5, 2027}, {5, 1800}}){
			try { CommunityAgeGate.answer(bad[0], bad[1], 11, 2026, s); fail(); } catch (IllegalArgumentException expected) {}
		}
		assertNull(s.value);
	}
}
