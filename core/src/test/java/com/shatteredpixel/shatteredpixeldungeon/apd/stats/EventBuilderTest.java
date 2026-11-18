package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import static org.junit.Assert.*;

import org.junit.Test;

/** A disabled writer hands out inert builders: every call is a no-op and nothing can throw into gameplay. */
public class EventBuilderTest {
	@Test public void inertBuilderAcceptsEveryCallAndEmitsNothing(){
		EventBuilder b = new EventBuilder(null, 7, true);
		assertSame(b, b.flags(3).clock(12).unsigned(5).unsigned(-1).signed(-9).bool(true)
				.f32(1.5f).f64(2.5).bytes(null).bytes(new byte[]{1}).string(null).string("rat"));
		assertFalse(b.emit());
		assertFalse("still false the second time", b.emit());
	}

	@Test public void manyStringsDoNotOverflowAnInertBuilder(){
		EventBuilder b = new EventBuilder(null, 1, true);
		for (int i = 0; i < 100; i++) b.string("s" + i);
		assertFalse(b.emit());
	}
}
