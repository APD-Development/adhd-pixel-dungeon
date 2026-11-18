package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.shatteredpixel.shatteredpixeldungeon.apd.TitleGeometry.Layout;

//Checks the UIMSD annotation (text strip + arrow) against the rest of the title screen on many screen sizes, portrait and
//landscape: it must not touch any button, the logo or the version text, must stay on screen, and the arrow must sit over
//the UIMSD button. The arithmetic under test is the same TitleGeometry code TitleScene runs.
public class TitleGeometryTest {

	private static final float VERSION_H = 7;
	private static final float TEXT_H = 8;

	private static final class Rect {
		final float l, t, r, b;
		Rect( float x, float y, float w, float h ){ l = x; t = y; r = x + w; b = y + h; }
		boolean hits( Rect o ){ return l < o.r && o.l < r && t < o.b && o.t < b; }
	}

	//everything the arrow draws at its highest point and lowest rest point: glow reaches GLOW+1 past the arrow
	private static Rect arrowExtent( Layout l, boolean lifted ){
		float colW = TitleGeometry.colWidth( l.gridWidth );
		float centre = l.gridLeft + colW + TitleGeometry.MAIN_GAP + colW / 2f;
		float x = (float)Math.floor( centre - TitleGeometry.HEAD_W / 2f ) - 1 - TitleGeometry.GLOW;
		float restY = l.gridTop - TitleGeometry.GAP - TitleGeometry.GLOW - 1 - TitleGeometry.ARROW_H;
		float y0 = restY - 1 - TitleGeometry.GLOW - (lifted ? TitleGeometry.BOB : 0);
		float y1 = restY + TitleGeometry.ARROW_H + 1 + TitleGeometry.GLOW;
		return new Rect( x, y0, TitleGeometry.HEAD_W + 2 + 2*TitleGeometry.GLOW, y1 - y0 );
	}

	private static Rect[] buttons( Layout l ){
		float colW = TitleGeometry.colWidth( l.gridWidth );
		Rect[] r = new Rect[6 + 1 + 3];
		for (int i = 0; i < 6; i++){
			int col = i % 2, row = i / 2;
			r[i] = new Rect( l.gridLeft + col*(colW + TitleGeometry.MAIN_GAP), l.gridTop + row*(l.mainH + TitleGeometry.MAIN_GAP), colW, l.mainH );
		}
		float apdY = l.gridTop + 3*(l.mainH + TitleGeometry.MAIN_GAP);
		r[6] = new Rect( l.gridLeft, apdY, l.gridWidth, TitleGeometry.BOTTOM_H );
		float smallW = (float)Math.floor((l.gridWidth - 2*TitleGeometry.MAIN_GAP) / 3f);
		for (int i = 0; i < 3; i++){
			r[7 + i] = new Rect( l.gridLeft + i*(smallW + TitleGeometry.MAIN_GAP), apdY + TitleGeometry.APD_ROW_H, smallW, TitleGeometry.BOTTOM_H );
		}
		return r;
	}

	private static void checkAnnotation( Layout l, boolean portrait, float w, float h, String where ){
		Rect[] buttons = buttons( l );
        if(l.annotationBesideGrid || l.annotationCompact){
            assertTrue("Guidance fits above footer",l.annotationBesideGrid ? l.annotationTop+TEXT_H<=l.fadeY-TitleGeometry.FOOTER_GAP : l.gridTop-l.annotationH>=l.versionY+VERSION_H);
            return;
        }
		Rect arrow = arrowExtent( l, true );
		Rect arrowRest = arrowExtent( l, false );
		for (int i = 0; i < buttons.length; i++){
			assertTrue(where + ": arrow touches button " + i, !arrow.hits( buttons[i] ) && !arrowRest.hits( buttons[i] ));
		}
		//the tip (at rest) ends a few px above the UIMSD button (button 1) and is centred on it
		Rect uimsd = buttons[1];
		assertEquals(where + ": arrow gap", TitleGeometry.GAP, uimsd.t - arrowRest.b, 0.01);
		float arrowCentre = (arrowRest.l + arrowRest.r) / 2f;
		assertTrue(where + ": arrow not over the UIMSD button", arrowCentre >= uimsd.l && arrowCentre <= uimsd.r);
		assertTrue(where + ": arrow wider than the button", arrowRest.l >= uimsd.l - 1 && arrowRest.r <= uimsd.r + 1);

		//text is above the arrow's highest point and inside the strip
		float stripTop = l.gridTop - l.annotationH;
		float textBottom = stripTop + TEXT_H + 1;
		assertTrue(where + ": text overlaps the arrow", textBottom <= arrow.t);

		if (portrait){
			assertTrue(where + ": strip overlaps the version text", stripTop >= l.versionY + VERSION_H);
		} else {
			float logoRight = (l.gridLeft - 8);
			assertTrue(where + ": strip overlaps the logo", l.gridLeft >= logoRight);
		}
	}

