package com.shatteredpixel.shatteredpixeldungeon.ui;
import org.junit.Test;
import static org.junit.Assert.*;
public class SliderGestureTest {
 @Test public void dominantAxisLocksAcrossZoomAndLaterDiagonalChanges(){
  for(float zoom:new float[]{1,2,4,6}){
   SliderGesture v=new SliderGesture(100,100,zoom);
   assertEquals(SliderGesture.Direction.VERTICAL,v.move(100+3*zoom,100-6*zoom));
   assertEquals(SliderGesture.Direction.VERTICAL,v.move(100+70*zoom,100-8*zoom));
   SliderGesture h=new SliderGesture(100,100,zoom);
   assertEquals(SliderGesture.Direction.HORIZONTAL,h.move(100+6*zoom,100-3*zoom));
   assertEquals(SliderGesture.Direction.HORIZONTAL,h.finish(100+8*zoom,100-70*zoom));
  }
 }
 @Test public void pendingMovementDoesNotCommitAndShortSwipesClassifyOnRelease(){
  SliderGesture v=new SliderGesture(10,10,3);
  assertEquals(SliderGesture.Direction.PENDING,v.move(11,8));
  assertEquals(SliderGesture.Direction.VERTICAL,v.finish(11,4));
  SliderGesture h=new SliderGesture(10,10,3);
  assertEquals(SliderGesture.Direction.PENDING,h.move(12,11));
  assertEquals(SliderGesture.Direction.HORIZONTAL,h.finish(16,11));
 }
 @Test public void tapAndFingerJitterRemainTapsButTiedDiagonalChoosesScrolling(){
  assertEquals(SliderGesture.Direction.PENDING,new SliderGesture(0,0,4).finish(2,2));
  assertEquals(SliderGesture.Direction.PENDING,new SliderGesture(0,0,4).finish(0,0));
  assertEquals(SliderGesture.Direction.VERTICAL,new SliderGesture(0,0,4).move(20,20));
 }
 @Test public void rapidNewGesturesDoNotInheritPriorDirection(){
  for(int i=0;i<100;i++){
   assertEquals(SliderGesture.Direction.VERTICAL,new SliderGesture(0,0,2).finish(0,-6));
   assertEquals(SliderGesture.Direction.HORIZONTAL,new SliderGesture(0,0,2).finish(6,0));
   assertEquals(SliderGesture.Direction.PENDING,new SliderGesture(0,0,2).finish(0,0));
  }
 }
}
