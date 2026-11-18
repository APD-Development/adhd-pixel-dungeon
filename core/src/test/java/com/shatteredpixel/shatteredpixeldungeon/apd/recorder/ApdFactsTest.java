package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import static org.junit.Assert.*;

import org.junit.Test;

/** With no run being recorded, recording a fact is a safe no-op. */
public class ApdFactsTest {
	@Test public void inactiveRecorderIgnoresFacts(){
		boolean was = ApdFacts.active;
		ApdFacts.active = false;
		try {
			assertFalse(ApdFacts.isActive());
			ApdFacts.record(null, "GuidingLight", 1, 3);
			ApdFacts.record(null, "GuidingLight", "Rat", new long[]{1}, new String[]{"x"});
		} finally { ApdFacts.active = was; }
	}
}