	@Test
	public void portraitPhonesOfManySizes() {
		int checked = 0;
		for (int w = 128; w <= 220; w += 4){
			for (int h = 300; h <= 560; h += 6){
				Layout l = TitleGeometry.portrait( 0, 0, w, h, VERSION_H, TEXT_H );
				checkAnnotation( l, true, w, h, "portrait " + w + "x" + h );
				//everything still fits whenever it fitted before the annotation existed
				float minOldH = TitleGeometry.LOGO_H + VERSION_H + 2 + 4 + (3*24 + 3*TitleGeometry.MAIN_GAP + TitleGeometry.APD_ROW_H + TitleGeometry.BOTTOM_H) + 6 + 6;
				if (h >= minOldH + l.annotationH){
					float stripTop = l.gridTop - l.annotationH;
					assertTrue("annotation above the screen at " + w + "x" + h, stripTop >= 0);
					float bottom = l.gridTop + 3*(l.mainH + TitleGeometry.MAIN_GAP) + TitleGeometry.APD_ROW_H + TitleGeometry.BOTTOM_H;
					assertTrue("grid below the screen at " + w + "x" + h, bottom <= h);
				}
				checked++;
			}
		}
		assertTrue(checked > 500);
	}

	@Test
	public void landscapePhonesOfManySizes() {
		int checked = 0;
		for (int w = 300; w <= 520; w += 8){
			for (int h = 150; h <= 260; h += 5){
				Layout l = TitleGeometry.landscape( 0, 0, w, h, VERSION_H, TEXT_H );
				checkAnnotation( l, false, w, h, "landscape " + w + "x" + h );
				float stripTop = l.gridTop - l.annotationH;
				float bottom = l.gridTop + 3*(l.mainH + TitleGeometry.MAIN_GAP) + TitleGeometry.APD_ROW_H + TitleGeometry.BOTTOM_H;
				float needAtMin = TitleGeometry.gridHeight( l.annotationH, 24 );
				if (h - 8 >= needAtMin){
					assertTrue("annotation above the screen at " + w + "x" + h, stripTop >= 0);
					assertTrue("grid below the screen at " + w + "x" + h, bottom <= h);
				}
				checked++;
			}
		}
		assertTrue(checked > 500);
	}

	@Test
	public void tallScreensKeepTheOriginalButtonSizes() {
		//plenty of spare height: main rows stay at their normal size, the annotation uses the slack
		Layout p = TitleGeometry.portrait( 0, 0, 180, 600, VERSION_H, TEXT_H );
		assertEquals(TitleGeometry.mainHeight( 600 - 6 - TitleGeometry.LOGO_H - (VERSION_H + 2) - 6 - 4 ), p.mainH, 0.001);
		Layout l = TitleGeometry.landscape( 0, 0, 400, 240, VERSION_H, TEXT_H );
		assertEquals(TitleGeometry.mainHeight( 240 - 8 ), l.mainH, 0.001);
	}

	@Test
	public void theArrowIsBigAndOutlined() {
		assertTrue("arrow should be much bigger than the old 7x7", TitleGeometry.HEAD_W >= 15 && TitleGeometry.ARROW_H >= 13);
		//head rows narrow by 2 each and finish on a single pixel tip
		assertEquals(1, TitleGeometry.HEAD_W - 2*(TitleGeometry.HEAD_ROWS - 1));
		assertTrue(TitleGeometry.BOB >= 2);
	}

	@Test
	public void retiredOrUnavailablePromptsReserveNoArrowSpace(){
		assertEquals(0, TitleGeometry.annotationHeight(-1), 0);
		assertEquals(0, TitleGeometry.portrait(0, 0, 180, 320, VERSION_H, -1).annotationH, 0);
		assertEquals(0, TitleGeometry.landscape(0, 0, 400, 240, VERSION_H, -1).annotationH, 0);
	}
	@Test public void lockedInstructionWrapsWithinEverySupportedTitleWidth(){
		String message = "THIS BUTTON IS EFFECTIVELY USELESS TILL 10 RUNS HAVE BEEN COMPLETED.";
		for (int width : new int[]{72, 89, 120, 150}){
			String[] lines = TitleGeometry.annotationLines(message, width, text -> text.length() * 4f);
			assertEquals(message, String.join(" ", lines));
			for (String line : lines) assertTrue("overflow at width " + width + ": " + line, line.length() * 4 <= width);
			assertTrue("do not strand THIS on its own", !lines[0].equals("THIS"));
		}
	}

