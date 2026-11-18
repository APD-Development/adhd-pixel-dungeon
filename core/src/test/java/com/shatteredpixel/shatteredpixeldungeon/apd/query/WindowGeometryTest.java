package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

//UIMSD's dialogs must fit and line up on any screen, in either orientation. Found on a phone in build 22: the Views list sat far to the
//right of its window. The cause was placing the scroll pane before the window was resized (resizing moves the window's camera), which
//is checked at the source level below, and the sizes are checked here for a spread of screen sizes.
public class WindowGeometryTest {

	private static final float[] PORTRAIT_SCREENS = { 100, 118, 135, 150, 180, 240, 360 };
	private static final float[] LANDSCAPE_SCREENS = { 200, 240, 300, 360, 480, 640, 1000 };

	@Test
	public void theWindowIsAlwaysNarrowerThanTheScreenAndWithinItsPreferredWidth(){
		for (boolean landscape : new boolean[]{false, true}){
			for (float screen : landscape ? LANDSCAPE_SCREENS : PORTRAIT_SCREENS){
				int w = WindowGeometry.width(landscape, screen);
				assertTrue("screen " + screen + " landscape " + landscape, w > 0);
				assertTrue(w <= (landscape ? WindowGeometry.LANDSCAPE_WIDTH : WindowGeometry.PORTRAIT_WIDTH));
				assertTrue("window " + w + " must leave a margin on a screen " + screen, w + 2 * WindowGeometry.SIDE_MARGIN <= screen);
				if (screen >= WindowGeometry.MIN_WIDTH + 2 * WindowGeometry.SIDE_MARGIN){
					assertTrue(w >= WindowGeometry.MIN_WIDTH);
				}
			}
		}
	}

	@Test
	public void aWiderScreenNeverGivesANarrowerWindow(){
		int last = 0;
		for (float screen = 100; screen <= 1000; screen += 5){
			int w = WindowGeometry.width(false, screen);
			assertTrue(w >= last);
			last = w;
		}
	}

	@Test
	public void theListIsAsTallAsItsContentUntilItReachesMostOfTheScreenThenItScrolls(){
		float screen = 225;
		float max = WindowGeometry.maxPane(screen, 30, 0);
		assertEquals(screen * WindowGeometry.MAX_HEIGHT_SHARE - 30, max, 0.01f);
		assertEquals(60f, WindowGeometry.paneHeight(60, max), 0.01f);
		assertFalse(WindowGeometry.scrolls(60, max));
		assertEquals(max, WindowGeometry.paneHeight(10000, max), 0.01f);
		assertTrue(WindowGeometry.scrolls(10000, max));
		//with a very short screen the list still keeps a usable height
		assertEquals(WindowGeometry.MIN_PANE, WindowGeometry.maxPane(60, 40, 20), 0.01f);
		//a landscape phone (short and wide)
		float landscape = WindowGeometry.maxPane(160, 24, 0);
		assertTrue(landscape + 24 < 160);
	}

	@Test
	public void rowsStackWithoutGapsOrOverlapsAndEachTapFindsItsRow(){
		float[] heights = { 15, 31, 15, 47, 15 };
		float[] tops = WindowGeometry.stack(heights);
		assertEquals(0, tops[0], 0);
		for (int i = 0; i < heights.length; i++) assertEquals(tops[i] + heights[i], tops[i + 1], 0.0001f);
		assertEquals(123, tops[heights.length], 0.0001f);
		for (int i = 0; i < heights.length; i++){
			assertEquals(i, WindowGeometry.rowAt(tops, tops[i] + 0.5f));
			assertEquals(i, WindowGeometry.rowAt(tops, tops[i + 1] - 0.5f));
		}
		assertEquals(-1, WindowGeometry.rowAt(tops, -1));
		assertEquals(-1, WindowGeometry.rowAt(tops, 123));
	}

	@Test
	public void rowsGrowWithTheirTextAndTextStaysInsideTheWindow(){
		assertEquals(WindowGeometry.MIN_ROW, WindowGeometry.rowHeight(7), 0);
		assertTrue(WindowGeometry.rowHeight(40) >= 40);
		for (float screen : PORTRAIT_SCREENS){
			int w = WindowGeometry.width(false, screen);
			int text = WindowGeometry.rowTextWidth(w);
			assertTrue(text >= 60);
			assertTrue("text must start and end inside the window", WindowGeometry.ROW_PAD + 1 + text <= w);
		}
	}

