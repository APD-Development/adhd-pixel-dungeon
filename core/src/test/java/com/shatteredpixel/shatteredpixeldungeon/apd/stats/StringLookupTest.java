package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** The two lookup contracts: unknown ids answer null, and positioned lookups hear where each reference ended. */
public class StringLookupTest {
	@Test public void unknownIdsAnswerNull(){
		String[] table = {"rat", "gnoll"};
		StringLookup lookup = id -> id >= 0 && id < table.length ? table[id] : null;
		assertEquals("gnoll", lookup.get(1));
		assertNull(lookup.get(5));
		assertNull(lookup.get(-1));
	}

	@Test public void positionedLookupIsAlsoAPlainLookup(){
		List<Integer> ends = new ArrayList<>();
		PositionedStringLookup lookup = new PositionedStringLookup(){
			@Override public String get(int id){ return "s" + id; }
			@Override public String get(int id, int endPosition){ ends.add(endPosition); return get(id); }
		};
		StringLookup plain = lookup;
		assertEquals("s3", plain.get(3));
		assertEquals("s4", lookup.get(4, 17));
		assertEquals(17, (int) ends.get(0));
	}
}