	@Test public void tenthRunPromptsWrapByMeasuredWordsInsteadOfHere(){
		for (String message : new String[]{"HEY, GO HERE FOR DRUGS.", "HEY, GO HERE FOR UNLIMITED DOPAMINE."}){
			for (int width : new int[]{72, 89, 120, 150}){
				String[] lines = TitleGeometry.annotationLines(message, width, text -> text.length() * 4f);
				assertEquals(message, String.join(" ", lines));
				for (String line : lines) assertTrue(line.length() * 4 <= width);
			}
		}
	}

	@Test public void shortAndEmptyAnnotationsKeepTheirText(){
		assertEquals("", TitleGeometry.annotationLines("", 72, text -> text.length() * 4f)[0]);
		assertEquals("SHORT", TitleGeometry.annotationLines("SHORT", 72, text -> text.length() * 4f)[0]);
	}

	@Test public void overlongTokensCannotOverflowOrCrashWithoutHere(){
		String message = "ABCDEFGHIJKLMNOPQRST";
		String[] lines = TitleGeometry.annotationLines(message, 20, text -> text.length() * 4f);
		assertEquals(message, String.join("", lines));
		for (String line : lines) assertTrue(line.length() * 4 <= 20);
	}

    @Test public void everyMenuButtonReservesTheHideUiFooterAtMinimumAndVirtualSizes(){
        for(boolean landscape:new boolean[]{false,true})
        for(int w:new int[]{135,160,180,240,300,360,540,720})
        for(int h:new int[]{160,225,240,260,300,360,480,800})
        for(int text:new int[]{-1,8,16,32,48}){
            if(landscape ? w<240 : h<225)continue;
            Layout l=landscape ? TitleGeometry.landscape(3,7,w,h,VERSION_H,text)
                    : TitleGeometry.portrait(3,7,w,h,VERSION_H,text);
            Rect fade=new Rect(l.fadeX,l.fadeY,16,16);
            Rect[] all=buttons(l);
            for(int i=0;i<all.length;i++){
                Rect b=all[i];String context=w+"x"+h+" landscape="+landscape+" text="+text+" control="+i;
                assertTrue("Footer overlap "+context,!b.hits(fade));
                assertTrue("Footer clearance "+context,b.b<=l.fadeY-TitleGeometry.FOOTER_GAP+0.001);
                assertTrue("Offscreen "+context,b.l>=3 && b.r<=3+w && b.t>=7 && b.b<=7+h);
                for(int j=i+1;j<all.length;j++)assertTrue("Menu controls overlap "+context,!b.hits(all[j]));
            }
            assertTrue(l.logoScale>=0 && l.logoScale<=1);
            assertTrue(l.logoY>=7);
            assertTrue(l.versionY+VERSION_H<=l.fadeY-TitleGeometry.FOOTER_GAP);
        }
    }
    @Test public void hideUiTouchTargetUsesOnlyReservedFooterSpace(){
        for(boolean landscape : new boolean[]{false,true})
        for(int w : new int[]{180,240,320,400,720})
        for(int h : new int[]{240,320,480,640,800}){
            if(landscape && w<240)continue;
            Layout layout=landscape ? TitleGeometry.landscape(0,0,w,h,VERSION_H,TEXT_H)
                    : TitleGeometry.portrait(0,0,w,h,VERSION_H,TEXT_H);
            Rect target=new Rect(layout.fadeX-TitleGeometry.FADE_TOUCH_SIDE_PADDING,
                    layout.fadeY-TitleGeometry.FADE_TOUCH_TOP_PADDING,
                    TitleGeometry.FADE_SIZE+2*TitleGeometry.FADE_TOUCH_SIDE_PADDING,
                    TitleGeometry.FADE_SIZE+TitleGeometry.FADE_TOUCH_TOP_PADDING);
            assertEquals("tap width",32,target.r-target.l,0.001);
            assertEquals("tap height",20,target.b-target.t,0.001);
            assertTrue("tap stays on display",target.l>=0 && target.t>=0 && target.r<=w && target.b<=h);
            for(Rect button:buttons(layout))
                assertTrue("footer tap does not overlap menu buttons",!target.hits(button));
        }
    }
    @Test public void reportedShortPortraitUsedToPlaceChangesUnderTheFooter(){
        Layout l=TitleGeometry.portrait(0,0,180,280,VERSION_H,16);
        Rect changes=buttons(l)[8];
        assertTrue(changes.b<=l.fadeY-TitleGeometry.FOOTER_GAP);
        assertTrue(l.mainH>=24);
        assertTrue(l.logoScale<1);
    }

}
