package com.shatteredpixel.shatteredpixeldungeon.apd;
import org.junit.Test;
import static org.junit.Assert.*;
public class TitleBackgroundCoverTest {
 @Test public void backgroundCoversPhoneCompactLandscapeAndSquareAtAllScales(){
  int[][] screens={{1216,2688},{1080,2400},{480,800},{2400,1080},{800,800}};
  for(int[] screen:screens)for(int zoom=2;zoom<=9;zoom++){
   float w=screen[0]/(float)zoom,h=screen[1]/(float)zoom;boolean landscape=w>h;
   int iw=landscape?400:200,ih=landscape?200:360;
   float scale=TitleGeometry.backgroundCoverScale(w,h,iw,ih,zoom);
   assertTrue(iw*scale+.001f>=w);assertTrue(ih*scale+.001f>=h);
   assertEquals(Math.round(scale*zoom),scale*zoom,.0001f);
  }
 }
 @Test public void changingUiScaleDoesNotShrinkThePhysicalBackground(){
  float six=TitleGeometry.backgroundCoverScale(1216f/6,2688f/6,200,360,6)*6;
  float seven=TitleGeometry.backgroundCoverScale(1216f/7,2688f/7,200,360,7)*7;
  assertEquals(six,seven,.0001f);
 }
}