	//the longest real entries of the Views list, on the narrowest layouts, wrap into several lines and still add up to a list that
	//scrolls when it is too tall
	@Test
	public void theViewsListFitsOrScrollsOnEveryScreen(){
		float charWidth = 4.8f, lineHeight = 8f;
		for (boolean landscape : new boolean[]{false, true}){
			for (float screen : landscape ? LANDSCAPE_SCREENS : PORTRAIT_SCREENS){
				int w = WindowGeometry.width(landscape, screen);
				int textWidth = WindowGeometry.rowTextWidth(w);
				java.util.List<ViewDef> views = Datasets.allViews();
				float[] heights = new float[views.size()];
				for (int i = 0; i < heights.length; i++){
					String label = views.get(i).title + " - " + views.get(i).description;
					int lines = (int) Math.ceil(label.length() * charWidth / textWidth);
					heights[i] = WindowGeometry.rowHeight(lines * lineHeight);
				}
				float total = WindowGeometry.stack(heights)[heights.length];
				float screenHeight = landscape ? 160 : 225;
				float max = WindowGeometry.maxPane(screenHeight, 14, 0);
				float pane = WindowGeometry.paneHeight(total, max);
				assertTrue(pane <= max + 0.001f);
				assertTrue("the dialog must fit on the screen", 14 + pane <= screenHeight);
				assertTrue(WindowGeometry.scrolls(total, max) == (total > max));
			}
		}
	}

	private static String src( String relative ) throws Exception {
		return new String(Files.readAllBytes(new File(System.getProperty("apd.game.src"), relative).toPath()), "UTF-8");
	}

	@Test
	public void wrappedActionsKeepPaddingAndUsableTouchTargets(){
		for (float textHeight : new float[]{0, 9, 18, 27, 100}){
			float height = WindowGeometry.actionHeight(textHeight);
			assertTrue(height >= 22);
			assertTrue(height >= textHeight + 2 * WindowGeometry.ROW_PAD);
		}
	}

	@Test
	public void runActionsWrapBeforeMeasuringAndScrollWithTheDetails() throws Exception {
		String text = src("windows/WndUimsdRow.java");
		assertTrue(text.indexOf("label.maxWidth(") < text.indexOf("WindowGeometry.actionHeight(label.height())"));
		assertTrue(text.contains("content.add(label)"));
		assertTrue(text.contains("actionSpans.add("));
		assertFalse(text.contains("actions.size() * 18"));
		assertFalse(text.contains("b.setRect(0, pos, width, 16)"));
	}

	//The bug itself: a window with a scroll pane must be resized BEFORE the pane is given its rectangle.
	@Test
	public void everyUimsdWindowWithAScrollPaneIsResizedBeforeThePaneIsPlaced() throws Exception {
		String[] windows = { "windows/WndUimsdList.java", "windows/WndUimsdText.java", "windows/WndUimsdRow.java" };
		for (String w : windows){
			String text = src(w);
			assertTrue(w + " has a scroll pane", text.contains("ScrollPane") || text.contains("ListPane"));
			int resize = text.indexOf("resize(width");
			int place = text.indexOf("pane.setRect(");
			assertTrue(w + " must call resize(width, ...)", resize > 0);
			assertTrue(w + " must place the pane", place > 0);
			assertTrue(w + " places the scroll pane before resizing the window: its contents would end up offset", resize < place);
			//and nothing places the pane a second time before the resize
			assertEquals(w, text.lastIndexOf("pane.setRect("), place);
		}
	}

	@Test
	public void noUimsdWindowUsesAFixedPixelWidth() throws Exception {
		for (String w : new String[]{"windows/WndUimsdList.java", "windows/WndUimsdText.java", "windows/WndUimsdRow.java", "windows/WndUimsdOptions.java"}){
			String text = src(w);
			assertFalse(w + " hard-codes a width", text.contains("WIDTH_P") || text.contains("WIDTH_L") || text.matches("(?s).*int width = \\d+;.*"));
		}
		assertTrue(src("windows/WndUimsdList.java").contains("WindowGeometry.width("));
	}

	@Test
	public void emptyAndExactRowBoundariesDoNotInventTouches(){
		float[] empty = WindowGeometry.stack(new float[0]);
		assertEquals(1, empty.length);
		assertEquals(0f, empty[0], 0f);
		assertEquals(-1, WindowGeometry.rowAt(empty, 0));

		float[] tops = WindowGeometry.stack(new float[]{15, 31});
		assertEquals(0, WindowGeometry.rowAt(tops, 0));
		assertEquals(1, WindowGeometry.rowAt(tops, 15));
		assertEquals(-1, WindowGeometry.rowAt(tops, 46));
	}

}
