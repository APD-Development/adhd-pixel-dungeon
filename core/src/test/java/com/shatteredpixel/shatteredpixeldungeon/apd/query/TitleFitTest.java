package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;

import org.junit.Test;

//The UIMSD header shows the full name and fits beside the exit button on every supported screen width.
public class TitleFitTest {

	@Test
	public void theHeaderIsTheFullName(){
		assertEquals("Ultimate Infinite Mega Statistics Database", UimsdScene.FULL_NAME);
	}

	//the room left of the exit button, as the scene works it out: content width minus the part of the exit button that overlaps it
	private static float room( float screenWidth, boolean landscape ){
		float margin = 4, exit = landscape ? 40 : 20;
		float contentW = screenWidth - 2 * margin;
		return contentW - Math.max(0, exit - margin) - 2;
	}

	@Test
	public void itFitsInTwoLinesAcrossPortraitAndLandscapePhoneWidths(){
		//the game keeps its camera at least 135 units wide in portrait and 240 in landscape (PixelScene), and phones reach about 200 and 420
		for (int w = 135; w <= 200; w += 5){
			float room = room(w, false);
			int size = TitleFit.size(room, UimsdScene.FULL_NAME);
			assertTrue("portrait " + w + " size " + size, TitleFit.lines(room, UimsdScene.FULL_NAME, size) <= TitleFit.MAX_LINES);
			assertTrue(size >= 6);
		}
		for (int w = 240; w <= 420; w += 10){
			float room = room(w, true);
			int size = TitleFit.size(room, UimsdScene.FULL_NAME);
			assertTrue("landscape " + w + " size " + size, TitleFit.lines(room, UimsdScene.FULL_NAME, size) <= TitleFit.MAX_LINES);
		}
	}

	@Test
	public void wideScreensGetLargerTextAndNarrowOnesNeverGetLarger(){
		int last = 0;
		for (int w = 100; w <= 600; w += 10){
			int size = TitleFit.size(room(w, false), UimsdScene.FULL_NAME);
			assertTrue("text got smaller as the screen got wider at " + w, size >= last);
			last = size;
		}
		assertEquals(10, TitleFit.size(1000, UimsdScene.FULL_NAME));
	}

	@Test
	public void aWordIsNeverSplitAndAnImpossiblyNarrowScreenStillGivesAnAnswer(){
		//"Statistics" is the longest word (10 characters): in a width smaller than that it has a line to itself
		assertTrue(TitleFit.lines(20, UimsdScene.FULL_NAME, 6) >= 5);
		assertEquals(6, TitleFit.size(20, UimsdScene.FULL_NAME));
	}

	@Test
	public void theShortNameStaysTheShortNameElsewhere(){
		//the window titles and buttons keep UIMSD: this class only owns the header
		assertTrue(UimsdScene.FULL_NAME.length() > "UIMSD".length());
	}
}
