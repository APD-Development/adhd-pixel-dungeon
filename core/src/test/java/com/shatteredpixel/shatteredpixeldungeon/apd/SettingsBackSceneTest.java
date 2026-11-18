package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertEquals;

import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings;

import org.junit.Test;

/** Back from ordinary Settings must not unconditionally rebuild the current scene. */
public class SettingsBackSceneTest {

	@Test
	public void ordinarySettingsInheritsDirectWindowHideInsteadOfResettingTheScene() throws Exception {
		assertEquals(Window.class, WndSettings.class.getMethod("hide").getDeclaringClass());
	}
}
