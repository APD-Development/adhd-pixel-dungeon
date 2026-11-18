package com.shatteredpixel.shatteredpixeldungeon.apd.stats;

import org.junit.Test;

/** Registering the built-in fact kinds is safe to repeat (every run start does it). */
public class BuiltinFactKindsTest {
	@Test public void registerIsRepeatable(){
		BuiltinFactKinds.register();
		BuiltinFactKinds.register();
	}
}
